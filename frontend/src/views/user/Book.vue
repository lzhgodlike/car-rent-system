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

  await request.post('/rent-orders', { carId: selectedCar.value.id, ...rentForm.value })
  ElMessage.success('预订成功！')
  selectedCar.value = null; loadData(true)
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
              <div class="car-meta">{{ car.plateNumber }} · {{ (car.mileage / 10000).toFixed(1) }}万km</div>
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
            <div style="font-size:12px;color:var(--muted);text-align:right;">里程 {{ (detailCar.mileage / 10000).toFixed(1) }}万km<br>{{ detailCar.plateNumber }}</div>
          </div>
          <div class="detail-rows">
            <div class="detail-row"><span class="detail-label">车辆编号</span><span class="detail-val">{{ detailCar.carNo }}</span></div>
            <div class="detail-row"><span class="detail-label">车牌号</span><span class="detail-val">{{ detailCar.plateNumber }}</span></div>
            <div class="detail-row"><span class="detail-label">品牌型号</span><span class="detail-val">{{ detailCar.brand }} {{ detailCar.model }}</span></div>
            <div class="detail-row"><span class="detail-label">车型分类</span><span class="detail-val">{{ carTypeName(detailCar.typeId) }}</span></div>
            <div class="detail-row"><span class="detail-label">总里程数</span><span class="detail-val">{{ detailCar.mileage?.toLocaleString() }} km</span></div>
            <div class="detail-row"><span class="detail-label">取车地址</span><span class="detail-val">{{ detailCar.pickupAddress || '待补充' }}</span></div>
            <div class="detail-row"><span class="detail-label">车辆状态</span><span class="detail-val">{{ detailCar.status === 'AVAILABLE' ? '可用' : detailCar.status }}</span></div>
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
