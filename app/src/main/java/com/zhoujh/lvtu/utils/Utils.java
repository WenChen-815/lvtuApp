package com.zhoujh.lvtu.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.zhoujh.lvtu.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * 合并两个Bitmap
     * @param firstBitmap 底层图片
     * @param secondBitmap 顶层图片
     * @param _x x偏移量（默认居中）
     * @param _y y偏移量（默认居中）
     * @return 合并后的Bitmap
     */
    public static Bitmap mergeBitmap(Bitmap firstBitmap, Bitmap secondBitmap,int _x, int _y) {
        Bitmap bitmap = Bitmap.createBitmap(
                firstBitmap.getWidth(),
                firstBitmap.getHeight(),
                firstBitmap.getConfig()
        );
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(firstBitmap, new Matrix(), null);

        // 计算居中坐标
        int x = (firstBitmap.getWidth() - secondBitmap.getWidth()) / 2 + _x;
        int y = (firstBitmap.getHeight() - secondBitmap.getHeight()) / 2 + _y;

        // 绘制第二张图到居中位置
        canvas.drawBitmap(secondBitmap, x, y, null);
        return bitmap;
    }

    /**
     * 将 Bitmap 裁剪成圆形
     * @param bitmap 原始 Bitmap
     * @return 圆形 Bitmap
     */
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        // 创建一个与原始 Bitmap 大小相同的圆形 Bitmap
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 创建一个画笔
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        // 设置画笔的抗锯齿属性
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // 在画布上绘制一个圆形
        canvas.drawOval(rectF, paint);

        // 设置画笔的混合模式为 SRC_IN，用于将原始 Bitmap 与圆形重叠部分显示出来
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // 在画布上绘制原始 Bitmap
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public static void showToast(Context context, String toastText,int duration) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.custom_toast, null);
        TextView textView = view.findViewById(R.id.text);
        textView.setText(toastText);
        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        view.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.toast_background, null));
        toast.setView(view);
        toast.show();
    }

    //通过uri获取文件
    public static File getFileFromUri(Uri uri, Context context) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            String displayName = null;
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                displayName = cursor.getString(index);
            }
            cursor.close();
            if (displayName != null) {
                InputStream inputStream = contentResolver.openInputStream(uri);
                if (inputStream != null) {
                    File file = new File(context.getCacheDir(), displayName);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                    return file;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
