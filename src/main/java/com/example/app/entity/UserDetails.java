package com.example.app.entity;

public class UserDetails {
    private String userSystemId;
    private String userName;
    private String ipAddress;
    private String customPath;

    public UserDetails(String userSystemId, String userName, String ipAddress, String customPath) {
        this.userSystemId = userSystemId;
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.customPath = customPath;
    }

    // Getters and setters

    public String getUserSystemId() {
        return userSystemId;
    }

    public void setUserSystemId(String userSystemId) {
        this.userSystemId = userSystemId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCustomPath() {
        return customPath;
    }

    public void setCustomPath(String customPath) {
        this.customPath = customPath;
    }
}
