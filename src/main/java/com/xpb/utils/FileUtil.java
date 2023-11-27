package com.xpb.utils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Slf4j
public class FileUtil {
    public static void readFile(HttpServletResponse response,String filePath){
        FileInputStream fileInputStream=null;
        ServletOutputStream outputStream = null;
        try {
            fileInputStream=new FileInputStream(filePath);
            outputStream = response.getOutputStream();
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = fileInputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,len);
            }
            outputStream.flush();
        } catch (FileNotFoundException e) {
            log.error("出问题了头像不存在QAQ");
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
}
