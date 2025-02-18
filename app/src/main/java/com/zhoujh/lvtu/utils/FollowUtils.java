package com.zhoujh.lvtu.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FollowUtils {
    private static final String TAG = "FollowUtils";
    /**
     * 设置关注按钮的UI
     *
     * @param followStr  关注按钮的文字
     * @param textColor  文本颜色
     * @param drawableId 背景
     */
    public static void setFollowUI(String followStr, int textColor, int drawableId, Button follow, Context context) {
        Drawable drawable;
        follow.setText(followStr);
        follow.setTextColor(textColor);
        drawable = ContextCompat.getDrawable(context, drawableId);
        follow.setBackground(drawable);
    }

    /**
     * 更新关注关系并设置UI
     * @param creatorInfo     计划或帖子的发布者信息
     * @param newRelationship 用户希望变更的关系类型
     * @param follow          关注按钮
     * @param context         上下文
     *
     * @return 0：未关注，1：已关注，2：互关，3：拉黑用户
     */
    public static int updateFollow(UserInfo creatorInfo, int newRelationship, Button follow, Context context) {
        OkHttpClient client = new OkHttpClient();
        AtomicInteger responseData = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);
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
                    responseData.set(Integer.parseInt(response.body().string()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        thread.start();
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (responseData.get() == 0) {
            // 设置UI为未关注状态
            setFollowUI("+关注", Color.parseColor("#FFFFFF"), R.drawable.round_button_unfollowed_background, follow, context);
        } else if (responseData.get() == 1) {
            setFollowUI("已关注", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, context);
        } else if (responseData.get() == 2) {
            setFollowUI("互关", Color.parseColor("#181A23"), R.drawable.round_button_followed_background, follow, context);
            HuanXinUtils.handleMutualFollow(MainActivity.USER_ID, creatorInfo.getUserId());
        } else if (responseData.get() == 3) {
            Log.e(TAG, "获取到拉黑用户信息！");
        }
        return responseData.get();
    }
}
