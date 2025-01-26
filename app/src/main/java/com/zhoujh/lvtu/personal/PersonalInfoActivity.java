package com.zhoujh.lvtu.personal;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PersonalInfoActivity extends AppCompatActivity {
    private static final String TAG = "PersonalInfoActivity";
    private final Gson gson = MainActivity.gson;

    // 声明视图变量
    private ImageView imgBack;
    private TextView txtSave;
    private ImageView imgAvatar;
    private EditText edtName;
    private Spinner spGender;
    private EditText edtBirth;
    private EditText edtEmail;

    private boolean isUploadAvatar = false;
    private String avatarType = "png";
    private byte[] avatarImageBytes;

    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private OkHttpClient client = new OkHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);
        StatusBarUtils.setImmersiveStatusBar(this, findViewById(R.id.root_personal_info), StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.PRC);

        // 注册图片选择器的结果回调
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        Log.i(TAG, "Selected image URI: " + selectedImageUri);
                        // 判断图片为png格式还是jpg格式
                        if (selectedImageUri.toString().endsWith(".png")) {
                            avatarType = "png";
                        } else if (selectedImageUri.toString().endsWith(".jpg")) {
                            avatarType = "jpg";
                        }
                        // 根据 URI 获取 Bitmap 对象
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            // 将 Bitmap 转换为字节数组
                            avatarImageBytes = bitmapToBytes(bitmap);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        isUploadAvatar = true;
                        // 将图片加载到ImageView中
                        RequestOptions requestOptions = new RequestOptions()
                                .transform(new CircleCrop());
                        Glide.with(getApplicationContext())
                                .load(selectedImageUri)
                                .placeholder(R.drawable.headimg)  // 设置占位图
                                .apply(requestOptions)// 设置签名
                                .into(imgAvatar);
                    }
                }
        );
        initView();
        setListeners();
        setData();
    }

    private void setData() {
        edtName.setText(MainActivity.user.getUserName());
        edtBirth.setText(MainActivity.user.getBirth());
        edtEmail.setText(MainActivity.user.getEmail());
        // 设置Spinner的适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);
        spGender.setSelection(MainActivity.user.getGender());

        // 清除Glide缓存在后台线程
        new Thread(() -> {
            Glide.get(this).clearDiskCache();
            runOnUiThread(() -> {
                Glide.get(this).clearMemory();
            });
        }).start();

        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(getApplicationContext())
                .load("http://"+MainActivity.IP + MainActivity.user.getAvatarUrl())
                .placeholder(R.drawable.headimg)  // 设置占位图
                .apply(requestOptions)// 设置签名
                .into(imgAvatar);
    }

    private void initView() {
        imgBack = findViewById(R.id.img_back);
        txtSave = findViewById(R.id.txt_save);
        imgAvatar = findViewById(R.id.img_avatar);
        edtName = findViewById(R.id.edt_name);
        spGender = findViewById(R.id.sp_gender);
        edtBirth = findViewById(R.id.edt_birth);
        edtEmail = findViewById(R.id.edt_email);
    }
    private void setListeners() {
        edtBirth.setOnClickListener(v -> {
            Calendar newCalendar = Calendar.getInstance();
            datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    edtBirth.setText(dateFormatter.format(newDate.getTime()));
                }

            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            // 启动图片选择器并等待结果
            imagePickerLauncher.launch(intent);
        });
        imgBack.setOnClickListener(v -> {
            finish();
        });
        txtSave.setOnClickListener(v -> {
            RequestBody requestBody;
            if (isUploadAvatar) {
                // 判断头像图片格式为png或jpg
                if (avatarType.equals("png")) {
                    Log.i(TAG, "png");
                    requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "avatar.png", RequestBody.create(MediaType.parse("image/png"), avatarImageBytes))
                            .addFormDataPart("userId", MainActivity.USER_ID)
                            .addFormDataPart("userName", edtName.getText().toString())
                            .addFormDataPart("gender", String.valueOf(spGender.getSelectedItemPosition()))
                            .addFormDataPart("email", edtEmail.getText().toString())
                            .addFormDataPart("birth", edtBirth.getText().toString())
                            .build();
                } else {
                    Log.i(TAG, "jpg");
                    requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", "avatar.jpg", RequestBody.create(MediaType.parse("image/jpeg"), avatarImageBytes))
                            .addFormDataPart("userId", MainActivity.USER_ID)
                            .addFormDataPart("userName", edtName.getText().toString())
                            .addFormDataPart("gender", String.valueOf(spGender.getSelectedItemPosition()))
                            .addFormDataPart("email", edtEmail.getText().toString())
                            .addFormDataPart("birth", edtBirth.getText().toString())
                            .build();
                }
            } else {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("userId", MainActivity.USER_ID)
                        .addFormDataPart("userName", edtName.getText().toString())
                        .addFormDataPart("gender", String.valueOf(spGender.getSelectedItemPosition()))
                        .addFormDataPart("email", edtEmail.getText().toString())
                        .addFormDataPart("birth", edtBirth.getText().toString())
                        .build();
            }
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/user/update")
                    .post(requestBody)
                    .build();
            Thread thread = new Thread(() -> {
                try(Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        if(!responseData.isEmpty()){
                            MainActivity.user = gson.fromJson(responseData, User.class);
                            runOnUiToast("更新成功");
                        } else {
                            runOnUiToast("更新失败");
                        }
                    } else {
                        runOnUiToast("网络错误");
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
        });
    }
    private void runOnUiToast(String msg){
        runOnUiThread(() -> {
            Toast.makeText(PersonalInfoActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    // 辅助方法，将 Bitmap 转换为字节数组
    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(avatarType.equals("jpg")){
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        } else {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        return stream.toByteArray();
    }
}