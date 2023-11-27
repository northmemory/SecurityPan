package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.utils.FileUtil;
import com.xpb.utils.ResponseResult;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Value("${File.avatar-path}")
    private String avatarPath;

    @RequestMapping("/avatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult getAvatar(HttpServletResponse response, @VerifyParam @PathVariable("userId") String userId){
        String userAvatarPath=avatarPath+"\\"+userId+".jpg";
        File avatar=new File(userAvatarPath);
        if(!avatar.exists()){
            return new ResponseResult(500,"用户头像不存在捏");
        }
        response.setContentType("image/jpg");
        response.setHeader("Cache-Control","max-age=3600");
        FileUtil.readFile(response,userAvatarPath);
        return null;
    }
}
