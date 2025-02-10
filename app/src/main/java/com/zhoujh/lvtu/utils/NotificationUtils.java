package com.zhoujh.lvtu.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.main.PlanDisplayActivity;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    public static final String CHANNEL_ID = "lvtu";

    public static void showNotification(Context context, String title, String message, PendingIntent pendingIntent) {
        // 创建通知渠道
        createNotificationChannel(context);

        // 构建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
                .setSmallIcon(R.drawable.tutu24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // 点击通知后自动取消通知
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent); // 设置点击通知时的PendingIntent
        }

        // 获取通知管理器
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 显示通知
        int notificationId = 1;
        notificationManager.notify(notificationId, builder.build());
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.i(TAG, "createNotificationChannel");
            CharSequence name = "Lvtu Channel";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
