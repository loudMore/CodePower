/**
 * 文件说明：U se rD et ai ls 服务实现，处理对应模块的核心业务逻辑。
 */
package com.ls.service.impl;

import com.ls.domain.User;
import com.ls.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Spring Security 用户加载实现 — 从数据库查询用户并构建权限 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        
        // 判断是用户名还是邮箱
        if (username.contains("@")) {
            // 邮箱登录统一去空格并转小写，和注册入库格式保持一致。
            String email = username.trim().toLowerCase(Locale.ROOT);
            user = userMapper.findByEmail(email);
        } else {
            // 用户名登录
            user = userMapper.findByUsername(username);
        }
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 用户状态检查
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已注销: " + username);
        }
        
        // 构建权限列表
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
        
        // 返回Spring Security的UserDetails对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
} 
