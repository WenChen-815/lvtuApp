package com.zhoujh.lvtu.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class Post implements Serializable {
    String postId; // 帖子id
    String postTitle; // 帖子标题
    String postContent; // 帖子内容
    String userId; // 发帖人id
    LocalDateTime createTime; // 创建时间
    LocalDateTime updateTime; // 修改时间
    List<String> picturePath; // 帖子图片路径
    int pictureCount; // 图片数量
    int status; // 1: 发布, 0: 草稿, 2: 已删除
    List<String> tags; // 用于表示帖子标签，可以是多个标签
    int likeCount; // 点赞数
    int commentCount; // 评论数
    int starCount; // 收藏数
    int privacy; // 1: 公开, 2: 私密, 3: 仅好友可见

    @Override
    public String toString() {
        return "Post{" +
                "postId='" + postId + '\'' +
                ", postTitle='" + postTitle + '\'' +
                ", postContent='" + postContent + '\'' +
                ", userId='" + userId + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", picturePath=" + picturePath +
                ", pictureCount=" + pictureCount +
                ", status=" + status +
                ", tags=" + tags +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", starCount=" + starCount +
                ", privacy=" + privacy +
                '}';
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(List<String> picturePath) {
        this.picturePath = picturePath;
    }

    public int getPictureCount() {
        return pictureCount;
    }

    public void setPictureCount(int pictureCount) {
        this.pictureCount = pictureCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getStarCount() {
        return starCount;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }

    public int getPrivacy() {
        return privacy;
    }

    public void setPrivacy(int privacy) {
        this.privacy = privacy;
    }
}
