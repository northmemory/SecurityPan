package com.xpb.utils;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class FileUtil {
    //向流中传输文件
    public static void readFile(OutputStream out, String filePath) throws IOException {
        try (out;
            FileInputStream fileInputStream=new FileInputStream(filePath)){
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = fileInputStream.read(buffer))!=-1){
                out.write(buffer,0,len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            log.error("出问题了["+filePath+"]文件不存在QAQ");
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }
    public static File createFile(String filePath) throws IOException {
        File file=new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("["+filePath+"]文件创建失败");
                throw e;
            }
        }else {
            log.error("["+filePath+"]文件已经存在");
        }
        return file;
    }
    public static void deleteFile(String filePath){
        File file=new File(filePath);
        if (file.exists()){
            file.delete();
        }
    }
    public static void saveFile(InputStream input ,String filePath) throws IOException {
        File file=createFile(filePath);
        if (file==null){
            log.error("创建["+filePath+"]文件失败");
        }
        try(input;
            FileOutputStream fileOutputStream=new FileOutputStream(file)) {
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = input.read(buffer))!=-1){
                fileOutputStream.write(buffer,0,len);
            }
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            log.error("["+filePath+"]文件不存在");
            throw e;
        } catch (IOException e) {
            log.error("["+filePath+"]文件写入失败");
            throw e;
        }
    }
    public static void mergeFile(String fileFolder,int num, String targetPath) throws IOException {
        File targetFile=new File(targetPath);
        boolean error=false;
        if (!targetFile.getParentFile().exists()){
            targetFile.getParentFile().mkdirs();
        }
        try (FileOutputStream out= new FileOutputStream(targetFile)){
            byte[] buffer=new byte[1024];
            int len=0;
            for (int i = 0; i < num ; i++) {
                File sliceI=new File(fileFolder+"/"+i);
                try (InputStream input=new FileInputStream(sliceI)){
                    while ((len = input.read(buffer))!=-1){
                        out.write(buffer,0,len);
                    }
                }catch (IOException e){
                    error=true;
                    log.error("读取分片文件["+sliceI.getName()+"]失败");
                    throw e;
                }
            }
            out.flush();
        }catch (IOException e) {
            error=true;
            log.error("创建新合并文件["+targetPath+"]失败");
            throw e;
        }finally {
            if (error)
                deleteFile(targetPath+"/"+targetPath);
        }
    }


    public static boolean fileExist(String filePath){
        File file=new File(filePath);
        return file.exists();
    }

    public static void createFolder(String filePath){
        File directory=new File(filePath);
        if(!directory.exists()){
            boolean result = directory.mkdir();
            if (!result)
                log.error("创建["+filePath+"]文件夹失败");
        }
    }

    public static void deleteFolder(String folderPath){
        Path folder = Paths.get(folderPath);
        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            log.error("删除缓存["+folderPath+"]失败");
        }
    }
}
