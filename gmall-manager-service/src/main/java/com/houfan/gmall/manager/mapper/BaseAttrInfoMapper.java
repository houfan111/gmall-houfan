package com.houfan.gmall.manager.mapper;

import com.houfan.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo>{
    List<BaseAttrInfo> selectBaseAttrInfoListByValueIds(@Param("valueIds") String valueIds);
}
