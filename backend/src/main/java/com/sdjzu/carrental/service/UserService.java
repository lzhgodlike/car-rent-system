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
        // 校验手机号
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        // 校验身份证号
        if (StringUtils.hasText(request.getIdCard()) && !request.getIdCard().matches("^\\d{17}[\\dXx]$")) {
            throw new BusinessException("身份证号应为18位");
        }
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setIdCard(request.getIdCard());
        user.setGender(request.getGender());
        if (StringUtils.hasText(request.getPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException("请输入原密码");
            }
            String oldPwdHash = DigestUtils.md5DigestAsHex(request.getOldPassword().getBytes(StandardCharsets.UTF_8));
            if (!oldPwdHash.equals(user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        }
        userMapper.updateById(user);
    }

    public PageResult<User> listUsers(int pageNum, int pageSize, String role, Integer status) {
        SecurityUtils.requireAdmin();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(StringUtils.hasText(role), User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
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

        boolean isSelf = id.equals(SecurityUtils.getUserId());

        // 管理员不能修改自己的角色和状态
        if (isSelf) {
            if (StringUtils.hasText(request.getRole()) && !request.getRole().equals(user.getRole())) {
                throw new BusinessException("不能修改自己的角色");
            }
            if (request.getStatus() != null && !request.getStatus().equals(user.getStatus())) {
                throw new BusinessException("不能修改自己的账号状态");
            }
        }

        // 禁用管理员前检查是否是最后一个
        if (request.getStatus() != null && request.getStatus() == 0
                && "ADMIN".equalsIgnoreCase(user.getRole())) {
            Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")
                    .eq(User::getStatus, 1));
            if (adminCount != null && adminCount <= 1) {
                throw new BusinessException("系统至少需要一个启用的管理员，无法禁用");
            }
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

    public void deleteUser(Long id) {
        SecurityUtils.requireAdmin();
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BusinessException("不能删除自己的账号");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 如果要删除的是管理员，检查是否是最后一个
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            Long adminCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN"));
            if (adminCount != null && adminCount <= 1) {
                throw new BusinessException("系统至少需要一个管理员，无法删除");
            }
        }
        userMapper.deleteById(id);
    }
}
