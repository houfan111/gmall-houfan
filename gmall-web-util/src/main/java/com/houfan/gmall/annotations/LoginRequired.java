package com.houfan.gmall.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {

    // 用来判断是否是一定需要登录的(特殊模块,虽然不用登录也可以访问,但是需要验证用户登录状态)
    boolean isNeededSuccess() default true;
}
