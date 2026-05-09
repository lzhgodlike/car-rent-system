<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
const totalCount = ref(0)
const detailVisible = ref(false)
const detailCar = ref(null)

const today = new Date().toISOString().split('T')[0]
const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0]
const searchForm = ref({ city: '郑州市', pickDate: today, dropDate: tomorrow, typeId: '' })
const { isLoggedIn } = useAuth()

onMounted(async () => {
  pageReady.value = false
  await nextTick()
  pageReady.value = true
  carTypes.value = await request.get('/car-types')
  const page = await request.get('/cars', { params: { pageNum: 1, pageSize: 6, sort: 'rentCount' } })
  cars.value = page.records
  totalCount.value = page.total
  if (isLoggedIn.value) loadFavorites()
})

const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'
const openDetail = (car) => { detailCar.value = car; detailVisible.value = true }
</script>

<template>
  <div class="home-page">
    <!-- Hero -->
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

    <!-- Search -->
    <section class="search-section">
      <div class="search-box">
        <div class="search-field">
          <label><el-icon><Location /></el-icon> 取车城市</label>
          <el-select v-model="searchForm.city" style="width:100%">
            <el-option label="郑州市" value="郑州市" /><el-option label="北京市" value="北京市" /><el-option label="上海市" value="上海市" />
          </el-select>
        </div>
        <div class="search-divider"></div>
        <div class="search-field">
          <label><el-icon><Calendar /></el-icon> 取车日期</label>
          <el-date-picker v-model="searchForm.pickDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="search-divider"></div>
        <div class="search-field">
          <label><el-icon><Calendar /></el-icon> 还车日期</label>
          <el-date-picker v-model="searchForm.dropDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </div>
        <div class="search-divider"></div>
        <div class="search-field" style="max-width:130px;">
          <label><el-icon><Van /></el-icon> 车型</label>
          <el-select v-model="searchForm.typeId" style="width:100%">
            <el-option label="全部类型" value="" />
            <el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </div>
        <button class="search-btn" @click="router.push('/book')"><el-icon><Search /></el-icon> 搜索</button>
      </div>
    </section>

    <!-- Hot rentals -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">近期热租</h2>
      </div>

      <div class="car-grid">
        <div v-for="car in cars" :key="car.id" class="car-card" @click="openDetail(car)">
          <div class="car-img">
            <img v-if="car.carImage" :src="car.carImage" :alt="`${car.brand} ${car.model}`" @error="(e) => e.target.style.display='none'" />
            <el-icon v-else size="64" style="color:var(--muted2);"><Van /></el-icon>
            <div class="car-tag">{{ carTypeName(car.typeId) }}</div>
            <div class="car-fav" :class="{ liked: favoriteIds.has(car.id) }" @click.stop="toggleFavorite(car)"><el-icon><Star /></el-icon></div>
          </div>
          <div class="car-body">
            <div class="car-name">{{ car.brand }} {{ car.model }}</div>
            <div class="car-meta">{{ car.plateNumber }} · {{ (car.mileage / 10000).toFixed(1) }}万km</div>
            <div class="car-features">
              <div class="car-feat"><el-icon><User /></el-icon> 5座</div>
              <div class="car-feat"><el-icon><OfficeBuilding /></el-icon> {{ carTypeName(car.typeId) }}</div>
              <div class="car-feat"><el-icon><Location /></el-icon> {{ car.pickupAddress || '待补充' }}</div>
            </div>
          </div>
          <div class="car-footer">
            <div class="car-price"><span>¥{{ car.dayPrice }}</span><small>/天</small></div>
            <div class="car-stars"><el-icon><Star /></el-icon> 4.8<span>(126)</span></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Promo -->
    <section class="promo-banner">
      <div class="promo-bg"></div><div class="promo-bg2"></div>
      <div class="promo-content">
        <h2 class="promo-title">新用户首租<br>立减优惠</h2>
        <p class="promo-sub">注册即送优惠券，首单专享折扣</p>
        <div class="promo-actions">
          <button class="btn-promo btn-promo-light" @click="openLoginModal">立即领取 →</button>
          <button class="btn-promo btn-promo-outline">了解详情</button>
        </div>
      </div>
      <div class="promo-badge"><div>8折</div><small>首单专享</small></div>
    </section>

    <!-- Footer -->
    <footer class="site-footer">
      <div class="footer-grid">
        <div>
          <div class="footer-logo">驰云租车</div>
          <div class="footer-desc">专注优质出行体验，提供全品类车型租赁服务，让每一段旅程都充满驾驭乐趣。</div>
        </div>
        <div class="footer-col"><h4>快速导航</h4><ul><li @click="router.push('/book')">找车租车</li><li @click="router.push('/my-orders')">我的订单</li><li @click="router.push('/my-profile')">个人中心</li><li>优惠活动</li></ul></div>
        <div class="footer-col"><h4>服务支持</h4><ul><li>使用指南</li><li>常见问题</li><li>联系客服</li><li>投诉建议</li></ul></div>
        <div class="footer-col"><h4>关于我们</h4><ul><li>公司介绍</li><li>加入我们</li><li>合作加盟</li><li>隐私政策</li></ul></div>
      </div>
      <div class="footer-bottom">© 2026 驰云租车 · 豫ICP备XXXXXXXX号</div>
    </footer>

    <!-- Car Detail Modal -->
    <div v-if="detailVisible" class="detail-overlay" @click.self="detailVisible = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">{{ detailCar?.brand }} {{ detailCar?.model }}</div>
          <button class="detail-close" @click="detailVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body" v-if="detailCar">
          <div class="detail-img-wrap">
            <img v-if="detailCar.carImage" :src="detailCar.carImage" class="detail-img" @error="(e) => e.target.style.display='none'" />
            <div v-else class="detail-img-placeholder"><el-icon size="48" style="color:var(--muted2);"><Van /></el-icon></div>
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
          <button class="auth-btn" style="margin-top:16px;" @click="detailVisible=false;router.push('/book')">立即预订</button>
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
.car-img img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.car-card:hover .car-img img { transform: scale(1.05); }
.car-tag {
  position: absolute; top: 12px; left: 12px;
  background: var(--white); border-radius: 6px; padding: 3px 9px;
  font-size: 11px; font-weight: 500; color: var(--text); box-shadow: var(--shadow-sm);
}
.car-fav {
  position: absolute; top: 12px; right: 12px;
  width: 30px; height: 30px; background: var(--white); border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm); cursor: pointer; transition: all .18s;
  font-size: 15px; color: var(--muted);
}
.car-fav:hover { color: var(--accent); }
.car-fav.liked { color: var(--accent); }
.car-body { padding: 16px 18px; }
.car-name { font-size: 16px; font-weight: 500; }
.car-meta { font-size: 12px; color: var(--muted); margin-top: 3px; }
.car-features { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.car-feat { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--muted); background: var(--bg); padding: 3px 9px; border-radius: 20px; }
.car-feat .el-icon { font-size: 12px; }
.car-footer { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-top: 1px solid var(--border); }
.car-price { font-family: 'Bebas Neue', monospace; }
.car-price span { font-size: 22px; font-weight: 500; color: var(--accent); }
.car-price small { font-size: 12px; color: var(--muted); }
.car-stars { display: flex; align-items: center; gap: 3px; font-size: 12px; color: var(--gold); }
.car-stars .el-icon { font-size: 13px; }
.car-stars span { color: var(--muted); margin-left: 2px; }

/* Promo */
.promo-banner {
  margin: 0 40px 60px; max-width: 1120px; margin-left: auto; margin-right: auto;
  background: linear-gradient(135deg, #1a1a1a 0%, #2e1a0a 100%);
  border-radius: 20px; padding: 40px 48px;
  display: flex; align-items: center; justify-content: space-between;
  position: relative; overflow: hidden;
}
.promo-bg { position: absolute; right: -40px; top: -40px; width: 280px; height: 280px; border-radius: 50%; background: rgba(200,56,42,0.15); }
.promo-bg2 { position: absolute; right: 80px; bottom: -60px; width: 180px; height: 180px; border-radius: 50%; background: rgba(196,154,60,0.12); }
.promo-content { position: relative; z-index: 1; }
.promo-title { font-family: 'Playfair Display', serif; font-size: 32px; font-weight: 700; color: #fff; line-height: 1.2; }
.promo-sub { font-size: 14px; color: rgba(255,255,255,0.6); margin-top: 8px; }
.promo-actions { display: flex; gap: 10px; margin-top: 20px; }
.btn-promo { padding: 10px 22px; border-radius: 24px; font-size: 13px; font-weight: 500; font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .18s; display: flex; align-items: center; gap: 6px; border: none; }
.btn-promo-light { background: #fff; color: #1a1a1a; }
.btn-promo-outline { background: transparent; color: rgba(255,255,255,0.7); border: 1px solid rgba(255,255,255,0.3); }
.btn-promo-outline:hover { border-color: rgba(255,255,255,0.6); color: #fff; }
.promo-badge {
  background: var(--accent); color: #fff;
  font-family: 'Bebas Neue', monospace; font-size: 36px; font-weight: 500;
  padding: 16px 28px; border-radius: 12px; position: relative; z-index: 1;
  text-align: center; line-height: 1;
}
.promo-badge small { display: block; font-size: 12px; margin-top: 4px; font-family: 'Noto Sans SC', sans-serif; }

/* Footer */
.site-footer { background: #1a1a1a; color: rgba(255,255,255,0.5); padding: 48px 40px 32px; margin-top: 40px; }
.footer-grid { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; max-width: 1120px; margin: 0 auto 40px; }
.footer-logo { font-family: 'Playfair Display', serif; font-size: 22px; color: #fff; margin-bottom: 12px; }
.footer-desc { font-size: 13px; line-height: 1.7; }
.footer-col h4 { color: #fff; font-size: 13px; font-weight: 500; margin-bottom: 14px; }
.footer-col ul { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.footer-col ul li { font-size: 13px; cursor: pointer; transition: color .15s; }
.footer-col ul li:hover { color: rgba(255,255,255,0.8); }
.footer-bottom { border-top: 1px solid rgba(255,255,255,0.08); padding-top: 24px; text-align: center; font-size: 12px; max-width: 1120px; margin: 0 auto; }

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
.detail-img-wrap {
  border-radius: 12px; overflow: hidden; background: var(--bg); margin-bottom: 20px;
}
.detail-img { width: 100%; height: 200px; object-fit: cover; display: block; }
.detail-img-placeholder { height: 200px; display: flex; align-items: center; justify-content: center; }
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
