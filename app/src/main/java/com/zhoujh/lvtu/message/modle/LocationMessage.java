package com.zhoujh.lvtu.message.modle;

import lombok.Data;

@Data
public class LocationMessage {
    public final static int SINGLE_TYPE = 1;
    public final static int GROUP_TYPE = 2;

    private String groupId;
    private String userId;
    private int type;
    private double longitude;//经度
    private double latitude;//纬度
    private String senderId;

    /**
     * 构造方法
     * @param groupId 组号
     * @param userId 用户id
     * @param latitude 纬度
     * @param longitude 经度
     */
    public LocationMessage(String groupId, String userId, int type, double longitude, double latitude, String senderId) {
        this.groupId = groupId;
        this.userId = userId;
        this.type = type;
        this.longitude = longitude;
        this.latitude = latitude;
        this.senderId = senderId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}
