package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.User;
import com.xpb.service.LoginService;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.RegexEnum;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult login(@VerifyParam @RequestParam("username") String username,
                                @VerifyParam(regexVerify = RegexEnum.PASSWORD) @RequestParam("password") String password,
                                @VerifyParam @RequestParam("uuid") String uuid,
                                @VerifyParam @RequestParam("authCode") String authCode){
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
