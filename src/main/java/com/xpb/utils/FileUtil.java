package com.xpb.utils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;

@Slf4j
public class FileUtil {
    //向流中传输文件
    public static void readFile(OutputStream outputStream, String filePath){
        FileInputStream fileInputStream=null;
        try {
            fileInputStream=new FileInputStream(filePath);
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = fileInputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,len);
            }
            outputStream.flush();
        } catch (FileNotFoundException e) {
            log.error("出问题了文件不存在QAQ");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("IO错误");
            throw new RuntimeException(e);
        }finally {
            if (fileInputStream!=null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败");
                    throw new RuntimeException(e);
                }
            }
            if (outputStream!=null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    log.error("关闭输出流失败");
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public static File createFile(String filePath){
        File file=new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("文件创建失败");
                throw new RuntimeException(e);
            }
        }else {
            log.error("文件已经存在");
        }
        return file;
    }
    public static void deleteFile(String filePath){
        File file=new File(filePath);
        if (file.exists()){
            file.delete();
        }
    }
    public static void saveFile(InputStream inputStream,String filePath){
        FileOutputStream fileOutputStream=null;
        File file=createFile(filePath);
        try {
            fileOutputStream=new FileOutputStream(file);
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = inputStream.read(buffer))!=-1){
                fileOutputStream.write(buffer,0,len);
            }
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            log.error("文件不存在");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("文件写入失败");
            throw new RuntimeException(e);
        }finally {
            if (inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("关闭输入流失败");
                    throw new RuntimeException(e);
                }
            }
            if (fileOutputStream!=null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.error("关闭文件输出流失败");
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public static boolean fileExist(String filePath){
        File file=new File(filePath);
        return file.exists();
    }
}
