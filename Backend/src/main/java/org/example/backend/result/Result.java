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

    /**
     * 成功的处理逻辑。
     * @return Result 类型的处理结果。
     */
    public static Result success() {
        return success(Collections.emptyMap());
    }

    /**
     * 成功的处理逻辑。
     * @param data 数据参数。
     * @return Result 类型的处理结果。
     */
    public static Result success(Object data) {
        return success(SUCCESS_MESSAGE, data);
    }

    /**
     * 成功的处理逻辑。
     * @param message 消息参数。
     * @param data 数据参数。
     * @return Result 类型的处理结果。
     */
    public static Result success(String message, Object data) {
        Result result = new Result();
        result.code = SUCCESS_CODE;
        result.message = message;
        result.data = data;
        return result;
    }

    /**
     * 错误的处理逻辑。
     * @param message 消息参数。
     * @return Result 类型的处理结果。
     */
    public static Result error(String message) {
        return error(ERROR_CODE, message);
    }

    /**
     * 错误的处理逻辑。
     * @param code 编码参数。
     * @param message 消息参数。
     * @return Result 类型的处理结果。
     */
    public static Result error(Integer code, String message) {
        Result result = new Result();
        result.code = code;
        result.message = message;
        result.data = Collections.emptyMap();
        return result;
    }

    /**
     * 获取编码。
     * @return Integer 类型的处理结果。
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 设置编码。
     * @param code 编码参数。
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * 获取消息。
     * @return String 类型的处理结果。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置消息。
     * @param message 消息参数。
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取数据。
     * @return Object 类型的处理结果。
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置数据。
     * @param data 数据参数。
     */
    public void setData(Object data) {
        this.data = data;
    }
}
