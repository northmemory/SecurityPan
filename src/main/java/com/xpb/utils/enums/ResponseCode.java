package com.xpb.utils.enums;



public enum ResponseCode {
    CODE_200(200,"请求成功"),
    CODE_404(404,"请求的地址不存在"),
    CODE_600(600,"请求成功"),
    CODE_601(601,"信息已经存在"),
    CODE_500(500,"服务器发生错误,请联系管理员");
    private Integer code;
    private String msg;

    ResponseCode(Integer code, String msg){
        this.code=code;
        this.msg=msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
