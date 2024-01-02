package com.xpb.utils.enums;

public enum FileCategoryEnum {
    VIDEO(1,"video","视频"),
    MUSIC(2,"music","音乐"),
    IMAGE(3,"image","图片"),
    DOC(4,"doc","文档"),
    OTHER(5,"other","其它");


    private Integer category;
    private String code;
    private String description;

    FileCategoryEnum(Integer category, String code, String description) {
        this.category = category;
        this.code = code;
        this.description = description;
    }

    public static FileCategoryEnum getByCode(String code){
        for (FileCategoryEnum value : FileCategoryEnum.values()) {
            if (value.code.equals(code))
                return value;
        }
        return null;
    }

    public Integer getCategory() {
        return category;
    }

    public String getCode() {
        return code;
    }
}
