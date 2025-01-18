package com.zhoujh.lvtu.message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.Utils.StatusBarUtils;
import com.zhoujh.lvtu.adapter.UserAdapter;
import com.zhoujh.lvtu.model.PageResponse;
import com.zhoujh.lvtu.model.User;
import com.zhoujh.lvtu.model.UserInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserSearchActivity extends AppCompatActivity {
    private static final String TAG = "UserSearchActivity";
    private EditText searchText;
    private RefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<UserInfo> userInfoList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    private Gson gson = new Gson();

    private int currentPage = 1;
    private int totalPages = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        StatusBarUtils.setImmersiveStatusBar(this, getWindow().getDecorView(), StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        initView();
    }

    private void initView() {
        searchText = findViewById(R.id.et_searchtext);
        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                loadUserData(v.getText().toString(), currentPage, 10);
                return true;
            }
            return false;
        });

        refreshLayout = findViewById(R.id.refreshLayout);
        // 设置刷新监听器
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshLayout) {
                refreshUserData();
            }
        });
        // 设置加载更多监听器
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshLayout) {
                loadMoreUserData();
            }
        });

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userInfoList, this);
        recyclerView.setAdapter(userAdapter);
    }

    private void refreshUserData() {
        currentPage = 1; // 重置页码
        userInfoList.clear(); // 清空原有数据
        loadUserData(searchText.getText().toString(), currentPage, 10);
    }

    private void loadMoreUserData() {
        if (currentPage < totalPages) {
            loadUserData(searchText.getText().toString(), currentPage + 1, 10);
        } else {
            // 如果没有更多数据，结束加载更多状态
            refreshLayout.finishLoadMoreWithNoMoreData();
        }
    }
    private void loadUserData(String userNameStr, int pageNum, int pageSize) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/user/search?userId="+MainActivity.USER_ID+"&userNameStr="+userNameStr+"&pageNum="+pageNum+"&pageSize="+pageSize)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body()!=null){
                    Type type = new TypeToken<PageResponse<UserInfo>>() {}.getType();
                    PageResponse<UserInfo> pageResponse = gson.fromJson(response.body().string(), type);
                    if (pageResponse != null) {
                        runOnUiThread(() -> {
                            userInfoList.addAll(pageResponse.getRecords());
                            currentPage = pageResponse.getCurrent();
                            totalPages = pageResponse.getPages();
                            userAdapter.notifyDataSetChanged();

                            // 根据是刷新还是加载更多，完成对应状态
                            if (pageNum == 1) {
                                refreshLayout.finishRefresh(); // 结束下拉刷新状态
                            } else {
                                refreshLayout.finishLoadMore(); // 结束加载更多状态
                            }
                        });
                    } else {
                        // 数据加载失败，结束刷新和加载更多状态
                        runOnUiThread(() -> {
                            refreshLayout.finishRefresh(false);
                            refreshLayout.finishLoadMore(false);
                        });
                    }
                } else {
                    Log.e(TAG, "加载用户请求失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}