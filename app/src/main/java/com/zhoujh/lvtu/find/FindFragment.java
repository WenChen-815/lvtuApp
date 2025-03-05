package com.zhoujh.lvtu.find;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.main.PlanSearchActivity;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.adapter.FragmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class FindFragment extends Fragment {
    private final String TAG = "FindFragment";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ConstraintLayout root_layout;
    private ImageView search;

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
//        StatusBarUtils.setImmersiveStatusBar(getActivity(), null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
        root_layout = view.findViewById(R.id.root_layout);
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
                                tab.setText("全部");
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
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                ((View)tab.view).setScaleX(1.2f);
                ((View)tab.view).setScaleY(1.2f);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                ((View)tab.view).setScaleX(1.0f);
                ((View)tab.view).setScaleY(1.0f);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                ((View)tab.view).setScaleX(1.2f);
                ((View)tab.view).setScaleY(1.2f);
            }
        });
        tabLayout.selectTab(tabLayout.getTabAt(0));

        search = view.findViewById(R.id.search_button);
        search.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostSearchActivity.class);
            startActivity(intent);
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        StatusBarUtils.setImmersiveStatusBar(getActivity(), root_layout, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
    }
}