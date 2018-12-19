package com.houfan.gmall.bean;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

public class UserInfo implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;

    @Column
    private String loginName;

    @Column
    private String nickName;

    @Column
    private String passwd;

    @Column
    private String name;

    @Column
    private String phoneNum;

    @Column
    private String email;

    @Column
    private String headImg;

    @Column
    private String userLevel;

    @Column
    private List<UserAddress> userAddressList;


    public UserInfo() {
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "id=" + id +
                ", loginName='" + loginName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", passwd='" + passwd + '\'' +
                ", name='" + name + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                ", email='" + email + '\'' +
                ", headImg='" + headImg + '\'' +
                ", userLevel='" + userLevel + '\'' +
                ", userAddressList=" + userAddressList +
                '}';
    }

    public UserInfo(String loginName, String nickName, String passwd, String name, String phoneNum, String email, String headImg, String userLevel, List<UserAddress> userAddressList) {
        this.loginName = loginName;
        this.nickName = nickName;
        this.passwd = passwd;
        this.name = name;
        this.phoneNum = phoneNum;
        this.email = email;
        this.headImg = headImg;
        this.userLevel = userLevel;
        this.userAddressList = userAddressList;
    }

    public void setUserAddressList(List<UserAddress> userAddressList) {
        this.userAddressList = userAddressList;
    }

    public List<UserAddress> getUserAddressList() {
        return userAddressList;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setHeadImg(String headImg) {
        this.headImg = headImg;
    }

    public void setUserLevel(String userLevel) {
        this.userLevel = userLevel;
    }

    public Integer getId() {
        return id;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getPasswd() {
        return passwd;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getEmail() {
        return email;
    }

    public String getHeadImg() {
        return headImg;
    }

    public String getUserLevel() {
        return userLevel;
    }
}
