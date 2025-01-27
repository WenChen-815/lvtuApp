package com.zhoujh.lvtu.find;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.adapter.CommentAdapter;
import com.zhoujh.lvtu.model.Comment;
import com.zhoujh.lvtu.model.Post;
import com.zhoujh.lvtu.model.UserInfo;
import com.zhoujh.lvtu.utils.Carousel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostDisplayActivity extends AppCompatActivity {
    private static final String TAG = "PostDisplayActivity";

    private Post post;
    private UserInfo creatorInfo;
    private List<Comment> commentList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private CommentAdapter commentAdapter;

    private ImageView star_btn;
    private ImageView like_btn;
    private ImageView back_btn;
    private ImageView menuBtn;
    private ImageView avatar;
    private Button submit;
    private Button follow;
    private LinearLayout dotLinerLayout;
    private ViewPager2 postImage;
    private TextView content;
    private TextView title;
    private TextView userName;
    private RecyclerView commentListView;
    private EditText chatInputEt;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_display);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String postJson = getIntent().getStringExtra("post");
        String postId = getIntent().getStringExtra("postId");
        if (postJson == null && postId == null) {
            Log.e(TAG, "post is null");
            finish();
        } else {
            if (postJson != null) {
                post = gson.fromJson(getIntent().getStringExtra("post"), Post.class);
                findCreaterInfo(post.getUserId());
            } else if (postId != null) {
                // TODO 通过ID查找帖子
            }
        }
        initView();
        setListener();
        setData(post);
    }

    private void findCreaterInfo(String creatorId) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/relationship/getCreatorInfo?userId="+MainActivity.USER_ID+"&creatorId="+creatorId)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        // 处理请求成功
                        Log.i(TAG, "查找用户信息成功: " + responseData);
                        creatorInfo = gson.fromJson(responseData, UserInfo.class);
                        runOnUiThread(()->{
                            userName.setText(creatorInfo.getUserName());
                            RequestOptions requestOptions = new RequestOptions()
                                    .transform(new CircleCrop());
                            Glide.with(getApplicationContext())
                                    .load("http://"+MainActivity.IP + creatorInfo.getAvatarUrl())
                                    .placeholder(R.drawable.headimg)  // 设置占位图
                                    .apply(requestOptions)// 设置签名
                                    .into(avatar);
                            if (creatorInfo.getRelationship() == 0){
                                // 设置UI为未关注状态
                                setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background);
                            } else if(creatorInfo.getRelationship() == 1){
                                setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
                            } else if(creatorInfo.getRelationship() == 2){
                                setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
                            } else if(creatorInfo.getRelationship() == 3){
                                Log.e(TAG, "获取到拉黑用户信息！");
                            }
                        });
                    } else {
                        // 处理请求失败
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

    /**
     * 设置关注按钮的UI
     *
     * @param followStr 关注按钮的文字
     * @param textColor 文本颜色
     * @param drawableId 背景
     */
    private void setFollowUI(String followStr, int textColor, int drawableId){
        Drawable drawable;
        follow.setText(followStr);
        follow.setTextColor(textColor);
        drawable = ContextCompat.getDrawable(getApplicationContext(), drawableId);
        follow.setBackground(drawable);
    }
    private void setData(Post post) {
        title.setText(post.getPostTitle());
        content.setText(post.getPostContent());
        // 图片展示
        //Carousel为自定义轮播图工具类
        Carousel carousel = new Carousel(PostDisplayActivity.this, dotLinerLayout, postImage);
        carousel.initViews(post.getPicturePath());

        loadComment(post.getPostId());
    }

    private void loadComment(String postId) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/comments/getByPostId?postId="+postId)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        commentList = gson.fromJson(responseData, new TypeToken<List<Comment>>() {}.getType());
                    } else {
                        Log.i(TAG, "查找评论失败: " + responseData);
                    }
                } else {
                    Log.e(TAG, "请求失败: " + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void initView() {
        follow = findViewById(R.id.follow);
        like_btn = findViewById(R.id.btn_like);
        menuBtn = findViewById(R.id.popupmenu);
        postImage = findViewById(R.id.post_image);
        dotLinerLayout = findViewById(R.id.index_dot);
        content = findViewById(R.id.post_content);
        title = findViewById(R.id.post_title);
        userName = findViewById(R.id.user_name);
        avatar = findViewById(R.id.avatar);
        back_btn = findViewById(R.id.btn_back);
        star_btn = findViewById(R.id.btn_star);
        submit = findViewById(R.id.submit);
        chatInputEt = findViewById(R.id.chatInputEt);
        scrollView = findViewById(R.id.sc_view);
        commentListView = findViewById(R.id.comment_list);
        commentListView.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(commentList, this);
    }

    public void setListener() {
        //点击头像跳转个人信息页
//        avatar.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(PostDisplayActivity.this, UserInfoActivity.class);
//                intent.putExtra("AuthorId", postWithUserInfo.getUserInfo().getUserId());
//                startActivity(intent);
//            }
//        });

//        follow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //处理关注点击事件
//                if (status==""){
//                    showLoginDialog();
//                } else if (MainActivity.USER_ID.equals(post.getUserId())) {
//                    //提示
//                    Toast.makeText(PostDisplayActivity.this, "不能关注自己", Toast.LENGTH_SHORT).show();
//                } else {
//                    try {
//                        //未关注，进行关注
//                        if(!isFollowed(postWithUserInfo.getUserInfo().getUserId())){
//                            follow.setBackground(getResources().getDrawable(R.drawable.round_button_followed_background));
//                            follow.setTextColor(Color.parseColor("#181A23"));
//                            follow.setText("已关注");
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    //进行关注
//                                    OkHttpClient client1 = new OkHttpClient();
//                                    RequestBody formBody = new FormBody.Builder()
//                                            .add("userId", userId)
//                                            .add("followId", postWithUserInfo.getUserInfo().getUserId())
//                                            .build();
//                                    Request request = new Request.Builder()
//                                            .url(url+"follow/addFollow")
//                                            .post(formBody)
//                                            .build();
//                                    //发起请求
//                                    try {
//                                        Response response = client1.newCall(request).execute();
//                                        //检测请求是否成功
//                                        if (response.isSuccessful()){
//
//                                        }
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            }).start();
//                        }else{
//                            //已关注，取消关注
//                            //弹出确认窗口
//                            AlertDialog.Builder builder = new AlertDialog.Builder(PostDisplayActivity.this);
//                            builder.setTitle("取消关注");
//                            builder.setMessage("确定取消关注吗？");
//                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    //已关注，取消关注
//                                    follow.setText("关注");
//                                    follow.setTextColor(Color.parseColor("#ffffff"));
//                                    follow.setBackground(getResources().getDrawable(R.drawable.round_button_unfollowed_background));
//                                    isFollow = false;
//                                    new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            String deleteUrl = url+"follow/deleteFollow";
//                                            //进行关注
//                                            OkHttpClient client1 = new OkHttpClient();
//                                            RequestBody formBody = new FormBody.Builder()
//                                                    .add("userId", userId)
//                                                    .add("followId", postWithUserInfo.getUserInfo().getUserId())
//                                                    .build();
//                                            Request request = new Request.Builder()
//                                                    .url(deleteUrl)
//                                                    .post(formBody)
//                                                    .build();
//                                            //发起请求
//                                            try {
//                                                Response response = client1.newCall(request).execute();
//                                                //检测请求是否成功
//                                                if (response.isSuccessful()){
//
//                                                }
//                                            } catch (IOException e) {
//                                                e.printStackTrace();
//                                            }
//                                        }
//                                    }).start();
//
//                                }
//                            });
//                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            });
//                            builder.show();
//                        }
//
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//        menuBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showPopupMenu(view);
//            }
//        });
//        like_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 执行收藏代码
//                if (status==""){
//                    showLoginDialog();
//                }else if (likeStatus==0){
//                    addExp("完成5次点赞");
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String postId = postWithUserInfo.getPost().getPostId();
//                            client = new OkHttpClient();
//                            MultipartBody.Builder builder = new MultipartBody.Builder()
//                                    .setType(MultipartBody.FORM)
//                                    .addFormDataPart("postId",postId)
//                                    .addFormDataPart("userId",userId);
//                            RequestBody requestBody = builder.build();
//                            Request request = new Request.Builder()
//                                    .url(url+"posts/like")
//                                    .post(requestBody)
//                                    .build();
//                            try {
//                                Response response = client.newCall(request).execute();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                    likeStatus=1;
//                    YoYo.with(Techniques.RubberBand)
//                            .duration(700)
//                            .playOn(like_btn);
//                    like_btn.setImageResource(R.mipmap.like);
//                } else if (likeStatus==1) {
//                    //取消点赞
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String postId = postWithUserInfo.getPost().getPostId();
//                            client = new OkHttpClient();
//                            MultipartBody.Builder builder = new MultipartBody.Builder()
//                                    .setType(MultipartBody.FORM)
//                                    .addFormDataPart("postId",postId)
//                                    .addFormDataPart("userId",userId);
//                            RequestBody requestBody = builder.build();
//                            Request request = new Request.Builder()
//                                    .url(url+"posts/like")
//                                    .post(requestBody)
//                                    .build();
//                            try {
//                                Response response = client.newCall(request).execute();
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                    likeStatus=0;
//                    YoYo.with(Techniques.RubberBand)
//                            .duration(700)
//                            .playOn(like_btn);
//                    like_btn.setImageResource(R.mipmap.like1);
//                }
//            }
//        });
//        star_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // 执行收藏代码
//                if (status == "") {
//                    showLoginDialog();
//                } else if (starStatus == 0) {
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String postId = postWithUserInfo.getPost().getPostId();
//                            client = new OkHttpClient();
//                            MultipartBody.Builder builder = new MultipartBody.Builder()
//                                    .setType(MultipartBody.FORM)
//                                    .addFormDataPart("postId", postId)
//                                    .addFormDataPart("userId", userId);
//                            RequestBody requestBody = builder.build();
//                            Request request = new Request.Builder()
//                                    .url(url + "posts/star")
//                                    .post(requestBody)
//                                    .build();
//                            try {
//                                Response response = client.newCall(request).execute();
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                    star_btn.setImageResource(R.mipmap.star);
//                    starStatus = 1;
//                    YoYo.with(Techniques.RubberBand)
//                            .duration(700)
//                            .playOn(star_btn);
//                } else if (starStatus == 1) {
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            String postId = postWithUserInfo.getPost().getPostId();
//                            client = new OkHttpClient();
//                            MultipartBody.Builder builder = new MultipartBody.Builder()
//                                    .setType(MultipartBody.FORM)
//                                    .addFormDataPart("postId",postId)
//                                    .addFormDataPart("userId",userId);
//                            RequestBody requestBody = builder.build();
//                            Request request = new Request.Builder()
//                                    .url(url+"posts/star")
//                                    .post(requestBody)
//                                    .build();
//                            try {
//                                Response response = client.newCall(request).execute();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }).start();
//                    starStatus = 0;
//                    YoYo.with(Techniques.RubberBand)
//                            .duration(700)
//                            .playOn(star_btn);
//                    star_btn.setImageResource(R.mipmap.star1);
//                }
//            }
//        });
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}