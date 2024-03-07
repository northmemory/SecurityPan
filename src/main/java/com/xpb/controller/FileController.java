package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.FileInfo;
import com.xpb.entities.LoginUser;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.entities.dto.FolderDto;
import com.xpb.service.FileService;
import com.xpb.utils.FileUtil;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.FileCategoryEnum;
import com.xpb.utils.enums.ResponseCode;
import com.xpb.utils.exceptions.BusinessException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Autowired
    FileService fileService;
    @GetMapping ("/loadFileList/{fileCateGory}")
    public ResponseResult loadFileList(@AuthenticationPrincipal LoginUser loginUser ,@PathVariable("fileCateGory") String cateGory){
        FileCategoryEnum fileCategory = FileCategoryEnum.getByCode(cateGory);
        if (fileCategory==null)
            return new ResponseResult(500,"文件类别不存在");
        String userId = loginUser.getUser().getUserId();
        return new ResponseResult(200,fileService.loadFileListByCategory(cateGory,userId));
    }

    @PostMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseResult uploadFile(@AuthenticationPrincipal LoginUser loginUser,
                                     String fileId,
                                     MultipartFile file,
                                     @VerifyParam  String fileName,
                                     @VerifyParam  String filePid,
                                     @VerifyParam  String fileMd5,
                                     @VerifyParam  Integer chunkIndex,
                                     @VerifyParam  Integer totalChunks
                                     )  {
        String userId=loginUser.getUser().getUserId();

        FileUploadResultDto uploadResult = null;
        try {
            uploadResult = fileService.upload(userId, file, fileId, fileName, filePid, fileMd5, chunkIndex, totalChunks);
        } catch (BusinessException e) {
            return new ResponseResult(e.getWrongCode(),e.getMessage());
        }
        return new ResponseResult(200,uploadResult);
    }
    @GetMapping("/getFileCover/{fileId}")
    public void getFileCover(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable("fileId") String fileId,
                                       HttpServletResponse response){
        if (fileId==null || fileId.equals(""))
            return;
        String cover = fileService.getCover(fileId, loginUser.getUser().getUserId());
        if (cover==null)
            return;
        response.setHeader("Cache-Control","max-age=2592000");
        String suffix= FileUtil.getFileSuffix(cover);
        String contextType="image/"+suffix;
        response.setContentType(contextType);
        try {
            FileUtil.readFile(response.getOutputStream(),cover);
        } catch (IOException e) {
            log.error("用户"+loginUser.getUser().getUserId()+"的"+fileId+"文件封面IO错误");
        }
    }
    @PostMapping("/createFolder")
    @GlobalInterceptor
    public ResponseResult createFolder(@AuthenticationPrincipal LoginUser loginUser,
                             String folderName,
                             String filePid){
        if (folderName==null || folderName.isEmpty())
            folderName="新建文件夹";
        if (filePid==null || filePid.isEmpty())
            filePid="0";
        String userId = loginUser.getUser().getUserId();
        try {
            FileInfo folder = fileService.createFolder(userId, filePid, folderName);
            return new ResponseResult<>(ResponseCode.CODE_200.getCode(), new FolderDto(folder));
        } catch (BusinessException e) {
            return new ResponseResult<>(ResponseCode.CODE_500.getCode(), e.getMessage());
        }
    }

    @GetMapping("/getFolderInfo/{folderId}")
    @GlobalInterceptor
    public ResponseResult getFolderInfo(@AuthenticationPrincipal LoginUser loginUser,
                                                 @VerifyParam @PathVariable("folderId") String folderId){
        List<FileInfoDto> folderInfo = fileService.getFolderInfo(loginUser.getUser().getUserId(), folderId);
        return new ResponseResult<>(ResponseCode.CODE_200.getCode(), folderInfo);
    }
}
