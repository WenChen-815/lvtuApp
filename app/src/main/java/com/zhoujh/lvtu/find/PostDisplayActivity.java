package com.zhoujh.lvtu.find;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.find.adapter.CommentAdapter;
import com.zhoujh.lvtu.find.modle.Comment;
import com.zhoujh.lvtu.find.modle.Post;
import com.zhoujh.lvtu.message.UserSearchActivity;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.FollowUtils;
import com.zhoujh.lvtu.utils.modle.UserInfo;
import com.zhoujh.lvtu.utils.modle.Carousel;
import com.zhoujh.lvtu.customView.NoScrollRecyclerView;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.Utils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostDisplayActivity extends AppCompatActivity {
    private static final String TAG = "PostDisplayActivity";

    private Post post;
    private UserInfo creatorInfo;
    private PostLike postLike;
    private List<Comment> commentList = new ArrayList<>();
    private List<Comment> finalCommentList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private CommentAdapter commentAdapter;
    private Comment replyComment;

    private ConstraintLayout rootLayout;
    private ConstraintLayout mChatInputPanel;
    private ImageView star_btn;
    private ImageView like_btn;
    private ImageView back_btn;
    private ImageView delete;
    private ImageView cancelReplyBtn;
    private ImageView avatar;
    private Button submit;
    private Button follow;
    private LinearLayout dotLinerLayout;
    private ViewPager2 postImage;
    private TextView content;
    private TextView title;
    private TextView comment_title;
    private TextView userName;
    private NoScrollRecyclerView commentListView;
    private EditText chatInputEt;
    private ScrollView scrollView;
    private RelativeLayout userItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_display);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        String postJson = getIntent().getStringExtra("post");
        String postId = getIntent().getStringExtra("postId");
        if (postJson == null && postId == null) {
            Log.e(TAG, "post is null");
            finish();
        } else {
            if (postJson != null) {
                post = gson.fromJson(getIntent().getStringExtra("post"), Post.class);
                findCreatorInfo(post.getUserId());
                checkIsLike(post.getPostId(), MainActivity.USER_ID);
            } else if (postId != null) {
                // TODO 通过ID查找帖子
            }
        }
        initView();
        setListener();
        setData(post);
    }

    private void checkIsLike(String postId, String userId) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/post/isLikePost?userId=" + userId + "&postId=" + postId)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        runOnUiThread(() -> {
                            like_btn.setImageResource(R.mipmap.like);
                            postLike = gson.fromJson(responseData, PostLike.class);
                            postLike.isLiked = true;
                        });
                    } else {
                        like_btn.setImageResource(R.mipmap.like1);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void findCreatorInfo(String creatorId) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/relationship/getCreatorInfo?userId=" + MainActivity.USER_ID + "&creatorId=" + creatorId)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        // 处理请求成功
                        Log.i(TAG, "查找用户信息成功: " + responseData);
                        creatorInfo = gson.fromJson(responseData, UserInfo.class);
                        runOnUiThread(() -> {
                            userName.setText(creatorInfo.getUserName());
                            RequestOptions requestOptions = new RequestOptions()
                                    .transform(new CircleCrop());
                            Glide.with(getApplicationContext())
                                    .load("http://" + MainActivity.IP + creatorInfo.getAvatarUrl())
                                    .placeholder(R.drawable.headimg)  // 设置占位图
                                    .apply(requestOptions)// 设置签名
                                    .into(avatar);
                            if (creatorInfo.getRelationship() == 0) {
                                // 设置UI为未关注状态
                                FollowUtils.setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background, follow, this);
                            } else if (creatorInfo.getRelationship() == 1) {
                                FollowUtils.setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
                            } else if (creatorInfo.getRelationship() == 2) {
                                FollowUtils.setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
                            } else if (creatorInfo.getRelationship() == 3) {
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

    private void setData(Post post) {
        title.setText(post.getPostTitle());
        content.setText(post.getPostContent());
        // 图片展示
        //Carousel为自定义轮播图工具类
        Carousel carousel = new Carousel(PostDisplayActivity.this, dotLinerLayout, postImage);
        carousel.initViews(post.getPicturePath());
        comment_title.setText("  共 " + post.getCommentCount() + " 条评论");
        loadComment(post.getPostId());
    }

    private void loadComment(String postId) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/comments/getByPostId?postId=" + postId)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        Log.i(TAG, "查找评论成功: " + responseData);
                        commentList = gson.fromJson(responseData, new TypeToken<List<Comment>>() {
                        }.getType());
                        runOnUiThread(() -> {
                            createFinalCommentList();
                            commentAdapter = new CommentAdapter(finalCommentList, this, new CommentReplyListener());
                            commentListView.setAdapter(commentAdapter);
                        });
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
        delete = findViewById(R.id.delete);
        if (post.getUserId().equals(MainActivity.USER_ID)) {
            delete.setVisibility(View.VISIBLE);
        }
        comment_title = findViewById(R.id.comment_title);
        follow = findViewById(R.id.follow);
        like_btn = findViewById(R.id.btn_like);
        postImage = findViewById(R.id.post_image);
        dotLinerLayout = findViewById(R.id.index_dot);
        content = findViewById(R.id.post_content);
        title = findViewById(R.id.post_title);
        userName = findViewById(R.id.user_name);
        avatar = findViewById(R.id.avatar);
        back_btn = findViewById(R.id.btn_back);
//        star_btn = findViewById(R.id.btn_star);
        submit = findViewById(R.id.submit);
        chatInputEt = findViewById(R.id.chatInputEt);
        scrollView = findViewById(R.id.sc_view);
        commentListView = findViewById(R.id.comment_list);
        commentListView.setLayoutManager(new LinearLayoutManager(this));
        rootLayout = findViewById(R.id.root_layout);
        mChatInputPanel = findViewById(R.id.mChatInputPanel);
        cancelReplyBtn = findViewById(R.id.cancelReply);
        userItem = findViewById(R.id.user_item);
    }

    public void setListener() {
        delete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除帖子")
                    .setMessage("确定要删除帖子吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread(() -> {
                                Request request = new Request.Builder()
                                        .url("http://" + MainActivity.IP + "/lvtu/post/delete?postId=" + post.getPostId())
                                        .build();
                                try (Response response = client.newCall(request).execute()) {
                                    if (response.isSuccessful()) {
                                        String responseData = response.body().string();
                                        if (!responseData.isEmpty()) {
                                            Log.i(TAG, "删除帖子成功: " + responseData);
                                            runOnUiThread(() -> {
                                                finish();
                                            });
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        userItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserInfoActivity.class);
            intent.putExtra("userInfo", gson.toJson(creatorInfo));
            startActivity(intent);
        });
        like_btn.setOnClickListener(v -> {
            if (postLike == null || !postLike.isLiked){
                new Thread(() -> {
                    Request request = new Request.Builder()
                            .url("http://" + MainActivity.IP + "/lvtu/post/likePost?postId=" + post.getPostId()+"&userId="+MainActivity.USER_ID)
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            if (!responseData.isEmpty()) {
                                postLike = gson.fromJson(responseData, PostLike.class);
                                postLike.isLiked = true;
                                runOnUiThread(() -> {
                                    like_btn.setImageResource(R.mipmap.like);
                                });
                                Log.i(TAG, "点赞成功: " + responseData);
                            } else {
                                Log.i(TAG, "点赞失败: " + responseData);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } else {
                new Thread(() -> {
                    Request request = new Request.Builder()
                            .url("http://" + MainActivity.IP + "/lvtu/post/unlikePost?postId=" + post.getPostId()+"&userId="+MainActivity.USER_ID)
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            if (!responseData.isEmpty()) {
                                postLike.isLiked = false;
                                runOnUiThread(() -> {
                                    like_btn.setImageResource(R.mipmap.like1);
                                });
                                Log.i(TAG, "取消点赞: " + responseData);
                            } else {
                                Log.i(TAG, "取消点赞失败: " + responseData);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        });
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
        // 监听布局变化
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mChatInputPanel.getLayoutParams();
                if (keypadHeight > screenHeight * 0.15) {
                    // 软键盘显示时，将输入框上移
                    layoutParams.bottomMargin = keypadHeight;
                } else {
                    // 软键盘隐藏
                    layoutParams.bottomMargin = 0;
                }
                mChatInputPanel.setLayoutParams(layoutParams);
            }
        });
        cancelReplyBtn.setOnClickListener(v -> {
            chatInputEt.setHint("");
            chatInputEt.setTag("");
            chatInputEt.setPadding(chatInputEt.getPaddingLeft(), chatInputEt.getPaddingTop(), Utils.dpToPx(5, PostDisplayActivity.this), chatInputEt.getPaddingBottom());
            cancelReplyBtn.setVisibility(View.GONE);
            // 隐藏键盘
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(chatInputEt.getWindowToken(), 0);
            }
            // 清除回复
            replyComment = null;
        });
        submit.setOnClickListener(v -> {
            String content = chatInputEt.getText().toString();
            if (content.isEmpty()) {
                Toast.makeText(PostDisplayActivity.this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            } else {
                Comment comment;
                // 定义日期时间格式
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                if (replyComment != null) {
                    comment = new Comment(post.getPostId(),
                            replyComment.getId(),
                            MainActivity.user.getUserId(),
                            replyComment.getUserId(),
                            content,
                            LocalDateTime.now(),
                            MainActivity.user.getUserName(),
                            replyComment.getUserName()
                    );
                } else {
                    comment = new Comment(post.getPostId(),
                            MainActivity.user.getUserId(),
                            content,
                            LocalDateTime.now(),
                            MainActivity.user.getUserName()
                    );
                }

                new Thread(() -> {
                    RequestBody requestBody = RequestBody.create(
                            gson.toJson(comment),
                            MediaType.parse("application/json; charset=utf-8")
                    );
                    Request request = new Request.Builder()
                            .url("http://" + MainActivity.IP + "/lvtu/comments/addComment")
                            .post(requestBody)
                            .build();
                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            String responseData = response.body().string();
                            if (!responseData.isEmpty()) {
                                Log.i(TAG, "评论成功: " + responseData);
                                Comment newComment = gson.fromJson(responseData, Comment.class);
                                runOnUiThread(() -> {
                                    post.setCommentCount(post.getCommentCount() + 1);
                                    comment_title.setText("  共 " + post.getCommentCount() + " 条评论");
                                    Toast.makeText(PostDisplayActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                                    commentList.add(newComment);
                                    finalCommentList.clear();
                                    createFinalCommentList();
//                                    commentAdapter = new CommentAdapter(finalCommentList, this, new CommentReplyListener());
//                                    commentListView.setAdapter(commentAdapter);
                                    commentAdapter.notifyDataSetChanged();

                                    chatInputEt.setText("");
                                    chatInputEt.setTag("");
                                    // 隐藏键盘
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(chatInputEt.getWindowToken(), 0);
                                    }
                                });
                            } else {
                                Log.i(TAG, "评论失败: " + responseData);
                            }
                        } else {
                            Log.e(TAG, "请求失败: " + response.code());
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        });
        follow.setOnClickListener(v -> {
            if (creatorInfo.getUserId().equals(MainActivity.USER_ID)) {
                Toast.makeText(PostDisplayActivity.this, "不能关注自己", Toast.LENGTH_SHORT).show();
            } else {
                int newRelationship;
                switch (creatorInfo.getRelationship()) {
                    case 0:
                        newRelationship = FollowUtils.updateFollow(creatorInfo, 1, follow, this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        creatorInfo.setRelationship(newRelationship);
                        break;
                    case 1:
                    case 2:
                        newRelationship = FollowUtils.updateFollow(creatorInfo, 0, follow, this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        creatorInfo.setRelationship(newRelationship);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void updateFollow(UserInfo creatorInfo, int newRelationship) {
        Thread thread = new Thread(() -> {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("userId", MainActivity.USER_ID)
                    .addFormDataPart("relatedUserId", creatorInfo.getUserId())
                    .addFormDataPart("relationshipType", String.valueOf(newRelationship))
                    .build();
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/relationship/update")
                    .post(requestBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    int responseData = Integer.parseInt(response.body().string());
                    runOnUiThread(() -> {
                        if (responseData == -1) {
                            Log.e(TAG, "update relationship error");
                        } else {
                            Log.i(TAG, "relationship：" + responseData);
                            if (responseData == 0) {
                                // 设置UI为未关注状态
                                FollowUtils.setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background, follow, this);
                            } else if (responseData == 1) {
                                FollowUtils.setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
                            } else if (responseData == 2) {
                                FollowUtils.setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, this);
                            } else if (responseData == 3) {
                                Log.e(TAG, "获取到拉黑用户信息！");
                            }
                            creatorInfo.setRelationship(responseData);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }


    public class CommentReplyListener {
        public void onReplyClick(Comment comment) {
            replyComment = comment;
            chatInputEt.setHint("回复 " + comment.getUserName() + " :");
            chatInputEt.setTag(comment.getId());
            chatInputEt.setPadding(chatInputEt.getPaddingLeft(), chatInputEt.getPaddingTop(), Utils.dpToPx(40, PostDisplayActivity.this), chatInputEt.getPaddingBottom());
            cancelReplyBtn.setVisibility(View.VISIBLE);
            // 弹出用户键盘
            chatInputEt.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(chatInputEt, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    public void createFinalCommentList() {
        // 构建父子关系映射
        Map<String, List<Comment>> childCommentsMap = new HashMap<>();
        for (Comment comment : commentList) {
            String parentId = comment.getParentId();
            if (parentId != null && !parentId.isEmpty()) {
                childCommentsMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(comment);
            }
        }

        // 找出顶级评论
        List<Comment> topLevelComments = new ArrayList<>();
        for (Comment comment : commentList) {
            if (comment.getParentId() == null || comment.getParentId().isEmpty()) {
                topLevelComments.add(comment);
            }
        }

        // 重新排列评论
        for (Comment topLevelComment : topLevelComments) {
            finalCommentList.add(topLevelComment);
            addChildComments(topLevelComment, childCommentsMap);
        }
    }

    private void addChildComments(Comment parentComment, Map<String, List<Comment>> childCommentsMap) {
        List<Comment> childComments = childCommentsMap.get(parentComment.getId());
        if (childComments != null) {
            for (Comment childComment : childComments) {
                finalCommentList.add(childComment);
                addChildComments(childComment, childCommentsMap);
            }
        }
    }

    private static class PostLike {
        public Long id;
        public String postId;
        public String userId;
        public boolean isLiked;

        public PostLike() {
        }

        public PostLike(Long id, String postId, String userId) {
            this.id = id;
            this.postId = postId;
            this.userId = userId;
        }
    }
}