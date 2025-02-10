package com.zhoujh.lvtu.utils.modle;

import java.util.Map;

public class ClientPush {
    private String pusherId;
    private String pusherName;
    private String targetId;
    private String title;
    private String body;
    private Map<String,String> dataMap;

    /**
     * 推送消息
     * @param pusherId 推送者ID
     * @param pusherName 推送者名称
     * @param targetId 接收者ID
     * @param title 标题
     * @param body 内容
     * @param dataMap 推送数据
     */
    public ClientPush(String pusherId, String pusherName, String targetId, String title, String body, Map<String, String> dataMap) {
        this.pusherId = pusherId;
        this.pusherName = pusherName;
        this.targetId = targetId;
        this.title = title;
        this.body = body;
        this.dataMap = dataMap;
    }
}
