package com.zhoujh.lvtu.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.flyjingfish.openimagelib.BaseInnerFragment;
import com.flyjingfish.openimagelib.OpenImage;
import com.flyjingfish.openimagelib.beans.OpenImageUrl;
import com.flyjingfish.openimagelib.listener.OnItemLongClickListener;
import com.flyjingfish.openimagelib.listener.SourceImageViewIdGet;
import com.flyjingfish.openimagelib.transformers.ScaleInTransformer;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.model.ImageEntity;

import java.util.ArrayList;
import java.util.List;

public class BannerPagerAdapter extends RecyclerView.Adapter<BannerPagerAdapter.ImageViewHolder> {
    private int type = 0; //轮播图类型 0为普通轮播图：点击图片放大；  1为页面转跳的轮播图 默认为0
    private Context context;
    private final ViewPager2 viewPager2;
    private final List<ImageEntity> imageData = new ArrayList<>();

    public void setType(int type) {
        this.type = type;
    }

    public BannerPagerAdapter(List<String> imagePath, ViewPager2 viewPager2) {
        this.viewPager2 = viewPager2;
        for (String imageUrl : imagePath) {
            String wholePath = "http://"+MainActivity.IP + imageUrl;
            imageData.add(new ImageEntity(wholePath, wholePath, null, null, null, ImageEntity.TYPE_IMAGE));
        }
    }

    @NonNull
    @Override
    public BannerPagerAdapter.ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();

        View view;
        if (type == 0) {
            view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.image_item_type1, parent, false);
        }
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerPagerAdapter.ImageViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (type == 0) {
//            MyImageLoader.getInstance().load(holder.imageView, imageData.get(position % imageData.size()).getCoverImageUrl(), R.mipmap.loading, R.mipmap.blank);
            // 使用Glide加载图片
            Glide.with(context)
                    .load(imageData.get(position % imageData.size()).getCoverImageUrl())
                    .placeholder(R.drawable.headimg)  // 设置占位图
                    .into(holder.imageView);
            holder.imageView.setOnClickListener(v -> {
                OpenImage.with(context).setClickViewPager2(viewPager2, new SourceImageViewIdGet() {
                            @Override
                            public int getImageViewId(OpenImageUrl data, int position) {
                                return R.id.imageView;
                            }
                        })
                        .setAutoScrollScanPosition(true)
                        .setImageUrlList(imageData)
                        .setSrcImageViewScaleType(ImageView.ScaleType.CENTER_CROP, true)
                        .addPageTransformer(new ScaleInTransformer())
                        .setClickPosition(position % imageData.size(), position)
                        .setOnItemLongClickListener(new OnItemLongClickListener() {
                            @Override
                            public void onItemLongClick(BaseInnerFragment fragment, OpenImageUrl openImageUrl, int position) {
                                Toast.makeText(context, "长按图片", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setShowDownload()
                        //添加更多布局控件
//                        .addMoreView(R.layout.big_img_layout,
//                                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
//                                MoreViewShowType.BOTH,
//                                new OnLoadViewFinishListener() {
//                                    @Override
//                                    public void onLoadViewFinish(View view) {
//                                        TextView tv = view.findViewById(R.id.big_img_tv);
//                                        tv.setText("");
//                                    }
//                                })
                        .show();
            });
        } else if (type == 1) {
            // 待扩展
        }

    }

    @Override
    public int getItemCount() {
//        return imagepath != null ? imagepath.size() : 0;
        return Integer.MAX_VALUE;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
