package com.houfan.gmall.service;

import com.houfan.gmall.bean.OrderDetail;
import com.houfan.gmall.bean.OrderInfo;

import java.util.List;

public interface OrderService {
    List<OrderDetail> getOrderDetailListByUserId(Integer userId);

    void saveTradeCode(String tradeCode,Integer userId);

    boolean checkTradeCode(String tradeCode, Integer userId);

    void saveOrderInfoToDb(OrderInfo orderInfoToDb);

    void deleteCheckedCartInfosBySkuIds(List<Integer> delSkuIds);

    OrderInfo getOrderByOutTradeNo(String outTradeNo);

    void updatePaymentStatusByMq(OrderInfo orderInfo);
}
