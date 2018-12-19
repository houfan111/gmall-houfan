package com.houfan.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.houfan.gmall.annotations.LoginRequired;
import com.houfan.gmall.bean.CartInfo;
import com.houfan.gmall.bean.SkuInfo;
import com.houfan.gmall.consts.ConstantBean;
import com.houfan.gmall.service.CartService;
import com.houfan.gmall.service.SkuService;
import com.houfan.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private SkuService skuService;


    // 用来接收ajax请求,局部刷新购物车信息
    // 用来表示需要验证登录信息,但是没登录也可以访问
    @LoginRequired(isNeededSuccess = false)
    @RequestMapping("/checkCart")
    public String checkCart(
            HttpServletResponse response,
            HttpServletRequest request,
            Integer isCheckedFlag, Integer skuId,
            Integer skuNum, BigDecimal skuPrice,
            String allCheck,
            Model model) {

        //Integer userId = 2 ;
        Integer userId = (Integer) request.getAttribute("userId");
        List<CartInfo> cartList = null;
        BigDecimal totalPrice = null;

        // 封装CartInfo
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuNum(skuNum);
        cartInfo.setIsChecked(isCheckedFlag);
        //cartInfo.setCartPrice(new BigDecimal(skuNum).multiply(skuPrice));


        // 如果没登录,需要走Cookie获取列表
        if (userId == null) {
            String cookieName = ConstantBean.CART_COOKIE_NAME;
            String cookieValue = CookieUtil.getCookieValue(request, cookieName, true);
            if (StringUtils.isNotBlank(cookieValue)) {
                // 说明以前保存过(肯定添加过购物车,不然怎么进来的)
                // 更新对应skuId的cookie
                List<CartInfo> cookieCartList = JSON.parseArray(cookieValue, CartInfo.class);
                for (int i = 0; i < cookieCartList.size(); i++) {
                    CartInfo info = cookieCartList.get(i);
                    // 判断是否是全选框过来的
                    if (ConstantBean.CART_ALLCHECKE.equals(allCheck)) {
                        // 更改每一个购物车下的选中状态
                        info.setIsChecked(isCheckedFlag);

                    }else {
                        if (info.getSkuId() == skuId) {
                            // 直接改状态就行了兄弟
                            info.setIsChecked(isCheckedFlag);
                            info.setCartPrice(new BigDecimal(skuNum).multiply(skuPrice));
                            info.setSkuNum(cartInfo.getSkuNum());
                        }
                    }

                }
                // 添加cookie,更新状态,赋值给显示页面
                cartList = cookieCartList;
                String newCookieValue = JSON.toJSONString(cartList);
                CookieUtil.setCookie(request, response, cookieName, newCookieValue, 60 * 60 * 24 * 7, true);
            }


        } else {
            // 需要根据skuId和userId来更改是否被选中的状态,业务处理在service
            cartInfo.setUserId(userId);
            cartService.updateCartCheckedToDb(cartInfo,allCheck,isCheckedFlag);
            // 获取最新的购物车信息,放入到域中
            cartList = cartService.getCartListFromDb(userId);
        }

        // 存入域中
        totalPrice = getTotalPrice(cartList);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartList", cartList);
        return "cartListInner";
    }


    @LoginRequired(isNeededSuccess = false)
    @RequestMapping("cartList")
    public String cartListPage(HttpServletRequest request,HttpServletResponse response, Model model) {

        // 需要提供cartList给页面,由于没登录,用户也可以使用购物车,所以这个功能要分为登录和没登录两个方向
        //Integer userId = 2;
        Integer userId = (Integer) request.getAttribute("userId");
        List<CartInfo> cartInfoList = null;
        BigDecimal totalPrice = null;
        String cookieName = ConstantBean.CART_COOKIE_NAME;

        // 如果没登录,需要走Cookie
        if (userId == null) {
            cartInfoList = getCookieCartInfoList(request,cookieName);
        } else {
            // 合并购物车(将cookie中的值取出来,合并存入到数据库,注意注意,以前cookie中的数据没有userId哦,需要给每个都设置,再存入db中)
            cartInfoList = cartService.getCartListFromDb(userId);
            if ( cartInfoList != null && cartInfoList.size()>0){
                List<CartInfo> cookieCartInfoList = getCookieCartInfoList(request,cookieName);
                if (cookieCartInfoList != null && cookieCartInfoList.size() > 0){
                    // 这里需要合并购物车了,并返回合并后的购物车信息
                    cartInfoList = cartService.mergCart(userId,cartInfoList,cookieCartInfoList);
                    // 删除cookie
                    CookieUtil.deleteCookie(request,response,cookieName);
                }
            }
        }

        // 获得总价
        totalPrice = getTotalPrice(cartInfoList);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartList", cartInfoList);
        return "cartList";
    }


    private List<CartInfo> getCookieCartInfoList(HttpServletRequest request,String cookieName) {

        List<CartInfo> cartInfoList = null;
        // 没登录,查询cookie,封装了一个Util
        String cookieValue = CookieUtil.getCookieValue(request, cookieName, true);
        if (StringUtils.isNotBlank(cookieValue)) {
            // cookie中有
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
        }
        return cartInfoList;
    }


    // 添加购物车跳转页面
    @LoginRequired(isNeededSuccess = false)
    @RequestMapping("/addToCart")
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model, Integer skuId, Integer num) {

        // 根据skuId查询skuInfo
        SkuInfo skuInfo = skuService.getSimpleSkuInfoBySkuId(skuId);

        // 完成添加的逻辑
        //Integer userId = 2;
        Integer userId = (Integer) request.getAttribute("userId");
        // 封装cartInfo
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setSkuNum(num);
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setSkuId(skuId);
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setIsChecked(1);
        //这个购物车中的价格
        cartInfo.setCartPrice(new BigDecimal(num).multiply(skuInfo.getPrice()));

        // 声明一个存放cartList的集合

        List<CartInfo> cartInfos = new ArrayList<>();
        if (userId == null) {
            // 没登录
            String cookieName = ConstantBean.CART_COOKIE_NAME;
            String cookieValue = CookieUtil.getCookieValue(request, cookieName, true);
            if (StringUtils.isNotBlank(cookieValue)) {
                // 表示以前添加过购物车模块
                cartInfos = JSON.parseArray(cookieValue, CartInfo.class);
                // 需要找到以前是否添加过这个skuId的商品
                for (int i = 0; i < cartInfos.size(); i++) {

                    // 中途有相等的,就说明以前添加过这个商品,更新它
                    if (cartInfos.get(i).getSkuId() == skuId) {
                        // 将数目相加,将价格相加,搞定
                        Integer skuNum = cartInfos.get(i).getSkuNum();
                        BigDecimal cookieCartPrice = cartInfos.get(i).getCartPrice();
                        cartInfo.setSkuNum(skuNum + num);
                        // 这个购物车的价格 + cookie中的价格
                        BigDecimal cartPrice = cartInfo.getCartPrice();
                        cartInfo.setCartPrice(cartPrice.add(cookieCartPrice));
                        // 移除以前的旧的这个skuId的购物车信息
                        cartInfos.remove(i);
                        // 添加进集合
                        cartInfos.add(cartInfo);
                        break;

                    } else {
                        // 如果循环完毕还没有找到skuId那么就表示以前没添加过这个商品,直接添加(注意是size() - 1表示最后一个)
                        if (i == cartInfos.size() - 1) {
                            if (cartInfos.get(i).getSkuId() != skuId) {
                                // 最后一个都还不相等,就说明没有,直接添加到cookie
                                cartInfos.add(cartInfo);
                                // 解决bug(由于添加了后,集合长度变长,所以如果没有break,
                                // 就会继续往后循环,可怕,反正已经到以前的集合最后一个元素,所以直接break)
                                break;
                            }
                        }
                    }
                }
            } else {
                // value没得值,根本没用过购物车,直接添加
                cartInfos.add(cartInfo);
            }
            // 封装到cookie
            String newCookieValues = JSON.toJSONString(cartInfos);
            int maxAge = 60 * 60 * 24 * 7;
            CookieUtil.setCookie(request, response, cookieName, newCookieValues, maxAge, true);

        } else {
            // 用户已登录,添加到数据库db中
            cartService.saveCartInfoToDb(cartInfo);
        }

        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("skuNum", num);
        return "success";
    }


    private BigDecimal getTotalPrice(List<CartInfo> cartInfoList) {
        // 初始化总价
        BigDecimal totalPrice = new BigDecimal(0);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            for (CartInfo cartInfo : cartInfoList) {
                BigDecimal cartPrice = cartInfo.getCartPrice();
                if (cartInfo.getIsChecked().equals(1)) {
                    totalPrice = totalPrice.add(cartPrice);
                }
            }
        }
        return totalPrice;
    }

}
