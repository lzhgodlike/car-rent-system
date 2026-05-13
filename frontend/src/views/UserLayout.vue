<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { clearAuth, setAuth, useAuth, authModalType, openLoginModal, openRegisterModal, closeAuthModal, switchToLoginModal, switchToRegisterModal } from '../utils/auth'

const router = useRouter()
const route = useRoute()
const { isLoggedIn, userInfo } = useAuth()
const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '用户')

const publicNav = [
  { path: '/home', label: '首页' },
  { path: '/book', label: '找车租车' },
]
const privateNav = [
  { path: '/my-orders', label: '我的订单' },
  { path: '/my-profile', label: '个人中心' },
]

const logout = () => {
  clearAuth()
  router.replace('/home')
}

// Handle login query param (from router guard redirect)
watch(() => route.query.login, (val) => {
  if (val === '1' || val === 1) {
    openLogin()
    router.replace({ path: route.path })
  }
}, { immediate: true })

// Auth modal
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', realName: '', phone: '', idCard: '', gender: '' })

const openLogin = () => {
  loginForm.value = { username: '', password: '' }
  openLoginModal()
}
const openRegister = () => {
  registerForm.value = { username: '', password: '', realName: '', phone: '', idCard: '', gender: '' }
  openRegisterModal()
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) { ElMessage.warning('请输入用户名和密码'); return }
  loginLoading.value = true
  try {
    const data = await request.post('/auth/login', loginForm.value)
    setAuth(data)
    closeAuthModal()
    const role = data.userInfo?.role
    if (role === 'ADMIN') {
      router.replace('/admin/dashboard')
    }
  } finally { loginLoading.value = false }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请输入用户名和密码'); return
  }
  registerLoading.value = true
  try {
    await request.post('/auth/register', registerForm.value)
    ElMessage.success('注册成功，请登录')
    loginForm.value = { username: registerForm.value.username, password: registerForm.value.password }
    switchToLoginModal()
  } finally { registerLoading.value = false }
}
</script>

<template>
  <div class="user-layout">
    <nav class="top-nav">
      <router-link to="/home" class="nav-logo">
        <span class="logo-dot"></span> 驰云租车
      </router-link>

      <div class="nav-links">
        <router-link
          v-for="item in publicNav" :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: route.path === item.path }"
        >
          {{ item.label }}
        </router-link>
        <template v-if="isLoggedIn">
          <router-link
            v-for="item in privateNav" :key="item.path"
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
          <el-dropdown trigger="click" @command="(cmd) => cmd === 'logout' && logout()">
            <div class="nav-user">
              <div class="nav-avatar">{{ userName.charAt(0) }}</div>
              <span class="nav-username">{{ userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/my-profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/my-orders')">我的订单</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <button class="nav-btn nav-btn-ghost" @click="openLogin"><el-icon><User /></el-icon> 登录</button>
          <button class="nav-btn nav-btn-solid" @click="openRegister">免费注册</button>
        </template>
      </div>
    </nav>

    <main class="user-content">
      <router-view />
    </main>

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
.user-layout { min-height: 100vh; background: var(--bg); }

.top-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 100;
  height: 64px;
  background: rgba(245,243,239,0.92); backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border);
  display: flex; align-items: center;
  padding: 0 40px; gap: 32px;
}
.nav-logo {
  display: flex; align-items: center; gap: 10px;
  font-size: 20px; font-weight: 700; color: var(--text);
  text-decoration: none;
}
.logo-dot { width: 8px; height: 8px; background: var(--accent); border-radius: 50%; }
.nav-links { display: flex; gap: 4px; margin-left: 20px; }
.nav-link {
  padding: 6px 14px; border-radius: 20px; font-size: 14px;
  color: var(--muted); cursor: pointer; transition: all .18s;
  text-decoration: none;
}
.nav-link:hover { color: var(--text); background: rgba(0,0,0,0.05); }
.nav-link.active { color: var(--accent); background: var(--accent-light); font-weight: 500; }
.nav-right { margin-left: auto; display: flex; align-items: center; gap: 12px; }
.nav-user {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 12px 4px 4px;
  border-radius: 24px; border: 1px solid var(--border-hover);
  background: var(--white); transition: border-color .18s;
}
.nav-user:hover { border-color: var(--accent); }
.nav-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--gold));
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 12px; font-weight: 700;
}
.nav-username { font-size: 13px; font-weight: 500; color: var(--text); }

/* Navbar buttons - match design spec */
.nav-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border-radius: 24px; font-size: 13px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .18s;
  font-weight: 500;
}
.nav-btn-ghost {
  background: none; border: 1.5px solid var(--border-dark); color: var(--text);
}
.nav-btn-ghost:hover { border-color: var(--text); }
.nav-btn-solid {
  background: var(--accent); border: none; color: #fff;
}
.nav-btn-solid:hover { background: #b02e22; }

.user-content { padding-top: 64px; }

/* Auth Modal */
.auth-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.auth-modal {
  background: var(--white); border-radius: 20px;
  width: 440px; max-height: 88vh; overflow-y: auto;
  box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(.97); } to { opacity: 1; transform: none; } }
.auth-modal-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.auth-modal-title { font-family: 'Playfair Display', serif; font-size: 20px; font-weight: 700; }
.auth-modal-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; padding: 4px; transition: color .15s; }
.auth-modal-close:hover { color: var(--text); }
.auth-modal-body { padding: 24px; }
.auth-field { margin-bottom: 16px; }
.auth-field label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.auth-field input {
  width: 100%; padding: 10px 14px; border: 1.5px solid var(--border); border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans SC', sans-serif; background: var(--bg);
  outline: none; color: var(--text); transition: border-color .15s;
}
.auth-field input:focus { border-color: var(--accent); }
.auth-btn {
  width: 100%; padding: 13px; background: var(--accent); color: #fff; border: none; border-radius: 12px;
  font-size: 15px; font-weight: 500; font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  box-shadow: 0 4px 16px rgba(200,56,42,0.25); transition: background .18s; margin-top: 8px;
}
.auth-btn:hover { background: #b02e22; }
.auth-btn:disabled { opacity: .6; cursor: not-allowed; }
.auth-switch { text-align: center; font-size: 13px; color: var(--muted); margin-top: 14px; }
.auth-switch a { color: var(--accent); cursor: pointer; }
.auth-switch a:hover { text-decoration: underline; }
.gender-row { display: flex; gap: 8px; }
.gender-opt {
  flex: 1; padding: 9px; text-align: center;
  border: 1.5px solid var(--border); border-radius: 10px;
  font-size: 14px; color: var(--muted); cursor: pointer; transition: all .15s;
}
.gender-opt:hover { border-color: var(--border-dark); color: var(--text); }
.gender-opt.active { border-color: var(--accent); background: var(--accent-light); color: var(--accent); font-weight: 500; }
</style>
