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
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.FollowUtils;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<UserInfo> userInfoList;
    private final Context context;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public UserAdapter(List<UserInfo> userInfoList, Context context) {
        this.userInfoList = userInfoList;
        this.context = context;
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
        // 设置关注状态的UI显示
        setFollowStateUI(holder, userInfo.getRelationship(), position);

        holder.followState.setOnClickListener(v -> {
            switch (userInfo.getRelationship()) {
                case 0:
                    updateFollow(holder, userInfo, 1, position);
                    break;
                case 1:
                case 2:
                    updateFollow(holder, userInfo, 0, position);
                    break;
                default:
                    break;
            }
        });
        holder.userItem.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserInfoActivity.class);
            intent.putExtra("userInfo", gson.toJson(userInfo));
            context.startActivity(intent);
        });
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(context)
                .load("http://" + MainActivity.IP + userInfo.getAvatarUrl())
                .placeholder(R.drawable.headimg)  // 设置占位图
                .apply(requestOptions)// 设置签名
                .into(holder.imgAvatar);
    }

    private void updateFollow(UserViewHolder holder, UserInfo userInfo, int newRelationship, int position) {
        int result = FollowUtils.updateFollow(userInfo, newRelationship, holder.followState,context);
        userInfoList.get(position).setRelationship(result);
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

    /**
     * 根据传入的状态更新关注状态的UI显示。
     *
     * @param state 状态值，用于决定UI的显示状态。0代表未关注状态,1代表已关注状态, 2代表互关状态。
     */
    private void setFollowStateUI(UserViewHolder holder, int state, int position) {
        Drawable drawable;
        switch (state) {
            case 0:
                // 设置UI为未关注状态
                holder.followState.setText("+关注");
                holder.followState.setTextColor(Color.parseColor("#FFFFFF"));
                // #03A9F4
                drawable = ContextCompat.getDrawable(context, R.drawable.round_button_unfollowed_background);
                holder.followState.setBackground(drawable);
                userInfoList.get(position).setRelationship(state);
                break;
            case 1:
                // 设置UI为已关注状态
                holder.followState.setText("已关注");
                holder.followState.setTextColor(Color.parseColor("#181A23"));
                drawable = ContextCompat.getDrawable(context, R.drawable.round_button_followed_background);
                holder.followState.setBackground(drawable);
                userInfoList.get(position).setRelationship(state);
                break;
            case 2:
                // 设置UI为互关状态
                holder.followState.setText("互关");
                holder.followState.setTextColor(Color.parseColor("#181A23"));
                drawable = ContextCompat.getDrawable(context, R.drawable.round_button_followed_background);
                holder.followState.setBackground(drawable);
                userInfoList.get(position).setRelationship(state);
                break;
            default:
                // 对于其他状态不做处理
                break;
        }
    }


}
