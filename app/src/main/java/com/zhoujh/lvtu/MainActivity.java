package com.zhoujh.lvtu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMError;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.exceptions.HyphenateException;
import com.zhoujh.lvtu.utils.LocalDateTimeAdapter;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.login.LoginActivity;
import com.zhoujh.lvtu.model.User;

import java.time.LocalDateTime;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    public static final String IP = "192.168.110.97:8080";
    public static String USER_ID = "userId";
    public static User user;
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private BottomNavigationView bottomNavigationView;
    private EMMessageListener msgListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                // 引导用户到设置页面授予权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
            } else {
                // 已经拥有权限
//                Toast.makeText(this, "已经拥有 WRITE_SETTINGS 权限", Toast.LENGTH_SHORT).show();
            }
        } else {
            // API 级别低于 23，不需要动态请求权限
//            Toast.makeText(this, "不需要动态请求 WRITE_SETTINGS 权限", Toast.LENGTH_SHORT).show();
        }

        // 沉浸式导航栏
        StatusBarUtils.setImmersiveStatusBar(this, getWindow().getDecorView(),StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        // 检查跳转附带的信息
        if (getIntent().getStringExtra("user") == null) {
            Toast.makeText(this, "请先登录！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else{
            Log.e(TAG, "user: " + getIntent().getStringExtra("user"));
            user = gson.fromJson(getIntent().getStringExtra("user"), User.class);
            USER_ID = user.getUserId();
            initView();
            initHuanXin();
        }
    }

    private void initHuanXin() {
        EMOptions options = new EMOptions();
        options.setAppKey("1121250105156229#demo");
        options.setIncludeSendMessageInMessageListener(true);
        // 其他 EMOptions 配置。
        // 初始化
        EMClient.getInstance().init(this, options);
        // 需要在 SDK 初始化后调用
        EMClient.getInstance().setDebugMode(false);

        EMConnectionListener connectionListener = new EMConnectionListener() {
            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected(int errorCode) {

            }

            @Override
            public void onLogout(int errorCode) {

            }

            @Override
            public void onTokenWillExpire() {

            }

            @Override
            public void onTokenExpired() {

            }

            // 连接成功，开始从服务器拉取离线消息时触发。
            // 注意：如果本次登录服务器没有离线消息，不会触发该回调。
            @Override
            public void onOfflineMessageSyncStart() {

            }

            // 离线用户上线后从服务器拉取离线消息结束时触发。
            // 注意：如果再拉取离线过程中因网络或其他原因导致连接断开，不会触发该回调。
            @Override
            public void onOfflineMessageSyncFinish() {

            }
        };
        // 注册连接状态监听
        EMClient.getInstance().addConnectionListener(connectionListener);
        // 尝试注册登录
        isHuanXinRegistered(user);
    }

    private void initView() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // 默认显示Home Fragment
        loadFragment(new MainFragment());
        // 设置 BottomNavigationView 的监听事件
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            switch (item.getItemId()) {
                case R.id.action_main:
                    fragment = new MainFragment();
                    break;
                case R.id.action_find:
                    fragment = new FindFragment();
                    break;
                case R.id.action_message:
                    fragment = new MessageFragment();
                    break;
                case R.id.action_my:
                    fragment = new PersonalFragment();
                    break;
            }
            return loadFragment(fragment);
        });
        bottomNavigationView.setItemIconTintList(null); //这行代码不可缺少，不然你的导航栏的item图片会很丑，这行代码，可以让你的图片正常展示,因为默认的menu有个很丑的类似于蒙层的感觉
//        bottomNavigationView.setItemTextColor(null); // 设置文字颜色
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            return true;
        }
        return false;
    }

    private void isHuanXinRegistered(User user) {
        new Thread(() -> {
            try {
                // 尝试使用提供的用户名和密码去注册
                String huanXinId = createHXId(user.getUserId());
                EMClient.getInstance().createAccount(huanXinId, "123123");
                // 如果没有抛出异常，说明注册成功
                Log.i(TAG, "环信注册成功");
                loginHuanXin();
            } catch (HyphenateException e) {
                if (e.getErrorCode() == EMError.USER_ALREADY_EXIST) {
                    // 如果捕获到的异常错误码表示用户已存在，说明账号已经注册过了
                    Log.i(TAG, "环信已注册");
                    loginHuanXin();
                } else{
                    // 其他异常情况
                    e.printStackTrace();
                    Log.e(TAG, "环信注册异常");
                }
            }
        }).start();
    }
    private void loginHuanXin(){
        // 登录
        EMClient.getInstance().login(createHXId(user.getUserId()), "123123", new EMCallBack() {
            // 登录成功回调
            @Override
            public void onSuccess() {
                Log.i(TAG, "环信登录成功");
                // 设置用户属性
                EMUserInfo userInfo = new EMUserInfo();
                userInfo.setUserId(EMClient.getInstance().getCurrentUser());
                userInfo.setNickname(user.getUserName());
                userInfo.setAvatarUrl(user.getAvatarUrl());
                userInfo.setBirth(user.getBirth());
                userInfo.setSignature("hello world");
                userInfo.setPhoneNumber(user.getPhoneNum());
                userInfo.setEmail(user.getEmail());
                userInfo.setGender(1);
                EMClient.getInstance().userInfoManager().updateOwnInfo(userInfo, new EMValueCallBack<String>() {
                    @Override
                    public void onSuccess(String value) {
                        Log.i(TAG, "信息更新成功");
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        Log.i(TAG, "信息更新失败");
                    }
                });
            }

            // 登录失败回调，包含错误信息
            @Override
            public void onError(int code, String error) {
                if (code == EMError.USER_ALREADY_LOGIN){
                    Log.i(TAG, "用户已登录");
                }else {
                    Log.i(TAG, "环信登录失败");
                }
            }
        });
    }

    /**
     * 构建环信ID
     * 去除其中的“-”，并每四位字符为一组进行组内逆序
     * @param uuid 用户的 UUID
     * @return 构建得到的 环信ID
     */
    private String createHXId(String uuid) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销消息监听
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        EMClient.getInstance().logout(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    // 用户授予权限
                    Toast.makeText(this, "用户授予权限", Toast.LENGTH_SHORT).show();
                } else {
                    // 用户拒绝权限
                    Toast.makeText(this, "用户拒绝权限", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}