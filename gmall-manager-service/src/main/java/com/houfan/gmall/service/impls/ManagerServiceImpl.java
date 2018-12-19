package com.houfan.gmall.service.impls;

import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.*;
import com.houfan.gmall.manager.mapper.*;
import com.houfan.gmall.service.ManagerService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Service
public class ManagerServiceImpl implements ManagerService{

   @Autowired
   private BaseCatalog1Mapper baseCatalog1Mapper;

   @Autowired
   private BaseCatalog2Mapper baseCatalog2Mapper;

   @Autowired
   private BaseCatalog3Mapper baseCatalog3Mapper;

   @Autowired
   private BaseAttrInfoMapper baseAttrInfoMapper;

   @Autowired
   private BaseAttrValueMapper baseAttrValueMapper;

    @Override
    public List<BaseCatalog1> getBaseCatalog1List() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getBaseCatalog2ListByC1Id(Integer catalog1Id) {
        return baseCatalog2Mapper.selectBaseCatalog2ListByC1Id(catalog1Id);
    }

    @Override
    public List<BaseCatalog3> getBaseCatalog3ListByC1Id(Integer catalog2Id) {
        // 还可以通过通用查询方法来,通过对象的id来查另一个表的外键
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return baseCatalog3List;
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfoList(Integer catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);
        return baseAttrInfoList;
    }


    /**
     * 这个方法可以同时增加和修改(修改是将以前的全部删除,只需要检查属性id是否为null)
     * @param baseAttrInfo
     */
    @Override
    public void saveBaseAttrInfo(BaseAttrInfo baseAttrInfo) {

        if (baseAttrInfo.getId() == null) {
            // 表示没有id,是添加的方法
            doSaveBaseAttrInfo(baseAttrInfo);
        } else {
            // 表示修改的方法
            // 先删除以前的所有信息
            baseAttrInfoMapper.deleteByPrimaryKey(baseAttrInfo.getId());
            // 根据这个对象的属性字段来删这个表的数据
            BaseAttrValue baseAttrValue = new BaseAttrValue();
            baseAttrValue.setAttrId(baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValue);

            // 调用添加的方法来添加
            doSaveBaseAttrInfo(baseAttrInfo);
        }
    }

    public void doSaveBaseAttrInfo(BaseAttrInfo baseAttrInfo){
        baseAttrInfoMapper.insertSelective(baseAttrInfo);
        // 由于添加了主键返回策略,所以可以在添加后获取到主键
        Integer attrInfoId = baseAttrInfo.getId();

        // 获得所有的值List
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        // 为每个value设置外键并保存
        for (BaseAttrValue baseAttrValue : attrValueList) {
            baseAttrValue.setAttrId(attrInfoId);
            baseAttrValueMapper.insertSelective(baseAttrValue);
        }
    }


    @Override
    public List<BaseAttrValue> getAttrInfoByAttrId(Integer attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public List<BaseAttrInfo> getBaseAttrInfoListByValueIds(Set<Integer> valueIdSet) {

        String valueIds = StringUtils.join(valueIdSet, ",");

        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectBaseAttrInfoListByValueIds( valueIds);
        return baseAttrInfoList;
    }
}
