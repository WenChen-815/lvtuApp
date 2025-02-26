package com.zhoujh.lvtu.message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.modle.LocationMessage;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.WebSocketClient;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Response;
import okhttp3.WebSocket;

public class LocationShareActivity extends AppCompatActivity {
    public static final String TAG = "LocationShareActivity";
    public final static int SINGLE_TYPE = 1;
    public final static int GROUP_TYPE = 2;
    private String GROUP_ID;
    private int type;
    private final Gson gson = MainActivity.gson;
    private List<UserInfo> userInfoList;
    private MyLocationStyle myLocationStyle;
    private MapView mapView;
    private AMap aMap;
    private UiSettings mUiSettings;//定义一个UiSettings对象
    // 在类变量区添加
    private static final float MIN_DISPLACEMENT = 5; // 单位：米
    private LatLng lastSentPosition;
    private Map<String, Marker> userMarkers = new ConcurrentHashMap<>();
    private Map<String, UserInfo> userInfoMap = new ConcurrentHashMap<>();

    private Handler handler = new Handler();
    private Runnable sendLocationRunnable;
    private BroadcastReceiver locationUpdateReceiver;

    private Messenger serviceMessenger;
    private boolean isBound;
    private final Messenger activityMessenger = new Messenger(new IncomingHandler());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_share);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        Intent intent = getIntent();
        if (intent.hasExtra("userInfoList") && intent.hasExtra("groupId")) {
            Type classType = TypeToken.getParameterized(List.class, UserInfo.class).getType();
            userInfoList = gson.fromJson(intent.getStringExtra("userInfoList"), classType);
            userInfoList.forEach(userInfo -> userInfoMap.put(userInfo.getUserId(), userInfo));
            GROUP_ID = intent.getStringExtra("groupId");
            type = intent.getIntExtra("type", 0);
        } else {
            Toast.makeText(this, "类型错误", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 高德
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        initViews();
        setListener();
        initMap();

        // 启动服务
        Intent serviceIntent = new Intent(this, LocationShareService.class);
        serviceIntent.putExtra("groupId", GROUP_ID);
        serviceIntent.putExtra("type", type);
        serviceIntent.putExtra("userInfoList", gson.toJson(userInfoList));
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);

        IntentFilter filter = new IntentFilter(LocationShareService.ACTION_LOCATION_UPDATE);
        // 添加 RECEIVER_NOT_EXPORTED 标志
        registerReceiver(locationUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void initMap() {
        myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.strokeColor(Color.alpha(0));
        myLocationStyle.radiusFillColor(Color.alpha(0));
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        // 开启室内地图显示
        aMap.showIndoorMap(true);
//        aMap.setTrafficEnabled(true);//显示实时路况图层，aMap是地图控制器对象。

        // 设置希望展示的地图缩放级别
        CameraUpdate mCameraUpdate = CameraUpdateFactory.zoomTo(17);
        aMap.moveCamera(mCameraUpdate);
    }

    private void setListener() {

    }

    private void initViews() {
    }

    private void handleLocationMessage(String locationMessageJson) {
        LocationMessage locationMessage = gson.fromJson(locationMessageJson, LocationMessage.class);
        String userId = locationMessage.getUserId();
        LatLng newPosition = new LatLng(
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        );
        if (userMarkers.containsKey(userId)) {
            // 更新已有Marker位置
            Marker marker = userMarkers.get(userId);
//                    marker.setPosition(new LatLng(locationMessage.getLatitude(), locationMessage.getLongitude()));
            LatLng oldPosition = marker.getPosition(); // 先保存旧位置
            ValueAnimator animator = ValueAnimator.ofObject(
                    new LatLngEvaluator(),
                    oldPosition, // 使用旧位置作为起点
                    newPosition   // 新位置作为终点
            );
            animator.setDuration(1000);
            animator.addUpdateListener(va -> {
                LatLng animatedPos = (LatLng) va.getAnimatedValue();
                marker.setPosition(animatedPos);
            });

            // 动画结束校准
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    marker.setPosition(newPosition); // 确保最终位置准确
                }
            });

            animator.start();
        } else {
            // 创建新Marker
            UserInfo userInfo = userInfoMap.get(locationMessage.getUserId());
            Bitmap bitmap0 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_marker);
            // 从网络加载图片转为bitmap
            new Thread(() -> {
                try {
                    Bitmap bitmap1 = null;
                    bitmap1 = BitmapFactory.decodeStream(new URL("http://" + MainActivity.IP + userInfo.getAvatarUrl()).openStream());
                    Bitmap finalBitmap = Bitmap.createScaledBitmap(bitmap0, 48, 48, true);
                    if (bitmap1 != null) {
                        if (MainActivity.MANUFACTURER.equals("Xiaomi")) {
                            bitmap1 = Bitmap.createScaledBitmap(bitmap1, 96, 96, true);
                        } else if (MainActivity.MANUFACTURER.equals("HUAWEI")) {
                            bitmap1 = Bitmap.createScaledBitmap(bitmap1, 64, 64, true);
                        } else if (MainActivity.MANUFACTURER.equals("OPPO")) {
                            bitmap1 = Bitmap.createScaledBitmap(bitmap1, 64, 64, true);
                        } else if (MainActivity.MANUFACTURER.equals("vivo")) {
                            bitmap1 = Bitmap.createScaledBitmap(bitmap1, 64, 64, true);
                        } else {
                            bitmap1 = Bitmap.createScaledBitmap(bitmap1, 64, 64, true);
                        }
                        // 将bitmap1 裁剪为圆形
                        bitmap1 = Utils.getCircularBitmap(bitmap1);
                        finalBitmap = Utils.mergeBitmap(bitmap0, bitmap1, 0, -10);
                    }

                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(locationMessage.getLatitude(), locationMessage.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromBitmap(finalBitmap))
                            .draggable(false)
                            .setFlat(false);
                    Marker marker = aMap.addMarker(markerOption);
                    userMarkers.put(userId, marker);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 添加动画辅助类
    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            double lat = startValue.latitude + ((endValue.latitude - startValue.latitude) * fraction);
            double lng = startValue.longitude + ((endValue.longitude - startValue.longitude) * fraction);
            return new LatLng(lat, lng);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
            }
        // 销毁地图视图
        if (mapView != null) {
            mapView.onDestroy();
        }
        // 关闭服务
        Intent serviceIntent = new Intent(this, LocationShareService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 地图视图恢复
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 地图视图暂停
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 地图视图保存状态
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceMessenger = new Messenger(service);
            isBound = true;
            try {
                Message msg = Message.obtain(null, LocationShareService.MSG_REGISTER_CLIENT);
                msg.replyTo = activityMessenger;
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceMessenger = null;
            isBound = false;
        }
    };

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LocationShareService.MSG_LOCATION_UPDATE:
                    String locationMessageJson = msg.getData().getString("locationMessage");
                    Log.i("LocationShareActivity", "handleMessage: " + locationMessageJson);
                    handleLocationMessage(locationMessageJson);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}