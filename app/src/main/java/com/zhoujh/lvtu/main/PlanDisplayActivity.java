package com.zhoujh.lvtu.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.model.TravelPlan;
import com.zhoujh.lvtu.model.UserInfo;
import com.zhoujh.lvtu.utils.Carousel;
import com.zhoujh.lvtu.utils.StatusBarUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlanDisplayActivity extends AppCompatActivity {
    public static final String TAG = "PlanDisplayActivity";
    public static final String TRAVEL_PLAN_ID = "travelPlanId";
    private TravelPlan travelPlan;
    private UserInfo creatorInfo;
    private OkHttpClient client = new OkHttpClient();
    private final Gson gson = MainActivity.gson;

    private ConstraintLayout rootLayout;
    private ConstraintLayout mChatInputPanel;
    private ImageView back_btn;
    private ImageView avatar;
    private Button submit;
    private Button follow;
    private LinearLayout dotLinerLayout;
    private ViewPager2 planImage;
    private TextView content;
    private TextView title;
    private TextView status, time, maxParticipants, currentParticipants, budget, address, travelMode;
    private TextView userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_display);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        Intent intent = getIntent();
        if (intent.getStringExtra("travelPlanJson") != null) {
            travelPlan = new TravelPlan();
            travelPlan = gson.fromJson(intent.getStringExtra("travelPlanJson"), TravelPlan.class);
            findCreatorInfo(travelPlan.getUserId());
            initView();
            setListener();
            setData();
        } else {
            Log.i(TAG, "空数据");
            finish();
        }
    }

    private void setData() {
        if (travelPlan != null) {
            title.setText(travelPlan.getTitle());
            content.setText(travelPlan.getContent());
            if (travelPlan.getImageUrl() != null) {
                Carousel carousel = new Carousel(PlanDisplayActivity.this, dotLinerLayout, planImage);
                List<String> imagePaths = new ArrayList<>();
                imagePaths.add(travelPlan.getImageUrl());
                carousel.initViews(imagePaths);
            }
            //0-草稿 1-进行中（默认）2-已取消 3-已结束
            switch (travelPlan.getStatus()) {
                case 0:
                    status.setText("草稿");
                    status.setTextColor(Color.parseColor("#FFA500"));
                    break;
                case 1:
                    status.setText("进行中");
                    status.setTextColor(Color.parseColor("#FFA500"));
                    break;
                case 2:
                    status.setText("已取消");
                    status.setTextColor(Color.parseColor("#FF0000"));
                    break;
                case 3:
                    status.setText("已结束");
                    status.setTextColor(Color.parseColor("#FFA500"));
                    break;
            }
            // 出行方式 0-其他 1-步行 2-骑行 3-自驾
            switch (travelPlan.getTravelMode()) {
                case 0:
                    travelMode.setText("其他");
                    break;
                case 1:
                    travelMode.setText("步行");
                    break;
                case 2:
                    travelMode.setText("骑行");
                    break;
                case 3:
                    travelMode.setText("自驾");
                    break;
            }
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.PRC);
            String startTime = dateFormatter.format(travelPlan.getStartTime());
            String endTime = dateFormatter.format(travelPlan.getEndTime());
            time.setText(startTime + " ~ " + endTime);

            currentParticipants.setText(String.valueOf(travelPlan.getCurrentParticipants()));
            if(travelPlan.getMaxParticipants() > 0){
                if (travelPlan.getCurrentParticipants() >= travelPlan.getMaxParticipants()) {
                    currentParticipants.setTextColor(Color.parseColor("#FF0000"));
                }
                maxParticipants.setText("/" + travelPlan.getMaxParticipants());
            }else{
                maxParticipants.setText("/无限制");
            }
            budget.setText(String.format("%.2f", travelPlan.getBudget())+"（元）");
            address.setText(travelPlan.getAddress());

            // TODO 添加高德地图 展示大致位置
        }
    }

    private void setListener() {
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
        follow.setOnClickListener(v -> {
            if (creatorInfo.getUserId().equals(MainActivity.USER_ID)) {
                Toast.makeText(PlanDisplayActivity.this, "不能关注自己", Toast.LENGTH_SHORT).show();
            } else {
                switch (creatorInfo.getRelationship()) {
                    case 0:
                        updateFollow(creatorInfo, 1);
                        break;
                    case 1:
                    case 2:
                        updateFollow(creatorInfo, 0);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void initView() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        follow = findViewById(R.id.follow);
//        like_btn = findViewById(R.id.btn_like);
//        menuBtn = findViewById(R.id.popupmenu);
        planImage = findViewById(R.id.plan_image);
        dotLinerLayout = findViewById(R.id.index_dot);
        content = findViewById(R.id.content);
        title = findViewById(R.id.title);
        userName = findViewById(R.id.user_name);
        avatar = findViewById(R.id.avatar);
//        star_btn = findViewById(R.id.btn_star);
        submit = findViewById(R.id.submit);
        rootLayout = findViewById(R.id.root_layout);
        mChatInputPanel = findViewById(R.id.mChatInputPanel);
        status = findViewById(R.id.status);
        time = findViewById(R.id.time);
        maxParticipants = findViewById(R.id.max_participants);
        currentParticipants = findViewById(R.id.current_participants);
        budget = findViewById(R.id.budget);
        travelMode = findViewById(R.id.travel_mode);
        address = findViewById(R.id.address);
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
                                setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background);
                            } else if (responseData == 1) {
                                setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
                            } else if (responseData == 2) {
                                setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
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
                                setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background);
                            } else if (creatorInfo.getRelationship() == 1) {
                                setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
                            } else if (creatorInfo.getRelationship() == 2) {
                                setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background);
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

    /**
     * 设置关注按钮的UI
     *
     * @param followStr  关注按钮的文字
     * @param textColor  文本颜色
     * @param drawableId 背景
     */
    private void setFollowUI(String followStr, int textColor, int drawableId) {
        Drawable drawable;
        follow.setText(followStr);
        follow.setTextColor(textColor);
        drawable = ContextCompat.getDrawable(getApplicationContext(), drawableId);
        follow.setBackground(drawable);
    }

}