package com.xpb.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpb.entities.FileInfo;
import com.xpb.listener.message.FileMergingMessage;
import com.xpb.mapper.FileInfoMapper;
import com.xpb.utils.FileUtil;
import com.xpb.utils.enums.FileStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class MergeFileListener {
    @Resource
    FileInfoMapper mapper;

    @RabbitListener(queues = "simple.queue")
    @Transactional
    public void fileMerge(FileMergingMessage message){
        String tempFilePath= message.getTempFilePath();
        int chunks=message.getChunks();
        String targetPath=message.getTargetPath();
        String fileId=message.getFileId();
        try {
            FileUtil.mergeFile(tempFilePath,chunks,targetPath);
            LambdaUpdateWrapper<FileInfo> wrapper=new LambdaUpdateWrapper<>();
            wrapper.eq(FileInfo::getFileId, fileId);
            wrapper.set(FileInfo::getStatus,FileStatusEnum.USING.getCode());
            mapper.update(null, wrapper);
            FileUtil.deleteFolder(tempFilePath);
        } catch (IOException e) {
            log.error("文件["+tempFilePath+"]合并失败");
            throw new RuntimeException(e);
        }
    }
}
