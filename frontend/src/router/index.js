import { createRouter, createWebHistory } from 'vue-router'
import { clearAuth, getAuth, isTokenExpired, setAuth } from '../utils/auth'
import request from '../utils/request'

const adminRoutes = [
  { path: '', redirect: '/admin/dashboard' },
  { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '控制台' } },
  { path: 'cars', name: 'AdminCars', component: () => import('../views/admin/Cars.vue'), meta: { title: '车辆管理' } },
  { path: 'car-types', name: 'AdminCarTypes', component: () => import('../views/admin/CarTypes.vue'), meta: { title: '车型管理' } },
  { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/Orders.vue'), meta: { title: '订单管理' } },
  { path: 'returns', name: 'AdminReturns', component: () => import('../views/admin/Returns.vue'), meta: { title: '归还处理' } },
  { path: 'fault-reports', name: 'AdminFaultReports', component: () => import('../views/admin/FaultReports.vue'), meta: { title: '维保管理' } },
  { path: 'support-conversations', name: 'AdminSupportConversations', component: () => import('../views/admin/SupportConversations.vue'), meta: { title: '客服会话' } },
  { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { title: '客户管理' } },
  { path: 'profile', name: 'AdminProfile', component: () => import('../views/admin/Profile.vue'), meta: { title: '个人中心' } },
]

const publicUserRoutes = [
  { path: '', redirect: '/home' },
  { path: 'home', name: 'UserHome', component: () => import('../views/user/Home.vue'), meta: { title: '首页', public: true } },
  { path: 'book', name: 'UserBook', component: () => import('../views/user/Book.vue'), meta: { title: '找车租车', public: true } },
]

const privateUserRoutes = [
  { path: 'my-orders', name: 'UserOrders', component: () => import('../views/user/Orders.vue'), meta: { title: '我的订单' } },
  { path: 'my-profile', name: 'UserProfile', component: () => import('../views/user/Profile.vue'), meta: { title: '个人中心' } },
]

const routes = [
  { path: '/admin', component: () => import('../views/AdminLayout.vue'), children: adminRoutes, meta: { requiresAuth: true, role: 'ADMIN' } },
  { path: '/', component: () => import('../views/UserLayout.vue'), children: [...publicUserRoutes, ...privateUserRoutes] },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0, behavior: 'smooth' }
  },
})

// token 续期防抖
let refreshPromise = null
async function refreshToken() {
  if (refreshPromise) return refreshPromise
  refreshPromise = request.post('/auth/refresh').then(data => {
    const auth = getAuth()
    setAuth({ ...auth, token: data.token, userInfo: data.userInfo })
  }).catch(() => {}).finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

router.beforeEach(async (to, from, next) => {
  const auth = getAuth()
  const token = auth?.token || ''

  // Token 过期 → 清除，不续期
  if (token && isTokenExpired(token)) {
    clearAuth()
  }

  const currentAuth = getAuth()

  // Token 未过期，切换页面时续期
  if (currentAuth?.token && !isTokenExpired(currentAuth.token)) {
    refreshToken()
  }

  const requiresAuth = to.matched.some(r => r.meta.requiresAuth)
  const requiresAdmin = to.matched.some(r => r.meta.role === 'ADMIN')

  // 管理员页面需要 ADMIN 角色
  if (requiresAdmin && (!currentAuth || currentAuth.userInfo?.role !== 'ADMIN')) {
    return next('/home')
  }

  // 需要登录但未登录 → 跳首页并打开登录弹窗
  if (requiresAuth && !currentAuth) {
    return next({ path: '/home', query: { login: 1 } })
  }

  // 已登录管理员访问用户页面 → 跳管理后台
  if (currentAuth && currentAuth.userInfo?.role === 'ADMIN' && !to.path.startsWith('/admin')) {
    return next('/admin/dashboard')
  }

  next()
})

export default router
