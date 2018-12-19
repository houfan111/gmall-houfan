package com.houfan.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.houfan.gmall.annotations.LoginRequired;
import com.houfan.gmall.bean.OrderDetail;
import com.houfan.gmall.bean.OrderInfo;
import com.houfan.gmall.bean.PaymentInfo;
import com.houfan.gmall.payment.configbean.AlipayConfig;
import com.houfan.gmall.payment.service.PaymentService;
import com.houfan.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tk.mybatis.mapper.entity.Example;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private AlipayClient alipayClient;

    @Reference
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("/index")
    public String toIndexPage(HttpServletRequest request, BigDecimal totalAmount, String outTradeNo, Model model) {
        Integer userId = (Integer) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("nickName", nickName);
        model.addAttribute("outTradeNo", outTradeNo);
        return "index";
    }

    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("/alipay/submit")
    @ResponseBody
    public ResponseEntity<String> toAlipaySubmit(HttpServletResponse response, HttpServletRequest request, BigDecimal totalAmount, String outTradeNo, Model model) {
        Integer userId = (Integer) request.getAttribute("userId");

        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("outTradeNo", outTradeNo);

        // 向数据库保存支付信息(剩下的等支付宝回调函数再保存)
        OrderInfo orderInfo = orderService.getOrderByOutTradeNo(outTradeNo);

        // 需要先做幂等性检查,如果订单状态已经为已支付,那就不用走下面的流程了,说明是重复提交,直接返回已支付状态
        if (orderInfo.getOrderStatus().equals("订单已支付")){
            return ResponseEntity.ok().body("订单已经支付成功,请勿重复提交");
        }

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus("订单未支付");
        // 生成描述
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfo.setOrderId(orderInfo.getId());
        // 保存到数据库
        paymentService.savePaymentInfo(paymentInfo);

        // 以下是支付宝的业务
        AlipayTradePagePayRequest alipayTradePagePayRequest = new AlipayTradePagePayRequest();
        // 设置公共参数
        alipayTradePagePayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayTradePagePayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        // 填充业务参数
        Map<String, String> map = new HashMap<>();
        // 封装价格(注意啊,这里面的键需要和支付宝官网一样,带下划线的)
        map.put("total_amount", "0.01");
        map.put("out_trade_no", outTradeNo);
        // 目前只支持这个
        String product_code = "FAST_INSTANT_TRADE_PAY";
        map.put("product_code", product_code);
        List<OrderDetail> detailList = orderService.getOrderDetailListByUserId(userId);
        // 封装描述(一般选择第一个意思意思)
        map.put("subject", detailList.get(0).getSkuName());

        // 转换成json
        String mapString = JSON.toJSONString(map);
        // 这个代码会调用page.pay的接口,set是填充业务参数
        alipayTradePagePayRequest.setBizContent(mapString);
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayTradePagePayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        System.out.println(form);
        response.setContentType("text/html;charset=utf-8");

        // 在提交之后,我们需要主动询问支付宝业务处理状态,因为有可能支付宝因为网络原因
        // 导致回传调用我们的函数发生失败,所以主动询问,根据支付宝回传的状态来处理业务
        // 设置需要查询的次数
        Integer count = 5;
        paymentService.sendDelayAlipayResult(outTradeNo ,count);


        return ResponseEntity.ok().body(form);
    }

    /**
     * 支付宝回调函数(同步是给用户看的,实际开发以异步为准)
     */
    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("alipay/callback/return")
    public String alipayCallback(HttpServletResponse response,HttpServletRequest request) {

        // System.err.println("我是支付宝回调");
        // 写回调的代码,接收阿里传回的参数
        String sign = request.getParameter("sign");
        // 前台回跳的时间
        String out_trade_no = request.getParameter("out_trade_no");
        // 支付宝的流水号
        String trade_no = request.getParameter("trade_no");
        String total_amount = request.getParameter("total_amount");

        boolean signVerified = true;
        try {
            // 给一个空的map,因为无法实现异步的验签,异步的话支付宝就会封装一个map集合,所以同步就没办法
            Map<String,String> paramsMap = new HashMap<>();
            signVerified  = AlipaySignature.rsaCheckV1(paramsMap,AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            System.err.println("验签成功");
            String trade_status = request.getParameter("trade_status");
            System.err.println(trade_status);
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            System.err.println("验签失败");
        }

        // 需要先做幂等性检查,如果订单状态已经为已支付,那就不用走下面的流程了,说明是重复提交,直接返回已支付状态
        boolean checked = paymentService.checkPaymentStatus(out_trade_no);
        // 说明已经是支付完成,直接返回
       if (checked){
            return "finish";
        }

        // 验签完成,更改payment状态
        PaymentInfo paymentInfo = new PaymentInfo();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date callbackTime = new Date();
        paymentInfo.setCallbackTime(callbackTime);
        paymentInfo.setPaymentStatus("订单已支付");
        paymentInfo.setAlipayTradeNo(trade_no);
        paymentInfo.setCallbackContent(request.getQueryString());
        // 支付系统修改各种信息
        paymentService.updatePaymentInfo(paymentInfo);

        // 支付成功(支付模块的任务已经完成),需要得到回传的参数,然后通过消息队列来向order服务发送消息
        paymentService.sendPaymentResult(out_trade_no,"success",trade_no);
        return "finish";
    }

    /**
     * 测试mq整合是否成功
     */
/*    @RequestMapping("/sendResult")
    @ResponseBody
    public String sendPaymentResult(@RequestParam("orderId") String orderId){
        paymentService.sendPaymentResult(out_trade_no, "success", orderId,"success" );
        return "has been sent";
    }*/





}
