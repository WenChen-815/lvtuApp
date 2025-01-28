package com.zhoujh.lvtu.utils;

import android.content.Context;

public class Utils {
    /**
     * 将dp转换为px
     * @param dp dp
     * @return px
     */
    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
