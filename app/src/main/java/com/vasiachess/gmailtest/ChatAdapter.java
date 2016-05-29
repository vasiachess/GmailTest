package com.vasiachess.gmailtest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.gmail.model.Message;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by root on 29.05.16.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

	private Context context;
	private List<Message> messages = new ArrayList<>();

	public ChatAdapter(Context context, List<Message> messages) {
		this.context = context;
		this.messages = messages;
	}

	@Override public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context)
				.inflate(R.layout.list_item_chat, parent, false);

		return new ChatViewHolder(view);
	}

	@Override public void onBindViewHolder(ChatViewHolder holder, int position) {
		Message message = messages.get(position);
		if (message.getSnippet() != null) {
			holder.tvLeft.setText(message.getSnippet());
		}
	}

	@Override public int getItemCount() {
		return messages.size();
	}

	class ChatViewHolder extends RecyclerView.ViewHolder {

		public View view;

		@Bind(R.id.tv_left_LIC)
		TextView tvLeft;

		public ChatViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
			view = itemView;
		}
	}
}
