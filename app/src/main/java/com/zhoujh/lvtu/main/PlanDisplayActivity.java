package com.zhoujh.lvtu.main;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.zhoujh.lvtu.find.modle.PlanParticipant;
import com.zhoujh.lvtu.main.modle.TravelPlan;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.FollowUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.UserInfo;
import com.zhoujh.lvtu.utils.modle.Carousel;
import com.zhoujh.lvtu.utils.StatusBarUtils;

import org.w3c.dom.Text;

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
    private List<UserInfo> participants = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private int SUBMIT_TYPE = 0; //0-草稿 1-已参加 2-未参加 3-已满员 4-已取消/已结束 5-创建者

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
    private HorizontalScrollView scroll;
    private RelativeLayout userItem;

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
        if (intent == null) {
            Log.e(TAG, "intent is null");
            finish();
        }
        if (intent.getStringExtra("travelPlanJson") != null) {
            travelPlan = new TravelPlan();
            travelPlan = gson.fromJson(intent.getStringExtra("travelPlanJson"), TravelPlan.class);
            initTravelPlan();
        } else if (intent.getStringExtra("travelPlanId") != null) {
            String travelPlanId = intent.getStringExtra("travelPlanId");
            new Thread(() -> {
                Request request = new Request.Builder()
                        .url("http://" + MainActivity.IP + "/lvtu/travelPlans/getPlanById?travelPlanId=" + travelPlanId)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        if (!responseData.isEmpty()) {
                            travelPlan = gson.fromJson(responseData, TravelPlan.class);
                            runOnUiThread(() -> {
                                initTravelPlan();
                            });
                        }
                    } else {
                        Log.i(TAG, "网络错误");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        } else {
            Log.i(TAG, "空数据");
            finish();
        }
    }

    private void initTravelPlan() {
        findCreatorInfo(travelPlan.getUserId());
        initView();
        setListener();
        setData();
        if (travelPlan.getStatus() == 0) {
            SUBMIT_TYPE = 0;
        } else if (travelPlan.getStatus() == 1) {
            if (MainActivity.USER_ID.equals(travelPlan.getUserId())) {
                SUBMIT_TYPE = 5;
                getParticipants();
                submit.setText("结束");
            } else {
                getParticipants();
            }
        } else if (travelPlan.getStatus() == 2 || travelPlan.getStatus() == 3) {
            SUBMIT_TYPE = 4;
            getParticipants();
            submit.setText("已取消或结束");
            submit.setEnabled(false);
        }
    }

    private void getParticipants() {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/getParticipants?travelPlanId=" + travelPlan.getTravelPlanId())
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        participants.clear();
                        scroll.removeAllViews();
                        participants.addAll(gson.fromJson(responseData, new com.google.gson.reflect.TypeToken<List<UserInfo>>() {}.getType()));
                        boolean isParticipant = participants.stream().anyMatch(participant -> participant.getUserId().equals(MainActivity.USER_ID));
                        displayCurrentParticipants();
                        if(SUBMIT_TYPE != 5){
                            if (isParticipant) {
                                SUBMIT_TYPE = 1;
                                runOnUiThread(()->{
                                    submit.setText("退出");
                                });
                            } else if (travelPlan.getCurrentParticipants() >= travelPlan.getMaxParticipants()) {
                                SUBMIT_TYPE = 3;
                                runOnUiThread(()->{
                                    submit.setText("已满员");
                                    submit.setEnabled(false);
                                });
                            } else {
                                SUBMIT_TYPE = 2;
                                runOnUiThread(()->{
                                    submit.setText("和TA一起");
                                });
                            }
                        }

                    }
                } else {
                    Log.e(TAG, "参与者查询失败：" + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void displayCurrentParticipants() {
        runOnUiThread(() -> {
            for (UserInfo participant : participants) {
                LinearLayout participantLayout = new LinearLayout(PlanDisplayActivity.this);
                participantLayout.setOrientation(LinearLayout.VERTICAL);
                participantLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                ImageView avatarImageView = new ImageView(PlanDisplayActivity.this);
                avatarImageView.setLayoutParams(new LinearLayout.LayoutParams(
                        Utils.dpToPx(40, PlanDisplayActivity.this),
                        Utils.dpToPx(40, PlanDisplayActivity.this)
                ));
                RequestOptions requestOptions = new RequestOptions()
                        .transform(new CircleCrop());
                Glide.with(this)
                        .load("http://" + MainActivity.IP + participant.getAvatarUrl())
                        .placeholder(R.drawable.headimg)
                        .apply(requestOptions)
                        .into(avatarImageView);
                TextView participantNameTextView = new TextView(PlanDisplayActivity.this);
                participantNameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        Utils.dpToPx(66, PlanDisplayActivity.this),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                // 设置控件在父控件中水平居中
                participantLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                participantNameTextView.setGravity(Gravity.CENTER);
                participantNameTextView.setText(participant.getUserName());
                participantLayout.addView(avatarImageView);
                participantLayout.addView(participantNameTextView);
                scroll.addView(participantLayout);
            }
        });
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
                    break;
                case 1:
                    status.setText("进行中");
                    break;
                case 2:
                    status.setText("已取消");
                    break;
                case 3:
                    status.setText("已结束");
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
            isMax();
            budget.setText(String.format("%.2f", travelPlan.getBudget()) + "（元）");
            address.setText(travelPlan.getAddress());

            // TODO 添加高德地图 展示大致位置
        }
    }

    private void setListener() {
        userItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserInfoActivity.class);
            intent.putExtra("userInfo", gson.toJson(creatorInfo));
            startActivity(intent);
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
        follow.setOnClickListener(v -> {
            if (creatorInfo.getUserId().equals(MainActivity.USER_ID)) {
                Toast.makeText(PlanDisplayActivity.this, "不能关注自己", Toast.LENGTH_SHORT).show();
            } else {
                int newRelationship;
                switch (creatorInfo.getRelationship()) {
                    case 0:
                        newRelationship = FollowUtils.updateFollow(creatorInfo, 1, follow,this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        creatorInfo.setRelationship(newRelationship);
                        break;
                    case 1:
                    case 2:
                        newRelationship = FollowUtils.updateFollow(creatorInfo, 0,follow,this);
                        Log.i(TAG, "relationship：" + newRelationship);
                        creatorInfo.setRelationship(newRelationship);
                        break;
                    default:
                        break;
                }
            }
        });
        submit.setOnClickListener(v -> {
            switch (SUBMIT_TYPE) {
                case 0: // 草稿
                    Intent intent = new Intent(PlanDisplayActivity.this, AddTravelPlanActivity.class);
                    intent.putExtra("travelPlan", gson.toJson(travelPlan));
                    startActivity(intent);
                    break;
                case 1: // 用户已参加
                    exitPlan();
                    break;
                case 2: // 用户未参加
                    addParticipants();
                    break;
                case 3: // 行程已满员 转跳至联系创建者
                    // TODO 联系创建者
                    break;
                case 4: // 已结束 不可点击 无需处理
                    break;
                case 5: // 创建者本身查看 可选择结束
                    finishPlan();
                    break;
            }
        });
    }

    private void addParticipants() {
        new Thread(()->{
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("travelPlanId", travelPlan.getTravelPlanId())
                    .addFormDataPart("userId", MainActivity.USER_ID)
                    .addFormDataPart("creatorId", travelPlan.getUserId())
                    .build();
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/addParticipants")
                    .post(requestBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        Log.i(TAG, "addParticipants: " + responseData);
                        runOnUiThread(() -> {
                            Toast.makeText(PlanDisplayActivity.this, "加入成功", Toast.LENGTH_SHORT).show();
                            getParticipants();
                            travelPlan.setCurrentParticipants(travelPlan.getCurrentParticipants() + 1);
                            isMax();
                        });
                    }
                } else {
                    Log.e(TAG, "加入失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void isMax() {
        currentParticipants.setText(String.valueOf(travelPlan.getCurrentParticipants()));
        currentParticipants.setTextColor(Color.parseColor("#FF03A9F4"));
        if (travelPlan.getMaxParticipants() > 0) {
            if (travelPlan.getCurrentParticipants() >= travelPlan.getMaxParticipants()) {
                currentParticipants.setTextColor(Color.parseColor("#FF0000"));
                maxParticipants.setText("/" + travelPlan.getMaxParticipants() + "  人数已满，请尝试联系发起人");
            } else {
                maxParticipants.setText("/" + travelPlan.getMaxParticipants());
            }
        } else {
            maxParticipants.setText("/无限制");
        }
    }

    private void exitPlan() {
        new Thread(()->{
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("travelPlanId", travelPlan.getTravelPlanId())
                    .addFormDataPart("userId", MainActivity.USER_ID)
                    .build();
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/exitPlan")
                    .post(requestBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        runOnUiThread(() -> {
                            Log.i(TAG, "exitPlan: " + responseData);
                            Toast.makeText(PlanDisplayActivity.this, "退出成功", Toast.LENGTH_SHORT).show();
                            getParticipants();
                            travelPlan.setCurrentParticipants(travelPlan.getCurrentParticipants() - 1);
                            isMax();
                        });
                    }
                } else {
                    Log.e(TAG, "退出失败");
                    runOnUiThread(() -> {
                        Toast.makeText(PlanDisplayActivity.this, "退出失败", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void finishPlan() {
        new Thread(()->{
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("travelPlanId", travelPlan.getTravelPlanId())
                    .build();
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/finishPlan")
                    .post(requestBody)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()) {
                        Log.i(TAG, "finishPlan: " + responseData);
                        runOnUiThread(() -> {
                            Toast.makeText(PlanDisplayActivity.this, "行程结束", Toast.LENGTH_SHORT).show();
                            submit.setText("行程已结束");
                            submit.setTextColor(Color.parseColor("#0c0c0c"));
                            submit.setEnabled(false);
                            // 设置背景
                            submit.setBackgroundResource(R.drawable.background_frame);
                        });
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
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
        scroll = findViewById(R.id.participants_container);
        userItem = findViewById(R.id.user_item);
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
}