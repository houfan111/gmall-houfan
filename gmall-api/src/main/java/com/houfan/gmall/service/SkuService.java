package com.houfan.gmall.service;

import com.houfan.gmall.bean.*;

import java.math.BigDecimal;
import java.util.List;

public interface SkuService {
    List<SkuInfo> getSkuInfoListBySpuId(Integer spuId);

    List<BaseAttrInfo> getAttrInfoByCatalog3Id(Integer catalog3Id);

    List<SpuImage> spuImageListBySpuId(Integer spuId);

    List<SpuSaleAttr> getSpuSaleAttrListBySpuId(Integer spuId);

    void saveSku(SkuInfo skuInfo);

    SkuInfo getSkuInfoBySkuId(Integer skuId);


    List<SkuInfo> getSkuListBySpuId(Integer spuId);

    List<SkuLsInfo> getSkuLsInfoList(Integer catalog3Id);

    List<SpuSaleAttr> getSpuSaleAttrListBySpuIdAndSkuId(Integer spuId, Integer skuId);

    SkuInfo getSimpleSkuInfoBySkuId(Integer skuId);
}
