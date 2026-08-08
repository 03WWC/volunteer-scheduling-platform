package com.volunteer.platform.common.api;

import java.io.Serializable;

public class Result<T> implements Serializable {

    public static final Integer SUCCESS_CODE = 200;
    public static final Integer SYSTEM_ERROR_CODE = 500;
    public static final String SUCCESS_MESSAGE = "success";
    public static final String SYSTEM_ERROR_MESSAGE = "system error";

    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(String message) {
        return fail(SYSTEM_ERROR_CODE, message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{"
            + "code=" + code
            + ", message='" + message + '\''
            + ", data=" + data
            + '}';
    }
}
