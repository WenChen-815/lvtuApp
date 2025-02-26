package com.zhoujh.lvtu.personal;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.flyjingfish.openimagelib.OpenImage;
import com.flyjingfish.openimagelib.enums.MediaType;
import com.flyjingfish.openimagelib.transformers.ScaleInTransformer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.find.PostDisplayActivity;
import com.zhoujh.lvtu.find.adapter.PostListAdapter;
import com.zhoujh.lvtu.find.modle.Post;
import com.zhoujh.lvtu.main.adapter.PlanListAdapter;
import com.zhoujh.lvtu.main.modle.TravelPlan;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.utils.FollowUtils;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserInfoActivity extends AppCompatActivity {
    private static final String TAG = "UserInfoActivity";
    private final Gson gson = MainActivity.gson;
    private UserInfo userInfo;
    private OkHttpClient client = new OkHttpClient();

    private ImageView backbtn, avatar;
    private Button follow;
    private TextView username;
    private RelativeLayout plan, post;
    private RecyclerView dataList;
    private Button toChat;

    private List<Post> posts = new ArrayList<>();
    private List<TravelPlan> travelPlans = new ArrayList<>();
    private PostListAdapter postListAdapter;
    private PlanListAdapter planListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "intent is null");
            finish();
        } else if (intent.getStringExtra("userInfo") != null) {
            initView();
            setListener();
            userInfo = gson.fromJson(intent.getStringExtra("userInfo"), UserInfo.class);
            setData();
            getPosts();
            getTravelPlans();
        } else {
            Log.e(TAG, "userInfo is null");
        }
    }

    private void getTravelPlans() {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/getPlansByUserId?userId=" + userInfo.getUserId())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        Type type = new TypeToken<List<TravelPlan>>() {}.getType();
                        List<TravelPlan> newTravelPlans = gson.fromJson(responseData, type);
                        travelPlans.clear();
                        travelPlans.addAll(newTravelPlans);
                        runOnUiThread(() -> {
                            if (planListAdapter == null) {
                                planListAdapter = new PlanListAdapter(travelPlans, UserInfoActivity.this);
                                dataList.setAdapter(planListAdapter);
                            } else {
                                planListAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        Log.e(TAG, "获取用户历史旅行计划失败");
                    }
                } else {
                    Log.e(TAG, "请求失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void getPosts() {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/post/getPostsByUserId?userId=" + userInfo.getUserId())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        Type type = new TypeToken<List<Post>>() {}.getType();
                        List<Post> newPosts = gson.fromJson(responseData, type);
                        posts.clear();
                        posts.addAll(newPosts);
                        runOnUiThread(() -> {
                            if (postListAdapter == null) {
                                postListAdapter = new PostListAdapter(posts, UserInfoActivity.this);
                                dataList.setAdapter(postListAdapter);
                            } else {
                                postListAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        Log.e(TAG, "获取用户发帖失败");
                    }
                } else {
                    Log.e(TAG, "请求失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void setData() {
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(getApplicationContext())
                .load("http://" + MainActivity.IP + userInfo.getAvatarUrl())
                .placeholder(R.drawable.headimg)
                .apply(requestOptions)
                .into(avatar);
        username.setText(userInfo.getUserName());
        if (userInfo.getRelationship() == 0) {
            // 设置UI为未关注状态
            FollowUtils.setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background, follow, this);
        } else if (userInfo.getRelationship() == 1) {
            FollowUtils.setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
        } else if (userInfo.getRelationship() == 2) {
            FollowUtils.setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
        } else if (userInfo.getRelationship() == 3) {
            Log.e(TAG, "获取到拉黑用户信息！");
        }
    }

    private void setListener() {
        //为头像添加大图显示
        avatar.setOnClickListener(v -> {
            OpenImage.with(UserInfoActivity.this).setClickImageView(avatar)
                    .setAutoScrollScanPosition(true)
                    .setSrcImageViewScaleType(ImageView.ScaleType.CENTER_CROP, true)
                    .addPageTransformer(new ScaleInTransformer())
                    .setImageUrlList(Collections.singletonList("http://" + MainActivity.IP + userInfo.getAvatarUrl()), MediaType.IMAGE)
                    .show();
        });
        backbtn.setOnClickListener(v -> finish());
        follow.setOnClickListener(v -> {
            if (userInfo.getUserId().equals(MainActivity.USER_ID)) {
                Toast.makeText(this, "不能关注自己", Toast.LENGTH_SHORT).show();
            } else {
                int newRelationship;
                switch (userInfo.getRelationship()) {
                    case 0:
                        newRelationship = FollowUtils.updateFollow(userInfo, 1, follow, this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        userInfo.setRelationship(newRelationship);
                        break;
                    case 1:
                    case 2:
                        newRelationship = FollowUtils.updateFollow(userInfo, 0, follow, this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        userInfo.setRelationship(newRelationship);
                        break;
                    default:
                        break;
                }
            }
        });
        post.setOnClickListener(v -> {
            dataList.setAdapter(postListAdapter);
        });
        plan.setOnClickListener(v -> {
            dataList.setAdapter(planListAdapter);
        });
        toChat.setOnClickListener(v -> {
            if (userInfo.getUserId().equals(MainActivity.USER_ID)) {
                Toast.makeText(this, "不能和自己聊天~", Toast.LENGTH_SHORT).show();
            } else if(userInfo.getRelationship() != 2){
                Toast.makeText(this, "互关用户才能聊天哦~", Toast.LENGTH_SHORT).show();
            } else{
                Intent intent = new Intent(UserInfoActivity.this, ChatActivity.class);
                intent.putExtra("USER", MainActivity.USER_ID);
                intent.putExtra("TO_USER", userInfo.getUserId());
                intent.putExtra("userInfo", gson.toJson(userInfo));
                intent.putExtra("type",ChatActivity.SINGLE_TYPE);
                startActivity(intent);
            }
        });
    }

    private void initView() {
        backbtn = findViewById(R.id.back);
        avatar = findViewById(R.id.avatar);
        follow = findViewById(R.id.follow);
        username = findViewById(R.id.user_name);
        dataList = findViewById(R.id.data_list);
        plan = findViewById(R.id.plan);
        post = findViewById(R.id.post);
        postListAdapter = new PostListAdapter(posts, this);
        planListAdapter = new PlanListAdapter(travelPlans, this);
        dataList.setLayoutManager(new LinearLayoutManager(this));
        dataList.setAdapter(postListAdapter);
        toChat = findViewById(R.id.to_chat);
    }
}