package com.xpb.service;

import com.xpb.entities.FileInfo;
import com.xpb.entities.dto.FileDownLoadUrlDto;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.utils.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface FileService {
    List<FileInfoDto> loadFileListByCategory(String category, String userId);


    FileUploadResultDto upload(String userId, MultipartFile file, String fileId, String fileName
            ,String filePid,String fileMd5, Integer chunkIndex, Integer chunks) throws BusinessException;

    String getCover(String fileId, String userId);

    FileInfo createFolder(String userId,String filePid,String fileName) throws BusinessException;

    List<FileInfoDto> getFolderInfo(String userId, String folderId, Integer pageNum);

    boolean renameFile(String fileId,String userId,String newFileName) throws BusinessException;

    FileDownLoadUrlDto generateDownloadUrl(String fileId, String userId) throws BusinessException;

    public void download(HttpServletRequest request, HttpServletResponse response, String code) throws BusinessException, IOException;
}
