package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpb.entities.User;
import com.xpb.mapper.UserMapper;
import com.xpb.service.UserService;
import com.xpb.utils.FileUtil;
import com.xpb.utils.ImageUtil;
import com.xpb.utils.ResponseResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Value("${File.avatar-path}")
    private String avatarPath;
    @Autowired
    UserMapper mapper;

    @Override
    public ResponseResult getAvatar(HttpServletResponse response, String userId) {
        String userAvatarPath=avatarPath+"\\"+userId+".jpg";
        File avatar=new File(userAvatarPath);
        if(!avatar.exists()){
            return new ResponseResult(500,"用户头像不存在捏");
        }
        try {
            FileUtil.readFile(response.getOutputStream(),userAvatarPath);
        } catch (IOException e) {
            log.error("获取头像输入流失败");
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ResponseResult uploadAvatar(String userId, MultipartFile avatar) {
        if(!ImageUtil.isImage(avatar))
            return new ResponseResult(500,"传入的图片格式错误");
        String absPath=avatarPath+"\\"+userId+".jpg";
        if (FileUtil.fileExist(absPath))
            FileUtil.deleteFile(absPath);
        try {
            FileUtil.createFile(absPath);
            if (ImageUtil.detectFileFormat(avatar)!="jpg") {
                ImageUtil.convertToJpg(avatar.getInputStream(),absPath);
            }
            FileUtil.saveFile(avatar.getInputStream(),absPath);
        } catch (IOException e) {
            log.error("头像保存失败");
            return new ResponseResult(500,"头像保存失败");
        }
        LambdaUpdateWrapper<User> wrapper=new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId,userId).set(User::getAvatarDir,absPath);
        return new ResponseResult(200,"头像保存成功");
    }
}
