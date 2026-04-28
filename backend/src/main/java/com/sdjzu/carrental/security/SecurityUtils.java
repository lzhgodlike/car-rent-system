package com.sdjzu.carrental.security;

import com.sdjzu.carrental.common.BusinessException;
import com.sdjzu.carrental.common.UserContext;
import com.sdjzu.carrental.model.dto.LoginUser;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException("未登录或登录已失效");
        }
        return loginUser;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getLoginUser().getRole());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException("无权限执行该操作");
        }
    }
}
