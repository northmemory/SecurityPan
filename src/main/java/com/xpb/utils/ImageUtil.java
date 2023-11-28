package com.xpb.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
public class ImageUtil {
    //检查MultipartFile文件格式
    public static boolean isImage(MultipartFile file){
        String fileFormat = detectFileFormat(file);
        return (fileFormat=="jpg" || fileFormat=="png" || fileFormat=="jpeg");
    }

    public static String detectFileFormat(MultipartFile file) {
        // 判断文件格式的逻辑
        // 在这里，你可以根据文件的头部字节来判断文件格式
        // 例如，可以通过比较文件的魔数（magic number）来判断文件格式
        InputStream inputStream = null;
        byte[] headerBytes = new byte[8];
        try {
            inputStream = file.getInputStream();
            inputStream.read(headerBytes);
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileFormat = "unknown";
        String name=file.getOriginalFilename();
        String extension=(name.split("\\.").length==2)? name.split("\\.")[1] : null;

        String headerHex = byteArrayToHexString(headerBytes);

        if (headerHex.startsWith("89504E470D0A1A0A") && extension.equals("png")) {
            fileFormat = "png";
        } else if (headerHex.startsWith("FFD8FF") && extension.equals("jpeg")) {
            fileFormat = "jpeg";
        } else if (headerHex.startsWith("FFD8FF") && extension.equals("jpg")) {
            fileFormat = "jpg";
        }

        return fileFormat;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();

        for (byte b : bytes) {
            stringBuilder.append(String.format("%02X", b));
        }

        return stringBuilder.toString();
    }

    public static void convertToJpg(InputStream inputStream, String outputPath) {
        // 读取输入流为 BufferedImage
        BufferedImage image = null;
        try {
            image = ImageIO.read(inputStream);
            // 创建输出文件路径
            Path outputPathFile = Path.of(outputPath);

            // 写入 BufferedImage 为 JPG 文件
            ImageIO.write(image, "jpg", Files.newOutputStream(outputPathFile, StandardOpenOption.CREATE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
