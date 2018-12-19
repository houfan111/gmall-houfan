package com.houfan.gmall.service;

import com.houfan.gmall.bean.*;

import java.util.List;
import java.util.Set;

public interface ManagerService {

    List<BaseCatalog1> getBaseCatalog1List();

    List<BaseCatalog2> getBaseCatalog2ListByC1Id(Integer catalog1Id);

    List<BaseCatalog3> getBaseCatalog3ListByC1Id(Integer catalog2Id);

    List<BaseAttrInfo> getBaseAttrInfoList(Integer catalog3Id);

    void saveBaseAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrInfoByAttrId(Integer attrId);

    List<BaseAttrInfo> getBaseAttrInfoListByValueIds(Set<Integer> valueIdSet);
}
