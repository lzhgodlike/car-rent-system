<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const pageSize = 6
const activeTab = ref('all')

// 还车弹窗
const returnDialogVisible = ref(false)
const returnForm = ref({ rentOrderId: null, actualMileage: null, damageDesc: '' })
const returnSubmitting = ref(false)
const returnMileageAtRent = ref(0)

// 订单详情弹窗
const detailVisible = ref(false)
const detailOrder = ref(null)

// 报修弹窗
const faultDialogVisible = ref(false)
const faultForm = ref({ carId: null, faultContent: '' })
const faultSubmitting = ref(false)

const statusMap = { PENDING_PICKUP: '待取车', RENTED: '进行中', RETURN_PENDING: '待还车', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusClass = { PENDING_PICKUP: 'os-pending', RENTED: 'os-active', RETURN_PENDING: 'os-waiting', COMPLETED: 'os-done', CANCELLED: 'os-cancelled' }
const money = (v) => `¥${Number(v || 0).toLocaleString()}`
const totalAmount = (row) => Number(row.totalPrice || 0) + Number(row.extraFee || 0)
const carName = (row) => row.carInfo ? `${row.carInfo.brand} ${row.carInfo.model}` : '-'

const tabs = [
  { key: 'all', label: '全部订单' },
  { key: 'active', label: '进行中' },
  { key: 'RETURN_PENDING', label: '待还车' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

const loadData = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
    orders.value = []
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const page = await request.get('/rent-orders', {
      params: { pageNum: pageNum.value, pageSize }
    })
    let records = page.records
    // 前端筛选 tab
    if (activeTab.value !== 'all') {
      if (activeTab.value === 'active') {
        records = records.filter(o => ['PENDING_PICKUP', 'RENTED'].includes(o.orderStatus))
      } else {
        records = records.filter(o => o.orderStatus === activeTab.value)
      }
    }
    if (reset) {
      orders.value = records
    } else {
      orders.value = [...orders.value, ...records]
    }
    hasMore.value = orders.value.length < page.total
    if (hasMore.value) pageNum.value++
  } finally { loading.value = false }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try { await loadData() } finally { loadingMore.value = false }
}

// 切换 tab 重新加载
watch(activeTab, () => loadData(true))

// 滚动检测
const showBackToTop = ref(false)
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })
let scrollTimer = null
const handleScroll = () => {
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight
    const clientHeight = document.documentElement.clientHeight
    showBackToTop.value = scrollTop > 300
    if (scrollTop + clientHeight >= scrollHeight - 200) {
      loadMore()
    }
  }, 100)
}

onMounted(() => {
  loadData()
  window.addEventListener('scroll', handleScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  clearTimeout(scrollTimer)
})

// 取消订单弹窗
const cancelDialogVisible = ref(false)
const cancelOrder = ref(null)
const openCancelDialog = (row) => { cancelOrder.value = row; cancelDialogVisible.value = true }
const confirmCancel = async () => {
  await request.put(`/rent-orders/${cancelOrder.value.id}/cancel`)
  ElMessage.success('订单已取消')
  cancelDialogVisible.value = false
  loadData(true)
}

const openReturnDialog = (row) => {
  returnMileageAtRent.value = row.carInfo?.mileage || 0
  returnForm.value = { rentOrderId: row.id, actualMileage: null, damageDesc: '' }
  returnDialogVisible.value = true
}

const submitReturn = async () => {
  if (!returnForm.value.actualMileage) { ElMessage.warning('请输入当前公里数'); return }
  if (returnForm.value.actualMileage <= returnMileageAtRent.value) {
    ElMessage.warning(`当前公里数必须大于出租时的 ${returnMileageAtRent.value} km`); return
  }
  returnSubmitting.value = true
  try {
    await request.post('/return-orders', returnForm.value)
    ElMessage.success('还车申请已提交，等待管理员确认')
    returnDialogVisible.value = false
    loadData(true)
  } finally { returnSubmitting.value = false }
}

const openDetail = (row) => { detailOrder.value = row; detailVisible.value = true }

const openFaultDialog = (row) => {
  faultForm.value = { carId: row.carId, faultContent: '' }
  faultDialogVisible.value = true
}

const submitFault = async () => {
  if (!faultForm.value.faultContent.trim()) { ElMessage.warning('请描述故障内容'); return }
  faultSubmitting.value = true
  try {
    await request.post('/fault-reports', faultForm.value)
    ElMessage.success('报修申请已提交')
    faultDialogVisible.value = false
  } finally { faultSubmitting.value = false }
}

const actionConfig = {
  cancel: { label: '取消订单', class: 'btn-sm-danger', handler: (order) => openCancelDialog(order) },
  return_car: { label: '申请还车', class: 'btn-sm-primary', handler: (order) => openReturnDialog(order) },
  return_pending: { label: '已提交还车申请', class: 'btn-sm-outline', disabled: true },
  report_fault: { label: '报修', class: 'btn-sm-warning', handler: (order) => openFaultDialog(order) },
  repurchase: { label: '再次预订', class: 'btn-sm-outline', handler: (order) => router.push({ path: '/book', query: { carId: order.carId } }) },
  view_detail: { label: '查看详情', class: 'btn-sm-outline', handler: (order) => openDetail(order) },
}

const handleAction = (action, order) => {
  const cfg = actionConfig[action]
  if (cfg?.handler) cfg.handler(order)
}
</script>

<template>
  <div class="orders-page">
    <h2 class="page-title">我的订单</h2>
    <div class="tab-bar">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <div>
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-head">
          <span class="order-id">{{ order.orderNo }}</span>
          <span class="order-status-badge" :class="statusClass[order.orderStatus]">{{ statusMap[order.orderStatus] }}</span>
        </div>
        <div class="order-body">
          <div class="order-car-img">
            <img v-if="order.carInfo?.carImage" :src="order.carInfo.carImage" @error="(e) => e.target.style.display='none'" />
            <el-icon v-else size="28" style="color:var(--muted2);"><Van /></el-icon>
          </div>
          <div class="order-info">
            <div class="order-car-name">{{ carName(order) }}</div>
            <div class="order-dates">
              <span><el-icon><Calendar /></el-icon> 取车：{{ order.rentDate }}</span>
              <span><el-icon><Calendar /></el-icon> 还车：{{ order.expectedReturnDate }}</span>
            </div>
            <div class="order-meta">{{ order.carInfo?.plateNumber }} · {{ order.rentDays }}天 · {{ order.carInfo?.pickupAddress || '待补充' }}</div>
          </div>
          <div class="order-price-col">
            <div class="order-price">{{ money(totalAmount(order)) }}</div>
          </div>
        </div>
        <div class="order-actions">
          <template v-for="action in (order.availableActions || [])" :key="action">
            <button
              v-if="actionConfig[action]"
              class="btn-sm"
              :class="actionConfig[action].class"
              :disabled="actionConfig[action].disabled"
              @click="handleAction(action, order)"
            >{{ actionConfig[action].label }}</button>
          </template>
        </div>
      </div>

      <div v-if="loading && orders.length === 0" class="loading-state">加载中…</div>
      <div v-if="loadingMore" class="loading-state">加载更多…</div>
      <div v-if="!hasMore && orders.length > 0" class="loading-state">没有更多了</div>
      <div v-if="!loading && orders.length === 0" class="empty-state">
        <el-icon size="48" style="color:var(--muted2);margin-bottom:12px;"><Document /></el-icon>
        <div>暂无订单</div>
      </div>
    </div>

    <!-- 返回顶部 -->
    <Transition name="back-top">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
        <el-icon size="15"><ArrowUp /></el-icon>
        <span>返回顶部</span>
      </button>
    </Transition>

    <!-- 还车弹窗 -->
    <div v-if="returnDialogVisible" class="detail-overlay" @click.self="returnDialogVisible = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">申请还车</div>
          <button class="detail-close" @click="returnDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body">
          <div class="return-field">
            <label>当前公里数 <span style="color:var(--accent);">*</span></label>
            <input v-model.number="returnForm.actualMileage" type="number" :min="returnMileageAtRent + 1" :placeholder="`须大于出租时 ${returnMileageAtRent} km`" />
            <div class="return-hint">出租时里程：{{ returnMileageAtRent.toLocaleString() }} km</div>
          </div>
          <div class="return-field">
            <label>车辆损伤</label>
            <textarea v-model="returnForm.damageDesc" rows="3" placeholder="如有损伤请描述，无则留空"></textarea>
          </div>
          <div class="return-actions">
            <button class="btn-sm btn-sm-outline" @click="returnDialogVisible = false">取消</button>
            <button class="btn-sm btn-sm-primary" :disabled="returnSubmitting" @click="submitReturn">{{ returnSubmitting ? '提交中...' : '提交还车申请' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 取消订单弹窗 -->
    <div v-if="cancelDialogVisible" class="detail-overlay" @click.self="cancelDialogVisible = false">
      <div class="detail-modal" style="width:400px;">
        <div class="detail-header">
          <div class="detail-title">取消订单</div>
          <button class="detail-close" @click="cancelDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body" style="text-align:center;">
          <div class="cancel-icon"><el-icon size="40" style="color:var(--accent);"><Warning /></el-icon></div>
          <p class="cancel-text">确定要取消该订单吗？</p>
          <p class="cancel-sub">取消后不可恢复，请谨慎操作</p>
          <div v-if="cancelOrder" class="cancel-info">
            <div class="cancel-info-row"><span>订单号</span><span class="font-mono">{{ cancelOrder.orderNo }}</span></div>
            <div class="cancel-info-row"><span>车辆</span><span>{{ carName(cancelOrder) }}</span></div>
            <div class="cancel-info-row"><span>金额</span><span class="text-accent font-mono">{{ money(totalAmount(cancelOrder)) }}</span></div>
          </div>
          <div class="return-actions" style="margin-top:20px;">
            <button class="btn-sm btn-sm-outline" @click="cancelDialogVisible = false">再想想</button>
            <button class="btn-sm btn-sm-danger" @click="confirmCancel">确定取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 报修弹窗 -->
    <div v-if="faultDialogVisible" class="detail-overlay" @click.self="faultDialogVisible = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">故障报修</div>
          <button class="detail-close" @click="faultDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body">
          <div class="return-field">
            <label>故障描述 <span style="color:var(--accent);">*</span></label>
            <textarea v-model="faultForm.faultContent" rows="4" placeholder="请详细描述车辆故障情况"></textarea>
          </div>
          <div class="return-actions">
            <button class="btn-sm btn-sm-outline" @click="faultDialogVisible = false">取消</button>
            <button class="btn-sm btn-sm-warning" :disabled="faultSubmitting" @click="submitFault">{{ faultSubmitting ? '提交中...' : '提交报修' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <div v-if="detailVisible" class="detail-overlay" @click.self="detailVisible = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">订单详情</div>
          <button class="detail-close" @click="detailVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body" v-if="detailOrder">
          <div class="detail-status-bar">
            <span class="order-status-badge" :class="statusClass[detailOrder.orderStatus]">{{ statusMap[detailOrder.orderStatus] }}</span>
            <span class="detail-order-no">{{ detailOrder.orderNo }}</span>
          </div>
          <div class="detail-car-section">
            <div class="detail-car-img">
              <img v-if="detailOrder.carInfo?.carImage" :src="detailOrder.carInfo.carImage" @error="(e) => e.target.style.display='none'" />
              <el-icon v-else size="40" style="color:var(--muted2);"><Van /></el-icon>
            </div>
            <div class="detail-car-info">
              <div class="detail-car-name">{{ carName(detailOrder) }}</div>
              <div class="detail-car-plate">{{ detailOrder.carInfo?.plateNumber }}</div>
            </div>
          </div>
          <div class="detail-rows">
            <div class="detail-row"><span class="detail-label">取车日期</span><span class="detail-val">{{ detailOrder.rentDate }}</span></div>
            <div class="detail-row"><span class="detail-label">还车日期</span><span class="detail-val">{{ detailOrder.expectedReturnDate }}</span></div>
            <div class="detail-row"><span class="detail-label">取车地点</span><span class="detail-val">{{ detailOrder.carInfo?.pickupAddress || '待补充' }}</span></div>
            <div class="detail-row"><span class="detail-label">租赁天数</span><span class="detail-val">{{ detailOrder.rentDays }}天</span></div>
            <div class="detail-row"><span class="detail-label">单价</span><span class="detail-val">{{ money(detailOrder.unitPrice) }}/天</span></div>
            <div class="detail-row"><span class="detail-label">租金合计</span><span class="detail-val">{{ money(detailOrder.totalPrice) }}</span></div>
            <div class="detail-row" v-if="Number(detailOrder.extraFee) > 0">
              <span class="detail-label">额外费用</span>
              <span class="detail-val" style="color:var(--accent);">{{ money(detailOrder.extraFee) }}</span>
            </div>
            <div class="detail-row detail-row-total">
              <span class="detail-label">应付合计</span>
              <span class="detail-val detail-total-price">{{ money(totalAmount(detailOrder)) }}</span>
            </div>
          </div>
          <div class="detail-row" v-if="detailOrder.remark" style="margin-top:12px;">
            <span class="detail-label">备注</span>
            <span class="detail-val">{{ detailOrder.remark }}</span>
          </div>
          <div class="detail-row" style="margin-top:12px;">
            <span class="detail-label">下单时间</span>
            <span class="detail-val">{{ detailOrder.createTime }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page { padding: 32px 40px; max-width: 900px; margin: 0 auto; }
.page-title { font-size: 24px; font-weight: 700; margin-bottom: 20px; }

/* Tab bar */
.tab-bar {
  display: flex; gap: 4px; margin-bottom: 24px;
  background: var(--white); border-radius: 12px; padding: 6px;
  border: 1px solid var(--border);
}
.tab-btn {
  flex: 1; padding: 8px; border-radius: 8px; border: none; background: none;
  font-size: 13px; font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  color: var(--muted); transition: all .18s; font-weight: 400;
}
.tab-btn:hover { color: var(--text); }
.tab-btn.active { background: var(--accent); color: #fff; font-weight: 500; }

/* Order card */
.order-card {
  background: var(--white); border: 1px solid var(--border); border-radius: var(--radius);
  margin-bottom: 14px; overflow: hidden; box-shadow: var(--shadow-sm); transition: box-shadow .18s;
}
.order-card:hover { box-shadow: var(--shadow-md); }
.order-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid var(--border); background: var(--surface);
}
.order-id { font-size: 12px; color: var(--muted); }
.order-status-badge { font-size: 12px; padding: 3px 10px; border-radius: 20px; font-weight: 500; }
.os-active { background: rgba(44,114,210,0.12); color: var(--info); }
.os-done { background: rgba(58,158,110,0.12); color: var(--success); }
.os-cancelled { background: rgba(200,56,42,0.10); color: var(--accent); }
.os-pending { background: rgba(196,154,60,0.12); color: var(--gold); }
.os-waiting { background: rgba(196,154,60,0.12); color: var(--gold); }

.order-body { display: flex; gap: 16px; padding: 16px 18px; align-items: center; }
.order-car-img {
  width: 80px; height: 56px; background: var(--bg); border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden;
}
.order-car-img img { width: 100%; height: 100%; object-fit: cover; }
.order-info { flex: 1; min-width: 0; }
.order-car-name { font-size: 15px; font-weight: 500; }
.order-dates { font-size: 12px; color: var(--muted); margin-top: 4px; display: flex; gap: 12px; }
.order-dates .el-icon { font-size: 13px; }
.order-meta { font-size: 12px; color: var(--muted); margin-top: 4px; }

.order-price-col { text-align: right; flex-shrink: 0; margin-left: auto; }
.order-price { font-family: 'Bebas Neue', sans-serif; font-size: 18px; font-weight: 500; color: var(--accent); }

.order-actions { padding: 12px 18px; border-top: 1px solid var(--border); display: flex; gap: 8px; justify-content: flex-end; }

/* 按钮 */
.btn-sm {
  padding: 7px 16px; border-radius: 8px; font-size: 13px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .15s;
  display: inline-flex; align-items: center; gap: 6px;
}
.btn-sm-outline { background: none; border: 1px solid var(--border-dark); color: var(--text); }
.btn-sm-outline:hover { border-color: var(--text); }
.btn-sm-outline:disabled { opacity: .5; cursor: not-allowed; border-color: var(--border); color: var(--muted); }
.btn-sm-danger { background: var(--accent-light); border: 1px solid transparent; color: var(--accent); }
.btn-sm-danger:hover { background: var(--accent-mid); }
.btn-sm-primary { background: var(--accent); border: none; color: #fff; }
.btn-sm-primary:hover { background: #b02e22; }
.btn-sm-primary:disabled { opacity: .6; cursor: not-allowed; }
.btn-sm-warning { background: rgba(212,130,10,0.10); border: 1px solid transparent; color: var(--warning); }
.btn-sm-warning:hover { background: rgba(212,130,10,0.18); }

.loading-state, .empty-state { text-align: center; padding: 40px; color: var(--muted); font-size: 13px; }
.empty-state { display: flex; flex-direction: column; align-items: center; }

/* 取消订单弹窗 */
.cancel-icon { margin-bottom: 16px; }
.cancel-text { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.cancel-sub { font-size: 13px; color: var(--muted); margin-bottom: 16px; }
.cancel-info { background: var(--bg); border-radius: 12px; overflow: hidden; text-align: left; }
.cancel-info-row { display: flex; justify-content: space-between; padding: 10px 16px; font-size: 13px; border-bottom: 1px solid var(--border); }
.cancel-info-row:last-child { border-bottom: none; }

/* 返回顶部 - 液态玻璃 */
.back-to-top {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%); z-index: 150;
  height: 44px; border: none; border-radius: 28px; padding: 0 24px;
  cursor: pointer; display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500; color: var(--text); font-family: 'Noto Sans SC', sans-serif;
  background: rgba(255,255,255,0.35);
  backdrop-filter: blur(24px) saturate(1.8) brightness(1.1);
  -webkit-backdrop-filter: blur(24px) saturate(1.8) brightness(1.1);
  border-top: 1px solid rgba(255,255,255,0.7);
  border-left: 1px solid rgba(255,255,255,0.5);
  border-right: 1px solid rgba(255,255,255,0.3);
  border-bottom: 1px solid rgba(255,255,255,0.2);
  box-shadow: 0 8px 32px rgba(0,0,0,0.1), 0 2px 8px rgba(0,0,0,0.04), inset 0 1px 0 rgba(255,255,255,0.8), inset 0 -1px 0 rgba(255,255,255,0.2);
  transition: all .3s cubic-bezier(.34,1.56,.64,1);
}
.back-to-top::before {
  content: ''; position: absolute; inset: 0; border-radius: inherit;
  background: linear-gradient(135deg, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 50%, rgba(255,255,255,0.1) 100%);
  pointer-events: none;
}
.back-to-top:hover { background: rgba(255,255,255,0.55); transform: translateX(-50%) scale(1.05); box-shadow: 0 12px 40px rgba(0,0,0,0.14), inset 0 1px 0 rgba(255,255,255,0.9); }
.back-to-top:active { transform: translateX(-50%) scale(0.95); background: rgba(255,255,255,0.45); transition-duration: .1s; }
.back-top-enter-active, .back-top-leave-active { transition: opacity .3s, transform .3s cubic-bezier(.4,0,.2,1); }
.back-top-enter-from, .back-top-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }

/* 还车/报修表单 */
.return-field { margin-bottom: 16px; }
.return-field label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.return-field input, .return-field textarea {
  width: 100%; padding: 10px 14px; border: 1.5px solid var(--border); border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans SC', sans-serif; background: var(--bg);
  outline: none; color: var(--text); transition: border-color .15s; box-sizing: border-box;
}
.return-field input:focus, .return-field textarea:focus { border-color: var(--accent); }
.return-field textarea { resize: vertical; }
.return-hint { font-size: 11px; color: var(--muted); margin-top: 4px; }
.return-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 20px; }

/* 弹窗 */
.detail-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.detail-modal {
  background: var(--white); border-radius: 20px;
  width: 480px; max-height: 85vh; overflow-y: auto;
  box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(.97); } to { opacity: 1; transform: none; } }
.detail-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.detail-title { font-size: 18px; font-weight: 700; }
.detail-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; padding: 4px; transition: color .15s; }
.detail-close:hover { color: var(--text); }
.detail-body { padding: 24px; }

.detail-status-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}
.detail-order-no { font-size: 12px; color: var(--muted); }

.detail-car-section {
  display: flex; gap: 16px; align-items: center;
  background: var(--bg); border-radius: 12px; padding: 16px; margin-bottom: 20px;
}
.detail-car-img {
  width: 80px; height: 56px; background: var(--white); border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden;
}
.detail-car-img img { width: 100%; height: 100%; object-fit: cover; }
.detail-car-name { font-size: 16px; font-weight: 500; }
.detail-car-plate { font-size: 12px; color: var(--muted); margin-top: 4px; }

.detail-rows {
  border: 1px solid var(--border); border-radius: 12px; overflow: hidden;
}
.detail-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; font-size: 14px;
  border-bottom: 1px solid var(--border);
}
.detail-row:last-child { border-bottom: none; }
.detail-label { color: var(--muted); font-size: 13px; }
.detail-val { font-weight: 500; }
.detail-row-total { background: var(--surface); }
.detail-total-price { font-family: 'Bebas Neue', sans-serif; font-size: 18px; color: var(--accent); font-weight: 500; }
</style>
