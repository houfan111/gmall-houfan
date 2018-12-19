package com.houfan.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.houfan.gmall.bean.PaymentInfo;
import com.houfan.gmall.consts.ConstantBean;
import com.houfan.gmall.payment.mapper.PaymentInfoMapper;
import com.houfan.gmall.payment.service.PaymentService;
import com.houfan.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        // 根据模板修改
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);

    }

    @Override
    public boolean checkPaymentStatus(String outTradeNo) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo info = paymentInfoMapper.selectOne(paymentInfo);
        if (info.getPaymentStatus().equals("订单已支付")){
            return true;
        }
        return false;
    }

    /**
     * 用来发送消息队列
     */
    public void sendPaymentResult(String out_trade_no, String result, String trade_no){
        Connection connection  = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_QUEUE");
            MapMessage mapMessage=new ActiveMQMapMessage();
            // 需要根据这个结果来更新order
            mapMessage.setString("outTradeNo",out_trade_no);
            // 传过来的结果状态
            mapMessage.setString("result",result);
            // 支付宝返回的订单流水号
            mapMessage.setString("trade_no",trade_no);

            // 创建生产者
            MessageProducer producer = session.createProducer(paymentResultQueue);

            // 发送消息
            producer.send(mapMessage);

            // 一定要提交session会话
            session.commit();

            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这个方法是用来发送延迟消息队列,用于用户支付后,主动查询支付宝的返回状态
     */
    @Override
    public void sendDelayAlipayResult(String outTradeNo, Integer count) {
        Connection connection  = activeMQUtil.getConnection();
        try {
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentResultQueue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建生产者
            MessageProducer producer = session.createProducer(paymentResultQueue);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            MapMessage mapMessage=new ActiveMQMapMessage();

            // 设置延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*20);

            // 需要根据这个结果来更新order
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("count",count);

            // 发送消息
            producer.send(mapMessage);

            // 一定要提交session会话
            session.commit();

            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }



    /**
     *  发出一个查询请求,来主动查询支付状态,并根据返回的支付状态,完成相应的业务逻辑
     */
    @Override
    public String sendCheckAlipayStatus(String outTradeNo) {

        System.err.println("调用支付宝状态查询的接口方法");

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String,String> paramsMap = new HashMap<>();
        paramsMap.put("out_trade_no",outTradeNo);
        // 传入参数
        request.setBizContent(JSON.toJSONString(paramsMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        boolean success = response.isSuccess();
        if(response.isSuccess()){
            System.err.println("调用支付宝查询方法成功");
            // 从响应中取参数
            String tradeStatus = response.getTradeStatus();
            if (ConstantBean.TRADE_SUCCESS.equals(tradeStatus) ){
                // 付款成功
                System.err.println("支付宝支付状态为成功");
                String tradeNo = response.getTradeNo();
                String buyerUserId = response.getBuyerUserId();
                String buyerPayAmount = response.getBuyerPayAmount();
                String buyerLogonId = response.getBuyerLogonId();

                // 则执行保存到数据库的业务以及发送订单付款成功的消息
                boolean status = checkPaymentStatus(outTradeNo);
                if (status){
                    // 表示已经是支付完成,直接结束方法
                    System.err.println("已经是支付状态,重复提交");
                    return ConstantBean.SUCCESS;
                }

                // 表示是未支付的状态,更改状态
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentStatus("订单已支付");
                paymentInfo.setAlipayTradeNo(tradeNo);
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(response.getBody());
                paymentInfo.setOutTradeNo(outTradeNo);
                updatePaymentInfo(paymentInfo);
                System.err.println("延迟队列更新支付状态完成,发送订单消息");
                // 向订单发送消息
                sendPaymentResult(outTradeNo, ConstantBean.SUCCESS,tradeNo);
                return ConstantBean.SUCCESS;
            }

        } else {
            // 可能会出现调用失败的情况,万一支付宝服务器炸了呢(或者一直在等待着用户扫描二维码)
            System.err.println("调用支付宝查询方法失败");
        }

        return ConstantBean.FAIL;

    }


}
