package com.zhoujh.lvtu.message;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.UserSearchActivity;

public class MessageFragment extends Fragment {
    private ImageView toAdd;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        toAdd = view.findViewById(R.id.to_add_friend);
        toAdd.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), UserSearchActivity.class);
                startActivity(intent);
            }
        );
    }
}