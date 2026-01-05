// ✅ Nothing should be above this
package com.techmania.chatapp.models;

public class User {

    String userId;
    String userName;
    String userEmail;
    String imageUrl;
    String status;
    long lastMessageTimestamp; // 🔥 New field

    public User() {
    }

    public User(String userId, String userName, String userEmail, String imageUrl, String status) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 🔥 Getter and Setter for lastMessageTimestamp
    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}
