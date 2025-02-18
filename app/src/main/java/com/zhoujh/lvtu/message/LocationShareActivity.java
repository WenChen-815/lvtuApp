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
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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
import com.zhoujh.lvtu.utils.WebSocketClient;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.WebSocket;

public class LocationShareActivity extends AppCompatActivity {
    public static final String TAG = "LocationShareActivity";
    public final static int SINGLE_TYPE = 1;
    public final static int GROUP_TYPE = 2;
    private final Gson gson = MainActivity.gson;
    private List<UserInfo> userInfos;
    private UserInfo userInfo;
    private int type;
    private WebSocketClient ws;

    private MyLocationStyle myLocationStyle;
    private MapView mapView;
    private AMap aMap;
    private UiSettings mUiSettings;//定义一个UiSettings对象
    // 在类变量区添加
    private static final float MIN_DISPLACEMENT = 5; // 单位：米
    private LatLng lastSentPosition;
    private Map<String, Marker> userMarkers = new ConcurrentHashMap<>();


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
        type = intent.getIntExtra("type", 0);
        Log.i(TAG, "type: " + type);
        if (type == SINGLE_TYPE) {
            userInfo = gson.fromJson(intent.getStringExtra("userInfo"), UserInfo.class);
        } else if (type == GROUP_TYPE) {
            Type classType = TypeToken.getParameterized(List.class, UserInfo.class).getType();
            userInfos = gson.fromJson(intent.getStringExtra("userInfos"), classType);
        } else {
            Toast.makeText(this, "类型错误", Toast.LENGTH_SHORT).show();
        }

        // 高德
        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        initViews();
        setListener();
        initMap();
        initWs("ws://" + MainActivity.IP + "/lvtu/ws/location");

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

        aMap.setOnMyLocationChangeListener(location -> {
            // 获取当前位置
            // 获取当前位置的经纬度
            LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());
            // 计算距离上次位置变化
            if (lastSentPosition != null && AMapUtils.calculateLineDistance(lastSentPosition, newPosition) < MIN_DISPLACEMENT) {
                return;
            } else if (lastSentPosition == null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
            }
            lastSentPosition = newPosition;
            LocationMessage locationMessage = new LocationMessage(MainActivity.USER_ID + "#" + userInfo.getUserId(),
                    MainActivity.USER_ID,
                    LocationMessage.SINGLE_TYPE,
                    newPosition.longitude,
                    newPosition.latitude,
                    "");
            String message = gson.toJson(locationMessage);
            if (!message.isEmpty()) {
                ws.sendMessage(message);
            }
        });

        //* 控件是指浮在地图图面上的一系列用于操作地图的组件，例如缩放按钮、指南针、定位按钮、比例尺等。
        //* UiSettings 类用于操控这些控件，以定制自己想要的视图效果。UiSettings 类对象的实例化需要通过 AMap 类来实现
        mUiSettings = aMap.getUiSettings();//实例化UiSettings类对象

        // 获取当前城市名称

        //设置希望展示的地图缩放级别
        CameraUpdate mCameraUpdate = CameraUpdateFactory.zoomTo(17);
        aMap.moveCamera(mCameraUpdate);
    }

    private void setListener() {

    }

    private void initViews() {
    }

    private void initWs(String wsUrl) {
        // 初始化WebSocket客户端
        ws = new WebSocketClient();

        // 连接到WebSocket服务器
        ws.connect(wsUrl, new LocationShareListener());
    }

    private class LocationShareListener extends WebSocketClient.mWebSocketListener {
        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            runOnUiThread(() -> {
                // 不需要clear
                LocationMessage locationMessage = gson.fromJson(text, LocationMessage.class);
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
                    MarkerOptions markerOption = new MarkerOptions()
                            .position(new LatLng(locationMessage.getLatitude(), locationMessage.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_marker)))
                            .draggable(false)
                            .setFlat(false);
                    Marker marker = aMap.addMarker(markerOption);
                    userMarkers.put(userId, marker);
                }
            });
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
        // 断开WebSocket连接
        ws.close(1000, "断开连接");
    }
}