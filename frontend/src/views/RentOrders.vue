<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')

const orders = ref([])
const loading = ref(false)

const detailVisible = ref(false)
const returnVisible = ref(false)
const currentOrder = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const handlePageChange = (page) => { currentPage.value = page; loadData() }
const handleSizeChange = (size) => { pageSize.value = size; currentPage.value = 1; loadData() }

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

const statusTone = (status) => (status === 'RENTED' ? 'warning' : 'success')
const formatOrderStatus = (status) => orderStatusMap[status] || status || '-'
const formatMoney = (value) => `￥${Number(value || 0).toFixed(2)}`
const totalAmount = (row) => Number(row.totalPrice || 0) + Number(row.extraFee || 0)

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

const carNo = (row) => row.carInfo?.carNo || '-'
const plateNumber = (row) => row.carInfo?.plateNumber || '-'
const carName = (row) => {
  const c = row.carInfo
  if (!c) return '-'
  return `${c.brand || ''} ${c.model || ''}`.trim() || '-'
}
const carImage = (row) => row.carInfo?.carImage || placeholderImage

const loadData = async () => {
  loading.value = true
  try {
    const orderPage = await request.get('/rent-orders', { params: { pageNum: currentPage.value, pageSize: pageSize.value } })
    orders.value = orderPage.records
    total.value = orderPage.total
    summary.value = orderPage.summary || {}
  } finally {
    loading.value = false
  }
}

const exportCSV = async () => {
  const allData = await request.get('/rent-orders', { params: { pageNum: 1, pageSize: total.value || 9999 } })
  const header = ['订单编号', '用户ID', '车辆名称', '车牌号', '租车日期', '预计还车', '天数', '总租金', '状态']
  const rows = allData.records.map((o) => [
    o.orderNo || '',
    o.userId || '',
    carName(o),
    plateNumber(o),
    o.rentDate || '',
    o.expectedReturnDate || '',
    o.rentDays || 0,
    totalAmount(o),
    formatOrderStatus(o.orderStatus),
  ].map((v) => {
    const s = String(v ?? '')
    return s.includes(',') || s.includes('"') ? `"${s.replace(/"/g, '""')}"` : s
  }))
  const csv = [header, ...rows].map((r) => r.join(',')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `租车订单_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}

const openDetail = (row) => {
  currentOrder.value = row
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
}

const currentReturnCar = computed(() => {
  if (!currentOrder.value) return null
  return currentOrder.value.carInfo || null
})

const openReturn = (row) => {
  returnForm.rentOrderId = row.id
  returnForm.actualMileage = currentReturnCar.value?.mileage || 0
  returnForm.damageDesc = ''
  returnVisible.value = true
}

const submitReturn = async () => {
  if (returnForm.actualMileage <= (currentReturnCar.value?.mileage || 0)) {
    ElMessage.warning('还车公里数必须大于初始公里数')
    return
  }
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
        <div class="metric-pill"><span>订单总数</span><strong>{{ total }}</strong></div>
        <div class="metric-pill"><span>租赁中</span><strong>{{ summary.active ?? 0 }}</strong></div>
        <div class="metric-pill"><span>已归还</span><strong>{{ summary.returned ?? 0 }}</strong></div>
      </div>
    </section>

    <div class="page-card" v-loading="loading">
      <div class="section-head">
        <div>
          <h3>订单明细</h3>
          <p>点击任意行可查看该订单全部信息。</p>
        </div>
        <el-button v-if="isAdmin" type="primary" plain @click="exportCSV">导出 CSV</el-button>
      </div>

      <div class="table-shell">
        <el-table :data="orders" stripe row-class-name="clickable-row" @row-click="openDetail">
          <el-table-column v-if="isAdmin" prop="orderNo" label="订单编号" min-width="170" />
          <el-table-column v-if="isAdmin" prop="userId" label="用户ID" width="90" />
          <el-table-column :label="isAdmin ? '车辆编号' : '车型'" min-width="220">
            <template #default="scope">
              {{ isAdmin ? carNo(scope.row) : carName(scope.row) }}
            </template>
          </el-table-column>
          <el-table-column v-if="!isAdmin" label="车牌号" min-width="150">
            <template #default="scope">
              {{ plateNumber(scope.row) }}
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
              {{ formatMoney(totalAmount(scope.row)) }}
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

      <div v-if="isAdmin" class="soft-note">
        管理员仍可在这里查看全部订单明细；订单归还由用户发起申请后，在还车记录页面完成确认。
      </div>
    </div>

    <div v-if="detailVisible && currentOrder" class="detail-overlay" @click.self="closeDetail">
      <section class="detail-modal">
        <header class="detail-modal-header">
          <div>
            <h2>订单详情</h2>
            <p>{{ carName(currentOrder) }}</p>
          </div>
          <button class="detail-close" type="button" @click="closeDetail">关闭</button>
        </header>

        <div class="order-detail-shell">
          <div class="order-detail-cover">
            <img :src="carImage(currentOrder)" :alt="carName(currentOrder)" @error="(e) => e.target.src = placeholderImage" />
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
                <strong>{{ carNo(currentOrder) }}</strong>
              </div>
              <div class="detail-item">
                <span>车辆名称</span>
                <strong>{{ carName(currentOrder) }}</strong>
              </div>
              <div class="detail-item">
                <span>车牌号</span>
                <strong>{{ plateNumber(currentOrder) }}</strong>
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
                <strong>{{ formatMoney(totalAmount(currentOrder)) }}</strong>
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
            v-if="!isAdmin && currentOrder.orderStatus === 'RENTED' && !currentOrder.hasReturnRequest"
            type="primary"
            @click="openReturn(currentOrder)"
          >
            申请还车
          </el-button>
          <el-button
            v-if="!isAdmin && currentOrder.orderStatus === 'RENTED' && currentOrder.hasReturnRequest"
            disabled
          >
            已提交还车申请
          </el-button>
        </footer>
      </section>
    </div>

    <el-dialog v-model="returnVisible" title="提交还车申请" width="520px" append-to-body :z-index="2200">
      <el-form label-width="100px">
        <el-form-item v-if="currentReturnCar" label="当前车辆">
          <span>{{ currentReturnCar.brand }} {{ currentReturnCar.model }} ({{ currentReturnCar.plateNumber }})</span>
        </el-form-item>
        <el-form-item v-if="currentReturnCar" label="初始公里数">
          <span>{{ currentReturnCar.mileage }} km</span>
        </el-form-item>
        <el-form-item label="还车公里数">
          <el-input-number v-model="returnForm.actualMileage" :min="currentReturnCar?.mileage || 0" style="width: 100%" />
          <div v-if="currentReturnCar" style="font-size:12px;color:var(--subtext);margin-top:4px">还车公里数不能低于初始值 {{ currentReturnCar.mileage }} km</div>
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
  border-radius: var(--radius-sm);
  min-height: 0;
  padding: 18px;
  background: var(--gray-100);
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
  border-radius: var(--radius-sm);
  background: var(--gray-50);
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
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
}

.detail-modal {
  width: 85%;
  max-width: 1320px;
  height: min(88vh, 900px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: var(--white);
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
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
  border-bottom: 1px solid var(--line);
}

.detail-modal-footer {
  justify-content: flex-end;
  border-top: 1px solid var(--line);
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
  border-radius: 6px;
  padding: 8px 14px;
  background: var(--gray-100);
  color: var(--gray-600);
  cursor: pointer;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
