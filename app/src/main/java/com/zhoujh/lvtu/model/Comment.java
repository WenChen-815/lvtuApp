package com.zhoujh.lvtu.model;

import java.time.LocalDateTime;

public class Comment {
    private String id;
    private String postId;
    private String parentId;
    private String userId;
    private String replyToUserId;
    private String content;
    private LocalDateTime createTime;
    private String userName;
    private String replyToUserName;

    public Comment(String postId, String parentId, String userId, String replyToUserId, String content, LocalDateTime createTime, String userName, String replyToUserName) {
        this.postId = postId;
        this.parentId = parentId;
        this.userId = userId;
        this.replyToUserId = replyToUserId;
        this.content = content;
        this.createTime = createTime;
        this.userName = userName;
        this.replyToUserName = replyToUserName;
    }

    public Comment(String postId, String userId, String content, LocalDateTime createTime, String userName) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createTime = createTime;
        this.userName = userName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getReplyToUserId() {
        return replyToUserId;
    }

    public void setReplyToUserId(String replyToUserId) {
        this.replyToUserId = replyToUserId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getReplyToUserName() {
        return replyToUserName;
    }

    public void setReplyToUserName(String replyToUserName) {
        this.replyToUserName = replyToUserName;
    }
}
