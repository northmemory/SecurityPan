package com.xpb.entities.dto;

import com.xpb.entities.FileInfo;
import lombok.Data;

@Data
public class FolderDto {
    private String fileName;
    private String fileId;
    private String filePid;
    private Integer fileFolderType;

    public FolderDto(FileInfo fileInfo){
        this.fileName=fileInfo.getFileName();
        this.fileId=fileInfo.getFileId();
        this.filePid=fileInfo.getFilePid();
        this.fileFolderType=fileInfo.getFolderType();
    }
}
