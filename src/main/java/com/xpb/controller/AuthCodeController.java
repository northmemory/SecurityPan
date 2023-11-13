package com.xpb.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpb.aop.annotation.CurrentLimiting;
import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import com.xpb.service.MailService;
import com.xpb.utils.RedisCache;
import com.xpb.utils.RegexUtil;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.RegexEnum;
import com.xpb.utils.enums.ResponseCode;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/authCode")
public class AuthCodeController {

    @Autowired
    RedisCache redisCache;
    @Autowired
    MailService mailService;
    @Autowired
    UserMapper userMapper;
    @Autowired
    private TemplateEngine templateEngine;

    @Value("${authcode.image.expire-time}")
    private int imageAuthcodeExpireTime;
    @Value("${authcode.email.expire-time}")
    private int emailAuthcodeExpireTime;

    @GetMapping("/getImageAuthCode")
    @CurrentLimiting
    public void getImageAuthCode(HttpServletResponse response) throws IOException {
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200,100);
        String uuid= UUID.randomUUID().toString();
        response.setHeader("Pragma","no-cache");
        response.setHeader("Cache-Control","no-cache");
        response.setDateHeader("Expires",0);
        response.setHeader("X-Captcha-UUID",uuid);
        response.setContentType("image/png");
        String code=lineCaptcha.getCode();
        redisCache.setCacheObject(uuid,code,imageAuthcodeExpireTime, TimeUnit.MINUTES);
        lineCaptcha.write(response.getOutputStream());
    }
    @PostMapping("/getEmailAuthCode")
    @CurrentLimiting
    @GlobalInterceptor(checkParams = true)
    public ResponseResult getEmailAuthCode(@RequestParam("email") @VerifyParam(regexVerify = RegexEnum.EMAIL) String email){
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper();
        wrapper.eq(User::getEmail,email);
        User user = userMapper.selectOne(wrapper);
        if (user == null)
            return new ResponseResult(400,"提供的邮件不存在");
        String authCode=generateRandomString(6);
        Context context=new Context();
        context.setVariable("project","SecurityPan");
        context.setVariable("author","cascadeBo");
        context.setVariable("authcode",authCode);
        String emailTemplate = templateEngine.process("AuthCode", context);
        redisCache.setCacheObject(email,authCode,emailAuthcodeExpireTime, TimeUnit.MINUTES);
        try {
            mailService.sendHtmlMail(email,"SecurityPan 验证码",emailTemplate);
        } catch (MessagingException e) {
            return new ResponseResult(500,"邮件发送失败,请检查邮件地址");
        }
        return new ResponseResult(200,"验证码发送成功请检查你的邮箱");
    }

    public String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        return new String(text);
    }
}
