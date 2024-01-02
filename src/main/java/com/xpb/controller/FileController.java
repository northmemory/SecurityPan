package com.xpb.controller;

import com.xpb.entities.LoginUser;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.FileCategoryEnum;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController {
    @RequestMapping("/loadFileList")
    public ResponseResult loadFileList(@AuthenticationPrincipal LoginUser loginUser ,String cateGory){
        return null;
    }
}
