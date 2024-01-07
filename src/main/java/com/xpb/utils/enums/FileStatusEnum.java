package com.xpb.utils.enums;


public enum FileStatusEnum {
    TRANSFER(0,"转码中"),TRANSFER_FAIL(1,"转码失败"),USING(2,"使用中");
    Integer code;
    String description;

    FileStatusEnum(Integer code, String description) {
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
