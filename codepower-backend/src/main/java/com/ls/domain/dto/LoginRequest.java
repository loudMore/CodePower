/**
 * 文件说明：登录请求 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class LoginRequest {
    private String username;
    @ToString.Exclude
    private String password;
    @ToString.Exclude
    private String verificationToken;
    private Boolean rememberMe;
} 
