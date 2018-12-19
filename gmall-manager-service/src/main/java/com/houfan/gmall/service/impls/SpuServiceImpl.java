package com.houfan.gmall.service.impls;

import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.*;
import com.houfan.gmall.manager.mapper.*;
import com.houfan.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService{

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Override
    public List<SpuInfo> getSupListByClg3Id(Integer catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpu(SpuInfo spuInfo) {
        // 保存spuInfo,返回主键,然后为其他的小伙伴设置spuid
        spuInfoMapper.insertSelective(spuInfo);

        Integer spuId = spuInfo.getId();
        // 保存图片信息
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuId);
                spuImageMapper.insertSelective(spuImage);
            }
        }

        // 保存saleAttrValue
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList !=null && spuSaleAttrList.size() > 0 ){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                // 对每个spuSaleAttr设置spuId
                spuSaleAttr.setSpuId(spuId);
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                // 第二层循环
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    spuSaleAttrValue.setSpuId(spuId);
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }
        }


    }
}
