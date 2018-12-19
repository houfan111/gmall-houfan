package com.houfan.gmall.user.service.impls;

import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.UserAddress;
import com.houfan.gmall.bean.UserInfo;
import com.houfan.gmall.service.UserInfoService;
import com.houfan.gmall.user.mapper.UserAddressMapper;
import com.houfan.gmall.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 注意,这里使用的是dubbo的Service注解,用来注册服务端的注解
@Service
@Transactional(readOnly = true)
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> getUserInfoListAll() {
        return userInfoMapper.selectAll();
    }

    /**
     * 查询用户的详细信息包括地址
     */
    @Override
    public List<UserInfo> getAllUserAndAddress() {
        return userInfoMapper.selectAllUserAndAddress();
    }

    @Override
    public UserInfo getUserInfoByUserNameAndPasswd(UserInfo userInfo) {

        return userInfoMapper.selectOne(userInfo);

    }

    @Override
    public List<UserAddress> getUserAddrListByUserId(Integer userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddressList = userAddressMapper.select(userAddress);
        return userAddressList;
    }

    @Override
    @Transactional(readOnly = false,rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public Integer addUser(UserInfo userInfo) {
        return userInfoMapper.insert(userInfo);
    }

    @Override
    @Transactional(readOnly = false,rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public Integer updateUser(UserInfo userInfo) {
        return userInfoMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    @Transactional(readOnly = false,rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    public Integer removeUserById(Integer userInfoId) {
        return userInfoMapper.deleteByPrimaryKey(userInfoId);
    }


}
