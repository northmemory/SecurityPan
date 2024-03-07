package com.xpb.service;

import com.xpb.entities.FileInfo;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.utils.exceptions.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface FileService {
    List<FileInfoDto> loadFileListByCategory(String category, String userId);


    FileUploadResultDto upload(String userId, MultipartFile file, String fileId, String fileName
            ,String filePid,String fileMd5, Integer chunkIndex, Integer chunks) throws BusinessException;

    String getCover(String fileId, String userId);

    FileInfo createFolder(String userId,String filePid,String fileName) throws BusinessException;

    List<FileInfoDto> getFolderInfo(String userId, String folderId);
}
