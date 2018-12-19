package com.houfan.gmall.user.mapper;

import com.houfan.gmall.bean.UserAddress;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserAddressMapper extends Mapper<UserAddress> {
    List<UserAddress> selectUserAddressListByUserId(Integer userId);
}
