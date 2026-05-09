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
const form = ref({ realName: '', phone: '', idCard: '', gender: '', password: '' })
const favorites = ref([])

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const loadData = async () => {
  const data = await request.get('/users/profile')
  Object.assign(form.value, data, { password: '' })
  try {
    favorites.value = await request.get('/favorites')
  } catch {}
}
onMounted(loadData)

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await request.put('/users/profile', form.value)
  const latest = await request.get('/auth/me')
  setAuth({ ...auth, userInfo: latest })
  ElMessage.success('已更新')
}

const removeFavorite = async (carId) => {
  await request.delete(`/favorites/${carId}`)
  favorites.value = favorites.value.filter(c => c.id !== carId)
  ElMessage.success('已取消收藏')
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
      <!-- Info -->
      <div class="pcard">
        <div class="pcard-title"><el-icon><User /></el-icon> 基本信息</div>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
          <el-form-item label="姓名" prop="realName"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="身份证"><el-input v-model="form.idCard" /></el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
          </el-form-item>
          <el-form-item label="新密码"><el-input v-model="form.password" type="password" show-password placeholder="不修改可留空" /></el-form-item>
          <el-form-item><el-button type="primary" @click="save">保存修改</el-button></el-form-item>
        </el-form>
      </div>

      <!-- Favorites -->
      <div class="pcard">
        <div class="pcard-title"><el-icon><Star /></el-icon> 我的收藏</div>
        <template v-if="favorites.length > 0">
          <div v-for="car in favorites" :key="car.id" class="info-row">
            <span>{{ car.brand }} {{ car.model }}</span>
            <div style="display:flex;align-items:center;gap:12px;">
              <span class="fav-price">¥{{ car.dayPrice }}/天</span>
              <el-icon class="fav-remove" @click="removeFavorite(car.id)"><Close /></el-icon>
            </div>
          </div>
        </template>
        <div v-else style="text-align:center;padding:24px;color:var(--muted);font-size:13px;">暂无收藏</div>
        <el-button style="width:100%;margin-top:16px;" @click="router.push('/book')">去预订</el-button>
      </div>

      <!-- Account -->
      <div class="pcard">
        <div class="pcard-title"><el-icon><Setting /></el-icon> 账户设置</div>
        <div class="info-row"><span class="label">用户名</span><span class="val">{{ auth?.userInfo?.username }}</span></div>
        <div class="info-row"><span class="label">角色</span><span class="val">{{ auth?.userInfo?.role === 'ADMIN' ? '管理员' : '普通用户' }}</span></div>
        <div class="info-row"><span class="label">消息通知</span><span class="val" style="color:var(--success);">已开启</span></div>
        <div class="info-row"><span class="label">实名认证</span><span class="val" style="color:var(--success);">已认证</span></div>
        <el-button type="danger" plain style="width:100%;margin-top:16px;" @click="logout">
          <el-icon><SwitchButton /></el-icon> 退出登录
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { padding: 32px 40px; max-width: 900px; margin: 0 auto; }

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

.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.pcard {
  background: var(--white); border: 1px solid var(--border); border-radius: var(--radius);
  padding: 20px 22px; box-shadow: var(--shadow-sm);
}
.pcard-title { font-size: 14px; font-weight: 500; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
.pcard-title .el-icon { color: var(--accent); }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border); font-size: 14px; }
.info-row:last-child { border-bottom: none; }
.info-row .label { color: var(--muted); font-size: 13px; }
.info-row .val { font-weight: 500; }
.fav-price { color: var(--accent); font-family: var(--font-mono); font-weight: 500; }
.fav-remove { cursor: pointer; color: var(--muted); transition: color .15s; }
.fav-remove:hover { color: var(--accent); }
</style>
