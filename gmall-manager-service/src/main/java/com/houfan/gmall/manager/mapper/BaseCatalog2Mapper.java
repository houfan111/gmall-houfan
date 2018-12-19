package com.houfan.gmall.manager.mapper;

import com.houfan.gmall.bean.BaseCatalog2;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseCatalog2Mapper extends Mapper<BaseCatalog2> {
    List<BaseCatalog2> selectBaseCatalog2ListByC1Id(Integer catalog1Id);
}
