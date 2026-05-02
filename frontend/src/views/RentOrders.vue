<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')

const orders = ref([])
const cars = ref([])
const returnOrders = ref([])

const detailVisible = ref(false)
const returnVisible = ref(false)
const currentOrder = ref(null)

const returnForm = reactive({
  rentOrderId: null,
  actualMileage: 0,
  damageDesc: '',
})

const orderStatusMap = {
  RENTED: '租赁中',
  RETURNED: '已归还',
}

const placeholderImage =
  'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1200&q=80'

const activeCount = computed(() => orders.value.filter((item) => item.orderStatus === 'RENTED').length)
const returnedCount = computed(() => orders.value.filter((item) => item.orderStatus === 'RETURNED').length)
const totalIncome = computed(() =>
  orders.value.reduce((sum, item) => sum + Number(item.totalPrice || 0), 0).toFixed(2),
)

const carMap = computed(() => {
  const map = new Map()
  cars.value.forEach((car) => {
    map.set(car.id, car)
  })
  return map
})

const submittedReturnIds = computed(() => new Set(returnOrders.value.map((item) => item.rentOrderId)))

const statusTone = (status) => (status === 'RENTED' ? 'warning' : 'success')
const formatOrderStatus = (status) => orderStatusMap[status] || status || '-'
const formatMoney = (value) => `￥${Number(value || 0).toFixed(2)}`

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

const getCar = (carId) => carMap.value.get(carId) || null
const formatCarNo = (carId) => getCar(carId)?.carNo || '-'
const formatPlateNumber = (carId) => getCar(carId)?.plateNumber || '-'
const formatCarName = (carId) => {
  const car = getCar(carId)
  if (!car) {
    return '-'
  }
  return `${car.brand || ''} ${car.model || ''}`.trim() || '-'
}
const formatCarImage = (carId) => getCar(carId)?.carImage || placeholderImage

const hasReturnRequest = (orderId) => submittedReturnIds.value.has(orderId)

const loadData = async () => {
  const [orderData, carData, returnData] = await Promise.all([
    request.get('/rent-orders'),
    request.get('/cars'),
    request.get('/return-orders'),
  ])
  orders.value = orderData
  cars.value = carData
  returnOrders.value = returnData
}

const openDetail = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
}

const openReturn = (row) => {
  returnForm.rentOrderId = row.id
  returnForm.actualMileage = 0
  returnForm.damageDesc = ''
  returnVisible.value = true
}

const submitReturn = async () => {
  await request.post('/return-orders', returnForm)
  ElMessage.success('还车申请已提交')
  returnVisible.value = false
  detailVisible.value = false
  await loadData()
}

watch(detailVisible, (visible) => {
  document.body.style.overflow = visible ? 'hidden' : ''
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
})

onMounted(loadData)
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Orders</div>
      <h1 class="hero-title">租车订单</h1>
      <p class="hero-desc">
        这里集中查看每一笔租车订单，快速确认车辆名称、车牌号、租期、状态和订单金额。
      </p>
      <div class="metric-strip">
        <div class="metric-pill"><span>订单总数</span><strong>{{ orders.length }}</strong></div>
        <div class="metric-pill"><span>租赁中</span><strong>{{ activeCount }}</strong></div>
        <div class="metric-pill"><span>已归还</span><strong>{{ returnedCount }}</strong></div>
        <div class="metric-pill"><span>累计租金</span><strong>{{ formatMoney(totalIncome) }}</strong></div>
      </div>
    </section>

    <div class="page-card">
      <div class="page-header compact-page-head">
        <div>
          <h2 class="page-title">租车信息管理</h2>
          <p class="page-desc">点击任意订单可查看完整详情，用户也可以直接从详情中发起还车申请。</p>
        </div>
      </div>

      <div class="summary-grid">
        <div class="summary-card"><span>订单总数</span><strong>{{ orders.length }}</strong></div>
        <div class="summary-card"><span>当前租赁中</span><strong>{{ activeCount }}</strong></div>
        <div class="summary-card"><span>已完成归还</span><strong>{{ returnedCount }}</strong></div>
        <div class="summary-card"><span>累计总租金</span><strong>{{ formatMoney(totalIncome) }}</strong></div>
      </div>

      <div class="section-card">
        <div class="section-head">
          <div>
            <h3>订单明细</h3>
            <p>点击任意行可查看该订单全部信息。</p>
          </div>
        </div>

        <div class="table-shell">
          <el-table :data="orders" stripe row-class-name="clickable-row" @row-click="openDetail">
            <el-table-column v-if="isAdmin" prop="orderNo" label="订单编号" min-width="170" />
            <el-table-column v-if="isAdmin" prop="userId" label="用户ID" width="90" />
            <el-table-column :label="isAdmin ? '车辆编号' : '车型'" min-width="220">
              <template #default="scope">
                {{ isAdmin ? formatCarNo(scope.row.carId) : formatCarName(scope.row.carId) }}
              </template>
            </el-table-column>
            <el-table-column v-if="!isAdmin" label="车牌号" min-width="150">
              <template #default="scope">
                {{ formatPlateNumber(scope.row.carId) }}
              </template>
            </el-table-column>
            <el-table-column prop="rentDate" label="租车日期" min-width="120" />
            <el-table-column prop="expectedReturnDate" label="预计还车日期" min-width="130" />
            <el-table-column v-if="!isAdmin" label="创建时间" min-width="170">
              <template #default="scope">
                {{ formatDateTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="rentDays" label="天数" width="90" />
            <el-table-column label="总租金" min-width="120">
              <template #default="scope">
                {{ formatMoney(scope.row.totalPrice) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.orderStatus)">
                  {{ formatOrderStatus(scope.row.orderStatus) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div v-if="isAdmin" class="soft-note">
        管理员仍可在这里查看全部订单明细；订单归还由用户发起申请后，在还车记录页面完成确认。
      </div>
    </div>

    <div v-if="detailVisible && currentOrder" class="detail-overlay" @click.self="closeDetail">
      <section class="detail-modal">
        <header class="detail-modal-header">
          <div>
            <h2>订单详情</h2>
            <p>{{ formatCarName(currentOrder.carId) }}</p>
          </div>
          <button class="detail-close" type="button" @click="closeDetail">关闭</button>
        </header>

        <div class="order-detail-shell">
          <div class="order-detail-cover">
            <img :src="formatCarImage(currentOrder.carId)" :alt="formatCarName(currentOrder.carId)" />
          </div>

          <div class="order-detail-content">
            <div class="order-detail-grid">
              <div class="detail-item">
                <span>订单编号</span>
                <strong>{{ currentOrder.orderNo || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>用户ID</span>
                <strong>{{ currentOrder.userId || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>车辆编号</span>
                <strong>{{ formatCarNo(currentOrder.carId) }}</strong>
              </div>
              <div class="detail-item">
                <span>车辆名称</span>
                <strong>{{ formatCarName(currentOrder.carId) }}</strong>
              </div>
              <div class="detail-item">
                <span>车牌号</span>
                <strong>{{ formatPlateNumber(currentOrder.carId) }}</strong>
              </div>
              <div class="detail-item">
                <span>订单状态</span>
                <strong>{{ formatOrderStatus(currentOrder.orderStatus) }}</strong>
              </div>
              <div class="detail-item">
                <span>租车日期</span>
                <strong>{{ currentOrder.rentDate || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>预计还车</span>
                <strong>{{ currentOrder.expectedReturnDate || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>实际还车</span>
                <strong>{{ currentOrder.actualReturnDate || '-' }}</strong>
              </div>
              <div class="detail-item">
                <span>租赁天数</span>
                <strong>{{ currentOrder.rentDays || 0 }} 天</strong>
              </div>
              <div class="detail-item">
                <span>日租金</span>
                <strong>{{ formatMoney(currentOrder.unitPrice) }}</strong>
              </div>
              <div class="detail-item">
                <span>总租金</span>
                <strong>{{ formatMoney(currentOrder.totalPrice) }}</strong>
              </div>
              <div class="detail-item">
                <span>订单创建时间</span>
                <strong>{{ formatDateTime(currentOrder.createTime) }}</strong>
              </div>
              <div class="detail-item detail-item-full">
                <span>备注</span>
                <strong>{{ currentOrder.remark || '无' }}</strong>
              </div>
            </div>
          </div>
        </div>

        <footer class="detail-modal-footer">
          <el-button
            v-if="!isAdmin && currentOrder.orderStatus === 'RENTED' && !hasReturnRequest(currentOrder.id)"
            type="primary"
            @click="openReturn(currentOrder)"
          >
            申请还车
          </el-button>
          <el-button
            v-if="!isAdmin && currentOrder.orderStatus === 'RENTED' && hasReturnRequest(currentOrder.id)"
            disabled
          >
            已提交还车申请
          </el-button>
        </footer>
      </section>
    </div>

    <el-dialog v-model="returnVisible" title="提交还车申请" width="520px">
      <el-form label-width="100px">
        <el-form-item label="当前公里数">
          <el-input-number v-model="returnForm.actualMileage" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="损坏说明">
          <el-input v-model="returnForm.damageDesc" type="textarea" :rows="4" placeholder="没有可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-detail-shell {
  display: grid;
  grid-template-columns: minmax(360px, 1.2fr) minmax(0, 0.9fr);
  gap: 24px;
  align-items: stretch;
  flex: 1;
  min-height: 0;
  padding: 12px 28px 0;
}

.order-detail-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 22px;
  min-height: 0;
  padding: 18px;
  background: rgba(255, 248, 240, 0.92);
}

.order-detail-cover img {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  display: block;
  border-radius: inherit;
}

.order-detail-content {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.order-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 10px 8px 0;
}

.detail-item {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(191, 108, 47, 0.06);
}

.detail-item span {
  display: block;
  font-size: 12px;
  color: var(--subtext);
}

.detail-item strong {
  display: block;
  margin-top: 6px;
  line-height: 1.6;
  word-break: break-word;
}

.detail-item-full {
  grid-column: 1 / -1;
}

:deep(.clickable-row) {
  cursor: pointer;
}

.detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 2100;
  display: grid;
  place-items: center;
  padding: 0;
  background: rgba(17, 13, 10, 0.52);
  backdrop-filter: blur(6px);
}

.detail-modal {
  width: 85%;
  max-width: 1320px;
  height: min(88vh, 900px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.98), rgba(248, 240, 230, 0.98));
  box-shadow: 0 28px 80px rgba(29, 20, 13, 0.28);
}

.detail-modal-header,
.detail-modal-footer {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px;
}

.detail-modal-header {
  border-bottom: 1px solid rgba(191, 108, 47, 0.12);
}

.detail-modal-footer {
  justify-content: flex-end;
  border-top: 1px solid rgba(191, 108, 47, 0.12);
}

.detail-modal-header h2 {
  margin: 0;
  font-size: 24px;
}

.detail-modal-header p {
  margin: 6px 0 0;
  color: var(--subtext);
}

.detail-close {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(191, 108, 47, 0.1);
  color: var(--brand-deep);
  cursor: pointer;
}

@media (max-width: 720px) {
  .detail-modal {
    width: 94%;
    height: 94vh;
  }

  .order-detail-shell {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 12px 20px 0;
  }

  .order-detail-cover {
    height: 220px;
    padding: 14px;
  }

  .order-detail-grid {
    grid-template-columns: 1fr;
    padding-right: 0;
  }
}
</style>
