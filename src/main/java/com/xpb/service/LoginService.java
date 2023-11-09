package com.xpb.service;

import com.xpb.entities.User;
import com.xpb.utils.ResponseResult;


public interface LoginService {
    ResponseResult passwordLogin(String username,String password,String uuid,String authcode);
    ResponseResult emailLogin(String email,String emailAuthCode,String uuid,String imageAuthCode);
}
