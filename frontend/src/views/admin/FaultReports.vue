<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const records = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const handleDialog = ref(false)
const pendingRow = ref(null)
const handleResult = ref('已安排维修')
const completeDialog = ref(false)
const completeRow = ref(null)
const completeResult = ref('')
const filterStatus = ref('')

const statusMap = { PENDING: '待处理', REPAIRING: '维修中', RESOLVED: '已修复', REJECTED: '已拒绝' }
const statusClass = { PENDING: 'status-pending', REPAIRING: 'status-repairing', RESOLVED: 'status-resolved', REJECTED: 'status-rejected' }
const carName = (row) => row.carInfo ? `${row.carInfo.brand} ${row.carInfo.model}` : '-'

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/fault-reports', { params: { pageNum: currentPage.value, pageSize: pageSize.value, status: filterStatus.value || undefined } })
    records.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
const onFilterChange = () => { currentPage.value = 1; loadData() }
onMounted(loadData)

const openHandle = (row) => { pendingRow.value = row; handleResult.value = '已安排维修'; handleDialog.value = true }
const doHandle = async () => {
  await request.put(`/fault-reports/${pendingRow.value.id}/handle`, { handleResult: handleResult.value })
  ElMessage.success('已处理'); handleDialog.value = false; loadData()
}
const openComplete = (row) => { completeRow.value = row; completeResult.value = ''; completeDialog.value = true }
const doComplete = async () => {
  await request.put(`/fault-reports/${completeRow.value.id}/complete-repair`, { handleResult: completeResult.value || '维修完成' })
  ElMessage.success('维修完成'); completeDialog.value = false; loadData()
}
const rejectFault = async (row) => {
  await ElMessageBox.confirm('确定拒绝此故障报告？', '确认', { type: 'warning' })
  await request.put(`/fault-reports/${row.id}/reject`, { handleResult: '故障报告不成立' })
  ElMessage.success('已拒绝'); loadData()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="summary-strip">
        <span class="sum-item">全部 <strong>{{ total }}</strong></span>
        <span class="sum-item">待处理 <strong>{{ summary.pending ?? 0 }}</strong></span>
        <span class="sum-item">维修中 <strong>{{ summary.repairing ?? 0 }}</strong></span>
        <span class="sum-item">已修复 <strong>{{ summary.resolved ?? 0 }}</strong></span>
      </div>
      <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="small" style="width:120px;" @change="onFilterChange">
        <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
      </el-select>
    </div>
    <div class="card">
      <el-table :data="records" v-loading="loading">
        <el-table-column label="车辆" min-width="160">
          <template #default="{row}"><div style="font-weight:500;">{{ carName(row) }}</div><div style="font-size:11px;color:var(--muted);">{{ row.carInfo?.carNo }}</div></template>
        </el-table-column>
        <el-table-column prop="faultContent" label="故障内容" min-width="160" />
        <el-table-column label="工单状态" width="100">
          <template #default="{row}"><span class="status-badge" :class="statusClass[row.faultStatus]">{{ statusMap[row.faultStatus] }}</span></template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="170"><template #default="{row}"><span class="font-mono" style="font-size:12px;">{{ row.reportTime?.replace('T',' ') }}</span></template></el-table-column>
        <el-table-column prop="handleResult" label="处理结果" min-width="120"><template #default="{row}">{{ row.handleResult || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <button v-if="row.faultStatus === 'PENDING'" class="btn-sm btn-sm-primary" @click="openHandle(row)">处理</button>
            <button v-if="row.faultStatus === 'PENDING'" class="btn-sm btn-sm-ghost" @click="rejectFault(row)">拒绝</button>
            <button v-if="row.faultStatus === 'REPAIRING'" class="btn-sm btn-sm-success" @click="openComplete(row)">完成维修</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <el-dialog v-model="handleDialog" title="处理故障" width="440px">
      <div v-if="pendingRow" class="confirm-info">
        <div class="info-row"><span>车辆</span><strong>{{ carName(pendingRow) }} ({{ pendingRow.carInfo?.carNo }})</strong></div>
        <div class="info-row"><span>故障内容</span><strong>{{ pendingRow.faultContent }}</strong></div>
      </div>
      <el-form label-width="80px" style="margin-top:16px;">
        <el-form-item label="处理说明"><el-input v-model="handleResult" /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="handleDialog = false">取消</button>
        <button class="btn-sm btn-sm-primary" @click="doHandle">确认处理</button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialog" title="完成维修" width="440px">
      <div v-if="completeRow" class="confirm-info">
        <div class="info-row"><span>车辆</span><strong>{{ carName(completeRow) }} ({{ completeRow.carInfo?.carNo }})</strong></div>
        <div class="info-row"><span>故障内容</span><strong>{{ completeRow.faultContent }}</strong></div>
      </div>
      <el-form label-width="80px" style="margin-top:16px;">
        <el-form-item label="维修详情"><el-input v-model="completeResult" type="textarea" :rows="3" placeholder="请填写维修详情" /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="completeDialog = false">取消</button>
        <button class="btn-sm btn-sm-success" @click="doComplete">确认完成</button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; }
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
.confirm-info { padding: 16px; border-radius: 8px; background: var(--surface2); display: grid; gap: 10px; }
.info-row { display: flex; justify-content: space-between; gap: 12px; }
.info-row span { font-size: 13px; color: var(--muted); white-space: nowrap; }
.info-row strong { font-size: 13px; text-align: right; word-break: break-word; }
</style>
