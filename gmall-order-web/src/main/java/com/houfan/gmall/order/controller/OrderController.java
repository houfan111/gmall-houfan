package com.houfan.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.houfan.gmall.annotations.LoginRequired;
import com.houfan.gmall.bean.*;
import com.houfan.gmall.bean.enums.PaymentWay;
import com.houfan.gmall.service.CartService;
import com.houfan.gmall.service.OrderService;
import com.houfan.gmall.service.UserInfoService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import tk.mybatis.mapper.entity.Example;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private OrderService orderService;

    @Reference
    private CartService cartService;





    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("/submitOrder")
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo,String tradeCode){
        Integer userId = (Integer) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        // 首先验证订单码是否存在,如果不存在,则说明用户是重复提交订单
        boolean result =  orderService.checkTradeCode(tradeCode,userId);

        if (result == false){
            // 重复提交订单,返回订单失败页
            return "tradeFail";
        }

        // 生成订单详情和订单数据(更改数据库中的数据)
        OrderInfo orderInfoToDb = new OrderInfo();
        BeanUtils.copyProperties(orderInfo,orderInfoToDb);
        orderInfoToDb.setCreateTime(new Date());
        orderInfoToDb.setOrderComment("我是订单描述测试");
        orderInfoToDb.setUserId(userId);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        // 设置过期时间,设置为一天后过期
        Calendar calendar = Calendar.getInstance();
        // 第一个代表需要表示的日期维度,第二个表示偏移量
        calendar.add(Calendar.DATE,1);
        orderInfoToDb.setExpireTime(calendar.getTime());
        orderInfoToDb.setOrderStatus("订单已提交");
        orderInfoToDb.setPaymentWay(PaymentWay.ONLINE);
        orderInfoToDb.setProcessStatus("订单已提交");

        String outTradeNo = "HOUFANGMALL" + System.currentTimeMillis() + format.format(new Date());
        orderInfoToDb.setOutTradeNo(outTradeNo);
        // 设置总价格
        BigDecimal totalAmount = getTotalPrice(orderInfo.getOrderDetailList());
        orderInfoToDb.setTotalAmount(totalAmount);
        List<OrderDetail> orderDetailListByUserId = orderService.getOrderDetailListByUserId(userId);

        orderInfoToDb.setOrderDetailList(orderDetailListByUserId);
        // 保存订单信息表
        orderService.saveOrderInfoToDb(orderInfoToDb);

        // 删除购物车中被选中的数据
        List<Integer> delSkuIds =  new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailListByUserId) {
            // 这些都是被选中的,找到skuid然后根据skuId删除对应的购物车信息就可以了
            delSkuIds.add(orderDetail.getSkuId());
        }
        //orderService.deleteCheckedCartInfosBySkuIds(delSkuIds);
        // 重定向到支付页面
        return "redirect:http://payment.gmall.com:9042/index?outTradeNo="+ outTradeNo + "&totalAmount=" + totalAmount;
    }

    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("/toTrade")
    public String toTradePage(HttpServletRequest request, Model model){
        Integer userId = (Integer) request.getAttribute("userId");
        String nickName = (String) request.getAttribute("nickName");

        // 结算界面需要给点啥呢,将选中的购物车信息展示出来
        // 收货地址列表和用户基本信息
        List<UserAddress> userAddressList = userInfoService.getUserAddrListByUserId(userId);

        // 获得订单详情列表
        List<OrderDetail> orderDetailList = orderService.getOrderDetailListByUserId(userId);

        // 防止重复提交订单,需要生成一个唯一的订单码,然后页面一个,缓存一个,当点击提交后,将redis中的删除
        String tradeCode = UUID.randomUUID().toString();
        // 存到redis中
        orderService.saveTradeCode(tradeCode,userId);

        BigDecimal totalPrice = getTotalPrice(orderDetailList);
        model.addAttribute("tradeCode",tradeCode);
        model.addAttribute("totalAmount",totalPrice);
        model.addAttribute("orderDetailList",orderDetailList);
        model.addAttribute("userAddressList",userAddressList);
        return "trade";
    }

    private BigDecimal getTotalPrice(List<OrderDetail> orderDetailList) {
        // 初始化总价
        BigDecimal totalPrice = new BigDecimal(0);
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                BigDecimal cartPrice = orderDetail.getOrderPrice();
                    totalPrice = totalPrice.add(cartPrice);
            }
        }
        return totalPrice;
    }

}
