package com.houfan.gmall.payment.mqlistener;

import com.houfan.gmall.consts.ConstantBean;
import com.houfan.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class DelayPaymentStatusMqListener {


    @Autowired
    private PaymentService paymentService;

    /**
     * 这里是mq消息的消费者监听到有消息,就消费,不满足条件,继续发送查询的请求
     */
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {
        // 从mapMessage中获取数据
        String outTradeNo = mapMessage.getString("outTradeNo");
        int count = mapMessage.getInt("count");

        // 查询支付宝状态
        String result = paymentService.sendCheckAlipayStatus(outTradeNo);
        if (ConstantBean.FAIL.equals(result) && count > 0 ){
            System.err.println("再次发送队列消息,查询状态");
            System.err.println("剩余查询次数:" + count +  "次" );
            // 表示已经支没有付成功,继续发送查询的请求
            paymentService.sendDelayAlipayResult(outTradeNo,count-1);
        }

    }




}
