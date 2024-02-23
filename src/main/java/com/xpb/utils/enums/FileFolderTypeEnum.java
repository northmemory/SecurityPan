package com.xpb.utils.enums;

public enum FileFolderTypeEnum {
    FILE(0,"非目录"),
    FOLDER(1,"目录");
    int type;
    String description;

    FileFolderTypeEnum(int type, String description) {
        this.type = type;
        this.description = description;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
