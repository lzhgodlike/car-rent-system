<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useAuth, openLoginModal } from '../../utils/auth'

const favoriteIds = ref(new Set())

const loadFavorites = async () => {
  try {
    const list = await request.get('/favorites')
    favoriteIds.value = new Set(list.map(c => c.id))
  } catch {}
}

const toggleFavorite = async (car) => {
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

const router = useRouter()
const pageReady = ref(false)
const cars = ref([])
const carTypes = ref([])
const cities = ref([])
const totalCount = ref(0)
const detailVisible = ref(false)
const detailCar = ref(null)
const imageZoomVisible = ref(false)
const showBackToTop = ref(false)
const scrollToTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

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
const tomorrowDate = new Date(getStartOfToday())
tomorrowDate.setDate(tomorrowDate.getDate() + 1)
const tomorrow = formatDate(tomorrowDate)
const searchForm = ref({ city: '', pickDate: today, dropDate: tomorrow, typeId: '' })
const { isLoggedIn } = useAuth()

const onScroll = () => { showBackToTop.value = window.scrollY > 300 }

onMounted(async () => {
  pageReady.value = false
  await nextTick()
  pageReady.value = true
  const [typeList, cityList, page] = await Promise.all([
    request.get('/car-types'),
    request.get('/cars/cities'),
    request.get('/cars', { params: { pageNum: 1, pageSize: 6, sort: 'rentCount', status: 'AVAILABLE' } })
  ])
  carTypes.value = typeList
  cities.value = cityList
  cars.value = page.records
  totalCount.value = page.total
  if (isLoggedIn.value) loadFavorites()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => { window.removeEventListener('scroll', onScroll) })

const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'
const disablePickDate = (date) => {
  const current = new Date(date)
  current.setHours(0, 0, 0, 0)
  return current < getStartOfToday()
}
const disableDropDate = (date) => {
  const current = new Date(date)
  current.setHours(0, 0, 0, 0)
  const minDate = new Date(`${searchForm.value.pickDate || today}T00:00:00`)
  return current <= minDate
}
const syncDropDate = (pickDate) => {
  if (!pickDate) return
  if (!searchForm.value.dropDate || searchForm.value.dropDate <= pickDate) {
    const nextDay = new Date(`${pickDate}T00:00:00`)
    nextDay.setDate(nextDay.getDate() + 1)
    searchForm.value.dropDate = formatDate(nextDay)
  }
}
const handleSearch = () => {
  const query = {}
  if (searchForm.value.city) query.city = searchForm.value.city
  if (searchForm.value.pickDate) query.pickDate = searchForm.value.pickDate
  if (searchForm.value.dropDate) query.dropDate = searchForm.value.dropDate
  if (searchForm.value.typeId) query.typeId = searchForm.value.typeId
  router.push({ path: '/book', query })
}
watch(() => searchForm.value.pickDate, (pickDate) => {
  syncDropDate(pickDate)
})
const detailImages = computed(() => {
  const images = detailCar.value?.carImages || []
  const normalizedImages = images
    .map(image => image?.imageUrl || image)
    .filter(Boolean)
  if (normalizedImages.length) return normalizedImages
  return detailCar.value?.carImage ? [detailCar.value.carImage] : []
})
const detailActiveImageIndex = ref(0)
const detailActiveImage = computed(() => detailImages.value[detailActiveImageIndex.value] || '')
const hasMultipleDetailImages = computed(() => detailImages.value.length > 1)
const openDetail = async (car) => {
  detailCar.value = await request.get(`/cars/${car.id}`)
  detailActiveImageIndex.value = 0
  detailVisible.value = true
}
const selectDetailImage = (index) => {
  detailActiveImageIndex.value = index
}
const prevDetailImage = () => {
  if (!hasMultipleDetailImages.value) return
  detailActiveImageIndex.value = (detailActiveImageIndex.value - 1 + detailImages.value.length) % detailImages.value.length
}
const nextDetailImage = () => {
  if (!hasMultipleDetailImages.value) return
  detailActiveImageIndex.value = (detailActiveImageIndex.value + 1) % detailImages.value.length
}
const openImageZoom = () => {
  if (detailActiveImage.value) imageZoomVisible.value = true
}
</script>

<template>
  <div class="home-page">
    <!-- 首页 -->
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-label"><div class="hero-label-dot"></div>{{ totalCount }}辆车辆 · 即时可用</div>
      <h1 v-if="pageReady" class="hero-title fade-up">驾驭每一段<br><em>旅程的自由</em></h1>
      <p v-if="pageReady" class="hero-sub fade-up-1">豪华轿车、城市代步、商务出行——无论何种需求，找到最适合您的座驾</p>
      <div v-if="pageReady" class="hero-actions fade-up-2">
        <button class="btn-hero-primary" @click="router.push('/book')"><el-icon><Search /></el-icon> 立即找车</button>
        <button v-if="!isLoggedIn" class="btn-hero-ghost" @click="openLoginModal"><el-icon><User /></el-icon> 登录账户</button>
        <button v-else class="btn-hero-ghost" @click="router.push('/my-orders')">我的订单</button>
      </div>
      <div v-if="pageReady" class="hero-stats fade-up-3">
        <div class="hero-stat"><div class="hero-stat-num">{{ totalCount }}+</div><div class="hero-stat-label">车辆可选</div></div>
        <div class="hero-stat"><div class="hero-stat-num">4.9★</div><div class="hero-stat-label">用户评分</div></div>
        <div class="hero-stat"><div class="hero-stat-num">2万+</div><div class="hero-stat-label">服务用户</div></div>
        <div class="hero-stat"><div class="hero-stat-num">¥99起</div><div class="hero-stat-label">每日租金</div></div>
      </div>
    </section>

    <!--筛选框 -->
    <section class="search-section">
      <div class="search-box">
        <div class="search-field">
          <label><el-icon><Location /></el-icon> 取车城市</label>
          <el-select v-model="searchForm.city" style="width:100%">
            <el-option v-for="city in cities" :key="city" :label="city" :value="city" />
          </el-select>
        </div>
        <div class="search-divider"></div>
        <div class="search-field">
          <label><el-icon><Calendar /></el-icon> 取车日期</label>
          <el-date-picker v-model="searchForm.pickDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disablePickDate" :editable="false" />
        </div>
        <div class="search-divider"></div>
        <div class="search-field">
          <label><el-icon><Calendar /></el-icon> 还车日期</label>
          <el-date-picker v-model="searchForm.dropDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disableDropDate" :editable="false" />
        </div>
        <div class="search-divider"></div>
        <div class="search-field" style="max-width:130px;">
          <label><el-icon><Van /></el-icon> 车型</label>
          <el-select v-model="searchForm.typeId" style="width:100%">
            <el-option label="全部类型" value="" />
            <el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </div>
        <button class="search-btn" @click="handleSearch"><el-icon><Search /></el-icon> 搜索</button>
      </div>
    </section>

    <!-- 近期热租 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">近期热租</h2>
      </div>

      <div class="car-grid">
        <div v-for="car in cars" :key="car.id" class="car-card" @click="openDetail(car)">
          <div class="car-img">
            <div class="car-img-placeholder"><el-icon size="32" style="color:var(--muted2);"><Van /></el-icon></div>
            <img v-if="car.carImage" :src="car.carImage" :alt="`${car.brand} ${car.model}`" loading="lazy" class="car-img-el" @load="(e) => e.target.classList.add('loaded')" @error="(e) => e.target.style.display='none'" />
            <div class="car-tag">{{ carTypeName(car.typeId) }}</div>
            <div class="car-fav" :class="{ liked: favoriteIds.has(car.id) }" @click.stop="toggleFavorite(car)">
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
            <div class="car-price"><span>¥{{ car.dayPrice }}</span><small>/天</small></div>
            <button class="btn-book-sm" @click.stop="router.push({ path: '/book', query: { carId: car.id } })">立即预订</button>
          </div>
        </div>
      </div>
    </section>

    <!-- 返回顶部 -->
    <Transition name="back-top">
      <button v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
        <el-icon size="15"><ArrowUp /></el-icon>
        <span>返回顶部</span>
      </button>
    </Transition>

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
              <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-left" @click.stop="prevDetailImage"><el-icon><ArrowLeft /></el-icon></button>
              <img v-if="detailActiveImage" :src="detailActiveImage" class="detail-img" @error="(e) => e.target.style.display='none'" />
              <div v-else class="detail-img-placeholder"><el-icon size="48" style="color:var(--muted2);"><Van /></el-icon></div>
              <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-right" @click.stop="nextDetailImage"><el-icon><ArrowRight /></el-icon></button>
            </div>
          </div>
          <div v-if="detailImages.length > 1" class="detail-thumbs">
            <button
              v-for="(image, index) in detailImages"
              :key="`${image}-${index}`"
              class="detail-thumb"
              :class="{ active: index === detailActiveImageIndex }"
              @click.stop="selectDetailImage(index)"
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
          <button class="detail-book-btn" @click="detailVisible=false;router.push({ path: '/book', query: { carId: detailCar.id } })">
            <el-icon><Check /></el-icon> 立即预订
          </button>
        </div>
      </div>
    </div>

    <div v-if="imageZoomVisible" class="zoom-overlay" @click="imageZoomVisible = false">
      <div class="zoom-panel" @click.stop>
        <div class="zoom-main">
          <button class="gallery-arrow zoom-arrow zoom-close" @click="imageZoomVisible = false"><el-icon><Close /></el-icon></button>
          <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-left zoom-arrow" @click.stop="prevDetailImage"><el-icon><ArrowLeft /></el-icon></button>
          <img :src="detailActiveImage" class="zoom-img" :alt="`${detailCar?.brand || ''} ${detailCar?.model || ''}`" />
          <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-right zoom-arrow" @click.stop="nextDetailImage"><el-icon><ArrowRight /></el-icon></button>
        </div>
        <div v-if="detailImages.length > 1" class="zoom-thumbs">
          <button
            v-for="(image, index) in detailImages"
            :key="`zoom-${image}-${index}`"
            class="detail-thumb zoom-thumb"
            :class="{ active: index === detailActiveImageIndex }"
            @click.stop="selectDetailImage(index)"
          >
            <img :src="image" />
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.home-page { background: var(--bg); }

/* Hero */
.hero {
  min-height: 88vh; display: flex; flex-direction: column; align-items: center; justify-content: center;
  text-align: center; padding: 80px 40px 60px; position: relative; overflow: hidden;
}
.hero-bg {
  position: absolute; inset: 0; z-index: 0;
  background: radial-gradient(ellipse 60% 50% at 50% 120%, rgba(200,56,42,0.10) 0%, transparent 70%),
              radial-gradient(ellipse 40% 30% at 80% 20%, rgba(196,154,60,0.08) 0%, transparent 60%);
}
.hero-label {
  display: inline-flex; align-items: center; gap: 6px;
  background: var(--white); border: 1px solid var(--border-dark); border-radius: 20px;
  padding: 5px 14px 5px 8px; font-size: 12px; color: var(--muted);
  margin-bottom: 28px; position: relative; z-index: 1; box-shadow: var(--shadow-sm);
}
.hero-label-dot { width: 6px; height: 6px; background: var(--success); border-radius: 50%; animation: blink 2s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:.3} }
.hero-title {
  font-family: 'Playfair Display', serif;
  font-size: clamp(42px, 7vw, 80px); font-weight: 900; line-height: 1.08;
  color: var(--text); position: relative; z-index: 1; max-width: 800px;
}
.hero-title em { color: var(--accent); font-style: normal; }
.hero-sub { font-size: 16px; color: var(--muted); margin-top: 20px; max-width: 480px; line-height: 1.7; position: relative; z-index: 1; }
.hero-actions { display: flex; gap: 12px; margin-top: 36px; position: relative; z-index: 1; }
.btn-hero-primary {
  padding: 14px 32px; background: var(--accent); color: #fff; border: none; border-radius: 32px;
  font-size: 15px; font-weight: 500; font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  box-shadow: 0 8px 24px rgba(200,56,42,0.30); transition: all .2s;
  display: flex; align-items: center; gap: 8px;
}
.btn-hero-primary:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(200,56,42,0.38); }
.btn-hero-ghost {
  padding: 14px 32px; background: var(--white); color: var(--text);
  border: 1.5px solid var(--border-dark); border-radius: 32px; font-size: 15px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .2s;
  display: flex; align-items: center; gap: 8px;
}
.btn-hero-ghost:hover { border-color: var(--text); }
.hero-stats { display: flex; gap: 48px; margin-top: 60px; position: relative; z-index: 1; }
.hero-stat { text-align: center; }
.hero-stat-num { font-family: 'DM Mono', monospace; font-size: 28px; font-weight: 500; color: var(--text); }
.hero-stat-label { font-size: 12px; color: var(--muted); margin-top: 2px; }

/* Search */
.search-section { padding: 0 40px 60px; }
.search-box {
  background: var(--white); border: 1px solid var(--border-dark); border-radius: 20px;
  padding: 20px 24px; box-shadow: var(--shadow-md);
  display: flex; gap: 16px; align-items: flex-end; max-width: 960px; margin: 0 auto;
}
.search-field { flex: 1; }
.search-field label { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--muted); text-transform: uppercase; letter-spacing: .8px; margin-bottom: 8px; }
.search-divider { width: 1px; background: var(--border); align-self: stretch; margin: 4px 0; }
.search-btn {
  padding: 12px 28px; background: var(--accent); color: #fff; border: none; border-radius: 12px;
  font-size: 14px; font-weight: 500; font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  display: flex; align-items: center; gap: 8px; white-space: nowrap; transition: background .18s;
}
.search-btn:hover { background: #b02e22; }

/* Categories */
.section { padding: 0 40px 60px; max-width: 1200px; margin: 0 auto; }
.section-header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 24px; }
.section-title { font-family: 'Playfair Display', serif; font-size: 28px; font-weight: 700; }
.cat-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.cat-chip {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 18px; background: var(--white);
  border: 1.5px solid var(--border); border-radius: 32px;
  cursor: pointer; transition: all .18s; font-size: 14px;
}
.cat-chip .el-icon { font-size: 18px; color: var(--muted); transition: color .18s; }
.cat-chip:hover { border-color: var(--accent); color: var(--accent); }
.cat-chip:hover .el-icon { color: var(--accent); }
.cat-chip.active { border-color: var(--accent); background: var(--accent-light); color: var(--accent); font-weight: 500; }
.cat-chip.active .el-icon { color: var(--accent); }

/* Car grid */
.car-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; margin-top: 24px; }
.car-card {
  background: var(--white); border: 1px solid var(--border); border-radius: var(--radius);
  overflow: hidden; cursor: pointer; transition: all .22s; box-shadow: var(--shadow-sm);
}
.car-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); border-color: rgba(0,0,0,0.12); }
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
  font-size: 11px; font-weight: 500; color: var(--text); box-shadow: var(--shadow-sm);
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
.car-price { font-family: 'Bebas Neue', monospace; }
.car-price span { font-size: 22px; font-weight: 500; color: var(--accent); }
.car-price small { font-size: 12px; color: var(--muted); }
.btn-book-sm {
  padding: 6px 14px; border-radius: 8px; font-size: 12px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .15s;
  background: var(--accent); border: none; color: #fff; font-weight: 500;
}
.btn-book-sm:hover { background: #b02e22; }

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

/* Detail modal */
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
.detail-gallery-block { margin-bottom: 20px; }
.detail-img-wrap {
  position: relative;
  border-radius: 12px; overflow: hidden; background: var(--bg); margin-bottom: 12px;
  cursor: pointer;
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
  border-radius: 10px;
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
.detail-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 20px; }
.detail-stat { text-align: center; padding: 12px; background: var(--bg); border-radius: 10px; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.detail-stat span { font-size: 12px; color: var(--muted); }
.detail-price-box { display: flex; align-items: center; justify-content: space-between; padding: 14px; background: var(--accent-light); border-radius: 10px; }
.detail-price { font-family: 'Bebas Neue', sans-serif; font-size: 24px; font-weight: 500; color: var(--accent); }
.detail-rows { border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-top: 20px; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; font-size: 14px; border-bottom: 1px solid var(--border); }
.detail-row:last-child { border-bottom: none; }
.detail-label { color: var(--muted); font-size: 13px; }
.detail-val { font-weight: 500; }
.auth-btn {
  width: 100%; padding: 13px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 15px; font-weight: 500;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer;
  box-shadow: 0 4px 16px rgba(200,56,42,0.25); transition: background .18s;
}
.auth-btn:hover { background: #b02e22; }
</style>
