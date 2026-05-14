<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { clearAuth, getAuth } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const userName = computed(() => auth?.userInfo?.realName || auth?.userInfo?.username || '管理员')
const pageTitle = computed(() => route.meta?.title || '控制台')
const collapsed = ref(false)

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
    { path: '/admin/profile', icon: 'Setting', label: '个人中心' },
  ]},
]

const logout = () => {
  clearAuth()
  router.replace('/home')
}
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
          <el-badge :value="3" :max="99" class="notif-badge">
            <el-button :icon="Bell" circle />
          </el-badge>
        </div>
      </header>
      <main class="admin-content">
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
.notif-badge :deep(.el-button) {
  background: var(--surface2); border: 1px solid var(--border);
  color: var(--muted);
}
.admin-content {
  flex: 1; overflow-y: auto; padding: 24px;
}
</style>
