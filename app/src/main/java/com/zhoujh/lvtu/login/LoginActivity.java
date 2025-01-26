package com.zhoujh.lvtu.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.model.User;

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
    private final String loginUrl = "http://"+ MainActivity.IP +"/lvtu/user/registerAndLogin"; // 后端登录接口URL
    private final Gson gson = MainActivity.gson;
    private User user;
    private EditText etPhoneNum;
    private EditText etPassword;
    private Button btnLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
    }

    // 初始化视图组件
    private void initView() {
        etPhoneNum = findViewById(R.id.etPhoneNum);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {
            //TODO 登录or注册
            user = new User();
            user.setPhoneNum(etPhoneNum.getText().toString());
            user.setPassword(md5(etPassword.getText().toString()));
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
                                });
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("user", responseData);
                                startActivity(intent);
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

        });
    }

    // MD5加密
    private String md5(String input) {
        try {
            // 创建MessageDigest实例，指定算法为MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组并进行哈希计算
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}