package com.houfan.gmall.payment.service;

import com.houfan.gmall.bean.PaymentInfo;

import java.util.Date;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfo(PaymentInfo paymentInfo);

    boolean checkPaymentStatus(String out_trade_no);

    void sendPaymentResult(String out_trade_no, String result, String trade_no);

    String sendCheckAlipayStatus(String outTradeNo);

    void sendDelayAlipayResult(String outTradeNo, Integer count);
}
