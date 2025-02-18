package com.zhoujh.lvtu.message.modle;

import com.hyphenate.chat.EMConversation;

public class ConservationAdapterItem {
    private String conversationId;
    private EMConversation emConversation;
    private UserConversation userConversation;

    public ConservationAdapterItem(String conversationId, EMConversation emConversation, UserConversation userConversation) {
        this.conversationId = conversationId;
        this.emConversation = emConversation;
        this.userConversation = userConversation;
    }

    public String getConversationId() {
        return conversationId;
    }

    public EMConversation getEmConversation() {
        return emConversation;
    }

    public UserConversation getUserConversation() {
        return userConversation;
    }
}
