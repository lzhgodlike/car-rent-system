<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { clearAuth, getAuth } from '../utils/auth'
import request from '../utils/request'
import { createNotificationSocket } from '../utils/notificationSocket'

const router = useRouter()
const route = useRoute()
const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const userName = computed(() => auth?.userInfo?.realName || auth?.userInfo?.username || '管理员')
const pageTitle = computed(() => route.meta?.title || '控制台')
const collapsed = ref(false)
const messageLoading = ref(false)
const messageList = ref([])
const messageUnread = ref(0)
const bannerNotice = ref(null)
const notificationSocketConnected = ref(false)
const dismissedBannerIds = new Set()
let notificationSocket = null
let notificationSocketActive = false
let messageFallbackTimer = null

const ensureNotificationSocket = () => {
  if (!notificationSocket) {
    notificationSocket = createNotificationSocket({
      onOpen: () => {
        notificationSocketConnected.value = true
        stopMessageFallbackPolling()
      },
      onClose: () => {
        notificationSocketConnected.value = false
        if (notificationSocketActive) {
          startMessageFallbackPolling()
        }
      },
      onError: () => {
        notificationSocketConnected.value = false
      },
      onMessage: (payload) => {
        if (payload?.type !== 'NOTICE_CREATED' || !payload.payload) return
        receiveRealtimeNotice(payload.payload)
      },
    })
  }
  return notificationSocket
}

const navSections = [
  { label: '概览', items: [
    { path: '/admin/dashboard', icon: 'Odometer', label: '控制台' },
  ]},
  { label: '业务管理', items: [
    { path: '/admin/orders', icon: 'Document', label: '订单管理', badge: 0 },
    { path: '/admin/cars', icon: 'Van', label: '车辆管理' },
    { path: '/admin/car-types', icon: 'PriceTag', label: '车型管理' },
    { path: '/admin/users', icon: 'User', label: '客户管理' },
    { path: '/admin/returns', icon: 'SwitchButton', label: '归还处理' },
  ]},
  { label: '运营', items: [
    { path: '/admin/fault-reports', icon: 'SetUp', label: '维保管理' },
    { path: '/admin/support-conversations', icon: 'ChatRound', label: '客服会话' },
    { path: '/admin/profile', icon: 'Setting', label: '个人中心' },
  ]},
]

const logout = () => {
  clearAuth()
  router.replace('/home')
}

const formatMessageTime = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : ''

const isSupportCreatedNotice = (item, payload = {}) => {
  if (!item) return false
  if (item.messageType !== 'SUPPORT_MESSAGE_CREATED') return false
  if (payload.bizType && item.bizType !== payload.bizType) return false
  if (payload.conversationId && Number(item.bizId) !== Number(payload.conversationId)) return false
  return true
}

const resolveSupportConversationPath = (conversationId) => {
  if (!conversationId) return '/admin/support-conversations'
  return `/admin/support-conversations?conversationId=${conversationId}`
}

const resolveNoticeActionPath = (notice) => {
  if (notice?.messageType === 'RETURN_ORDER_CREATED') return '/admin/returns'
  if (notice?.messageType === 'FAULT_REPORT_CREATED') return '/admin/fault-reports'
  if (notice?.messageType === 'RENT_ORDER_CREATED') return '/admin/orders'
  if (notice?.messageType === 'SUPPORT_MESSAGE_CREATED') {
    return resolveSupportConversationPath(notice?.bizId)
  }
  return '/admin/dashboard'
}

const receiveRealtimeNotice = (notice) => {
  if (!notice?.id) return
  const exists = messageList.value.some(item => item.id === notice.id)
  if (exists) return
  messageList.value = [notice, ...messageList.value.filter(item => item.id !== notice.id)].slice(0, 20)
  if (notice.readStatus === 0) {
    messageUnread.value += 1
    syncMessageBanner(notice)
  }
}

const syncMessageBanner = (latestUnread) => {
  if (!latestUnread) {
    bannerNotice.value = null
    return
  }
  if (dismissedBannerIds.has(latestUnread.id)) {
    return
  }
  if (bannerNotice.value?.id === latestUnread.id) {
    return
  }
  bannerNotice.value = latestUnread
}

const closeMessageBanner = () => {
  if (!bannerNotice.value) return
  dismissedBannerIds.add(bannerNotice.value.id)
  bannerNotice.value = null
}

const isNoticeActionable = (item) => item?.actionable !== false

const goHandleMessageBanner = async () => {
  if (!bannerNotice.value) return
  const current = bannerNotice.value
  await goHandleMessage(current)
  bannerNotice.value = null
}

const handleSupportPrompt = (event) => {
  const detail = event?.detail
  if (!detail?.conversationId) return
  if (detail.action === 'read') {
    clearSupportCreatedNotifications({
      conversationId: detail.conversationId,
      bizType: 'SUPPORT_CONVERSATION',
    })
    return
  }
  if (notificationSocketConnected.value) {
    return
  }
  const syntheticNotice = {
    id: `support-${detail.conversationId}`,
    title: detail.title || '新的客服消息',
    content: detail.content || '有新的客服消息需要处理',
    messageType: 'SUPPORT_MESSAGE_CREATED',
    bizType: 'SUPPORT_CONVERSATION',
    bizId: detail.conversationId,
  }
  syncMessageBanner(syntheticNotice)
}

const refreshMessageStatus = async () => {
  try {
    const page = await request.get('/notifications', { params: { pageNum: 1, pageSize: 1, unreadOnly: true } })
    messageUnread.value = Number(page.summary?.unread || 0)
    syncMessageBanner(page.records?.[0] || null)
  } catch {}
}

const loadMessages = async () => {
  messageLoading.value = true
  try {
    const page = await request.get('/notifications', { params: { pageNum: 1, pageSize: 20 } })
    messageList.value = page.records || []
    messageUnread.value = Number(page.summary?.unread || 0)
  } finally {
    messageLoading.value = false
  }
}

const markMessageRead = async (item) => {
  if (!item || item.readStatus === 1) return
  await request.put(`/notifications/${item.id}/read`)
  item.readStatus = 1
  if (bannerNotice.value?.id === item.id) {
    bannerNotice.value = null
  }
  dismissedBannerIds.add(item.id)
  if (messageUnread.value > 0) {
    messageUnread.value -= 1
  }
}

const goHandleMessage = async (item) => {
  if (!item || !isNoticeActionable(item)) return
  if (item.readStatus === 0) {
    try {
      await markMessageRead(item)
    } catch {}
  }
  await router.push(resolveNoticeActionPath(item))
}

const deleteMessage = async (item) => {
  if (!item) return
  const wasUnread = item.readStatus === 0
  await request.delete(`/notifications/${item.id}`)
  messageList.value = messageList.value.filter(message => message.id !== item.id)
  dismissedBannerIds.add(item.id)
  if (bannerNotice.value?.id === item.id) {
    bannerNotice.value = null
  }
  if (wasUnread && messageUnread.value > 0) {
    messageUnread.value -= 1
  }
}

const markAllMessagesRead = async () => {
  if (!messageUnread.value) return
  await request.put('/notifications/read-all')
  messageList.value = messageList.value.map(item => ({ ...item, readStatus: 1 }))
  messageUnread.value = 0
  bannerNotice.value = null
}

const clearSupportCreatedNotifications = async (payload = {}) => {
  const targets = messageList.value.filter(item => isSupportCreatedNotice(item, payload))
  if (!targets.length) {
    if (isSupportCreatedNotice(bannerNotice.value, payload)) {
      bannerNotice.value = null
    }
    return
  }
  await Promise.all(
    targets.map(item => request.delete(`/notifications/${item.id}`).catch(() => null))
  )
  const targetIds = new Set(targets.map(item => item.id))
  const unreadRemoved = targets.filter(item => item.readStatus === 0).length
  messageList.value = messageList.value.filter(item => !targetIds.has(item.id))
  targets.forEach(item => dismissedBannerIds.add(item.id))
  if (bannerNotice.value && targetIds.has(bannerNotice.value.id)) {
    bannerNotice.value = null
  }
  if (unreadRemoved > 0) {
    messageUnread.value = Math.max(0, messageUnread.value - unreadRemoved)
  }
}

const stopMessageFallbackPolling = () => {
  if (!messageFallbackTimer) return
  clearInterval(messageFallbackTimer)
  messageFallbackTimer = null
}

const startMessageFallbackPolling = () => {
  stopMessageFallbackPolling()
  if (notificationSocketConnected.value) return
  messageFallbackTimer = setInterval(() => {
    refreshMessageStatus()
  }, 45000)
}

const connectNotificationSocket = async () => {
  notificationSocketActive = true
  try {
    await ensureNotificationSocket().connect()
  } catch {
    notificationSocketConnected.value = false
    startMessageFallbackPolling()
  }
}

const disconnectNotificationSocket = () => {
  notificationSocketActive = false
  stopMessageFallbackPolling()
  notificationSocketConnected.value = false
  notificationSocket?.disconnect()
}

onMounted(async () => {
  await loadMessages()
  await connectNotificationSocket()
  if (!notificationSocketConnected.value) {
    await refreshMessageStatus()
  }
  window.addEventListener('admin-support-notice', handleSupportPrompt)
})

onBeforeUnmount(() => {
  disconnectNotificationSocket()
  window.removeEventListener('admin-support-notice', handleSupportPrompt)
})
</script>

<template>
  <div class="admin-layout theme-dark">
    <!-- Sidebar -->
    <aside class="admin-sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <div class="logo-icon"><el-icon><Van /></el-icon></div>
        <div v-show="!collapsed" class="logo-text">
          <div class="logo-title">驰云租车</div>
          <div class="logo-sub">管理系统 v2.0</div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div v-for="section in navSections" :key="section.label" class="nav-section">
          <div v-show="!collapsed" class="nav-label">{{ section.label }}</div>
          <router-link
            v-for="item in section.items" :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-text">{{ item.label }}</span>
            <span v-if="item.badge && !collapsed" class="nav-badge">{{ item.badge }}</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-bottom">
        <div class="user-card" @click="router.push('/admin/profile')">
          <div class="avatar">{{ userName.charAt(0) }}</div>
          <div v-show="!collapsed" class="user-info">
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">超级管理员</div>
          </div>
        </div>
        <button v-show="!collapsed" class="logout-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </button>
      </div>
    </aside>

    <!-- Main -->
    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          <h1 class="header-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <el-popover placement="bottom-end" :width="348" trigger="click" @show="loadMessages">
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
                  <span v-if="messageUnread > 0" class="bell-badge">{{ messageUnread > 99 ? '99+' : messageUnread }}</span>
                </span>
              </button>
            </template>
            <div class="message-pop">
              <div class="message-pop-head">
                <div class="message-pop-title">
                  消息通知
                  <span v-if="messageUnread > 0" class="message-pop-count">{{ messageUnread }} 条未读</span>
                </div>
                <button type="button" class="message-read-all" :disabled="messageUnread === 0" @click="markAllMessagesRead">
                  全部已读
                </button>
              </div>
              <div v-loading="messageLoading" class="message-pop-body">
                <div v-if="messageList.length === 0" class="message-empty">暂无消息</div>
                <div
                  v-for="item in messageList"
                  :key="item.id"
                  class="message-item"
                  :class="{ unread: item.readStatus === 0, read: item.readStatus === 1 }"
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
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14 2 14 8 20 8" />
                      </svg>
                    </div>
                    <div class="message-meta">
                      <div class="message-title">
                        <span v-if="item.readStatus === 0" class="unread-dot"></span>
                        {{ item.title }}
                      </div>
                      <div class="message-time">{{ formatMessageTime(item.createTime) }}</div>
                    </div>
                  </div>
                  <div class="message-content">{{ item.content }}</div>
                  <div class="message-item-actions">
                    <button type="button" class="msg-btn read" :disabled="item.readStatus === 1" @click="markMessageRead(item)">标为已读</button>
                    <button type="button" class="msg-btn action" :disabled="!isNoticeActionable(item)" @click="goHandleMessage(item)">{{ isNoticeActionable(item) ? '去操作' : '已处理' }}</button>
                    <button type="button" class="msg-btn delete" @click="deleteMessage(item)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
        </div>
      </header>
      <main class="admin-content">
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
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex; height: 100vh; overflow: hidden;
  background: var(--bg); color: var(--text);
}

/* Sidebar */
.admin-sidebar {
  width: 220px; background: var(--surface);
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column; flex-shrink: 0;
  transition: width .25s;
}
.admin-sidebar.collapsed { width: 64px; }
.sidebar-logo {
  padding: 18px 20px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; gap: 10px;
}
.logo-icon {
  width: 36px; height: 36px; border-radius: 8px;
  background: var(--accent-light); color: var(--accent);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.logo-title { font-family: 'Bebas Neue', sans-serif; font-size: 22px; letter-spacing: 1px; color: var(--text); }
.logo-sub { font-size: 10px; color: var(--muted); margin-top: -2px; }

.sidebar-nav { flex: 1; overflow-y: auto; padding: 0; }
.nav-section { padding: 16px 12px 8px; }
.nav-label {
  font-size: 10px; color: var(--muted); letter-spacing: 1.5px;
  text-transform: uppercase; padding: 0 8px 8px;
}
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 10px; border-radius: 8px;
  color: var(--muted); cursor: pointer;
  transition: all .18s; font-size: 13px; font-weight: 400;
  text-decoration: none; position: relative;
}
.nav-item:hover { color: var(--text); background: var(--surface2); }
.nav-item.active {
  color: var(--accent); background: var(--accent-dim);
  font-weight: 500;
}
.nav-item .el-icon { font-size: 16px; width: 18px; text-align: center; flex-shrink: 0; }
.nav-badge {
  margin-left: auto; background: var(--danger); color: #fff;
  font-size: 10px; padding: 1px 6px; border-radius: 20px;
  font-family: 'Space Mono', monospace;
}

.sidebar-bottom {
  padding: 12px; border-top: 1px solid var(--border);
}
.user-card {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border-radius: 8px;
  cursor: pointer; transition: background .18s;
}
.user-card:hover { background: var(--surface2); }
.avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), #e25c5c);
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}
.user-name { font-size: 13px; font-weight: 500; }
.user-role { font-size: 11px; color: var(--muted); }
.logout-btn {
  width: 100%; margin-top: 8px; padding: 7px;
  border-radius: 8px; border: 1px solid var(--border);
  background: transparent; color: var(--muted);
  font-size: 12px; cursor: pointer; transition: all .18s;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.logout-btn:hover { color: var(--danger); border-color: var(--danger); }

/* Main */
.admin-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.admin-header {
  height: 56px; background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 18px; cursor: pointer; color: var(--muted); }
.collapse-btn:hover { color: var(--text); }
.header-title { font-family: 'Bebas Neue', sans-serif; font-size: 20px; letter-spacing: .5px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.header-search { width: 260px; }
.header-search :deep(.el-input__wrapper) {
  background: var(--surface2); border: 1px solid var(--border);
  border-radius: 8px; box-shadow: none;
}
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
  color: inherit;
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
@media (max-width: 768px) {
  .message-banner {
    top: 72px;
    right: 12px;
    width: calc(100vw - 24px);
  }
}
.admin-content {
  flex: 1; overflow-y: auto; padding: 24px;
}
</style>
