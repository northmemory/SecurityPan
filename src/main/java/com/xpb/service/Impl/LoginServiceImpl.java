package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpb.entities.LoginUser;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import com.xpb.service.LoginService;
import com.xpb.utils.JwtUtil;
import com.xpb.utils.RedisCache;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Value("${login.expire-time}")
    Integer loginTime;
    @Override
    public ResponseResult passwordLogin(String username,String password,String uuid,String authCode) {

        String authCodeInRedis = (String)redisCache.getCacheObject(uuid);
        if (authCodeInRedis==null || !authCodeInRedis.equals(authCode))
            return new ResponseResult(500,"验证码错误");
        UsernamePasswordAuthenticationToken authenticationToken=new UsernamePasswordAuthenticationToken(username,password);
        Authentication authenticate = manager.authenticate(authenticationToken);
        if (authenticate==null)
            return new ResponseResult(500,"用户名或密码错误");
        //登录成功为用户生成jwt,并将jwt存入redis中
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        String jwt=resolveLoginUser(loginUser);
        Map<String,String> responseData=new HashMap<>();
        responseData.put("token",jwt);
        return new ResponseResult(ResponseCode.CODE_200.getCode(), responseData);
    }

    @Override
    public ResponseResult emailLogin(String email, String imageAuthCode, String uuid, String emailAuthCode) {
        ResponseResult resolveResult = resolveAuthCode(uuid, imageAuthCode, email, emailAuthCode);
        if (resolveResult!=null) return resolveResult;
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail,email);
        User user = userMapper.selectOne(queryWrapper);
        LoginUser loginUser=new LoginUser(user,null);
        String jwt=resolveLoginUser(loginUser);
        Map<String,String> responseData=new HashMap<>();
        responseData.put("token",jwt);
        return new ResponseResult(ResponseCode.CODE_200.getCode(), responseData);
    }

    @Override
    public ResponseResult registration(String email, String nickname, String password, String uuid, String imageAuthCode, String emailAuthCode) {
        ResponseResult resolveResult = resolveAuthCode(uuid, imageAuthCode, email, emailAuthCode);
        if (resolveResult!=null) return resolveResult;
        User newUser=new User();
        newUser.setEmail(email);
        newUser.setNickname(nickname);
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encodePassword= passwordEncoder.encode(password);
        newUser.setPassword(encodePassword);
        int insert = userMapper.insert(newUser);
        if (insert==0)
            return new ResponseResult(500, "注册失败,请联系服务器管理员");
        return new ResponseResult(ResponseCode.CODE_200.getCode(), "注册成功，请使用用户名密码或邮箱登录");
    }

    @Override
    public ResponseResult resetPassword(String email, String emailAuthCode, String uuid, String imageAuthCode, String newPassword) {
        ResponseResult resolveResult = resolveAuthCode(uuid, imageAuthCode, email, emailAuthCode);
        if (resolveResult!=null) return resolveResult;
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail,email).select(User::getPassword);
        String oldPassword = userMapper.selectOne(queryWrapper).getPassword();
        if (oldPassword.equals(newPassword)) return new ResponseResult(500,"传入的密码不能够和上次相同");
        BCryptPasswordEncoder passwordEncoder=new BCryptPasswordEncoder();
        String encodeNewPassword= passwordEncoder.encode(newPassword);
        LambdaUpdateWrapper<User> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getEmail,email).set(User::getPassword,encodeNewPassword);
        int update = userMapper.update(null, updateWrapper);
        if (update==0)
            return new ResponseResult(500,"密码更新失败");
        return new ResponseResult(200,"密码更新成功");
    }

    /*
    * 处理图片验证码和邮件验证码*/
    private ResponseResult resolveAuthCode(String uuid,String imageAuthCode,String email, String emailAuthCode){
        String authCodeInRedis = (String)redisCache.getCacheObject(uuid);
        if (authCodeInRedis==null)
            return new ResponseResult(500,"图片验证码已经过期");
        if (!authCodeInRedis.equals(imageAuthCode))
            throw new RuntimeException("验证码错误");
        String cacheEmailCode = (String) redisCache.getCacheObject(email);
        if (cacheEmailCode==null)
            return new ResponseResult(500,"邮箱验证码已经过期");
        if (!cacheEmailCode.equals(emailAuthCode))
            return new ResponseResult(500,"邮箱验证码错误");
        return null;
    }
    /*处理登录成功后逻辑*/
    private String resolveLoginUser(LoginUser loginUser){
        String userId = loginUser.getUser().getUserId();
        String jwt = jwtUtil.creatJWT(userId, null);
        loginUser.setJwt(jwt);
        //将用户信息存入redis
        redisCache.setCacheObject("login"+userId,loginUser,loginTime, TimeUnit.DAYS);
        return jwt;
    }
}
