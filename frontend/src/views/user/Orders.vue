<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import SupportChatDrawer from '../../components/support/SupportChatDrawer.vue'

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
const carImages = (row) => {
  const images = row.carInfo?.carImages?.filter(Boolean) || []
  if (images.length) return images
  return row.carInfo?.carImage ? [row.carInfo.carImage] : []
}
const primaryCarImage = (row) => carImages(row)[0] || ''

const tabs = [
  { key: 'all', label: '全部订单' },
  { key: 'active', label: '进行中' },
  { key: 'RETURN_PENDING', label: '待还车' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

const tabStatusParams = {
  all: undefined,
  active: 'active',
  RETURN_PENDING: 'RETURN_PENDING',
  COMPLETED: 'COMPLETED',
  CANCELLED: 'CANCELLED'
}

const loadData = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
    orders.value = []
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const status = tabStatusParams[activeTab.value]
    const page = await request.get('/rent-orders', {
      params: { pageNum: pageNum.value, pageSize, status }
    })
    const records = page.records
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

watch(activeTab, () => loadData(true))

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

const supportDialogVisible = ref(false)

const openDetail = (row) => { detailOrder.value = row; detailVisible.value = true }
const openSupportDialog = () => { supportDialogVisible.value = true }

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
  contact_support: { label: '联系客服', class: 'btn-sm-primary', handler: () => openSupportDialog() },
}

const handleAction = (action, order) => {
  const cfg = actionConfig[action]
  if (cfg?.handler) cfg.handler(order)
}
</script>

<template>
  <div class="orders-page">
    <div class="page-header">
      <h2 class="page-title">我的订单</h2>
      <p class="page-sub">管理您的所有租车记录</p>
    </div>

    <div class="tab-bar">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
      </button>
    </div>

    <div class="orders-list">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <!-- 左侧状态竖条 -->
        <div class="order-side-bar" :class="statusClass[order.id]"></div>

        <div class="order-inner">
          <div class="order-head">
            <div class="order-head-left">
              <span class="order-id-label">订单号</span>
              <span class="order-id">{{ order.orderNo }}</span>
            </div>
            <span class="order-status-badge" :class="statusClass[order.orderStatus]">
              <span class="status-dot"></span>
              {{ statusMap[order.orderStatus] }}
            </span>
          </div>

          <div class="order-body">
            <div class="order-car-img">
              <img v-if="primaryCarImage(order)" :src="primaryCarImage(order)" @error="(e) => e.target.style.display='none'" />
              <el-icon v-else size="28" style="color:var(--muted2);"><Van /></el-icon>
            </div>
            <div class="order-info">
              <div class="order-car-name">{{ carName(order) }}</div>
              <div class="order-plate">{{ order.carInfo?.plateNumber }}</div>
              <div class="order-dates">
                <div class="date-item">
                  <span class="date-label">取车</span>
                  <span class="date-val">{{ order.rentDate }}</span>
                </div>
                <div class="date-divider">→</div>
                <div class="date-item">
                  <span class="date-label">还车</span>
                  <span class="date-val">{{ order.expectedReturnDate }}</span>
                </div>
              </div>
              <div class="order-meta">
                <span class="meta-tag">{{ order.rentDays }}天</span>
                <span class="meta-sep">·</span>
                <span>{{ order.carInfo?.pickupAddress || '取车地点待补充' }}</span>
              </div>
            </div>
            <div class="order-price-col">
              <div class="order-price-label">应付金额</div>
              <div class="order-price">{{ money(totalAmount(order)) }}</div>
            </div>
          </div>

          <div class="order-actions">
            <button class="btn-sm btn-sm-primary" @click="openSupportDialog">联系客服</button>
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
      </div>

      <div v-if="loading && orders.length === 0" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中…</span>
      </div>
      <div v-if="loadingMore" class="loading-more">
        <div class="loading-spinner sm"></div>
        <span>加载更多…</span>
      </div>
      <div v-if="!hasMore && orders.length > 0" class="end-hint">— 已显示全部订单 —</div>
      <div v-if="!loading && orders.length === 0" class="empty-state">
        <div class="empty-icon"><el-icon size="40"><Document /></el-icon></div>
        <div class="empty-text">暂无订单</div>
        <div class="empty-sub">您目前没有符合条件的订单记录</div>
      </div>
    </div>

    <!-- 返回顶部 -->
    <Transition name="back-top">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
        <el-icon size="14"><ArrowUp /></el-icon>
        <span>返回顶部</span>
      </button>
    </Transition>

    <!-- 还车弹窗 -->
    <div v-if="returnDialogVisible" class="modal-overlay" @click.self="returnDialogVisible = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title-wrap">
            <div class="modal-icon-badge primary"><el-icon><Van /></el-icon></div>
            <div class="modal-title">申请还车</div>
          </div>
          <button class="modal-close" @click="returnDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">当前公里数 <span class="required">*</span></label>
            <input v-model.number="returnForm.actualMileage" type="number" class="form-input" :min="returnMileageAtRent + 1" :placeholder="`须大于出租时 ${returnMileageAtRent} km`" />
            <div class="form-hint">出租时里程：{{ returnMileageAtRent.toLocaleString() }} km</div>
          </div>
          <div class="form-field">
            <label class="form-label">车辆损伤说明</label>
            <textarea v-model="returnForm.damageDesc" class="form-textarea" rows="3" placeholder="如有损伤请描述，无则留空"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-sm btn-sm-outline" @click="returnDialogVisible = false">取消</button>
            <button class="btn-sm btn-sm-primary" :disabled="returnSubmitting" @click="submitReturn">
              {{ returnSubmitting ? '提交中…' : '确认提交' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 取消订单弹窗 -->
    <div v-if="cancelDialogVisible" class="modal-overlay" @click.self="cancelDialogVisible = false">
      <div class="modal modal-sm">
        <div class="modal-header">
          <div class="modal-title-wrap">
            <div class="modal-icon-badge danger"><el-icon><Warning /></el-icon></div>
            <div class="modal-title">取消订单</div>
          </div>
          <button class="modal-close" @click="cancelDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="modal-body">
          <p class="cancel-text">确定要取消该订单吗？</p>
          <p class="cancel-sub">此操作不可撤销，请谨慎确认</p>
          <div v-if="cancelOrder" class="info-card">
            <div class="info-row"><span class="info-label">订单号</span><span class="info-val mono">{{ cancelOrder.orderNo }}</span></div>
            <div class="info-row"><span class="info-label">车辆</span><span class="info-val">{{ carName(cancelOrder) }}</span></div>
            <div class="info-row"><span class="info-label">金额</span><span class="info-val accent">{{ money(totalAmount(cancelOrder)) }}</span></div>
          </div>
          <div class="modal-actions">
            <button class="btn-sm btn-sm-outline" @click="cancelDialogVisible = false">再想想</button>
            <button class="btn-sm btn-sm-danger" @click="confirmCancel">确定取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 报修弹窗 -->
    <div v-if="faultDialogVisible" class="modal-overlay" @click.self="faultDialogVisible = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title-wrap">
            <div class="modal-icon-badge warning"><el-icon><Tools /></el-icon></div>
            <div class="modal-title">故障报修</div>
          </div>
          <button class="modal-close" @click="faultDialogVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="modal-body">
          <div class="form-field">
            <label class="form-label">故障描述 <span class="required">*</span></label>
            <textarea v-model="faultForm.faultContent" class="form-textarea" rows="4" placeholder="请详细描述车辆故障情况，包括故障现象、发生时间、所在位置等"></textarea>
          </div>
          <div class="modal-actions">
            <button class="btn-sm btn-sm-outline" @click="faultDialogVisible = false">取消</button>
            <button class="btn-sm btn-sm-warning" :disabled="faultSubmitting" @click="submitFault">
              {{ faultSubmitting ? '提交中…' : '提交报修' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
      <div class="modal modal-detail">
        <div class="modal-header">
          <div class="modal-title-wrap">
            <div class="modal-title">订单详情</div>
          </div>
          <button class="modal-close" @click="detailVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="modal-body" v-if="detailOrder">
          <div class="detail-status-bar">
            <span class="order-status-badge lg" :class="statusClass[detailOrder.orderStatus]">
              <span class="status-dot"></span>
              {{ statusMap[detailOrder.orderStatus] }}
            </span>
            <span class="detail-order-no">{{ detailOrder.orderNo }}</span>
          </div>

          <div class="detail-car-section">
            <div class="detail-car-img">
              <img v-if="primaryCarImage(detailOrder)" :src="primaryCarImage(detailOrder)" @error="(e) => e.target.style.display='none'" />
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
              <span class="detail-val accent">{{ money(detailOrder.extraFee) }}</span>
            </div>
            <div class="detail-row detail-row-total">
              <span class="detail-label">应付合计</span>
              <span class="detail-val detail-total-price">{{ money(totalAmount(detailOrder)) }}</span>
            </div>
          </div>

          <div class="detail-footer">
            <div class="detail-row-flat" v-if="detailOrder.remark"><span class="detail-label">备注</span><span class="detail-val">{{ detailOrder.remark }}</span></div>
            <div class="detail-row-flat"><span class="detail-label">下单时间</span><span class="detail-val mono-sm">{{ detailOrder.createTime }}</span></div>
          </div>
        </div>
      </div>
    </div>

    <SupportChatDrawer v-model="supportDialogVisible" />
  </div>
</template>

<style scoped>
/* ─── 页面容器 ─── */
.orders-page {
  padding: 40px 48px;
  max-width: 920px;
  margin: 0 auto;
}

/* ─── 页头 ─── */
.page-header { margin-bottom: 28px; }
.page-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0 0 4px;
  color: var(--text);
}
.page-sub {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

/* ─── Tab 栏 ─── */
.tab-bar {
  display: flex;
  gap: 2px;
  margin-bottom: 24px;
  background: var(--surface);
  border-radius: 14px;
  padding: 5px;
  border: 1px solid var(--border);
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.04);
}
.tab-btn {
  flex: 1;
  padding: 9px 4px;
  border-radius: 10px;
  border: none;
  background: none;
  font-size: 13px;
  font-family: 'Noto Sans SC', sans-serif;
  cursor: pointer;
  color: var(--muted);
  transition: all .18s ease;
  font-weight: 400;
  letter-spacing: 0.2px;
}
.tab-btn:hover { color: var(--text); }
.tab-btn.active {
  background: var(--white);
  color: var(--accent);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08), 0 1px 3px rgba(0,0,0,0.04);
}

/* ─── 订单卡片 ─── */
.orders-list { display: flex; flex-direction: column; gap: 12px; }

.order-card {
  display: flex;
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04), 0 4px 16px rgba(0,0,0,0.03);
  transition: box-shadow .2s ease, transform .2s ease;
}
.order-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.08), 0 1px 6px rgba(0,0,0,0.04);
  transform: translateY(-1px);
}

/* 左侧状态色条 */
.order-side-bar {
  width: 4px;
  flex-shrink: 0;
  background: var(--border);
}
.order-card:has(.os-active) .order-side-bar { background: var(--info); }
.order-card:has(.os-done) .order-side-bar { background: var(--success); }
.order-card:has(.os-cancelled) .order-side-bar { background: var(--accent); }
.order-card:has(.os-pending) .order-side-bar { background: var(--gold); }
.order-card:has(.os-waiting) .order-side-bar { background: var(--gold); }

.order-inner { flex: 1; min-width: 0; }

/* 卡片头 */
.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 20px 12px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.order-head-left { display: flex; align-items: center; gap: 8px; }
.order-id-label {
  font-size: 11px;
  color: var(--muted2);
  background: var(--bg);
  border: 1px solid var(--border);
  padding: 2px 7px;
  border-radius: 4px;
  letter-spacing: 0.3px;
}
.order-id { font-size: 12px; color: var(--muted); font-family: 'Bebas Neue', monospace; letter-spacing: 0.8px; }

/* 状态徽章 */
.order-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 4px 10px 4px 8px;
  border-radius: 20px;
  font-weight: 500;
  letter-spacing: 0.2px;
}
.order-status-badge.lg { font-size: 13px; padding: 5px 12px 5px 10px; }

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.os-active { background: rgba(44,114,210,0.10); color: var(--info); }
.os-active .status-dot { background: var(--info); box-shadow: 0 0 0 2px rgba(44,114,210,0.25); animation: pulse 2s infinite; }
.os-done { background: rgba(58,158,110,0.10); color: var(--success); }
.os-done .status-dot { background: var(--success); }
.os-cancelled { background: rgba(200,56,42,0.08); color: var(--accent); }
.os-cancelled .status-dot { background: var(--accent); }
.os-pending { background: rgba(196,154,60,0.10); color: var(--gold); }
.os-pending .status-dot { background: var(--gold); }
.os-waiting { background: rgba(196,154,60,0.10); color: var(--gold); }
.os-waiting .status-dot { background: var(--gold); animation: pulse 2s infinite; }

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 currentColor; }
  50% { box-shadow: 0 0 0 3px transparent; }
}

/* 卡片主体 */
.order-body {
  display: flex;
  gap: 18px;
  padding: 18px 20px;
  align-items: center;
}
.order-car-img {
  width: 88px;
  height: 62px;
  background: var(--bg);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border);
}
.order-car-img img { width: 100%; height: 100%; object-fit: cover; }

.order-info { flex: 1; min-width: 0; }
.order-car-name { font-size: 15px; font-weight: 600; letter-spacing: -0.1px; margin-bottom: 2px; }
.order-plate {
  display: inline-block;
  font-size: 11px;
  color: var(--muted);
  background: var(--bg);
  border: 1px solid var(--border);
  padding: 1px 7px;
  border-radius: 4px;
  margin-bottom: 10px;
  letter-spacing: 1px;
}

.order-dates {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.date-item { display: flex; align-items: center; gap: 5px; }
.date-label {
  font-size: 10px;
  color: var(--muted2);
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 500;
}
.date-val { font-size: 13px; color: var(--text); }
.date-divider { font-size: 12px; color: var(--muted2); }

.order-meta { font-size: 12px; color: var(--muted); display: flex; align-items: center; gap: 6px; }
.meta-tag {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 1px 7px;
  border-radius: 4px;
  font-size: 11px;
}
.meta-sep { color: var(--muted2); }

.order-price-col {
  text-align: right;
  flex-shrink: 0;
  margin-left: auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.order-price-label { font-size: 10px; color: var(--muted2); letter-spacing: 0.3px; }
.order-price {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 22px;
  color: var(--accent);
  letter-spacing: 0.5px;
  line-height: 1;
}

/* 卡片操作栏 */
.order-actions {
  padding: 12px 20px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  background: var(--surface);
}

/* ─── 按钮 ─── */
.btn-sm {
  padding: 7px 16px;
  border-radius: 8px;
  font-size: 12.5px;
  font-family: 'Noto Sans SC', sans-serif;
  cursor: pointer;
  transition: all .15s ease;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
  letter-spacing: 0.2px;
}
.btn-sm-outline {
  background: var(--white);
  border: 1px solid var(--border-dark);
  color: var(--text);
}
.btn-sm-outline:hover { border-color: var(--text); background: var(--bg); }
.btn-sm-outline:disabled { opacity: .45; cursor: not-allowed; border-color: var(--border); color: var(--muted); }
.btn-sm-danger {
  background: rgba(200,56,42,0.07);
  border: 1px solid rgba(200,56,42,0.2);
  color: var(--accent);
}
.btn-sm-danger:hover { background: rgba(200,56,42,0.12); border-color: rgba(200,56,42,0.35); }
.btn-sm-primary {
  background: var(--accent);
  border: none;
  color: #fff;
  box-shadow: 0 2px 8px rgba(200,56,42,0.3);
}
.btn-sm-primary:hover { background: #b02e22; box-shadow: 0 3px 12px rgba(200,56,42,0.35); }
.btn-sm-primary:disabled { opacity: .6; cursor: not-allowed; box-shadow: none; }
.btn-sm-warning {
  background: rgba(212,130,10,0.08);
  border: 1px solid rgba(212,130,10,0.2);
  color: var(--warning);
}
.btn-sm-warning:hover { background: rgba(212,130,10,0.14); }

/* ─── 状态提示 ─── */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 40px;
  color: var(--muted);
  font-size: 13px;
}
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: var(--muted);
  font-size: 13px;
}
.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2.5px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin .7s linear infinite;
}
.loading-spinner.sm { width: 16px; height: 16px; border-width: 2px; }
@keyframes spin { to { transform: rotate(360deg); } }

.end-hint {
  text-align: center;
  padding: 20px;
  color: var(--muted2);
  font-size: 12px;
  letter-spacing: 1px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 40px;
  color: var(--muted);
}
.empty-icon {
  width: 72px;
  height: 72px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: var(--muted2);
}
.empty-text { font-size: 16px; font-weight: 600; margin-bottom: 6px; color: var(--text); }
.empty-sub { font-size: 13px; color: var(--muted2); }

/* ─── 返回顶部 ─── */
.back-to-top {
  position: fixed;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 150;
  height: 40px;
  border: none;
  border-radius: 40px;
  padding: 0 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--text);
  font-family: 'Noto Sans SC', sans-serif;
  background: rgba(255,255,255,0.6);
  backdrop-filter: blur(20px) saturate(1.6);
  -webkit-backdrop-filter: blur(20px) saturate(1.6);
  border: 1px solid rgba(255,255,255,0.7);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1), 0 2px 6px rgba(0,0,0,0.05), inset 0 1px 0 rgba(255,255,255,0.9);
  transition: all .25s cubic-bezier(.34,1.56,.64,1);
}
.back-to-top:hover {
  background: rgba(255,255,255,0.85);
  transform: translateX(-50%) scale(1.04);
  box-shadow: 0 12px 32px rgba(0,0,0,0.12), inset 0 1px 0 rgba(255,255,255,1);
}
.back-to-top:active { transform: translateX(-50%) scale(0.96); transition-duration: .1s; }
.back-top-enter-active, .back-top-leave-active { transition: opacity .25s, transform .25s cubic-bezier(.4,0,.2,1); }
.back-top-enter-from, .back-top-leave-to { opacity: 0; transform: translateX(-50%) translateY(12px); }

/* ─── 弹窗 ─── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: overlayIn .2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }

.modal {
  background: var(--white);
  border-radius: 20px;
  width: 480px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 24px 60px rgba(0,0,0,0.16), 0 8px 24px rgba(0,0,0,0.08);
  animation: modalIn .25s cubic-bezier(.34,1.56,.64,1);
  border: 1px solid rgba(255,255,255,0.8);
}
.modal-sm { width: 400px; }
.modal-detail { width: 500px; }
@keyframes modalIn { from { opacity: 0; transform: translateY(16px) scale(.96); } to { opacity: 1; transform: none; } }

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.modal-title-wrap { display: flex; align-items: center; gap: 10px; }
.modal-icon-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.modal-icon-badge.primary { background: rgba(200,56,42,0.1); color: var(--accent); }
.modal-icon-badge.danger { background: rgba(200,56,42,0.1); color: var(--accent); }
.modal-icon-badge.warning { background: rgba(212,130,10,0.1); color: var(--warning); }

.modal-title { font-size: 17px; font-weight: 700; letter-spacing: -0.2px; }
.modal-close {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  color: var(--muted);
  font-size: 16px;
  padding: 6px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
}
.modal-close:hover { background: var(--bg); color: var(--text); border-color: var(--border-dark); }
.modal-body { padding: 24px; }

/* 取消弹窗内容 */
.cancel-text { font-size: 16px; font-weight: 600; margin: 0 0 6px; text-align: center; }
.cancel-sub { font-size: 13px; color: var(--muted); margin: 0 0 20px; text-align: center; }

.info-card {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 4px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 16px;
  font-size: 13.5px;
  border-bottom: 1px solid var(--border);
}
.info-row:last-child { border-bottom: none; }
.info-label { color: var(--muted); }
.info-val { font-weight: 500; }
.info-val.mono { font-family: 'Bebas Neue', monospace; letter-spacing: 0.8px; color: var(--muted); }
.info-val.accent { color: var(--accent); font-weight: 600; }

/* 表单 */
.form-field { margin-bottom: 18px; }
.form-label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 7px; font-weight: 500; letter-spacing: 0.2px; }
.required { color: var(--accent); }
.form-input, .form-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  font-family: 'Noto Sans SC', sans-serif;
  background: var(--bg);
  outline: none;
  color: var(--text);
  transition: border-color .15s, box-shadow .15s;
  box-sizing: border-box;
}
.form-input:focus, .form-textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(200,56,42,0.08);
  background: var(--white);
}
.form-textarea { resize: vertical; }
.form-hint { font-size: 11px; color: var(--muted2); margin-top: 5px; }

.modal-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 24px; }

/* 订单详情弹窗内容 */
.detail-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.detail-order-no { font-size: 11px; color: var(--muted2); font-family: 'Bebas Neue', monospace; letter-spacing: 1px; }

.detail-car-section {
  display: flex;
  gap: 16px;
  align-items: center;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 20px;
}
.detail-car-img {
  width: 84px;
  height: 60px;
  background: var(--white);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid var(--border);
}
.detail-car-img img { width: 100%; height: 100%; object-fit: cover; }
.detail-car-name { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.detail-car-plate {
  display: inline-block;
  font-size: 11px;
  color: var(--muted);
  background: var(--white);
  border: 1px solid var(--border);
  padding: 1px 7px;
  border-radius: 4px;
  letter-spacing: 1px;
}

.detail-rows {
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
}
.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 11px 16px;
  font-size: 13.5px;
  border-bottom: 1px solid var(--border);
}
.detail-row:last-child { border-bottom: none; }
.detail-label { color: var(--muted); font-size: 13px; }
.detail-val { font-weight: 500; }
.detail-val.accent { color: var(--accent); }
.detail-row-total { background: var(--surface); }
.detail-total-price {
  font-family: 'Bebas Neue', sans-serif;
  font-size: 20px;
  color: var(--accent);
  letter-spacing: 0.5px;
}

.detail-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-row-flat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 4px 0;
}
.detail-row-flat .detail-label { color: var(--muted); }
.mono-sm { font-family: monospace; font-size: 12px; color: var(--muted); }
</style>