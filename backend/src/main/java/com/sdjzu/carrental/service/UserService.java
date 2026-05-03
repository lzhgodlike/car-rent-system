package com.sdjzu.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.mapper.UserMapper;
import com.sdjzu.carrental.model.entity.User;
import com.sdjzu.carrental.model.request.UserManageRequest;
import com.sdjzu.carrental.model.request.UserProfileRequest;
import com.sdjzu.carrental.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sdjzu.carrental.common.PageResult;

import java.nio.charset.StandardCharsets;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User profile() {
        User user = userMapper.selectById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void updateProfile(UserProfileRequest request) {
        User user = userMapper.selectById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(request.getGender());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        }
        userMapper.updateById(user);
    }

    public PageResult<User> listUsers(int pageNum, int pageSize) {
        SecurityUtils.requireAdmin();
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<User>().orderByDesc(User::getId));
        page.getRecords().forEach(item -> item.setPassword(null));
        PageResult<User> result = PageResult.of(page);
        result.summary("admin", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN")));
        result.summary("normal", userMapper.selectCount(
                new LambdaQueryWrapper<User>().ne(User::getRole, "ADMIN")));
        return result;
    }

    public void updateUser(Long id, UserManageRequest request) {
        SecurityUtils.requireAdmin();
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(request.getUsername())) {
            User exists = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .ne(User::getId, id));
            if (exists != null) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        }
        userMapper.updateById(user);
    }
}
