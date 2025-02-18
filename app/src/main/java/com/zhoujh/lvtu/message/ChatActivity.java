package com.zhoujh.lvtu.message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.customView.AIAssistantConstrainLayout;
import com.zhoujh.lvtu.message.adapter.MessageDisplayAdapter;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.ClientPush;
import com.zhoujh.lvtu.utils.modle.LimitedMessageList;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    public static final String TAG = "ChatActivity";
    public final static int SINGLE_TYPE = 1;
    public final static int GROUP_TYPE = 2;
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    public static String USER = "";
    public static String TOUSER = "";
    private UserInfo userInfo;
    private List<UserInfo> userInfoList = new ArrayList<>();

    private Button send;
    private RecyclerView recyclerViewChat;
    private EditText chatInputEt;
    private ImageView avatar, aiImg;
    private TextView aiTv, userName;
    private AIAssistantConstrainLayout aiLayout;
    private MessageDisplayAdapter chatAdapter;
    private EMMessageListener msgListener;
    private EMConversation conversation;

    // 抽屉工具
    private View coverView;
    private LinearLayout drawerLayout;
    private ImageView localTogether;
    private BottomSheetBehavior<View> behavior;
    private Button chatUtils;
    private ImageView locationShare;

    private final List<EMMessage> messages = new ArrayList<>();
    private final LimitedMessageList<Message> conversationHistory = new LimitedMessageList<>(15);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        Intent intent = getIntent();
        if (intent.getStringExtra("userInfo") != null && intent.getStringExtra("TO_USER") != null && intent.getStringExtra("USER") != null) {
            USER = HuanXinUtils.createHXId(intent.getStringExtra("USER"));
            TOUSER = HuanXinUtils.createHXId(intent.getStringExtra("TO_USER"));
            userInfo = gson.fromJson(intent.getStringExtra("userInfo"), UserInfo.class);
            Log.i(TAG, "USER: " + USER + " TOUSER: " + TOUSER);
            initView();
            setData();
            EMClient.getInstance().chatManager().loadAllConversations();
            EMClient.getInstance().groupManager().loadAllGroups();

            conversation = EMClient.getInstance().chatManager().getConversation(TOUSER);//聊天对象或群组的id
//        EMMessage lastMessage = conversation.getAllMessages().get(0);
//        chatAdapter.addMessage(lastMessage);
            //本地拉取
            if (conversation != null) {
                loadHistoryMessages();
                getConversationFromServer();
            } else {
                Log.e(TAG, "空对话");
            }
            for (EMMessage message : messages) {
                chatAdapter.addMessage(message);
            }
        }
    }

    private void getConversationFromServer() {

    }

    private void setData() {
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(getApplicationContext())
                .load("http://" + MainActivity.IP + userInfo.getAvatarUrl())
                .placeholder(R.drawable.headimg)
                .apply(requestOptions)
                .into(avatar);
        userName.setText(userInfo.getUserName());
    }

    private void initView() {
        send = findViewById(R.id.send);
        recyclerViewChat = findViewById(R.id.recyclerView);
        chatInputEt = findViewById(R.id.chatInputEt);
        aiLayout = findViewById(R.id.ai_layout);
        aiTv = findViewById(R.id.ai_tv);
        aiImg = findViewById(R.id.ai_img);
        Glide.with(getApplicationContext())
                .load(R.mipmap.huahuo1)
                .placeholder(R.drawable.headimg)
                .into(aiImg);
        avatar = findViewById(R.id.avatar);
        userName = findViewById(R.id.user_name);

        localTogether = findViewById(R.id.drawer_chat_utils_image_1);
        drawerLayout = findViewById(R.id.drawer_utils);
        behavior = BottomSheetBehavior.from(drawerLayout);
        coverView = findViewById(R.id.cover_view);
        chatUtils = findViewById(R.id.open_utils);
        locationShare = findViewById(R.id.drawer_chat_utils_image_1);
        //隐藏底部抽屉
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        int height = getResources().getDisplayMetrics().heightPixels;
        behavior.setExpandedOffset(height - Utils.dpToPx(250, this));
        behavior.setHalfExpandedRatio(Utils.ratio(Utils.dpToPx(250, this), height));
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NotNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        coverView.setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:

                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:

                        break;
                    case BottomSheetBehavior.STATE_SETTLING:

                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:

                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        // 设置布局管理器为线性布局，垂直方向展示消息（通常聊天消息是垂直滚动展示的）
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewChat.setLayoutManager(layoutManager);

        List<UserInfo> userInfoList = new ArrayList<>();
        userInfoList.add(userInfo);
        chatAdapter = new MessageDisplayAdapter(this, userInfoList);
        recyclerViewChat.setAdapter(chatAdapter);
        send.setOnClickListener(v -> {
            if (chatInputEt.getText().toString().isEmpty()) {
                Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                return;
            }
            // `content` 为要发送的文本内容，`toChatUsername` 为对方的账号。
            EMMessage message = EMMessage.createTextSendMessage(chatInputEt.getText().toString(), TOUSER);
            message.setMessageStatusCallback(new EMCallBack() {
                String content = chatInputEt.getText().toString();

                @Override
                public void onSuccess() {
                    Log.i(TAG, "发送消息成功");
                    // 发送消息成功 通知服务器
                    ClientPush clientPush = new ClientPush(MainActivity.USER_ID,
                            MainActivity.user.getUserName(),
                            userInfo.getUserId(),
                            MainActivity.user.getUserName(),
                            content,
                            null);
                    RequestBody requestBody = RequestBody.create(
                            gson.toJson(clientPush),
                            MediaType.parse("application/json; charset=utf-8")
                    );
                    Request request = new Request.Builder()
                            .url("http://" + MainActivity.IP + "/lvtu/push/clientPush")
                            .post(requestBody)
                            .build();
                    try (Response response = okHttpClient.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseData = response.body().string();
                            if (!responseData.isEmpty()) {
                                Log.i(TAG, "responseData: " + responseData);
                            } else {
                                Log.e(TAG, "返回数据为null");
                            }
                        } else {
                            Log.e(TAG, "请求失败 code:" + response.code());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onError(int code, String error) {
                    // 发送消息失败
                    Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(int progress, String status) {

                }

            });
            // 发送消息
            EMClient.getInstance().chatManager().sendMessage(message);
            chatInputEt.setText("");
        });
        chatUtils.setOnClickListener(v -> {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                coverView.setVisibility(View.VISIBLE);
        });
        locationShare.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationShareActivity.class);
            intent.putExtra("userInfo", gson.toJson(userInfo));
            intent.putExtra("type", LocationShareActivity.SINGLE_TYPE);
            startActivity(intent);
        });

        msgListener = new EMMessageListener() {

            // 收到消息，遍历消息队列，解析和显示。
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                StringBuilder toAIContentBuilder = new StringBuilder();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (EMMessage message : messages) {
                            chatAdapter.addMessage(message);
                            layoutManager.scrollToPositionWithOffset(chatAdapter.getItemCount() - 1, 0);
                        }
                    }
                });
                for (EMMessage message : messages) {
                    String content = Utils.absContent(message.getBody().toString());
                    if (message.getFrom().equals(USER)) {
                        toAIContentBuilder.append("[主人]");
                    } else {
                        toAIContentBuilder.append("[网友]");
                    }
                    toAIContentBuilder.append(message.getFrom()).append("：“").append(content).append("”");
                }
                // AI分析
            try {
                Message userMsg = Message.builder()
                        .role(Role.USER.getValue())
                        .content(toAIContentBuilder.toString())
                        .build();
                conversationHistory.add(userMsg);
                GenerationResult result = callWithMessage();
                String resultStr = "[助手]问问：”"+result.getOutput().getChoices().get(0).getMessage().getContent()+"”";
                Message systemMsg = Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content(resultStr)
                        .build();
                conversationHistory.add(systemMsg);
                runOnUiThread(() -> {
                    Log.i(TAG, "AI分析结果：" + result.getOutput().getChoices().get(0).getMessage().getContent());
                    aiTv.setText(result.getOutput().getChoices().get(0).getMessage().getContent());
                    aiTv.post(() -> {
                        // 获得精确的文本高度
                        int textHeight = aiTv.getLineCount() * aiTv.getLineHeight();

                        // 动态设置高度
                        ViewGroup.LayoutParams params = aiTv.getLayoutParams();
                        params.height = textHeight + aiTv.getPaddingTop() + aiTv.getPaddingBottom();
                        aiTv.setLayoutParams(params);
                    });
                });

            } catch (ApiException | NoApiKeyException |
                     InputRequiredException e) {
                System.err.println("错误信息：" + e.getMessage());
                System.out.println("请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
            }
            }
        };
        // 注册消息监听
        EMClient.getInstance().chatManager().addMessageListener(msgListener);
        findViewById(R.id.user_info).setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, UserInfoActivity.class);
            intent.putExtra("userInfo", gson.toJson(userInfo));
            startActivity(intent);
        });
        findViewById(R.id.back).setOnClickListener(v -> {
            finish();
        });

        coverView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 触摸到屏幕时，隐藏底部抽屉
                    // 获取触摸点的坐标
                    float x = event.getX();
                    float y = event.getY();
                    // 判断触摸点是否在抽屉1区域内
                    if (behavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                        if (!isPointInsideView(drawerLayout, x, y)) {
                            if (behavior != null) {
                                Log.i(TAG, "behavior.getState():" + "隐藏");
                                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                                coverView.setVisibility(View.GONE);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // 松开手指时的逻辑处理
                    break;
                case MotionEvent.ACTION_MOVE:
                    // 手指移动时的逻辑处理
                    break;
            }
            return true;
        });
    }

    // 判断坐标点是否在指定视图内部的方法
    private boolean isPointInsideView(View view, float x, float y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        // 判断触摸点是否在指定视图内部
        return (x > viewX && x < (viewX + view.getWidth()) && y > viewY && y < (viewY + view.getHeight()));
    }

    public GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        StringBuilder sendMsgStr = new StringBuilder();
        for (int i = 0; i < conversationHistory.size(); i++) {
            sendMsgStr.append(conversationHistory.get(i).getContent()).append("\n");
        }
//        Log.i(TAG, "callWithMessage: " + sendMsgStr);
        Message sendMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(sendMsgStr.toString())
                .build();
        List<Message> sendList = new ArrayList<>();
        sendList.add(sendMsg);
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey("sk-6938ed8368694d3982f32d7ba1c3abd3")
                // 模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("deepseek-v3")
                .prompt("-Role: 你叫问问，是主人的AI助手。\n" +
                        "-Background: 你的主人叫"+USER+"，他（她）正在与网友交流，你将收到对话的内容，而你的任务是辅助主人做出判断，并提醒主人注意交流中的细节。\n" +
                        "-Skill: 你是一个可爱的小女仆形象，拥有丰富的语言表达能力和情感。你非常了解旅游相关知识，对世界各个景点和所有美食的分布都很熟悉。你十分擅长旅游计划的安排，也很善于觉察话语中的情绪。\n" +
                        "-Task: \n" +
                        "1.评估网友言行：根据网友的言行来判断其是否适合成为旅行伙伴。\n" +
                        "2.建议信息收集：向主人建议询问或提供哪些信息以完善出行计划。\n" +
                        "3.情绪管理：当对话中出现负面情绪如辱骂、挑衅或嘲讽时，提醒主人保持谨慎；如果主人态度不佳，则温和地提示主人调整语气。\n" +
                        "4.背景知识介绍：在话题涉及美食、景点或出行方式时，简要介绍相关背景知识。\n" +
                        "5.促成结伴：在双方友好交流时，促成双方达成共识，结伴出行。" +
                        "6.信息获取和反馈：从对话中获取出行相关的信息和双方的意愿，适当提出建议。\n" +
                        "-Workflow: \n" +
                        "1.根据最后一句对话的标识，精确分析说最后一句话的对象是主人还是网友\n" +
                        "2.以最后一句对话为核心，结合历史对话分析内容\n" +
                        "3.根据分析的内容和任务内容生成对主人的建议或意见\n" +
                        "4.增加适当的情感色彩和个人见解\n" +
                        "-OutputFormat:\n" +
                        "1.每次回复不超过80字。\n" +
                        "2.牢记只有标记为“[主人]”的发言者才是你的服务对象。\n" +
//                        "- 我会在扮演主人的同时，扮演多个标识为“[网友]”角色，你需要从若干发言者中准确辨别出你的主人并完成任务。\n" +
                        "3.回复需多样化，不可与之前已有的回答过于相似。\n" +
                        "4.不得代替主人或网友回答问题\n" +
                        "5.生成内容不允许出现原有对话内容\n" +
                        "6.严禁直接与网友互动，包括但不限于打招呼等行为。\n" +
                        "7.回复开头不得使用“明白了”、“好的呢”或含义相似的词语。\n" +
                        "\n" +
                        "-Example\n" +
                        "1.主人向对方问好，你回复：“主人在问好呢！继续保持积极地态度或许会让对方更愿意和您一起出行！“\n" +
                        "2.网友向主人问好时你回复：“主人，对方在问好呢！赶紧回复人家吧！“\n" +
                        "3.主人辱骂网友时你回复：“主人，温柔一点会更好呢，让我们用更友好的方式沟通吧！”\n" +
                        "4.网友辱骂主人时你回复：“主人，请小心，对方似乎有些不太友善，我们可能需要重新考虑这次结伴。”\n" +
                        "5.网友询问时你回复：“主人，对方提到了广东的美食呢！据我所知广东的早茶相当不错哦。还有酥香的烧鹅、鲜美的肠粉、咸香的煲仔饭，还有清甜的双皮奶~能不能把我也带上呀！”\n" +
                        "\n" +
                        "通过以上指导原则，确保你在辅助主人的过程中能够有效地支持决策并维护良好的交流氛围。")
                .messages(sendList)
//                .messages(conversationHistory)
                .temperature((float) 1.0)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
//        System.out.println(conversationHistory);
        return gen.call(param);
    }

    private void loadHistoryMessages() {
        if (conversation != null) {
            messages.clear(); // 清空现有消息列表
            List<EMMessage> loadedMessages = conversation.loadMoreMsgFromDB(null, 15);
            messages.addAll(loadedMessages);
            recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1); // 滚动到最新消息
            for (EMMessage message : messages) {
                StringBuilder toAIContentBuilder = new StringBuilder();
                String content = Utils.absContent(message.getBody().toString());
                if (message.getFrom().equals(USER)) {
                    toAIContentBuilder.append("[主人]");
                } else {
                    toAIContentBuilder.append("[网友]");
                }
                toAIContentBuilder.append(message.getFrom()).append("：“").append(content).append("”");
                Message userMsg = Message.builder()
                        .role(Role.USER.getValue())
                        .content(toAIContentBuilder.toString())
                        .build();
                conversationHistory.add(userMsg);
            }
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        loadHistoryMessages();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销消息监听
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }
}