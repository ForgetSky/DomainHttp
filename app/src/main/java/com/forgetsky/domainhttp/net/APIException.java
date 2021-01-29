package com.forgetsky.domainhttp.net;

import org.jetbrains.annotations.NotNull;


public class APIException extends RuntimeException {
    private int code;
    private String message;


    public APIException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    @NotNull
    @Override
    public String toString() {
        return "APIException{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
