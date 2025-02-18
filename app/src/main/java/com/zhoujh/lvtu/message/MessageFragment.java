package com.zhoujh.lvtu.message;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMCursorResult;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.adapter.ConversationListAdapter;
import com.zhoujh.lvtu.message.modle.ConservationAdapterItem;
import com.zhoujh.lvtu.message.modle.UserConversation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";
//    private static List<String> friendIdList = new ArrayList<>();
    private List<UserConversation> userConversationList = new ArrayList<>();
    private List<EMConversation> emConversationList = new ArrayList<>();
    private List<ConservationAdapterItem> conservationAdapterItemList = new ArrayList<>();

    private ImageView toAdd;
    private RecyclerView conversationListView;
    private SmartRefreshLayout refreshLayout;

    private OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;
    private ConversationListAdapter conversationListAdapter;
    private int limit = 25;
    private String cursor = "";
    private int refreshFlag = 0; // 1- 刷新，2- 加载更多

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        initView(view);
        setListener();
        loadConversationUser();
        return view;
    }

    private void loadConversationUser() {
        new Thread(()->{
            Request request = new Request.Builder()
                    .url("http://"+ MainActivity.IP +"/lvtu/conversation/getByUserId?userId="+ MainActivity.USER_ID)
                    .build();
            try (Response response = okHttpClient.newCall(request).execute()){
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    if (!responseData.isEmpty()){
                        Type type = new TypeToken<List<UserConversation>>(){}.getType();
                        List<UserConversation> userConversations = gson.fromJson(responseData, type);
                        userConversationList.addAll(userConversations);
                        loadConversation();
                    } else {
                        Log.e(TAG, "列表为空");
                    }
                } else {
                    Log.e(TAG, "获取会话列表失败:" + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void loadConversation() {
        EMClient.getInstance().chatManager().asyncFetchConversationsFromServer(limit, cursor, new EMValueCallBack<EMCursorResult<EMConversation>>() {
            @Override
            public void onSuccess(EMCursorResult<EMConversation> result) {
                // 获取到的会话列表
                List<EMConversation> conversations = result.getData();
                emConversationList.addAll(conversations);
                for (EMConversation e: emConversationList){
                    Log.i(TAG, "会话："+e.conversationId());
//                    if (e.conversationId().equals("b52e89c9-4675-42c9-8f87-a9f720a29d4b")){
//                        EMClient.getInstance().chatManager().deleteConversationFromServer(e.conversationId(), e.getType(), true, new EMCallBack() {
//                            @Override
//                            public void onSuccess() {
//
//                            }
//
//                            @Override
//                            public void onError(int code, String error) {
//
//                            }
//                        });
//                    }
                }
                for (EMConversation conversation : conversations){
//                    Log.i(TAG, "获取会话列表成功:" + conversation.conversationId());
                    for (UserConversation userConversation : userConversationList){
                        if (conversation.conversationId().equals(userConversation.getConversationId())){
                            conservationAdapterItemList.add(new ConservationAdapterItem(
                                    userConversation.getConversationId(),
                                    conversation,
                                    userConversation));
                        }
                    }
                }
                getActivity().runOnUiThread(()->{
                    conversationListAdapter.notifyDataSetChanged();
                    // 根据是刷新还是加载更多，完成对应状态
                    if (refreshFlag == 1) {
                        refreshLayout.finishRefresh(); // 结束下拉刷新状态
                    } else if(refreshFlag == 2){
                        refreshLayout.finishLoadMore(); // 结束加载更多状态
                    }
                    refreshFlag = 0;
                });
                // 下一次请求的 cursor
                cursor = result.getCursor();
            }

            @Override
            public void onError(int error, String errorMsg) {
                Log.e(TAG, "获取会话列表失败:" + errorMsg);
            }
        });
    }

    private void setListener() {
        toAdd.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), UserSearchActivity.class);
                    startActivity(intent);
                }
        );
        // 设置刷新监听器
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                refreshFlag = 1;
                emConversationList.clear();
                userConversationList.clear();
                conservationAdapterItemList.clear();
                cursor = "";
                loadConversationUser();
            }
        });
        // 设置加载更多监听器
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                refreshFlag = 2;
                if (cursor.equals("")){
                    Log.i(TAG, "没有更多数据了");
                    refreshLayout.finishLoadMoreWithNoMoreData();
                } else {
                    loadConversation();
                }
            }
        });
    }

    private void initView(View view) {
        toAdd = view.findViewById(R.id.to_add_friend);
        refreshLayout = view.findViewById(R.id.refreshLayout);
        conversationListView = view.findViewById(R.id.conversation_list);
        conversationListView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationListAdapter = new ConversationListAdapter(conservationAdapterItemList, getContext());
        conversationListView.setAdapter(conversationListAdapter);
    }
}