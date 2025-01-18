package com.zhoujh.lvtu.model;

import java.util.Date;

public class User {
    private String userId;       // 用户ID (UUID)
    private String userName;     // 用户昵称
    private String phoneNum;     // 手机号
    private String email;        // 邮箱
    private String password;     // 密码
    private Integer status;      // 用户状态
    private Date createTime;     // 创建时间
    private Integer gender;      // 性别
    private Integer age;         // 年龄
    private String birth;        // 生日
    private String avatarUrl;    // 头像路径
    private Date updateTime;

    public User() {
    }

    public User(String userId, String userName, String phoneNum, String email, String password, Integer status, Date createTime, Integer gender, Integer age, String birth, String avatarUrl,Date updateTime) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNum = phoneNum;
        this.email = email;
        this.password = password;
        this.status = status;
        this.createTime = createTime;
        this.gender = gender;
        this.age = age;
        this.birth = birth;
        this.avatarUrl = avatarUrl;
        this.updateTime = updateTime;
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

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
