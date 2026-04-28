<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const users = ref([])
const dialogVisible = ref(false)
const currentId = ref(null)
const form = reactive({
  username: '',
  realName: '',
  phone: '',
  idCard: '',
  gender: '',
  role: 'USER',
  status: 1,
  password: '',
})

const loadUsers = async () => {
  users.value = await request.get('/users')
}

onMounted(loadUsers)

const adminCount = computed(() => users.value.filter((item) => item.role === 'ADMIN').length)
const normalCount = computed(() => users.value.filter((item) => item.role !== 'ADMIN').length)
const maleCount = computed(() => users.value.filter((item) => item.gender === '男').length)
const femaleCount = computed(() => users.value.filter((item) => item.gender === '女').length)

const roleLabel = (role) => (role === 'ADMIN' ? '管理员' : '普通用户')
const roleTone = (role) => (role === 'ADMIN' ? 'danger' : 'neutral')
const statusLabel = (status) => (Number(status) === 1 ? '启用' : '停用')
const statusTone = (status) => (Number(status) === 1 ? 'success' : 'warning')

const openEdit = (row) => {
  currentId.value = row.id
  Object.assign(form, {
    username: row.username || '',
    realName: row.realName || '',
    phone: row.phone || '',
    idCard: row.idCard || '',
    gender: row.gender || '',
    role: row.role || 'USER',
    status: Number(row.status ?? 1),
    password: '',
  })
  dialogVisible.value = true
}

const saveUser = async () => {
  await request.put(`/users/${currentId.value}`, form)
  ElMessage.success('用户信息已更新')
  dialogVisible.value = false
  await loadUsers()
}
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Members</div>
      <h1 class="hero-title">用户管理</h1>
      <p class="hero-desc">快速查看系统内成员规模、角色分布和基础身份信息，也可以直接编辑用户资料与账号状态。</p>
      <div class="metric-strip">
        <div class="metric-pill"><span>用户总数</span><strong>{{ users.length }}</strong></div>
        <div class="metric-pill"><span>管理员</span><strong>{{ adminCount }}</strong></div>
        <div class="metric-pill"><span>普通用户</span><strong>{{ normalCount }}</strong></div>
        <div class="metric-pill"><span>男女分布</span><strong>{{ maleCount }}/{{ femaleCount }}</strong></div>
      </div>
    </section>

    <div class="page-card">
      <div class="page-header compact-page-head">
        <div>
          <h2 class="page-title">用户信息管理</h2>
          <p class="page-desc">管理员可查看并编辑系统用户信息，便于后续运营与账号管理。</p>
        </div>
      </div>

      <div class="summary-grid">
        <div class="summary-card"><span>用户总数</span><strong>{{ users.length }}</strong></div>
        <div class="summary-card"><span>管理员</span><strong>{{ adminCount }}</strong></div>
        <div class="summary-card"><span>普通用户</span><strong>{{ normalCount }}</strong></div>
        <div class="summary-card"><span>男女用户</span><strong>{{ maleCount }}/{{ femaleCount }}</strong></div>
      </div>

      <div class="section-card">
        <div class="section-head">
          <div>
            <h3>平台成员列表</h3>
            <p>支持查看用户名、联系方式、身份信息、角色与账号状态，并可直接编辑。</p>
          </div>
        </div>

        <div class="table-shell">
          <el-table :data="users" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="phone" label="手机号" />
            <el-table-column prop="idCard" label="身份证号" />
            <el-table-column prop="gender" label="性别" width="90" />
            <el-table-column label="角色" width="110">
              <template #default="scope">
                <span class="status-badge" :class="roleTone(scope.row.role)">
                  {{ roleLabel(scope.row.role) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.status)">
                  {{ statusLabel(scope.row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button size="small" type="primary" @click="openEdit(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑用户信息" width="560px">
      <el-form label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
