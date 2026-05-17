<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import request from '../../utils/request'
import { getAuth, setAuth, clearAuth } from '../../utils/auth'

const router = useRouter()
const route = useRoute()
const auth = getAuth()
const userName = computed(() => auth?.userInfo?.realName || auth?.userInfo?.username || '用户')
const formRef = ref(null)
const form = ref({ realName: '', phone: '', idCard: '', gender: '' })
const formSnapshot = ref(null)
const pwdForm = ref({ oldPassword: '', password: '', confirmPassword: '' })
const editing = ref(false)
const changingPwd = ref(false)
const favorites = ref([])

const maskPhone = (v) => {
  if (!v || v.length < 7) return v || '—'
  return v.slice(0, 3) + ' **** ' + v.slice(-4)
}
const maskIdCard = (v) => {
  if (!v || v.length < 8) return v || '—'
  return v.slice(0, 4) + ' •••• •••• ' + v.slice(-4)
}

const validatePhone = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入手机号'))
  if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式不正确'))
  callback()
}
const validateIdCard = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入身份证号'))
  if (!/^\d{17}[\dXx]$/.test(value)) return callback(new Error('身份证号应为18位'))
  callback()
}

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  idCard: [{ required: true, validator: validateIdCard, trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const hasFieldValue = (v) => {
  if (typeof v === 'string') return v.trim().length > 0
  return Boolean(v)
}
const isVerified = computed(() => {
  return hasFieldValue(form.value.realName) && hasFieldValue(form.value.phone) && hasFieldValue(form.value.idCard)
})
const shouldOpenEdit = (value) => {
  if (Array.isArray(value)) return value.some(v => v === '1' || v === 'true')
  return value === '1' || value === 'true'
}
const clearOpenEditQuery = () => {
  if (!Object.prototype.hasOwnProperty.call(route.query, 'openEdit')) return
  const nextQuery = { ...route.query }
  delete nextQuery.openEdit
  router.replace({ path: route.path, query: nextQuery })
}
const openEditFromQuery = () => {
  if (!shouldOpenEdit(route.query.openEdit)) return
  startEdit()
  clearOpenEditQuery()
}

const loadData = async () => {
  const data = await request.get('/users/profile')
  form.value.realName = data.realName || ''
  form.value.phone = data.phone || ''
  form.value.idCard = data.idCard || ''
  form.value.gender = data.gender || ''
  pwdForm.value = { oldPassword: '', password: '', confirmPassword: '' }
  try {
    favorites.value = await request.get('/favorites')
  } catch {}
  openEditFromQuery()
}
onMounted(loadData)

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) { ElMessage.warning('请先完善必填信息'); return }
  try {
    await request.put('/users/profile', { ...form.value, password: '' })
    const latest = await request.get('/auth/me')
    const latestAuth = getAuth()
    setAuth({ ...latestAuth, userInfo: latest })
    editing.value = false
    ElMessage.success('信息已更新')
  } catch {}
}

const savePwd = async () => {
  if (!pwdForm.value.oldPassword) { ElMessage.warning('请输入原密码'); return }
  if (!pwdForm.value.password || pwdForm.value.password.length < 6) { ElMessage.warning('新密码至少6位'); return }
  if (pwdForm.value.password !== pwdForm.value.confirmPassword) { ElMessage.warning('两次密码不一致'); return }
  if (pwdForm.value.oldPassword === pwdForm.value.password) { ElMessage.warning('新密码不能与原密码相同'); return }
  try {
    await request.put('/users/profile', { ...form.value, password: pwdForm.value.password, oldPassword: pwdForm.value.oldPassword })
    ElMessage.success('密码已修改，请重新登录')
    clearAuth()
    router.replace('/home')
  } catch {}
}

const removeFavorite = async (carId) => {
  await request.delete(`/favorites/${carId}`)
  favorites.value = favorites.value.filter(c => c.id !== carId)
  ElMessage.success('已取消收藏')
}

const startEdit = () => {
  if (editing.value) return
  formSnapshot.value = { ...form.value }
  editing.value = true
}
const cancelEdit = () => {
  if (formSnapshot.value) Object.assign(form.value, formSnapshot.value)
  editing.value = false
}

const logout = () => { clearAuth(); router.replace('/home') }
</script>

<template>
  <div class="profile-page">

    <!-- ══════════ HERO ══════════ -->
    <div class="hero">
      <div class="hero-bg">
        <div class="hero-orb hero-orb-1"></div>
        <div class="hero-orb hero-orb-2"></div>
        <div class="hero-noise"></div>
      </div>

      <div class="hero-content">
        <div class="avatar-wrap">
          <div class="avatar-ring"></div>
          <div class="avatar">{{ userName.charAt(0) }}</div>
        </div>

        <div class="hero-info">
          <div class="hero-name">{{ userName }}</div>
          <div class="hero-meta">
            <span class="badge-member">
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none"><polygon points="5,1 6.5,3.8 9.5,4.3 7.25,6.5 7.8,9.5 5,8 2.2,9.5 2.75,6.5 0.5,4.3 3.5,3.8" fill="currentColor"/></svg>
              普通会员
            </span>
            <span class="hero-divider">·</span>
            <span class="hero-sub">注册用户</span>
          </div>
        </div>

        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-num">—</div>
            <div class="stat-label">累计订单</div>
          </div>
          <div class="stat-sep"></div>
          <div class="stat-item">
            <div class="stat-num">—</div>
            <div class="stat-label">累计消费</div>
          </div>
          <div class="stat-sep"></div>
          <div class="stat-item">
            <div class="stat-num gold">4.9</div>
            <div class="stat-label">信用评分</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════ GRID ══════════ -->
    <div class="pg-grid">

      <!-- 基本信息 -->
      <div class="card card--info">
        <div class="card-header">
          <span class="card-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
          </span>
          <span class="card-title">基本信息</span>
        </div>

        <div class="info-list">
          <div class="info-item">
            <span class="info-label">姓名</span>
            <span class="info-val">{{ form.realName || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">手机号</span>
            <span class="info-val mono">{{ maskPhone(form.phone) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">证件号</span>
            <span class="info-val mono">{{ maskIdCard(form.idCard) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">性别</span>
            <span class="info-val">{{ form.gender || '—' }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="action-btn action-btn--ghost" @click="startEdit">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/></svg>
            编辑信息
          </button>
          <button class="action-btn action-btn--ghost" @click="changingPwd = true">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            修改密码
          </button>
        </div>
      </div>

      <!-- 我的收藏 -->
      <div class="card card--fav">
        <div class="card-header">
          <span class="card-icon card-icon--gold">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          </span>
          <span class="card-title">我的收藏</span>
          <span class="card-badge" v-if="favorites.length > 0">{{ favorites.length }}</span>
        </div>

        <div v-if="favorites.length > 0" class="fav-list">
          <div v-for="car in favorites" :key="car.id" class="fav-item">
            <div class="fav-thumb">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M5 17H3a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v9a2 2 0 0 1-2 2h-2m-4 0a2 2 0 1 1-4 0 2 2 0 0 1 4 0Zm6 0a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z"/></svg>
            </div>
            <div class="fav-info">
              <div class="fav-name">{{ car.brand }} {{ car.model }}</div>
              <div class="fav-price">¥{{ car.dayPrice }}<span>/天</span></div>
            </div>
            <div class="fav-actions">
              <button
                class="action-btn action-btn--primary action-btn--xs"
                :disabled="car.status !== 'AVAILABLE'"
                @click="car.status === 'AVAILABLE' ? router.push({ path: '/book', query: { carId: car.id } }) : ElMessage.warning('该车辆当前不可用')"
              >预订</button>
              <button class="fav-del" @click="removeFavorite(car.id)">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" opacity=".3"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          <p>暂无收藏车辆</p>
        </div>
      </div>

      <!-- 账户设置 -->
      <div class="card card--settings">
        <div class="card-header">
          <span class="card-icon">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v2M12 20v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M2 12h2M20 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          </span>
          <span class="card-title">账户设置</span>
        </div>

        <div class="settings-list">
          <div class="setting-row">
            <div class="setting-left">
              <div class="setting-label">用户名</div>
              <div class="setting-val">{{ auth?.userInfo?.username }}</div>
            </div>
          </div>

          <div class="setting-row">
            <div class="setting-left">
              <div class="setting-label">角色</div>
              <div class="setting-val">
                <span class="role-tag" :class="auth?.userInfo?.role === 'ADMIN' ? 'role-tag--admin' : 'role-tag--user'">
                  {{ auth?.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}
                </span>
              </div>
            </div>
          </div>


          <div class="setting-row">
            <div class="setting-left">
              <div class="setting-label">实名认证</div>
              <div class="setting-desc">{{ isVerified ? '身份信息已核验' : '请完善证件信息' }}</div>
            </div>
            <span v-if="isVerified" class="verify-badge verify-badge--ok">
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
              已认证
            </span>
            <button v-else class="verify-badge verify-badge--pending" @click="startEdit">去认证</button>
          </div>
        </div>

        <button class="logout-btn" @click="logout">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          退出登录
        </button>
      </div>
    </div>

    <!-- ══════════ 编辑信息弹窗 ══════════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="editing" class="overlay" @click.self="cancelEdit">
          <div class="modal">
            <div class="modal-header">
              <div class="modal-title">编辑基本信息</div>
              <button class="modal-close" @click="cancelEdit">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <el-form ref="formRef" :model="form" :rules="rules" label-width="72px">
                <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
                <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" placeholder="请输入 11 位手机号" /></el-form-item>
                <el-form-item label="证件号" prop="idCard"><el-input v-model="form.idCard" placeholder="请输入 18 位身份证号" /></el-form-item>
                <el-form-item label="性别" prop="gender">
                  <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
                </el-form-item>
                <el-form-item style="margin-bottom:0">
                  <div class="modal-foot">
                    <button type="button" class="action-btn action-btn--ghost" @click="cancelEdit">取消</button>
                    <button type="button" class="action-btn action-btn--primary" @click="save">保存修改</button>
                  </div>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ══════════ 修改密码弹窗 ══════════ -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="changingPwd" class="overlay" @click.self="changingPwd = false">
          <div class="modal modal--sm">
          <div class="modal-header">
            <div class="modal-title">修改密码</div>
            <button class="modal-close" @click="changingPwd = false">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field">
              <label>原密码 <em>*</em></label>
              <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
            </div>
            <div class="field">
              <label>新密码 <em>*</em></label>
              <input v-model="pwdForm.password" type="password" placeholder="至少 6 位" />
            </div>
            <div class="field">
              <label>确认密码 <em>*</em></label>
              <input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" />
            </div>
            <div class="modal-foot">
              <button class="action-btn action-btn--ghost" @click="changingPwd = false">取消</button>
              <button class="action-btn action-btn--primary" @click="savePwd">确认修改</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
    </Teleport>

  </div>
</template>

<style scoped>
/* ══════════════════════════════════════
   TOKEN LAYER
══════════════════════════════════════ */
.profile-page {
  --red:    #c8382a;
  --red-lt: rgba(200,56,42,.08);
  --red-md: rgba(200,56,42,.14);
  --gold:   #c49a3c;
  --gold-lt:rgba(196,154,60,.12);
  --gold-md:rgba(196,154,60,.22);
  --green:  #3e9d6f;
  --green-lt:rgba(62,157,111,.1);

  --bg:     #f5f3f0;
  --surface:#ffffff;
  --border: rgba(0,0,0,.07);
  --border-strong: rgba(0,0,0,.12);
  --text:   #1a1510;
  --muted:  #7d7468;
  --muted-lt:#b0a99e;

  --r-card: 18px;
  --r-btn:  10px;
  --shadow: 0 2px 12px rgba(0,0,0,.06), 0 1px 3px rgba(0,0,0,.04);
  --shadow-lg: 0 20px 60px rgba(0,0,0,.18), 0 4px 16px rgba(0,0,0,.1);

  --font-display: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-ui: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --font-mono: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;

  font-family: var(--font-ui);
  color: var(--text);
  background: var(--bg);
  padding: 36px 44px 60px;
  max-width: 940px;
  margin: 0 auto;
  animation: pageFadeIn .4s ease both;
}

@keyframes pageFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: none; }
}

/* ══════════════════════════════════════
   HERO
══════════════════════════════════════ */
.hero {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  margin-bottom: 28px;
  background: #141010;
}
.hero-bg {
  position: absolute; inset: 0; overflow: hidden;
}
.hero-orb {
  position: absolute; border-radius: 50%;
  filter: blur(60px); opacity: .6;
}
.hero-orb-1 {
  width: 340px; height: 340px;
  background: radial-gradient(circle, rgba(200,56,42,.45), transparent 70%);
  top: -80px; left: -60px;
}
.hero-orb-2 {
  width: 280px; height: 280px;
  background: radial-gradient(circle, rgba(196,154,60,.35), transparent 70%);
  bottom: -60px; right: -40px;
}
.hero-noise {
  position: absolute; inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='1'/%3E%3C/svg%3E");
  opacity: .035; mix-blend-mode: overlay;
}

.hero-content {
  position: relative; z-index: 1;
  display: flex; align-items: center; gap: 28px;
  padding: 36px 40px;
}

/* Avatar */
.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar-ring {
  position: absolute; inset: -4px; border-radius: 50%;
  background: conic-gradient(from 0deg, var(--gold), var(--red), var(--gold));
  animation: spin 6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.avatar {
  position: relative; z-index: 1;
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, var(--red), var(--gold));
  border: 3px solid #141010;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-ui);
  font-size: 28px; font-weight: 700;
  color: #ffffff;
  box-shadow: 0 0 0 3px #141010;
}

/* Hero text */
.hero-info { flex: 1; min-width: 0; }
.hero-name {
  font-family: var(--font-display);
  font-size: 26px; font-weight: 700; color: #f5f0e8;
  letter-spacing: .02em; line-height: 1.2;
}
.hero-meta { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.badge-member {
  display: inline-flex; align-items: center; gap: 5px;
  background: linear-gradient(135deg, rgba(196,154,60,.2), rgba(196,154,60,.08));
  border: 1px solid rgba(196,154,60,.3);
  color: var(--gold); border-radius: 20px;
  padding: 4px 12px; font-size: 11px; font-weight: 500; letter-spacing: .04em;
}
.hero-divider { color: rgba(255,255,255,.2); }
.hero-sub { font-size: 12px; color: rgba(255,255,255,.35); }

/* Stats */
.hero-stats {
  display: flex; align-items: center; gap: 0;
  background: rgba(255,255,255,.04);
  border: 1px solid rgba(255,255,255,.08);
  border-radius: 16px; padding: 16px 28px;
}
.stat-item { text-align: center; padding: 0 20px; }
.stat-sep { width: 1px; height: 36px; background: rgba(255,255,255,.08); }
.stat-num {
  font-family: var(--font-mono); font-size: 22px;
  color: rgba(255,255,255,.85); font-weight: 400; letter-spacing: -.02em;
}
.stat-num.gold { color: var(--gold); }
.stat-label { font-size: 11px; color: rgba(255,255,255,.3); margin-top: 4px; letter-spacing: .04em; }

/* ══════════════════════════════════════
   GRID
══════════════════════════════════════ */
.pg-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.card--info     { grid-column: 1; }
.card--fav      { grid-column: 2; grid-row: 1 / 3; align-self: start; }
.card--settings { grid-column: 1; }

.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-card);
  padding: 24px;
  box-shadow: var(--shadow);
  display: flex; flex-direction: column;
  animation: cardIn .45s ease both;
}
.card--info     { animation-delay: .05s; }
.card--fav      { animation-delay: .10s; }
.card--settings { animation-delay: .15s; }

@keyframes cardIn {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: none; }
}

/* Card header */
.card-header {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 20px;
}
.card-icon {
  width: 28px; height: 28px; border-radius: 8px;
  background: var(--red-lt);
  display: flex; align-items: center; justify-content: center;
  color: var(--red); flex-shrink: 0;
}
.card-icon--gold { background: var(--gold-lt); color: var(--gold); }
.card-title { font-size: 13px; font-weight: 600; letter-spacing: .04em; color: var(--text); flex: 1; }
.card-badge {
  background: var(--gold-lt); color: var(--gold);
  border: 1px solid var(--gold-md);
  border-radius: 20px; padding: 1px 9px; font-size: 11px; font-weight: 600;
}

/* ── Info list ── */
.info-list { flex: 1; }
.info-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 11px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13.5px;
}
.info-item:last-child { border-bottom: none; }
.info-label { color: var(--muted); font-size: 12.5px; }
.info-val { font-weight: 500; color: var(--text); }
.info-val.mono { font-family: var(--font-mono); font-size: 12.5px; letter-spacing: .06em; }

/* Card actions */
.card-actions {
  display: flex; gap: 8px; margin-top: 20px;
  padding-top: 18px; border-top: 1px solid var(--border);
}
.card-actions .action-btn { flex: 1; justify-content: center; }

/* ── Favorites ── */
.fav-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
  max-height: 320px;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--border-strong) transparent;
}
.fav-list::-webkit-scrollbar { width: 4px; }
.fav-list::-webkit-scrollbar-track { background: transparent; }
.fav-list::-webkit-scrollbar-thumb { background: var(--border-strong); border-radius: 4px; }
.fav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  transition: background .15s;
}
.fav-item:last-child { border-bottom: none; }
.fav-thumb {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, var(--red-lt), var(--gold-lt));
  display: flex; align-items: center; justify-content: center;
  color: var(--muted); flex-shrink: 0;
}
.fav-info { flex: 1; min-width: 0; }
.fav-name { font-size: 13px; font-weight: 500; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fav-price { font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-top: 2px; }
.fav-price span { font-family: var(--font-ui); font-size: 11px; color: var(--muted); }
.fav-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.fav-del {
  width: 28px; height: 28px; border-radius: 8px; border: none;
  background: transparent; color: var(--muted-lt);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all .15s;
}
.fav-del:hover { background: var(--red-lt); color: var(--red); }

.empty-state {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
  padding: 40px 0; color: var(--muted);
}
.empty-state p { font-size: 13px; margin: 0; }

/* ── Settings ── */
.settings-list { flex: 1; }
.setting-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 13px 0;
  border-bottom: 1px solid var(--border);
}
.setting-row:last-child { border-bottom: none; }
.setting-label { font-size: 13px; font-weight: 500; color: var(--text); }
.setting-desc { font-size: 11.5px; color: var(--muted); margin-top: 2px; }

.role-tag {
  display: inline-flex; align-items: center;
  border-radius: 6px; padding: 3px 10px;
  font-size: 11.5px; font-weight: 600; letter-spacing: .04em;
}
.role-tag--admin { background: var(--gold-lt); color: var(--gold); border: 1px solid var(--gold-md); }
.role-tag--user  { background: var(--red-lt);  color: var(--red);  border: 1px solid var(--red-md);  }

/* Verify badge */
.verify-badge {
  display: inline-flex; align-items: center; gap: 5px;
  border-radius: 8px; padding: 4px 12px;
  font-size: 12px; font-weight: 600; letter-spacing: .02em;
}
.verify-badge--ok {
  background: var(--green-lt); color: var(--green);
  border: none;
}
.verify-badge--pending {
  background: var(--red-lt); color: var(--red);
  border: 1px solid var(--red-md);
  cursor: pointer; transition: all .15s;
}
.verify-badge--pending:hover { background: var(--red-md); }

.logout-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; margin-top: 20px; padding: 10px;
  border-radius: var(--r-btn); border: 1px solid var(--border-strong);
  background: transparent; color: var(--muted);
  font-family: var(--font-ui); font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all .15s;
}
.logout-btn:hover {
  border-color: var(--red); color: var(--red);
  background: var(--red-lt);
}

/* ══════════════════════════════════════
   BUTTONS
══════════════════════════════════════ */
.action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: var(--r-btn);
  font-family: var(--font-ui); font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all .15s; white-space: nowrap;
}
.action-btn--ghost {
  background: transparent;
  border: 1px solid var(--border-strong);
  color: var(--text);
}
.action-btn--ghost:hover { border-color: var(--text); background: rgba(0,0,0,.03); }
.action-btn--primary {
  background: var(--red); border: none; color: #fff;
}
.action-btn--primary:hover { background: #a82d21; }
.action-btn--primary:disabled { opacity: .4; cursor: not-allowed; }
.action-btn--xs { padding: 4px 11px; font-size: 11.5px; border-radius: 7px; }

/* ══════════════════════════════════════
   MODAL
══════════════════════════════════════ */
.overlay {
  position: fixed; inset: 0; z-index: 300;
  background: rgba(10,8,6,.55);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.modal {
  background: var(--surface); border-radius: 22px;
  width: 460px; max-width: calc(100vw - 32px);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border);
}
.modal--sm { width: 400px; }

.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 28px 20px;
  border-bottom: 1px solid var(--border);
}
.modal-title { font-family: var(--font-display); font-size: 18px; font-weight: 700; letter-spacing: .01em; }
.modal-close {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: transparent; color: var(--muted);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all .15s;
}
.modal-close:hover { background: var(--red-lt); color: var(--red); }
.modal-body { padding: 24px 28px 28px; }
.modal-foot { display: flex; justify-content: flex-end; gap: 8px; width: 100%; }

/* Form fields (password modal) */
.field { margin-bottom: 16px; }
.field label { display: block; font-size: 12px; font-weight: 500; color: var(--muted); margin-bottom: 6px; letter-spacing: .03em; }
.field label em { color: var(--red); font-style: normal; }
.field input {
  width: 100%; padding: 11px 14px;
  border: 1.5px solid var(--border-strong); border-radius: 10px;
  font-size: 14px; font-family: var(--font-ui);
  background: var(--bg); color: var(--text);
  outline: none; transition: border-color .15s; box-sizing: border-box;
}
.field input:focus { border-color: var(--red); background: var(--surface); }
.field input::placeholder { color: var(--muted-lt); }

/* el-form override */
:deep(.el-input__wrapper) {
  background: var(--bg) !important;
  border-radius: 10px !important;
  box-shadow: 0 0 0 1.5px var(--border-strong) !important;
  transition: box-shadow .15s !important;
}
:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--red) !important;
}
:deep(.el-input__inner) {
  font-family: var(--font-ui) !important;
  color: var(--text) !important;
}
:deep(.el-select__wrapper) {
  background: var(--bg) !important;
  border-radius: 10px !important;
  box-shadow: 0 0 0 1.5px var(--border-strong) !important;
}
:deep(.el-select__wrapper:hover),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1.5px var(--red) !important;
}
:deep(.el-form-item__label) {
  font-family: var(--font-ui) !important;
  font-size: 12.5px !important;
  color: var(--muted) !important;
}
:deep(.el-form-item.is-error .el-input__wrapper),
:deep(.el-form-item.is-error .el-select__wrapper) {
  box-shadow: 0 0 0 1.5px var(--el-color-danger) !important;
}

/* ══════════════════════════════════════
   MODAL TRANSITION (Teleport — needs :global)
══════════════════════════════════════ */
:global(.modal-enter-active),
:global(.modal-leave-active) { transition: opacity .2s ease; }
:global(.modal-enter-active .modal),
:global(.modal-leave-active .modal) { transition: transform .25s cubic-bezier(.34,1.2,.64,1), opacity .2s; }
:global(.modal-enter-from) { opacity: 0; }
:global(.modal-enter-from .modal) { transform: translateY(20px) scale(.96); opacity: 0; }
:global(.modal-leave-to) { opacity: 0; }
:global(.modal-leave-to .modal) { transform: translateY(8px) scale(.98); opacity: 0; }

/* ══════════════════════════════════════
   MODAL GLOBAL (Teleport to body)
══════════════════════════════════════ */
:global(.overlay) {
  position: fixed; inset: 0; z-index: 9000;
  background: rgba(10,8,6,.55);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
:global(.modal) {
  background: #ffffff; border-radius: 22px;
  width: 460px; max-width: calc(100vw - 32px);
  box-shadow: 0 20px 60px rgba(0,0,0,.18), 0 4px 16px rgba(0,0,0,.1);
  border: 1px solid rgba(0,0,0,.07);
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
:global(.modal--sm) { width: 400px; }
:global(.modal-header) {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 28px 20px;
  border-bottom: 1px solid rgba(0,0,0,.07);
}
:global(.modal-title) { font-size: 18px; font-weight: 700; color: #1a1510; }
:global(.modal-close) {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  background: transparent; color: #7d7468;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all .15s;
}
:global(.modal-close:hover) { background: rgba(200,56,42,.08); color: #c8382a; }
:global(.modal-body) { padding: 24px 28px 28px; }
:global(.modal-foot) { display: flex; justify-content: flex-end; gap: 8px; width: 100%; }

:global(.modal .field) { margin-bottom: 16px; }
:global(.modal .field label) { display: block; font-size: 12px; font-weight: 500; color: #7d7468; margin-bottom: 6px; }
:global(.modal .field label em) { color: #c8382a; font-style: normal; }
:global(.modal .field input) {
  width: 100%; padding: 11px 14px;
  border: 1.5px solid rgba(0,0,0,.12); border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  background: #f5f3f0; color: #1a1510;
  outline: none; transition: border-color .15s; box-sizing: border-box;
}
:global(.modal .field input:focus) { border-color: #c8382a; background: #fff; }
:global(.modal .field input::placeholder) { color: #b0a99e; }

:global(.modal .action-btn) {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 10px;
  font-family: 'Noto Sans SC', 'PingFang SC', sans-serif;
  font-size: 13px; font-weight: 500;
  cursor: pointer; transition: all .15s; white-space: nowrap;
}
:global(.modal .action-btn--ghost) {
  background: transparent; border: 1px solid rgba(0,0,0,.12); color: #1a1510;
}
:global(.modal .action-btn--ghost:hover) { border-color: #1a1510; background: rgba(0,0,0,.03); }
:global(.modal .action-btn--primary) { background: #c8382a; border: none; color: #fff; }
:global(.modal .action-btn--primary:hover) { background: #a82d21; }
</style>