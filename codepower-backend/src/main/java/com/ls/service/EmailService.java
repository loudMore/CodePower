/**
 * 文件说明：邮件发送 服务接口，定义对应模块可复用的业务能力。
 */
package com.ls.service;

/** 邮件发送服务 */
public interface EmailService {

    /**
     * 发送简单的文本邮件
     * @param to 收件人地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendSimpleMail(String to, String subject, String content);

    /**
     * 发送HTML格式邮件
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param content HTML格式的邮件内容
     */
    void sendHtmlMail(String to, String subject, String content);

    /**
     * 发送验证码邮件（使用HTML模板）
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param code 验证码
     */
    void sendVerificationCodeMail(String to, String subject, String code);

    /**
     * 发送密码重置邮件（使用HTML模板）
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param username 用户名
     * @param email 用户邮箱
     * @param code 重置验证码
     */
    void sendPasswordResetMail(String to, String subject, String username, String email, String code);
} 