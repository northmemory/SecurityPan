package com.xpb.controller;

import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.entities.LoginUser;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.service.FileService;
import com.xpb.utils.FileUtil;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.FileCategoryEnum;
import com.xpb.utils.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/file")
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
    public ResponseResult getFileCover(@AuthenticationPrincipal LoginUser loginUser,
                                       @PathVariable("fileId") String fileId,
                                       HttpServletResponse response){
        if (fileId==null || fileId.equals(""))
            return new ResponseResult(500,"fileId不可以为空");
        File cover = fileService.getCover(fileId, loginUser.getUser().getUserId());
        if (cover==null)
            return new ResponseResult(500,"所请求的文件的缩略图不存在");
        response.setHeader("Cache-Control","max-age=2592000");
        String suffix= FileUtil.getFileSuffix(cover.getName());
        String contextType="image/"+suffix;
        response.setContentType(contextType);

    }
}
