package com.zhoujh.lvtu.customView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class NoScrollRecyclerView extends RecyclerView {

    public NoScrollRecyclerView(@NonNull Context context) {
        super(context);
    }

    public NoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NoScrollRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthSpec, expandSpec);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
        params.height = getMeasuredHeight();
    }

    // 禁止滑动
    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
        return false;
    }
}
