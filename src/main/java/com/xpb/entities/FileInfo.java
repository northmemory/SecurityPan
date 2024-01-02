package com.xpb.entities;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("file_info")
public class FileInfo {
    private String fileId;
    private String userId;
    private String fileMd5;
    private String filePid;
    private long  fileSize;
    private String fileName;
    private String fileCover;
    private String filePath;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    private int folderType;
    private int fileCategory;
    private String fileType;
    private int status;
    private Date recoveryTime;
    private int delFlag;
}
