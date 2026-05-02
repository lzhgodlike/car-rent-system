import { createRouter, createWebHistory } from 'vue-router'
import { clearAuth, getAuth, isTokenExpired } from '../utils/auth'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/cars',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'cars', component: () => import('../views/Cars.vue') },
      { path: 'rent-orders', component: () => import('../views/RentOrders.vue') },
      { path: 'return-orders', component: () => import('../views/ReturnOrders.vue') },
      { path: 'fault-reports', component: () => import('../views/FaultReports.vue') },
      { path: 'profile', component: () => import('../views/Profile.vue') },
      { path: 'users', component: () => import('../views/Users.vue') },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('../views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const auth = getAuth()
  const token = auth?.token || ''
  const hasToken = Boolean(token)

  if (hasToken && isTokenExpired(token)) {
    clearAuth()
    if (to.path !== '/login') {
      next('/login')
      return
    }
  }

  if (to.path !== '/login' && !getAuth()) {
    next('/login')
    return
  }

  if (to.path === '/login' && getAuth()) {
    next('/cars')
    return
  }

  next()
})

export default router
