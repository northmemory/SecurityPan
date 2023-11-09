package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpb.entities.LoginUser;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import com.xpb.service.LoginService;
import com.xpb.utils.JwtUtil;
import com.xpb.utils.RedisCache;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    AuthenticationManager manager;
    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    RedisCache redisCache;

    @Autowired
    UserMapper userMapper;
    @Override
    public ResponseResult passwordLogin(String username,String password,String uuid,String authCode) {

        String authCodeInRedis = (String)redisCache.getCacheObject(uuid);
        if (!authCodeInRedis.equals(authCode))
            throw new RuntimeException("验证码错误");
        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username,password);
        Authentication authenticate = manager.authenticate(authenticationToken);
        if (authenticate==null)
            throw new RuntimeException("用户名或密码错误");
        //登录成功为用户生成jwt,并将jwt存入redis中
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        String jwt=resolveLoginUser(loginUser);
        Map<String,String> responseData=new HashMap<>();
        responseData.put("token",jwt);
        return new ResponseResult(ResponseCode.CODE_200.getCode(), responseData);
    }

    @Override
    public ResponseResult emailLogin(String email, String authCode, String uuid, String emailAuthCode) {
        String authCodeInRedis = (String)redisCache.getCacheObject(uuid);
        if (authCodeInRedis==null)
            return new ResponseResult(500,"图片验证码已经过期");
        if (!authCodeInRedis.equals(authCode))
            throw new RuntimeException("验证码错误");
        String cacheEmailCode = (String) redisCache.getCacheObject(email);
        if (cacheEmailCode==null)
            return new ResponseResult(500,"邮箱验证码已经过期");
        if (!cacheEmailCode.equals(emailAuthCode))
            throw new RuntimeException("邮箱验证码错误");
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail,email);
        User user = userMapper.selectOne(queryWrapper);
        LoginUser loginUser=new LoginUser(user,null);
        String jwt=resolveLoginUser(loginUser);
        Map<String,String> responseData=new HashMap<>();
        responseData.put("token",jwt);
        return new ResponseResult(ResponseCode.CODE_200.getCode(), responseData);
    }
    private String resolveLoginUser(LoginUser loginUser){
        String userId = loginUser.getUser().getUserId();
        String jwt = jwtUtil.creatJWT(userId, null);
        loginUser.setJwt(jwt);
        //将用户信息存入redis
        redisCache.setCacheObject("login"+userId,loginUser);
        return jwt;
    }
}
