package com.zhoujh.lvtu.personal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hyphenate.chat.EMClient;
import com.zhoujh.lvtu.LvtuHmsMessageService;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.login.LoginActivity;
import com.zhoujh.lvtu.main.FollowFragment;
import com.zhoujh.lvtu.main.RecommendFragment;
import com.zhoujh.lvtu.utils.StatusBarUtils;
import com.zhoujh.lvtu.utils.adapter.FragmentAdapter;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PersonalFragment extends Fragment {
    private static final String TAG = "PersonalFragment";

    private ImageView avatar, bigAvatar;
    private TextView userName, bigUserName,gender,age;
    private ImageView btnToInfo, btnLogout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FragmentAdapter fragmentAdapter;
    private List<Fragment> fragments = new ArrayList<>();
    //顶部渐变控件
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private ConstraintLayout toolbar_layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);
        initView(view);
        setData();
        setListener();
        return view;
    }

    private void setData() {
        userName.setText(MainActivity.user.getUserName());
        bigUserName.setText(MainActivity.user.getUserName());
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        Glide.with(getActivity())
                .load("http://" + MainActivity.IP + MainActivity.user.getAvatarUrl())
                .placeholder(R.drawable.headimg)
                .apply(requestOptions)
                .into(avatar);
        Glide.with(getActivity())
                .load("http://" + MainActivity.IP + MainActivity.user.getAvatarUrl())
                .placeholder(R.drawable.headimg)
                .apply(requestOptions)
                .into(bigAvatar);
        userName.setVisibility(View.GONE);
        avatar.setVisibility(View.GONE);
        if(MainActivity.user.getGender() == 0){
            gender.setText("性别：未知");
        }else if(MainActivity.user.getGender() == 1){
            gender.setText("性别：男");
        } else {
            gender.setText("性别：女");
        }
        if (MainActivity.user.getBirth() == null || MainActivity.user.getBirth().equals("")) {
            age.setText("年龄：未知");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 将字符串转换为 LocalDate 对象
            LocalDate birthDate = LocalDate.parse(MainActivity.user.getBirth(), formatter);
            // 获取当前日期
            LocalDate currentDate = LocalDate.now();
            // 计算年龄
            Period period = Period.between(birthDate, currentDate);
            age.setText("年龄：" + period.getYears());
        }
    }

    private void setListener() {
        btnLogout.setOnClickListener(v -> {
            MainActivity.user = null;
            // 清除sharePreference的信息
            SharedPreferences sp = getActivity().getSharedPreferences(LoginActivity.PREFS_NAME, getActivity().MODE_PRIVATE);
            sp.edit().clear().apply();
            if (MainActivity.msgListener != null) {
                EMClient.getInstance().chatManager().removeMessageListener(MainActivity.msgListener);
            }
            EMClient.getInstance().logout(true);
            LvtuHmsMessageService.setIsSendToServer(false);
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });
        btnToInfo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PersonalInfoActivity.class);
            startActivity(intent);
        });
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                toolbar.setBackgroundColor(changeAlpha(getResources().getColor(R.color.white), Math.abs(verticalOffset * 1.0f) / appBarLayout.getTotalScrollRange()));

                if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    // 完全折叠
                    userName.setVisibility(View.VISIBLE);
                    avatar.setVisibility(View.VISIBLE);
                    btnToInfo.setImageResource(R.mipmap.edit_blue);
                    btnLogout.setImageResource(R.mipmap.exit_blue);
                } else {
                    // 非完全折叠
                    userName.setVisibility(View.GONE);
                    avatar.setVisibility(View.GONE);
                    btnToInfo.setImageResource(R.mipmap.edit_white);
                    btnLogout.setImageResource(R.mipmap.exit_white);
                }
            }
        });
    }

    private void initView(View view) {
        //顶部渐变控件
        toolbar = view.findViewById(R.id.toolbar);
        appBarLayout = view.findViewById(R.id.appbar);
        gender = view.findViewById(R.id.gender);
        age = view.findViewById(R.id.age);
        toolbar_layout = view.findViewById(R.id.toolbar_layout);
//        setStatusBar();

        fragments.add(new MyPlanFragment());
        fragments.add(new MyPostFragment());

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
                                tab.setText("我的计划");
                                break;
                            case 1:
                                tab.setText("我的帖子");
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


        userName = view.findViewById(R.id.user_name);
        bigUserName = view.findViewById(R.id.big_user_name);
        avatar = view.findViewById(R.id.avatar);
        bigAvatar = view.findViewById(R.id.big_avatar);
        btnToInfo = view.findViewById(R.id.btn_to_info);
        btnLogout = view.findViewById(R.id.btn_logout);

        // 获取状态栏高度
        int statusBarHeight = getStatusBarHeight();
        // 动态设置 Toolbar 的高度
        ViewGroup.LayoutParams params = toolbar.getLayoutParams();
        params.height = statusBarHeight + params.height;
        toolbar.setLayoutParams(params);
//        toolbar.setPadding(
//                toolbar.getPaddingLeft(),
//                toolbar.getTitleMarginTop()+statusBarHeight,
//                toolbar.getPaddingRight(),
//                toolbar.getPaddingBottom());
    }

    //顶部渐变控件
    public int changeAlpha(int color, float fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int alpha = (int) (Color.alpha(color) * fraction);
        return Color.argb(alpha, red, green, blue);
    }
    // 获取状态栏高度
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    private void setStatusBar(){
        StatusBarUtils.setImmersiveStatusBar(getActivity(),null, StatusBarUtils.STATUS_BAR_TEXT_COLOR_DARK);
    }
    @Override
    public void onResume() {
        super.onResume();
        setStatusBar();
    }
}