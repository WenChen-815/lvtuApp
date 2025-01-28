package com.zhoujh.lvtu.adapter;

import static com.zhoujh.lvtu.utils.Utils.dpToPx;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.find.PostDisplayActivity;
import com.zhoujh.lvtu.model.Post;
import com.zhoujh.lvtu.model.UserInfo;
import com.zhoujh.lvtu.utils.Utils;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostListAdapter extends RecyclerView.Adapter<PostListAdapter.PostViewHolder> {
    private static final String TAG = "PostListAdapter";
    private List<Post> postList;
    private Context context;
    private final Gson gson = MainActivity.gson;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PostListAdapter(List<Post> postList, Context context) {
        this.postList = postList;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_list, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        List<String> picturePath = post.getPicturePath();
        loadCreatorInfo(post.getUserId(), holder);

        holder.title.setEllipsize(post.getPostContent().length() > 20 ? TextUtils.TruncateAt.END : null);
        holder.title.setText(post.getPostTitle());
        // 内容过长时显示省略号
        holder.content.setEllipsize(post.getPostContent().length() > 20 ? TextUtils.TruncateAt.END : null);
        holder.content.setText(post.getPostContent());
        holder.likeCount.setText(String.valueOf(post.getLikeCount()));
        holder.commentCount.setText(String.valueOf(post.getCommentCount()));

        // 图片展示 最多3张
        holder.imageContainer.removeAllViews(); // 清空容器
        for (int i = 0; i < Math.min(3, picturePath.size()); i++) {
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    dpToPx(111,context),
                    dpToPx(111,context)
            );
            layoutParams.setMargins(dpToPx(3,context), dpToPx(0,context), dpToPx(3,context), dpToPx(16,context));
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // 设置外边距
            Glide.with(context).load("http://" + MainActivity.IP + picturePath.get(i))
                    .placeholder(R.drawable.headimg)  // 设置占位图
                    .into(imageView);
            holder.imageContainer.addView(imageView);
        }

        holder.constraintLayout.setOnClickListener(v -> {
            // 转跳到详情页
            Intent intent = new Intent(context, PostDisplayActivity.class);
            intent.putExtra("post",gson.toJson(post, Post.class));
            context.startActivity(intent);
        });
    }

    private void loadCreatorInfo(String creatorId, PostViewHolder holder) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/relationship/getCreatorInfo?userId=" + MainActivity.USER_ID + "&creatorId=" + creatorId)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        UserInfo creatorInfo = gson.fromJson(responseData, UserInfo.class);
                        handler.post(() -> {
                            holder.userName.setText(creatorInfo.getUserName());
                            RequestOptions requestOptions = new RequestOptions()
                                    .transform(new CircleCrop());
                            Glide.with(context)
                                    .load("http://" + MainActivity.IP + creatorInfo.getAvatarUrl())
                                    .placeholder(R.drawable.headimg)  // 设置占位图
                                    .apply(requestOptions)// 设置签名
                                    .into(holder.avatar);
                            if (creatorInfo.getRelationship() == 0) {
                                holder.followState.setVisibility(View.INVISIBLE);
                            } else if (creatorInfo.getRelationship() == 1) {
                                holder.followState.setText("已关注");
                            } else if (creatorInfo.getRelationship() == 2) {
                                holder.followState.setText("互关");
                            } else if (creatorInfo.getRelationship() == 3) {
                                Log.e(TAG, "获取到拉黑用户信息！");
                            }
                        });

                    } else {
                        Log.i(TAG, "查找用户信息失败: " + responseData);
                    }
                } else {
                    Log.e(TAG, "请求失败: " + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView userName;
        TextView title;
        TextView content;
        TextView likeCount;
        TextView commentCount;
        TextView followState;
        LinearLayout imageContainer;
        ConstraintLayout constraintLayout;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
            userName = itemView.findViewById(R.id.textViewUsername);
            title = itemView.findViewById(R.id.textViewTitle);
            content = itemView.findViewById(R.id.textViewContentPreview);
            imageContainer = itemView.findViewById(R.id.image_container);
            likeCount = itemView.findViewById(R.id.like_count);
            commentCount = itemView.findViewById(R.id.comment_count);
            followState = itemView.findViewById(R.id.tx_follow_state);
            constraintLayout = itemView.findViewById(R.id.root_layout);
        }
    }
}
