<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const records = ref([])
const rentOrders = ref([])
const cars = ref([])
const createForm = reactive({ rentOrderId: null, actualMileage: 0, damageDesc: '' })
const confirmDialog = ref(false)
const currentId = ref(null)
const confirmForm = reactive({ extraFee: 0 })
const loading = ref(false)

const returnStatusMap = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
}

const pendingCount = computed(() => records.value.filter((item) => item.status === 'PENDING').length)
const confirmedCount = computed(() => records.value.filter((item) => item.status === 'CONFIRMED').length)
const extraFeeTotal = computed(() =>
  records.value.reduce((sum, item) => sum + Number(item.extraFee || 0), 0).toFixed(2),
)

const formatReturnStatus = (status) => returnStatusMap[status] || status || '-'
const statusTone = (status) => (status === 'PENDING' ? 'warning' : 'success')

const loadData = async () => {
  loading.value = true
  try {
    const [recordData, rentData, carData] = await Promise.all([
      request.get('/return-orders'),
      request.get('/rent-orders'),
      request.get('/cars'),
    ])
    records.value = recordData
    rentOrders.value = rentData
    cars.value = carData
  } finally {
    loading.value = false
  }
}

const rentOrderMap = computed(() => {
  const map = new Map()
  rentOrders.value.forEach((order) => {
    map.set(order.id, order)
  })
  return map
})

const carNoMap = computed(() => {
  const map = new Map()
  cars.value.forEach((car) => {
    map.set(car.id, car.carNo)
  })
  return map
})

const formatCarNo = (carId) => carNoMap.value.get(carId) || carId || '-'

const formatCarNoByRent = (rentOrderId) => {
  const order = rentOrderMap.value.get(rentOrderId)
  if (!order) {
    return rentOrderId || '-'
  }
  return formatCarNo(order.carId)
}

const formatCarNameByRent = (rentOrderId) => {
  const order = rentOrderMap.value.get(rentOrderId)
  if (!order) return '-'
  const car = cars.value.find((c) => c.id === order.carId)
  if (!car) return '-'
  return `${car.brand || ''} ${car.model || ''}`.trim() || '-'
}

const formatPlateByRent = (rentOrderId) => {
  const order = rentOrderMap.value.get(rentOrderId)
  if (!order) return '-'
  const car = cars.value.find((c) => c.id === order.carId)
  return car?.plateNumber || '-'
}

const formatRentPeriod = (rentOrderId) => {
  const order = rentOrderMap.value.get(rentOrderId)
  if (!order) return '-'
  return `${order.rentDate || ''} 至 ${order.expectedReturnDate || ''}`
}

const formatRentDays = (rentOrderId) => {
  const order = rentOrderMap.value.get(rentOrderId)
  return order?.rentDays || '-'
}

const currentReturnOrder = computed(() => {
  if (!currentId.value) return null
  return records.value.find((r) => r.id === currentId.value) || null
})

const currentReturnRent = computed(() => {
  if (!currentReturnOrder.value) return null
  return rentOrderMap.value.get(currentReturnOrder.value.rentOrderId) || null
})

const currentReturnCar = computed(() => {
  if (!currentReturnRent.value) return null
  return cars.value.find((c) => c.id === currentReturnRent.value.carId) || null
})

const availableRentOrders = computed(() => {
  const appliedIds = new Set(records.value.map((item) => item.rentOrderId))
  return rentOrders.value.filter((item) => item.orderStatus === 'RENTED' && !appliedIds.has(item.id))
})

const submitReturn = async () => {
  await request.post('/return-orders', createForm)
  ElMessage.success('还车申请已提交')
  Object.assign(createForm, { rentOrderId: null, actualMileage: 0, damageDesc: '' })
  loadData()
}

const openConfirm = (row) => {
  currentId.value = row.id
  confirmForm.extraFee = row.extraFee || 0
  confirmDialog.value = true
}

const doConfirm = async () => {
  await request.put(`/return-orders/${currentId.value}/confirm`, confirmForm)
  ElMessage.success('管理员已确认还车')
  confirmDialog.value = false
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Returns</div>
      <h1 class="hero-title">还车记录</h1>
      <p class="hero-desc">这里查看还车申请、公里数和附加费用，管理员可以继续确认还车并完成订单闭环。</p>
      <div class="metric-strip">
        <div class="metric-pill"><span>记录总数</span><strong>{{ records.length }}</strong></div>
        <div class="metric-pill"><span>待确认</span><strong>{{ pendingCount }}</strong></div>
        <div class="metric-pill"><span>已确认</span><strong>{{ confirmedCount }}</strong></div>
        <div class="metric-pill"><span>附加费用</span><strong>￥{{ extraFeeTotal }}</strong></div>
      </div>
    </section>

    <div class="page-card" v-loading="loading">
      <div v-if="!isAdmin" class="section-card">
        <div class="section-head">
          <div>
            <h3>提交还车申请</h3>
            <p>选择正在租用的订单，填写当前公里数后即可发起还车。</p>
          </div>
        </div>

        <div class="toolbar toolbar-card">
          <el-form inline>
            <el-form-item label="我的租车订单">
              <el-select v-model="createForm.rentOrderId" placeholder="请选择租车订单" style="width: 320px">
                <el-option
                  v-for="item in availableRentOrders"
                  :key="item.id"
                  :label="`${item.orderNo} / 车辆编号:${formatCarNo(item.carId)} / ${item.rentDate} 至 ${item.expectedReturnDate}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="当前公里数"><el-input-number v-model="createForm.actualMileage" :min="0" /></el-form-item>
            <el-form-item label="损坏说明"><el-input v-model="createForm.damageDesc" style="width: 220px" /></el-form-item>
            <el-form-item><el-button type="primary" :disabled="!createForm.rentOrderId" @click="submitReturn">提交还车申请</el-button></el-form-item>
          </el-form>
        </div>

        <div v-if="availableRentOrders.length === 0" class="soft-note">当前没有可申请还车的租车订单。</div>
      </div>

        <div class="table-shell">
          <el-table :data="records" stripe>
            <el-table-column label="车辆" min-width="180">
              <template #default="scope">
                <div>{{ formatCarNameByRent(scope.row.rentOrderId) }}</div>
                <div style="font-size:12px;color:var(--subtext)">{{ formatPlateByRent(scope.row.rentOrderId) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="原租期" min-width="200">
              <template #default="scope">
                <div>{{ formatRentPeriod(scope.row.rentOrderId) }}</div>
                <div style="font-size:12px;color:var(--subtext)">共 {{ formatRentDays(scope.row.rentOrderId) }} 天</div>
              </template>
            </el-table-column>
            <el-table-column prop="actualReturnTime" label="还车时间" width="170" />
            <el-table-column prop="actualMileage" label="还车公里数" width="110" />
            <el-table-column prop="damageDesc" label="损坏说明" min-width="140">
              <template #default="scope">
                {{ scope.row.damageDesc || '无' }}
              </template>
            </el-table-column>
            <el-table-column label="附加费用" width="110">
              <template #default="scope">
                ￥{{ scope.row.extraFee || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.status)">
                  {{ formatReturnStatus(scope.row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column v-if="isAdmin" label="操作" width="120">
              <template #default="scope">
                <el-button v-if="scope.row.status === 'PENDING'" size="small" type="primary" @click="openConfirm(scope.row)">确认还车</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

      <el-dialog v-model="confirmDialog" title="确认还车" width="520px">
        <div v-if="currentReturnOrder" class="confirm-info">
          <div class="confirm-info-row">
            <span>车辆</span>
            <strong>{{ currentReturnCar ? `${currentReturnCar.brand} ${currentReturnCar.model} (${currentReturnCar.plateNumber})` : '-' }}</strong>
          </div>
          <div class="confirm-info-row">
            <span>还车公里数</span>
            <strong>{{ currentReturnOrder.actualMileage }} km</strong>
          </div>
          <div class="confirm-info-row">
            <span>损坏说明</span>
            <strong>{{ currentReturnOrder.damageDesc || '无' }}</strong>
          </div>
          <div class="confirm-info-row" v-if="currentReturnRent">
            <span>原订单租金</span>
            <strong>￥{{ currentReturnRent.totalPrice || 0 }}</strong>
          </div>
        </div>
        <el-form label-width="90px" style="margin-top:16px">
          <el-form-item label="附加费用"><el-input-number v-model="confirmForm.extraFee" :min="0" style="width: 100%" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="confirmDialog = false">取消</el-button>
          <el-button type="primary" @click="doConfirm">确认还车</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.confirm-info {
  padding: 16px;
  border-radius: 14px;
  background: rgba(191, 108, 47, 0.06);
  display: grid;
  gap: 12px;
}

.confirm-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.confirm-info-row span {
  font-size: 13px;
  color: var(--subtext);
}

.confirm-info-row strong {
  font-size: 14px;
}
</style>

