package com.houfan.gmall.service;

import com.houfan.gmall.bean.SkuLsInfo;
import com.houfan.gmall.bean.SkuLsParam;

import java.util.List;

public interface ListService {

    List<SkuLsInfo> searchBySkuLsParam(SkuLsParam skuLsParam);
}
