/**
 * 文件说明：人机验证提交 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class VerificationAttempt {
    private String challengeId;
    private List<String> selection;
} 