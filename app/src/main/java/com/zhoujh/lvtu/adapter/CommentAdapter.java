package com.zhoujh.lvtu.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.model.Comment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder>{
    private static final String TAG = "CommentAdapter";
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());


    private List<Comment> finalList;

    private Context context;

    public CommentAdapter(List<Comment> commentList, Context context) {
        this.context = context;
        List<Comment> organizedList = new ArrayList<>();
        Map<String, List<Comment>> childCommentsMap = new HashMap<>();
        // 将二级评论归类到对应的 parentId
        for (Comment comment : commentList) {
            if (comment.getParentId() == null || comment.getParentId().isEmpty()) {
                // 一级评论直接加入到 organizedList
                organizedList.add(comment);
            } else {
                // 二级评论归类到 childCommentsMap
                childCommentsMap.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>()).add(comment);
            }
        }

        // 将二级评论插入到对应一级评论之后
        finalList = new ArrayList<>();
        for (Comment comment : organizedList) {
            finalList.add(comment); // 添加一级评论
            List<Comment> childComments = childCommentsMap.get(comment.getId());
            if (childComments != null) {
                finalList.addAll(childComments); // 插入二级评论
            }
        }
    }

    @NonNull
    @Override
    public CommentAdapter.CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentAdapter.CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.CommentViewHolder holder, int position) {
        Comment comment = finalList.get(position);

        // 设置头像
        loadUserAvatar(comment.getUserId(), holder);

        // 设置用户名
        holder.commentUserName.setText(comment.getUserName());

        // 设置回复的用户名
        if (comment.getReplyToUserName() != null && !comment.getReplyToUserName().isEmpty()) {
            holder.commentReplyToUserName.setText(" 回复 " + comment.getReplyToUserName());
            holder.commentReplyToUserName.setVisibility(View.VISIBLE);
        } else {
            holder.commentReplyToUserName.setVisibility(View.GONE);
        }

        // 设置评论内容
        holder.commentContent.setText(comment.getContent());

        // 设置评论创建时间
        String commentCreateTime = comment.getCreateTime().toString().replace("T", " ");
        holder.commentCreateTime.setText(commentCreateTime);
    }

    private void loadUserAvatar(String userId, CommentViewHolder holder) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/user/getUserAvatarById?userId=" + userId)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        handler.post(() -> {
                            RequestOptions requestOptions = new RequestOptions()
                                    .transform(new CircleCrop());
                            Glide.with(context)
                                    .load("http://" + MainActivity.IP + responseData)
                                    .placeholder(R.drawable.headimg)  // 设置占位图
                                    .apply(requestOptions)// 设置签名
                                    .into(holder.avatar);
                        });

                    } else {
                        Log.i(TAG, "查找用户头像失败: " + responseData);
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
        return finalList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder{
        public ImageView avatar;
        public TextView commentUserName;
        public TextView commentReplyToUserName;
        public TextView commentContent;
        public TextView commentCreateTime;
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            commentUserName = itemView.findViewById(R.id.commentUserName);
            commentReplyToUserName = itemView.findViewById(R.id.commentReplyToUserName);
            commentContent = itemView.findViewById(R.id.commentContent);
            commentCreateTime = itemView.findViewById(R.id.commentCreateTime);
        }
    }
}
