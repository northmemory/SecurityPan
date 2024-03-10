package com.xpb.service;

import com.xpb.utils.exceptions.BusinessException;

import java.io.File;

public interface PreviewService {
    public File previewVideo(String fileInfo,String userId) throws BusinessException;

    public File previewFile(String fileId,String userId) throws BusinessException;
}

