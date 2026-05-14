package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.LoginRequest;
import com.sdjzu.carrental.model.request.RegisterRequest;
import com.sdjzu.carrental.model.vo.TokenVO;
import com.sdjzu.carrental.security.JwtUtil;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest request) {
        User exists = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(request.getGender());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
    }

    public TokenVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(md5(request.getPassword()))) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("该账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenVO(token, sanitize(user));
    }

    public User me() {
        User user = userMapper.selectById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return sanitize(user);
    }

    public TokenVO refresh() {
        Long userId = SecurityUtils.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("该账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new TokenVO(token, sanitize(user));
    }

    public User sanitize(User user) {
        if (user == null) {
            return null;
        }
        user.setPassword(null);
        return user;
    }

    private String md5(String source) {
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
