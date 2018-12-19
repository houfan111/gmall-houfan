package com.houfan.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.houfan.gmall.bean.*;
import com.houfan.gmall.service.ManagerService;
import com.houfan.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class ListController {


    @Reference
    private ListService listService;

    @Reference
    private ManagerService managerService;

    /**
     * 根据页面传送过来的数据查询sku列表信息,然后显示
     * 只有两个入口,一个是首页搜索,一个是三级分类
     */
    @RequestMapping("/list.html")
    public String listPage(SkuLsParam skuLsParam, Model model){
        // 根据skuLsParam查询es中的数据
        List<SkuLsInfo> skuLsInfoList =  listService.searchBySkuLsParam(skuLsParam);
        List<BaseAttrInfo> baseAttrInfoList = null;
        String urlParam = "";

        String keyword = skuLsParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)){
            if (StringUtils.isBlank(urlParam)){

                urlParam = "keyword=" + keyword;
            }
            model.addAttribute("keyword",keyword);
        }

        Integer catalog3Id = skuLsParam.getCatalog3Id();
        if (catalog3Id != null && StringUtils.isBlank(urlParam)){
                urlParam = "catalog3Id=" + catalog3Id ;
        }

        // 需要根据valueId来筛选出剩下的sku,并删除所选的那个valueId所在的属性行
        Integer[] valueIds = skuLsParam.getValueId();
        if (valueIds != null && valueIds.length != 0){
            // 需要根据valueId的集合,去查出这些id对应的平台属性对象
            // 定义一个空的集合,然后将需要显示的sku放在这个集合中
            List<SkuLsInfo> skuLsInfos = new ArrayList<>();
            for (Integer valueId : valueIds) {
                // 首先拼接地址栏字符串
                urlParam = urlParam + "&valueId=" + valueId;
                // 根据valueId来查询出sku然后删除
                for (SkuLsInfo skuLsInfo : skuLsInfoList) {
                    List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
                    // 第三层循环
                    for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                        Integer valueId1 = skuLsAttrValue.getValueId();
                        if (valueId.equals(valueId1)){
                            // 表示需要留下这条skuInfo
                            skuLsInfos.add(skuLsInfo);
                            break;
                        }
                    }
                }

            }
            // 清空再赋值?
            skuLsInfoList.clear();
            skuLsInfoList = skuLsInfos;
            model.addAttribute("skuLsInfoList",skuLsInfoList);
        } else {
            model.addAttribute("skuLsInfoList",skuLsInfoList);
        }

        // 得到set集合
        Set<Integer> valueIdSet  = new HashSet<>();
        // 声明一个用来制作面包屑的集合
        List<BaseAttrValue> attrValueSelectedList = null;

        if (skuLsInfoList != null && skuLsInfoList.size() != 0){
            for (SkuLsInfo skuLsInfo : skuLsInfoList) {
                List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();

                for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                    Integer valueId1 = skuLsAttrValue.getValueId();
                    // 使用set集合自动去重复
                    valueIdSet.add(valueId1);
                }
            }

            // 调用BaseAttrService的方法来查询attrInfo
            baseAttrInfoList = managerService.getBaseAttrInfoListByValueIds(valueIdSet);

            if (baseAttrInfoList != null && baseAttrInfoList.size() != 0){
                if (valueIds != null && valueIds.length != 0) {
                    attrValueSelectedList = new ArrayList<>();
                    Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator();
                    while (iterator.hasNext()){
                        BaseAttrInfo baseAttrInfo = iterator.next();
                        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                        for (BaseAttrValue baseAttrValue : attrValueList) {
                            Integer id = baseAttrValue.getId();
                            for (Integer valueId : valueIds) {
                                // 在了里面组装需要显示的属性列表和与之对应的面包屑集合
                                if (id.equals(valueId)){
                                    // 需要添加被选择的valueId组成的list中,这时候面包屑中的url应该要减去这个valueId,和筛选时相反
                                    // 可以使用replace,就可以减去选中的valueId
                                    String newUrlParam = urlParam.replace("&valueId=" + valueId,"");
                                    baseAttrValue.setUrlParam(newUrlParam);
                                    attrValueSelectedList.add(baseAttrValue);
                                    // 表示要删,此处只能使用迭代器实现边遍历集合边删除的操作
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            }
        }

        model.addAttribute("attrList",baseAttrInfoList);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("attrValueSelectedList",attrValueSelectedList);
        return "list";
    }


    @RequestMapping("/index")
    public String indexPage(){
        return "index";
    }


}
