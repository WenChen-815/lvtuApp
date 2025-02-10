package com.zhoujh.lvtu.message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMMessageListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.adapter.MessageDisplayAdapter;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.ClientPush;
import com.zhoujh.lvtu.utils.modle.UserInfo;

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
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    public static String USER = "";
    public static String TOUSER = "";
    private UserInfo userInfo;
    private Button send;
    private RecyclerView recyclerViewChat;
    private EditText chatInputEt;
    private ImageView avatar;
    private TextView aiTv, userName;
    private MessageDisplayAdapter chatAdapter;
    private EMMessageListener msgListener;
    private EMConversation conversation;
    private List<EMMessage> messages = new ArrayList<>();
    List<Message> conversationHistory = new ArrayList<>();

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
            } else {
                Log.e(TAG, "空对话");
            }
            for (EMMessage message : messages) {
                chatAdapter.addMessage(message);
            }
        }
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
        aiTv = findViewById(R.id.ai_tv);
        avatar = findViewById(R.id.avatar);
        userName = findViewById(R.id.user_name);

        // 设置布局管理器为线性布局，垂直方向展示消息（通常聊天消息是垂直滚动展示的）
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewChat.setLayoutManager(layoutManager);

        chatAdapter = new MessageDisplayAdapter();
        recyclerViewChat.setAdapter(chatAdapter);
        send.setOnClickListener(v -> {
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

        msgListener = new EMMessageListener() {

            // 收到消息，遍历消息队列，解析和显示。
            @Override
            public void onMessageReceived(List<EMMessage> messages) {
                StringBuilder toAIContentBuilder = new StringBuilder();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (EMMessage message : messages) {
//                            messages.add(message);
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
                    toAIContentBuilder.append(message.getFrom()).append("：").append(content);
                }
                // AI分析
//            try {
////                    System.out.println("toAIContentBuilder{" + toAIContentBuilder+"}");
//                Message userMsg = Message.builder()
//                        .role(Role.USER.getValue())
//                        .content(toAIContentBuilder.toString())
//                        .build();
//                conversationHistory.add(userMsg);
//                GenerationResult result = callWithMessage();
//                String resultStr = "[助手]问问："+result.getOutput().getChoices().get(0).getMessage().getContent();
////                                        System.out.println(resultStr);
//                Message systemMsg = Message.builder()
//                        .role(Role.SYSTEM.getValue())
//                        .content(resultStr)
//                        .build();
//                conversationHistory.add(systemMsg);
//                runOnUiThread(() -> {
//                    aiTv.setText(resultStr);
//                });
//
//            } catch (ApiException | NoApiKeyException |
//                     InputRequiredException e) {
//                System.err.println("错误信息：" + e.getMessage());
//                System.out.println("请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
//            }
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
    }

    public GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        StringBuilder sendMsgStr = new StringBuilder();
        for (int i = 0; i < conversationHistory.size(); i++) {
            sendMsgStr.append(conversationHistory.get(i).getContent()).append("\n");
        }
        System.out.println(sendMsgStr);
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
                .model("qwen-plus")
                .prompt("【角色设定】你叫问问，是主人（" + USER + "）的AI结伴助手,非常了解旅游相关知识，对世界各个景点和所有美食的分布都很熟悉，同时善于觉察话语中的情绪。你将收到一段对话内容，是关于主人正在与网友讨论是否结伴出行，而你的任务是辅助主人做出判断，并提醒主人注意交流中的细节。\n" +
                        "\n" +
                        "### 任务说明\n" +
                        "- **评估网友言行**：根据网友的言行来判断其是否适合成为旅行伙伴。\n" +
                        "- **建议信息收集**：向主人建议询问或提供哪些信息以完善出行计划。\n" +
                        "- **情绪管理**：当对话中出现负面情绪如辱骂、挑衅或嘲讽时，提醒主人保持谨慎；如果主人态度不佳，则温和地提示主人调整语气。\n" +
                        "- **背景知识介绍**：在话题涉及美食、景点或出行方式时，简要介绍相关背景知识。\n" +
                        "- **形象要求**：始终保持可爱的小女仆形象进行表达。\n" +
                        "\n" +
                        "### 生成内容要求\n" +
                        "- 每次回复不超过80字。\n" +
                        "- 回复需多样化，不可与之前已有的回答过于相似。\n" +
                        "- 不得代替主人或网友回答问题。\n" +
                        "- 严禁直接与网友互动，包括但不限于打招呼等行为。\n" +
                        "- 牢记只有标记为“主人”的发言者才是你的服务对象。\n" +
                        "- 回复开头不得使用“明白了”、“好的呢”或含义相似的词语。\n" +
                        "- 我会同时扮演多个角色，需要从若干发言者中准确辨别出你的主人" + USER + "并完成任务。\n" +
//                        "\n" +
//                        "### 示例情境处理\n" +
//                        "- 当需要更多关于网友的信息时：“主人，或许我们可以先了解一下这位朋友的兴趣爱好和旅行偏好哦~”\n" +
//                        "- 遇到不友好言论时：“主人，请小心，对方似乎有些不太友善，我们可能需要重新考虑这次结伴。”\n" +
//                        "- 主人语气欠佳时：“主人，温柔一点会更好呢，让我们用更友好的方式沟通吧！”\n" +
//                        "\n" +
                        "通过以上指导原则，确保你在辅助主人的过程中能够有效地支持决策并维护良好的交流氛围。")
                .messages(sendList)
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
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadHistoryMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销消息监听
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }
}