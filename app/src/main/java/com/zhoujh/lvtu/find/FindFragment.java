package com.zhoujh.lvtu.find;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.utils.adapter.FragmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class FindFragment extends Fragment {
    private final String TAG = "FindFragment";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private FragmentAdapter fragmentAdapter;
    private List<Fragment> fragments = new ArrayList<>();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find, container, false);

        initView(view);

        return view;
    }

    private void initView(View view) {
        view.findViewById(R.id.to_add_post).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddPostActivity.class);
            startActivity(intent);
        });
        fragments.add(new RecommendPostFragment());
        fragments.add(new FollowPostFragment());

        fragmentAdapter = new FragmentAdapter(fragments,getActivity());
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(fragmentAdapter);
        TabLayoutMediator mediator = new TabLayoutMediator(
                tabLayout,
                viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {

                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        switch (position){
                            case 0:
                                tab.setText("推荐");
                                break;
                            case 1:
                                tab.setText("关注");
                                break;
                            default:
                                break;
                        }
                    }
                }
        );
        mediator.attach();
    }
}