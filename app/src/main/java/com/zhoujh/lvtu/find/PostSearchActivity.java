package com.zhoujh.lvtu.find;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.zhoujh.lvtu.find.adapter.PostListAdapter;
import com.zhoujh.lvtu.find.modle.Post;
import com.zhoujh.lvtu.message.adapter.UserAdapter;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.modle.PageResponse;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostSearchActivity extends AppCompatActivity {
    private static final String TAG = "PostSearchActivity";
    private EditText searchText;
    private RefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private PostListAdapter postListAdapter;
    private List<Post> postList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;

    private int currentPage = 1;
    private int totalPages = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        StatusBarUtils.setImmersiveStatusBar(this, null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        initView();
        postList.clear(); // 清空原有数据
    }

    private void initView() {
        searchText = findViewById(R.id.et_searchtext);
        searchText.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                postList.clear(); // 清空原有数据
                loadPostData(v.getText().toString(), currentPage, 15);
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
        postListAdapter = new PostListAdapter(postList, this);
        recyclerView.setAdapter(postListAdapter);
    }

    private void refreshUserData() {
        currentPage = 1; // 重置页码
        postList.clear(); // 清空原有数据
        loadPostData(searchText.getText().toString(), currentPage, 15);
    }

    private void loadMoreUserData() {
        if (currentPage < totalPages) {
            loadPostData(searchText.getText().toString(), currentPage + 1, 15);
        } else {
            // 如果没有更多数据，结束加载更多状态
            refreshLayout.finishLoadMoreWithNoMoreData();
        }
    }
    private void loadPostData(String titleStr, int pageNum, int pageSize) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/post/search?titleStr="+titleStr+"&pageNum="+pageNum+"&pageSize="+pageSize)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body()!=null){
                    Type type = new TypeToken<PageResponse<Post>>() {}.getType();
                    PageResponse<Post> pageResponse = gson.fromJson(response.body().string(), type);
                    if (pageResponse != null) {
                        runOnUiThread(() -> {
                            postList.addAll(pageResponse.getRecords());
                            currentPage = pageResponse.getCurrent();
                            totalPages = pageResponse.getPages();
                            postListAdapter.notifyDataSetChanged();

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
                    Log.e(TAG, "加载帖子请求失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}