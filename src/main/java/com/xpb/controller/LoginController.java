package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.User;
import com.xpb.service.LoginService;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.RegexEnum;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    @GlobalInterceptor(checkParams = true)
    public ResponseResult emailLogin(@VerifyParam(regexVerify = RegexEnum.EMAIL) @RequestParam("email") String email,
                                     @VerifyParam @RequestParam("emailAuthCode") String emailAuthCode,
                                     @VerifyParam @RequestParam("uuid") String uuid,
                                     @VerifyParam @RequestParam("imageAuthCode") String imageAuthCode){
        return loginService.emailLogin(email,imageAuthCode,uuid,emailAuthCode);
    }

    @PostMapping("/regis")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult registrate(@VerifyParam(regexVerify = RegexEnum.EMAIL) @RequestParam("email") String email,
                                     @VerifyParam @RequestParam("imageAuthCode") String imageAuthCode,
                                     @VerifyParam @RequestParam("uuid") String uuid,
                                     @VerifyParam @RequestParam("username") String username,
                                     @VerifyParam @RequestParam("password") String password,
                                     @VerifyParam @RequestParam("emailAuthCode") String emailAuthCode){
        return loginService.registration(email,username,password,uuid,imageAuthCode,emailAuthCode);
    }
    @PostMapping("/reset")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult resetPassword(@VerifyParam(regexVerify = RegexEnum.EMAIL) @RequestParam("email") String email,
                                     @VerifyParam @RequestParam("imageAuthCode") String imageAuthCode,
                                     @VerifyParam @RequestParam("uuid") String uuid,
                                     @VerifyParam @RequestParam("newPassword") String newPassword,
                                     @VerifyParam @RequestParam("emailAuthCode") String emailAuthCode){
        return loginService.resetPassword(email,emailAuthCode,uuid,imageAuthCode,newPassword);
    }
    @GetMapping("/logout")
    public ResponseResult logout(){
        return loginService.logout();
    }
}
