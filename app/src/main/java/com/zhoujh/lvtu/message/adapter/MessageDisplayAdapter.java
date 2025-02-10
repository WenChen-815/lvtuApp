package com.zhoujh.lvtu.message.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MessageDisplayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<EMMessage> chatMessages = new ArrayList<>();
    // 定义不同的视图类型，比如自己发送的消息和对方发送的消息可以用不同布局展示
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            // 加载自己发送消息的布局文件，这里假设叫item_chat_sent.xml
            View view = inflater.inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            // 加载接收消息的布局文件，这里假设叫item_chat_received.xml
            View view = inflater.inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EMMessage message = chatMessages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        EMMessage message = chatMessages.get(position);
        if (message.getFrom().equals(ChatActivity.USER)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    public void addMessage(EMMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
    }

    // 自己发送消息的ViewHolder
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMessage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_sent_message);
        }

        public void bind(EMMessage message) {
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }
    }

    // 接收消息的ViewHolder
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_received_message);
        }

        public void bind(EMMessage message) {
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }
    }
}
