/**
 * 文件说明：人机验证结果 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;

@Data
public class VerificationResult {
    private boolean success;
    private String message;
    private String verificationToken;
} 