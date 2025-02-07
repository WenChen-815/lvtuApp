package com.zhoujh.lvtu.main;

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
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.adapter.PlanListAdapter;
import com.zhoujh.lvtu.adapter.PostListAdapter;
import com.zhoujh.lvtu.model.PageResponse;
import com.zhoujh.lvtu.model.Post;
import com.zhoujh.lvtu.model.TravelPlan;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RecommendFragment extends Fragment {
    private static final String TAG = "RecommendFragment";

    private RecyclerView recyclerView;
    private RefreshLayout refreshLayout;

    private PlanListAdapter planListAdapter;
    private List<TravelPlan> planList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;

    private int currentPage = 1;
    private int totalPages = 1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.fragment_recommend, container, false);

        recyclerView = view.findViewById(R.id.recommend_recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        planListAdapter = new PlanListAdapter(planList, getContext());
        recyclerView.setAdapter(planListAdapter);
        refreshLayout = view.findViewById(R.id.refreshLayout);
        // 设置刷新监听器
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                refreshPlanData();
            }
        });
        // 设置加载更多监听器
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                loadMorePlanData();
            }
        });
        loadPlanList(currentPage, 20);
        return view;
    }

    private void refreshPlanData() {
        currentPage = 1; // 重置页码
        planList.clear(); // 清空原有数据
        loadPlanList(currentPage, 10);
    }

    private void loadMorePlanData() {
        if (currentPage < totalPages) {
            loadPlanList(currentPage + 1, 10);
        } else {
            // 如果没有更多数据，结束加载更多状态
            refreshLayout.finishLoadMoreWithNoMoreData();
        }
    }

    private void loadPlanList(int pageNum, int pageSize) {
        new Thread(() -> {
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/travelPlans/getPlans?pageNum="+pageNum+"&pageSize="+pageSize)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body()!=null){
                    Type type = new TypeToken<PageResponse<TravelPlan>>() {}.getType();
                    PageResponse<TravelPlan> pageResponse = gson.fromJson(response.body().string(), type);
                    if (pageResponse != null) {
                        getActivity().runOnUiThread(() -> {
                            planList.addAll(pageResponse.getRecords());
                            currentPage = pageResponse.getCurrent();
                            totalPages = pageResponse.getPages();
                            planListAdapter.notifyDataSetChanged();

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
                    Log.e(TAG, "加载计划列表请求失败");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}