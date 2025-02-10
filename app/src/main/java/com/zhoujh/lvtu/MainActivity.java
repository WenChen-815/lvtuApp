package com.zhoujh.lvtu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessaging;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMContactListener;
import com.hyphenate.EMError;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.chat.EMUserInfo;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.push.EMPushConfig;
import com.zhoujh.lvtu.find.FindFragment;
import com.zhoujh.lvtu.main.MainFragment;
import com.zhoujh.lvtu.message.MessageFragment;
import com.zhoujh.lvtu.personal.PersonalFragment;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.adapter.LocalDateTimeAdapter;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.login.LoginActivity;
import com.zhoujh.lvtu.utils.modle.User;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    public static final String IP = "192.168.110.96:8080";
    public static String USER_ID = "userId";
    public static final int PERMISSION_REQUEST_CODE = 123; // 定义一个请求码，用于识别权限请求
    public static User user;
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private BottomNavigationView bottomNavigationView;
    public static EMMessageListener msgListener;

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
        checkPermissions();

        // HMS 推送自动初始化
        setAutoInitEnabled(true);

        // 沉浸式导航栏
        StatusBarUtils.setImmersiveStatusBar(this, getWindow().getDecorView(), StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        // 从 SharedPreferences 中读取账号和密码
        SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        String savedUsername = sharedPreferences.getString(LoginActivity.KEY_PHONE_NUM, null);
        String savedPassword = sharedPreferences.getString(LoginActivity.KEY_PASSWORD, null);
        if (getIntent().getStringExtra("user") != null) {
            Log.e(TAG, "user: " + getIntent().getStringExtra("user"));
            user = gson.fromJson(getIntent().getStringExtra("user"), User.class);
            USER_ID = user.getUserId();

            initView();
            initHuanXin();
        } else if (savedUsername != null && savedPassword != null) {
            // 如果已保存账号和密码，直接使用这些信息进行登录
            autoLogin(savedUsername, savedPassword);
        } else {
            Toast.makeText(this, "请先登录！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void getToken() {
        Log.i(TAG, "get token");
        // 创建一个新线程
        new Thread(() -> {
            try {
                // 从agconnect-services.json文件中读取APP_ID
                String appId = "113389873";

                // 输入token标识"HCM"
                String tokenScope = "HCM";
                String token = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, tokenScope);
                Log.i(TAG, "get token success");

                // 判断token是否为空
                if (!TextUtils.isEmpty(token)) {
                    LvtuHmsMessageService.refreshedTokenToServer(token);
                }
            } catch (ApiException e) {
                Log.e(TAG, "get token failed, " + e);
            }
        }).start();
    }

    private void setAutoInitEnabled(final boolean isEnable) {
        if (isEnable) {
            // 设置自动初始化
            HmsMessaging.getInstance(this).setAutoInitEnabled(true);
        } else {
            // 禁止自动初始化
            HmsMessaging.getInstance(this).setAutoInitEnabled(false);
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                // 引导用户到设置页面授予权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 100);
            } else {
                // 已经拥有权限
            }
        } else {
            // API 级别低于 23，不需要动态请求权限
        }
    }

    private void initHuanXin() {
        EMOptions options = new EMOptions();
        EMPushConfig.Builder builder = new EMPushConfig.Builder(this);
        builder.enableHWPush();
        // 将 pushconfig 设置为 ChatOptions
        options.setPushConfig(builder.build());
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
        // 添加好友监听
        EMClient.getInstance().contactManager().setContactListener(new EMContactListener() {
            // 对方同意了好友请求。
            @Override
            public void onFriendRequestAccepted(String username) {
            }

            // 对方拒绝了好友请求。
            @Override
            public void onFriendRequestDeclined(String username) {
            }

            // 接收到好友请求。
            @Override
            public void onContactInvited(String username, String reason) {
                Log.i(TAG, "收到好友请求");
                runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "收到好友请求", Toast.LENGTH_SHORT).show();
                        }
                );
//                        try {
//                            EMClient.getInstance().contactManager().acceptInvitation(username);
//                            Log.i(TAG, "同意好友请求");
//                        } catch (HyphenateException e) {
//                            throw new RuntimeException(e);
//                        }
            }

            // 联系人被删除。
            @Override
            public void onContactDeleted(String username) {
            }

            // 联系人已添加。
            @Override
            public void onContactAdded(String username) {
            }
        });
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
                String huanXinId = HuanXinUtils.createHXId(user.getUserId());
                EMClient.getInstance().createAccount(huanXinId, "123123");
                // 如果没有抛出异常，说明注册成功
                Log.i(TAG, "环信注册成功");
                loginHuanXin();
            } catch (HyphenateException e) {
                if (e.getErrorCode() == EMError.USER_ALREADY_EXIST) {
                    // 如果捕获到的异常错误码表示用户已存在，说明账号已经注册过了
                    Log.i(TAG, "环信已注册");
                    loginHuanXin();
                } else {
                    // 其他异常情况
                    e.printStackTrace();
                    Log.e(TAG, "环信注册异常");
                }
            }
        }).start();
    }

    private void autoLogin(String phoneNum, String password) {
        //登录or注册
        user = new User();
        user.setPhoneNum(phoneNum);
        user.setPassword(Utils.md5(password));
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                    gson.toJson(user),
                    MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(LoginActivity.loginUrl)
                    .post(requestBody)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String responseData = responseBody.string();
                        if (!responseData.isEmpty()) {
                            Log.i(TAG, "登录成功: " + responseData);
                            runOnUiThread(() -> {
                                user = gson.fromJson(responseData, User.class);
                                USER_ID = user.getUserId();
                                Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                // 保存账号和密码到SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(LoginActivity.KEY_PHONE_NUM, phoneNum);
                                editor.putString(LoginActivity.KEY_PASSWORD, password);
                                editor.putString("userId", USER_ID);
                                LvtuHmsMessageService.setCurrentUserId(USER_ID);
                                editor.apply(); // 异步保存 或者使用 editor.commit();同步保存 但会阻塞主线程

                                initView();
                                initHuanXin();
                            });
                        } else {
                            runOnUiThread(() -> {
                                // 服务器返回null 的时候 responseData.isEmpty()会为true（内容为空） 但responseData本身不是null（对象非空）
                                Toast.makeText(MainActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "响应为空", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loginHuanXin() {
        // 登录
        EMClient.getInstance().login(HuanXinUtils.createHXId(user.getUserId()), "123123", new EMCallBack() {
            // 登录成功回调
            @Override
            public void onSuccess() {
                Log.i(TAG, "环信登录成功");
                getToken();// HMS 推送
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
                EMClient.getInstance().contactManager().asyncGetAllContactsFromServer(new EMValueCallBack<List<String>>() {
                    @Override
                    public void onSuccess(List<String> strings) {
                        MessageFragment.setFriendIdList(strings);
                    }

                    @Override
                    public void onError(int i, String s) {

                    }
                });
            }

            // 登录失败回调，包含错误信息
            @Override
            public void onError(int code, String error) {
                if (code == EMError.USER_ALREADY_LOGIN) {
                    Log.i(TAG, "用户已登录:" + HuanXinUtils.createHXId(user.getUserId()));
                    getToken();// HMS 推送
                    EMClient.getInstance().contactManager().asyncGetAllContactsFromServer(new EMValueCallBack<List<String>>() {
                        @Override
                        public void onSuccess(List<String> strings) {
                            MessageFragment.setFriendIdList(strings);
                            Log.i(TAG,"friendsId: " + strings.toString());
                        }

                        @Override
                        public void onError(int i, String s) {

                        }
                    });
                } else {
                    Log.i(TAG, "环信登录失败 code:" + code + " error:" + error);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销消息监听
        if (msgListener != null) {
            EMClient.getInstance().chatManager().removeMessageListener(msgListener);
            EMClient.getInstance().logout(true);
        }
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