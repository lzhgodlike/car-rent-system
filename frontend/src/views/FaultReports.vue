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
const handleForm = reactive({ handleResult: '' })
const currentId = ref(null)

const faultStatusMap = {
  PENDING: '待处理',
  REPAIRING: '维修中',
  RESOLVED: '已修复',
}

const pendingCount = computed(() => records.value.filter((item) => item.faultStatus === 'PENDING').length)
const repairingCount = computed(() => records.value.filter((item) => item.faultStatus === 'REPAIRING').length)
const resolvedCount = computed(() => records.value.filter((item) => item.faultStatus === 'RESOLVED').length)

const formatFaultStatus = (status) => faultStatusMap[status] || status || '-'
const statusTone = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'REPAIRING') return 'danger'
  return 'success'
}

const loadData = async () => {
  const [recordData, carData] = await Promise.all([request.get('/fault-reports'), request.get('/cars')])
  records.value = recordData
  cars.value = carData
}

const carNoMap = computed(() => {
  const map = new Map()
  cars.value.forEach((car) => {
    map.set(car.id, car.carNo)
  })
  return map
})

const formatCarNo = (carId) => carNoMap.value.get(carId) || carId || '-'

const submitFault = async () => {
  await request.post('/fault-reports', form)
  ElMessage.success('故障已上报')
  Object.assign(form, { carId: null, faultContent: '' })
  loadData()
}

const handleFault = async (row) => {
  await request.put(`/fault-reports/${row.id}/handle`, { handleResult: '已安排维修' })
  ElMessage.success('车辆已进入维修状态')
  loadData()
}

const openCompleteRepair = (row) => {
  currentId.value = row.id
  handleForm.handleResult = row.handleResult || ''
  dialogVisible.value = true
}

const completeRepair = async () => {
  await request.put(`/fault-reports/${currentId.value}/handle`, handleForm)
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
        <div class="metric-pill"><span>工单总数</span><strong>{{ records.length }}</strong></div>
        <div class="metric-pill"><span>待处理</span><strong>{{ pendingCount }}</strong></div>
        <div class="metric-pill"><span>维修中</span><strong>{{ repairingCount }}</strong></div>
        <div class="metric-pill"><span>已修复</span><strong>{{ resolvedCount }}</strong></div>
      </div>
    </section>

    <div class="page-card">
      <!-- <div class="page-header compact-page-head">
        <div>
          <h2 class="page-title">故障上报管理</h2>
          <p class="page-desc">用户负责上报问题，管理员处理故障后可将车辆设置为维修中，维修完成后恢复为空闲状态。</p>
        </div>
      </div>

      <div class="summary-grid">
        <div class="summary-card"><span>工单总数</span><strong>{{ records.length }}</strong></div>
        <div class="summary-card"><span>待处理</span><strong>{{ pendingCount }}</strong></div>
        <div class="summary-card"><span>维修中</span><strong>{{ repairingCount }}</strong></div>
        <div class="summary-card"><span>已修复</span><strong>{{ resolvedCount }}</strong></div>
      </div> -->

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
                  v-for="item in cars"
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
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column label="车辆编号" width="140">
              <template #default="scope">
                {{ formatCarNo(scope.row.carId) }}
              </template>
            </el-table-column>
            <el-table-column prop="faultContent" label="故障内容" />
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.faultStatus)">
                  {{ formatFaultStatus(scope.row.faultStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="reportTime" label="上报时间" width="180" />
            <el-table-column prop="handleResult" label="处理结果" />
            <el-table-column v-if="isAdmin" label="操作" width="220">
              <template #default="scope">
                <div class="table-actions">
                  <el-button v-if="scope.row.faultStatus === 'PENDING'" size="small" type="primary" @click="handleFault(scope.row)">处理</el-button>
                  <el-button v-if="scope.row.faultStatus === 'REPAIRING'" size="small" type="success" @click="openCompleteRepair(scope.row)">完成维修</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <el-dialog v-model="dialogVisible" title="完成维修" width="420px">
        <el-form label-width="90px">
          <el-form-item label="处理结果"><el-input v-model="handleForm.handleResult" type="textarea" :rows="4" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="completeRepair">确认完成</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

