<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const records = ref([])
const cars = ref([])
const form = reactive({ carId: null, faultContent: '' })
const dialogVisible = ref(false)
const currentId = ref(null)
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const handlePageChange = (page) => { currentPage.value = page; loadData() }
const handleSizeChange = (size) => { pageSize.value = size; currentPage.value = 1; loadData() }

const handleConfirmVisible = ref(false)
const pendingRow = ref(null)

const faultStatusMap = {
  PENDING: '待处理',
  REPAIRING: '维修中',
  RESOLVED: '已修复',
}

const formatFaultStatus = (status) => faultStatusMap[status] || status || '-'
const statusTone = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'REPAIRING') return 'danger'
  return 'success'
}

const myRentedCarIds = ref(new Set())

const loadData = async () => {
  loading.value = true
  try {
    const requests = [
      request.get('/fault-reports', { params: { pageNum: currentPage.value, pageSize: pageSize.value } }),
      request.get('/cars', { params: { pageNum: 1, pageSize: 9999 } }),
    ]
    if (!isAdmin.value) {
      requests.push(request.get('/rent-orders', { params: { pageNum: 1, pageSize: 9999 } }))
    }
    const [recordPage, carPage, orderPage] = await Promise.all(requests)
    records.value = recordPage.records
    total.value = recordPage.total
    summary.value = recordPage.summary || {}
    cars.value = carPage.records
    if (!isAdmin.value && orderPage) {
      myRentedCarIds.value = new Set(orderPage.records.map(o => o.carId))
    }
  } finally {
    loading.value = false
  }
}

const reportableCars = computed(() => {
  if (isAdmin.value) return cars.value
  return cars.value.filter(c => myRentedCarIds.value.has(c.id))
})

const carStatusMap = { AVAILABLE: '空闲可租', RENTED: '已出租', MAINTENANCE: '检修中' }
const carStatusTone = { AVAILABLE: 'success', RENTED: 'warning', MAINTENANCE: 'danger' }

const carName = (row) => {
  const c = row.carInfo
  if (!c) return '-'
  return `${c.brand || ''} ${c.model || ''}`.trim() || '-'
}
const carNo = (row) => row.carInfo?.carNo || '-'
const carStatus = (row) => carStatusMap[row.carInfo?.status] || '-'
const carStatusToneFn = (row) => carStatusTone[row.carInfo?.status] || 'neutral'

const submitFault = async () => {
  await request.post('/fault-reports', form)
  ElMessage.success('故障已上报')
  Object.assign(form, { carId: null, faultContent: '' })
  loadData()
}

const openHandleConfirm = (row) => {
  pendingRow.value = row
  handleConfirmVisible.value = true
}

const doHandleFault = async () => {
  await request.put(`/fault-reports/${pendingRow.value.id}/handle`, { handleResult: '已安排维修' })
  ElMessage.success('车辆已进入维修状态')
  handleConfirmVisible.value = false
  pendingRow.value = null
  loadData()
}

const openCompleteRepair = (row) => {
  currentId.value = row.id
  dialogVisible.value = true
}

const completeRepair = async () => {
  await request.put(`/fault-reports/${currentId.value}/complete-repair`)
  ElMessage.success('维修完成，车辆已恢复为空闲状态')
  dialogVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Service Desk</div>
      <h1 class="hero-title">车况工单</h1>
      <p class="hero-desc">把故障上报、维修处理中和维修完成的状态放在一个工作台里，方便快速安排车辆维修。</p>
      <div class="metric-strip">
        <div class="metric-pill"><span>工单总数</span><strong>{{ total }}</strong></div>
        <div class="metric-pill"><span>待处理</span><strong>{{ summary.pending ?? 0 }}</strong></div>
        <div class="metric-pill"><span>维修中</span><strong>{{ summary.repairing ?? 0 }}</strong></div>
        <div class="metric-pill"><span>已修复</span><strong>{{ summary.resolved ?? 0 }}</strong></div>
      </div>
    </section>

    <div class="page-card" v-loading="loading">
      <div class="section-card">
        <div class="section-head">
          <div>
            <h3>提交故障上报</h3>
            <p>选择车辆并填写问题描述，系统会将车辆工单纳入维修处理流程。</p>
          </div>
        </div>
        <div class="toolbar toolbar-card">
          <el-form inline>
            <el-form-item label="车辆编号">
              <el-select v-model="form.carId" placeholder="请选择车辆" style="width: 260px">
                <el-option
                  v-for="item in reportableCars"
                  :key="item.id"
                  :label="`${item.carNo} / ${item.brand} ${item.model}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="故障内容"><el-input v-model="form.faultContent" style="width: 320px" /></el-form-item>
            <el-form-item><el-button type="primary" :disabled="!form.carId" @click="submitFault">提交上报</el-button></el-form-item>
          </el-form>
        </div>
      </div>

      <div class="section-card">
        <div class="section-head">
          <div>
            <h3>工单列表</h3>
            <p>支持查看车辆编号、问题内容、上报时间和维修处理结果。</p>
          </div>
        </div>

        <div class="table-shell">
          <el-table :data="records" stripe>
            <el-table-column label="车辆" min-width="180">
              <template #default="scope">
                <div>{{ carName(scope.row) }}</div>
                <div style="font-size:12px;color:var(--subtext)">{{ carNo(scope.row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="车辆状态" width="110">
              <template #default="scope">
                <span class="status-badge" :class="carStatusToneFn(scope.row)">
                  {{ carStatus(scope.row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="faultContent" label="故障内容" min-width="160" />
            <el-table-column label="工单状态" width="110">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.faultStatus)">
                  {{ formatFaultStatus(scope.row.faultStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="reportTime" label="上报时间" width="170" />
            <el-table-column prop="handleResult" label="处理结果" min-width="130">
              <template #default="scope">
                {{ scope.row.handleResult || '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="isAdmin" label="操作" width="200">
              <template #default="scope">
                <div class="table-actions">
                  <el-button v-if="scope.row.faultStatus === 'PENDING'" size="small" type="primary" @click="openHandleConfirm(scope.row)">处理</el-button>
                  <el-button v-if="scope.row.faultStatus === 'REPAIRING'" size="small" type="success" @click="openCompleteRepair(scope.row)">完成维修</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <el-dialog v-model="dialogVisible" title="完成维修" width="420px">
        <p style="color:var(--subtext);font-size:14px;">确认后车辆将恢复为"空闲可租"状态。</p>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="completeRepair">确认完成</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="handleConfirmVisible" title="确认处理" width="440px">
        <div v-if="pendingRow" class="handle-confirm-info">
          <div class="handle-confirm-row">
            <span>车辆</span>
            <strong>{{ carName(pendingRow) }} ({{ carNo(pendingRow) }})</strong>
          </div>
          <div class="handle-confirm-row">
            <span>故障内容</span>
            <strong>{{ pendingRow.faultContent }}</strong>
          </div>
        </div>
        <p style="margin:16px 0 0;color:var(--subtext);font-size:13px">确认后车辆将变为"检修中"状态，无法出租。</p>
        <template #footer>
          <el-button @click="handleConfirmVisible = false">取消</el-button>
          <el-button type="primary" @click="doHandleFault">确认处理</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.handle-confirm-info {
  padding: 16px;
  border-radius: var(--radius-sm);
  background: var(--gray-50);
  display: grid;
  gap: 12px;
}

.handle-confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.handle-confirm-row span {
  font-size: 13px;
  color: var(--subtext);
  white-space: nowrap;
}

.handle-confirm-row strong {
  font-size: 14px;
  text-align: right;
  word-break: break-word;
}
</style>

