<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { getAuth, useAuth, openLoginModal } from '../../utils/auth'

const router = useRouter()
const route = useRoute()
const { isLoggedIn } = useAuth()

// 收藏
const favoriteIds = ref(new Set())
const loadFavorites = async () => {
  try {
    const list = await request.get('/favorites')
    favoriteIds.value = new Set(list.map(c => c.id))
  } catch {}
}
const toggleFavorite = async (e, car) => {
  e.stopPropagation()
  if (!isLoggedIn.value) { openLoginModal(); return }
  try {
    if (favoriteIds.value.has(car.id)) {
      await request.delete(`/favorites/${car.id}`)
      favoriteIds.value.delete(car.id)
      ElMessage.success('已取消收藏')
    } else {
      await request.post(`/favorites/${car.id}`)
      favoriteIds.value.add(car.id)
      ElMessage.success('已加入收藏')
    }
  } catch {}
}

const cars = ref([])
const carTypes = ref([])
const cities = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const pageNum = ref(1)
const pageSize = 6
const selectedCar = ref(null)
const formatDate = (date) => {
  const current = new Date(date)
  const year = current.getFullYear()
  const month = `${current.getMonth() + 1}`.padStart(2, '0')
  const day = `${current.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}
const getStartOfToday = () => {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  return date
}
const today = formatDate(getStartOfToday())
const tomorrow = (() => {
  const date = new Date(getStartOfToday())
  date.setDate(date.getDate() + 1)
  return formatDate(date)
})()
const rentForm = ref({ rentDate: today, expectedReturnDate: tomorrow, remark: '' })
const activeType = ref('')
const keyword = ref('')
const sort = ref('')
const typeId = ref('')
const city = ref('')
const toastMsg = ref('')
const toastVisible = ref(false)
const profileTipVisible = ref(false)
let toastTimer = null
let scrollTimer = null

// 支付相关
const paymentVisible = ref(false)
const paymentMethod = ref('ALIPAY')
const paying = ref(false)
const paymentSuccess = ref(false)
const currentOrderId = ref(null)

// 详情弹窗 & 图片放大
const detailVisible = ref(false)
const detailCar = ref(null)
const imageZoomVisible = ref(false)
const activeImageIndex = ref(0)

const carImages = computed(() => {
  const images = detailCar.value?.carImages || []
  const normalizedImages = images
    .map(image => image?.imageUrl || image)
    .filter(Boolean)
  if (normalizedImages.length) return normalizedImages
  return detailCar.value?.carImage ? [detailCar.value.carImage] : []
})

const activeImage = computed(() => carImages.value[activeImageIndex.value] || '')
const hasMultipleImages = computed(() => carImages.value.length > 1)

const loadTypes = async () => { carTypes.value = await request.get('/car-types') }
const loadCities = async () => { cities.value = await request.get('/cars/cities') }
const disableRentDate = (date) => {
  const current = new Date(date)
  current.setHours(0, 0, 0, 0)
  return current < getStartOfToday()
}
const disableReturnDate = (date) => {
  const current = new Date(date)
  current.setHours(0, 0, 0, 0)
  const minDate = new Date(`${rentForm.value.rentDate || today}T00:00:00`)
  return current <= minDate
}
const nextDate = (date) => {
  const nextDay = new Date(`${date}T00:00:00`)
  nextDay.setDate(nextDay.getDate() + 1)
  return formatDate(nextDay)
}
const initFromQuery = () => {
  const queryTypeId = route.query.typeId ? Number(route.query.typeId) : ''
  typeId.value = Number.isNaN(queryTypeId) ? '' : queryTypeId
  activeType.value = carTypes.value.find(t => t.id === typeId.value)?.typeName || ''
  city.value = route.query.city || ''

  const pickDate = typeof route.query.pickDate === 'string' ? route.query.pickDate : ''
  const dropDate = typeof route.query.dropDate === 'string' ? route.query.dropDate : ''
  rentForm.value.rentDate = pickDate && pickDate >= today ? pickDate : today
  rentForm.value.expectedReturnDate = dropDate && dropDate > rentForm.value.rentDate ? dropDate : nextDate(rentForm.value.rentDate)
}

const loadData = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
    cars.value = []
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const page = await request.get('/cars', {
      params: {
        status: 'AVAILABLE',
        typeId: typeId.value || undefined,
        city: city.value || undefined,
        sort: sort.value || undefined,
        keyword: keyword.value || undefined,
        pageNum: pageNum.value,
        pageSize,
      }
    })
    if (reset) {
      cars.value = page.records
    } else {
      cars.value = [...cars.value, ...page.records]
    }
    hasMore.value = cars.value.length < page.total
    if (hasMore.value) pageNum.value++
  } finally { loading.value = false }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    await loadData()
  } finally { loadingMore.value = false }
}

// 监听筛选/排序/搜索变化，重新加载
watch([typeId, city, sort], () => loadData(true))
watch(() => rentForm.value.rentDate, (rentDate) => {
  if (!rentDate) return
  if (!rentForm.value.expectedReturnDate || rentForm.value.expectedReturnDate <= rentDate) {
    rentForm.value.expectedReturnDate = nextDate(rentDate)
  }
})

let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadData(true), 300)
})

// 滚动检测
const showBackToTop = ref(false)

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

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

onMounted(async () => {
  await Promise.all([loadTypes(), loadCities()])
  initFromQuery()
  if (isLoggedIn.value) loadFavorites()
  window.addEventListener('scroll', handleScroll, { passive: true })
  // 从订单页/首页/收藏跳转过来，自动选中车辆
  const carId = route.query.carId
  if (carId) {
    try {
      const car = await request.get(`/cars/${carId}`)
      if (car && car.status === 'AVAILABLE') {
        await loadData()
        selectCar(car)
      } else {
        await loadData()
        ElMessage.warning('该车辆当前不可用，请选择其他车辆')
      }
    } catch {
      await loadData()
      ElMessage.warning('该车辆不存在或已下架')
    }
    const { carId: _, ...restQuery } = route.query
    router.replace({ path: '/book', query: restQuery })
  } else {
    await loadData()
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  clearTimeout(scrollTimer)
  clearTimeout(searchTimer)
})

const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'

const showToast = (msg) => {
  toastMsg.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2500)
}

const selectCar = (car) => {
  selectedCar.value = car
  showToast(`已选择 ${car.brand} ${car.model}`)
}

const openDetail = async (e, car) => {
  e.stopPropagation()
  detailCar.value = await request.get(`/cars/${car.id}`)
  activeImageIndex.value = 0
  detailVisible.value = true
}

const openImageZoom = () => {
  if (activeImage.value) imageZoomVisible.value = true
}

const selectImage = (index) => {
  activeImageIndex.value = index
}

const prevImage = () => {
  if (!hasMultipleImages.value) return
  activeImageIndex.value = (activeImageIndex.value - 1 + carImages.value.length) % carImages.value.length
}

const nextImage = () => {
  if (!hasMultipleImages.value) return
  activeImageIndex.value = (activeImageIndex.value + 1) % carImages.value.length
}

const days = computed(() => {
  if (!rentForm.value.rentDate || !rentForm.value.expectedReturnDate) return 0
  return Math.max(1, Math.ceil((new Date(rentForm.value.expectedReturnDate) - new Date(rentForm.value.rentDate)) / 86400000))
})
const totalPrice = computed(() => selectedCar.value ? selectedCar.value.dayPrice * days.value : 0)
const isFilled = (value) => String(value ?? '').trim().length > 0
const isProfileVerified = (profile) => isFilled(profile?.realName) && isFilled(profile?.phone) && isFilled(profile?.idCard)
const goToProfileVerification = () => {
  profileTipVisible.value = false
  router.push({ path: '/my-profile', query: { openEdit: '1' } })
}

const submitRent = async () => {
  if (!selectedCar.value) { ElMessage.warning('请先选择车辆'); return }
  if (!rentForm.value.rentDate || !rentForm.value.expectedReturnDate) { ElMessage.warning('请选择日期'); return }

  if (!getAuth()) {
    ElMessage.warning('请先登录')
    openLoginModal()
    return
  }

  try {
    const profile = await request.get('/users/profile')
    if (!isProfileVerified(profile)) {
      profileTipVisible.value = true
      return
    }
  } catch { return }

  // 先创建订单
  try {
    const orderId = await request.post('/rent-orders', {
      carId: selectedCar.value.id,
      ...rentForm.value
    })
    currentOrderId.value = orderId

    // 打开支付弹窗
    paymentMethod.value = 'ALIPAY'
    paying.value = false
    paymentSuccess.value = false
    paymentVisible.value = true
  } catch (e) {
    ElMessage.error('创建订单失败，请重试')
  }
}

const confirmPayment = async () => {
  paying.value = true
  try {
    // 模拟支付延迟
    await new Promise(resolve => setTimeout(resolve, 1500))

    await request.post(`/rent-orders/${currentOrderId.value}/pay`, {
      paymentMethod: paymentMethod.value
    })

    paymentSuccess.value = true
    setTimeout(() => {
      paymentVisible.value = false
      paymentSuccess.value = false
      ElMessage.success('支付成功，预订完成！')
      selectedCar.value = null
      currentOrderId.value = null
      loadData(true)
    }, 1200)
  } catch {
    ElMessage.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="book-page">
    <div class="book-layout">
      <!-- Car list -->
      <div class="book-main">
        <div class="book-main-header">
          <h2 class="page-title">选择车辆</h2>
          <div class="title-actions">
            <input class="search-input title-search-input" v-model="keyword" placeholder="搜索品牌、型号…" />
          </div>
        </div>
        <div class="filter-bar">
          <div class="filter-chips">
            <div class="filter-chip" :class="{ active: !activeType }" @click="activeType = ''; typeId = ''">全部</div>
            <div v-for="t in carTypes" :key="t.id" class="filter-chip" :class="{ active: activeType === t.typeName }" @click="activeType = t.typeName; typeId = t.id">{{ t.typeName }}</div>
          </div>
          <div class="filter-right">
            <select class="sort-sel city-sel" v-model="city">
              <option value="">全部城市</option>
              <option v-for="item in cities" :key="item" :value="item">{{ item }}</option>
            </select>
            <select class="sort-sel" v-model="sort">
              <option value="">默认排序</option>
              <option value="rentCount">近期热租</option>
              <option value="asc">价格从低到高</option>
              <option value="desc">价格从高到低</option>
            </select>
          </div>
        </div>

        <div class="car-grid">
          <div v-for="car in cars" :key="car.id" class="car-card" :class="{ selected: selectedCar?.id === car.id }" @click="selectCar(car)">
            <div class="car-img">
              <div class="car-img-placeholder"><el-icon size="32" style="color:var(--muted2);"><Van /></el-icon></div>
              <img v-if="car.carImage" :src="car.carImage" :alt="`${car.brand} ${car.model}`" loading="lazy" class="car-img-el" @load="(e) => e.target.classList.add('loaded')" @error="(e) => e.target.style.display='none'" />
              <div class="car-tag">{{ carTypeName(car.typeId) }}</div>
              <div class="car-fav" :class="{ liked: favoriteIds.has(car.id) }" @click="toggleFavorite($event, car)">
                <el-icon><component :is="favoriteIds.has(car.id) ? StarFilled : Star" /></el-icon>
              </div>
            </div>
            <div class="car-body">
              <div class="car-name">{{ car.brand }} {{ car.model }}</div>
              <div class="car-meta">{{ car.plateNumber }} · {{ car.mileage }}km</div>
              <div class="car-features">
                <div class="car-feat"><el-icon><User /></el-icon> 5座</div>
                <div class="car-feat"><el-icon><OfficeBuilding /></el-icon> {{ carTypeName(car.typeId) }}</div>
              </div>
              <div class="car-location"><el-icon><Location /></el-icon><span>{{ car.pickupAddress || '待补充' }}</span></div>
            </div>
            <div class="car-footer">
              <span class="car-price">¥{{ car.dayPrice }}<small>/天</small></span>
              <button class="btn-detail" @click="openDetail($event, car)">查看详情</button>
            </div>
          </div>
        </div>

        <div v-if="loading && cars.length === 0" class="loading-state">加载中…</div>
        <div v-if="loadingMore" class="loading-state">加载更多…</div>
        <div v-if="!hasMore && cars.length > 0" class="loading-state">没有更多了</div>
        <div v-if="!loading && cars.length === 0" class="empty-state">暂无符合条件的车辆</div>
      </div>

      <!-- Summary -->
      <div>
        <div class="book-summary">
          <div class="summary-title">预订信息</div>
          <div class="summary-field">
            <label>已选车辆</label>
            <input :value="selectedCar ? `${selectedCar.brand} ${selectedCar.model}  ·  ¥${selectedCar.dayPrice}/天` : '请在左侧选择车辆'" readonly :class="{ 'has-value': selectedCar }" />
          </div>
          <div class="summary-field">
            <label>取车地点</label>
            <input :value="selectedCar ? (selectedCar.pickupAddress || '待补充') : '-'" readonly :class="{ 'has-value': selectedCar }" />
          </div>
          <div class="summary-field">
            <label>取车 / 还车日期</label>
            <div class="date-row">
              <el-date-picker v-model="rentForm.rentDate" type="date" value-format="YYYY-MM-DD" format="YYYY/MM/DD" placeholder="取车日期" style="width:100%" :disabled-date="disableRentDate" :editable="false" />
              <el-date-picker v-model="rentForm.expectedReturnDate" type="date" value-format="YYYY-MM-DD" format="YYYY/MM/DD" placeholder="还车日期" style="width:100%" :disabled-date="disableReturnDate" :editable="false" />
            </div>
          </div>
          <div class="summary-field">
            <label>备注</label>
            <textarea v-model="rentForm.remark" rows="3" placeholder="如有特殊需求请备注"></textarea>
          </div>
          <hr class="summary-divider" />
          <div class="summary-row">
            <span class="label">车辆租金</span>
            <span class="val font-mono">{{ selectedCar && days ? `¥${selectedCar.dayPrice} × ${days}天` : '--' }}</span>
          </div>
          <hr class="summary-divider" />
          <div class="summary-total">
            <span>应付合计</span>
            <span class="price font-mono">{{ days ? `¥ ${totalPrice.toLocaleString()}` : '¥ --' }}</span>
          </div>
          <button class="btn-book" @click="submitRent" :disabled="!selectedCar">
            <el-icon><Check /></el-icon> 确认预订
          </button>
          <p class="book-note">取车前12小时免费取消 · 无隐藏费用</p>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition name="toast">
      <div v-if="toastVisible" class="toast">{{ toastMsg }}</div>
    </Transition>

    <!-- Profile tip dialog -->
    <div v-if="profileTipVisible" class="tip-overlay" @click.self="profileTipVisible = false">
      <div class="tip-modal">
        <div class="tip-header">
          <div class="tip-title">请先完善个人信息</div>
          <button class="tip-close" @click="profileTipVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="tip-body" style="text-align:center;">
          <div style="font-size:48px;margin-bottom:16px;">📋</div>
          <p style="font-size:14px;color:var(--muted);line-height:1.8;">
            租车前需要完善<strong style="color:var(--text);">姓名、手机号和身份证号</strong>信息。<br>
            请前往个人中心完成填写后再试。
          </p>
          <button class="tip-btn" style="margin-top:20px;" @click="goToProfileVerification">
            去完善信息
          </button>
        </div>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <div v-if="paymentVisible" class="payment-overlay" @click.self="paying ? null : paymentVisible = false">
      <div class="payment-modal">
        <!-- 支付成功状态 -->
        <div v-if="paymentSuccess" class="payment-success">
          <div class="success-icon">
            <svg viewBox="0 0 52 52" class="success-svg">
              <circle cx="26" cy="26" r="25" fill="none" stroke="currentColor" stroke-width="2"/>
              <path fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" d="M14 27l8 8 16-16"/>
            </svg>
          </div>
          <div class="success-text">支付成功</div>
          <div class="success-sub">订单已创建，等待确认取车</div>
        </div>

        <!-- 支付表单 -->
        <template v-else>
          <div class="payment-header">
            <div class="payment-title">确认支付</div>
            <button class="payment-close" @click="paymentVisible = false" :disabled="paying">
              <el-icon><Close /></el-icon>
            </button>
          </div>
          <div class="payment-body">
            <!-- 订单信息 -->
            <div class="payment-order-info">
              <div class="payment-car-name">{{ selectedCar?.brand }} {{ selectedCar?.model }}</div>
              <div class="payment-car-meta">{{ selectedCar?.plateNumber }} · {{ carTypeName(selectedCar?.typeId) }}</div>
              <div class="payment-date-range">
                {{ rentForm.rentDate }} 至 {{ rentForm.expectedReturnDate }} · {{ days }}天
              </div>
            </div>

            <!-- 支付金额 -->
            <div class="payment-amount">
              <span class="payment-amount-label">支付金额</span>
              <span class="payment-amount-value">¥{{ totalPrice.toLocaleString() }}</span>
            </div>

            <!-- 支付方式 -->
            <div class="payment-methods">
              <div class="payment-methods-title">选择支付方式</div>
              <div
                class="payment-method"
                :class="{ active: paymentMethod === 'ALIPAY' }"
                @click="paymentMethod = 'ALIPAY'"
              >
                <div class="payment-method-icon alipay-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#1677FF" d="M21.422 14.763c-1.323-.588-2.757-1.269-4.269-2.032a28.5 28.5 0 0 0 1.447-4.436h-4.09V6.613h5.078V5.39h-5.078V2.735h-2.16c-.2 0-.363.063-.363.063s-.05.117-.05.332v2.26H6.85v1.223h4.087v1.682H5.39v1.223h9.196a26 26 0 0 1-1.066 3.365c-2.452-1.076-5.27-2.155-8.026-2.155C1.494 13.526 0 15.486 0 17.37c0 2.233 1.978 3.63 4.418 3.63 3.415 0 6.378-2.529 8.463-5.078 2.541 1.35 5.925 3.066 8.541 3.84V14.763ZM4.418 19.32c-1.48 0-2.755-.757-2.755-1.95 0-1.194 1.276-1.951 2.755-1.951 2.337 0 4.514 1.58 6.007 3.062-1.163.565-2.655.839-4.007.839-1.04 0-1.612-.23-2-.839Z"/></svg>
                </div>
                <span>支付宝</span>
                <div class="payment-method-check">
                  <svg v-if="paymentMethod === 'ALIPAY'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
              <div
                class="payment-method"
                :class="{ active: paymentMethod === 'WECHAT' }"
                @click="paymentMethod = 'WECHAT'"
              >
                <div class="payment-method-icon wechat-icon">
                  <svg viewBox="0 0 24 24" width="24" height="24"><path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348ZM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18Zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18Zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.045c.134 0 .24-.11.24-.245 0-.06-.024-.12-.04-.178l-.325-1.233a.492.492 0 0 1 .177-.554C23.026 18.582 24 16.89 24 14.978c0-3.33-2.776-5.998-7.062-6.12ZM14.033 13.3c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Zm4.844 0c.535 0 .969.44.969.983a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.983.97-.983Z"/></svg>
                </div>
                <span>微信支付</span>
                <div class="payment-method-check">
                  <svg v-if="paymentMethod === 'WECHAT'" viewBox="0 0 24 24" width="20" height="20"><path fill="var(--accent)" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"/></svg>
                </div>
              </div>
            </div>

            <!-- 支付按钮 -->
            <button
              class="btn-pay"
              @click="confirmPayment"
              :disabled="paying"
            >
              <span v-if="paying" class="paying-spinner"></span>
              {{ paying ? '支付处理中...' : `确认支付 ¥${totalPrice.toLocaleString()}` }}
            </button>

            <p class="payment-note">
              <el-icon><Lock /></el-icon> 模拟支付环境 · 不会产生真实扣款
            </p>
          </div>
        </template>
      </div>
    </div>

    <!-- 车辆详情弹窗 -->
    <div v-if="detailVisible" class="detail-overlay" @click.self="detailVisible = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">{{ detailCar?.brand }} {{ detailCar?.model }}</div>
          <button class="detail-close" @click="detailVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body" v-if="detailCar">
          <div class="detail-gallery-block">
            <div class="detail-img-wrap" @click="openImageZoom">
              <button v-if="hasMultipleImages" class="gallery-arrow gallery-arrow-left" @click.stop="prevImage"><el-icon><ArrowLeft /></el-icon></button>
              <img v-if="activeImage" :src="activeImage" class="detail-img" @error="(e) => e.target.style.display='none'" />
              <div v-else class="detail-img-placeholder"><el-icon size="48" style="color:var(--muted2);"><Van /></el-icon></div>
              <button v-if="hasMultipleImages" class="gallery-arrow gallery-arrow-right" @click.stop="nextImage"><el-icon><ArrowRight /></el-icon></button>
              <div class="detail-img-zoom-hint"><el-icon><ZoomIn /></el-icon> 点击查看大图</div>
            </div>
          </div>
          <div v-if="carImages.length > 1" class="detail-thumbs">
            <button
              v-for="(image, index) in carImages"
              :key="`${image}-${index}`"
              class="detail-thumb"
              :class="{ active: index === activeImageIndex }"
              @click.stop="selectImage(index)"
            >
              <img :src="image" />
            </button>
          </div>
          <div class="detail-stats">
            <div class="detail-stat"><el-icon size="18" style="color:var(--accent);"><User /></el-icon><span>5座</span></div>
            <div class="detail-stat"><el-icon size="18" style="color:var(--accent);"><OfficeBuilding /></el-icon><span>{{ carTypeName(detailCar.typeId) }}</span></div>
            <div class="detail-stat"><el-icon size="18" style="color:var(--accent);"><Location /></el-icon><span>{{ detailCar.pickupAddress || '待补充' }}</span></div>
          </div>
          <div class="detail-price-box">
            <div>
              <div style="font-size:12px;color:var(--muted);">每日租金</div>
              <div class="detail-price">¥{{ detailCar.dayPrice }}/天</div>
            </div>
            <div style="font-size:12px;color:var(--muted);text-align:right;">里程 {{ detailCar.mileage }}km<br>{{ detailCar.plateNumber }}</div>
          </div>
          <div class="detail-rows">
            <div class="detail-row"><span class="detail-label">车牌号</span><span class="detail-val">{{ detailCar.plateNumber }}</span></div>
            <div class="detail-row"><span class="detail-label">品牌型号</span><span class="detail-val">{{ detailCar.brand }} {{ detailCar.model }}</span></div>
            <div class="detail-row"><span class="detail-label">车型分类</span><span class="detail-val">{{ carTypeName(detailCar.typeId) }}</span></div>
            <div class="detail-row"><span class="detail-label">总里程数</span><span class="detail-val">{{ detailCar.mileage?.toLocaleString() }} km</span></div>
            <div class="detail-row"><span class="detail-label">取车地址</span><span class="detail-val">{{ detailCar.pickupAddress || '待补充' }}</span></div>
          </div>
          <button class="detail-book-btn" @click="detailVisible = false; selectCar(detailCar)">
            <el-icon><Check /></el-icon> 选择该车辆
          </button>
        </div>
      </div>
    </div>

    <!-- 返回顶部 -->
    <Transition name="back-top">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
        <el-icon size="15"><ArrowUp /></el-icon>
        <span>返回顶部</span>
      </button>
    </Transition>

    <!-- 图片放大 -->
    <div v-if="imageZoomVisible" class="zoom-overlay" @click="imageZoomVisible = false">
      <div class="zoom-panel" @click.stop>
        <div class="zoom-main">
          <button class="gallery-arrow zoom-arrow zoom-close" @click="imageZoomVisible = false"><el-icon><Close /></el-icon></button>
          <button v-if="hasMultipleImages" class="gallery-arrow gallery-arrow-left zoom-arrow" @click.stop="prevImage"><el-icon><ArrowLeft /></el-icon></button>
          <img :src="activeImage" class="zoom-img" />
          <button v-if="hasMultipleImages" class="gallery-arrow gallery-arrow-right zoom-arrow" @click.stop="nextImage"><el-icon><ArrowRight /></el-icon></button>
        </div>
        <div v-if="carImages.length > 1" class="zoom-thumbs">
          <button
            v-for="(image, index) in carImages"
            :key="`zoom-${image}-${index}`"
            class="detail-thumb zoom-thumb"
            :class="{ active: index === activeImageIndex }"
            @click.stop="selectImage(index)"
          >
            <img :src="image" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.book-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; }
.book-layout { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
.book-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.page-title { font-size: 24px; font-weight: 700; margin: 0; }
.title-actions {
  margin-left: auto;
}
.search-input.title-search-input {
  width: 220px;
  background: #fff;
  border: 2px solid #d1d5db;
  box-sizing: border-box;
}
.search-input.title-search-input:hover {
  border-color: var(--accent);
}
.search-input.title-search-input:focus {
  background: #fff;
  border-color: var(--accent);
  box-shadow: none;
}

.filter-bar {
  display: flex; gap: 10px; align-items: center;
  background: var(--white); border: 1px solid var(--border);
  border-radius: 12px; padding: 12px 16px; margin-bottom: 20px; flex-wrap: wrap;
}
.filter-chips { display: flex; gap: 6px; flex-wrap: wrap; flex: 1; }
.filter-chip {
  padding: 6px 14px; border-radius: 20px; font-size: 13px;
  border: 1px solid var(--border); background: var(--bg);
  cursor: pointer; transition: all .15s; color: var(--muted);
}
.filter-chip.active { background: var(--accent-light); border-color: var(--accent); color: var(--accent); font-weight: 500; }
.filter-right { display: flex; gap: 8px; align-items: center; margin-left: auto; flex-wrap: wrap; justify-content: flex-end; }
.search-input {
  padding: 6px 12px; border-radius: 20px; font-size: 13px;
  border: 1px solid var(--border); background: var(--bg);
  color: var(--text); font-family: 'Noto Sans SC', sans-serif; outline: none;
  width: 160px; transition: border-color .15s;
}
.search-input:focus { border-color: var(--accent); }
.sort-sel {
  padding: 6px 12px; border-radius: 20px; font-size: 13px;
  border: 1px solid var(--border); background: var(--bg);
  color: var(--text); font-family: 'Noto Sans SC', sans-serif; outline: none;
}
.city-sel {
  min-width: 132px;
}

.car-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.car-card {
  background: var(--white); border: 2px solid var(--border); border-radius: var(--radius);
  overflow: hidden; cursor: pointer; transition: all .22s; box-shadow: var(--shadow-sm);
}
.car-card:hover { border-color: var(--accent); transform: translateY(-4px); box-shadow: var(--shadow-md); }
.car-card.selected { border-color: var(--accent); background: var(--accent-light); }
.car-img {
  height: 160px; background: var(--bg); display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.car-img-placeholder {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; z-index: 0;
}
.car-img-el {
  width: 100%; height: 100%; object-fit: cover; display: block; position: relative; z-index: 1;
  opacity: 0; transition: opacity .3s, transform .3s;
}
.car-img-el.loaded { opacity: 1; }
.car-card:hover .car-img-el.loaded { transform: scale(1.05); }
.car-tag {
  position: absolute; top: 12px; left: 12px; z-index: 2;
  background: var(--white); border-radius: 6px; padding: 3px 9px;
  font-size: 11px; font-weight: 500; box-shadow: var(--shadow-sm);
}
.car-fav {
  position: absolute; top: 12px; right: 12px; z-index: 2;
  width: 30px; height: 30px; background: var(--white); border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm); cursor: pointer; transition: all .18s;
  font-size: 15px; color: var(--muted);
}
.car-fav:hover { color: var(--accent); }
.car-fav.liked {
  color: var(--accent);
  background: #fff1f0;
}
.car-fav.liked :deep(svg) {
  fill: currentColor;
}
.car-body { padding: 16px 18px; }
.car-name { font-size: 16px; font-weight: 500; }
.car-meta { font-size: 12px; color: var(--muted); margin-top: 3px; }
.car-features { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.car-feat { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--muted); background: var(--bg); padding: 3px 9px; border-radius: 20px; }
.car-feat .el-icon { font-size: 12px; }
.car-location {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}
.car-location .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}
.car-location span {
  min-width: 0;
  word-break: break-all;
}
.car-footer { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-top: 1px solid var(--border); }
.car-price { font-family: 'Bebas Neue', monospace; font-size: 22px; font-weight: 500; color: var(--accent); }
.car-price small { font-size: 12px; color: var(--muted); }
.btn-detail {
  padding: 6px 14px; border-radius: 8px; font-size: 12px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .15s;
  background: none; border: 1px solid var(--border-dark); color: var(--text);
}
.btn-detail:hover { border-color: var(--accent); color: var(--accent); }

.loading-state, .empty-state {
  text-align: center; padding: 32px; color: var(--muted); font-size: 13px;
}

/* Summary */
.book-summary {
  background: var(--white); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 22px;
  position: sticky; top: calc(64px + 20px); box-shadow: var(--shadow-sm);
}
.summary-title { font-weight: 500; font-size: 15px; margin-bottom: 16px; }
.summary-field { margin-bottom: 14px; }
.summary-field label {
  display: block; font-size: 11px; color: var(--muted);
  text-transform: uppercase; letter-spacing: .6px; margin-bottom: 6px;
}
.summary-field input, .summary-field select, .summary-field textarea {
  width: 100%; padding: 9px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); background: var(--bg);
  font-size: 14px; color: var(--text); font-family: 'Noto Sans SC', sans-serif;
  outline: none; transition: border-color .15s; box-sizing: border-box;
}
.summary-field input:focus, .summary-field select:focus, .summary-field textarea:focus { border-color: var(--accent); }
.summary-field textarea { resize: vertical; }
.summary-field input.has-value { color: var(--text); }
.summary-field :deep(.el-date-editor) { width: 100%; }
.summary-field :deep(.el-date-editor .el-input__wrapper) {
  background: var(--bg); border: 1px solid var(--border); border-radius: var(--radius-sm);
  box-shadow: none !important; padding: 7px 12px;
}
.summary-field :deep(.el-date-editor .el-input__wrapper:focus-within) { border-color: var(--accent); }
.summary-field :deep(.el-date-editor input) { font-size: 14px; }
.date-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.summary-divider { border: none; border-top: 1px solid var(--border); margin: 16px 0; }
.summary-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px; }
.summary-row .label { color: var(--muted); }
.summary-row .val { font-weight: 500; }
.summary-total { display: flex; justify-content: space-between; font-size: 16px; font-weight: 500; margin-top: 10px; }
.summary-total .price { font-family: 'Bebas Neue', monospace; font-size: 20px; color: var(--accent); }
.btn-book {
  width: 100%; padding: 13px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 15px; font-weight: 500;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  margin-top: 16px; display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: background .18s; box-shadow: 0 4px 16px rgba(200,56,42,0.25);
}
.btn-book:hover { background: #b02e22; }
.btn-book:disabled { opacity: .5; cursor: not-allowed; }
.book-note { font-size: 11px; color: var(--muted); text-align: center; margin-top: 10px; }

/* Toast */
.toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%);
  background: #1a1a1a; color: #fff; padding: 12px 24px; border-radius: 32px;
  font-size: 14px; z-index: 999; display: flex; align-items: center; gap: 8px;
  box-shadow: var(--shadow-lg); pointer-events: none;
}
.toast-enter-active { animation: toastIn .25s ease; }
.toast-leave-active { animation: toastIn .2s ease reverse; }
@keyframes toastIn { from { opacity: 0; transform: translateX(-50%) translateY(10px); } to { opacity: 1; transform: translateX(-50%) translateY(0); } }

/* 个人信息提示弹窗 */
.tip-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.tip-modal {
  background: var(--white); border-radius: 20px;
  width: 420px; box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(.97); } to { opacity: 1; transform: none; } }
.tip-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.tip-title { font-weight: 700; font-size: 18px; }
.tip-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; }
.tip-body { padding: 24px; }
.tip-btn {
  width: 100%; padding: 13px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 15px; font-weight: 500;
  cursor: pointer; box-shadow: 0 4px 16px rgba(200,56,42,0.25); transition: background .18s;
}
.tip-btn:hover { background: #b02e22; }

/* 车辆详情弹窗 */
.detail-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
.detail-modal {
  background: var(--white); border-radius: 20px;
  width: 480px; max-height: 85vh; overflow-y: auto;
  box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
}
.detail-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.detail-title { font-size: 18px; font-weight: 700; }
.detail-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; padding: 4px; transition: color .15s; }
.detail-close:hover { color: var(--text); }
.detail-body { padding: 24px; }
.detail-gallery-block { margin-bottom: 20px; }
.detail-img-wrap {
  position: relative; border-radius: 12px; overflow: hidden;
  background: var(--bg); cursor: pointer; margin-bottom: 12px;
}
.detail-img { width: 100%; height: 200px; object-fit: cover; display: block; }
.detail-img-placeholder { height: 200px; display: flex; align-items: center; justify-content: center; }
.detail-thumbs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.detail-thumb {
  width: 72px;
  height: 52px;
  padding: 0;
  border: 2px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--white);
  cursor: pointer;
}
.detail-thumb.active {
  border-color: var(--accent);
}
.detail-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.gallery-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.58);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
}
.gallery-arrow-left { left: 12px; }
.gallery-arrow-right { right: 12px; }
.detail-img-zoom-hint {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(0,0,0,0.5); color: #fff; font-size: 12px;
  padding: 6px; text-align: center; display: flex; align-items: center; justify-content: center; gap: 4px;
  opacity: 0; transition: opacity .2s;
}
.detail-img-wrap:hover .detail-img-zoom-hint { opacity: 1; }
.detail-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
.detail-stat {
  text-align: center; padding: 12px; background: var(--bg); border-radius: 10px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
}
.detail-stat span { font-size: 12px; color: var(--muted); }
.detail-price-box {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px; background: var(--accent-light); border-radius: 10px;
}
.detail-price { font-family: 'Bebas Neue', sans-serif; font-size: 24px; font-weight: 500; color: var(--accent); }
.detail-rows { border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-top: 20px; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; font-size: 14px; border-bottom: 1px solid var(--border); }
.detail-row:last-child { border-bottom: none; }
.detail-label { color: var(--muted); font-size: 13px; }
.detail-val { font-weight: 500; }

/* 详情预订按钮 */
.detail-book-btn {
  width: 100%; margin-top: 16px; padding: 14px 24px;
  background: var(--accent); color: #fff; border: none; border-radius: 14px;
  font-size: 15px; font-weight: 600; font-family: 'Noto Sans SC', sans-serif;
  cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px;
  box-shadow: 0 6px 20px rgba(200,56,42,0.3); transition: all .2s;
}
.detail-book-btn:hover { background: #b02e22; transform: translateY(-1px); box-shadow: 0 8px 28px rgba(200,56,42,0.4); }
.detail-book-btn:active { transform: translateY(0); box-shadow: 0 4px 12px rgba(200,56,42,0.25); }

/* 返回顶部 - 液态玻璃效果 */
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
  box-shadow:
    0 8px 32px rgba(0,0,0,0.1),
    0 2px 8px rgba(0,0,0,0.04),
    inset 0 1px 0 rgba(255,255,255,0.8),
    inset 0 -1px 0 rgba(255,255,255,0.2);
  transition: all .3s cubic-bezier(.34,1.56,.64,1);
}
.back-to-top::before {
  content: '';
  position: absolute; inset: 0; border-radius: inherit;
  background: linear-gradient(135deg, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 50%, rgba(255,255,255,0.1) 100%);
  pointer-events: none;
}
.back-to-top:hover {
  background: rgba(255,255,255,0.55);
  transform: translateX(-50%) scale(1.05);
  box-shadow:
    0 12px 40px rgba(0,0,0,0.14),
    0 4px 12px rgba(0,0,0,0.06),
    inset 0 1px 0 rgba(255,255,255,0.9);
}
.back-to-top:active {
  transform: translateX(-50%) scale(0.95);
  background: rgba(255,255,255,0.45);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transition-duration: .1s;
}

.back-top-enter-active, .back-top-leave-active { transition: opacity .3s, transform .3s cubic-bezier(.4,0,.2,1); }
.back-top-enter-from, .back-top-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }

/* 支付弹窗 */
.payment-overlay {
  position: fixed; inset: 0; z-index: 250;
  background: rgba(0,0,0,0.55); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
.payment-modal {
  background: var(--white); border-radius: 20px;
  width: 420px; box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
  overflow: hidden;
}
.payment-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.payment-title { font-weight: 700; font-size: 18px; }
.payment-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; padding: 4px; transition: color .15s; }
.payment-close:hover { color: var(--text); }
.payment-close:disabled { opacity: .5; cursor: not-allowed; }
.payment-body { padding: 24px; }

.payment-order-info {
  background: var(--bg); border-radius: 12px; padding: 16px; margin-bottom: 20px;
}
.payment-car-name { font-size: 16px; font-weight: 600; }
.payment-car-meta { font-size: 13px; color: var(--muted); margin-top: 4px; }
.payment-date-range { font-size: 13px; color: var(--muted); margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--border); }

.payment-amount {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px; background: var(--accent-light); border-radius: 12px; margin-bottom: 20px;
}
.payment-amount-label { font-size: 14px; color: var(--muted); }
.payment-amount-value { font-family: 'Bebas Neue', monospace; font-size: 28px; font-weight: 500; color: var(--accent); }

.payment-methods { margin-bottom: 20px; }
.payment-methods-title { font-size: 13px; color: var(--muted); margin-bottom: 10px; }
.payment-method {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border: 2px solid var(--border); border-radius: 12px;
  cursor: pointer; transition: all .15s; margin-bottom: 8px;
}
.payment-method:hover { border-color: var(--accent); }
.payment-method.active { border-color: var(--accent); background: var(--accent-light); }
.payment-method-icon { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.alipay-icon { background: #e8f4ff; }
.wechat-icon { background: #e8f8ee; }
.payment-method span { flex: 1; font-size: 14px; font-weight: 500; }
.payment-method-check { width: 20px; height: 20px; }

.btn-pay {
  width: 100%; padding: 14px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 16px; font-weight: 600;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: background .18s; box-shadow: 0 4px 16px rgba(200,56,42,0.25);
}
.btn-pay:hover { background: #b02e22; }
.btn-pay:disabled { opacity: .7; cursor: not-allowed; }

.paying-spinner {
  width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: spin .6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.payment-note {
  display: flex; align-items: center; justify-content: center; gap: 4px;
  font-size: 12px; color: var(--muted); margin-top: 12px;
}
.payment-note .el-icon { font-size: 12px; }

/* 支付成功 */
.payment-success {
  padding: 48px 24px; text-align: center;
}
.success-icon { margin-bottom: 20px; }
.success-svg {
  width: 64px; height: 64px; color: #22c55e;
  animation: successPop .4s ease;
}
.success-svg circle { stroke-dasharray: 157; stroke-dashoffset: 157; animation: circleIn .5s .1s ease forwards; }
.success-svg path { stroke-dasharray: 48; stroke-dashoffset: 48; animation: checkIn .3s .5s ease forwards; }
@keyframes successPop { 0% { transform: scale(0); } 50% { transform: scale(1.1); } 100% { transform: scale(1); } }
@keyframes circleIn { to { stroke-dashoffset: 0; } }
@keyframes checkIn { to { stroke-dashoffset: 0; } }
.success-text { font-size: 20px; font-weight: 700; color: var(--text); margin-bottom: 8px; }
.success-sub { font-size: 14px; color: var(--muted); }

/* 图片放大 */
.zoom-overlay {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(0,0,0,0.88); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
.zoom-panel {
  width: min(92vw, 1120px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.zoom-main {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(92vw, 1120px);
  height: min(72vh, 760px);
  padding: 24px;
  background: rgba(255,255,255,0.04);
  border-radius: 16px;
  box-sizing: border-box;
}
.zoom-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
  animation: modalIn .25s ease;
}
.zoom-thumbs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}
.zoom-thumb {
  background: rgba(255,255,255,0.08);
}
.zoom-arrow {
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(8px);
}
.zoom-close {
  top: 12px;
  right: 12px;
  left: auto;
  transform: none;
  z-index: 2;
}
</style>
