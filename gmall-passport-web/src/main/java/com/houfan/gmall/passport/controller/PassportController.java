package com.houfan.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.houfan.gmall.bean.UserInfo;
import com.houfan.gmall.service.UserInfoService;
import com.houfan.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("/index")
    public String indexPage(Model model, String oldUrl){
        // 为了在登录页面中获取
        model.addAttribute("oldUrl",oldUrl);
        return "index";
    }

    @RequestMapping("/verify")
    @ResponseBody
    public String verify(HttpServletRequest request,String token ,String salt){
        String key = "houfan0725233333";
        Map<String, Object> decode = JwtUtil.decode(token, key, salt);
        if (decode == null){
            // 解析失败
            return "fail";
        }
        return "success";
    }


    @RequestMapping("/login")
    @ResponseBody
    public String login(HttpServletRequest request, UserInfo userInfo){
        // 登录就会走这个函数,发送一个令牌,根据传过来的数据查询,异步过来的
        // 根据数据库查询用户信息
        UserInfo userInfoFromDb = userInfoService.getUserInfoByUserNameAndPasswd(userInfo);

        if (userInfoFromDb == null){
            // 说明没有数据,不给过返回失败
            return "fail";
        } else {
            // url不可能为空,这时候应该颁发令牌
            String key = "houfan0725233333";
            Map<String,Object> map = new HashMap<>();
            map.put("userId",2);
            map.put("nickName",userInfoFromDb.getNickName());
            // 通过负载均衡nginx
            String ip = request.getHeader("x-forwarded-for");
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
            }
            String token = JwtUtil.encode(key, map, ip);

            // 颁发令牌
            return token;
        }




    }


}
