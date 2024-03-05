package com.xpb.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpb.entities.FileInfo;
import com.xpb.listener.message.FileMergingMessage;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.utils.FileUtil;
import com.xpb.utils.StreamMediaUtil;
import com.xpb.utils.enums.FileCategoryEnum;
import com.xpb.utils.enums.FileStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class MergeFileListener {
    @Resource
    FileInfoMapper mapper;

    @Value("${File.file-storage-path}")
    private String fileStoragePath;
    @Value("${stream.thumbnail-width}")
    private Integer thumbnailWidth;
    @RabbitListener(queues = "simple.queue")
    @Transactional
    public void transfer(FileMergingMessage message){
        String tempFilePath= message.getTempFilePath();
        int chunks=message.getChunks();
        String targetPath=message.getTargetPath();
        String fileId=message.getFileId();
        String userId=message.getUserId();
        SimpleDateFormat format=new SimpleDateFormat("yyyyMM");
        String mouth=format.format(new Date());
        String coverPath=null;
        try {
            FileUtil.mergeFile(tempFilePath,chunks,targetPath);
            //视频文件切割
            String[] fileNameSlice=targetPath.split("\\.");
            String suffix=fileNameSlice[fileNameSlice.length-1];
            FileCategoryEnum type=FileCategoryEnum.getBySuffix(suffix);
            if (type==FileCategoryEnum.VIDEO){
                //视频文件转码切割
                StreamMediaUtil.transferVideo(fileId,targetPath);
                //生成图视频缩略图
                coverPath=fileStoragePath+"/"+mouth+"/"+fileId+userId+"-cover.png";
                StreamMediaUtil.generateThumbnailForVideo(targetPath,thumbnailWidth,coverPath);
            }else if (type==FileCategoryEnum.IMAGE){
                coverPath=targetPath.replace(".","_.");
                boolean success = StreamMediaUtil.generateThumbnailForImage(targetPath, thumbnailWidth, coverPath);
                if (!success){
                    FileUtil.copyFile(targetPath,coverPath);
                }
            }
            LambdaUpdateWrapper<FileInfo> wrapper=new LambdaUpdateWrapper<>();
            wrapper.eq(FileInfo::getFileId, fileId);
            wrapper.set(FileInfo::getStatus,FileStatusEnum.USING.getCode());
            if (coverPath!=null)
                wrapper.set(FileInfo::getFileCover,coverPath);
            mapper.update(null, wrapper);
            FileUtil.deleteFolder(tempFilePath);
        } catch (IOException e) {
            log.error("文件["+tempFilePath+"]转码失败");
            throw new RuntimeException(e);
        }
    }
}
