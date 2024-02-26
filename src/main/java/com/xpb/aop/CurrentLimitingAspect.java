package com.xpb.aop;

import com.xpb.aop.annotation.CurrentLimiting;
import com.xpb.utils.ResponseResult;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

@Aspect
@Component
@Slf4j
public class CurrentLimitingAspect {
    @Resource
    public RedisTemplate redisTemplate;

    private static DefaultRedisScript<Long> defaultRedisScript;

    static {
        // Lua脚本, redis是单线程的同时只能执行一个lua脚本,也就是说把并发控制交给了redis
        defaultRedisScript = new DefaultRedisScript<Long>();
        defaultRedisScript.setResultType(Long.class);
        defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/TrafficLimit.lua")));
    }

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
        // 执行Lua脚本
        Long currentCount = (Long) redisTemplate.execute(
                defaultRedisScript,
                Collections.singletonList(key), // KEYS[1]
                currentLimiting.time() // ARGV[1]
        );


        if (currentCount!=null && currentCount.intValue()>currentLimiting.value())
            return new ResponseResult(429,"请求次数过多").ToJson();

        return point.proceed();
    }

}
