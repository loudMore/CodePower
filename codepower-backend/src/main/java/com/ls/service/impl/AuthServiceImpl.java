/**
 * 文件说明：认证授权 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ls.domain.User;
import com.ls.domain.UserProfile;
import com.ls.domain.dto.LoginRequest;
import com.ls.domain.dto.RegisterRequest;
import com.ls.mapper.UserMapper;
import com.ls.mapper.UserProfileMapper;
import com.ls.service.AuthService;
import com.ls.service.EmailService;
import com.ls.service.NotificationService;
import com.ls.service.RegionService;
import com.ls.service.VerificationService;
import com.ls.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** 用户认证服务实现 — 注册、登录、密码重置 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RegionService regionService;

    // 注册验证码缓存
    private final Cache<String, String> emailCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
            
    // 密码重置验证码缓存，有效期更长
    private final Cache<String, String> passwordResetCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    @Override
    public ResponseEntity<?> sendRegistrationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的邮箱地址"));
        }
        if (userMapper.findByEmail(normalizedEmail) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "该邮箱已被注册"));
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        emailCodeCache.put(normalizedEmail, code);
        String subject = "欢迎注册 CodePower！";

        try {
            // 使用HTML模板发送验证码邮件；发送失败必须告诉前端，不能假装验证码已发出。
            emailService.sendVerificationCodeMail(normalizedEmail, subject, code);
        } catch (Exception e) {
            emailCodeCache.invalidate(normalizedEmail);
            return ResponseEntity.internalServerError().body(Map.of("message", "验证码邮件发送失败，请稍后重试或联系管理员检查邮箱 SMTP 配置"));
        }
        
        return ResponseEntity.ok(Map.of("message", "验证码已发送"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的邮箱地址"));
        }
        String cachedCode = emailCodeCache.getIfPresent(email);
        if (cachedCode == null || !cachedCode.equals(request.getVerificationCode())) {
            return ResponseEntity.badRequest().body(Map.of("message", "验证码错误或已失效"));
        }

        // 验证用户名格式
        if (!request.getUsername().matches("^[a-zA-Z0-9_\u4e00-\u9fa5]+$")) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名只能包含字母、数字、下划线和中文"));
        }

        if (userMapper.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }

        if (userMapper.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "该邮箱已被注册"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(email); // 存储小写的邮箱地址
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("NORMAL_USER");
        user.setStatus(1); // 直接设为正常状态
        userMapper.insert(user);

        String requestedRegion = regionService.normalizeRegion(request.getRegion());
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setRegion(requestedRegion != null ? requestedRegion : regionService.resolveCurrentRequestRegion());
        profile.setAvatarUrl("/avatars/avatar-1.svg");
        userProfileMapper.insert(profile);

        notificationService.createNotification(user.getId(), "WELCOME",
                "欢迎加入 CodePower！",
                "欢迎你，" + user.getUsername() + "！CodePower 是一个 AI 赋能的编程学习平台，快去刷题提升自己吧！每日签到可以获取经验和 AI 积分哦。",
                null);

        emailCodeCache.invalidate(email);
        return ResponseEntity.ok(Map.of("message", "注册成功"));
    }

    @Override
    public ResponseEntity<?> login(LoginRequest request) {
        if (!verificationService.validateVerificationToken(request.getVerificationToken())) {
            return ResponseEntity.status(401).body(Map.of("message", "人机验证失败或票据已过期"));
        }

        try {
            // 检查用户输入的是用户名还是邮箱
            String username = request.getUsername() == null ? "" : request.getUsername().trim();
            User user = null;
            
            // 如果包含@符号，认为是邮箱登录
            if (username.contains("@")) {
                // 邮箱地址统一去空格并转小写，确保 @QQ.COM、@qq.com 等写法都能命中同一账号。
                String email = normalizeEmail(username);
                user = userMapper.findByEmail(email);
                if (user == null) {
                    return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
                }
                // 使用找到的用户名进行认证
                username = user.getUsername();
            } else {
                // 用户名登录，直接使用输入的用户名
                user = userMapper.findByUsername(username);
                if (user == null) {
                    return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
                }
            }
            
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            
            // 认证成功，生成JWT（包含 userId 和 role）
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token;
            if (Boolean.TRUE.equals(request.getRememberMe())) {
                token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole(), 7 * 24 * 3600 * 1000L);
            } else {
                token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole());
            }
            
            // 返回JWT令牌和用户信息
            Map<String, Object> response = new HashMap<>();
            response.put("message", "登录成功");
            response.put("token", token);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            // 获取头像
            UserProfile profile = userProfileMapper.selectOne(
                    new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
            userInfo.put("avatar", profile != null && profile.getAvatarUrl() != null
                    ? profile.getAvatarUrl() : "/avatars/avatar-1.svg");
            response.put("user", userInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 认证失败
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }
    }
    
    @Override
    public ResponseEntity<?> requestPasswordReset(String email, String verificationToken) {
        // 验证人机验证token
        if (!verificationService.validateVerificationToken(verificationToken)) {
            return ResponseEntity.status(401).body(Map.of("message", "人机验证失败或票据已过期"));
        }

        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的邮箱地址"));
        }
        
        // 检查用户是否存在
        User user = userMapper.findByEmail(normalizedEmail);
        if (user == null) {
            // 为了安全，不告诉用户邮箱是否存在
            return ResponseEntity.ok(Map.of("message", "如果该邮箱已注册，验证码将发送到该邮箱"));
        }
        
        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        passwordResetCodeCache.put(normalizedEmail, code);
        
        // 发送密码重置邮件
        String subject = "CodePower 密码重置";
        try {
            emailService.sendPasswordResetMail(normalizedEmail, subject, user.getUsername(), normalizedEmail, code);
        } catch (Exception e) {
            passwordResetCodeCache.invalidate(normalizedEmail);
            return ResponseEntity.internalServerError().body(Map.of("message", "密码重置邮件发送失败，请稍后重试或联系管理员检查邮箱 SMTP 配置"));
        }
        
        return ResponseEntity.ok(Map.of("message", "如果该邮箱已注册，验证码将发送到该邮箱"));
    }
    
    @Override
    public ResponseEntity<?> resetPassword(Map<String, String> resetInfo) {
        String email = normalizeEmail(resetInfo.get("email"));
        String code = resetInfo.get("code");
        String newPassword = resetInfo.get("newPassword");
        
        // 验证基本参数
        if (email.isEmpty() || code == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "请求参数不完整"));
        }
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入有效的邮箱地址"));
        }
        
        // 验证码校验
        String cachedCode = passwordResetCodeCache.getIfPresent(email);
        if (cachedCode == null || !cachedCode.equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "验证码错误或已失效"));
        }
        
        // 查找用户
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updatePassword(user.getId(), user.getPassword());
        notificationService.createNotification(user.getId(), "PASSWORD_RESET_SELF",
                "登录密码已通过邮箱重置",
                "你的账号刚刚通过邮箱验证码完成密码重置。如果这不是你本人操作，请立即联系管理员。",
                user.getId());
        
        // 清除验证码缓存
        passwordResetCodeCache.invalidate(email);
        
        return ResponseEntity.ok(Map.of("message", "密码重置成功，请使用新密码登录"));
    }
    
    @Override
    public ResponseEntity<?> checkUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }
        
        // 检查用户名格式
        if (!username.matches("^[a-zA-Z0-9_\u4e00-\u9fa5]+$")) {
            return ResponseEntity.ok(Map.of(
                "exists", false,
                "message", "用户名格式不正确"
            ));
        }
        
        // 查询数据库
        User user = userMapper.findByUsername(username);
        
        return ResponseEntity.ok(Map.of(
            "exists", user != null,
            "message", user != null ? "用户名已存在" : "用户名可用"
        ));
    }
    
    @Override
    public ResponseEntity<?> checkEmailExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "邮箱不能为空"));
        }
        if (!isValidEmail(normalizedEmail)) {
            return ResponseEntity.ok(Map.of(
                    "exists", false,
                    "message", "邮箱格式不正确"
            ));
        }
        
        // 查询数据库
        User user = userMapper.findByEmail(normalizedEmail);
        
        return ResponseEntity.ok(Map.of(
            "exists", user != null,
            "message", user != null ? "该邮箱已被注册" : "邮箱可用"
        ));
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String cachedCode = emailCodeCache.getIfPresent(normalizedEmail);
        if (cachedCode != null && cachedCode.equals(code)) {
            emailCodeCache.invalidate(normalizedEmail);
            return true;
        }
        return false;
    }

    @Override
    public void sendCodeToEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("请输入有效的邮箱地址");
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        emailCodeCache.put(normalizedEmail, code);
        String subject = "CodePower 邮箱验证";
        try {
            emailService.sendVerificationCodeMail(normalizedEmail, subject, code);
        } catch (Exception e) {
            emailCodeCache.invalidate(normalizedEmail);
            throw e;
        }
    }

    /**
     * 邮箱统一规范化：去掉用户复制时带入的前后空格，并转为小写作为缓存键和数据库存储值。
     */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 校验常见邮箱格式，避免无效地址进入验证码发送和注册流程。
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
} 
