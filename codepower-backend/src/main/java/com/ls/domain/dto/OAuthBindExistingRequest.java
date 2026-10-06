/**
 * 文件说明：第三方绑定已有账号请求 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class OAuthBindExistingRequest {
    private String account;
    @ToString.Exclude
    private String password;
}
