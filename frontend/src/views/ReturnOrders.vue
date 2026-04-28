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
  const [recordData, rentData, carData] = await Promise.all([
    request.get('/return-orders'),
    request.get('/rent-orders'),
    request.get('/cars'),
  ])
  records.value = recordData
  rentOrders.value = rentData
  cars.value = carData
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

    <div class="page-card">
      <!-- <div class="page-header compact-page-head">
        <div>
          <h2 class="page-title">还车信息管理</h2>
          <p class="page-desc">用户提交还车申请，管理员确认还车并录入附加费用。</p>
        </div>
      </div>

      <div class="summary-grid">
        <div class="summary-card"><span>记录总数</span><strong>{{ records.length }}</strong></div>
        <div class="summary-card"><span>待确认</span><strong>{{ pendingCount }}</strong></div>
        <div class="summary-card"><span>已确认</span><strong>{{ confirmedCount }}</strong></div>
        <div class="summary-card"><span>附加费用合计</span><strong>￥{{ extraFeeTotal }}</strong></div>
      </div> -->

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

      <!-- <div class="section-card"> -->
        <!-- <div class="section-head">
          <div>
            <h3>还车记录列表</h3>
            <p>支持核对租车订单、车辆编号、还车公里数和确认状态。</p>
          </div>
        </div> -->

        <div class="table-shell">
          <el-table :data="records" stripe>
            <el-table-column prop="rentOrderId" label="租车订单ID" width="120" />
            <el-table-column label="车辆编号" width="140">
              <template #default="scope">
                {{ formatCarNoByRent(scope.row.rentOrderId) }}
              </template>
            </el-table-column>
            <el-table-column prop="actualReturnTime" label="申请时间" width="180" />
            <el-table-column prop="actualMileage" label="还车公里数" width="120" />
            <el-table-column prop="damageDesc" label="损坏说明" />
            <el-table-column label="附加费用" width="120">
              <template #default="scope">
                ￥{{ scope.row.extraFee || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.status)">
                  {{ formatReturnStatus(scope.row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column v-if="isAdmin" label="操作" width="130">
              <template #default="scope">
                <el-button v-if="scope.row.status === 'PENDING'" size="small" type="primary" @click="openConfirm(scope.row)">确认还车</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      <!-- </div> -->

      <el-dialog v-model="confirmDialog" title="确认还车" width="420px">
        <el-form label-width="90px">
          <el-form-item label="附加费用"><el-input-number v-model="confirmForm.extraFee" :min="0" style="width: 100%" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="confirmDialog = false">取消</el-button>
          <el-button type="primary" @click="doConfirm">确认</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

