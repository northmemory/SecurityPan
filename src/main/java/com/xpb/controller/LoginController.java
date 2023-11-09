package com.xpb.controller;

import com.xpb.entities.User;
import com.xpb.service.LoginService;
import com.xpb.utils.ResponseResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public ResponseResult login(@RequestBody Map<String,String> requestBody){
        String username=requestBody.get("username");
        String password=requestBody.get("password");
        String uuid=requestBody.get("uuid");
        String authCode=requestBody.get("authCode");
        if (username==null || password==null || authCode==null ||uuid==null)
            return new ResponseResult(500,"请求错误,用户名或密码或uuid或验证码为空");
        return loginService.passwordLogin(username,password,uuid,authCode);
    }
    @PostMapping("/emailLogin")
    public ResponseResult emailLogin(@RequestBody Map<String,String> requestBody){
        String email=requestBody.get("email");
        String emailAuthcode=requestBody.get("emailAuthCode");
        String uuid=requestBody.get("uuid");
        String imageAuthCode=requestBody.get("imageAuthCode");
        if (email==null || emailAuthcode==null || emailAuthcode==null ||uuid==null)
            return new ResponseResult(500,"请求错误,邮箱或邮件验证码或uuid或图片验证码为空");
        return loginService.emailLogin(email,imageAuthCode,uuid,emailAuthcode);
    }
}
