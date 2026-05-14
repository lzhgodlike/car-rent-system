<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const orders = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const filterStatus = ref('')
const keyword = ref('')

// 详情弹窗
const detailVisible = ref(false)
const detailOrder = ref(null)

// 取车处理弹窗
const pickupDialogVisible = ref(false)
const pickupOrder = ref(null)

const statusMap = { PENDING_PICKUP: '待取车', RENTED: '租赁中', RETURN_PENDING: '待确认还车', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusClass = { PENDING_PICKUP: 'status-pending', RENTED: 'status-rented', RETURN_PENDING: 'status-return-pending', COMPLETED: 'status-completed', CANCELLED: 'status-cancelled' }
const carStatusMap = { AVAILABLE: '空闲', RESERVED: '已预订', RENTED: '租赁中', AWAITING_REPAIR: '待维修', REPAIRING: '维修中', DISABLED: '停用' }
const returnStatusMap = { PENDING: '待确认', CONFIRMED: '已确认' }
const fmt = (v) => v ? String(v).replace('T', ' ') : '-'
const money = (v) => `¥ ${Number(v || 0).toLocaleString()}`
const carName = (row) => row.carInfo ? `${row.carInfo.brand} ${row.carInfo.model}` : '-'
const maskIdCard = (v) => {
  if (!v || v.length < 8) return v || '-'
  return v.slice(0, 4) + '****' + v.slice(-4)
}

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/rent-orders', { params: { pageNum: currentPage.value, pageSize: pageSize.value, status: filterStatus.value || undefined, keyword: keyword.value || undefined } })
    orders.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
const onFilterChange = () => { currentPage.value = 1; loadData() }
const onReset = () => { keyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
let searchTimer = null
const onKeywordChange = () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { currentPage.value = 1; loadData() }, 300) }
onMounted(loadData)

const openDetail = (row) => { detailOrder.value = row; detailVisible.value = true }

const openPickupDialog = (row) => { pickupOrder.value = row; pickupDialogVisible.value = true }

const confirmPickup = async () => {
  await request.put(`/rent-orders/${pickupOrder.value.id}/pickup`)
  ElMessage.success('已确认取车')
  pickupDialogVisible.value = false
  loadData()
}

const rejectPickup = async () => {
  try {
    await ElMessageBox.confirm('确定拒绝该用户的取车申请吗？订单将被取消。', '拒绝取车', { type: 'warning', confirmButtonText: '确定拒绝', cancelButtonText: '再想想' })
    await request.put(`/rent-orders/${pickupOrder.value.id}/reject-pickup`)
    ElMessage.success('已拒绝取车')
    pickupDialogVisible.value = false
    loadData()
  } catch {}
}
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
      <div style="display:flex;gap:8px;align-items:center;">
        <el-input v-model="keyword" placeholder="搜索用户名、车辆、车牌号…" clearable size="small" style="width:220px;" @input="onKeywordChange" @clear="onKeywordChange" />
        <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="small" style="width:140px;" @change="onFilterChange">
          <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
        </el-select>
        <button class="btn-sm btn-sm-ghost" @click="onReset"><el-icon><RefreshLeft /></el-icon></button>
      </div>
    </div>
    <div class="card">
      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="180"><template #default="{row}"><span class="font-mono" style="font-size:12px;">{{ row.orderNo }}</span></template></el-table-column>
        <el-table-column label="用户名" width="100"><template #default="{row}">{{ row.userName || '-' }}</template></el-table-column>
        <el-table-column label="车辆" min-width="160">
          <template #default="{row}">
            <div class="car-cell">
              <div class="car-cell-name">{{ carName(row) }}</div>
              <div class="car-cell-plate">{{ row.carInfo?.plateNumber }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="rentDate" label="取车" width="110" />
        <el-table-column prop="expectedReturnDate" label="还车" width="110" />
        <el-table-column prop="rentDays" label="天数" width="70"><template #default="{row}"><span class="font-mono">{{ row.rentDays }}</span></template></el-table-column>
        <el-table-column label="金额" width="110"><template #default="{row}"><span class="font-mono text-accent">{{ money(row.totalPrice) }}</span></template></el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{row}"><span class="status-badge" :class="statusClass[row.orderStatus]">{{ statusMap[row.orderStatus] || row.orderStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{row}">
            <button v-if="row.orderStatus === 'PENDING_PICKUP'" class="btn-sm btn-sm-primary" @click="openPickupDialog(row)">处理取车</button>
            <button v-if="row.orderStatus === 'RETURN_PENDING'" class="btn-sm btn-sm-success" @click="$router.push('/admin/returns')">去确认还车</button>
            <button class="btn-sm btn-sm-ghost" @click="openDetail(row)">查看详情</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <!-- 处理取车弹窗 -->
    <el-dialog v-model="pickupDialogVisible" title="处理取车" width="480px" destroy-on-close>
      <div v-if="pickupOrder" class="pickup-dialog">
        <div class="pickup-info">
          <div class="pickup-row"><span class="pickup-label">订单号</span><span class="font-mono">{{ pickupOrder.orderNo }}</span></div>
          <div class="pickup-row"><span class="pickup-label">用户名</span><span>{{ pickupOrder.userName || '-' }}</span></div>
          <div class="pickup-row"><span class="pickup-label">姓名</span><span>{{ pickupOrder.userRealName || '-' }}</span></div>
          <div class="pickup-row"><span class="pickup-label">车辆</span><span>{{ carName(pickupOrder) }} {{ pickupOrder.carInfo?.plateNumber }}</span></div>
          <div class="pickup-row"><span class="pickup-label">取车地点</span><span>{{ pickupOrder.carInfo?.pickupAddress || '待补充' }}</span></div>
          <div class="pickup-row"><span class="pickup-label">取车日期</span><span>{{ pickupOrder.rentDate }}</span></div>
          <div class="pickup-row"><span class="pickup-label">还车日期</span><span>{{ pickupOrder.expectedReturnDate }}</span></div>
          <div class="pickup-row"><span class="pickup-label">租赁天数</span><span>{{ pickupOrder.rentDays }}天</span></div>
          <div class="pickup-row"><span class="pickup-label">金额</span><span class="text-accent font-mono">{{ money(pickupOrder.totalPrice) }}</span></div>
          <div v-if="pickupOrder.remark" class="pickup-remark">
            <div class="pickup-remark-label">用户备注</div>
            <div class="pickup-remark-text">{{ pickupOrder.remark }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn-sm btn-sm-danger" @click="rejectPickup">拒绝取车</button>
        <button class="btn-sm btn-sm-primary" @click="confirmPickup">确认取车</button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="520px" destroy-on-close>
      <div v-if="detailOrder" class="detail-content">
        <!-- 用户信息 -->
        <div class="detail-section-title">用户信息</div>
        <div class="detail-rows">
          <div class="detail-row"><span class="detail-label">用户名</span><span class="detail-val">{{ detailOrder.userName || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">姓名</span><span class="detail-val">{{ detailOrder.userRealName || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">手机号</span><span class="detail-val">{{ detailOrder.userPhone || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">身份证号</span><span class="detail-val">{{ maskIdCard(detailOrder.userIdCard) }}</span></div>
        </div>

        <!-- 车辆信息 -->
        <div class="detail-section-title">车辆信息</div>
        <div class="detail-car-card">
          <div class="detail-car-img">
            <img v-if="detailOrder.carInfo?.carImage" :src="detailOrder.carInfo.carImage" @error="(e) => e.target.style.display='none'" />
            <el-icon v-else size="32" style="color:var(--muted2);"><Van /></el-icon>
          </div>
          <div class="detail-car-info">
            <div class="detail-car-name">{{ carName(detailOrder) }}</div>
            <div class="detail-car-plate">{{ detailOrder.carInfo?.plateNumber }}</div>
          </div>
        </div>
        <div class="detail-rows">
          <div class="detail-row"><span class="detail-label">车型分类</span><span class="detail-val">{{ detailOrder.carInfo?.typeName || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">每日租金</span><span class="detail-val">{{ money(detailOrder.carInfo?.dayPrice) }}/天</span></div>
          <div class="detail-row"><span class="detail-label">车辆状态</span><span class="detail-val">{{ carStatusMap[detailOrder.carInfo?.status] || detailOrder.carInfo?.status || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">取车地址</span><span class="detail-val">{{ detailOrder.carInfo?.pickupAddress || '-' }}</span></div>
        </div>

        <!-- 订单信息 -->
        <div class="detail-section-title">订单信息</div>
        <div class="detail-rows">
          <div class="detail-row"><span class="detail-label">订单号</span><span class="detail-val font-mono">{{ detailOrder.orderNo }}</span></div>
          <div class="detail-row"><span class="detail-label">订单状态</span><span class="detail-val"><span class="status-badge" :class="statusClass[detailOrder.orderStatus]">{{ statusMap[detailOrder.orderStatus] }}</span></span></div>
          <div class="detail-row"><span class="detail-label">取车日期</span><span class="detail-val">{{ detailOrder.rentDate }}</span></div>
          <div class="detail-row"><span class="detail-label">还车日期</span><span class="detail-val">{{ detailOrder.expectedReturnDate }}</span></div>
          <div class="detail-row"><span class="detail-label">实际还车</span><span class="detail-val">{{ detailOrder.actualReturnDate || '未还车' }}</span></div>
          <div class="detail-row"><span class="detail-label">租赁天数</span><span class="detail-val">{{ detailOrder.rentDays }}天</span></div>
          <div class="detail-row" v-if="detailOrder.remark"><span class="detail-label">备注</span><span class="detail-val">{{ detailOrder.remark }}</span></div>
          <div class="detail-row"><span class="detail-label">下单时间</span><span class="detail-val">{{ fmt(detailOrder.createTime) }}</span></div>
        </div>

        <!-- 还车信息 -->
        <template v-if="detailOrder.returnOrder">
          <div class="detail-section-title">还车信息</div>
          <div class="detail-rows">
            <div class="detail-row"><span class="detail-label">还车状态</span><span class="detail-val">{{ returnStatusMap[detailOrder.returnOrder.status] || detailOrder.returnOrder.status }}</span></div>
            <div class="detail-row"><span class="detail-label">申请时间</span><span class="detail-val">{{ fmt(detailOrder.returnOrder.createTime) }}</span></div>
            <div class="detail-row"><span class="detail-label">还车公里数</span><span class="detail-val">{{ detailOrder.returnOrder.actualMileage?.toLocaleString() || '-' }} km</span></div>
            <div class="detail-row"><span class="detail-label">车辆损伤</span><span class="detail-val">{{ detailOrder.returnOrder.damageDesc || '无' }}</span></div>
            <div class="detail-row"><span class="detail-label">附加费用</span><span class="detail-val text-accent">{{ money(detailOrder.returnOrder.extraFee) }}</span></div>
            <div class="detail-row"><span class="detail-label">处理时间</span><span class="detail-val">{{ fmt(detailOrder.returnOrder.updateTime) }}</span></div>
          </div>
        </template>

        <!-- 费用明细 -->
        <div class="detail-section-title">费用明细</div>
        <div class="detail-rows">
          <div class="detail-row"><span class="detail-label">租金</span><span class="detail-val">{{ money(detailOrder.unitPrice) }} × {{ detailOrder.rentDays }}天 = {{ money(detailOrder.totalPrice) }}</span></div>
          <div class="detail-row" v-if="Number(detailOrder.extraFee) > 0"><span class="detail-label">额外费用</span><span class="detail-val text-accent">{{ money(detailOrder.extraFee) }}</span></div>
          <div class="detail-row detail-row-total"><span class="detail-label">应付合计</span><span class="detail-val detail-total-price">{{ money(Number(detailOrder.totalPrice || 0) + Number(detailOrder.extraFee || 0)) }}</span></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
.car-cell-name { font-weight: 500; font-size: 13px; }
.car-cell-plate { font-size: 11px; color: var(--muted); margin-top: 2px; }
:deep(.el-input--small) { --el-input-bg-color: var(--surface2); --el-input-border-color: var(--border); --el-input-hover-border-color: var(--accent); --el-input-focus-border-color: var(--accent); }
:deep(.el-select--small) { --el-select-input-bg-color: var(--surface2); --el-select-border-color: var(--border); }

/* 取车弹窗 */
.pickup-info { border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pickup-row { display: flex; justify-content: space-between; padding: 12px 16px; font-size: 14px; border-bottom: 1px solid var(--border); }
.pickup-row:last-child { border-bottom: none; }
.pickup-label { color: var(--muted); font-size: 13px; }
.pickup-remark {
  margin: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.25);
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.08);
}
.pickup-remark-label {
  font-size: 12px;
  color: var(--accent);
  margin-bottom: 8px;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.pickup-remark-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text);
  font-weight: 500;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 详情弹窗 */
.detail-content { max-height: 65vh; overflow-y: auto; }
.detail-section-title { font-size: 13px; font-weight: 500; color: var(--muted); margin: 16px 0 8px; }
.detail-section-title:first-child { margin-top: 0; }
.detail-rows { border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; font-size: 14px; border-bottom: 1px solid var(--border); }
.detail-row:last-child { border-bottom: none; }
.detail-label { color: var(--muted); font-size: 13px; }
.detail-val { font-weight: 500; }
.detail-row-total { background: var(--bg); }
.detail-total-price { font-family: var(--font-mono); font-size: 18px; color: var(--accent); }

/* 车辆卡片 */
.detail-car-card {
  display: flex; gap: 14px; align-items: center;
  background: var(--bg); border-radius: 12px; padding: 14px; margin-bottom: 4px;
}
.detail-car-img {
  width: 72px; height: 48px; background: var(--surface); border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden;
}
.detail-car-img img { width: 100%; height: 100%; object-fit: cover; }
.detail-car-name { font-size: 15px; font-weight: 500; }
.detail-car-plate { font-size: 12px; color: var(--muted); margin-top: 2px; }
</style>
