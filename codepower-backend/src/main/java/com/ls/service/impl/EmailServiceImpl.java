/**
 * 文件说明：邮件发送服务实现，处理验证码和密码重置邮件的 SMTP 投递。
 */
package com.ls.service.impl;

import com.ls.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** 邮件发送服务实现 — SMTP 发送验证码/密码重置邮件 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.from}")
    private String from;

    @Override
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        try {
            mailSender.send(message);
            logger.info("简单邮件已成功发送至 {}", to);
        } catch (Exception e) {
            logger.error("发送简单邮件至 {} 时发生错误", to, e);
            throw new IllegalStateException("简单邮件发送失败", e);
        }
    }
    
    @Override
    public void sendHtmlMail(String to, String subject, String content) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            mailSender.send(message);
            logger.info("HTML邮件已成功发送至 {}", to);
        } catch (Exception e) {
            logger.error("发送HTML邮件至 {} 时发生错误", to, e);
            throw new IllegalStateException("HTML邮件发送失败", e);
        }
    }
    
    @Override
    public void sendVerificationCodeMail(String to, String subject, String code) {
        try {
            // 读取HTML模板
            String templateContent = readTemplateFile("templates/verification-code.html");
            
            // 替换模板中的变量
            String htmlContent = templateContent.replace("${code}", code);
            
            // 优先发送 HTML 模板邮件，模板读取或 SMTP 失败时交给 catch 走文本兜底。
            sendHtmlMail(to, subject, htmlContent);
            logger.info("验证码邮件已成功发送至 {}", to);
        } catch (Exception htmlError) {
            logger.warn("验证码HTML邮件发送失败，尝试文本邮件: to={}", to, htmlError);
            sendSimpleMail(to, subject, "您的验证码是：" + code + "，有效期为5分钟。");
            logger.info("验证码文本邮件已成功发送至 {}", to);
        }
    }
    
    @Override
    public void sendPasswordResetMail(String to, String subject, String username, String email, String code) {
        try {
            // 读取HTML模板
            String templateContent = readTemplateFile("templates/password-reset.html");
            
            // 获取当前时间
            String timestamp = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy年MM月dd日 HH:mm:ss")
                    .format(java.time.LocalDateTime.now());
            
            // 替换模板中的变量
            String htmlContent = templateContent
                    .replace("${code}", code)
                    .replace("${username}", username)
                    .replace("${email}", email)
                    .replace("${timestamp}", timestamp);
            
            // 优先发送 HTML 模板邮件，模板读取或 SMTP 失败时交给 catch 走文本兜底。
            sendHtmlMail(to, subject, htmlContent);
            logger.info("密码重置邮件已成功发送至 {}", to);
        } catch (Exception htmlError) {
            logger.warn("密码重置HTML邮件发送失败，尝试文本邮件: to={}", to, htmlError);
            sendSimpleMail(to, subject, 
                    "亲爱的 " + username + "，\n\n" +
                    "您的密码重置验证码是：" + code + "，有效期为15分钟。\n\n" +
                    "如果这不是您本人的操作，请忽略此邮件。\n\n" +
                    "CodePower团队");
            logger.info("密码重置文本邮件已成功发送至 {}", to);
        }
    }
    
    /**
     * 读取模板文件内容
     * @param templatePath 模板路径
     * @return 模板内容
     * @throws IOException 文件读取异常
     */
    private String readTemplateFile(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
} 
