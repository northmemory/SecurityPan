package com.xpb.utils.exceptions;

public class BusinessException extends Exception{
    private int wrongCode;
    private Exception exception;

    public BusinessException() {
    }

    public BusinessException(int wrongCode,String message) {
        super(message);
        this.wrongCode = wrongCode;
    }
    public BusinessException(Exception e,int wrongCode,String message){
        super(message);
        this.wrongCode = wrongCode;
        this.exception=e;
    }

    public int getWrongCode() {
        return wrongCode;
    }


}
