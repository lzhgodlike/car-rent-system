<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const records = ref([])
const rentOrders = ref([])
const createForm = reactive({ rentOrderId: null, actualMileage: 0, damageDesc: '' })
const confirmDialog = ref(false)
const currentId = ref(null)
const confirmForm = reactive({ extraFee: 0 })
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const handlePageChange = (page) => { currentPage.value = page; loadData() }
const handleSizeChange = (size) => { pageSize.value = size; currentPage.value = 1; loadData() }

const returnStatusMap = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
}

const formatReturnStatus = (status) => returnStatusMap[status] || status || '-'
const statusTone = (status) => (status === 'PENDING' ? 'warning' : 'success')

const loadData = async () => {
  loading.value = true
  try {
    const [recordPage, rentPage] = await Promise.all([
      request.get('/return-orders', { params: { pageNum: currentPage.value, pageSize: pageSize.value } }),
      request.get('/rent-orders', { params: { pageNum: 1, pageSize: 9999 } }),
    ])
    records.value = recordPage.records
    total.value = recordPage.total
    summary.value = recordPage.summary || {}
    rentOrders.value = rentPage.records
  } finally {
    loading.value = false
  }
}

const carName = (row) => {
  const c = row.carInfo
  if (!c) return '-'
  return `${c.brand || ''} ${c.model || ''}`.trim() || '-'
}
const plateNumber = (row) => row.carInfo?.plateNumber || '-'
const rentPeriod = (row) => {
  const b = row.rentOrderBrief
  if (!b) return '-'
  return `${b.rentDate || ''} 至 ${b.expectedReturnDate || ''}`
}
const rentDays = (row) => row.rentOrderBrief?.rentDays || '-'

const currentReturnOrder = computed(() => {
  if (!currentId.value) return null
  return records.value.find((r) => r.id === currentId.value) || null
})

const currentReturnCar = computed(() => {
  if (!currentReturnOrder.value) return null
  return currentReturnOrder.value.carInfo || null
})

const availableRentOrders = computed(() => {
  const appliedIds = new Set(records.value.map((item) => item.rentOrderId))
  return rentOrders.value.filter((item) => item.orderStatus === 'RENTED' && !appliedIds.has(item.id))
})

const selectedRentOrderCarMileage = computed(() => {
  if (!createForm.rentOrderId) return 0
  const order = rentOrders.value.find((o) => o.id === createForm.rentOrderId)
  return order?.carInfo?.mileage || 0
})

watch(() => createForm.rentOrderId, () => {
  createForm.actualMileage = selectedRentOrderCarMileage.value
})

const submitReturn = async () => {
  if (createForm.actualMileage <= selectedRentOrderCarMileage.value) {
    ElMessage.warning('还车公里数必须大于初始公里数')
    return
  }
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
        <div class="metric-pill"><span>记录总数</span><strong>{{ total }}</strong></div>
        <div class="metric-pill"><span>待确认</span><strong>{{ summary.pending ?? 0 }}</strong></div>
        <div class="metric-pill"><span>已确认</span><strong>{{ summary.confirmed ?? 0 }}</strong></div>
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
                  :label="`${item.orderNo} / ${item.carInfo?.brand || ''} ${item.carInfo?.model || ''} (${item.carInfo?.plateNumber || '-'}) / ${item.rentDate} 至 ${item.expectedReturnDate}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="当前公里数">
              <el-input-number v-model="createForm.actualMileage" :min="selectedRentOrderCarMileage" style="width: 100%" />
            </el-form-item>
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
                <div>{{ carName(scope.row) }}</div>
                <div style="font-size:12px;color:var(--subtext)">{{ plateNumber(scope.row) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="原租期" min-width="200">
              <template #default="scope">
                <div>{{ rentPeriod(scope.row) }}</div>
                <div style="font-size:12px;color:var(--subtext)">共 {{ rentDays(scope.row) }} 天</div>
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

      <el-dialog v-model="confirmDialog" title="确认还车" width="520px" append-to-body>
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
          <div class="confirm-info-row" v-if="currentReturnOrder.rentOrderBrief">
            <span>原订单租金</span>
            <strong>￥{{ currentReturnOrder.rentOrderBrief.totalPrice || 0 }}</strong>
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

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

