package com.xpb.service;

import com.xpb.utils.ResponseResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    public ResponseResult getAvatar(HttpServletResponse response, String userId);
    public ResponseResult uploadAvatar(String userId, MultipartFile avatar);

    public ResponseResult getTotalSize(String userId);
    public ResponseResult getUsedSize(String userId);
}
