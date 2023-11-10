package com.xpb.aop.annotation;

import com.xpb.utils.enums.RegexEnum;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.FIELD})
public @interface VerifyParam {

    int min() default -1;
    int max() default -1;

    boolean required() default true;

    RegexEnum regexVerify() default RegexEnum.NO;
}
