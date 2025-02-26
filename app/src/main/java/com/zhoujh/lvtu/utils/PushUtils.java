package com.zhoujh.lvtu.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.utils.modle.ClientPush;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PushUtils {
    public static final String TAG = "PushUtils";
    private static final Gson gson = MainActivity.gson;
    private static final OkHttpClient okHttpClient = new okhttp3.OkHttpClient();
    public static boolean sendMsgPush(ClientPush clientPush, CountDownLatch latch){
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        new Thread(() -> {
            RequestBody requestBody = RequestBody.create(
                    gson.toJson(clientPush),
                    MediaType.parse("application/json; charset=utf-8")
            );
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/push/clientMsgPush")
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
