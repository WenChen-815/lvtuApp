package com.zhoujh.lvtu;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.zhoujh.lvtu.personal.PersonalInfoActivity;

public class PersonalFragment extends Fragment {
    private Button btnToInfo;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        btnToInfo = view.findViewById(R.id.btn_to_info);
        btnToInfo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PersonalInfoActivity.class);
            startActivity(intent);
        });
    }
}