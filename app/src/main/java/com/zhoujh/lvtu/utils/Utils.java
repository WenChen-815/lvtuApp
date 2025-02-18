package com.zhoujh.lvtu.utils;

import android.content.Context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    /**
     * 获取""之间的内容
     * @param str str
     * @return str
     */
    public static String absContent(String str) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            System.out.println("未找到匹配的内容");
            return "";
        }
    }
    /**
     * 将dp转换为px
     * @param dp dp
     * @return px
     */
    public static int dpToPx(int dp, Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // MD5加密
    public static String md5(String input) {
        try {
            // 创建MessageDigest实例，指定算法为MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将输入字符串转换为字节数组并进行哈希计算
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static float ratio(int i, int height) {
        return (float) i / (float) height;
    }
}
