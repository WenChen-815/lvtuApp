package com.zhoujh.lvtu.message;

import static com.zhoujh.lvtu.message.LocationShareActivity.GROUP_TYPE;
import static com.zhoujh.lvtu.message.LocationShareActivity.SINGLE_TYPE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.message.modle.LocationMessage;
import com.zhoujh.lvtu.utils.WebSocketClient;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Response;
import okhttp3.WebSocket;

public class LocationShareService extends Service implements AMapLocationListener {
    public static final String TAG = "LocationShareService";
    public static final String ACTION_LOCATION_UPDATE = "com.zhoujh.lvtu.ACTION_LOCATION_UPDATE";
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_LOCATION_UPDATE = 2;
    private static final String CHANNEL_ID = "LocationShareChannel";
    private static final int NOTIFICATION_ID = 1;
    private WebSocketClient ws;
    private AMapLocationClient locationClient;
    private Gson gson;
    private String GROUP_ID;
    private int type;
    private LatLng lastSentPosition;
    private static final float MIN_DISPLACEMENT = 5; // 单位：米
    private Map<String, Marker> userMarkers = new ConcurrentHashMap<>();
    private Map<String, UserInfo> userInfoMap = new ConcurrentHashMap<>();
    private Handler handler = new Handler();
    private Runnable sendLocationRunnable;

    private Messenger activityMessenger;
    private final Messenger serviceMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();
        gson = MainActivity.gson;
        // 初始化 WebSocket 连接
        initWs("ws://" + MainActivity.IP + "/lvtu/ws/location");
        // 初始化定位客户端
        try {
            initLocationClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (intent != null) {
            GROUP_ID = intent.getStringExtra("groupId");
            type = intent.getIntExtra("type", 0);
            Type classType = TypeToken.getParameterized(List.class, UserInfo.class).getType();
            List<UserInfo> userInfoList = gson.fromJson(intent.getStringExtra("userInfoList"), classType);
            if (userInfoList != null) {
                for (UserInfo userInfo : userInfoList) {
                    userInfoMap.put(userInfo.getUserId(), userInfo);
                }
            }
        }
        // 启动定位
        startLocation();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止定位
        stopLocation();
        // 断开 WebSocket 连接
        if (ws != null) {
            ws.close(1000, "断开连接");
        }
    }

    //    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }

    private void initWs(String wsUrl) {
        // 初始化 WebSocket 客户端
        ws = new WebSocketClient();
        // 连接到 WebSocket 服务器
        ws.connect(wsUrl, new WebSocketClient.mWebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                super.onOpen(webSocket, response);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                super.onMessage(webSocket, text);
                Log.i(TAG, "收到消息：" + text);
                // 接收到 WebSocket 消息，通过 Messenger 发送消息
                if (activityMessenger != null) {
                    Message msg = Message.obtain(null, MSG_LOCATION_UPDATE);
                    Bundle data = new Bundle();
                    data.putString("locationMessage", text);
                    msg.setData(data);
                    try {
                        activityMessenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void initLocationClient() throws Exception {
        Log.i(TAG, "初始化定位客户端");
        locationClient = new AMapLocationClient(this);
        AMapLocationClientOption option = new AMapLocationClientOption();
        option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        option.setInterval(1000); // 定位间隔 3 秒
        locationClient.setLocationOption(option);
        locationClient.setLocationListener(this);
    }

    private void startLocation() {
        if (locationClient != null) {
            Log.i(TAG, "启动定位");
            locationClient.startLocation();
        }
    }

    private void stopLocation() {
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            LatLng newPosition = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            // 计算距离上次位置变化
            if (lastSentPosition != null && AMapUtils.calculateLineDistance(lastSentPosition, newPosition) < MIN_DISPLACEMENT) {
                return;
            }
            lastSentPosition = newPosition;
            LocationMessage locationMessage = new LocationMessage(GROUP_ID,
                    MainActivity.USER_ID,
                    type == SINGLE_TYPE ? SINGLE_TYPE : GROUP_TYPE,
                    newPosition.longitude,
                    newPosition.latitude,
                    "");
            String message = gson.toJson(locationMessage);
            if (!message.isEmpty() && ws != null) {
                ws.sendMessage(message);
            }
        }
    }

    private Notification createNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Share", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("位置共享")
                .setContentText("正在共享位置信息")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    // 处理客户端注册的逻辑
                    activityMessenger = msg.replyTo;
                    break;
                case MSG_LOCATION_UPDATE:
                    // 处理位置更新消息的逻辑
                    String locationMessageJson = msg.getData().getString("locationMessage");
                    // 这里可以添加处理位置消息的代码，比如发送广播等
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}