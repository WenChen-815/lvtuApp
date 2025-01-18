package com.zhoujh.lvtu.personal;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {
    // 声明视图变量
    private ImageView imgBack;
    private TextView txtSave;
    private ImageView imgAvatar;
    private EditText edtName;
    private Spinner spGender;
    private EditText edtBirth;
    private EditText edtEmail;

    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.PRC);
        initView();
        setListeners();
        setData();
    }

    private void setData() {
        edtName.setText(MainActivity.user.getUserName());
        edtBirth.setText(MainActivity.user.getBirth());
        edtEmail.setText(MainActivity.user.getEmail());
        spGender.setSelection(MainActivity.user.getGender());
//        spGender.getSelectedItemPosition();
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
    }
}