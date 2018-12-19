package com.houfan.gmall.manager.mapper;

import com.houfan.gmall.bean.SpuSaleAttrValue;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrValueMapper extends Mapper<SpuSaleAttrValue> {

    List<SpuSaleAttrValue> selectSpuSaleAttrValueListBySpuId(@Param("spuId") Integer spuId, @Param("saleAttrId") Integer saleAttrId);

    List<SpuSaleAttrValue> selectSpuSaleAttrValueListBySpuId(
            @Param("spuId") Integer spuId,
            @Param("skuId") Integer skuId,
            @Param("saleAttrId") Integer saleAttrId);
}
