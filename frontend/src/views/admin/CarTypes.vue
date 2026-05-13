<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const types = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentId = ref(null)
const form = ref({ typeName: '', description: '' })

const loadData = async () => {
  loading.value = true
  try {
    types.value = await request.get('/car-types')
  } finally { loading.value = false }
}
onMounted(loadData)

const openAdd = () => { currentId.value = null; form.value = { typeName: '', description: '' }; dialogVisible.value = true }
const openEdit = (row) => { currentId.value = row.id; form.value = { typeName: row.typeName, description: row.description || '' }; dialogVisible.value = true }

const save = async () => {
  if (!form.value.typeName) { ElMessage.warning('请输入类型名称'); return }
  if (currentId.value) {
    await request.put(`/car-types/${currentId.value}`, form.value)
    ElMessage.success('修改成功')
  } else {
    await request.post('/car-types', form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除车型「${row.typeName}」吗？`, '删除确认', { type: 'warning' })
  await request.delete(`/car-types/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="title">车型列表</span>
      <button class="btn-sm btn-sm-primary" @click="openAdd"><el-icon><Plus /></el-icon> 新增车型</button>
    </div>
    <div class="card">
      <el-table :data="types" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="typeName" label="类型名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="280">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <button class="btn-sm btn-sm-ghost" @click="openEdit(row)">编辑</button>
            <button class="btn-sm btn-sm-danger" @click="remove(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="currentId ? '编辑车型' : '新增车型'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型名称" required>
          <el-input v-model="form.typeName" placeholder="例如：轿车、SUV、新能源" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="dialogVisible = false">取消</button>
        <button class="btn-sm btn-sm-primary" @click="save">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.title { font-size: 16px; font-weight: 600; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
</style>
