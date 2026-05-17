<script setup>
import { ref, computed, watch, onBeforeUnmount, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import request from "../utils/request";
import SupportChatDrawer from "../components/support/SupportChatDrawer.vue";
import { createNotificationSocket } from "../utils/notificationSocket";
import {
  clearAuth,
  setAuth,
  useAuth,
  authModalType,
  openLoginModal,
  openRegisterModal,
  closeAuthModal,
  switchToLoginModal,
  switchToRegisterModal,
} from "../utils/auth";

const router = useRouter();
const route = useRoute();
const { isLoggedIn, userInfo } = useAuth();
const userName = computed(
  () => userInfo.value?.realName || userInfo.value?.username || "用户"
);
const messageLoading = ref(false);
const messageList = ref([]);
const messageUnread = ref(0);
const bannerNotice = ref(null);
const messagePopoverVisible = ref(false);
const messagePopoverRef = ref(null);
const supportDrawerVisible = ref(false);
const notificationSocketConnected = ref(false);
const dismissedBannerIds = new Set();
let notificationSocket = null;
let notificationSocketActive = false;
let messageFallbackTimer = null;
let bannerSwitchTimer = null;

const ensureNotificationSocket = () => {
  if (!notificationSocket) {
    notificationSocket = createNotificationSocket({
      onOpen: () => {
        notificationSocketConnected.value = true;
        stopMessageFallbackPolling();
      },
      onClose: () => {
        notificationSocketConnected.value = false;
        if (notificationSocketActive) {
          startMessageFallbackPolling();
        }
      },
      onError: () => {
        notificationSocketConnected.value = false;
      },
      onMessage: (payload) => {
        if (payload?.type !== "NOTICE_CREATED" || !payload.payload) return;
        receiveRealtimeNotice(payload.payload);
      },
    });
  }
  return notificationSocket;
};

const publicNav = [
  { path: "/home", label: "首页" },
  { path: "/book", label: "找车租车" },
];
const privateNav = [
  { path: "/my-orders", label: "我的订单" },
  { path: "/my-profile", label: "个人中心" },
];

const logout = () => {
  clearAuth();
  router.replace("/home");
};

const formatMessageTime = (value) =>
  value ? String(value).replace("T", " ").slice(0, 16) : "";

const isSupportReplyNotice = (item, payload = {}) => {
  if (!item) return false;
  if (item.messageType !== "SUPPORT_MESSAGE_REPLIED") return false;
  if (payload.bizType && item.bizType !== payload.bizType) return false;
  if (payload.conversationId && Number(item.bizId) !== Number(payload.conversationId)) return false;
  return true;
};

const resolveNoticeActionPath = (notice) => {
  if (notice?.messageType === "SUPPORT_MESSAGE_REPLIED") {
    return null;
  }
  if (
    notice?.bizType === "RENT_ORDER" ||
    notice?.bizType === "RETURN_ORDER" ||
    notice?.bizType === "FAULT_REPORT"
  ) {
    return "/my-orders";
  }
  return "/my-profile";
};

const clearBannerSwitchTimer = () => {
  if (!bannerSwitchTimer) return;
  clearTimeout(bannerSwitchTimer);
  bannerSwitchTimer = null;
};

const receiveRealtimeNotice = (notice) => {
  if (!notice?.id) return;
  const exists = messageList.value.some((item) => item.id === notice.id);
  if (exists) return;
  messageList.value = [notice, ...messageList.value.filter((item) => item.id !== notice.id)].slice(0, 20);
  if (notice.readStatus === 0) {
    messageUnread.value += 1;
    syncMessageBanner(notice);
  }
};

const syncMessageBanner = (latestUnread) => {
  clearBannerSwitchTimer();
  if (!latestUnread) {
    bannerNotice.value = null;
    return;
  }
  if (dismissedBannerIds.has(latestUnread.id)) {
    return;
  }
  if (bannerNotice.value?.id === latestUnread.id) {
    return;
  }
  if (!bannerNotice.value) {
    bannerNotice.value = latestUnread;
    return;
  }
  bannerNotice.value = null;
  bannerSwitchTimer = setTimeout(() => {
    bannerNotice.value = latestUnread;
    bannerSwitchTimer = null;
  }, 300);
};

const closeMessageBanner = () => {
  clearBannerSwitchTimer();
  if (!bannerNotice.value) return;
  dismissedBannerIds.add(bannerNotice.value.id);
  bannerNotice.value = null;
};

const openMessageList = async () => {
  clearBannerSwitchTimer();
  if (!isLoggedIn.value) return;
  await loadMessages();
  await nextTick();
  messagePopoverVisible.value = true;
  bannerNotice.value = null;
};

const isNoticeActionable = (item) => item?.actionable !== false;

const goHandleMessageBanner = async () => {
  if (!bannerNotice.value) return;
  await goHandleMessage(bannerNotice.value, { closeBanner: true });
};

const refreshMessageStatus = async () => {
  if (!isLoggedIn.value) {
    messageUnread.value = 0;
    bannerNotice.value = null;
    return;
  }
  try {
    const page = await request.get("/notifications", {
      params: { pageNum: 1, pageSize: 1, unreadOnly: true },
    });
    messageUnread.value = Number(page.summary?.unread || 0);
    syncMessageBanner(page.records?.[0] || null);
  } catch {}
};

const loadMessages = async () => {
  if (!isLoggedIn.value) return;
  messageLoading.value = true;
  try {
    const page = await request.get("/notifications", {
      params: { pageNum: 1, pageSize: 20 },
    });
    messageList.value = page.records || [];
    messageUnread.value = Number(page.summary?.unread || 0);
  } finally {
    messageLoading.value = false;
  }
};

watch(messagePopoverVisible, (visible) => {
  if (visible) {
    loadMessages();
  }
});

const markMessageRead = async (item) => {
  if (!item || item.readStatus === 1) return;
  await request.put(`/notifications/${item.id}/read`);
  item.readStatus = 1;
  if (bannerNotice.value?.id === item.id) {
    bannerNotice.value = null;
  }
  dismissedBannerIds.add(item.id);
  if (messageUnread.value > 0) {
    messageUnread.value -= 1;
  }
};

const goHandleMessage = async (item, options = {}) => {
  if (!item || !isNoticeActionable(item)) return;
  if (item.readStatus === 0) {
    try {
      await markMessageRead(item);
    } catch {}
  }
  if (item.messageType === "SUPPORT_MESSAGE_REPLIED") {
    supportDrawerVisible.value = true;
    messagePopoverVisible.value = false;
    if (options.closeBanner) {
      bannerNotice.value = null;
    }
    return;
  }
  const targetPath = resolveNoticeActionPath(item);
  if (targetPath) {
    await router.push(targetPath);
  }
  messagePopoverVisible.value = false;
  if (options.closeBanner || bannerNotice.value?.id === item.id) {
    bannerNotice.value = null;
  }
};

const deleteMessage = async (item) => {
  if (!item) return;
  const wasUnread = item.readStatus === 0;
  await request.delete(`/notifications/${item.id}`);
  messageList.value = messageList.value.filter(
    (message) => message.id !== item.id
  );
  dismissedBannerIds.add(item.id);
  if (bannerNotice.value?.id === item.id) {
    bannerNotice.value = null;
  }
  if (wasUnread && messageUnread.value > 0) {
    messageUnread.value -= 1;
  }
  ElMessage.success("消息已删除");
};

const markAllMessagesRead = async () => {
  if (!messageUnread.value) return;
  await request.put("/notifications/read-all");
  messageList.value = messageList.value.map((item) => ({
    ...item,
    readStatus: 1,
  }));
  messageUnread.value = 0;
  bannerNotice.value = null;
};

const clearSupportReplyNotifications = async (payload = {}) => {
  const targets = messageList.value.filter((item) =>
    isSupportReplyNotice(item, payload)
  );
  if (!targets.length) {
    if (isSupportReplyNotice(bannerNotice.value, payload)) {
      bannerNotice.value = null;
    }
    return;
  }
  await Promise.all(
    targets.map((item) => request.delete(`/notifications/${item.id}`).catch(() => null))
  );
  const targetIds = new Set(targets.map((item) => item.id));
  const unreadRemoved = targets.filter((item) => item.readStatus === 0).length;
  messageList.value = messageList.value.filter((item) => !targetIds.has(item.id));
  targets.forEach((item) => dismissedBannerIds.add(item.id));
  if (bannerNotice.value && targetIds.has(bannerNotice.value.id)) {
    bannerNotice.value = null;
  }
  if (unreadRemoved > 0) {
    messageUnread.value = Math.max(0, messageUnread.value - unreadRemoved);
  }
};

const stopMessageFallbackPolling = () => {
  if (!messageFallbackTimer) return;
  clearInterval(messageFallbackTimer);
  messageFallbackTimer = null;
};

const startMessageFallbackPolling = () => {
  stopMessageFallbackPolling();
  if (!isLoggedIn.value || notificationSocketConnected.value) return;
  messageFallbackTimer = setInterval(() => {
    refreshMessageStatus();
  }, 45000);
};

const connectNotificationSocket = async () => {
  if (!isLoggedIn.value) return;
  notificationSocketActive = true;
  try {
    await ensureNotificationSocket().connect();
  } catch {
    notificationSocketConnected.value = false;
    startMessageFallbackPolling();
  }
};

const disconnectNotificationSocket = () => {
  notificationSocketActive = false;
  stopMessageFallbackPolling();
  notificationSocketConnected.value = false;
  notificationSocket?.disconnect();
};

// Handle login query param (from router guard redirect)
watch(
  () => route.query.login,
  (val) => {
    if (val === "1" || val === 1) {
      openLogin();
      router.replace({ path: route.path });
    }
  },
  { immediate: true }
);

// Auth modal
const loginLoading = ref(false);
const registerLoading = ref(false);
const loginForm = ref({ username: "", password: "" });
const registerForm = ref({
  username: "",
  password: "",
  realName: "",
  phone: "",
  idCard: "",
  gender: "",
});

const openLogin = () => {
  loginForm.value = { username: "", password: "" };
  openLoginModal();
};
const openRegister = () => {
  registerForm.value = {
    username: "",
    password: "",
    realName: "",
    phone: "",
    idCard: "",
    gender: "",
  };
  openRegisterModal();
};
const openSupportChat = () => {
  if (!isLoggedIn.value) {
    openLogin();
    return;
  }
  supportDrawerVisible.value = true;
};

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }
  loginLoading.value = true;
  try {
    const data = await request.post("/auth/login", loginForm.value);
    setAuth(data);
    closeAuthModal();
    const role = data.userInfo?.role;
    if (role === "ADMIN") {
      router.replace("/admin/dashboard");
    }
  } finally {
    loginLoading.value = false;
  }
};

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }
  registerLoading.value = true;
  try {
    await request.post("/auth/register", registerForm.value);
    ElMessage.success("注册成功，请登录");
    loginForm.value = {
      username: registerForm.value.username,
      password: registerForm.value.password,
    };
    switchToLoginModal();
  } finally {
    registerLoading.value = false;
  }
};

watch(
  () => isLoggedIn.value,
  async (loggedIn) => {
    if (loggedIn) {
      await loadMessages();
      await connectNotificationSocket();
      if (!notificationSocketConnected.value) {
        await refreshMessageStatus();
      }
    } else {
      disconnectNotificationSocket();
      messageList.value = [];
      messageUnread.value = 0;
      bannerNotice.value = null;
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  disconnectNotificationSocket();
  clearBannerSwitchTimer();
});
</script>

<template>
  <div class="user-layout">
    <nav class="top-nav">
      <router-link to="/home" class="nav-logo">
        <span class="logo-dot"></span> 驰云租车
      </router-link>

      <div class="nav-links">
        <router-link
          v-for="item in publicNav"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: route.path === item.path }"
        >
          {{ item.label }}
        </router-link>
        <template v-if="isLoggedIn">
          <router-link
            v-for="item in privateNav"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: route.path === item.path }"
          >
            {{ item.label }}
          </router-link>
        </template>
      </div>

      <div class="nav-right">
        <template v-if="isLoggedIn">
          <button class="support-trigger" type="button" @click="openSupportChat">
            <el-icon><ChatRound /></el-icon>
            <span>联系客服</span>
          </button>
          <el-popover
            ref="messagePopoverRef"
            v-model:visible="messagePopoverVisible"
            placement="bottom-end"
            :width="348"
            trigger="click"
            @show="loadMessages"
          >
            <template #reference>
              <button class="message-trigger" type="button">
                <span class="bell-badge-wrap">
                  <svg
                    width="17"
                    height="17"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                  <span v-if="messageUnread > 0" class="bell-badge">{{
                    messageUnread > 99 ? "99+" : messageUnread
                  }}</span>
                </span>
              </button>
            </template>

            <div class="message-pop">
              <div class="message-pop-head">
                <div class="message-pop-title">
                  消息通知
                  <span v-if="messageUnread > 0" class="message-pop-count"
                    >{{ messageUnread }} 条未读</span
                  >
                </div>
                <button
                  type="button"
                  class="message-read-all"
                  :disabled="messageUnread === 0"
                  @click="markAllMessagesRead"
                >
                  全部已读
                </button>
              </div>
              <div v-loading="messageLoading" class="message-pop-body">
                <div v-if="messageList.length === 0" class="message-empty">
                  暂无消息
                </div>
                <div
                  v-for="item in messageList"
                  :key="item.id"
                  class="message-item"
                  :class="{
                    unread: item.readStatus === 0,
                    read: item.readStatus === 1,
                  }"
                >
                  <div class="message-item-header">
                    <div class="message-icon">
                      <svg
                        width="13"
                        height="13"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2.5"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path
                          d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                        />
                        <polyline points="14 2 14 8 20 8" />
                      </svg>
                    </div>
                    <div class="message-meta">
                      <div class="message-title">
                        <span
                          v-if="item.readStatus === 0"
                          class="unread-dot"
                        ></span>
                        {{ item.title }}
                      </div>
                      <div class="message-time">
                        {{ formatMessageTime(item.createTime) }}
                      </div>
                    </div>
                  </div>
                  <div class="message-content">{{ item.content }}</div>
                  <div class="message-item-actions">
                    <button
                      type="button"
                      class="msg-btn read"
                      :disabled="item.readStatus === 1"
                      @click="markMessageRead(item)"
                    >
                      标为已读
                    </button>
                    <button
                      type="button"
                      class="msg-btn action"
                      :disabled="!isNoticeActionable(item)"
                      @click="goHandleMessage(item)"
                    >
                      {{ isNoticeActionable(item) ? "去操作" : "已处理" }}
                    </button>
                    <button
                      type="button"
                      class="msg-btn delete"
                      @click="deleteMessage(item)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>

          <el-dropdown
            trigger="click"
            @command="(cmd) => cmd === 'logout' && logout()"
          >
            <div class="nav-user">
              <div class="nav-avatar-wrap">
                <div class="nav-avatar">{{ userName.charAt(0) }}</div>
                <div class="nav-avatar-status"></div>
              </div>
              <span class="nav-username">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <div class="custom-dropdown-menu">
                <div class="dropdown-header">
                  <div class="dropdown-avatar">{{ userName.charAt(0) }}</div>
                  <div>
                    <div class="dropdown-name">{{ userName }}</div>
                    <div class="dropdown-role">普通用户</div>
                  </div>
                </div>
                <div
                  class="dropdown-item-wrap"
                  @click="router.push('/my-profile')"
                >
                  <el-icon><User /></el-icon> 个人中心
                </div>
                <div
                  class="dropdown-item-wrap"
                  @click="router.push('/my-orders')"
                >
                  <el-icon><Document /></el-icon> 我的订单
                </div>
                <div class="dropdown-divider"></div>
                <div class="dropdown-item-wrap danger" @click="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </div>
              </div>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <button class="nav-btn nav-btn-ghost" @click="openLogin">
            <el-icon><User /></el-icon> 登录
          </button>
          <button class="nav-btn nav-btn-solid" @click="openRegister">免费注册</button>
        </template>
      </div>
    </nav>

    <main class="user-content">
      <transition name="banner-slide">
        <div v-if="bannerNotice" class="message-banner" @click="goHandleMessageBanner">
          <div class="banner-left">
            <div class="banner-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            </div>
            <div>
              <div class="banner-title">{{ bannerNotice.title }}</div>
              <div class="banner-text">{{ bannerNotice.content }}</div>
            </div>
          </div>
          <div class="banner-actions">
            <button type="button" class="banner-btn close" @click.stop="closeMessageBanner">关闭</button>
          </div>
        </div>
      </transition>
      <router-view />
    </main>

    <SupportChatDrawer
      v-model="supportDrawerVisible"
      @support-read="clearSupportReplyNotifications"
    />

    <!-- Auth Modal -->
    <div v-if="authModalType" class="auth-overlay" @click.self="closeAuthModal">
      <div v-if="authModalType === 'login'" class="auth-modal">
        <div class="auth-modal-header">
          <div class="auth-modal-title">欢迎回来</div>
          <button class="auth-modal-close" @click="closeAuthModal"><el-icon><Close /></el-icon></button>
        </div>
        <div class="auth-modal-body">
          <div class="auth-field"><label>用户名</label><input v-model="loginForm.username" placeholder="请输入用户名" @keyup.enter="handleLogin" /></div>
          <div class="auth-field"><label>密码</label><input v-model="loginForm.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" /></div>
          <button class="auth-btn" :disabled="loginLoading" @click="handleLogin">{{ loginLoading ? '登录中...' : '登录' }}</button>
          <div class="auth-switch">还没有账号？<a @click="switchToRegisterModal">立即注册</a> · <a>忘记密码</a></div>
        </div>
      </div>
      <div v-if="authModalType === 'register'" class="auth-modal">
        <div class="auth-modal-header">
          <div class="auth-modal-title">创建账户</div>
          <button class="auth-modal-close" @click="closeAuthModal"><el-icon><Close /></el-icon></button>
        </div>
        <div class="auth-modal-body">
          <div class="auth-field"><label>用户名</label><input v-model="registerForm.username" placeholder="请输入用户名" /></div>
          <div class="auth-field"><label>密码</label><input v-model="registerForm.password" type="password" placeholder="请输入密码" /></div>
          <button class="auth-btn" :disabled="registerLoading" @click="handleRegister">{{ registerLoading ? '注册中...' : '立即注册' }}</button>
          <div class="auth-switch">已有账号？<a @click="switchToLoginModal">去登录</a></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-layout {
  min-height: 100vh;
  background: var(--bg);
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgba(245, 243, 239, 0.92);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 40px;
  gap: 32px;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
}
.logo-dot {
  width: 8px;
  height: 8px;
  background: var(--accent);
  border-radius: 50%;
}
.nav-links {
  display: flex;
  gap: 4px;
  margin-left: 20px;
}
.nav-link {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.18s;
  text-decoration: none;
}
.nav-link:hover {
  color: var(--text);
  background: rgba(0, 0, 0, 0.05);
}
.nav-link.active {
  color: var(--accent);
  background: var(--accent-light);
  font-weight: 500;
}
.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 24px;
  font-size: 13px;
  font-family: 'Noto Sans SC', sans-serif;
  cursor: pointer;
  transition: all .18s;
  font-weight: 500;
}
.nav-btn-ghost {
  background: none;
  border: 1.5px solid var(--border-dark);
  color: var(--text);
}
.nav-btn-ghost:hover {
  border-color: var(--text);
}
.nav-btn-solid {
  background: var(--accent);
  border: none;
  color: #fff;
}
.nav-btn-solid:hover {
  background: #b02e22;
}
.support-trigger {
  height: 38px;
  border-radius: 20px;
  border: 1px solid var(--border-hover);
  background: var(--white);
  color: var(--text);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 14px;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.18s;
}
.support-trigger:hover {
  color: var(--accent);
  border-color: var(--accent);
}
.message-trigger {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-hover);
  background: var(--white);
  color: var(--muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.18s;
}
.message-trigger:hover {
  color: var(--accent);
  border-color: var(--accent);
}
/* ── Avatar ── */
.nav-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 14px 5px 5px;
  border-radius: 40px;
  border: 1px solid var(--border-hover);
  background: var(--white);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
}
.nav-user:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 10px rgba(200, 56, 42, 0.12);
}
.nav-avatar-wrap {
  position: relative;
  width: 34px;
  height: 34px;
}
.nav-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent) 0%, var(--gold) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 1.5px rgba(200, 56, 42, 0.35);
}
.nav-avatar-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid var(--white);
}
.nav-username {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

/* ── Custom Dropdown ── */
.custom-dropdown-menu {
  width: 210px;
  padding: 6px;
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1), 0 2px 8px rgba(0, 0, 0, 0.06);
}
.dropdown-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 12px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 4px;
}
.dropdown-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent) 0%, var(--gold) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 1.5px rgba(200, 56, 42, 0.3);
  flex-shrink: 0;
}
.dropdown-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  line-height: 1.3;
}
.dropdown-role {
  font-size: 11px;
  color: var(--muted2);
  margin-top: 1px;
}
.dropdown-item-wrap {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border-radius: 9px;
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
  transition: background 0.15s;
}
.dropdown-item-wrap:hover {
  background: var(--bg);
}
.dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: 4px 0;
}
.dropdown-item-wrap.danger {
  color: var(--accent);
}
.dropdown-item-wrap.danger:hover {
  background: var(--accent-light);
}

/* ── Bell ── */
.message-trigger {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid var(--border-hover);
  background: var(--white);
  color: var(--muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
  position: relative;
}
.message-trigger:hover {
  color: var(--accent);
  border-color: var(--accent);
  box-shadow: 0 2px 10px rgba(200, 56, 42, 0.12);
}
.bell-badge-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bell-badge {
  position: absolute;
  top: -10px;
  right: -12px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid var(--white);
  line-height: 1;
}

/* ── Message Popup ── */
.message-pop {
  min-height: 80px;
}
.message-pop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.message-pop-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 6px;
}
.message-pop-count {
  background: var(--accent-light);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 10px;
}
.message-read-all {
  font-size: 12px;
  color: var(--muted2);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.15s;
}
.message-read-all:hover {
  background: var(--bg);
  color: var(--text);
}
.message-read-all:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.message-pop-body {
  max-height: 360px;
  overflow-y: auto;
}
.message-empty {
  padding: 28px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

.message-item {
  border: 1px solid transparent;
  border-radius: 12px;
  padding: 11px 12px;
  margin-bottom: 6px;
  background: var(--bg);
  cursor: pointer;
  transition: background 0.15s;
}
.message-item:last-child {
  margin-bottom: 0;
}
.message-item.unread {
  background: rgba(200, 56, 42, 0.06);
  border-color: rgba(200, 56, 42, 0.2);
}
.message-item-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.message-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--accent-light);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.message-item.read .message-icon {
  background: rgba(0, 0, 0, 0.05);
  color: var(--muted2);
}
.message-meta {
  flex: 1;
  min-width: 0;
}
.message-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: flex;
  align-items: center;
  gap: 5px;
}
.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  flex-shrink: 0;
}
.message-time {
  font-size: 11px;
  color: var(--muted2);
  margin-top: 1px;
}
.message-content {
  margin-top: 5px;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}
.message-item-actions {
  margin-top: 8px;
  display: flex;
  gap: 5px;
}
.msg-btn {
  height: 24px;
  border-radius: 6px;
  padding: 0 9px;
  font-size: 11px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.msg-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.msg-btn.read {
  border: 1px solid var(--border-hover);
  background: var(--white);
  color: var(--muted);
}
.msg-btn.action {
  border: none;
  background: var(--accent);
  color: #fff;
}
.msg-btn.action:hover {
  background: #b02e22;
}
.msg-btn.delete {
  border: 1px solid rgba(200, 56, 42, 0.2);
  background: transparent;
  color: var(--accent);
  margin-left: auto;
}
.msg-btn.delete:hover {
  background: var(--accent-light);
}

/* ── Banner ── */
.banner-slide-enter-active,
.banner-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.28s ease;
}
.banner-slide-enter-from,
.banner-slide-leave-to {
  opacity: 0;
  transform: translateX(48px);
}
.banner-slide-enter-to,
.banner-slide-leave-from {
  opacity: 1;
  transform: translateX(0);
}
.message-banner {
  position: fixed;
  top: 76px;
  right: 24px;
  width: min(380px, calc(100vw - 32px));
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: var(--white);
  border: 1px solid rgba(200, 56, 42, 0.25);
  border-left: 3px solid var(--accent);
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 8px 24px rgba(200, 56, 42, 0.1), 0 2px 6px rgba(0, 0, 0, 0.06);
  z-index: 160;
  cursor: pointer;
}
.banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.banner-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--accent-light);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.banner-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
}
.banner-text {
  font-size: 12px;
  color: var(--muted);
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}
.banner-actions {
  display: flex;
  gap: 7px;
  flex-shrink: 0;
}
.banner-btn {
  height: 30px;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.banner-btn.close {
  border: 1px solid var(--border-hover);
  background: transparent;
  color: var(--muted);
}
.banner-btn.close:hover {
  background: var(--bg);
}
.banner-btn.action {
  border: none;
  background: var(--accent);
  color: #fff;
}
.banner-btn.action:hover {
  background: #b02e22;
}
@media (max-width: 768px) {
  .message-banner {
    top: 72px;
    right: 12px;
    width: calc(100vw - 24px);
  }
}

/* Auth Modal */
.auth-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
.auth-modal {
  background: var(--white);
  border-radius: 20px;
  width: 440px;
  max-height: 88vh;
  overflow-y: auto;
  box-shadow: var(--shadow-lg);
  animation: modalIn 0.25s ease;
}
@keyframes modalIn {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.auth-modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.auth-modal-title {
  font-family: "Playfair Display", serif;
  font-size: 20px;
  font-weight: 700;
}
.auth-modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--muted);
  font-size: 20px;
  padding: 4px;
  transition: color 0.15s;
}
.auth-modal-close:hover {
  color: var(--text);
}
.auth-modal-body {
  padding: 24px;
}
.auth-field {
  margin-bottom: 16px;
}
.auth-field label {
  display: block;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}
.auth-field input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  font-family: "Noto Sans SC", sans-serif;
  background: var(--bg);
  outline: none;
  color: var(--text);
  transition: border-color 0.15s;
}
.auth-field input:focus {
  border-color: var(--accent);
}
.auth-btn {
  width: 100%;
  padding: 13px;
  background: var(--accent);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  font-family: "Noto Sans SC", sans-serif;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(200, 56, 42, 0.25);
  transition: background 0.18s;
  margin-top: 8px;
}
.auth-btn:hover {
  background: #b02e22;
}
.auth-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-switch {
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  margin-top: 14px;
}
.auth-switch a {
  color: var(--accent);
  cursor: pointer;
}
.auth-switch a:hover {
  text-decoration: underline;
}
.gender-row {
  display: flex;
  gap: 8px;
}
.gender-opt {
  flex: 1;
  padding: 9px;
  text-align: center;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s;
}
.gender-opt:hover {
  border-color: var(--border-dark);
  color: var(--text);
}
.gender-opt.active {
  border-color: var(--accent);
  background: var(--accent-light);
  color: var(--accent);
  font-weight: 500;
}
</style>
