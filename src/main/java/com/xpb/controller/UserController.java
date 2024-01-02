package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.LoginUser;
import com.xpb.service.UserService;
import com.xpb.utils.FileUtil;
import com.xpb.utils.ResponseResult;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/avatar")
    public ResponseResult getAvatar(HttpServletResponse response, @AuthenticationPrincipal LoginUser loginUser){
        response.setHeader("Cache-Control","max-age=3600");
        String userId=loginUser.getUser().getUserId();
        return userService.getAvatar(response,userId);
    }

    @PostMapping("/uploadAvatar")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult uploadAvator(@RequestParam("avatar") @VerifyParam MultipartFile avatar,
                                       @AuthenticationPrincipal LoginUser loginUser){
        String userId=loginUser.getUser().getUserId();
        return userService.uploadAvatar(userId,avatar);
    }

}
