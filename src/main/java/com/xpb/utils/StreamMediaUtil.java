package com.xpb.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class StreamMediaUtil {

    private static final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";

    private static final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%04d.ts";

    private static final String CMD_GEN_COVER_4_VIDEO="ffmpeg -i %s -y -vframes 1 -vf scale=%d:%d %s";

    private static final String CMD_GEN_COVER_4_IMAGE="ffmpeg -i %s -vf scale=%d:-1 %s -y";

    public static void transferVideo(String fileId,String videoFilePath) throws IOException {
        //创建同名目录
        String video2tsPath=Constants.TsPath+"/"+getFilePrefix(videoFilePath);
        File folder=new File(video2tsPath);
        if (!folder.exists()){
            folder.mkdirs();
        }
        String tsPath=video2tsPath+"/index.ts";
        try {
            String cmd=String.format(CMD_TRANSFER_2TS,videoFilePath,tsPath);
            CommandUtil.exec(cmd);
            cmd=String.format(CMD_CUT_TS,tsPath,folder.getPath()+"/index.m3u8",folder.getPath(),fileId);
            CommandUtil.exec(cmd);
            new File(tsPath).delete();
        } catch (IOException e) {
            log.error("["+videoFilePath+"]视频转码失败");
            throw e;
        }
    }

    public static void generateThumbnailForVideo(String sourceFile, int width,String targetFile) throws IOException {
        String cmd=String.format(CMD_GEN_COVER_4_VIDEO,sourceFile,width,width,targetFile);
        try {
            CommandUtil.exec(cmd);
        } catch (IOException e) {
            log.error("["+sourceFile+"]视频生成封面失败");
            throw e;
        }
    }
    public static boolean generateThumbnailForImage(String sourceFile, int width,String targetFile) throws IOException {
        try {
            File sourceImage=new File(sourceFile);
            BufferedImage src= ImageIO.read(sourceImage);
            int SWidth=src.getWidth();
            int SHeight=src.getHeight();
            if (SWidth < width ){
                return false;
            }
            compressImage(sourceFile,width,targetFile);
            return true;
        } catch (IOException e) {
            log.error("文件["+sourceFile+"]生成缩略图失败");
            throw e;
        }
    }
    public static void compressImage(String sourcePath, int width, String targetPath) throws IOException {
        String cmd=String.format(CMD_GEN_COVER_4_IMAGE,sourcePath,width,targetPath);
        try {
            CommandUtil.exec(cmd);
        } catch (IOException e) {
            log.error("图片["+sourcePath+"]生成缩略图失败");
            throw e;
        }
    }

    private static String getFilePrefix(String filePath){
        String[] split = filePath.split("\\.");
        StringBuilder builder=new StringBuilder();
        for (int i=0;i<split.length-1;i++)
            builder.append(split[i]);
        String fullResult=builder.toString();
        String[] split1 = fullResult.split("/");
        return split1[split1.length-2]+"/"+split1[split1.length-1];
    }
}
