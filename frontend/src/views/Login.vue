<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { setAuth } from '../utils/auth'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const tiltCard = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  idCard: '',
  gender: '',
})

const phonePattern = /^1[3-9]\d{9}$/
const idCardPattern = /^(\d{15}|\d{17}[\dXx])$/

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (phonePattern.test(value)) {
          callback()
          return
        }
        callback(new Error('请输入合法手机号'))
      },
      trigger: ['blur', 'change'],
    },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || idCardPattern.test(value)) {
          callback()
          return
        }
        callback(new Error('请输入合法身份证号'))
      },
      trigger: ['blur', 'change'],
    },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await request.post('/auth/login', loginForm)
    setAuth(data)
    ElMessage.success('登录成功')
    router.replace('/cars')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先修正注册信息')
    return
  }
  loading.value = true
  try {
    await request.post('/auth/register', registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
  } finally {
    loading.value = false
  }
}

const handleMouseMove = (event) => {
  const card = tiltCard.value
  if (!card) {
    return
  }

  const rect = card.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2
  const maxTilt = 2.5
  const rotateY = ((x - centerX) / centerX) * maxTilt
  const rotateX = -((y - centerY) / centerY) * maxTilt

  card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.006)`
}

const handleMouseLeave = () => {
  const card = tiltCard.value
  if (!card) {
    return
  }

  card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1)'
}
</script>

<template>
  <div class="login-container animated-background">
    <svg class="filter-defs" aria-hidden="true">
      <filter id="glass-distortion" x="0%" y="0%" width="100%" height="100%" filterUnits="objectBoundingBox">
        <feTurbulence type="fractalNoise" baseFrequency="0.001 0.005" numOctaves="1" seed="17" result="turbulence" />
        <feComponentTransfer in="turbulence" result="mapped">
          <feFuncR type="gamma" amplitude="1" exponent="10" offset="0.5" />
          <feFuncG type="gamma" amplitude="0" exponent="1" offset="0" />
          <feFuncB type="gamma" amplitude="0" exponent="1" offset="0.5" />
        </feComponentTransfer>
        <feGaussianBlur in="turbulence" stdDeviation="3" result="softMap" />
        <feSpecularLighting
          in="softMap"
          surfaceScale="5"
          specularConstant="1"
          specularExponent="100"
          lighting-color="white"
          result="specLight"
        >
          <fePointLight x="-200" y="-200" z="300" />
        </feSpecularLighting>
        <feComposite in="specLight" operator="arithmetic" k1="0" k2="1" k3="1" k4="0" result="litImage" />
        <feDisplacementMap in="SourceGraphic" in2="softMap" scale="200" xChannelSelector="R" yChannelSelector="G" />
      </filter>
    </svg>

    <section class="login-scene-copy">
      <div class="login-badge">RoadFlow Car Rental</div>
      <h1>租车管理系统</h1>
      <p></p>
      <div class="login-highlight-grid">
        <article class="login-highlight-card">
          <span>车辆展厅</span>
          <strong>图片化展示车辆、状态、车牌与日租价格</strong>
        </article>
        <article class="login-highlight-card">
          <span>订单运营</span>
          <strong>支持按日、按月、按年查看订单趋势与收入变化</strong>
        </article>
        <article class="login-highlight-card">
          <span>安全体验</span>
          <strong>车牌与身份证校验、Token 失效自动回到登录页</strong>
        </article>
      </div>
    </section>

    <section
      ref="tiltCard"
      class="glass-component login-card"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <div class="glass-effect"></div>
      <div class="glass-tint"></div>
      <div class="glass-shine"></div>

      <div class="glass-content">
        <div class="login-box-head">
          <div class="layout-topbar-label">欢迎回来</div>
          <h2 class="login-title">登录与注册</h2>
        </div>

        <el-tabs v-model="activeTab" stretch class="login-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form class="login-form" label-position="top" @submit.prevent>
              <div class="form-group">
                <el-input v-model="loginForm.username" class="glass-input-shell" placeholder="请输入用户名" />
              </div>

              <div class="form-group">
                <el-input
                  v-model="loginForm.password"
                  class="glass-input-shell"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                />
              </div>

              <el-button class="glass-button" type="primary" :loading="loading" @click="handleLogin">
                立即登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              class="login-form"
              label-position="top"
              :model="registerForm"
              :rules="registerRules"
              status-icon
              @submit.prevent
            >
              <el-form-item class="form-group" prop="username">
                <el-input v-model="registerForm.username" class="glass-input-shell" placeholder="请输入用户名" />
              </el-form-item>

              <el-form-item class="form-group" prop="password">
                <el-input
                  v-model="registerForm.password"
                  class="glass-input-shell"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                />
              </el-form-item>

              <el-form-item class="form-group" prop="realName">
                <el-input v-model="registerForm.realName" class="glass-input-shell" placeholder="请输入真实姓名" />
              </el-form-item>

              <el-form-item class="form-group" prop="phone">
                <el-input v-model="registerForm.phone" class="glass-input-shell" placeholder="请输入手机号" />
              </el-form-item>

              <el-form-item class="form-group" prop="idCard">
                <el-input v-model="registerForm.idCard" class="glass-input-shell" placeholder="请输入身份证号" />
              </el-form-item>

              <el-form-item class="form-group gender-form-item" prop="gender">
                <div class="gender-row">
                  <div class="gender-label">性别</div>
                  <el-radio-group v-model="registerForm.gender" class="glass-radio-group">
                    <el-radio-button label="男">男</el-radio-button>
                    <el-radio-button label="女">女</el-radio-button>
                  </el-radio-group>
                </div>
              </el-form-item>

              <el-button class="glass-button" type="primary" :loading="loading" @click="handleRegister">
                完成注册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
  </div>
</template>

<style scoped>
.filter-defs {
  position: absolute;
  width: 0;
  height: 0;
}

.login-container {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 1.15fr) minmax(420px, 500px);
  align-items: center;
  gap: 24px;
  padding: 48px;
  position: relative;
  overflow: hidden;
}

.animated-background {
  background:
    radial-gradient(circle at 14% 18%, rgba(255, 255, 255, 0.14), transparent 18%),
    radial-gradient(circle at 78% 16%, rgba(255, 169, 77, 0.18), transparent 20%),
    radial-gradient(circle at 82% 78%, rgba(255, 234, 167, 0.12), transparent 22%),
    linear-gradient(135deg, #0c1017 0%, #1b1f2b 26%, #35281f 58%, #7a4b22 100%);
}

.login-container::before {
  content: '';
  position: absolute;
  inset: auto auto -120px -80px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 199, 118, 0.22), transparent 65%);
  filter: blur(12px);
}

.login-container::after {
  content: '';
  position: absolute;
  inset: 70px -80px auto auto;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(145, 183, 255, 0.16), transparent 68%);
  filter: blur(10px);
}

.login-scene-copy {
  position: relative;
  z-index: 2;
  color: #fff;
  max-width: 640px;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 244, 232, 0.88);
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  backdrop-filter: blur(10px);
}

.login-scene-copy h1 {
  margin: 18px 0 14px;
  font-size: clamp(40px, 5vw, 62px);
  line-height: 1.02;
  letter-spacing: 0.02em;
}

.login-scene-copy p {
  margin: 0 0 28px;
  color: rgba(255, 244, 232, 0.8);
  line-height: 1.8;
  font-size: 15px;
}

.login-highlight-grid {
  display: grid;
  gap: 14px;
}

.login-highlight-card {
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(255, 228, 196, 0.12);
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(10px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.login-highlight-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 244, 232, 0.62);
}

.login-highlight-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
  line-height: 1.5;
  color: #fff8ef;
}

.login-card {
  width: min(100%, 460px);
  position: relative;
  justify-self: start;
  margin-left: -40%;
  border-radius: 28px;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.22), 0 8px 24px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s ease-out;
  will-change: transform;
  z-index: 2;
}

.glass-component {
  transform-style: preserve-3d;
}

.glass-effect {
  position: absolute;
  inset: 0;
  z-index: 0;
  backdrop-filter: blur(5px);
  filter: url(#glass-distortion);
  isolation: isolate;
  border-radius: 28px;
}

.glass-tint {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, rgba(7, 12, 20, 0.34), rgba(7, 12, 20, 0.16));
  border-radius: 28px;
}

.glass-shine {
  position: absolute;
  inset: 0;
  z-index: 2;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 28px;
  box-shadow:
    inset 1px 1px 8px rgba(255, 255, 255, 0.18),
    inset -1px -1px 8px rgba(255, 255, 255, 0.08);
  pointer-events: none;
}

.glass-content {
  position: relative;
  z-index: 3;
  padding: 38px 36px 34px;
  color: #fff;
}

.login-box-head {
  margin-bottom: 14px;
}

.layout-topbar-label {
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 244, 232, 0.68);
}

.login-title {
  margin: 8px 0 10px;
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
}

.login-subtitle {
  margin: 0;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.7;
  font-size: 14px;
}

.form-group {
  width: 98%;
  justify-content: center;
  margin-left: 3px;
  margin-bottom: 20px;
  margin-top: 2px;
}

:deep(.el-form-item__error) {
  color: #ffb3b3;
}

:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px rgba(255, 107, 107, 0.85) !important;
  background: rgba(255, 107, 107, 0.12) !important;
}

:deep(.el-form-item.is-error .el-radio-button__inner) {
  box-shadow: 0 0 0 1px rgba(255, 107, 107, 0.75) inset !important;
}

.gender-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.76);
  white-space: nowrap;
  line-height: 1;
}

.gender-row {
  display: grid;
  grid-template-columns: 44px 1fr;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.gender-form-item {
  margin-bottom: 24px;
}

:deep(.login-tabs .el-tabs__header) {
  margin-bottom: 28px;
}

:deep(.login-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.login-tabs .el-tabs__nav-scroll) {
  overflow: visible;
}

:deep(.login-tabs .el-tabs__nav) {
  width: 100%;
  padding: 6px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 8px 24px rgba(0, 0, 0, 0.12);
}

:deep(.login-tabs .el-tabs__item) {
  height: 46px;
  border-radius: 14px;
  color: rgba(255, 255, 255, 0.66);
  font-weight: 700;
  letter-spacing: 0.02em;
  transition: all 0.25s ease;
}

:deep(.login-tabs .el-tabs__item:hover) {
  color: #fff;
}

:deep(.login-tabs .el-tabs__item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 214, 153, 0.16));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 10px 20px rgba(0, 0, 0, 0.12);
}

:deep(.login-tabs .el-tabs__active-bar) {
  display: none;
}

:deep(.glass-input-shell .el-input__wrapper) {
  min-height: 46px;
  box-shadow: none !important;
  border: none !important;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(5px);
}

:deep(.glass-input-shell .el-input__inner) {
  color: #fff !important;
}

:deep(.glass-input-shell .el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.72) !important;
}

:deep(.glass-radio-group) {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 50%;
}

:deep(.glass-radio-group .el-radio-button__inner) {
  width: 100%;
  border: none !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 0.78) !important;
  box-shadow: none !important;
}

:deep(.glass-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.22) !important;
  color: #fff !important;
}

.glass-button {
  width: 98%;
  min-height: 48px;
  justify-content: center;
  margin-left: 3px;
  margin-bottom: 2px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(255, 198, 112, 0.45), rgba(255, 255, 255, 0.18));
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(5px);
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
}

.glass-button:hover {
  background: linear-gradient(135deg, rgba(255, 210, 140, 0.54), rgba(255, 255, 255, 0.24));
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.2);
}

@media (max-width: 980px) {
  .login-container {
    grid-template-columns: 1fr;
    gap: 28px;
    padding: 28px 18px;
  }

  .login-card {
    justify-self: stretch;
    width: 100%;
    margin-left: 0;
  }

  .login-scene-copy h1 {
    font-size: 42px;
  }
}
</style>
