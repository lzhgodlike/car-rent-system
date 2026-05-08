<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth, getAuth } from '../utils/auth'

const router = useRouter()
const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')

const menus = computed(() => {
  const base = [
    { path: '/cars', label: '车辆展厅' },
    { path: '/rent-orders', label: '租车订单' },
    { path: '/return-orders', label: '还车记录' },
    { path: '/fault-reports', label: '车况工单' },
    { path: '/profile', label: '个人中心' },
  ]
  if (isAdmin.value) {
    return [
      { path: '/dashboard', label: '运营看板' },
      ...base,
      { path: '/users', label: '用户管理' },
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
        <strong>{{ auth?.userInfo?.realName || auth?.userInfo?.username }}</strong>
      </div>
      <nav class="side-nav">
        <router-link v-for="item in menus" :key="item.path" :to="item.path" class="side-link">
          {{ item.label }}
        </router-link>
        <button class="side-link side-link-button" type="button" @click="logout">
          退出登录
        </button>
      </nav>
    </aside>

    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>
