/**
 * 文件说明：注册请求 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String verificationCode;
    private String region;
} 
