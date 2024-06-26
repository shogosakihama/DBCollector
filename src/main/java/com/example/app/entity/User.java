package com.example.app.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "db_backup_check_detail")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "seq_serial")
  private Long id;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "custom_path")
  private String customPath;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "custom_path_enable_flag")
  private int customPathEnableFlag;

  @Column(name = "path_type")
  private String pathType;

  @Column(name = "option_username")
  private String optionUsername;

  @Column(name = "option_password")
  private String optionPassword;

  // コンストラクタ
  protected User() {}

  public User(String ipAddress, String customPath, Long userId, int customPathEnableFlag, String pathType, String optionUsername, String optionPassword) {
    this.ipAddress = ipAddress;
    this.customPath = customPath;
    this.userId = userId;
    this.customPathEnableFlag = customPathEnableFlag;
    this.pathType = pathType;
    this.optionUsername = optionUsername;
    this.optionPassword = optionPassword;
  }

  // ゲッターとセッター
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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

  @Override
  public String toString() {
    return String.format("{id:%d, ipAddress:%s, customPath:%s, userId:%d, customPathEnableFlag:%d, pathType:%s, optionUsername:%s, optionPassword:%s}", id, ipAddress, customPath, userId, customPathEnableFlag, pathType, optionUsername, optionPassword);
  }
}
