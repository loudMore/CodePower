/**
 * 文件说明：人机验证挑战 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class VerificationChallenge {
    private String challengeId;
    private String prompt;
    private List<String> options;
    private String backgroundImage;
} 