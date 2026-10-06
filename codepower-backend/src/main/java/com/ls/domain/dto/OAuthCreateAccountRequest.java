/**
 * 文件说明：第三方创建账号请求 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class OAuthCreateAccountRequest {
    private String username;
    private String email;
    private String verificationCode;
    private String region;
    @ToString.Exclude
    private String password;
    @ToString.Exclude
    private String confirmPassword;
}
