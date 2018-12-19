package com.houfan.gmall.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @param
 * @return
 */
public class SkuLsResult implements Serializable{

    private List<SkuLsInfo> skuLsInfoList;

    private int Total;

    private List<String> valueIdList;

    public List<SkuLsInfo> getSkuLsInfoList() {
        return skuLsInfoList;
    }

    public void setSkuLsInfoList(List<SkuLsInfo> skuLsInfoList) {
        this.skuLsInfoList = skuLsInfoList;
    }

    public int getTotal() {
        return Total;
    }

    public void setTotal(int total) {
        Total = total;
    }

    public List<String> getValueIdList() {
        return valueIdList;
    }

    public void setValueIdList(List<String> valueIdList) {
        this.valueIdList = valueIdList;
    }
}
