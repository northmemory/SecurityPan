package com.xpb.entities.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownLoadRedisDto {
    String fileName;
    String filePath;
}
