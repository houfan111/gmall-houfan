package com.houfan.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.houfan.gmall.bean.SkuSaleAttrValue;
import com.houfan.gmall.bean.SkuInfo;
import com.houfan.gmall.bean.SpuSaleAttr;
import com.houfan.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private SkuService skuService;


/*    // 遗弃(逻辑有点错误)
    @RequestMapping("/{skuId}.html")
    public String item(@PathVariable("skuId") Integer skuId, Model model) {

        // 需要把所有的sku查出来,根据skuId
        SkuInfo skuInfo = skuService.getSkuInfoBySkuId(skuId);

        Integer spuId = skuInfo.getSpuId();

        // 需要查出对应的spu所有的销售属性集合
        List<SpuSaleAttr> spuSaleAttrListCheckBySku = skuService.getSpuSaleAttrListBySpuId(spuId);

        // 需要找出当前被选中的是哪一个sku(简单的方法是写sql,最笨的就是两层循环比对,就可以知道哪个sku是被选中的)
        List<SpuSaleAttrValue> spuSaleAttrValueList = skuService.getSpuSaleAttrValueListBySpuId(spuId);
        // 查出这个sku中的销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
            Integer spuSaleAttrValueId = spuSaleAttrValue.getId();
            // 这个spu下所有的销售属性
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // 获得sku销售属性id
                Integer skuSaleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();
                // 判断是够相等,如果相等,则就说明存在
                if (spuSaleAttrValueId == skuSaleAttrValueId) {
                    spuSaleAttrValue.setIsChecked(1);
                } else {
                    spuSaleAttrValue.setIsChecked(0);
                }
            }
        }

        for (SpuSaleAttr spuSaleAttr : spuSaleAttrListCheckBySku) {
            spuSaleAttr.setSpuSaleAttrValueList(spuSaleAttrValueList);
        }

        // 需要查出这个spu下的所有sku集合
        List<SkuInfo> skuInfoList = skuService.getSkuListBySpuId(spuId);

        model.addAttribute("valuesSku", skuInfoList);
        model.addAttribute("spuSaleAttrListCheckBySku", spuSaleAttrListCheckBySku);
        model.addAttribute("skuInfo", skuInfo);

        return "item";
    }*/


    @RequestMapping("/{skuId}.html")
    public String itemInfo(@PathVariable("skuId") Integer skuId, Model model) {
        // 需要把sku查出来,根据skuId
        SkuInfo skuInfo = skuService.getSkuInfoBySkuId(skuId);

        Integer spuId = skuInfo.getSpuId();

        // 需要查出这个spu下的所有sku集合
        List<SkuInfo> skuInfoList = skuService.getSkuListBySpuId(spuId);
        // 组合来制作hash表
        Map<String, Integer> map = new HashMap<>();
        for (SkuInfo info : skuInfoList) {
            String skuSaleAttrValueIdsKey = "";

            List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();

            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // 封装哈希表key
                skuSaleAttrValueIdsKey = skuSaleAttrValueIdsKey + "|" + skuSaleAttrValue.getSaleAttrValueId();
            }

            Integer skuSaleAttrValueIdsValue = info.getId();
            map.put(skuSaleAttrValueIdsKey,skuSaleAttrValueIdsValue);
        }

        String valuesSkuJson = JSON.toJSONString(map);

        // 需要查出对应的spu所有的销售属性集合
        List<SpuSaleAttr> spuSaleAttrListCheckBySku = skuService.getSpuSaleAttrListBySpuIdAndSkuId(spuId,skuId);

        model.addAttribute("spuSaleAttrListCheckBySku", spuSaleAttrListCheckBySku);
        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("valuesSkuJson", valuesSkuJson);
        return "item";
    }


}
