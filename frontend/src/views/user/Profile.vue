<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { getAuth, setAuth, clearAuth } from '../../utils/auth'

const router = useRouter()
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
  if (!v || v.length < 7) return v || '-'
  return v.slice(0, 3) + '****' + v.slice(-4)
}
const maskIdCard = (v) => {
  if (!v || v.length < 8) return v || '-'
  return v.slice(0, 4) + '****' + v.slice(-4)
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

const loadData = async () => {
  const data = await request.get('/users/profile')
  Object.assign(form.value, data)
  pwdForm.value = { oldPassword: '', password: '', confirmPassword: '' }
  try {
    favorites.value = await request.get('/favorites')
  } catch {}
}
onMounted(loadData)

const save = async () => {
  if (!form.value.realName) { ElMessage.warning('请输入姓名'); return }
  if (!/^1[3-9]\d{9}$/.test(form.value.phone || '')) { ElMessage.warning('请输入正确的手机号'); return }
  if (!/^\d{17}[\dXx]$/.test(form.value.idCard || '')) { ElMessage.warning('请输入正确的身份证号'); return }
  if (!form.value.gender) { ElMessage.warning('请选择性别'); return }
  try {
    await request.put('/users/profile', { ...form.value, password: '' })
    const latest = await request.get('/auth/me')
    setAuth({ ...auth, userInfo: latest })
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
    <!-- Hero -->
    <div class="profile-hero">
      <div class="profile-avatar">{{ userName.charAt(0) }}</div>
      <div class="profile-info">
        <div class="profile-name">{{ userName }}</div>
        <div class="profile-since">注册用户</div>
        <div class="profile-level"><el-icon size="13"><Star /></el-icon> 普通会员</div>
      </div>
      <div class="profile-stats">
        <div><div class="p-stat-num">-</div><div class="p-stat-label">累计订单</div></div>
        <div><div class="p-stat-num">-</div><div class="p-stat-label">累计消费</div></div>
        <div><div class="p-stat-num">4.9</div><div class="p-stat-label">信用评分</div></div>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 基本信息 -->
      <div class="pcard pcard-fixed">
        <div class="pcard-title"><el-icon><User /></el-icon> 基本信息</div>
        <template v-if="!editing">
          <div class="info-row"><span class="label">姓名</span><span class="val">{{ form.realName || '-' }}</span></div>
          <div class="info-row"><span class="label">手机号</span><span class="val">{{ maskPhone(form.phone) }}</span></div>
          <div class="info-row"><span class="label">证件号</span><span class="val">{{ maskIdCard(form.idCard) }}</span></div>
          <div class="info-row"><span class="label">性别</span><span class="val">{{ form.gender || '-' }}</span></div>
          <div class="btn-row">
            <button class="btn-sm btn-sm-outline" @click="startEdit"><el-icon><Edit /></el-icon> 编辑信息</button>
            <button class="btn-sm btn-sm-outline" @click="changingPwd = true"><el-icon><Lock /></el-icon> 修改密码</button>
          </div>
        </template>
        <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="70px">
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="证件号" prop="idCard"><el-input v-model="form.idCard" /></el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
          </el-form-item>
          <el-form-item>
            <button type="button" class="btn-sm btn-sm-primary" @click="save">保存修改</button>
            <button type="button" class="btn-sm btn-sm-outline" style="margin-left:8px;" @click="cancelEdit">取消</button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 我的收藏 -->
      <div class="pcard">
        <div class="pcard-title"><el-icon><Star /></el-icon> 我的收藏</div>
        <template v-if="favorites.length > 0">
          <div v-for="car in favorites" :key="car.id" class="info-row">
            <span>{{ car.brand }} {{ car.model }}</span>
            <div style="display:flex;align-items:center;gap:8px;">
              <span class="fav-price">¥{{ car.dayPrice }}/天</span>
              <button
                class="btn-sm btn-sm-outline btn-sm-xs"
                :disabled="car.status !== 'AVAILABLE'"
                @click="car.status === 'AVAILABLE' ? router.push({ path: '/book', query: { carId: car.id } }) : ElMessage.warning('该车辆当前不可用')"
              >去预订</button>
              <el-icon class="fav-remove" @click="removeFavorite(car.id)"><Close /></el-icon>
            </div>
          </div>
        </template>
        <div v-else class="empty-hint">暂无收藏</div>
      </div>

      <!-- 账户设置 -->
      <div class="pcard">
        <div class="pcard-title"><el-icon><Setting /></el-icon> 账户设置</div>
        <div class="info-row"><span class="label">用户名</span><span class="val">{{ auth?.userInfo?.username }}</span></div>
        <div class="info-row"><span class="label">角色</span><span class="val">{{ auth?.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}</span></div>
        <div class="info-row"><span class="label">消息通知</span><span class="val" style="color:var(--success);">已开启</span></div>
        <div class="info-row"><span class="label">实名认证</span><span class="val" style="color:var(--success);">已认证</span></div>
        <button class="btn-sm btn-sm-danger" style="margin-top:14px;width:100%;" @click="logout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </button>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="changingPwd" class="detail-overlay" @click.self="changingPwd = false">
      <div class="detail-modal">
        <div class="detail-header">
          <div class="detail-title">修改密码</div>
          <button class="detail-close" @click="changingPwd = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="detail-body">
          <div class="return-field">
            <label>原密码 <span style="color:var(--accent);">*</span></label>
            <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
          </div>
          <div class="return-field">
            <label>新密码 <span style="color:var(--accent);">*</span></label>
            <input v-model="pwdForm.password" type="password" placeholder="至少6位" />
          </div>
          <div class="return-field">
            <label>确认密码 <span style="color:var(--accent);">*</span></label>
            <input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" />
          </div>
          <div class="return-actions">
            <button class="btn-sm btn-sm-outline" @click="changingPwd = false">取消</button>
            <button class="btn-sm btn-sm-primary" @click="savePwd">确认修改</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { padding: 32px 40px; max-width: 900px; margin: 0 auto; }

/* Hero */
.profile-hero {
  background: linear-gradient(135deg, #1a1a1a 0%, #2d1f0e 100%);
  border-radius: 20px; padding: 32px; margin-bottom: 24px;
  display: flex; gap: 24px; align-items: center; position: relative; overflow: hidden;
}
.profile-hero::before {
  content: ''; position: absolute; right: -20px; bottom: -20px;
  width: 200px; height: 200px; border-radius: 50%;
  background: rgba(200,56,42,0.12);
}
.profile-avatar {
  width: 72px; height: 72px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--gold));
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; color: #fff; font-weight: 700;
  border: 3px solid rgba(255,255,255,0.2); flex-shrink: 0; position: relative; z-index: 1;
}
.profile-name { font-size: 24px; font-weight: 700; color: #fff; position: relative; z-index: 1; }
.profile-since { font-size: 13px; color: rgba(255,255,255,0.5); margin-top: 4px; position: relative; z-index: 1; }
.profile-level {
  display: inline-flex; align-items: center; gap: 6px;
  background: rgba(196,154,60,0.2); border: 1px solid rgba(196,154,60,0.3);
  color: var(--gold); border-radius: 20px; padding: 4px 12px; font-size: 12px;
  margin-top: 8px; position: relative; z-index: 1;
}
.profile-stats { display: flex; gap: 24px; margin-left: auto; text-align: center; position: relative; z-index: 1; }
.p-stat-num { font-family: var(--font-mono); font-size: 26px; color: #fff; font-weight: 500; }
.p-stat-label { font-size: 11px; color: rgba(255,255,255,0.5); margin-top: 2px; }

/* Grid */
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.pcard {
  background: var(--white); border: 1px solid var(--border); border-radius: var(--radius);
  padding: 20px 22px; box-shadow: var(--shadow-sm);
}
.pcard-fixed { min-height: 240px; }
.pcard-title { font-size: 14px; font-weight: 500; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.pcard-title .el-icon { color: var(--accent); }

/* Info rows */
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border); font-size: 14px; }
.info-row:last-child { border-bottom: none; }
.info-row .label { color: var(--muted); font-size: 13px; }
.info-row .val { font-weight: 500; }
.empty-hint { text-align: center; padding: 24px; color: var(--muted); font-size: 13px; }

/* Favorites */
.fav-price { color: var(--accent); font-family: var(--font-mono); font-weight: 500; }
.fav-remove { cursor: pointer; color: var(--muted); transition: color .15s; }
.fav-remove:hover { color: var(--accent); }

/* Buttons — 对齐设计稿 */
.btn-sm {
  padding: 7px 16px; border-radius: 8px; font-size: 13px;
  font-family: 'Noto Sans SC', sans-serif; cursor: pointer; transition: all .15s;
  display: inline-flex; align-items: center; gap: 6px;
}
.btn-sm-outline { background: none; border: 1px solid var(--border-dark); color: var(--text); }
.btn-sm-outline:hover { border-color: var(--text); }
.btn-sm-primary { background: var(--accent); border: none; color: #fff; }
.btn-sm-primary:hover { background: #b02e22; }
.btn-sm-danger { background: var(--accent-light); border: 1px solid transparent; color: var(--accent); }
.btn-sm-danger:hover { background: var(--accent-mid); }
.btn-sm-xs { padding: 3px 10px; font-size: 11px; }
.btn-sm-xs:disabled { opacity: .4; cursor: not-allowed; border-color: var(--border); color: var(--muted); }

.btn-row { display: flex; gap: 8px; margin-top: 14px; }
.btn-row .btn-sm { flex: 1; justify-content: center; }

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
  width: 400px; box-shadow: var(--shadow-lg); animation: modalIn .25s ease;
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
.return-field { margin-bottom: 16px; }
.return-field label { display: block; font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.return-field input {
  width: 100%; padding: 10px 14px; border: 1.5px solid var(--border); border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans SC', sans-serif; background: var(--bg);
  outline: none; color: var(--text); transition: border-color .15s; box-sizing: border-box;
}
.return-field input:focus { border-color: var(--accent); }
.return-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 20px; }
</style>
