import { ref, computed } from 'vue'

const STORAGE_KEY = 'car-rental-auth'

// 响应式 auth 状态
const authData = ref(null)

// 全局登录弹窗状态
export const authModalType = ref(null) // null | 'login' | 'register'

export function openLoginModal() { authModalType.value = 'login' }
export function openRegisterModal() { authModalType.value = 'register' }
export function closeAuthModal() { authModalType.value = null }
export function switchToLoginModal() { authModalType.value = 'login' }
export function switchToRegisterModal() { authModalType.value = 'register' }

// 初始化：从 localStorage 读取
function initAuth() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) { authData.value = null; return }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || !parsed.token) { authData.value = null; return }
    if (isTokenExpired(parsed.token)) { authData.value = null; localStorage.removeItem(STORAGE_KEY); return }
    authData.value = parsed
  } catch { authData.value = null; localStorage.removeItem(STORAGE_KEY) }
}

function decodeJwtPayload(token) {
  const segments = token.split('.')
  if (segments.length < 2) return null
  try {
    const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const json = decodeURIComponent(atob(padded).split('').map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''))
    return JSON.parse(json)
  } catch { return null }
}

export function isTokenExpired(token) {
  if (!token) return true
  const payload = decodeJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') return true
  return payload.exp * 1000 <= Date.now()
}

export function getAuth() {
  // 每次调用时检查是否需要重新初始化
  if (!authData.value) initAuth()
  return authData.value
}

// 响应式 getter
export function useAuth() {
  if (!authData.value) initAuth()
  const isLoggedIn = computed(() => !!authData.value)
  const userInfo = computed(() => authData.value?.userInfo || null)
  const isAdmin = computed(() => authData.value?.userInfo?.role === 'ADMIN')
  return { authData, isLoggedIn, userInfo, isAdmin }
}

export function setAuth(data) {
  authData.value = data
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function clearAuth() {
  authData.value = null
  localStorage.removeItem(STORAGE_KEY)
}

// 初始化
initAuth()
