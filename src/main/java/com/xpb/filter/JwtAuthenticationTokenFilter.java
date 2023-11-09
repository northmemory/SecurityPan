package com.xpb.filter;

import com.xpb.entities.LoginUser;
import com.xpb.utils.JwtUtil;
import com.xpb.utils.RedisCache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    private RedisCache redisCache;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //获取token
        String token = request.getHeader("token");
        if (token==null){
            filterChain.doFilter(request,response);
            return;
        }
        String userid;
        //解析token并获取其中的userid
        Claims claims= null;
        try {
            claims = jwtUtil.parseJWT(token);
            userid= claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("jwt解析错误");
        }
        //从redis中获取用户信息
        String redisKey="login"+userid;
        LoginUser loginUserInfo =redisCache.getCacheObject(redisKey);
        if (Objects.isNull(loginUserInfo)){
            throw new RuntimeException("用户未进行登录");
        }
        String storeJwt=loginUserInfo.getJwt();
        //存入SecurityContextHolder
        if (storeJwt.equals(token)){
            UsernamePasswordAuthenticationToken jwtAuthenticationToken=new UsernamePasswordAuthenticationToken(loginUserInfo,null,null);
            SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken);
        }
        filterChain.doFilter(request,response);
    }
}
