package com.xpb.utils.enums;

import java.util.Arrays;
import java.util.HashSet;

public enum FileCategoryEnum {
    VIDEO(1,"video",new HashSet<String>(Arrays.asList("mp4","avi","mov","MKV","mpeg","mpg")),"视频"),
    MUSIC(2,"music",new HashSet<String>(Arrays.asList("mp3","wav","flac")),"音乐"),
    IMAGE(3,"image",new HashSet<String>(Arrays.asList("jpeg","jpg","png","gif","tiff","svg")),"图片"),
    DOC(4,"doc",new HashSet<String>(Arrays.asList("doc","docx","ppt","pdf","txt","xls","xlsx","csv","latex")),"文档"),
    OTHER(5,"other",null,"其它");


    private Integer category;
    private String code;

    private HashSet<String> suffix;
    private String description;

    FileCategoryEnum(Integer category, String code, HashSet<String> set ,String description) {
        this.category = category;
        this.code = code;
        this.description = description;
        this.suffix=set;
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

    public HashSet<String> getSuffix(){
        return suffix;
    }
}
