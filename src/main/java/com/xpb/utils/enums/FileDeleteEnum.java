package com.xpb.utils.enums;

public enum FileDeleteEnum {
    DELETE(0,"已删除"),
    RECYCLE(1,"进入回收站"),
    USING(2,"正在使用");

    private Integer code;
    private String describe;

    FileDeleteEnum(Integer code, String describe) {
        this.code = code;
        this.describe = describe;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}
