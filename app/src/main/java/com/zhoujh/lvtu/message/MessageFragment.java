package com.zhoujh.lvtu.message;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.UserSearchActivity;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class MessageFragment extends Fragment {
    private static final String TAG = "MessageFragment";
    private static List<String> friendIdList = new ArrayList<>();

    private ImageView toAdd;
    private RecyclerView conversationListView;
    private RefreshLayout refreshLayout;

    private OkHttpClient okHttpClient = new OkHttpClient();
    private final Gson gson = MainActivity.gson;

    public static void setFriendIdList(List<String> friendIdList) {
        MessageFragment.friendIdList.clear();
        MessageFragment.friendIdList.addAll(friendIdList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        initView(view);
        setListener();
        return view;
    }

    private void setListener() {
        toAdd.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), UserSearchActivity.class);
                    startActivity(intent);
                }
        );
    }

    private void initView(View view) {
        toAdd = view.findViewById(R.id.to_add_friend);
        conversationListView = view.findViewById(R.id.conversation_list);
        refreshLayout = view.findViewById(R.id.refreshLayout);
    }
}