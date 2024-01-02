package com.xpb.service;

import com.xpb.entities.FileInfo;

import java.util.List;

public interface FileService {
    List<FileInfo> loadFileListByCategory(String category);
}
