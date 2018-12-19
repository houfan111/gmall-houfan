package com.houfan.gmall.order.mqlistener;

import com.houfan.gmall.bean.OrderInfo;
import com.houfan.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PaymentStatusListener {

    @Autowired
    private OrderService orderService;

    /**
     * 接收消息队列,订单完成的消息
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        // 可以冲mapMessage中获取存储的参数
        String outTradeNo = mapMessage.getString("outTradeNo");
        String result = mapMessage.getString("result");
        String trade_no = mapMessage.getString("trade_no");

        if (result.equals("success")){
            // 说明是支付成功啦,开始干活
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setProcessStatus("订单已支付");
            orderInfo.setOrderStatus("订单已支付");

            orderInfo.setExpireTime(new Date());
            orderInfo.setTrackingNo(trade_no);

            orderService.updatePaymentStatusByMq(orderInfo);

            System.err.println("订单系统验证mq支付状态成功");
        } else {
            // 不是支付成功的消息,失败了,向系统发送一个失败的消息,通知库存系统?
            System.err.println("订单系统验证mq支付状态失败");
        }


    }
}
