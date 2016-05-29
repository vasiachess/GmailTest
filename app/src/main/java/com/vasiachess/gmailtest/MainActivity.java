package com.vasiachess.gmailtest;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity
		implements EasyPermissions.PermissionCallbacks {

	@Bind(R.id.btn_sign_in) Button btnSignIn;
	@Bind(R.id.rv_messages) RecyclerView rvMessages;

	private MessageAdapter adapter;
	private String accountName;


	/**
	 * Create the main activity.
	 *
	 * @param savedInstanceState previously saved instance data.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		setUpView();
	}


	/**
	 * Attempt to call the API, after verifying that all the preconditions are
	 * satisfied. The preconditions are: Google Play Services installed, an
	 * account was selected and the device currently has online access. If any
	 * of the preconditions are not satisfied, the app will prompt the user as
	 * appropriate.
	 */
	private void getResultsFromApi() {
		if (!isGooglePlayServicesAvailable()) {
			acquireGooglePlayServices();
		} else if (mCredential.getSelectedAccountName() == null) {
			chooseAccount();
		} else if (!isDeviceOnline()) {
			Toast.makeText(this, "No network connection available.", Toast.LENGTH_LONG).show();
		} else {
			new MakeRequestTask(mCredential).execute();
		}
	}

	/**
	 * Attempts to set the account used with the API credentials. If an account
	 * name was previously saved it will use that one; otherwise an account
	 * picker dialog will be shown to the user. Note that the setting the
	 * account to use with the credentials object requires the app to have the
	 * GET_ACCOUNTS permission, which is requested here if it is not already
	 * present. The AfterPermissionGranted annotation indicates that this
	 * function will be rerun automatically whenever the GET_ACCOUNTS permission
	 * is granted.
	 */
	@AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
	private void chooseAccount() {
		if (EasyPermissions.hasPermissions(
				this, Manifest.permission.GET_ACCOUNTS)) {
			accountName = getPreferences(Context.MODE_PRIVATE)
					.getString(PREF_ACCOUNT_NAME, null);
			if (accountName != null) {
				mCredential.setSelectedAccountName(accountName);
				getResultsFromApi();
			} else {
				// Start a dialog from which the user can choose an account
				startActivityForResult(
						mCredential.newChooseAccountIntent(),
						REQUEST_ACCOUNT_PICKER);
			}
		} else {
			// Request the GET_ACCOUNTS permission via a user dialog
			EasyPermissions.requestPermissions(
					this,
					"This app needs to access your Google account (via Contacts).",
					REQUEST_PERMISSION_GET_ACCOUNTS,
					Manifest.permission.GET_ACCOUNTS);
		}
	}

	/**
	 * Called when an activity launched here (specifically, AccountPicker
	 * and authorization) exits, giving you the requestCode you started it with,
	 * the resultCode it returned, and any additional data from it.
	 *
	 * @param requestCode code indicating which activity result is incoming.
	 * @param resultCode  code indicating the result of the incoming
	 *                    activity result.
	 * @param data        Intent (containing result data) returned by incoming
	 *                    activity result.
	 */
	@Override
	protected void onActivityResult(
			int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_GOOGLE_PLAY_SERVICES:
				if (resultCode != RESULT_OK) {
					Toast.makeText(this,
							"This app requires Google Play Services. Please install " +
									"Google Play Services on your device and relaunch this app.", Toast.LENGTH_LONG).show();
				} else {
					getResultsFromApi();
				}
				break;
			case REQUEST_ACCOUNT_PICKER:
				if (resultCode == RESULT_OK && data != null &&
						data.getExtras() != null) {
					String accountName =
							data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
					if (accountName != null) {
						SharedPreferences settings =
								getPreferences(Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = settings.edit();
						editor.putString(PREF_ACCOUNT_NAME, accountName);
						editor.apply();
						mCredential.setSelectedAccountName(accountName);
						getResultsFromApi();
					}
				}
				break;
			case REQUEST_AUTHORIZATION:
				if (resultCode == RESULT_OK) {
					getResultsFromApi();
				}
				break;
		}
	}

	/**
	 * Respond to requests for permissions at runtime for API 23 and above.
	 *
	 * @param requestCode  The request code passed in
	 *                     requestPermissions(android.app.Activity, String, int, String[])
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions
	 *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		EasyPermissions.onRequestPermissionsResult(
				requestCode, permissions, grantResults, this);
	}

	/**
	 * Callback for when a permission is granted using the EasyPermissions
	 * library.
	 *
	 * @param requestCode The request code associated with the requested
	 *                    permission
	 * @param list        The requested permission list. Never null.
	 */
	@Override
	public void onPermissionsGranted(int requestCode, List<String> list) {
		// Do nothing.
	}

	/**
	 * Callback for when a permission is denied using the EasyPermissions
	 * library.
	 *
	 * @param requestCode The request code associated with the requested
	 *                    permission
	 * @param list        The requested permission list. Never null.
	 */
	@Override
	public void onPermissionsDenied(int requestCode, List<String> list) {
		// Do nothing.
	}

	@OnClick(R.id.btn_sign_in) public void onClick() {
		getResultsFromApi();
	}

	private void setUpView() {
		rvMessages.setLayoutManager(new LinearLayoutManager(this,
				android.support.v7.widget.LinearLayoutManager.VERTICAL, false));
	}

	/**
	 * An asynchronous task that handles the Gmail API call.
	 * Placing the API calls in their own task ensures the UI stays responsive.
	 */
	private class MakeRequestTask extends AsyncTask<Void, Void, List<Thread>> {
		private Gmail mService = null;
		private Exception mLastError = null;

		public MakeRequestTask(GoogleAccountCredential credential) {
			HttpTransport transport = AndroidHttp.newCompatibleTransport();
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			mService = new Gmail.Builder(
					transport, jsonFactory, credential)
					.setApplicationName("Gmail Test")
					.build();
		}

		/**
		 * Background task to call Gmail API.
		 *
		 * @param params no parameters needed for this task.
		 */
		@Override
		protected List<Thread> doInBackground(Void... params) {
			try {
				return getDataFromApi();
			} catch (Exception e) {
				mLastError = e;
				cancel(true);
				return null;
			}
		}

		/**
		 * Fetch a list of Gmail labels attached to the specified account.
		 *
		 * @return List of Strings labels.
		 * @throws IOException
		 */
		private List<Thread> getDataFromApi() throws IOException {
			// Get the labels in the user's account.
			String user = "me";

			ListThreadsResponse listResponse =
					mService.users().threads().list(user).setMaxResults(10L).execute();

			return listResponse.getThreads();
		}


		@Override
		protected void onPreExecute() {
			mProgress.show();
		}

		@Override
		protected void onPostExecute(List<Thread> output) {
			mProgress.hide();
			if (output == null || output.size() == 0) {
				Toast.makeText(MainActivity.this, "No results returned.", Toast.LENGTH_LONG).show();
			} else {
				if (getSupportActionBar() != null) {
					getSupportActionBar().setTitle("My mail");
				}
				rvMessages.setVisibility(View.VISIBLE);
				adapter = new MessageAdapter(MainActivity.this, output);
				adapter.setListener(this::navigateToMessageActivity);
				rvMessages.setAdapter(adapter);
			}
		}

		private void navigateToMessageActivity(Thread thread) {
			MessagesActivity.startActivity(MainActivity.this, thread.getId(), accountName);
		}

		@Override
		protected void onCancelled() {
			mProgress.hide();
			if (mLastError != null) {
				if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
					showGooglePlayServicesAvailabilityErrorDialog(
							((GooglePlayServicesAvailabilityIOException) mLastError)
									.getConnectionStatusCode());
				} else if (mLastError instanceof UserRecoverableAuthIOException) {
					startActivityForResult(
							((UserRecoverableAuthIOException) mLastError).getIntent(),
							MainActivity.REQUEST_AUTHORIZATION);
				} else {
					Toast.makeText(MainActivity.this, "The following error occurred:\n"
							+ mLastError.getMessage(), Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(MainActivity.this, "Request cancelled.", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		ButterKnife.unbind(this);
	}
}
