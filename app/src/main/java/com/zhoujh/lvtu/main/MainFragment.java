package com.zhoujh.lvtu.main;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhoujh.lvtu.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.OkHttpClient;

public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        initView(view);

        return view;
    }

    private void initView(View view) {
        view.findViewById(R.id.to_add_travel_plan).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTravelPlanActivity.class);
            startActivity(intent);
        });
    }
}