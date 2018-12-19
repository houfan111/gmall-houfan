package com.houfan.gmall.service;

import com.houfan.gmall.bean.UserAddress;
import com.houfan.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    public List<UserInfo> getUserInfoListAll();

    public Integer addUser(UserInfo userInfo);

    public Integer updateUser(UserInfo userInfo);

    public Integer removeUserById(Integer userInfoId);

    List<UserInfo> getAllUserAndAddress();

    UserInfo getUserInfoByUserNameAndPasswd(UserInfo userInfo);

    List<UserAddress> getUserAddrListByUserId(Integer userId);
}
