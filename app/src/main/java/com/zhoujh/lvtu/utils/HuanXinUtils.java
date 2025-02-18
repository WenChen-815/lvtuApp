package com.zhoujh.lvtu.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.message.modle.UserConversation;
import com.zhoujh.lvtu.utils.modle.ClientPush;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HuanXinUtils {
    public static final String TAG = "HuanXinUtils";
    private static final Gson gson = MainActivity.gson;
    private static final OkHttpClient okHttpClient = new okhttp3.OkHttpClient();
    /**
     * 构建环信ID
     * 去除其中的“-”，并每四位字符为一组进行组内逆序
     * @param uuid 用户的 UUID
     * @return 构建得到的 环信ID
     */
    public static String createHXId(String uuid) {
        // 去除字符串中的 -
        String uuidWithoutDash = uuid.replace("-", "");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < uuidWithoutDash.length(); i += 4) {
            String group = uuidWithoutDash.substring(i, Math.min(i + 4, uuidWithoutDash.length()));
            // 对每一组进行逆序
            StringBuilder reversedGroup = new StringBuilder(group).reverse();
            result.append(reversedGroup);
        }
        return result.toString();
    }

    /**
     * 还原 UUID
     * @param processedId
     * @returnm 还原得到的 UUID
     */
    public static String restoreUUID(String processedId) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < processedId.length(); i += 4) {
            String group = processedId.substring(i, Math.min(i + 4, processedId.length()));
            // 对每一组进行逆序
            StringBuilder reversedGroup = new StringBuilder(group).reverse();
            result.append(reversedGroup);
        }
        // 在指定位置插入 -
        StringBuilder finalUUID = new StringBuilder(result);
        finalUUID.insert(8, '-');
        finalUUID.insert(13, '-');
        finalUUID.insert(18, '-');
        finalUUID.insert(23, '-');
        return finalUUID.toString();
    }

    public static void handleMutualFollow(String myUserId, String tagUserId){
        EMMessage message = EMMessage.createTextSendMessage("我们已经互相关注了，快来聊天吧！", createHXId(tagUserId));
        message.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                // 发送消息成功 通知服务器
                ClientPush clientPush = new ClientPush(myUserId,
                        MainActivity.user.getUserName(),
                        tagUserId,
                        MainActivity.user.getUserName(),
                        "我们已经互相关注了，快来聊天吧！",
                        null);
                sendMsgPush(clientPush, null);

                // 新建Conversation
                EMConversation conversation = EMClient.getInstance().chatManager().getConversation(createHXId(tagUserId));
                Log.i(TAG, "id "+ conversation.conversationId());
                UserConversation userConversation = new UserConversation();
                userConversation.setUserId(myUserId);
                userConversation.setConversationId(conversation.conversationId());
                userConversation.setConversationType(conversation.getType().name());
                List<String> members = new ArrayList<>();
                members.add(tagUserId);
                userConversation.setMembers(members);

                new Thread(() -> {
                    RequestBody requestBody = RequestBody.create(
                            gson.toJson(userConversation),
                            MediaType.parse("application/json; charset=utf-8")
                    );
                    Request request = new Request.Builder()
                            .url("http://" + MainActivity.IP + "/lvtu/conversation/createChat")
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
                }).start();
            }

            @Override
            public void onError(int code, String error) {
                // 发送消息失败
                Log.i(TAG, "发送消息失败");
            }

            @Override
            public void onProgress(int progress, String status) {

            }

        });
        // 发送消息
        EMClient.getInstance().chatManager().sendMessage(message);
    }
    public static boolean sendMsgPush(ClientPush clientPush, CountDownLatch latch){
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        new Thread(() -> {
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
                        isSuccess.set(true);
                    } else {
                        Log.e(TAG, "返回数据为null");
                    }
                } else {
                    Log.e(TAG, "请求失败 code:" + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (latch != null) {
                    latch.countDown();
                }
            }
        }).start();
        if (latch != null){
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isSuccess.get();
    }
}
