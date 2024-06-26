package com.example.app.dto;

public class UserDetailsDTO {
    private String userSystemId;
    private String userName;
    private String ipAddress;
    private String customPath;
    private int customPathEnableFlag;
    private String pathType;
    private String optionUsername;
    private String optionPassword;

    public UserDetailsDTO(String userSystemId, String userName, String ipAddress, String customPath, int customPathEnableFlag, String pathType, String optionUsername, String optionPassword) {
        this.userSystemId = userSystemId;
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.customPath = customPath;
        this.customPathEnableFlag = customPathEnableFlag;
        this.pathType = pathType;
        this.optionUsername = optionUsername;
        this.optionPassword = optionPassword;
    }

    // ゲッターとセッター
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

    public int getCustomPathEnableFlag() {
        return customPathEnableFlag;
    }

    public void setCustomPathEnableFlag(int customPathEnableFlag) {
        this.customPathEnableFlag = customPathEnableFlag;
    }

    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

    public String getOptionUsername() {
        return optionUsername;
    }

    public void setOptionUsername(String optionUsername) {
        this.optionUsername = optionUsername;
    }

    public String getOptionPassword() {
        return optionPassword;
    }

    public void setOptionPassword(String optionPassword) {
        this.optionPassword = optionPassword;
    }

    // フォーマットされたパスを生成するメソッド
    public String getFormattedPath() {
        String formattedPath;
        if (customPathEnableFlag == 0 && "pg".equalsIgnoreCase(pathType)) {
            formattedPath = "\\backup_home\\PgBackups";
        } else if (customPathEnableFlag == 0 && "v9".equalsIgnoreCase(pathType)) {
            formattedPath = "\\backup_home\\Db2Backups";
        } else {
            formattedPath = customPath.replace("/", "\\");
        }
        return "\\\\" + ipAddress + formattedPath;
    }

    // 共有名を取得するメソッド
    public String getShareName() {
        String formattedPath = getFormattedPath();
        System.out.println("Debug Formatted Path: " + formattedPath); // デバッグ情報
        // フォーマットされたパスが \\ip_address\share_name\... であることを仮定
        String[] parts = formattedPath.split("\\\\");
        return parts.length > 3 ? parts[3] : "";
    }

    // フォルダ部分を取得するメソッド
    public String getFolderName() {
        String formattedPath = getFormattedPath();
        String[] parts = formattedPath.split("\\\\");
        return parts.length > 4 ? parts[4] : "";
    }

    @Override
    public String toString() {
        return String.format("{userSystemId:%s, userName:%s, ipAddress:%s, customPath:%s, customPathEnableFlag:%d, pathType:%s, optionUsername:%s, optionPassword:%s}", userSystemId, userName, ipAddress, customPath, customPathEnableFlag, pathType, optionUsername, optionPassword);
    }
}
