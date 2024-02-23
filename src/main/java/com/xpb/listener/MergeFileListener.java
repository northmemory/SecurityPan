package com.xpb.listener;

import com.xpb.listener.message.FileMergingMessage;
import com.xpb.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class MergeFileListener {

    @RabbitListener(queues = "simple.queue")
    public void fileMerge(FileMergingMessage message){
        String tempFilePath= message.getTempFilePath();
        int chunks=message.getChunks();
        String targetPath=message.getTargetPath();
        try {
            FileUtil.mergeFile(tempFilePath,chunks,targetPath);
        } catch (IOException e) {
            log.error(tempFilePath+"合并失败");
        }
        FileUtil.deleteFolder(tempFilePath);
    }
}
