<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const orders = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})

const statusMap = { PENDING_PICKUP: '待取车', RENTED: '租赁中', RETURN_PENDING: '待确认还车', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusClass = { PENDING_PICKUP: 'status-pending', RENTED: 'status-rented', RETURN_PENDING: 'status-return-pending', COMPLETED: 'status-completed', CANCELLED: 'status-cancelled' }
const fmt = (v) => v ? String(v).replace('T', ' ') : '-'
const money = (v) => `¥ ${Number(v || 0).toLocaleString()}`
const carName = (row) => row.carInfo ? `${row.carInfo.brand} ${row.carInfo.model}` : '-'

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/rent-orders', { params: { pageNum: currentPage.value, pageSize: pageSize.value } })
    orders.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
onMounted(loadData)

const pickup = async (row) => { await request.put(`/rent-orders/${row.id}/pickup`); ElMessage.success('确认取车'); loadData() }
const cancelOrder = async (row) => { await request.put(`/rent-orders/${row.id}/cancel`); ElMessage.success('已取消'); loadData() }
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="summary-strip">
        <span class="sum-item">全部 <strong>{{ total }}</strong></span>
        <span class="sum-item">待取车 <strong>{{ summary.pendingPickup ?? 0 }}</strong></span>
        <span class="sum-item">租赁中 <strong>{{ summary.active ?? 0 }}</strong></span>
        <span class="sum-item">待确认还车 <strong>{{ summary.returnPending ?? 0 }}</strong></span>
        <span class="sum-item">已完成 <strong>{{ summary.completed ?? 0 }}</strong></span>
      </div>
    </div>
    <div class="card">
      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180"><template #default="{row}"><span class="font-mono" style="font-size:12px;">{{ row.orderNo }}</span></template></el-table-column>
        <el-table-column label="车辆" min-width="160"><template #default="{row}">{{ carName(row) }} <span class="font-mono text-muted" style="font-size:11px;margin-left:4px;">{{ row.carInfo?.plateNumber }}</span></template></el-table-column>
        <el-table-column prop="rentDate" label="取车" width="110" />
        <el-table-column prop="expectedReturnDate" label="还车" width="110" />
        <el-table-column prop="rentDays" label="天数" width="70"><template #default="{row}"><span class="font-mono">{{ row.rentDays }}</span></template></el-table-column>
        <el-table-column label="金额" width="110"><template #default="{row}"><span class="font-mono text-accent">{{ money(row.totalPrice) }}</span></template></el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{row}"><span class="status-badge" :class="statusClass[row.orderStatus]">{{ statusMap[row.orderStatus] || row.orderStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button v-if="row.orderStatus === 'PENDING_PICKUP'" size="small" type="primary" @click="pickup(row)">确认取车</el-button>
            <el-button v-if="row.orderStatus === 'RETURN_PENDING'" size="small" type="success" @click="$router.push('/admin/returns')">去确认还车</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar { margin-bottom: 16px; }
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
</style>
