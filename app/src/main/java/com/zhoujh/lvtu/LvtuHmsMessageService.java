package com.zhoujh.lvtu;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.zhoujh.lvtu.main.PlanDisplayActivity;
import com.zhoujh.lvtu.utils.NotificationUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LvtuHmsMessageService extends HmsMessageService {
    private static final String TAG = "LvtuHmsMessageService";
    private static boolean isSendToServer = false;
    private static String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", "");
//        EMOptions options = new EMOptions();
//        options.setAppKey("1121250105156229#demo");
//        options.setIncludeSendMessageInMessageListener(true);
//        EMClient.getInstance().init(this, options);
    }

    public static void setCurrentUserId(String currentUserId) {
        LvtuHmsMessageService.currentUserId = currentUserId;
    }

    @Override
    public void onNewToken(String token, Bundle bundle) {
        // 获取token
        Log.i(TAG, "have received refresh token: " + token);

        // 判断token是否为空
        if (!TextUtils.isEmpty(token)) {
            refreshedTokenToServer(token);
        }
    }

    public static void refreshedTokenToServer(String token) {
        if (!isSendToServer && !(MainActivity.USER_ID.equals("userId"))) {
            Log.i(TAG, "sending token to server.");
            // 上传华为推送 token
            EMClient.getInstance().sendHMSPushTokenToServer(token);
            new Thread(() -> {
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("userId", MainActivity.USER_ID)
                        .addFormDataPart("hmsToken", token)
                        .build();
                Request request = new Request.Builder()
                        .url("http://" + MainActivity.IP + "/lvtu/push/register")
                        .post(requestBody)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        if (!responseData.isEmpty()) {
                            Log.i(TAG, "refreshedTokenToServer: " + responseData);
                            isSendToServer = true;
                        } else {
                            Log.e(TAG, "返回数据为null");
                        }
                    } else {
                        Log.e(TAG, "请求失败");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.i(TAG, "onMessageReceived is called");

        // 判断消息是否为空
        if (message == null) {
            Log.e(TAG, "Received message entity is null!");
            return;
        }

        // 获取消息内容
        Log.i(TAG, "get Data: " + message.getData()
                + "\n getFrom: " + message.getFrom()
                + "\n getTo: " + message.getTo()
                + "\n getMessageId: " + message.getMessageId()
                + "\n getSentTime: " + message.getSentTime()
                + "\n getDataMap: " + message.getDataOfMap()
                + "\n getMessageType: " + message.getMessageType()
                + "\n getTtl: " + message.getTtl());
        Map<String, String> data = message.getDataOfMap();
        Log.i(TAG, "currentUserId: " + currentUserId);
        if (Objects.equals(data.get("tagUserId"), currentUserId)) {
            if (Objects.equals(data.get("messageTag"), "joinPlan")) {
                // 创建一个Intent，指定要启动的Activity
                Intent intent = new Intent(this, PlanDisplayActivity.class);
                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("travelPlanId", data.get("travelPlanId"));
                Log.i(TAG, "intent data: " + data.get("travelPlanId"));
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                // 在通知栏中显示通知
                NotificationUtils.showNotification(this, data.get("title"), data.get("body"), pendingIntent);
            } else if (Objects.equals(data.get("messageTag"), "follow")) {
                // 在通知栏中显示通知
                NotificationUtils.showNotification(this, data.get("title"), data.get("body"), null);
            } else if (Objects.equals(data.get("messageTag"), "IM")) {
                NotificationUtils.showNotification(this, data.get("title"), data.get("body"), null);
            }
        }
    }

    public static void setIsSendToServer(boolean isSendToServer) {
        LvtuHmsMessageService.isSendToServer = isSendToServer;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
