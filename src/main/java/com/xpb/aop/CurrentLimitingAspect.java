package com.xpb.aop;

import com.xpb.aop.annotation.CurrentLimiting;
import com.xpb.utils.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class CurrentLimitingAspect {
    @Autowired
    public RedisTemplate redisTemplate;

    @Pointcut("@annotation(currentLimiting)")
    public void controllerAspect(CurrentLimiting currentLimiting) {
    }
    @Around("controllerAspect(currentLimiting)")
    public Object AroundAdvice(ProceedingJoinPoint point,CurrentLimiting currentLimiting) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes=(ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String ip=request.getHeader("X-Forwarded-For")==null ? request.getRemoteAddr() : request.getHeader("X-Forwarded-For").split(",")[0];
        String url=request.getRequestURL().toString();
        String key=ip+url;
        // Lua脚本, redis是单线程的同时只能执行一个lua脚本,也就是说把并发控制交给了redis
        String luaScript = "if redis.call('exists', KEYS[1]) == 0 then " +
                "    redis.call('set', KEYS[1], '1') " +
                "    redis.call('expire', KEYS[1], ARGV[1]) " +
                "    return 1 " +
                "else " +
                "    return redis.call('incr', KEYS[1]) " +
                "end";

        // 执行Lua脚本
       Long currentCount = (Long) redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Collections.singletonList(key), // KEYS[1]
                String.valueOf(currentLimiting.time()) // ARGV[1]
        );


        if (currentCount!=null && currentCount.intValue()>currentLimiting.value())
            return new ResponseResult(429,"请求次数过多").ToJson();

        return point.proceed();
    }

}
