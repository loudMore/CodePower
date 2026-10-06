/**
 * 文件说明：用户 控制器，提供前端调用的 REST 接口入口。
 */
package com.ls.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ls.common.Result;
import com.ls.domain.RoleUpgradeRequest;
import com.ls.domain.Submission;
import com.ls.domain.User;
import com.ls.domain.dto.UserProfileDto;
import com.ls.mapper.RoleUpgradeRequestMapper;
import com.ls.mapper.SubmissionMapper;
import com.ls.mapper.UserMapper;
import com.ls.service.AuthService;
import com.ls.service.NotificationService;
import com.ls.service.UserAbilityService;
import com.ls.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 用户控制器 — 个人资料、密码修改、邮箱换绑、头像上传、能力模型 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SubmissionMapper submissionMapper;
    @Autowired
    private UserAbilityService userAbilityService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private RoleUpgradeRequestMapper roleUpgradeRequestMapper;
    @Autowired
    private NotificationService notificationService;

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getUserByUsername(userDetails.getUsername());
    }

    /** 更新当前用户个人资料 */
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody UserProfileDto profileDto) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }
        return userService.updateUser(user.getId(), profileDto);
    }

    /** 获取当前用户的能力雷达图数据 */
    @GetMapping("/me/abilities")
    public Result<Map<String, Object>> getMyAbilities(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(userAbilityService.getAbilityProfile(user.getId()));
    }

    /** 获取指定用户的公开能力雷达图数据 */
    @GetMapping("/{id}/abilities")
    public Result<Map<String, Object>> getUserAbilities(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(userAbilityService.getAbilityProfile(id));
    }

    /** 分页获取当前用户的提交记录 */
    @GetMapping("/me/submissions")
    public Result<IPage<Submission>> getMySubmissions(@AuthenticationPrincipal UserDetails userDetails,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) Long problemId) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(Submission::getUserId, user.getId())
                .isNull(Submission::getContestId)
                .eq(problemId != null, Submission::getProblemId, problemId)
                .orderByDesc(Submission::getCreatedAt);
        IPage<Submission> submissions = submissionMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(submissions);
    }

    /** 修改当前用户密码（需验证旧密码） */
    @PutMapping("/me/password")
    public Result<?> changeMyPassword(@AuthenticationPrincipal UserDetails userDetails,
                                      @RequestBody Map<String, String> payload) {
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            return Result.error("请提供当前密码和新密码");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度至少为6位");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Result.error("当前密码不正确");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        notificationService.createNotification(user.getId(), "PASSWORD_CHANGED",
                "登录密码已修改",
                "你的账号刚刚完成了登录密码修改。如果这不是你本人操作，请尽快联系管理员处理。",
                user.getId());
        return Result.success("密码修改成功");
    }

    /** 发送换绑邮箱的验证码 */
    @PostMapping("/me/send-email-code")
    public Result<?> sendChangeEmailCode(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody Map<String, String> payload) {
        String newEmail = normalizeEmail(payload.get("newEmail"));
        String currentPassword = payload.get("currentPassword");
        if (newEmail.isEmpty() || currentPassword == null) {
            return Result.error("请提供新邮箱和当前密码");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Result.error("当前密码不正确");
        }
        User existing = userMapper.findByEmail(newEmail);
        if (existing != null) {
            return Result.error("该邮箱已被其他账号使用");
        }
        try {
            authService.sendCodeToEmail(newEmail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("验证码邮件发送失败，请稍后重试或联系管理员检查邮箱 SMTP 配置");
        }
        return Result.success("验证码已发送到新邮箱");
    }

    /** 换绑邮箱（需验证码确认） */
    @PutMapping("/me/email")
    public Result<?> changeMyEmail(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestBody Map<String, String> payload) {
        String currentPassword = payload.get("currentPassword");
        String newEmail = normalizeEmail(payload.get("newEmail"));
        String verificationCode = payload.get("verificationCode");
        if (currentPassword == null || newEmail.isEmpty() || verificationCode == null) {
            return Result.error("请提供完整信息");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return Result.error("当前密码不正确");
        }
        User existing = userMapper.findByEmail(newEmail);
        if (existing != null) {
            return Result.error("该邮箱已被其他账号使用");
        }
        // Verify the email code using the same cache as registration
        if (!authService.verifyEmailCode(newEmail, verificationCode)) {
            return Result.error("验证码不正确或已过期");
        }
        user.setEmail(newEmail);
        userMapper.updateById(user);
        notificationService.createNotification(user.getId(), "EMAIL_CHANGED",
                "登录邮箱已修改",
                "你的账号绑定邮箱已修改为 " + newEmail + "。后续验证码和找回密码邮件会发送到新邮箱。",
                user.getId());
        return Result.success("邮箱修改成功");
    }

    /** 邮箱统一存储为小写，保证登录、验证码和换绑时大小写不敏感。 */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 根据ID获取用户公开信息 */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchUsers(@AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestParam String keyword,
                                                         @RequestParam(defaultValue = "10") int size) {
        User currentUser = userMapper.findByUsername(userDetails.getUsername());
        if (currentUser == null) {
            return Result.error("用户不存在");
        }
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return Result.success(List.of());
        }

        int safeSize = Math.min(Math.max(size, 1), 20);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getRole, User::getLevel)
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 1)
                .ne(User::getId, currentUser.getId())
                .like(User::getUsername, trimmedKeyword)
                .orderByAsc(User::getUsername)
                .last("LIMIT " + safeSize));

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("role", user.getRole());
            item.put("level", user.getLevel() != null ? user.getLevel() : 0);
            item.put("avatar", "/avatars/avatar-1.svg");
            result.add(item);
        }
        return Result.success(result);
    }

    /** 根据ID获取用户公开信息 */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /** 获取全部用户列表（仅管理员） */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return userService.getAllUsers();
    }

    /** 更新指定用户资料（本人或管理员） */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                       @RequestBody UserProfileDto profileDto,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isCurrentUserOrAdmin(id, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限修改此用户信息");
        }
        return userService.updateUser(id, profileDto);
    }

    /** 删除指定用户（本人或管理员） */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        if (!userService.isCurrentUserOrAdmin(id, userDetails)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限删除此用户");
        }
        return userService.deleteUser(id);
    }

    /** 提交角色升级申请 */
    @PostMapping("/me/upgrade-request")
    public Result<?> submitUpgradeRequest(@AuthenticationPrincipal UserDetails userDetails,
                                          @RequestBody Map<String, String> payload) {
        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error("请填写申请理由");
        }
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        if (!"NORMAL_USER".equals(user.getRole())) {
            return Result.error("您当前角色无需升级");
        }
        Long pending = roleUpgradeRequestMapper.selectCount(new LambdaQueryWrapper<RoleUpgradeRequest>()
                .eq(RoleUpgradeRequest::getUserId, user.getId())
                .eq(RoleUpgradeRequest::getStatus, "PENDING"));
        if (pending > 0) {
            return Result.error("您已有待审核的申请，请耐心等待");
        }
        RoleUpgradeRequest req = new RoleUpgradeRequest();
        req.setUserId(user.getId());
        req.setRequestedRole("SENIOR_USER");
        req.setReason(reason.trim());
        req.setStatus("PENDING");
        roleUpgradeRequestMapper.insert(req);
        notificationService.createNotification(user.getId(), "UPGRADE_REQUEST_SUBMITTED",
                "角色升级申请已提交",
                "你的高级用户申请已进入管理员审核队列，请耐心等待审核结果。",
                req.getId());
        notifyAdminsForUpgradeRequest(user, req);
        return Result.success("申请已提交，请等待管理员审核");
    }

    /** 查询当前用户的角色升级申请状态 */
    @GetMapping("/me/upgrade-status")
    public Result<?> getUpgradeStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        RoleUpgradeRequest latest = roleUpgradeRequestMapper.selectOne(
                new LambdaQueryWrapper<RoleUpgradeRequest>()
                        .eq(RoleUpgradeRequest::getUserId, user.getId())
                        .orderByDesc(RoleUpgradeRequest::getCreatedAt)
                        .last("LIMIT 1"));
        if (latest == null) return Result.success(Map.of("hasRequest", false));
        return Result.success(Map.of(
                "hasRequest", true,
                "status", latest.getStatus(),
                "reason", latest.getReason(),
                "reviewComment", latest.getReviewComment() != null ? latest.getReviewComment() : "",
                "createdAt", latest.getCreatedAt().toString()
        ));
    }

    /**
     * 智能推荐：结合弱项标签、当前难度阶段与训练习惯推荐题目
     */
    @GetMapping("/me/recommend")
    public Result<?> getRecommendedProblems(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestParam(defaultValue = "10") int size) {
        User user = userMapper.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        return Result.success(userAbilityService.getPersonalizedRecommendations(user.getId(), size));
    }

    private void notifyAdminsForUpgradeRequest(User applicant, RoleUpgradeRequest request) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId)
                .eq(User::getDeleted, 0)
                .eq(User::getStatus, 1)
                .eq(User::getRole, "ADMIN"));
        for (User admin : admins) {
            notificationService.createNotification(admin.getId(), "UPGRADE_REQUEST_CREATED",
                    "收到新的角色升级申请",
                    applicant.getUsername() + " 提交了高级用户申请，请到管理后台审核。",
                    request.getId());
        }
    }
} 
