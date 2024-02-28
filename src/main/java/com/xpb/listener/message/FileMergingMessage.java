package com.xpb.listener.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileMergingMessage {
    String fileId;
    String tempFilePath;
    int chunks;
    String targetPath;
}
