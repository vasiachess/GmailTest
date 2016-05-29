package com.vasiachess.gmailtest;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;


/**
 * Created by root on 29.05.16.
 */
public abstract class BaseActivity extends AppCompatActivity {

	protected GoogleAccountCredential mCredential;
	protected ProgressDialog mProgress;

	public static final int REQUEST_ACCOUNT_PICKER = 1000;
	public static final int REQUEST_AUTHORIZATION = 1001;
	public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
	public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
	public static final String PREF_ACCOUNT_NAME = "accountName";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mProgress = new ProgressDialog(this);
		mProgress.setMessage("Calling Gmail API ...");

		// Initialize credentials and service object.
		mCredential = GoogleAccountCredential.usingOAuth2(
				getApplicationContext(), GmailScopes.all())
				.setBackOff(new ExponentialBackOff());
	}

	protected void acquireGooglePlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
		}
	}

	protected void showGooglePlayServicesAvailabilityErrorDialog(
			final int connectionStatusCode) {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		Dialog dialog = apiAvailability.getErrorDialog(
				BaseActivity.this,
				connectionStatusCode,
				REQUEST_GOOGLE_PLAY_SERVICES);
		dialog.show();
	}


	public boolean isGooglePlayServicesAvailable() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
		return connectionStatusCode == ConnectionResult.SUCCESS;
	}

	protected boolean isDeviceOnline() {
		ConnectivityManager connMgr =
				(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
}
