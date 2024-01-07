package com.xpb.utils.enums;

public enum FileUploadStatusEnum {
    UPLOAD_SECOND(0,"秒传"),
    UPLOADING(1,"上传中"),
    UPLOAD_FININSED(2,"上传完成");
    Integer code;
    String description;
    FileUploadStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


}
