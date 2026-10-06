/**
 * 文件说明：统一响应状态码，供控制器和服务层统一返回错误与状态信息。
 */
package com.ls.common;

import lombok.Getter;

@Getter
/** 统一响应状态码枚举 */
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行此操作"),
    NOT_FOUND(404, "请求的资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
