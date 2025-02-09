package com.zhoujh.lvtu.personal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.hyphenate.chat.EMClient;
import com.zhoujh.lvtu.LvtuHmsMessageService;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.login.LoginActivity;

public class PersonalFragment extends Fragment {
    private static final String TAG = "PersonalFragment";
    private Button btnToInfo, btnLogout;
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
        btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            MainActivity.user = null;
            // 清除sharePreference的信息
            SharedPreferences sp = getActivity().getSharedPreferences(LoginActivity.PREFS_NAME, getActivity().MODE_PRIVATE);
            sp.edit().clear().apply();
            if(MainActivity.msgListener != null){
                EMClient.getInstance().chatManager().removeMessageListener(MainActivity.msgListener);
            }
            EMClient.getInstance().logout(true);
            LvtuHmsMessageService.setIsSendToServer(false);
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
    }
}