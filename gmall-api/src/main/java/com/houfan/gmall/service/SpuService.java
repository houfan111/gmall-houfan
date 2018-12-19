package com.houfan.gmall.service;

import com.houfan.gmall.bean.BaseSaleAttr;
import com.houfan.gmall.bean.SpuInfo;

import java.util.List;

public interface SpuService {


    List<SpuInfo> getSupListByClg3Id(Integer catalog3Id);

    List<BaseSaleAttr> getBaseSaleAttrList();

    void saveSpu(SpuInfo spuInfo);
}
