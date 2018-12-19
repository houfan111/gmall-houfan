package com.houfan.gmall.service.impls;

import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.*;
import com.houfan.gmall.manager.mapper.*;
import com.houfan.gmall.service.SkuService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    /**
     * 根据spuId查所有的图片列表
     */
    @Override
    public List<SkuInfo> getSkuInfoListBySpuId(Integer spuId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);

        List<SkuInfo> skuInfoList = skuInfoMapper.select(skuInfo);
        return skuInfoList;
    }

    /**
     * 根据三级分类id查询所有的平台属性和平台属性值
     */
    @Override
    public List<BaseAttrInfo> getAttrInfoByCatalog3Id(Integer catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.select(baseAttrInfo);

        // 根据所有的平台属性id查出所有的平台属性值
        for (BaseAttrInfo attrInfo : baseAttrInfoList) {
            Integer attrInfoId = attrInfo.getId();
            BaseAttrValue baseAttrValue = new BaseAttrValue();
            baseAttrValue.setAttrId(attrInfoId);
            List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

            // 将每个attrInfoId查出来的集合封装到对应的value集合里面
            attrInfo.setAttrValueList(baseAttrValueList);
        }
        return baseAttrInfoList;
    }


    /**
     * 查询某个spu下的所有图片信息供sku选择
     */
    @Override
    public List<SpuImage> spuImageListBySpuId(Integer spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        List<SpuImage> spuImageList = spuImageMapper.select(spuImage);
        return spuImageList;
    }

    /**
     * 根据spuId查询所有的销售属性
     * @param spuId
     */
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuId(Integer spuId) {

        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuId);

        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.select(spuSaleAttr);

        // 查出所有的销售属性值
        for (SpuSaleAttr saleAttr : spuSaleAttrList) {
            // 这里需要销售属性id,不是需要id
            Integer saleAttrId = saleAttr.getSaleAttrId();

            SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
            // 需要spuId和saleAttrId来定位到添加这个spu时选择的那些销售属性,此时通用Mapper没用了(需要用到三层循环)
            //spuSaleAttr.setSpuId(spuId);
            //spuSaleAttrValue.setSaleAttrId(saleAttrId);

            // 写sql语句
            List<SpuSaleAttrValue> spuSaleAttrValueList =
                    spuSaleAttrValueMapper.selectSpuSaleAttrValueListBySpuId(spuId,saleAttrId);

            saleAttr.setSpuSaleAttrValueList(spuSaleAttrValueList);
        }

        return spuSaleAttrList;
    }


    /**
     * 保存sku
     * @param skuInfo
     */
    @Override
    public void saveSku(SkuInfo skuInfo) {

        // 有主键返回策略,可以通过写入到传入的参数中
        skuInfoMapper.insertSelective(skuInfo);

        Integer skuId = skuInfo.getId();

        // 保存skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insertSelective(skuAttrValue);
        }

        // 保存SkuImage
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insertSelective(skuImage);
        }

        // 保存skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
        }



    }

    @Override
    public SkuInfo getSkuInfoBySkuId(Integer skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        // 查询所有的集合,封装进skuInfo
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);


        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueList);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);

        return skuInfo;
    }

    //
    @Override
    public List<SkuInfo> getSkuListBySpuId(Integer spuId) {

        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> skuInfoList = skuInfoMapper.select(skuInfo);

        // 还要封装skuInfo中的集合
        for (SkuInfo info : skuInfoList) {
            Integer skuId = info.getId();

            // 根据skuId查图片列表
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(skuId);
            List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
            info.setSkuImageList(skuImageList);

            // 根据skuId封装sku平台属性
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
            info.setSkuAttrValueList(skuAttrValueList);

            // 根据skuId封装销售属性
            SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
            skuSaleAttrValue.setSkuId(skuId);
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValue);
            info.setSkuSaleAttrValueList(skuSaleAttrValueList);
        }

        return skuInfoList;
    }


    // 这个方法是向es中同步数据使用的
    @Override
    public List<SkuLsInfo> getSkuLsInfoList(Integer catalog3Id) {

        List<SkuLsInfo> skuLsInfos = new ArrayList<>();

        // 得到SkuInfo
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);
        List<SkuInfo> skuInfos = skuInfoMapper.select(skuInfo);

        for (SkuInfo info : skuInfos) {
            Integer skuId = info.getId();
            // 组装skuLsInfo
            SkuLsInfo skuLsInfo = new SkuLsInfo();
            BeanUtils.copyProperties(info,skuLsInfo);
            skuLsInfos.add(skuLsInfo);
        }

        return skuLsInfos;
    }




/*    //
    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuIdAndSkuId(Integer spuId, Integer skuId) {

        List<SpuSaleAttr> spuSaleAttrList = new ArrayList<>();

        //先查出saleAttrId,根据skuId查对应的saleAttrId
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValues = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValues) {
            Integer saleAttrId = saleAttrValue.getSaleAttrId();

            // 根据
            List<SpuSaleAttrValue> spuSaleAttrValueList =
                    spuSaleAttrValueMapper.selectSpuSaleAttrValueListBySpuId(spuId,skuId, saleAttrId);

            SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValueList);
            spuSaleAttrList.add(spuSaleAttr);
        }
        return spuSaleAttrList;
    }*/

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListBySpuIdAndSkuId(Integer spuId, Integer skuId) {

            Map<String,Object> map = new HashMap<>();
            map.put("skuId",skuId);
            map.put("spuId" + "",spuId );
            List<SpuSaleAttr> spuSaleAttrs = spuSaleAttrMapper.selectSpuSaleAttrListBySpuId(map);

            return spuSaleAttrs;
    }

    @Override
    public SkuInfo getSimpleSkuInfoBySkuId(Integer skuId) {
        return skuInfoMapper.selectByPrimaryKey(skuId);
    }

}
