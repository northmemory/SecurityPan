package com.xpb.entities.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDownLoadUrlDto {
    String fileId;
    String code;
}
