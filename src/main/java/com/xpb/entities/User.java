package com.xpb.entities;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_info")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private String userId;
    private String password;
    private String nickname;
    private String email;
    private String qqOpenId;
    private String avatarDir;
    @TableField(fill = FieldFill.INSERT)
    private Date registrationTime;
    private Date lastLoginTime;
    private boolean status;
    private long totalSpace;
    private long usedSpace;
}
