<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { setAuth } from '../utils/auth'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
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
        if (phonePattern.test(value)) { callback(); return }
        callback(new Error('请输入合法手机号'))
      },
      trigger: ['blur', 'change'],
    },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || idCardPattern.test(value)) { callback(); return }
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
</script>

<template>
  <div class="login-container">
    <section class="login-card">
      <div class="login-card-head">
        <div class="login-brand">租车管理系统</div>
        <p class="login-subtitle">车辆、订单、还车与维修一站式管理</p>
      </div>

      <el-tabs v-model="activeTab" stretch class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form class="login-form" label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%">
              {{ loading ? '登录中...' : '登录' }}
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
            @submit.prevent="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="registerForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="registerForm.gender">
                <el-radio-button label="男">男</el-radio-button>
                <el-radio-button label="女">女</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-button type="primary" :loading="loading" @click="handleRegister" style="width:100%">
              {{ loading ? '注册中...' : '完成注册' }}
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg);
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--white);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

.login-card-head {
  margin-bottom: 4px;
}

.login-brand {
  font-size: 20px;
  font-weight: 700;
  color: var(--gray-900);
}

.login-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--subtext);
}

.login-tabs {
  margin-top: 20px;
}

.login-form {
  margin-top: 4px;
}

:deep(.el-tabs__header) {
  margin-bottom: 20px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 600;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 8px;
}

:deep(.el-radio-button__inner) {
  border-radius: var(--radius-sm) !important;
}
</style>
