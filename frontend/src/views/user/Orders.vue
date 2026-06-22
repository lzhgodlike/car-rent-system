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

// 获取显示状态（考虑支付状态）
const getDisplayStatus = (order) => {
  if (order.paymentStatus === 'UNPAID' && order.orderStatus !== 'CANCELLED') {
    return { text: '待支付', class: 'os-unpaid' }
  }
  // 检查附加费用支付状态
  if (order.orderStatus === 'COMPLETED' && order.returnOrder?.extraFee > 0 && order.returnOrder?.extraFeePaymentStatus === 'UNPAID') {
    return { text: '待支付附加费', class: 'os-unpaid-extra' }
  }
  return { text: statusMap[order.orderStatus] || order.orderStatus, class: statusClass[order.orderStatus] || '' }
}
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

// 支付弹窗
const payDialogVisible = ref(false)
const payOrder = ref(null)
const payMethod = ref('ALIPAY')
const paySubmitting = ref(false)
const paySuccess = ref(false)

const openPayDialog = (order) => {
  payOrder.value = order
  payMethod.value = 'ALIPAY'
  paySubmitting.value = false
  paySuccess.value = false
  payDialogVisible.value = true
}

const confirmPay = async () => {
  paySubmitting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    await request.post(`/rent-orders/${payOrder.value.id}/pay`, {
      paymentMethod: payMethod.value
    })
    paySuccess.value = true
    setTimeout(() => {
      payDialogVisible.value = false
      paySuccess.value = false
      ElMessage.success('支付成功！')
      loadData(true)
    }, 1200)
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    paySubmitting.value = false
  }
}

// 附加费用支付弹窗
const extraFeePayDialogVisible = ref(false)
const extraFeePayOrder = ref(null)
const extraFeePayMethod = ref('ALIPAY')
const extraFeePaySubmitting = ref(false)
const extraFeePaySuccess = ref(false)

const openExtraFeePayDialog = (order) => {
  extraFeePayOrder.value = order
  extraFeePayMethod.value = 'ALIPAY'
  extraFeePaySubmitting.value = false
  extraFeePaySuccess.value = false
  extraFeePayDialogVisible.value = true
}

const confirmExtraFeePay = async () => {
  extraFeePaySubmitting.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1500))
    await request.post(`/return-orders/${extraFeePayOrder.value.returnOrder.id}/pay-extra-fee`, {
      paymentMethod: extraFeePayMethod.value
    })
    extraFeePaySuccess.value = true
    setTimeout(() => {
      extraFeePayDialogVisible.value = false
      extraFeePaySuccess.value = false
      ElMessage.success('附加费用支付成功！')
      loadData(true)
    }, 1200)
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    extraFeePaySubmitting.value = false
  }
}

const actionConfig = {
  cancel: { label: '取消订单', class: 'btn-sm-danger', handler: (order) => openCancelDialog(order) },
  pay: { label: '去支付', class: 'btn-sm-primary', handler: (order) => openPayDialog(order) },
  pay_extra_fee: { label: '支付附加费', class: 'btn-sm-warning', handler: (order) => openExtraFeePayDialog(order) },
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
            <span class="order-status-badge" :class="getDisplayStatus(order).class">
              <span class="status-dot"></span>
              {{ getDisplayStatus(order).text }}
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
            <span class="order-status-badge lg" :class="getDisplayStatus(detailOrder).class">
              <span class="status-dot"></span>
              {{ getDisplayStatus(detailOrder).text }}
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

    <!-- 支付弹窗 -->
    <div v-if="payDialogVisible" class="modal-overlay" @click.self="paySubmitting ? null : payDialogVisible = false">
      <div class="modal modal-pay">
        <div class="payment-success" v-if="paySuccess">
          <div class="success-icon">
            <svg viewBox="0 0 52 52" class="success-svg">
              <circle cx="26" cy="26" r="25" fill="none" stroke="currentColor" stroke-width="2"/>
              <path fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" d="M14 27l8 8 16-16"/>
            </svg>
          </div>
          <div class="success-text">支付成功</div>
          <div class="success-sub">订单已完成支付</div>
        </div>
        <template v-else>
          <div class="modal-header">
            <div class="modal-title">确认支付</div>
            <button class="modal-close" @click="payDialogVisible = false" :disabled="paySubmitting"><el-icon><Close /></el-icon></button>
          </div>
          <div class="modal-body" v-if="payOrder">
            <div class="pay-order-info">
              <div class="pay-car-name">{{ carName(payOrder) }}</div>
              <div class="pay-car-meta">{{ payOrder.carInfo?.plateNumber }} · {{ payOrder.rentDays }}天</div>
            </div>
            <div class="pay-amount">
              <span>支付金额</span>
              <span class="pay-amount-val">{{ money(totalAmount(payOrder)) }}</span>
            </div>
            <div class="pay-methods">
              <div class="pay-method" :class="{ active: payMethod === 'ALIPAY' }" @click="payMethod = 'ALIPAY'">
                <div class="pay-method-icon alipay-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#1677FF" d="M21.422 14.763c-1.323-.588-2.757-1.269-4.269-2.032a28.5 28.5 0 0 0 1.447-4.436h-4.09V6.613h5.078V5.39h-5.078V2.735h-2.16c-.2 0-.363.063-.363.063s-.05.117-.05.332v2.26H6.85v1.223h4.087v1.682H5.39v1.223h9.196a26 26 0 0 1-1.066 3.365c-2.452-1.076-5.27-2.155-8.026-2.155C1.494 13.526 0 15.486 0 17.37c0 2.233 1.978 3.63 4.418 3.63 3.415 0 6.378-2.529 8.463-5.078 2.541 1.35 5.925 3.066 8.541 3.84V14.763ZM4.418 19.32c-1.48 0-2.755-.757-2.755-1.95 0-1.194 1.276-1.951 2.755-1.951 2.337 0 4.514 1.58 6.007 3.062-1.163.565-2.655.839-4.007.839-1.04 0-1.612-.23-2-.839Z"/></svg>
                </div>
                <span>支付宝</span>
                <div class="pay-method-check">
                  <svg v-if="payMethod === 'ALIPAY'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
              <div class="pay-method" :class="{ active: payMethod === 'WECHAT' }" @click="payMethod = 'WECHAT'">
                <div class="pay-method-icon wechat-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348ZM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18Zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18Zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.11.24-.245 0-.06-.024-.12-.04-.178l-.325-1.233a.492.492 0 0 1 .177-.554C23.026 18.582 24 16.89 24 14.978c0-3.33-2.776-5.998-7.062-6.12ZM14.033 13.3c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Zm4.844 0c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Z"/></svg>
                </div>
                <span>微信支付</span>
                <div class="pay-method-check">
                  <svg v-if="payMethod === 'WECHAT'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
            </div>
            <button class="btn-pay" @click="confirmPay" :disabled="paySubmitting">
              <span v-if="paySubmitting" class="spinner"></span>
              {{ paySubmitting ? '处理中...' : `确认支付 ${money(totalAmount(payOrder))}` }}
            </button>
            <p class="pay-note">模拟支付环境 · 不会产生真实扣款</p>
          </div>
        </template>
      </div>
    </div>

    <!-- 附加费用支付弹窗 -->
    <div v-if="extraFeePayDialogVisible" class="modal-overlay" @click.self="extraFeePaySubmitting ? null : extraFeePayDialogVisible = false">
      <div class="modal modal-pay">
        <div class="payment-success" v-if="extraFeePaySuccess">
          <div class="success-icon">
            <svg viewBox="0 0 52 52" class="success-svg">
              <circle cx="26" cy="26" r="25" fill="none" stroke="currentColor" stroke-width="2"/>
              <path fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" d="M14 27l8 8 16-16"/>
            </svg>
          </div>
          <div class="success-text">支付成功</div>
          <div class="success-sub">附加费用已支付完成</div>
        </div>
        <template v-else>
          <div class="modal-header">
            <div class="modal-title">支付附加费用</div>
            <button class="modal-close" @click="extraFeePayDialogVisible = false" :disabled="extraFeePaySubmitting"><el-icon><Close /></el-icon></button>
          </div>
          <div class="modal-body" v-if="extraFeePayOrder">
            <div class="pay-order-info">
              <div class="pay-car-name">{{ carName(extraFeePayOrder) }}</div>
              <div class="pay-car-meta">{{ extraFeePayOrder.carInfo?.plateNumber }}</div>
            </div>
            <div class="extra-fee-desc" v-if="extraFeePayOrder.returnOrder?.damageDesc">
              <div class="extra-fee-label">损伤说明</div>
              <div class="extra-fee-text">{{ extraFeePayOrder.returnOrder.damageDesc }}</div>
            </div>
            <div class="pay-amount">
              <span>附加费用</span>
              <span class="pay-amount-val">{{ money(extraFeePayOrder.returnOrder?.extraFee) }}</span>
            </div>
            <div class="pay-methods">
              <div class="pay-method" :class="{ active: extraFeePayMethod === 'ALIPAY' }" @click="extraFeePayMethod = 'ALIPAY'">
                <div class="pay-method-icon alipay-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#1677FF" d="M21.422 14.763c-1.323-.588-2.757-1.269-4.269-2.032a28.5 28.5 0 0 0 1.447-4.436h-4.09V6.613h5.078V5.39h-5.078V2.735h-2.16c-.2 0-.363.063-.363.063s-.05.117-.05.332v2.26H6.85v1.223h4.087v1.682H5.39v1.223h9.196a26 26 0 0 1-1.066 3.365c-2.452-1.076-5.27-2.155-8.026-2.155C1.494 13.526 0 15.486 0 17.37c0 2.233 1.978 3.63 4.418 3.63 3.415 0 6.378-2.529 8.463-5.078 2.541 1.35 5.925 3.066 8.541 3.84V14.763ZM4.418 19.32c-1.48 0-2.755-.757-2.755-1.95 0-1.194 1.276-1.951 2.755-1.951 2.337 0 4.514 1.58 6.007 3.062-1.163.565-2.655.839-4.007.839-1.04 0-1.612-.23-2-.839Z"/></svg>
                </div>
                <span>支付宝</span>
                <div class="pay-method-check">
                  <svg v-if="extraFeePayMethod === 'ALIPAY'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
              <div class="pay-method" :class="{ active: extraFeePayMethod === 'WECHAT' }" @click="extraFeePayMethod = 'WECHAT'">
                <div class="pay-method-icon wechat-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348ZM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18Zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18Zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.11.24-.245 0-.06-.024-.12-.04-.178l-.325-1.233a.492.492 0 0 1 .177-.554C23.026 18.582 24 16.89 24 14.978c0-3.33-2.776-5.998-7.062-6.12ZM14.033 13.3c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Zm4.844 0c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Z"/></svg>
                </div>
                <span>微信支付</span>
                <div class="pay-method-check">
                  <svg v-if="extraFeePayMethod === 'WECHAT'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
            </div>
            <button class="btn-pay" @click="confirmExtraFeePay" :disabled="extraFeePaySubmitting">
              <span v-if="extraFeePaySubmitting" class="spinner"></span>
              {{ extraFeePaySubmitting ? '处理中...' : `确认支付 ${money(extraFeePayOrder.returnOrder?.extraFee)}` }}
            </button>
            <p class="pay-note">模拟支付环境 · 不会产生真实扣款</p>
          </div>
        </template>
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
.os-unpaid { background: rgba(234,88,12,0.10); color: #ea580c; }
.os-unpaid .status-dot { background: #ea580c; animation: pulse 2s infinite; }
.os-unpaid-extra { background: rgba(234,88,12,0.10); color: #ea580c; }
.os-unpaid-extra .status-dot { background: #ea580c; animation: pulse 2s infinite; }

.order-card:has(.os-unpaid) .order-side-bar,
.order-card:has(.os-unpaid-extra) .order-side-bar { background: #ea580c; }

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

/* 支付弹窗 */
.modal-pay { width: 420px; }
.pay-order-info {
  background: var(--bg); border-radius: 12px; padding: 16px; margin-bottom: 16px;
}
.pay-car-name { font-size: 15px; font-weight: 600; }
.pay-car-meta { font-size: 13px; color: var(--muted); margin-top: 4px; }
.pay-amount {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; background: var(--accent-light); border-radius: 12px; margin-bottom: 16px;
  font-size: 14px; color: var(--muted);
}
.pay-amount-val { font-family: 'Bebas Neue', monospace; font-size: 26px; color: var(--accent); }
.pay-methods { margin-bottom: 16px; }
.pay-method {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border: 2px solid var(--border); border-radius: 10px;
  cursor: pointer; transition: all .15s; margin-bottom: 8px;
}
.pay-method:hover { border-color: var(--accent); }
.pay-method.active { border-color: var(--accent); background: var(--accent-light); }
.pay-method-icon {
  width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center;
}
.alipay-icon { background: #e8f4ff; }
.wechat-icon { background: #e8f8ee; }
.pay-method-check { width: 20px; height: 20px; }
.pay-method span { flex: 1; font-size: 14px; font-weight: 500; }
.btn-pay {
  width: 100%; padding: 13px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 15px; font-weight: 600;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: background .18s;
}
.btn-pay:hover { background: #b02e22; }
.btn-pay:disabled { opacity: .7; cursor: not-allowed; }
.spinner {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%; animation: spin .6s linear infinite;
}
.pay-note { font-size: 12px; color: var(--muted); text-align: center; margin-top: 12px; }

/* 附加费用说明 */
.extra-fee-desc {
  background: var(--bg); border-radius: 12px; padding: 14px 16px; margin-bottom: 16px;
  border-left: 3px solid var(--warning);
}
.extra-fee-label { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.extra-fee-text { font-size: 14px; color: var(--text); line-height: 1.6; }

/* 支付成功 */
.payment-success { padding: 48px 24px; text-align: center; }
.success-icon { margin-bottom: 20px; }
.success-svg {
  width: 64px; height: 64px; color: #22c55e; animation: successPop .4s ease;
}
.success-svg circle { stroke-dasharray: 157; stroke-dashoffset: 157; animation: circleIn .5s .1s ease forwards; }
.success-svg path { stroke-dasharray: 48; stroke-dashoffset: 48; animation: checkIn .3s .5s ease forwards; }
@keyframes successPop { 0% { transform: scale(0); } 50% { transform: scale(1.1); } 100% { transform: scale(1); } }
@keyframes circleIn { to { stroke-dashoffset: 0; } }
@keyframes checkIn { to { stroke-dashoffset: 0; } }
.success-text { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
.success-sub { font-size: 14px; color: var(--muted); }
</style>