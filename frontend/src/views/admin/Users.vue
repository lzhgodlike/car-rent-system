<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const users = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const dialogVisible = ref(false)
const currentId = ref(null)
const form = ref({ username: '', realName: '', phone: '', idCard: '', gender: '', role: 'USER', status: 1, password: '' })
const notifyDialogVisible = ref(false)
const notifySending = ref(false)
const notifyTargetName = ref('')
const notifyForm = ref({ receiverId: null, title: '', content: '' })
const filterRole = ref('')
const filterStatus = ref('')
const keyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    if (filterRole.value) params.role = filterRole.value
    if (filterStatus.value !== '') params.status = filterStatus.value
    if (keyword.value) params.keyword = keyword.value
    const page = await request.get('/users', { params })
    users.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
const onFilterChange = () => { currentPage.value = 1; loadData() }
const onReset = () => { keyword.value = ''; filterRole.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
let searchTimer = null
const onKeywordChange = () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { currentPage.value = 1; loadData() }, 300) }
onMounted(loadData)

const openEdit = (row) => {
  currentId.value = row.id
  form.value = { username: row.username, realName: row.realName, phone: row.phone, idCard: row.idCard, gender: row.gender, role: row.role, status: Number(row.status), password: '' }
  dialogVisible.value = true
}
const saveUser = async () => {
  await request.put(`/users/${currentId.value}`, form.value)
  ElMessage.success('已更新'); dialogVisible.value = false; loadData()
}
const deleteUser = async (row) => {
  await request.delete(`/users/${row.id}`); ElMessage.success('已删除'); loadData()
}
const toggleUserStatus = async (row) => {
  if (row.role !== 'USER') return
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  await request.put(`/users/${row.id}/status`, { status: nextStatus })
  ElMessage.success(`已${actionText}`)
  loadData()
}
const openNotify = (row) => {
  if (row.role !== 'USER') {
    ElMessage.warning('只能向普通用户发送消息')
    return
  }
  notifyTargetName.value = row.realName || row.username || '用户'
  notifyForm.value = { receiverId: row.id, title: '', content: '' }
  notifyDialogVisible.value = true
}
const sendCustomNotify = async () => {
  if (!notifyForm.value.title.trim()) {
    ElMessage.warning('请输入消息标题')
    return
  }
  if (!notifyForm.value.content.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }
  notifySending.value = true
  try {
    await request.post('/notifications/admin/custom', {
      receiverId: notifyForm.value.receiverId,
      title: notifyForm.value.title.trim(),
      content: notifyForm.value.content.trim()
    })
    ElMessage.success('消息已发送')
    notifyDialogVisible.value = false
  } finally {
    notifySending.value = false
  }
}
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="summary-strip">
        <span class="sum-item">用户总数 <strong>{{ total }}</strong></span>
        <span class="sum-item">管理员 <strong>{{ summary.admin ?? 0 }}</strong></span>
        <span class="sum-item">普通用户 <strong>{{ summary.normal ?? 0 }}</strong></span>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <el-input v-model="keyword" placeholder="搜索用户名、姓名、手机号…" clearable size="small" style="width:200px;" @input="onKeywordChange" @clear="onKeywordChange" />
        <el-select v-model="filterRole" placeholder="角色" clearable size="small" style="width:120px;" @change="onFilterChange">
          <el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width:100px;" @change="onFilterChange">
          <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
        </el-select>
        <button class="btn-sm btn-sm-ghost" @click="onReset"><el-icon><RefreshLeft /></el-icon></button>
      </div>
    </div>
    <div class="card">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130"><template #default="{row}"><span class="font-mono">{{ row.phone }}</span></template></el-table-column>
        <el-table-column prop="idCard" label="身份证号" width="180"><template #default="{row}"><span class="font-mono" style="font-size:12px;">{{ row.idCard }}</span></template></el-table-column>
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column label="角色" width="120">
          <template #default="{row}"><span class="status-badge" :class="row.role === 'ADMIN' ? 'status-repairing' : 'status-available'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}"><span class="status-badge" :class="Number(row.status) === 1 ? 'status-available' : 'status-disabled'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{row}">
            <div style="display:flex;gap:6px;">
              <button v-if="row.role === 'USER'" class="btn-sm btn-sm-primary" title="发送消息" @click="openNotify(row)">通知</button>
              <button
                v-if="row.role === 'USER'"
                :class="['btn-sm', 'btn-icon', Number(row.status) === 1 ? 'btn-sm-warning' : 'btn-sm-success']"
                :title="Number(row.status) === 1 ? '禁用用户' : '启用用户'"
                @click="toggleUserStatus(row)"
              ><el-icon><component :is="Number(row.status) === 1 ? 'Lock' : 'Unlock'" /></el-icon></button>
              <button class="btn-sm btn-sm-ghost btn-icon" title="编辑" @click="openEdit(row)"><el-icon><Edit /></el-icon></button>
              <button class="btn-sm btn-sm-danger btn-icon" title="删除" @click="deleteUser(row)"><el-icon><Delete /></el-icon></button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑用户" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="性别"><el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role" style="width:100%"><el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status" style="width:100%"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="dialogVisible = false">取消</button>
        <button class="btn-sm btn-sm-primary" @click="saveUser">保存</button>
      </template>
    </el-dialog>

    <el-dialog v-model="notifyDialogVisible" title="发送自定义消息" width="520px">
      <el-form :model="notifyForm" label-width="90px">
        <el-form-item label="接收用户">
          <el-input :model-value="notifyTargetName" disabled />
        </el-form-item>
        <el-form-item label="消息标题">
          <el-input v-model="notifyForm.title" maxlength="40" show-word-limit placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="消息内容">
          <el-input
            v-model="notifyForm.content"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请输入发送给用户的消息内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="notifyDialogVisible = false">取消</button>
        <button class="btn-sm btn-sm-primary" :disabled="notifySending" @click="sendCustomNotify">
          {{ notifySending ? '发送中...' : '发送消息' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
:deep(.el-input--small) { --el-input-bg-color: var(--surface2); --el-input-border-color: var(--border); --el-input-hover-border-color: var(--accent); --el-input-focus-border-color: var(--accent); }
:deep(.el-select--small) { --el-select-input-bg-color: var(--surface2); --el-select-border-color: var(--border); }
</style>
