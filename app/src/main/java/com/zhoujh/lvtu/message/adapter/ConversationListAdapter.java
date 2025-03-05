package com.zhoujh.lvtu.message.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.message.modle.ConservationAdapterItem;
import com.zhoujh.lvtu.message.modle.UserConversation;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.List;

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ConversationViewHolder> {
    private static final String TAG = "ConversationListAdapter";
    private final Gson gson =MainActivity.gson;
    private List<ConservationAdapterItem> conservationAdapterItemList;
    private Context context;

    public ConversationListAdapter(List<ConservationAdapterItem> conservationAdapterItemList, Context context) {
        this.conservationAdapterItemList = conservationAdapterItemList;
        this.context = context;
    }
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConservationAdapterItem conservationAdapterItem = conservationAdapterItemList.get(position);
        UserConversation userConversation = conservationAdapterItem.getUserConversation();
        EMConversation emConversation = conservationAdapterItem.getEmConversation();
//        Log.i(TAG,userConversation.getConversationType());
        List<UserInfo> userInfoList = userConversation.getUserInfoList();
        if (userConversation.getConversationType().equals("Chat")){
            UserInfo userInfo = userInfoList.get(0);
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new CircleCrop());
            Glide.with(context)
                    .load("http://" + MainActivity.IP + userInfo.getAvatarUrl())
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(holder.avatar);
            holder.username.setText(userInfo.getUserName());
            String lastMessage = "";
            if (emConversation.getLastMessage().getType() == EMMessage.Type.TXT){
                lastMessage = Utils.absContent(emConversation.getLastMessage().getBody().toString());
            }
            else if (emConversation.getLastMessage().getType() == EMMessage.Type.IMAGE){
                lastMessage = "[图片]";
            }
            holder.lastMessage.setText(lastMessage);
            holder.itemLayout.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("USER", userConversation.getUserId());
                intent.putExtra("TO_USER", userInfo.getUserId());
                intent.putExtra("userInfo", gson.toJson(userInfo));
                intent.putExtra("userConversation", gson.toJson(userConversation));
                intent.putExtra("type",ChatActivity.SINGLE_TYPE);
                context.startActivity(intent);
            });
        } else if(userConversation.getConversationType().equals("GroupChat")){
            RequestOptions requestOptions = new RequestOptions()
                    .transform(new CircleCrop());
            Glide.with(context)
                    .load(R.mipmap.qun)
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(holder.avatar);
            holder.username.setText(userConversation.getGroupName());
            String lastMessage = "";
            if (emConversation.getLastMessage().getType() == EMMessage.Type.TXT){
                lastMessage = Utils.absContent(emConversation.getLastMessage().getBody().toString());
            }
            else if (emConversation.getLastMessage().getType() == EMMessage.Type.IMAGE){
                lastMessage = "[图片]";
            }
            holder.lastMessage.setText(lastMessage);
            holder.itemLayout.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("USER", userConversation.getUserId());
                intent.putExtra("groupId", userConversation.getConversationId());
                intent.putExtra("userInfoList", gson.toJson(userInfoList));
                intent.putExtra("userConversation", gson.toJson(userConversation));
                intent.putExtra("type",ChatActivity.GROUP_TYPE);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return conservationAdapterItemList != null ? conservationAdapterItemList.size() : 0;
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView username, lastMessage;
        RelativeLayout itemLayout;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            username = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            itemLayout = itemView.findViewById(R.id.conversation_item);
        }
    }
}
