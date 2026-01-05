package com.techmania.chatapp.models;

public class MessagesModel {

    private String senderId;
    private String receiverId;
    private String messageId;
    private String message;
    private long timestamp;
    private String status;     // "sent", "delivered", "seen"
    private String type;       // "text", "image", "pdf"
    private String mediaUrl;   // If type is image or pdf
    private boolean deleted;   // true if deleted for everyone

    public MessagesModel() {
        // Required empty constructor for Firebase
    }

    public MessagesModel(String senderId, String receiverId, String messageId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageId = messageId;
        this.message = message;
        this.timestamp = timestamp;
        this.status = "sent";
        this.type = "text";      // Default
        this.mediaUrl = null;
        this.deleted = false;
    }

    // Getters
    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public boolean isDeleted() {
        return deleted;
    }

    // Setters
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
