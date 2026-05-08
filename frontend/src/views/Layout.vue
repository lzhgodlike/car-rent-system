<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth, getAuth } from '../utils/auth'

const router = useRouter()
const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')

const menus = computed(() => {
  const overview = [
    { path: '/dashboard', label: '运营看板' },
  ]
  const business = [
    { path: '/cars', label: '车辆展厅' },
    { path: '/rent-orders', label: '租车订单' },
    { path: '/return-orders', label: '还车记录' },
    { path: '/fault-reports', label: '车况工单' },
  ]
  const system = [
    { path: '/profile', label: '个人中心' },
  ]
  if (isAdmin.value) {
    system.push({ path: '/users', label: '用户管理' })
    return { overview, business, system }
  }
  return { overview: [], business, system }
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
        <template v-if="menus.overview.length">
          <div class="nav-group-label">概览</div>
          <router-link v-for="item in menus.overview" :key="item.path" :to="item.path" class="side-link">
            {{ item.label }}
          </router-link>
        </template>

        <div class="nav-group-label">业务</div>
        <router-link v-for="item in menus.business" :key="item.path" :to="item.path" class="side-link">
          {{ item.label }}
        </router-link>

        <div class="nav-group-label">系统</div>
        <router-link v-for="item in menus.system" :key="item.path" :to="item.path" class="side-link">
          {{ item.label }}
        </router-link>
      </nav>

      <div class="nav-spacer"></div>
      <button class="side-link side-link-button" type="button" @click="logout">
        退出登录
      </button>
    </aside>

    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>
