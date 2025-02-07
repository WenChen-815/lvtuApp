package com.zhoujh.lvtu.adapter;

import static com.zhoujh.lvtu.utils.Utils.dpToPx;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import com.zhoujh.lvtu.main.PlanDisplayActivity;
import com.zhoujh.lvtu.model.TravelPlan;
import com.zhoujh.lvtu.model.UserInfo;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.PlanViewHolder>{
    private static final String TAG = "PlanListAdapter";
    private List<TravelPlan> planList;
    private Context context;
    private final Gson gson = MainActivity.gson;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PlanListAdapter(List<TravelPlan> planList, Context context) {
        this.planList = planList;
        this.context = context;
    }
    @NonNull
    @Override
    public PlanListAdapter.PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_list, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanListAdapter.PlanViewHolder holder, int position) {
        TravelPlan travelPlan = planList.get(position);
        loadCreatorInfo(travelPlan.getUserId(), holder);

        holder.title.setEllipsize(travelPlan.getTitle().length() > 20 ? TextUtils.TruncateAt.END : null);
        holder.title.setText(travelPlan.getTitle());
        holder.content.setEllipsize(travelPlan.getContent().length() > 20 ? TextUtils.TruncateAt.END : null);
        holder.content.setText(travelPlan.getContent());
        holder.currentParticipants.setText(String.valueOf(travelPlan.getCurrentParticipants()));
        if(travelPlan.getMaxParticipants() > 0){
            if (travelPlan.getCurrentParticipants() >= travelPlan.getMaxParticipants()) {
                holder.currentParticipants.setTextColor(Color.parseColor("#FF0000"));
            }
            holder.maxParticipants.setText("/" + travelPlan.getMaxParticipants());
        }else{
            holder.maxParticipants.setText("/无限制");
        }
        if (travelPlan.getImageUrl() != null){
            holder.imageContainer.removeAllViews(); // 清空容器
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    dpToPx(111,context),
                    dpToPx(111,context)
            );
            layoutParams.setMargins(dpToPx(3,context), dpToPx(0,context), dpToPx(3,context), dpToPx(16,context));
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            // 设置外边距
            Glide.with(context).load("http://" + MainActivity.IP + travelPlan.getImageUrl())
                    .placeholder(R.drawable.headimg)  // 设置占位图
                    .into(imageView);
            holder.imageContainer.addView(imageView);
        }

        holder.constraintLayout.setOnClickListener(v -> {
            // 转跳到详情页
            Intent intent = new Intent(context, PlanDisplayActivity.class);
            intent.putExtra("travelPlanJson",gson.toJson(travelPlan, TravelPlan.class));
            context.startActivity(intent);
        });
    }

    private void loadCreatorInfo(String creatorId, PlanListAdapter.PlanViewHolder holder) {
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
        return planList != null ? planList.size() : 0;
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView userName;
        TextView title;
        TextView content;
        TextView maxParticipants, currentParticipants;
        TextView followState;
        LinearLayout imageContainer;
        ConstraintLayout constraintLayout;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.imageViewUserAvatar);
            userName = itemView.findViewById(R.id.textViewUsername);
            title = itemView.findViewById(R.id.textViewTitle);
            content = itemView.findViewById(R.id.textViewContentPreview);
            imageContainer = itemView.findViewById(R.id.image_container);
            followState = itemView.findViewById(R.id.tx_follow_state);
            constraintLayout = itemView.findViewById(R.id.root_layout);
            maxParticipants = itemView.findViewById(R.id.max_participants);
            currentParticipants = itemView.findViewById(R.id.current_participants);
        }
    }
}
