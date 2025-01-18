package com.zhoujh.lvtu.model;

public class UserInfo {
    private String userId;       // 用户ID (UUID)
    private String userName;     // 用户昵称
    private Integer status;      // 用户状态
    private Integer gender;      // 性别
    private Integer age;         // 年龄
    private String birth;        // 生日
    private String avatarUrl;    // 头像路径
    private Integer relationship = 0; // 关系状态 (0: 未关注, 1: 已关注, 2:互关, 3: 拉黑)

    public UserInfo(String userId, String userName, Integer status, Integer gender, Integer age, String birth, String avatarUrl, Integer relationship) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.gender = gender;
        this.age = age;
        this.birth = birth;
        this.avatarUrl = avatarUrl;
        this.relationship = relationship;
    }
    public UserInfo() {

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getRelationship() {
        return relationship;
    }

    public void setRelationship(Integer relationship) {
        this.relationship = relationship;
    }
}
