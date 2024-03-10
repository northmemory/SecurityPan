package com.xpb.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpb.entities.FileInfo;
import com.xpb.entities.User;
import com.xpb.entities.dto.FileDownLoadRedisDto;
import com.xpb.entities.dto.FileDownLoadUrlDto;
import com.xpb.entities.dto.FileInfoDto;
import com.xpb.entities.dto.FileUploadResultDto;
import com.xpb.listener.message.FileMergingMessage;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.mapper.UserMapper;
import com.xpb.service.FileService;
import com.xpb.utils.Constants;
import com.xpb.utils.FileUtil;
import com.xpb.utils.RedisCache;
import com.xpb.utils.StringUtil;
import com.xpb.utils.enums.*;
import com.xpb.utils.exceptions.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
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

    @Resource
    private RabbitTemplate rabbitTemplate;

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
    @Value("${mq-name.file-merge}")
    private String fileMergeQueue;

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public FileUploadResultDto upload(String userId, MultipartFile file, String fileId, String fileName,
                                      String filePid, String fileMd5, Integer chunkIndex, Integer chunks) throws BusinessException {
        boolean accident=false;
        try{
            FileUploadResultDto fileUploadResultDto = new FileUploadResultDto();
            if (fileId==null || fileId.isEmpty()){
                fileId = String.valueOf(IdWorker.getId());
            }
            fileUploadResultDto.setFileId(fileId);
            Date curDate=new Date();
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
                    fileUploadResultDto.setStatus(FileUploadStatusEnum.UPLOAD_SECOND.getCode());
                    fileUploadResultDto.setStatusDescription(FileUploadStatusEnum.UPLOAD_SECOND.getDescription());
                    //更新用户使用空间
                    LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(User::getUserId, userId);
                    updateWrapper.set(User::getUsedSpace, user.getUsedSpace() + fileInfo.getFileSize());
                    int update = userMapper.update(null, updateWrapper);
                    if (update == 0) {
                        throw new BusinessException(ResponseCode.CODE_500.getCode(), ResponseCode.CODE_500.getMsg() + ":用户空间信息更新失败");
                    }
                    return fileUploadResultDto;
                }
            }
            //判断磁盘空间
            if (getRedisTempSize(userId,fileId)+user.getUsedSpace()+file.getSize()>user.getTotalSpace()){
                accident=true;
                throw new BusinessException(ResponseCode.CODE_904.getCode(),ResponseCode.CODE_904.getMsg());
            }
            //暂存临时目录,如果目录不存在就新建目录
            String currentUserFolderName=fileId+userId;
            String tempFolderName=tempPath+"/"+currentUserFolderName;
            File tempFolder=new File(tempFolderName);
            if (!tempFolder.exists()){
                tempFolder.mkdir();
            }
            //保存分片文件
            String chunkFileName=tempFolderName+"/"+chunkIndex;
            FileUtil.saveFile(file.getInputStream(),chunkFileName);
            if (chunkIndex<chunks-1){
                fileUploadResultDto.setStatus(FileUploadStatusEnum.UPLOADING.getCode());
                fileUploadResultDto.setStatusDescription(FileUploadStatusEnum.UPLOADING.getDescription());
                saveRedisTempSize(userId,fileId,file.getSize());
                return fileUploadResultDto;
            } else if (chunkIndex==chunks-1) {
                fileUploadResultDto.setStatus(FileUploadStatusEnum.UPLOAD_FININSED.getCode());
                fileUploadResultDto.setStatusDescription(FileUploadStatusEnum.UPLOAD_FININSED.getDescription());
                saveRedisTempSize(userId,fileId,file.getSize());
                //计算存储数据库所需的相关信息
                SimpleDateFormat format=new SimpleDateFormat("yyyyMM");
                String mouth=format.format(new Date());
                fileName = renameIfFileNameExist(userId, fileName, filePid);
                String filePath=fileStoragePath+'/'+mouth+'/'+fileName;
                String[] fileNameSlice=fileName.split("\\.");
                String suffix=fileNameSlice[fileNameSlice.length-1];
                Integer type=getFileCategory(suffix);
                Long size=redisCache.getCacheObject(redisKeyPre+':'+userId+fileId);
                //最后一个分片上传保存到数据库中
                FileInfo fileInfo=new FileInfo();
                fileInfo.setFileId(fileId);
                fileInfo.setFilePid(filePid);
                fileInfo.setFileMd5(fileMd5);
                fileInfo.setCreateTime(curDate);
                fileInfo.setUserId(userId);
                fileInfo.setFileName(fileName);
                fileInfo.setDelFlag(FileDeleteEnum.USING.getCode());
                fileInfo.setStatus(FileStatusEnum.TRANSFER.getCode());
                fileInfo.setFilePath(filePath);
                fileInfo.setFileCategory(type);
                fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
                fileInfo.setFileSize(size);
                //删除redis的tempSize
                deleteRedisTempSize(userId,fileId);
                int insert = fileInfoMapper.insert(fileInfo);
                if (insert == 0) {
                    accident=true;
                    throw new BusinessException(ResponseCode.CODE_500.getCode(), ResponseCode.CODE_500.getMsg() + ":文件信息更新失败");
                }
                //修改User剩余空间
                LambdaUpdateWrapper<User> wrapper=new LambdaUpdateWrapper<>();
                wrapper.eq(User::getUserId,userId);
                wrapper.set(User::getUsedSpace,user.getUsedSpace()-size);
                userMapper.update(null,wrapper);
                //合并文件并，删除缓存文件夹和里面的内容，未来改成异步操作
                String msgId=UUID.randomUUID().toString();
                CorrelationData cd=new CorrelationData(msgId);
                FileMergingMessage fileMergingMessage = new FileMergingMessage(fileId, tempFolderName, chunks, filePath,userId);
                rabbitTemplate.convertAndSend(fileMergeQueue, fileMergingMessage,cd);
                redisCache.setCacheObject(msgId,fileMergingMessage,3,TimeUnit.MINUTES);
                return fileUploadResultDto;
            }
            return fileUploadResultDto;
        } catch (IOException e){
            accident=true;
            log.error(userId+"的"+fileId+"文件IO异常");
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "文件传输异常");
        } finally {
            if (accident)
                FileUtil.deleteFolder(tempPath+"/"+fileId+userId);
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
    private void deleteRedisTempSize(String userId, String fileId){
        String redisKey=redisKeyPre+":"+userId+fileId;
        redisCache.deleteObject(redisKey);
    }
    /*
    * 根据文件的后缀名识别文件的种类*/
    private int getFileCategory(String suffix){
        String lower=suffix.toLowerCase();
        if (FileCategoryEnum.VIDEO.getSuffix().contains(lower))
            return FileCategoryEnum.VIDEO.getCategory();
        if (FileCategoryEnum.IMAGE.getSuffix().contains(lower))
            return FileCategoryEnum.IMAGE.getCategory();
        if (FileCategoryEnum.MUSIC.getSuffix().contains(lower))
            return FileCategoryEnum.MUSIC.getCategory();
        if (FileCategoryEnum.DOC.getSuffix().contains(lower))
            return FileCategoryEnum.DOC.getCategory();
        return FileCategoryEnum.OTHER.getCategory();
    }

    @Override
    public String getCover(String fileId, String userId) {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId,userId);
        wrapper.eq(FileInfo::getFileId,fileId);
        wrapper.select(FileInfo::getFileCover);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        String coverPath=fileInfo.getFileCover();
        if (coverPath==null)
            return null;
        else
            return coverPath;
    }

    @Override
    @Transactional
    public FileInfo createFolder(String userId, String filePid, String fileName) throws BusinessException {
        checkFileName(fileName,filePid,userId,FileFolderTypeEnum.FOLDER.getType());
        FileInfo folder=new FileInfo();
        folder.setFileId(String.valueOf(IdWorker.getId()));
        folder.setUserId(userId);
        folder.setFilePid(filePid);
        folder.setCreateTime(new Date());
        folder.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        folder.setDelFlag(FileDeleteEnum.USING.getCode());
        folder.setFileName(fileName);
        folder.setStatus(FileStatusEnum.USING.getCode());
        fileInfoMapper.insert(folder);
        return folder;
    }

    private void checkFileName(String fileName, String filePid, String userId, Integer fileFolderType) throws BusinessException {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getFileName,fileName);
        wrapper.eq(FileInfo::getUserId,userId);
        wrapper.eq(FileInfo::getFilePid,filePid);
        wrapper.eq(FileInfo::getFolderType,fileFolderType);
        Long result = fileInfoMapper.selectCount(wrapper);
        if (result > 0)
            throw new BusinessException(500,"请求创建的文件夹已经存在");
    }

    @Override
    public List<FileInfoDto> getFolderInfo(String userId, String folderId, Integer pageNum) {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId,userId);
        wrapper.eq(FileInfo::getFilePid,fileInfoMapper);
        wrapper.orderByAsc(FileInfo::getFileId);
        Page<FileInfo> page=new Page<>(pageNum-1,50);
        IPage<FileInfo> records = fileInfoMapper.selectPage(page, wrapper);
        List<FileInfo> fileInfos = records.getRecords();
        ArrayList<FileInfoDto> result=new ArrayList<>();
        fileInfos.forEach(fileInfo -> {
            result.add(new FileInfoDto(fileInfo));
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public boolean renameFile(String fileId, String userId, String newFileName) throws BusinessException {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper();
        wrapper.eq(FileInfo::getFileId,fileId).eq(FileInfo::getUserId,userId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if(fileInfo==null)
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "请求的文件不存在");
        String newName=newFileName+FileUtil.getFileSuffix(fileInfo.getFileName());
        newName = renameIfFileNameExist(userId, newName, fileInfo.getFilePid());
        LambdaUpdateWrapper<FileInfo> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileInfo::getUserId,userId)
                .eq(FileInfo::getFileId,fileId)
                .set(FileInfo::getFileName,newName);
        int update = fileInfoMapper.update(null, updateWrapper);
        return update==1;
    }

    @Override
    public FileDownLoadUrlDto generateDownloadUrl(String fileId, String userId) throws BusinessException {
        LambdaQueryWrapper<FileInfo> wrapper=new LambdaQueryWrapper();
        wrapper.eq(FileInfo::getFileId,fileId).eq(FileInfo::getFileId,fileId);
        FileInfo fileInfo = fileInfoMapper.selectOne(wrapper);
        if (fileInfo==null || fileInfo.getStatus()!=FileStatusEnum.USING.getCode())
            throw new BusinessException(ResponseCode.CODE_500.getCode(), "所请求的文件Id错误");
        String filePath=fileInfo.getFilePath();
        String randomKey= StringUtil.generateRandomString(Constants.FileUrlKeyLength);
        FileDownLoadRedisDto redisDto=new FileDownLoadRedisDto(fileInfo.getFileName(),filePath);
        redisCache.setCacheObject(randomKey,redisDto,Constants.FileUrlExpireTime,Constants.FileUrlExpireTimeunit);
        return new FileDownLoadUrlDto(fileId,randomKey);
    }

    public void download(HttpServletRequest request, HttpServletResponse response, String code)  {
        try {
            FileDownLoadRedisDto fileInfo=redisCache.getCacheObject(code);
            if (fileInfo==null){
                response.getWriter().print("没有下载权限");
                return;
            }
            String fileName=fileInfo.getFileName();
            String filePath=fileInfo.getFilePath();
            response.setContentType("application/x-msdownload; charset=UTF-8");
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0){
                fileName= URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            }else {
                fileName=new String(fileName.getBytes(StandardCharsets.UTF_8),StandardCharsets.ISO_8859_1);
            }
            response.setHeader("Content-Disposition","attachment;filename=\""+fileName+"\"");
            FileUtil.readFile(response.getOutputStream(),filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
