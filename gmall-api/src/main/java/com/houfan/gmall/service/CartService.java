package com.houfan.gmall.service;

import com.houfan.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    List<CartInfo> getCartListFromDb(Integer userId);

    void updateCartCheckedToDb(CartInfo cartInfo, String allChick, Integer isCheckedFlag);

    void saveCartInfoToDb(CartInfo cartInfo);

    List<CartInfo> mergCart(Integer userId, List<CartInfo> cartInfoList, List<CartInfo> cookieCartInfoList);

    void deleteCheckedCartInfosBySkuIds(List<Integer> delSkuIds);
}
