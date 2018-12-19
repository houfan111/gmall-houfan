package com.houfan.gmall.bean;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @param
 * @return
 */
public class SkuLsParam implements Serializable{

    private Integer  catalog3Id;

    private Integer[] valueId;

    private String keyword;

    private BigDecimal price;

    private int  pageNo=1;

    private int pageSize=20;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getCatalog3Id() {
        return catalog3Id;
    }

    public void setCatalog3Id(Integer catalog3Id) {
        this.catalog3Id = catalog3Id;
    }

    public Integer[] getValueId() {
        return valueId;
    }

    public void setValueId(Integer[] valueId) {
        this.valueId = valueId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
