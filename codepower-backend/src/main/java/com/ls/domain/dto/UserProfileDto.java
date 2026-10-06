/**
 * 文件说明：用户资料 DTO 数据传输对象，承载前后端请求或响应字段。
 */
package com.ls.domain.dto;

import lombok.Data;

/**
 * 用户资料数据传输对象
 */
@Data
public class UserProfileDto {
    private String username;
    private String email;
    private String region;
    private String bio;
    private String avatarUrl;
} 