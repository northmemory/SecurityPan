package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpb.entities.FileInfo;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.service.PreviewService;
import com.xpb.utils.Constants;
import com.xpb.utils.FileUtil;
import com.xpb.utils.enums.FileCategoryEnum;
import com.xpb.utils.enums.ResponseCode;
import com.xpb.utils.exceptions.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;


@Service
@Slf4j
public class PreviewServiceImpl implements PreviewService {
    @Resource
    FileInfoMapper fileInfoMapper;
    @Override
    public File previewVideo(String fileInfo, String userId) throws BusinessException{
        if (!fileInfo.endsWith("ts")){
            String fileId=fileInfo;
            LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper();
            wrapper.eq(FileInfo::getFileId,fileId);
            wrapper.eq(FileInfo::getUserId,userId);
            FileInfo fileDetail = fileInfoMapper.selectOne(wrapper);
            if(fileDetail==null)
                throw new BusinessException(ResponseCode.CODE_500.getCode(),"所请求的视频id错误");
            String filePath=fileDetail.getFilePath();
            String filePrefix= FileUtil.getFilePrefix(filePath);
            String tsPath = filePrefix.replaceFirst(Constants.FilePath, Constants.TsPath);
            String m3u8Path=tsPath+"/"+Constants.m3u8FileName;
            File m3u8File=new File(m3u8Path);
            if (!m3u8File.exists()){
                log.error("["+m3u8Path+"]所对应的文件不存在");
                throw new BusinessException(ResponseCode.CODE_500.getCode(),"服务器错误请联系管理员");
            }
            return m3u8File;
        }else {
            String fileName=fileInfo;
            String[] slice = fileName.split("_");
            if (slice.length!=2)
                throw new BusinessException(ResponseCode.CODE_500.getCode(), "所请求的文件名错误");
            String fileId=slice[0];
            LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper();
            wrapper.eq(FileInfo::getFileId,fileId);
            wrapper.eq(FileInfo::getUserId,userId);
            FileInfo fileDetail = fileInfoMapper.selectOne(wrapper);
            if(fileDetail==null)
                throw new BusinessException(ResponseCode.CODE_500.getCode(),"所请求的视频分片错误");
            String filePath=fileDetail.getFilePath();
            String filePrefix= FileUtil.getFilePrefix(filePath);
            String tsPath = filePrefix.replaceFirst(Constants.FilePath, Constants.TsPath);
            String slicePath=tsPath+"/"+fileName;
            File sliceFile=new File(slicePath);
            if (!sliceFile.exists()){
                log.error("["+slicePath+"]所对应的分片不存在");
                throw new BusinessException(ResponseCode.CODE_500.getCode(),"服务器错误请联系管理员");
            }
            return sliceFile;
        }
    }
    /*预览一般文件*/
    @Override
    public File previewFile(String fileId, String userId) throws BusinessException {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId,userId).eq(FileInfo::getFileId,fileId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo==null)
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "请求的Id错误,文件不存在");
        int type=fileInfo.getFileCategory();
        if (type != FileCategoryEnum.DOC.getCategory() && type!= FileCategoryEnum.IMAGE.getCategory())
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "该类型的文件不支持预览");
        File file=new File(fileInfo.getFilePath());
        if (!file.exists()){
            log.error("用户"+userId+"的文件["+fileId+"]不存在");
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "服务器发生错误,请联系管理员");
        }
        return file;
    }
}
