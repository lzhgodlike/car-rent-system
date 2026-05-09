<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { getAuth, setAuth, useAuth } from '../../utils/auth'

const router = useRouter()

const cars = ref([])
const carTypes = ref([])
const loading = ref(false)
const selectedCar = ref(null)
const rentForm = ref({ rentDate: '', expectedReturnDate: '', remark: '' })
const activeType = ref('')
const query = ref({ brand: '', typeId: '', status: 'AVAILABLE', sort: '' })
const today = new Date().toISOString().split('T')[0]
const toastMsg = ref('')
const toastVisible = ref(false)
const profileTipVisible = ref(false)
const authModal = ref(null)
const loginLoading = ref(false)
const loginForm = ref({ username: '', password: '' })
let toastTimer = null

// 详情弹窗 & 图片放大
const detailVisible = ref(false)
const detailCar = ref(null)
const imageZoomVisible = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/cars', { params: { ...query.value, pageNum: 1, pageSize: 50 } })
    cars.value = page.records
  } finally { loading.value = false }
}
const loadTypes = async () => { carTypes.value = await request.get('/car-types') }
onMounted(async () => { await loadTypes(); loadData() })

const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'
const filteredCars = computed(() => {
  if (!activeType.value) return cars.value
  return cars.value.filter(c => carTypeName(c.typeId) === activeType.value)
})

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

const openDetail = (e, car) => {
  e.stopPropagation()
  detailCar.value = car
  detailVisible.value = true
}

const openImageZoom = () => {
  if (detailCar.value?.carImage) imageZoomVisible.value = true
}

const days = computed(() => {
  if (!rentForm.value.rentDate || !rentForm.value.expectedReturnDate) return 0
  return Math.max(1, Math.ceil((new Date(rentForm.value.expectedReturnDate) - new Date(rentForm.value.rentDate)) / 86400000))
})
const totalPrice = computed(() => selectedCar.value ? selectedCar.value.dayPrice * days.value : 0)

const openLogin = () => { loginForm.value = { username: '', password: '' }; authModal.value = 'login' }
const closeAuthModal = () => { authModal.value = null }

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) { ElMessage.warning('请输入用户名和密码'); return }
  loginLoading.value = true
  try {
    const data = await request.post('/auth/login', loginForm.value)
    setAuth(data)
    authModal.value = null
    const role = data.userInfo?.role
    if (role === 'ADMIN') {
      router.replace('/admin/dashboard')
    } else {
      ElMessage.success('登录成功，欢迎回来！')
    }
  } finally { loginLoading.value = false }
}

const submitRent = async () => {
  if (!selectedCar.value) { ElMessage.warning('请先选择车辆'); return }
  if (!rentForm.value.rentDate || !rentForm.value.expectedReturnDate) { ElMessage.warning('请选择日期'); return }

  if (!getAuth()) {
    ElMessage.warning('请先登录')
    openLogin()
    return
  }

  try {
    const profile = await request.get('/users/profile')
    if (!profile.realName || !profile.phone || !profile.idCard) {
      profileTipVisible.value = true
      return
    }
  } catch { return }

  await request.post('/rent-orders', { carId: selectedCar.value.id, ...rentForm.value })
  ElMessage.success('预订成功！')
  selectedCar.value = null; loadData()
}
</script>

<template>
  <div class="book-page">
    <div class="book-layout">
      <!-- Car list -->
      <div class="book-main">
        <h2 class="page-title">选择车辆</h2>
        <div class="filter-bar">
          <div class="filter-chips">
            <div class="filter-chip" :class="{ active: !activeType }" @click="activeType = ''; query.typeId = ''">全部</div>
            <div v-for="t in carTypes" :key="t.id" class="filter-chip" :class="{ active: activeType === t.typeName }" @click="activeType = t.typeName; query.typeId = t.id">{{ t.typeName }}</div>
          </div>
          <select class="sort-sel" v-model="query.sort">
            <option value="">默认排序</option>
            <option value="asc">价格从低到高</option>
            <option value="desc">价格从高到低</option>
          </select>
        </div>

        <div class="car-grid" v-loading="loading">
          <div v-for="car in filteredCars" :key="car.id" class="car-card" :class="{ selected: selectedCar?.id === car.id }" @click="selectCar(car)">
            <div class="car-img">
              <img v-if="car.carImage" :src="car.carImage" :alt="`${car.brand} ${car.model}`" @error="(e) => e.target.style.display='none'" />
              <el-icon v-else size="40" style="color:var(--muted2);"><Van /></el-icon>
              <div class="car-tag">{{ carTypeName(car.typeId) }}</div>
            </div>
            <div class="car-body">
              <div class="car-name">{{ car.brand }} {{ car.model }}</div>
              <div class="car-meta">{{ car.plateNumber }} · {{ (car.mileage / 10000).toFixed(1) }}万km</div>
              <div class="car-features">
                <div class="car-feat"><el-icon><User /></el-icon> 5座</div>
                <div class="car-feat"><el-icon><OfficeBuilding /></el-icon> {{ carTypeName(car.typeId) }}</div>
              </div>
            </div>
            <div class="car-footer">
              <span class="car-price">¥{{ car.dayPrice }}<small>/天</small></span>
              <button class="btn-detail" @click="openDetail($event, car)">查看详情</button>
            </div>
          </div>
        </div>
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
            <label>取车 / 还车日期</label>
            <div class="date-row">
              <el-date-picker v-model="rentForm.rentDate" type="date" value-format="YYYY-MM-DD" format="YYYY/MM/DD" placeholder="取车日期" style="width:100%" :disabled-date="(d) => d < new Date(today)" />
              <el-date-picker v-model="rentForm.expectedReturnDate" type="date" value-format="YYYY-MM-DD" format="YYYY/MM/DD" placeholder="还车日期" style="width:100%" :disabled-date="(d) => d <= new Date(rentForm.rentDate || today)" />
            </div>
          </div>
          <div class="summary-field">
            <label>备注</label>
            <input v-model="rentForm.remark" />
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
          <p class="book-note">预订后24小时内免费取消 · 无隐藏费用</p>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition name="toast">
      <div v-if="toastVisible" class="toast">{{ toastMsg }}</div>
    </Transition>

    <!-- Login Modal -->
    <div v-if="authModal" class="auth-overlay" @click.self="closeAuthModal">
      <div class="auth-modal" style="width:420px;">
        <div class="auth-modal-header">
          <div class="auth-modal-title">请先登录</div>
          <button class="auth-modal-close" @click="closeAuthModal"><el-icon><Close /></el-icon></button>
        </div>
        <div class="auth-modal-body">
          <div class="auth-field"><label>用户名</label><input v-model="loginForm.username" placeholder="请输入用户名" @keyup.enter="handleLogin" /></div>
          <div class="auth-field"><label>密码</label><input v-model="loginForm.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" /></div>
          <button class="auth-btn" :disabled="loginLoading" @click="handleLogin">{{ loginLoading ? '登录中...' : '登录' }}</button>
        </div>
      </div>
    </div>

    <!-- Profile tip dialog -->
    <div v-if="profileTipVisible" class="auth-overlay" @click.self="profileTipVisible = false">
      <div class="auth-modal">
        <div class="auth-modal-header">
          <div class="auth-modal-title">请先完善个人信息</div>
          <button class="auth-modal-close" @click="profileTipVisible = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="auth-modal-body" style="text-align:center;">
          <div style="font-size:48px;margin-bottom:16px;">📋</div>
          <p style="font-size:14px;color:var(--muted);line-height:1.8;">
            租车前需要完善<strong style="color:var(--text);">姓名、手机号和身份证号</strong>信息。<br>
            请前往个人中心完成填写后再试。
          </p>
          <button class="auth-btn" style="margin-top:20px;" @click="profileTipVisible = false; router.push('/my-profile')">
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
          <!-- 图片 -->
          <div class="detail-img-wrap" @click="openImageZoom">
            <img v-if="detailCar.carImage" :src="detailCar.carImage" class="detail-img" @error="(e) => e.target.style.display='none'" />
            <div v-else class="detail-img-placeholder"><el-icon size="48" style="color:var(--muted2);"><Van /></el-icon></div>
            <div class="detail-img-zoom-hint"><el-icon><ZoomIn /></el-icon> 点击查看大图</div>
          </div>

          <!-- 属性 -->
          <div class="detail-stats">
            <div class="detail-stat">
              <el-icon size="18" style="color:var(--accent);"><User /></el-icon>
              <span>5座</span>
            </div>
            <div class="detail-stat">
              <el-icon size="18" style="color:var(--accent);"><OfficeBuilding /></el-icon>
              <span>{{ carTypeName(detailCar.typeId) }}</span>
            </div>
            <div class="detail-stat">
              <el-icon size="18" style="color:var(--accent);"><Location /></el-icon>
              <span>{{ detailCar.pickupAddress || '待补充' }}</span>
            </div>
          </div>

          <!-- 价格 -->
          <div class="detail-price-box">
            <div>
              <div style="font-size:12px;color:var(--muted);">每日租金</div>
              <div class="detail-price">¥{{ detailCar.dayPrice }}/天</div>
            </div>
            <div style="font-size:12px;color:var(--muted);text-align:right;">
              里程 {{ (detailCar.mileage / 10000).toFixed(1) }}万km<br>{{ detailCar.plateNumber }}
            </div>
          </div>

          <!-- 详细信息 -->
          <div class="detail-rows">
            <div class="detail-row"><span class="detail-label">车辆编号</span><span class="detail-val">{{ detailCar.carNo }}</span></div>
            <div class="detail-row"><span class="detail-label">车牌号</span><span class="detail-val">{{ detailCar.plateNumber }}</span></div>
            <div class="detail-row"><span class="detail-label">品牌型号</span><span class="detail-val">{{ detailCar.brand }} {{ detailCar.model }}</span></div>
            <div class="detail-row"><span class="detail-label">车型分类</span><span class="detail-val">{{ carTypeName(detailCar.typeId) }}</span></div>
            <div class="detail-row"><span class="detail-label">总里程数</span><span class="detail-val">{{ detailCar.mileage?.toLocaleString() }} km</span></div>
            <div class="detail-row"><span class="detail-label">取车地址</span><span class="detail-val">{{ detailCar.pickupAddress || '待补充' }}</span></div>
            <div class="detail-row"><span class="detail-label">车辆状态</span><span class="detail-val">{{ detailCar.status === 'AVAILABLE' ? '可用' : detailCar.status }}</span></div>
          </div>

          <button class="auth-btn" style="margin-top:16px;" @click="detailVisible = false; selectCar(detailCar)">
            选择该车辆
          </button>
        </div>
      </div>
    </div>

    <!-- 图片放大 -->
    <div v-if="imageZoomVisible" class="zoom-overlay" @click="imageZoomVisible = false">
      <img :src="detailCar?.carImage" class="zoom-img" />
    </div>
  </div>
</template>

<style scoped>
.book-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; }
.book-layout { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
.page-title { font-size: 24px; font-weight: 700; margin-bottom: 20px; }

.filter-bar {
  display: flex; gap: 10px; align-items: center;
  background: var(--white); border: 1px solid var(--border);
  border-radius: 12px; padding: 12px 16px; margin-bottom: 20px; flex-wrap: wrap;
}
.filter-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.filter-chip {
  padding: 6px 14px; border-radius: 20px; font-size: 13px;
  border: 1px solid var(--border); background: var(--bg);
  cursor: pointer; transition: all .15s; color: var(--muted);
}
.filter-chip.active { background: var(--accent-light); border-color: var(--accent); color: var(--accent); font-weight: 500; }
.sort-sel {
  margin-left: auto; padding: 6px 12px; border-radius: 20px; font-size: 13px;
  border: 1px solid var(--border); background: var(--bg);
  color: var(--text); font-family: 'Noto Sans SC', sans-serif; outline: none;
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
.car-img img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .3s; }
.car-card:hover .car-img img { transform: scale(1.05); }
.car-tag {
  position: absolute; top: 12px; left: 12px;
  background: var(--white); border-radius: 6px; padding: 3px 9px;
  font-size: 11px; font-weight: 500; box-shadow: var(--shadow-sm);
}
.car-body { padding: 16px 18px; }
.car-name { font-size: 16px; font-weight: 500; }
.car-meta { font-size: 12px; color: var(--muted); margin-top: 3px; }
.car-features { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.car-feat { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--muted); background: var(--bg); padding: 3px 9px; border-radius: 20px; }
.car-feat .el-icon { font-size: 12px; }
.car-footer { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-top: 1px solid var(--border); }
.car-price { font-family: 'Bebas Neue', monospace; font-size: 22px; font-weight: 500; color: var(--accent); }
.car-price small { font-size: 12px; color: var(--muted); }
.btn-detail {
  padding: 6px 14px; border-radius: 8px; font-size: 12px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .15s;
  background: none; border: 1px solid var(--border-dark); color: var(--text);
}
.btn-detail:hover { border-color: var(--accent); color: var(--accent); }

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
.summary-field input, .summary-field select {
  width: 100%; padding: 9px 12px; border: 1px solid var(--border);
  border-radius: var(--radius-sm); background: var(--bg);
  font-size: 14px; color: var(--text); font-family: 'Noto Sans SC', sans-serif;
  outline: none; transition: border-color .15s;
}
.summary-field input:focus, .summary-field select:focus { border-color: var(--accent); }
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

/* Auth overlay */
.auth-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.5); display: flex;
  align-items: center; justify-content: center;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.auth-modal {
  background: var(--white); border-radius: 20px;
  width: 420px; box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
}
@keyframes modalIn { from { opacity: 0; transform: translateY(12px) scale(.97); } to { opacity: 1; transform: none; } }
.auth-modal-header {
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
}
.auth-modal-title { font-weight: 700; font-size: 18px; }
.auth-modal-close { background: none; border: none; cursor: pointer; color: var(--muted); font-size: 20px; }
.auth-modal-body { padding: 24px; }
.auth-btn {
  width: 100%; padding: 13px; background: var(--accent); color: #fff;
  border: none; border-radius: 12px; font-size: 15px; font-weight: 500;
  cursor: pointer; box-shadow: 0 4px 16px rgba(200,56,42,0.25); transition: background .18s;
}
.auth-btn:hover { background: #b02e22; }
.auth-field { margin-bottom: 16px; }
.auth-field label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.auth-field input {
  width: 100%; padding: 10px 14px; border: 1.5px solid var(--border); border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans SC', sans-serif; background: var(--bg);
  outline: none; color: var(--text); transition: border-color .15s; box-sizing: border-box;
}
.auth-field input:focus { border-color: var(--accent); }

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

.detail-img-wrap {
  position: relative; border-radius: 12px; overflow: hidden;
  background: var(--bg); cursor: pointer; margin-bottom: 20px;
}
.detail-img { width: 100%; height: 200px; object-fit: cover; display: block; }
.detail-img-placeholder {
  height: 200px; display: flex; align-items: center; justify-content: center;
}
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

/* 图片放大 */
.zoom-overlay {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(0,0,0,0.85); display: flex;
  align-items: center; justify-content: center;
  cursor: zoom-out; animation: fadeIn .2s ease;
}
.zoom-img {
  max-width: 90vw; max-height: 90vh; object-fit: contain;
  border-radius: 8px; animation: modalIn .25s ease;
}
</style>
