package com.zhoujh.lvtu.personal;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.find.adapter.PostListAdapter;
import com.zhoujh.lvtu.find.modle.Post;
import com.zhoujh.lvtu.utils.modle.PageResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyPostFragment extends Fragment {
    private static final String TAG = "MyPostFragment";
    private RecyclerView recyclerView;
    private SmartRefreshLayout refreshLayout;

    private PostListAdapter postListAdapter;
    private List<Post> postList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;

    private int currentPage = 1;
    private int totalPages = 1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_post, container, false);
        recyclerView = view.findViewById(R.id.recommend_recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postListAdapter = new PostListAdapter(postList, getContext());
        recyclerView.setAdapter(postListAdapter);
        refreshLayout = view.findViewById(R.id.refreshLayout);
        // 设置刷新监听器
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                refreshPostData();
            }
        });
        // 设置加载更多监听器
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                loadMorePostData();
            }
        });
        loadPostList(currentPage, 20);
        return view;
    }
    private void loadMorePostData() {
        if (currentPage < totalPages) {
            loadPostList(currentPage + 1, 10);
        } else {
            // 如果没有更多数据，结束加载更多状态
            refreshLayout.finishLoadMoreWithNoMoreData();
        }
    }

    private void refreshPostData() {
        currentPage = 1; // 重置页码
        postList.clear(); // 清空原有数据
        loadPostList(currentPage, 10);
    }

    private void loadPostList(int pageNum, int pageSize) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/post/getMyPosts?pageNum="+pageNum+"&pageSize="+pageSize+"&userId="+ MainActivity.USER_ID)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body()!=null){
                    Type type = new TypeToken<PageResponse<Post>>() {}.getType();
                    PageResponse<Post> pageResponse = gson.fromJson(response.body().string(), type);
                    if (pageResponse != null) {
                        getActivity().runOnUiThread(() -> {
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
                        getActivity().runOnUiThread(() -> {
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