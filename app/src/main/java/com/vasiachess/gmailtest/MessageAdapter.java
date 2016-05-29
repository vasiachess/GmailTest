package com.vasiachess.gmailtest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.gmail.model.Thread;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by root on 29.05.16.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

	private Context context;
	private List<Thread> threads = new ArrayList<>();
	private OnMessageClickListener listener;

	public MessageAdapter(Context context, List<Thread> threads) {
		this.context = context;
		this.threads = threads;
	}

	public void setListener(OnMessageClickListener listener) {
		this.listener = listener;
	}

	public interface OnMessageClickListener {
		void onMessageClicked(Thread thread);
	}

	@Override public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context)
				.inflate(R.layout.list_item_messages, parent, false);

		return new MessageViewHolder(view);
	}

	@Override public void onBindViewHolder(MessageViewHolder holder, int position) {
		Thread thread = threads.get(position);
		if (thread.getSnippet() != null) {
			holder.tvTitle.setText(thread.getSnippet());
			holder.view.setOnClickListener(v -> listener.onMessageClicked(thread));
		}
	}

	@Override public int getItemCount() {
		return threads.size();
	}

	class MessageViewHolder extends RecyclerView.ViewHolder {

		public View view;

		@Bind(R.id.tv_title_LIM)
		TextView tvTitle;

		public MessageViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
			view = itemView;
		}
	}
}
