package com.example.app.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_master")
public class UserMaster {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "user_system_id")
  private String userSystemId;

  @Column(name = "user_name")
  private String userName;

  // コンストラクタ
  protected UserMaster() {}

  public UserMaster(String userSystemId, String userName) {
    this.userSystemId = userSystemId;
    this.userName = userName;
  }

  // ゲッターとセッター
  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

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

  @Override
  public String toString() {
    return String.format("{userId:%d, userSystemId:%s, userName:%s}", userId, userSystemId, userName);
  }
}
