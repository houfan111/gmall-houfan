package com.houfan.gmall.interceptor;

import com.houfan.gmall.annotations.LoginRequired;
import com.houfan.gmall.util.CookieUtil;
import com.houfan.gmall.util.HttpClientUtil;
import com.houfan.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import static com.houfan.gmall.util.JwtUtil.encode;

// 注意,使用到拦截器的地方,一定要保证主启动类能够扫描到这个component注解
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    // 是用来拦截所有项目中的方法,这里的request是浏览器过来最原始的客户请求,方法return true的话,就是直接放行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 向下转型获得被拦截的具体方法对象
        HandlerMethod method = (HandlerMethod) handler;
        // 获得拦截方法上的注解
        LoginRequired methodAnnotation = method.getMethodAnnotation(LoginRequired.class);

        if (methodAnnotation == null) {
            // 没有这个注解,表示公共的随便访问
            return true;
        }

        // 获取注解方法中的值
        boolean neededSuccess = methodAnnotation.isNeededSuccess();

        // 得到注解上的值,就需要根据这个判断逻辑

        String token = "";
        final String cookieName = "newToken";
        // 所有的token都是放在cookie中的,需要从cookie中取出来
        String oldToken = CookieUtil.getCookieValue(request, cookieName, true);

        // 还有一种就是刚刚从认证中心拿回来的token还没存入到cookie,直接在参数中获取
        String newToken = request.getParameter("newToken");

        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        // 以新token为准
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        // ============  下面是需要去验证登录状态   ====================
        // 服务器内部发送一个验证请求,找到验证的方法,然后返回一个验证的结果,相当于异步请求,这里就需要去校验token的真实性
        if (StringUtils.isNotBlank(token)) {
            // 第一次请求,啥也没有的情况
            String key = "houfan0725233333";
            // 通过负载均衡nginx
            String ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isBlank(ip)) {
                // 不是负载均衡来的(这里居然为空,为什么获取不到)
                ip = request.getRemoteAddr();
                if (StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            // 从passport验证令牌,需要根据ip来进行防伪
            String doGet = HttpClientUtil.doGet("http://passport.gmall.com:9022/verify?token=" + token + "&salt=" + ip);

            if (doGet.equals("success")) {
                // 验证成功,可以直接存储令牌到cookie
                // 获得token并写入到cookie中
                CookieUtil.setCookie(request, response, cookieName, token, 60 * 30, true);
                Map<String, Object> decode = JwtUtil.decode(token, key, ip);
                Integer userId = (Integer) decode.get("userId");
                String nickName = (String) decode.get("nickName");
                request.setAttribute("userId", userId);
                request.setAttribute("nickName",nickName);
                // 放行
                return true;
            }
        }

        // 下面token都为空
        if (neededSuccess == true) {
            // 说明没有token,需要强制去登录(需要带上原始请求的地址)
            String oldUrl = request.getRequestURL().toString();
            response.sendRedirect("http://passport.gmall.com:9022/index?oldUrl=" + oldUrl);
            return false;
        }
        return true;
    }
}
