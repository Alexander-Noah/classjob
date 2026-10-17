/*
 * Result实现模块。
 */
package org.example.backend.result;

import java.util.Collections;

public final class Result {
    private static final Integer SUCCESS_CODE = 200;
    private static final Integer ERROR_CODE = -1;
    private static final String SUCCESS_MESSAGE = "操作成功";

    private Integer code;
    private String message;
    private Object data;

    public static Result success() {
        return success(Collections.emptyMap());
    }

    public static Result success(Object data) {
        return success(SUCCESS_MESSAGE, data);
    }

    public static Result success(String message, Object data) {
        Result result = new Result();
        result.code = SUCCESS_CODE;
        result.message = message;
        result.data = data;
        return result;
    }

    public static Result error(String message) {
        return error(ERROR_CODE, message);
    }

    public static Result error(Integer code, String message) {
        Result result = new Result();
        result.code = code;
        result.message = message;
        result.data = Collections.emptyMap();
        return result;
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

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
