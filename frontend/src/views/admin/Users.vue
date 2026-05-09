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

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/users', { params: { pageNum: currentPage.value, pageSize: pageSize.value } })
    users.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
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
  await ElMessageBox.confirm(`确定删除用户「${row.realName || row.username}」？`, '删除确认', { type: 'warning' })
  await request.delete(`/users/${row.id}`); ElMessage.success('已删除'); loadData()
}
</script>

<template>
  <div>
    <div class="summary-strip">
      <span class="sum-item">用户总数 <strong>{{ total }}</strong></span>
      <span class="sum-item">管理员 <strong>{{ summary.admin ?? 0 }}</strong></span>
      <span class="sum-item">普通用户 <strong>{{ summary.normal ?? 0 }}</strong></span>
    </div>
    <div class="card" style="margin-top:16px;">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130"><template #default="{row}"><span class="font-mono">{{ row.phone }}</span></template></el-table-column>
        <el-table-column prop="idCard" label="身份证号" width="180"><template #default="{row}"><span class="font-mono" style="font-size:12px;">{{ row.idCard }}</span></template></el-table-column>
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column label="角色" width="90">
          <template #default="{row}"><span class="status-badge" :class="row.role === 'ADMIN' ? 'status-repairing' : 'status-available'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{row}"><span class="status-badge" :class="Number(row.status) === 1 ? 'status-available' : 'status-disabled'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="deleteUser(row)">删除</el-button>
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
</style>
