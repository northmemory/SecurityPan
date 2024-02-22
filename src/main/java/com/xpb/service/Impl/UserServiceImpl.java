package com.xpb.service.Impl;

import cn.hutool.system.UserInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserId,userId);
        String userAvatarPath = mapper.selectOne(queryWrapper).getAvatarDir();
        String type = userAvatarPath.split("\\.")[1];
        type=(type.equals("jpg"))? "jpeg":type;
        response.setContentType("image/"+type);
        File avatar=new File(userAvatarPath);
        if(!avatar.exists()){
            return new ResponseResult(500,"用户头像不存在捏");
        }
        if (!FileUtil.readFile(response,userAvatarPath)){
            log.error("头像传输失败");
        }
        return null;
    }

    @Override
    public ResponseResult uploadAvatar(String userId, MultipartFile avatar) {
        if(!ImageUtil.isImage(avatar))
            return new ResponseResult(500,"传入的图片格式错误");
        String absPath=avatarPath+"\\"+userId+"."+ImageUtil.detectFileFormat(avatar);
        if (FileUtil.fileExist(absPath))
            FileUtil.deleteFile(absPath);

        if (FileUtil.createFile(absPath)==null || FileUtil.saveFile(avatar,absPath) ){
            log.error("头像保存失败");
            return new ResponseResult(500,"用户头像上传失败");
        }
        LambdaUpdateWrapper<User> wrapper=new LambdaUpdateWrapper<>();
        wrapper.eq(User::getUserId,userId).set(User::getAvatarDir,absPath);
        mapper.update(null,wrapper);
        return new ResponseResult(200,"头像保存成功");
    }

    @Override
    public ResponseResult getTotalSize(String userId) {
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        wrapper.select(User::getTotalSpace);
        User user = mapper.selectOne(wrapper);
        return new ResponseResult(200,user.getTotalSpace());
    }

    @Override
    public ResponseResult getUsedSize(String userId) {
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        wrapper.select(User::getUsedSpace);
        User user = mapper.selectOne(wrapper);
        return new ResponseResult(200,user.getUsedSpace());
    }
}
