package com.xpb.utils.exceptions;

public class BusinessException extends Exception{
    private int wrongCode;

    public BusinessException() {
    }

    public BusinessException(int wrongCode,String message) {
        super(message);
        this.wrongCode = wrongCode;
    }
}
