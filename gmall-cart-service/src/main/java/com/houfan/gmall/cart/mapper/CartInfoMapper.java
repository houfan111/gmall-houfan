package com.houfan.gmall.cart.mapper;

import com.houfan.gmall.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

public interface CartInfoMapper extends Mapper<CartInfo> {
    void updateAllCheckedByUserId(@Param("userId") Integer userId,@Param("isCheckedFlag") Integer isCheckedFlag);
}
