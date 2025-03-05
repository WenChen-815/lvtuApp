package com.zhoujh.lvtu.message.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.FollowUtils;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class InviteUserAdapter extends RecyclerView.Adapter<InviteUserAdapter.UserViewHolder> {
    public static final String TAG = "InviteUserAdapter";
    private final List<UserInfo> userInfoList;
    private final List<String> members;
    private final Context context;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private ChatActivity.InviteUserListener listener;

    public InviteUserAdapter(List<UserInfo> userInfoList, Context context, List<String> members, ChatActivity.InviteUserListener listener) {
        this.userInfoList = userInfoList;
        this.context = context;
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserInfo userInfo = userInfoList.get(position);
        holder.userName.setText(userInfo.getUserName());
        // 设置入群状态的UI显示
        Drawable drawable;
        for (String member : members) {
            if (member.equals(userInfo.getUserId())) {
                holder.followState.setText("已入群");
                holder.followState.setTextColor(Color.parseColor("#181A23"));
                drawable = ContextCompat.getDrawable(context, R.drawable.round_button_followed_background);
                holder.followState.setBackground(drawable);
                holder.followState.setClickable(false);
                break;
            } else {
                holder.followState.setText("邀请");
                holder.followState.setTextColor(Color.parseColor("#FFFFFF"));
                drawable = ContextCompat.getDrawable(context, R.drawable.round_button_unfollowed_background);
                holder.followState.setBackground(drawable);
                holder.followState.setOnClickListener(v -> {
                    new Thread(()-> {
                        try {
                            EMClient.getInstance().groupManager().addUsersToGroup(ChatActivity.GROUP_ID, new String[]{HuanXinUtils.createHXId(userInfo.getUserId())});
                        } catch (HyphenateException e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                    new Thread(() -> {
                        Request request = new Request.Builder()
                                .url("http://" + MainActivity.IP + "/lvtu/conversation/inviteUser?userName="+MainActivity.user.getUserName()+"&groupId=" + ChatActivity.GROUP_ID + "&inviteUserId=" + userInfo.getUserId())
                                .build();
                        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
                            if (response.isSuccessful()) {
                                String responseData = response.body().string();
                                Log.i(TAG, responseData);
                                if (!responseData.isEmpty()) {
                                    UserInfo newUserInfo = gson.fromJson(responseData, UserInfo.class);
                                    listener.onInviteeSuccess(newUserInfo);
                                    members.add(userInfo.getUserId());
                                    holder.followState.setText("已入群");
                                    holder.followState.setTextColor(Color.parseColor("#181A23"));
                                    Drawable drawable1 = ContextCompat.getDrawable(context, R.drawable.round_button_followed_background);
                                    holder.followState.setBackground(drawable1);
                                    holder.followState.setClickable(false);
                                }
                            } else {
                                Log.e(TAG, "onBindViewHolder: " + response.code());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }
        }
//        holder.userItem.setOnClickListener(v -> {
//            Intent intent = new Intent(context, UserInfoActivity.class);
//            intent.putExtra("userInfo", gson.toJson(userInfo));
//            context.startActivity(intent);
//        });
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(context)
                .load("http://" + MainActivity.IP + userInfo.getAvatarUrl())
                .placeholder(R.drawable.headimg)  // 设置占位图
                .apply(requestOptions)// 设置签名
                .into(holder.imgAvatar);
    }

    @Override
    public int getItemCount() {
        return userInfoList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        Button followState;
        ImageView imgAvatar;
        RelativeLayout userItem;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            followState = itemView.findViewById(R.id.follow_state);
            imgAvatar = itemView.findViewById(R.id.user_avatar);
            userItem = itemView.findViewById(R.id.user_item);
        }
    }
}
