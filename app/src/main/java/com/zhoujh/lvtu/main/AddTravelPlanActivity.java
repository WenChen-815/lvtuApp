package com.zhoujh.lvtu.main;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.RotateAnimation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.bumptech.glide.Glide;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.adapter.PoiAdapter;
import com.zhoujh.lvtu.personal.PersonalInfoActivity;
import com.zhoujh.lvtu.utils.StatusBarUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddTravelPlanActivity extends AppCompatActivity {
    private final String TAG = "AddTravelPlanActivity";

    private ImageView backBtn;
    private TextView planTitle;
    private TextView planUploadBtn;
    private EditText inputTitle;
    private EditText inputContent;
    private EditText maxParticipants;
    private EditText budget;
    private TextView budgetUnit;
    private EditText addressInput;
    private MapView mapView;
    private Spinner spGender;
    private EditText startTime;
    private EditText endTime;
    private ImageView inputImage;
    private RecyclerView addressPoi;

    private boolean isUploadPhoto = false;
    private String photoType = "png";
    private byte[] imageBytes;

    private DatePickerDialog datePickerDialog;
    private SimpleDateFormat dateFormatter;
    private Calendar startDate;
    private PoiAdapter poiAdapter;
    private AMap aMap;
    private double addressLatitude = 39.904179;
    private double addressLongitude = 116.407387;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private OkHttpClient client = new OkHttpClient();
    private List<PoiItemV2> poiItemV2List = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_travel_plan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        StatusBarUtils.setImmersiveStatusBar(this, findViewById(R.id.root_personal_info), StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        // 设置隐私政策弹窗告知用户
        MapsInitializer.updatePrivacyShow(this, true, true);
        // 用户同意隐私政策
        MapsInitializer.updatePrivacyAgree(this, true);
        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);

        initView();
        setListener();


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
                            photoType = "png";
                        } else if (selectedImageUri.toString().endsWith(".jpg")) {
                            photoType = "jpg";
                        }
                        // 根据 URI 获取 Bitmap 对象
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            // 将 Bitmap 转换为字节数组
                            imageBytes = bitmapToBytes(bitmap);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        isUploadPhoto = true;
                        // 将图片加载到ImageView中
                        Glide.with(getApplicationContext())
                                .load(selectedImageUri)
                                .placeholder(R.drawable.headimg)  // 设置占位图
                                .into(inputImage);
                    }
                }
        );

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        aMap = mapView.getMap();

        initMap(aMap);

    }

    private void setListener() {
        findViewById(R.id.post_back_btn).setOnClickListener(v -> {
            finish();
        });

        planUploadBtn.setOnClickListener(v -> {
            RequestBody requestBody;
            if (isUploadPhoto) {
                // 判断图片格式为png或jpg
                Log.i(TAG, photoType);
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "planPhoto." + photoType, RequestBody.create(MediaType.parse("image/png"), imageBytes))
                        .addFormDataPart("userId", MainActivity.USER_ID)
                        .addFormDataPart("status", String.valueOf(1))
                        .addFormDataPart("title", planTitle.getText().toString())
                        .addFormDataPart("content", inputContent.getText().toString())
                        .addFormDataPart("maxParticipants", maxParticipants.getText().toString())
                        .addFormDataPart("currentParticipants", String.valueOf(0))
                        .addFormDataPart("budget", budget.getText().toString())
                        .addFormDataPart("address", addressInput.getText().toString())
                        .addFormDataPart("startTime", startTime.getText().toString())
                        .addFormDataPart("endTime", endTime.getText().toString())
                        .addFormDataPart("addressLatitude", String.valueOf(addressLatitude))
                        .addFormDataPart("addressLongitude", String.valueOf(addressLongitude))
                        .addFormDataPart("travelMode", String.valueOf(spGender.getSelectedItemPosition()))
                        .build();
            } else {
                requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("userId", MainActivity.USER_ID)
                        .addFormDataPart("status", String.valueOf(1))
                        .addFormDataPart("title", planTitle.getText().toString())
                        .addFormDataPart("content", inputContent.getText().toString())
                        .addFormDataPart("maxParticipants", maxParticipants.getText().toString())
                        .addFormDataPart("currentParticipants", String.valueOf(0))
                        .addFormDataPart("budget", budget.getText().toString())
                        .addFormDataPart("address", addressInput.getText().toString())
                        .addFormDataPart("startTime", startTime.getText().toString())
                        .addFormDataPart("endTime", endTime.getText().toString())
                        .addFormDataPart("addressLatitude", String.valueOf(addressLatitude))
                        .addFormDataPart("addressLongitude", String.valueOf(addressLongitude))
                        .addFormDataPart("travelMode", String.valueOf(spGender.getSelectedItemPosition()))
                        .build();
            }
            Request request = new Request.Builder()
                    .url("http://" + MainActivity.IP + "/lvtu/travelPlans/createPlan")
                    .post(requestBody)
                    .build();
            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        if (!responseData.isEmpty()) {
                            runOnUiToast("发布成功");
//                            finish();
                        } else {
                            runOnUiToast("发布失败");
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });

        addressInput.addTextChangedListener(new

                                                    TextWatcher() {

                                                        @Override
                                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                                        }

                                                        @Override
                                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                            PoiSearchV2.Query query = new PoiSearchV2.Query(addressInput.getText().toString(), "", "");
                                                            query.setPageSize(10);
                                                            query.setPageNum(0);
                                                            // 创建POI搜索
                                                            PoiSearchV2 poiSearch = null;
                                                            try {
                                                                poiSearch = new PoiSearchV2(AddTravelPlanActivity.this, query);
                                                            } catch (AMapException e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                            poiSearch.setOnPoiSearchListener(new PoiSearchV2.OnPoiSearchListener() {
                                                                @Override
                                                                public void onPoiSearched(PoiResultV2 poiResultV2, int i) {
                                                                    poiItemV2List.clear();
                                                                    poiItemV2List.addAll(poiResultV2.getPois());
                                                                    // 若当前城市查询不到所需POI信息，可以通过result.getSearchSuggestionCitys()获取当前Poi搜索的建议城市。
//                List<SuggestionCity> suggestionCities = poiResult.getSearchSuggestionCitys();
                                                                    // 如果搜索关键字明显为误输入，则可通过result.getSearchSuggestionKeywords()方法得到搜索关键词建议。
//                List<String> suggestionKeywords = poiResult.getSearchSuggestionKeywords();
                                                                    // 返回结果成功或者失败的响应码。1000为成功，其他为失败（详细信息参见网站开发指南-实用工具-错误码对照表）
                                                                    if (i == 1000) {
                                                                        // 搜索成功
                                                                        Log.i(TAG, "POI搜索成功");
                                                                        poiAdapter.notifyDataSetChanged(); // 通知适配器数据已更改
                                                                    } else {
                                                                        Toast.makeText(AddTravelPlanActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }

                                                                @Override
                                                                public void onPoiItemSearched(PoiItemV2 poiItemV2, int i) {

                                                                }
                                                            });
                                                            poiSearch.searchPOIAsyn();
                                                            addressPoi.setVisibility(View.VISIBLE);
                                                        }

                                                        @Override
                                                        public void afterTextChanged(Editable s) {

                                                        }
                                                    });
        startTime.setOnClickListener(v ->

        {
            Calendar newCalendar = Calendar.getInstance();
            datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    // 检查日期是否在当前日期之前
                    if (newDate.before(newCalendar)) {
                        Toast.makeText(AddTravelPlanActivity.this, "日期不能早于当前日期", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 记录选择的日期
                    startDate = newDate;
                    startTime.setText(dateFormatter.format(newDate.getTime()));
                }

            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        endTime.setOnClickListener(v ->

        {
            Calendar newCalendar = Calendar.getInstance();
            datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    if (startDate == null) {
                        Toast.makeText(AddTravelPlanActivity.this, "请先选择开始日期", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 检查日期是否在当前日期之前且不在开始日期之前
                    if (newDate.before(newCalendar) || newDate.before(startDate)) {
                        Toast.makeText(AddTravelPlanActivity.this, "日期不能早于当前日期或早于开始日期", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 记录选择的日期
                    endTime.setText(dateFormatter.format(newDate.getTime()));
                }

            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
        inputImage.setOnClickListener(v ->

        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            // 启动图片选择器并等待结果
            imagePickerLauncher.launch(intent);
        });


    }

    private void initMap(AMap aMap) {
    }

    private void initView() {

        planTitle = findViewById(R.id.plan_title);
        planUploadBtn = findViewById(R.id.plan_upload_btn);
        inputTitle = findViewById(R.id.input_title);
        inputContent = findViewById(R.id.input_content);
        maxParticipants = findViewById(R.id.maxParticipants);
        budget = findViewById(R.id.budget);
        budgetUnit = findViewById(R.id.budget_unit);

        addressInput = findViewById(R.id.address);

        addressPoi = findViewById(R.id.address_poi);
        addressPoi.setLayoutManager(new LinearLayoutManager(this));
        poiAdapter = new PoiAdapter(poiItemV2List, this, new PoiItemClickListener());
        addressPoi.setAdapter(poiAdapter);

        spGender = findViewById(R.id.sp_gender);
        // 设置Spinner的适配器
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.travel_mode_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);
        spGender.setSelection(0);

        startTime = findViewById(R.id.start_time);

        endTime = findViewById(R.id.end_time);

        inputImage = findViewById(R.id.input_image);

    }

    public class PoiItemClickListener {
        public void onPoiItemClick(PoiItemV2 poiItemV2) {
            if (poiItemV2 != null) {
                addressInput.setText(poiItemV2.getTitle());
                addressPoi.setVisibility(View.GONE);
                addressLatitude = poiItemV2.getLatLonPoint().getLatitude();
                addressLongitude = poiItemV2.getLatLonPoint().getLongitude();
                // 将地图中心点设置为选中的 POI 的位置
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(addressLatitude, addressLongitude), 15));

                // 创建一个Marker
                MarkerOptions markerOption = new MarkerOptions();
                markerOption.position(new LatLng(addressLatitude, addressLongitude));
                markerOption.title(poiItemV2.getTitle()).snippet(poiItemV2.getSnippet());

                markerOption.draggable(false);//设置Marker不可拖动
                markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(), R.drawable.location_marker)));
                // 将Marker设置为贴地显示，可以双指下拉地图查看效果
                markerOption.setFlat(false);//设置marker平贴地图效果

                Marker marker = aMap.addMarker(markerOption);
                marker.startAnimation();
            }
        }

    }

    // 辅助方法，将 Bitmap 转换为字节数组
    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (photoType.equals("jpg")) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        } else {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        return stream.toByteArray();
    }

    private void runOnUiToast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(AddTravelPlanActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}