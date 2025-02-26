package com.zhoujh.lvtu.message.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class MessageDisplayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<EMMessage> chatMessages = new ArrayList<>();
    private List<UserInfo> userInfoList = new ArrayList<>();
    private final Context context;
    private final Gson gson = MainActivity.gson;
    // 定义不同的视图类型，比如自己发送的消息和对方发送的消息可以用不同布局展示
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public MessageDisplayAdapter(Context context, List<UserInfo> userInfoList) {
        this.context = context;
        this.userInfoList = userInfoList;
    }

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
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        EMMessage message = chatMessages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
            Glide.with(context)
                    .load("http://" + MainActivity.IP + MainActivity.user.getAvatarUrl())
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(((SentMessageViewHolder) holder).avatar);
            ((SentMessageViewHolder) holder).avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("fragment_to_load", "action_my");
                context.startActivity(intent);
            });
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
            UserInfo u = new UserInfo();
            for (int i = 0; i < userInfoList.size(); i++) {
                if (HuanXinUtils.createHXId(userInfoList.get(i).getUserId()).equals(message.getFrom())) {
                    u = userInfoList.get(i);
                    break;
                }
            }
            Glide.with(context)
                    .load("http://" + MainActivity.IP + u.getAvatarUrl())
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(((ReceivedMessageViewHolder) holder).avatar);
            UserInfo finalU = u;
            ((ReceivedMessageViewHolder) holder).avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, UserInfoActivity.class);
                intent.putExtra("userInfo", gson.toJson(finalU));
                context.startActivity(intent);
            });
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
        TextView textViewMessage;
        ImageView avatar;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_sent_message);
            avatar = itemView.findViewById(R.id.avatar);
        }

        public void bind(EMMessage message) {
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }
    }

    // 接收消息的ViewHolder
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        ImageView avatar;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_view_received_message);
            avatar = itemView.findViewById(R.id.avatar);
        }

        public void bind(EMMessage message) {
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }
    }
}
