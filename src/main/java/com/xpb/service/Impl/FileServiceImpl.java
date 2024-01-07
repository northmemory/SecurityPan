package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xpb.entities.FileInfo;
import com.xpb.entities.User;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.mapper.UserMapper;
import com.xpb.service.FileService;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.FileDeleteEnum;
import com.xpb.utils.enums.FileStatusEnum;
import com.xpb.utils.enums.ResponseCode;
import com.xpb.utils.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    FileInfoMapper fileInfoMapper;
    @Autowired
    UserMapper userMapper;
    @Override
    public List<FileInfoDto> loadFileListByCategory(String category, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper();
        wrapper.eq(FileInfo::getUserId,userId);
        wrapper.eq(FileInfo::getDelFlag, FileDeleteEnum.USING);
        wrapper.orderByDesc(FileInfo::getCreateTime);
        List<FileInfo> fileInfos = fileInfoMapper.selectList(wrapper);
        List<FileInfoDto> fileInfoDtos =new ArrayList<>();
        for (FileInfo fileInfo : fileInfos) {
            fileInfoDtos.add(new FileInfoDto(fileInfo));
        }
        return fileInfoDtos;
    }

    @Override
    public FileUploadResultDto upload(String userId, MultipartFile file, String fileId, String fileName,
                                      String filePid, String fileMd5, Integer chunkIndex, Integer chunks) throws BusinessException {
        FileUploadResultDto fileUploadResultDto = new FileUploadResultDto();
        if (fileId==null || fileId.isEmpty()){
            fileId = String.valueOf(IdWorker.getId());
        }
        fileUploadResultDto.setFileId(fileId);
        Date curDate=new Date();
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        User user = userMapper.selectById(userId);
        //检查第一次上传时文件md5是否已经存在于数据库中
        if(chunkIndex==0){
            LambdaQueryWrapper<FileInfo> wrapperMd5=new LambdaQueryWrapper();
            wrapperMd5.eq(FileInfo::getFileMd5,fileMd5);
            wrapperMd5.eq(FileInfo::getStatus, FileStatusEnum.USING.getCode());
            FileInfo fileInfo = fileInfoMapper.selectOne(wrapperMd5);
            if (fileInfo!=null){
                if (user.getTotalSpace()- user.getUsedSpace()< fileInfo.getFileSize()){
                    throw new BusinessException(ResponseCode.CODE_904.getCode(),ResponseCode.CODE_904.getMsg());
                }
                fileInfo.setFileId(fileId);
                fileInfo.setFilePid(filePid);
                fileInfo.setFileMd5(fileMd5);
                fileInfo.setCreateTime(curDate);
                fileInfo.setUserId(userId);
                fileName=null;
                fileInfo.setFileName(fileName);
                fileInfo.setDelFlag(FileDeleteEnum.USING.getCode());
                fileInfo.setStatus(FileStatusEnum.USING.getCode());
                fileUploadResultDto.setStatus(FileStatusEnum.USING.getCode());
                fileUploadResultDto.setStatusDescription(FileStatusEnum.USING.getDescription());
                return fileUploadResultDto;
            }
        }
        return null;
    }

}
