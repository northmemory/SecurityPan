package com.xpb.utils.enums;

public enum RegexEnum {
    EMAIL("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$","校验邮件是否满足格式"),
    PASSWORD("^[A-Za-z0-9!@#$%^&*()_+={}\\[\\]:;\"'<>,.?\\/~-]{8,16}$","密码只包含字母数字和特殊字符8-16位"),
    NO("","不进行校验");
    private String regex;
    private String desc;

    RegexEnum(String regex, String desc) {
        this.regex = regex;
        this.desc = desc;
    }

    public String getRegex() {
        return regex;
    }

    public String getDesc() {
        return desc;
    }
}
