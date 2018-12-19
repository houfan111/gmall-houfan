package com.houfan.gmall.bean;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @param
 * @return
 */
public class SpuSaleAttrValue implements Serializable {


    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;

    @Column
    private Integer spuId;

    @Column
    private Integer saleAttrId;

    @Column
    private String saleAttrValueName;

    // 这个字段是用来在一个具体的sku下,表示这个sku是被选中的状态,区别与其他的兄弟姐妹sku
    @Transient
    private String isChecked;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSpuId() {
        return spuId;
    }

    public void setSpuId(Integer spuId) {
        this.spuId = spuId;
    }

    public Integer getSaleAttrId() {
        return saleAttrId;
    }

    public void setSaleAttrId(Integer saleAttrId) {
        this.saleAttrId = saleAttrId;
    }

    public String getSaleAttrValueName() {
        return saleAttrValueName;
    }

    public void setSaleAttrValueName(String saleAttrValueName) {
        this.saleAttrValueName = saleAttrValueName;
    }

    public String getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(String isChecked) {
        this.isChecked = isChecked;
    }
}
