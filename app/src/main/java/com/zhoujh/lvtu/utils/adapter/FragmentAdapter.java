package com.zhoujh.lvtu.utils.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class FragmentAdapter extends FragmentStateAdapter {
    private List<Fragment> fragments;
    public FragmentAdapter(List<Fragment> fragments, @NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragments = fragments;
    }
    /**
     * 返回指定位置的Fragment对象（某个确定的子页面）
     * @param position
     * @return
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments== null ? null : fragments.get(position);
    }

    /**
     * 返回子页面数量
     * @return
     */
    @Override
    public int getItemCount() {
        return fragments== null ? 0:fragments.size();
    }
}