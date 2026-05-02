<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth, setAuth } from '../utils/auth'

const auth = getAuth()
const formRef = ref(null)
const form = reactive({
  realName: '',
  phone: '',
  idCard: '',
  gender: '',
  password: '',
})

const phonePattern = /^1[3-9]\d{9}$/
const idCardPattern = /^(\d{15}|\d{17}[\dXx])$/

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: (_r, value, cb) => phonePattern.test(value) ? cb() : cb(new Error('请输入合法手机号')), trigger: ['blur', 'change'] },
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { validator: (_r, value, cb) => idCardPattern.test(value) ? cb() : cb(new Error('请输入合法身份证号')), trigger: ['blur', 'change'] },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const profileReady = computed(() => [form.realName, form.phone, form.idCard, form.gender].filter(Boolean).length)

const loadData = async () => {
  const data = await request.get('/users/profile')
  Object.assign(form, data, { password: '' })
}

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先修正表单信息')
    return
  }
  await request.put('/users/profile', form)
  const latest = await request.get('/auth/me')
  setAuth({ ...auth, userInfo: latest })
  ElMessage.success('个人信息已更新')
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Profile</div>
      <h1 class="hero-title">个人中心</h1>
      <p class="hero-desc">在这里维护你的姓名、联系方式、身份证信息和登录密码，保证租车和还车流程中的身份资料完整。</p>
      <div class="metric-strip">
        <div class="metric-pill"><span>当前账号</span><strong>{{ auth?.userInfo?.username || '-' }}</strong></div>
        <div class="metric-pill"><span>已填写字段</span><strong>{{ profileReady }}/4</strong></div>
        <div class="metric-pill"><span>角色</span><strong>{{ auth?.userInfo?.role || '-' }}</strong></div>
      </div>
    </section>

    <div class="page-card">
      <div class="section-card profile-form-card">
        <div class="section-head">
          <div>
            <h3>基础资料</h3>
            <p>更新后的资料会同步刷新当前登录用户信息。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" /></el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" style="width: 100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="save">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-form-card {
  max-width: 760px;
}
</style>
