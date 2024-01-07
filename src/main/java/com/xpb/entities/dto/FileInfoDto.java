package com.xpb.entities.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.xpb.entities.FileInfo;
import lombok.Data;

import java.util.Date;

@Data
public class FileInfoDto {
    private String fileId;
    private String fileMd5;
    private String filePid;
    private long  fileSize;
    private String fileName;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    private int folderType;
    private int fileCategory;
    private String fileType;

    public FileInfoDto(FileInfo fileInfo){
        this.fileId=fileInfo.getFileId();
        this.fileMd5=fileInfo.getFileMd5();
        this.filePid=fileInfo.getFilePid();
        this.fileSize=fileInfo.getFileSize();
        this.fileName=fileInfo.getFileName();
        this.createTime=fileInfo.getCreateTime();
        this.folderType=fileInfo.getFolderType();
        this.fileCategory=fileInfo.getFileCategory();
        this.fileType=fileInfo.getFileType();
    }
}
