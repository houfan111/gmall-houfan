package com.houfan.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.houfan.gmall.bean.CartInfo;
import com.houfan.gmall.bean.SkuInfo;
import com.houfan.gmall.cart.mapper.CartInfoMapper;
import com.houfan.gmall.consts.ConstantBean;
import com.houfan.gmall.service.CartService;
import com.houfan.gmall.service.SkuService;
import com.houfan.gmall.util.RedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {


    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisConfig redisConfig;

    @Reference
    private SkuService skuService;

    @Override
    public List<CartInfo> getCartListFromDb(Integer userId) {
        // 先从缓存中获取,如果没有,再从数据库获取
        Jedis jedis = redisConfig.getRedisUtil().getJedis();
        String cartKey = ConstantBean.CACHE_CART_PREFIX + userId + ConstantBean.CACHE_CART_SUFFIX;

        List<CartInfo> cartListFromCache = getCartListFromCache(jedis,cartKey);
        if (cartListFromCache != null && cartListFromCache.size() != 0){
            return cartListFromCache;
        }else{
            // 说明缓存里面没有数据,从数据库中查

            CartInfo cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            List<CartInfo> cartInfoList = cartInfoMapper.select(cartInfo);

            // 查了后,保存到redis缓存中,刷新缓存
            flashCatheByUserId(userId);
            return cartInfoList;
        }
    }


    /**
     * 根据传递过来的cartInfo修改信息
     * @param cartInfo
     * @param
     * @param isCheckedFlag
     */
    @Override
    public void updateCartCheckedToDb(CartInfo cartInfo, String allCheck, Integer isCheckedFlag) {
        Integer userId = cartInfo.getUserId();

        // 判断是否是全选框过来的
        if (ConstantBean.CART_ALLCHECKE.equals(allCheck)) {
            // 更改每一个购物车下的选中状态
            cartInfoMapper.updateAllCheckedByUserId(userId,isCheckedFlag);
        }else {
            // 根据模板来修改数据,需要组装cartPrice,单价一定是从数据库查的
            SkuInfo skuInfoBySkuId = skuService.getSimpleSkuInfoBySkuId(cartInfo.getSkuId());
            BigDecimal skuPrice = skuInfoBySkuId.getPrice();
            // 得到价格,根据数量组装单个sku购物车总价格
            BigDecimal cartPrice = skuPrice.multiply(new BigDecimal(cartInfo.getSkuNum()));


            Integer skuId = cartInfo.getSkuId();
            // 设置cartInfo
            cartInfo.setCartPrice(cartPrice);
            cartInfo.setSkuPrice(skuPrice);
            Example e = new Example(CartInfo.class);
            e.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
            cartInfoMapper.updateByExampleSelective(cartInfo,e);
        }

        // 更新购物车的缓存
        flashCatheByUserId(userId);

    }

    @Override
    public void saveCartInfoToDb(CartInfo cartInfo) {
        // 需要判断以前是否添加过这类商品
        Integer skuId = cartInfo.getSkuId();
        Integer userId = cartInfo.getUserId();
        CartInfo info = new CartInfo();
        info.setSkuId(skuId);
        info.setUserId(userId);
        CartInfo selectOne = cartInfoMapper.selectOne(info);

        if(selectOne == null){
            // 表明没有添加过,直接添加
            cartInfoMapper.insertSelective(cartInfo);
            // 刷新缓存
            flashCatheByUserId(userId);
        } else {
            // 添加过,将数目累加,然后算出总金额累加
            Integer skuNum = selectOne.getSkuNum();
            Integer skuNum1 = cartInfo.getSkuNum();
            selectOne.setSkuNum(skuNum+skuNum1);
            // 设置购物车价格
            BigDecimal cartPrice = cartInfo.getCartPrice();
            BigDecimal cartPrice1 = selectOne.getCartPrice();
            selectOne.setCartPrice(cartPrice.add(cartPrice1));
            // 添加默认被选中
            selectOne.setIsChecked(1);
            cartInfoMapper.updateByPrimaryKeySelective(selectOne);

            // 刷新缓存
            flashCatheByUserId(userId);
        }
    }


    @Override
    public List<CartInfo> mergCart(Integer userId, List<CartInfo> cartInfoList, List<CartInfo> cookieCartInfoList) {

        //遍历cookie中的数据,然后只要是不相同,就往数据库添加,需要判断某样商品在数据库中是否添加过
        for (CartInfo info : cookieCartInfoList) {
            // 判断是否以前添加过该商品
            boolean isExist =  isCartInfoExist(cartInfoList,info);
            if (isExist){
                // 修改数量,修改价格
                for (CartInfo cartInfo : cartInfoList) {

                    if (cartInfo.getSkuId().equals(info.getSkuId())){

                        cartInfo.setCartPrice(cartInfo.getCartPrice().add(info.getCartPrice()));
                        cartInfo.setSkuNum(cartInfo.getSkuNum() + info.getSkuNum());
                        if (cartInfo.getIsChecked().equals(1) || info.getIsChecked().equals(1)){
                            cartInfo.setIsChecked(1);
                        }
                        // 因为以前存在,所以有主键,直接根据主键修改
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfo);
                    }
                }
            }else {
                // 直接添加到数据库中
                info.setUserId(userId);
                cartInfoMapper.insertSelective(info);
            }

        }
        // 刷新缓存
        flashCatheByUserId(userId);
        CartInfo info = new CartInfo();
        info.setUserId(userId);
        List<CartInfo> select = cartInfoMapper.select(info);
        return select;
    }

    @Override
    public void deleteCheckedCartInfosBySkuIds(List<Integer> delSkuIds) {
        if (delSkuIds != null && delSkuIds.size() > 0){
            for (Integer delSkuId : delSkuIds) {
                CartInfo info = new CartInfo();
                info.setSkuId(delSkuId);
                cartInfoMapper.delete(info);
            }
        }
    }


    private boolean isCartInfoExist(List<CartInfo> cartInfoList, CartInfo info) {
        boolean b = false;
        for (CartInfo cartInfo : cartInfoList) {
            // 只要这个集合中存在这个sku就会返回true
            if (cartInfo.getSkuId() == info.getSkuId()){
                b = true;
            }
        }
        return b;
    }


    private void flashCatheByUserId(Integer userId) {

        Jedis jedis = redisConfig.getRedisUtil().getJedis();
        // 根据userId查询出所有的购物车,然后删除再添加到缓存,也就是刷新
        String cacheKey = ConstantBean.CACHE_CART_PREFIX + userId + ConstantBean.CACHE_CART_SUFFIX;
        // 根据key删除缓存
        jedis.del(cacheKey);

        // 添加缓存,使用hash的数据结构,可以单个的取出某一条购物车的数据,虽然没用到
        Map<String , String> map = new HashMap<>();
        // 根据userId查出所有的购物车信息
        CartInfo info = new CartInfo();
        info.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(info);

        if (cartInfos != null && cartInfos.size() > 0) {
            for (CartInfo cartInfo : cartInfos) {
                map.put(cartInfo.getId() + "",JSON.toJSONString(cartInfo));
            }
            jedis.hmset(cacheKey,map);
        }
        jedis.close();
    }





   private List<CartInfo> getCartListFromCache(Jedis jedis,String cartKey){
        // 得到userId缓存中所有购物车
       List<String> hvals = jedis.hvals(cartKey);
       jedis.close();

       // 用户第一次使用购物车
       List<CartInfo> cacheCartInfoList = new ArrayList<>();
       if(null!=hvals&&hvals.size()>0) {
           for (String hval : hvals) {
               cacheCartInfoList.add(JSON.parseObject(hval, CartInfo.class));
           }
       }
       return cacheCartInfoList;
     }







    // 遗弃
   /*   private void setCartListToCache(Jedis jedis,List<CartInfo> cartInfoList ,String cartKey){
         String s = JSON.toJSONString(cartInfoList);
         jedis.set(cartKey,s);
         jedis.close();
     }*/

}
