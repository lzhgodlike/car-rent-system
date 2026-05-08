<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getAuth } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')

const menus = computed(() => {
  const base = [
    { path: '/cars', label: '车辆展厅', tip: '挑车、看图、比配置' },
    { path: '/rent-orders', label: '租车订单', tip: '查看下单与租期' },
    { path: '/return-orders', label: '还车记录', tip: '追踪归还与里程' },
    { path: '/fault-reports', label: '车况工单', tip: '故障与维修进度' },
    { path: '/profile', label: '个人中心', tip: '账户与身份资料' },
  ]
  if (isAdmin.value) {
    return [
      { path: '/dashboard', label: '运营看板', tip: '订单、收入、趋势' },
      ...base,
      { path: '/users', label: '用户管理', tip: '平台成员维护' },
    ]
  }
  return base
})

const logout = () => {
  clearAuth()
  router.replace('/login')
}
</script>

<template>
  <div class="layout-shell">
    <aside class="layout-side">
      <div class="layout-brand">租车管理系统</div>
      <div class="layout-user-card">
        <span class="layout-user-eyebrow">当前登录</span>
        <strong>{{ auth?.userInfo?.realName || auth?.userInfo?.username }}</strong>
      </div>
      <nav class="side-nav">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="side-link">
          <span>{{ item.label }}</span>
          <small>{{ item.tip }}</small>
        </router-link>
        <button class="side-link side-link-button" type="button" @click="logout">
          <span>退出登录</span>
          <small>返回登录页</small>
        </button>
      </nav>
    </aside>

    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>
