package com.zhoujh.lvtu.utils.modle;

import com.flyjingfish.openimagelib.beans.OpenImageUrl;
import com.flyjingfish.openimagelib.enums.MediaType;

public class ImageEntity implements OpenImageUrl {
    public static int TYPE_IMAGE = 0;
    public static int TYPE_VIDEO = 1;
    public String photoUrl;//图片大图
    public String smallPhotoUrl;//图片小图
    public String coverUrl;//视频封面大图
    public String smallCoverUrl;//视频封面小图
    public String videoUrl;//视频链接
    public int resouceType; //0图片1视频



    @Override
    public String getImageUrl() {
        return resouceType == 1 ? coverUrl : photoUrl;//大图链接（或视频的封面大图链接）
    }

    @Override
    public String getVideoUrl() {
        return videoUrl;//视频链接
    }

    @Override
    public String getCoverImageUrl() {//这个代表前边列表展示的图片（即缩略图）
        return resouceType == 1 ? smallCoverUrl : smallPhotoUrl;//封面小图链接（或视频的封面小图链接）
    }

    @Override
    public MediaType getType() {
        return resouceType == 1 ? MediaType.VIDEO : MediaType.IMAGE;//数据是图片还是视频
    }
    public ImageEntity() {
    }
    public ImageEntity(String photoUrl, String smallPhotoUrl, String coverUrl, String smallCoverUrl, String videoUrl, int resouceType) {
        this.photoUrl = photoUrl;
        this.smallPhotoUrl = smallPhotoUrl;
        this.coverUrl = coverUrl;
        this.smallCoverUrl = smallCoverUrl;
        this.videoUrl = videoUrl;
        this.resouceType = resouceType;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getSmallPhotoUrl() {
        return smallPhotoUrl;
    }

    public void setSmallPhotoUrl(String smallPhotoUrl) {
        this.smallPhotoUrl = smallPhotoUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSmallCoverUrl() {
        return smallCoverUrl;
    }

    public void setSmallCoverUrl(String smallCoverUrl) {
        this.smallCoverUrl = smallCoverUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public int getResouceType() {
        return resouceType;
    }

    public void setResouceType(int resouceType) {
        this.resouceType = resouceType;
    }
}
