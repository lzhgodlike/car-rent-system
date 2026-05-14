import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { clearAuth, getAuth, isTokenExpired } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

let redirectedForAuth = false

function isPublicPage() {
  const path = router.currentRoute.value?.path || ''
  return path === '/home' || path === '/book' || path === '/login' || path === '/'
}

function isAdminPage() {
  const path = router.currentRoute.value?.path || ''
  return path.startsWith('/admin')
}

function redirectToLogin(message = '登录已失效，请重新登录') {
  if (redirectedForAuth) return
  redirectedForAuth = true
  const isAdmin = isAdminPage()
  clearAuth()
  const target = isAdmin ? '/home' : { path: '/home', query: { login: 1 } }
  router.replace(target).then(() => {
    if (isAdmin) {
      setTimeout(() => ElMessage.error(message), 300)
    }
  }).finally(() => {
    setTimeout(() => { redirectedForAuth = false }, 1000)
  })
}

function isAuthExpired(message, status) {
  if (status === 401) return true
  return typeof message === 'string' && (
    message.includes('UNAUTHORIZED') ||
    message.includes('未登录') ||
    message.includes('登录已失效') ||
    message.includes('token') ||
    message.includes('JWT expired')
  )
}

request.interceptors.request.use((config) => {
  const auth = getAuth()
  if (auth?.token) {
    if (isTokenExpired(auth.token)) {
      redirectToLogin()
      return Promise.reject(new Error('登录已失效'))
    }
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      const message = res.message || '请求失败'
      if (isAuthExpired(message, response.status)) {
        redirectToLogin()
        return Promise.reject(new Error(message))
      }
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return res.data
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络异常'
    if (isAuthExpired(message, status)) {
      redirectToLogin()
      return Promise.reject(error)
    }
    if (isPublicPage() && status === 401) {
      return Promise.reject(error)
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
