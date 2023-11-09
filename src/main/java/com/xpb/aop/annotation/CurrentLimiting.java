package com.xpb.aop.annotation;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentLimiting {
    int time() default 60; //限制访问的时间s
    int value() default 5; //允许请求的次数
}
