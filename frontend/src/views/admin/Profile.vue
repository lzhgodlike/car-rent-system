<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getAuth, setAuth } from '../../utils/auth'

const auth = getAuth()
const formRef = ref(null)
const form = ref({ realName: '', phone: '', idCard: '', gender: '', password: '' })

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const loadData = async () => {
  const data = await request.get('/users/profile')
  Object.assign(form.value, data, { password: '' })
}
onMounted(loadData)

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await request.put('/users/profile', form.value)
  const latest = await request.get('/auth/me')
  setAuth({ ...auth, userInfo: latest })
  ElMessage.success('已更新')
}
</script>

<template>
  <div class="profile-page">
    <div class="card">
      <div class="card-header"><span class="card-title">基本信息</span></div>
      <div class="card-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" style="max-width:500px;">
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
          <el-form-item label="性别" prop="gender"><el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select></el-form-item>
          <el-form-item label="新密码"><el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" /></el-form-item>
          <el-form-item><el-button type="primary" @click="save">保存修改</el-button></el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 700px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.card-header { padding: 16px 20px; border-bottom: 1px solid var(--border); }
.card-title { font-weight: 500; font-size: 14px; }
.card-body { padding: 20px; }
</style>
