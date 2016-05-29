package com.vasiachess.gmailtest;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ViewStubCompat;
import android.view.View;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;

import java.io.IOException;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MessagesActivity extends BaseActivity {

	@Bind(R.id.rv_chat) RecyclerView rvChat;
	private String threadId;
	private ChatAdapter adapter;
	private static final String THREAD_ID = "thread_id";
	private static final String ACCOUNT = "account";

	public static void startActivity(Context context, String threadId, String accountName) {
		Intent intent = new Intent(context, MessagesActivity.class);
		intent.putExtra(THREAD_ID, threadId);
		intent.putExtra(ACCOUNT, accountName);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_messages);
		ButterKnife.bind(this);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle("My mail");
		}

		threadId = getIntent().getStringExtra(THREAD_ID);
		setUpView();

		String accountName = getIntent().getStringExtra(ACCOUNT);
		if (accountName != null) {
			mCredential.setSelectedAccountName(accountName);
			getMessages();
		}
	}

	private void setUpView() {
		rvChat.setLayoutManager(new LinearLayoutManager(this,
				android.support.v7.widget.LinearLayoutManager.VERTICAL, false));
	}

	public void getMessages() {
		if (!isGooglePlayServicesAvailable()) {
			acquireGooglePlayServices();
		} else if (!isDeviceOnline()) {
			Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
		} else {
			new MakeRequestTask(mCredential).execute();
		}
	}

	private class MakeRequestTask extends AsyncTask<Void, Void, List<Message>> {

		private Gmail mService = null;

		public MakeRequestTask(GoogleAccountCredential credential) {
			HttpTransport transport = AndroidHttp.newCompatibleTransport();
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			mService = new Gmail.Builder(
					transport, jsonFactory, credential)
					.setApplicationName("Gmail Test")
					.build();
		}

		@Override
		protected List<Message> doInBackground(Void... params) {
			try {
				return getDataFromApi();
			} catch (IOException e) {
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			mProgress.show();
		}

		@Override
		protected void onPostExecute(List<Message> output) {
			mProgress.hide();
			if (output == null || output.size() == 0) {
				Toast.makeText(MessagesActivity.this, "No results returned.", Toast.LENGTH_LONG).show();
			} else {
				rvChat.setVisibility(View.VISIBLE);
				adapter = new ChatAdapter(MessagesActivity.this, output);
				rvChat.setAdapter(adapter);
			}
		}

		private List<Message> getDataFromApi() throws IOException {

			String user = "me";
			Thread thread =
					mService.users().threads().get(user, threadId).execute();
			return thread.getMessages();
		}
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		ButterKnife.unbind(this);
	}
}
