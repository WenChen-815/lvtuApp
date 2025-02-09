package com.zhoujh.lvtu.login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.User;
import com.zhoujh.lvtu.utils.StatusBarUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoginActivity extends AppCompatActivity {
    private final String TAG = "LoginActivity";
    public static final String loginUrl = "http://"+ MainActivity.IP +"/lvtu/user/registerAndLogin"; // 后端登录接口URL

    public static final String PREFS_NAME = "LoginPrefs";
    public static final String KEY_PHONE_NUM = "phoneNum";
    public static final String KEY_PASSWORD = "password";

    private final Gson gson = MainActivity.gson;
    private User user;
    private EditText etPhoneNum;
    private EditText etPassword;
    private Button btnLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        StatusBarUtils.setImmersiveStatusBar(this, getWindow().getDecorView(), StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        initView();
    }

    private void login(String phoneNum, String password) {
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
                    .url(loginUrl)
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
                                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                // 保存账号和密码到SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(KEY_PHONE_NUM, phoneNum);
                                editor.putString(KEY_PASSWORD, password);
                                editor.apply(); // 异步保存 或者使用 editor.commit();同步保存 但会阻塞主线程

                            });
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("user", responseData);
                            startActivity(intent);
                            finish();
                        } else {
                            runOnUiThread(() -> {
                                // 服务器返回null 的时候 responseData.isEmpty()会为true（内容为空） 但responseData本身不是null（对象非空）
                                Toast.makeText(LoginActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "响应为空", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "请求失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // 初始化视图组件
    private void initView() {
        etPhoneNum = findViewById(R.id.etPhoneNum);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            login(etPhoneNum.getText().toString(), etPassword.getText().toString());
        });
    }
}