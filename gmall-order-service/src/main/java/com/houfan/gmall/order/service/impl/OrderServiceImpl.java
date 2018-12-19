package com.houfan.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.houfan.gmall.bean.CartInfo;
import com.houfan.gmall.bean.OrderDetail;
import com.houfan.gmall.bean.OrderInfo;
import com.houfan.gmall.bean.enums.ProcessStatus;
import com.houfan.gmall.consts.ConstantBean;
import com.houfan.gmall.order.mapper.OrderInfoMapper;
import com.houfan.gmall.service.CartService;
import com.houfan.gmall.service.OrderService;
import com.houfan.gmall.order.mapper.OrderDetailMapper;
import com.houfan.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{

    @Reference
    private CartService cartService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<OrderDetail> getOrderDetailListByUserId(Integer userId) {
        List<OrderDetail> detailList = new ArrayList<>();
        List<CartInfo> cartListFromDb = cartService.getCartListFromDb(userId);
        if (cartListFromDb != null && cartListFromDb.size() > 0){
            for (CartInfo cartInfo : cartListFromDb) {
                if (cartInfo.getIsChecked().equals(1)){
                    // 封装detailList
                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setOrderPrice(cartInfo.getCartPrice());
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    orderDetail.setSkuName(cartInfo.getSkuName());
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    detailList.add(orderDetail);
                }
            }

        }
        return detailList;
    }

    @Override
    public void saveTradeCode(String tradeCode,Integer userId) {
        Jedis jedis = redisUtil.getJedis();
        String key = ConstantBean.ORDER_TRADE_CODE_PREFIX + userId + ConstantBean.ORDER_TRADE_CODE_SUFFIX;

        // 存放订单码,设置过期时间
        jedis.setex(key,60*15,tradeCode);
        jedis.close();
    }

    @Override
    public boolean checkTradeCode(String tradeCode, Integer userId) {

        Jedis jedis = redisUtil.getJedis();
        String key = ConstantBean.ORDER_TRADE_CODE_PREFIX + userId + ConstantBean.ORDER_TRADE_CODE_SUFFIX;
        String tradeCodeCache = jedis.get(key);
        // 查询过后马上删除这个订单码,下次再重复提交肯定不能通过了
        jedis.del(key);

        // 直接比较不一致或者为空都返回的false
        boolean equals = tradeCode.equals(tradeCodeCache);
        return equals;
    }

    @Override
    public void saveOrderInfoToDb(OrderInfo orderInfoToDb) {
        // 保存订单信息表
        orderInfoMapper.insert(orderInfoToDb);
        // 保存订单详情表
        List<OrderDetail> orderDetailList = orderInfoToDb.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Integer orderInfoId = orderInfoToDb.getId();
            orderDetail.setOrderId(orderInfoId);
            orderDetailMapper.insert(orderDetail);
        }
    }

    /**
     * 根据skuId集合删除被选中的购物车集合
     * @param delSkuIds
     */
    @Override
    public void deleteCheckedCartInfosBySkuIds(List<Integer> delSkuIds) {
        cartService.deleteCheckedCartInfosBySkuIds(delSkuIds);
    }

    @Override
    public OrderInfo getOrderByOutTradeNo(String outTradeNo) {

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        OrderInfo info = orderInfoMapper.selectOne(orderInfo);

        // 需要将订单详情集合封装进去
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(info.getId());
        List<OrderDetail> details = orderDetailMapper.select(orderDetail);
        info.setOrderDetailList(details);

        return info;
    }

    @Override
    public void updatePaymentStatusByMq(OrderInfo orderInfo) {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",orderInfo.getOutTradeNo());
        orderInfoMapper.updateByExampleSelective(orderInfo,example);
    }


}
