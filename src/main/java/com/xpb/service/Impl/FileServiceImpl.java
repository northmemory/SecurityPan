package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xpb.entities.FileInfo;
import com.xpb.entities.User;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.mapper.UserMapper;
import com.xpb.service.FileService;
import com.xpb.utils.FileUtil;
import com.xpb.utils.RedisCache;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.enums.FileDeleteEnum;
import com.xpb.utils.enums.FileStatusEnum;
import com.xpb.utils.enums.FileUploadStatusEnum;
import com.xpb.utils.enums.ResponseCode;
import com.xpb.utils.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisCache redisCache;

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

    @Value("${File.temp-path}")
    private String tempPath;
    @Value("${File.file-storage-path}")
    private String fileStoragePath;
    @Override
    @Transactional
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
        if(chunkIndex==0) {
            LambdaQueryWrapper<FileInfo> wrapperMd5 = new LambdaQueryWrapper();
            wrapperMd5.eq(FileInfo::getFileMd5, fileMd5);
            wrapperMd5.eq(FileInfo::getStatus, FileStatusEnum.USING.getCode());
            FileInfo fileInfo = fileInfoMapper.selectOne(wrapperMd5);
            //文件已经存在走秒传逻辑
            if (fileInfo != null) {
                if (user.getTotalSpace() - user.getUsedSpace() < fileInfo.getFileSize()) {
                    throw new BusinessException(ResponseCode.CODE_904.getCode(), ResponseCode.CODE_904.getMsg());
                }
                saveFileInfo(userId, fileId, fileName, filePid, fileMd5, curDate, fileInfo);
                fileUploadResultDto.setStatus(FileStatusEnum.USING.getCode());
                fileUploadResultDto.setStatusDescription(FileStatusEnum.USING.getDescription());
                //更新用户使用空间
                LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(User::getUserId, userId);
                updateWrapper.set(User::getUsedSpace, user.getUsedSpace() + fileInfo.getFileSize());
                int update = userMapper.update(null, wrapper);
                if (update == 0) {
                    throw new BusinessException(ResponseCode.CODE_500.getCode(), ResponseCode.CODE_500.getMsg() + ":用户空间信息更新失败");
                }
                return fileUploadResultDto;
            }
        }
        //判断磁盘空间
        if (getRedisTempSize(userId,fileId)+user.getUsedSpace()+file.getSize()>user.getTotalSpace())
            throw new BusinessException(ResponseCode.CODE_904.getCode(),ResponseCode.CODE_904.getMsg());
        //暂存临时目录,如果目录不存在就新建目录
        String currentUserFolderName=fileId+userId;
        String tempFolderName=tempPath+"/"+currentUserFolderName;
        File tempFolder=new File(tempFolderName);
        if (!tempFolder.exists()){
            tempFolder.mkdir();
        }
        //保存分片文件
        String chunkFileName=tempFolderName+"/"+chunkIndex;
        if (!FileUtil.saveFile(file,chunkFileName)){
            throw new BusinessException(ResponseCode.CODE_500.getCode(),ResponseCode.CODE_500.getMsg());
        }
        if (chunkIndex<chunks-1){
            fileUploadResultDto.setStatus(FileUploadStatusEnum.UPLOADING.getCode());
            fileUploadResultDto.setStatusDescription(FileUploadStatusEnum.UPLOADING.getDescription());
            saveRedisTempSize(userId,fileId,file.getSize());
            return fileUploadResultDto;
        } else if (chunkIndex==chunks-1) {
            fileUploadResultDto.setStatus(FileUploadStatusEnum.UPLOAD_FININSED.getCode());
            fileUploadResultDto.setStatusDescription(FileUploadStatusEnum.UPLOAD_FININSED.getDescription());
            FileUtil.mergeFile(tempFolderName,chunks,fileStoragePath+'/'+userId,fileName);
            deleteRedisTempSize(userId,fileId);
            //删除缓存文件夹和里面的内容
            FileUtil.deleteFolder(tempFolderName);
            //保存到数据库中,保存的信息不够，明天要加
            FileInfo fileInfo=new FileInfo();
            saveFileInfo(userId, fileId, fileName, filePid, fileMd5, curDate, fileInfo);
            return fileUploadResultDto;
        }
        return fileUploadResultDto;
    }

    private void saveFileInfo(String userId, String fileId, String fileName, String filePid, String fileMd5, Date curDate, FileInfo fileInfo) throws BusinessException {
        fileInfo.setFileId(fileId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileMd5(fileMd5);
        fileInfo.setCreateTime(curDate);
        fileInfo.setUserId(userId);
        fileName = renameIfFileNameExist(userId, fileName, filePid);
        fileInfo.setFileName(fileName);
        fileInfo.setDelFlag(FileDeleteEnum.USING.getCode());
        fileInfo.setStatus(FileStatusEnum.USING.getCode());
        int insert = fileInfoMapper.insert(fileInfo);
        if (insert == 0) {
            throw new BusinessException(ResponseCode.CODE_500.getCode(), ResponseCode.CODE_500.getMsg() + ":文件信息更新失败");
        }
    }

    //如果文件名重复命名为filename(i).xxx
    public String renameIfFileNameExist(String userId,String fileName,String filePid){
       LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper<>();
       wrapper.eq(FileInfo::getFileName,fileName)
               .eq(FileInfo::getFilePid,filePid)
               .eq(FileInfo::getUserId,userId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo!=null){
            String filePrefix=fileName.split("\\.")[0];
            String fileSuffix=fileName.split("\\.")[1];
            for(int i=1;i<Integer.MAX_VALUE;i++){
                String newFileName=filePrefix+"("+i+")."+fileSuffix;
                LambdaQueryWrapper<FileInfo> wrapperI=new LambdaQueryWrapper<>();
                wrapperI.eq(FileInfo::getFileName,newFileName)
                        .eq(FileInfo::getFilePid,filePid)
                        .eq(FileInfo::getUserId,userId);
                FileInfo fileInfoI = fileInfoMapper.selectOne(wrapperI);
                if (fileInfoI==null){
                    return newFileName;
                }
            }
        }
        return fileName;
    }
    @Value("${redis.redis-key.user-temp-size}")
    String redisKeyPre;
    private Long getRedisTempSize(String userId,String fileId){
        String redisKey=redisKeyPre+":"+userId+fileId;
        Object size = redisCache.getCacheObject(redisKey);
        if (size==null)
            return 0L;
        if (size instanceof Integer)
            return ((Integer) size).longValue();
        else if (size instanceof Long)
            return (Long) size;
        return null;
    }
    @Value("${redis.redis-key-expire.user-temp-size-expire}")
    Integer redisKeyExpireTime;
    private void saveRedisTempSize(String userId,String fileId,Long fileSize){
        String redisKey=redisKeyPre+":"+userId+fileId;
        Long currentSize=redisCache.getCacheObject(redisKey);
        if (currentSize==null)
            redisCache.setCacheObject(redisKey,fileSize,redisKeyExpireTime, TimeUnit.MINUTES);
        else
            redisCache.setCacheObject(redisKey,fileSize+currentSize,redisKeyExpireTime, TimeUnit.MINUTES);
    }
    private boolean deleteRedisTempSize(String userId,String fileId){
        String redisKey=redisKeyPre+":"+userId+fileId;
        return redisCache.deleteObject(redisKey);
    }

}
