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
    public static boolean readFile(HttpServletResponse response, String filePath) {
        try (OutputStream outputStream=response.getOutputStream();
            FileInputStream fileInputStream=new FileInputStream(filePath)){
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = fileInputStream.read(buffer))!=-1){
                outputStream.write(buffer,0,len);
            }
            outputStream.flush();
            return true;
        } catch (FileNotFoundException e) {
            log.error("出问题了文件不存在QAQ");
            return false;
        } catch (IOException e) {
            log.error("IO错误");
            return false;
        }
    }
    public static File createFile(String filePath){
        File file=new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("["+filePath+"]文件创建失败");
                return null;
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
    public static boolean saveFile(MultipartFile multipartFile,String filePath){
        File file=createFile(filePath);
        if (file==null){
            log.error("创建文件失败");
            return false;
        }
        try(InputStream inputStream=multipartFile.getInputStream();
            FileOutputStream fileOutputStream=new FileOutputStream(file)) {
            byte[] buffer=new byte[1024];
            int len=0;
            while ((len = inputStream.read(buffer))!=-1){
                fileOutputStream.write(buffer,0,len);
            }
            fileOutputStream.flush();
            return true;
        } catch (FileNotFoundException e) {
            log.error("["+filePath+"]文件不存在");
            return false;
        } catch (IOException e) {
            log.error("["+filePath+"]文件写入失败");
            return false;
        }
    }
    public static boolean mergeFile(String fileFolder,int num, String targetPath, String targetFileName){
        String targetAbsolute=targetPath+"/"+targetFileName;
        File targetFile=new File(targetAbsolute);
        if (!targetFile.getParentFile().exists()){
            targetFile.getParentFile().mkdirs();
        }
        if (createFile(targetAbsolute)==null){
            log.error("创建文件失败");
            return false;
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
                    log.error("读取分片文件["+sliceI.getName()+"]失败");
                    return false;
                }
            }
            out.flush();
            return true;
        }catch (IOException e) {
            log.error("创建新合并文件["+targetAbsolute+"]失败");
            return false;
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
                log.error("创建文件夹失败");
        }
    }

    public static void deleteFolder(String folderPath) {
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
            log.error("删除缓存失败");
        }
    }
}
