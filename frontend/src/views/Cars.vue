<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const cars = ref([])
const carTypes = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailCar = ref(null)
const detailOrders = ref([])
const detailFaults = ref([])
const detailLoading = ref(false)
const rentVisible = ref(false)
const currentId = ref(null)
const carFormRef = ref(null)
const rentFormRef = ref(null)
const query = reactive({ brand: '', typeId: '', status: '' })
const form = reactive({
  carNo: '',
  typeId: '',
  brand: '',
  model: '',
  plateNumber: '',
  dayPrice: 0,
  mileage: 0,
  pickupAddress: '',
  carImage: '',
  status: 'AVAILABLE',
})
const rentForm = reactive({ carId: null, rentDate: '', expectedReturnDate: '', remark: '' })

const carNoPattern = /^[A-Z]{2,6}[0-9]{2,6}$/
const platePattern = /^([\u4e00-\u9fa5][A-Z][A-Z0-9]{5,6}|[A-Z]{3}[A-Z0-9]{5})$/
const imagePattern = /^(|https?:\/\/.+)$/

const statusMap = {
  AVAILABLE: '空闲可租',
  RENTED: '已出租',
  MAINTENANCE: '检修中',
}

const statusClassMap = {
  AVAILABLE: 'available',
  RENTED: 'rented',
  MAINTENANCE: 'maintenance',
}

const carRules = {
  carNo: [
    { required: true, message: '请输入车辆编号', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!carNoPattern.test(value || '')) {
          callback(new Error('车辆编号需为大写字母加数字，例如 CAR001'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  typeId: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  brand: [{ required: true, message: '请输入车辆品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入车辆型号', trigger: 'blur' }],
  plateNumber: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!platePattern.test(value || '')) {
          callback(new Error('请输入合法车牌号，例如 鲁A12345 或 LUA12345'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  dayPrice: [{ required: true, message: '请输入日租金', trigger: 'change' }],
  mileage: [{ required: true, message: '请输入公里数', trigger: 'change' }],
  carImage: [
    {
      validator: (_, value, callback) => {
        if (!imagePattern.test(value || '')) {
          callback(new Error('图片链接需为空或以 http/https 开头'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const rentRules = {
  rentDate: [{ required: true, message: '请选择租车日期', trigger: 'change' }],
  expectedReturnDate: [{ required: true, message: '请选择预计还车日期', trigger: 'change' }],
}

const formatCarStatus = (status) => statusMap[status] || status || '-'
const statusClass = (status) => statusClassMap[status] || 'available'
const availableCount = computed(() => cars.value.filter((item) => item.status === 'AVAILABLE').length)
const rentedCount = computed(() => cars.value.filter((item) => item.status === 'RENTED').length)
const maintenanceCount = computed(() => cars.value.filter((item) => item.status === 'MAINTENANCE').length)

const loadTypes = async () => {
  carTypes.value = await request.get('/car-types')
}

const loadCars = async () => {
  loading.value = true
  try {
    cars.value = await request.get('/cars', { params: query })
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    carNo: '',
    typeId: '',
    brand: '',
    model: '',
    plateNumber: '',
    dayPrice: 0,
    mileage: 0,
    pickupAddress: '',
    carImage: '',
    status: 'AVAILABLE',
  })
}

const nextCarNo = computed(() => {
  let max = 0
  cars.value.forEach((car) => {
    const match = String(car.carNo || '').match(/^([A-Z]+)(\d+)$/)
    if (match) {
      max = Math.max(max, Number(match[2]))
    }
  })
  return `CAR${String(max + 1).padStart(3, '0')}`
})

const openAdd = () => {
  currentId.value = null
  resetForm()
  form.carNo = nextCarNo.value
  dialogVisible.value = true
}

const openEdit = (row) => {
  currentId.value = row.id
  const { id, ...rest } = row
  Object.assign(form, rest)
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
}

const closeRentDialog = () => {
  rentVisible.value = false
}

const saveCar = async () => {
  await carFormRef.value.validate()
  if (currentId.value) {
    await request.put(`/cars/${currentId.value}`, form)
    ElMessage.success('车辆修改成功')
  } else {
    await request.post('/cars', form)
    ElMessage.success('车辆新增成功')
  }
  closeDialog()
  await loadCars()
}

const removeCar = async (id) => {
  await ElMessageBox.confirm('确定要删除这辆车吗？此操作不可撤销。', '删除确认', { type: 'warning' })
  await request.delete(`/cars/${id}`)
  ElMessage.success('删除成功')
  await loadCars()
}

const openRent = (row) => {
  rentForm.carId = row.id
  rentForm.rentDate = ''
  rentForm.expectedReturnDate = ''
  rentForm.remark = ''
  rentVisible.value = true
}

const submitRent = async () => {
  await rentFormRef.value.validate()
  await request.post('/rent-orders', rentForm)
  ElMessage.success('租车成功，祝你一路顺风')
  rentVisible.value = false
  await loadCars()
}

const placeholderImage = 'https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?auto=format&fit=crop&w=1200&q=80'
const carTypeName = (typeId) => carTypes.value.find((item) => item.id === typeId)?.typeName || '车型待定'
const displayImage = (car) => car.carImage || placeholderImage

const detailTotalIncome = computed(() =>
  detailOrders.value.reduce((sum, o) => sum + Number(o.totalPrice || 0), 0).toFixed(2),
)
const detailRentCount = computed(() => detailOrders.value.length)
const detailFaultCount = computed(() => detailFaults.value.length)
const detailResolvedFaults = computed(() => detailFaults.value.filter((f) => f.faultStatus === 'RESOLVED').length)

const orderStatusMap = { RENTED: '租赁中', RETURNED: '已归还' }
const faultStatusMap = { PENDING: '待处理', REPAIRING: '维修中', RESOLVED: '已修复' }
const formatOrderStatus = (s) => orderStatusMap[s] || s || '-'
const formatFaultStatus = (s) => faultStatusMap[s] || s || '-'
const formatMoney = (v) => `￥${Number(v || 0).toFixed(2)}`
const formatDateTime = (v) => (v ? String(v).replace('T', ' ') : '-')

const openDetail = async (car) => {
  detailCar.value = car
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [orders, faults] = await Promise.all([
      request.get('/rent-orders'),
      request.get('/fault-reports'),
    ])
    detailOrders.value = orders.filter((o) => o.carId === car.id)
    detailFaults.value = faults.filter((f) => f.carId === car.id)
  } finally {
    detailLoading.value = false
  }
}

const closeDetail = () => {
  detailVisible.value = false
}

onMounted(async () => {
  await loadTypes()
  await loadCars()
})
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Showroom</div>
      <h1 class="hero-title">车辆展厅</h1>
      <p class="hero-desc">在这里挑车、看图、比配置，也能快速判断车队当前是空闲充足、正在出行还是进入检修状态。</p>
      <div class="metric-strip">
        <div class="metric-pill">
          <span>当前车辆</span>
          <strong>{{ cars.length }}</strong>
        </div>
        <div class="metric-pill">
          <span>可用车辆</span>
          <strong>{{ availableCount }}</strong>
        </div>
        <div class="metric-pill">
          <span>出租中</span>
          <strong>{{ rentedCount }}</strong>
        </div>
        <div class="metric-pill">
          <span>检修中</span>
          <strong>{{ maintenanceCount }}</strong>
        </div>
      </div>
    </section>

    <div class="page-card" v-loading="loading">
      <div class="toolbar toolbar-card">
        <el-input v-model="query.brand" placeholder="按品牌查询" style="width: 220px" clearable />
        <el-select v-model="query.typeId" placeholder="车辆类型" style="width: 180px" clearable>
          <el-option v-for="item in carTypes" :key="item.id" :label="item.typeName" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="车辆状态" style="width: 180px" clearable>
          <el-option label="空闲可租" value="AVAILABLE" />
          <el-option label="已出租" value="RENTED" />
          <el-option label="检修中" value="MAINTENANCE" />
        </el-select>
        <el-button type="primary" plain @click="loadCars">查询</el-button>
        <el-button v-if="isAdmin" type="primary" @click="openAdd">新增车辆</el-button>
      </div>

        <div class="showcase-grid">
          <article v-for="car in cars" :key="car.id" class="car-showcase" @click="openDetail(car)">
            <div class="car-media">
              <img :src="displayImage(car)" :alt="`${car.brand} ${car.model}`" @error="(e) => e.target.src = placeholderImage" />
              <div class="car-media-badge">{{ carTypeName(car.typeId) }}</div>
            </div>
            <div class="car-body">
              <div class="car-title-row">
                <div class="car-title-block">
                  <h3 class="car-title">{{ car.brand }} {{ car.model }}</h3>
                  <div class="car-badges">
                    <div class="plate-chip">{{ car.plateNumber }}</div>
                    <span class="status-chip" :class="statusClass(car.status)">{{ formatCarStatus(car.status) }}</span>
                  </div>
                </div>
                <div class="price-chip">￥{{ car.dayPrice }}/天</div>
              </div>
              <div class="car-meta">
                <div class="car-meta-item">
                  <span>车辆编号</span>
                  <strong>{{ car.carNo }}</strong>
                </div>
                <div class="car-meta-item">
                  <span>公里数</span>
                  <strong>{{ car.mileage }} km</strong>
                </div>
                <div class="car-meta-item">
                  <span>取车地点</span>
                  <strong>{{ car.pickupAddress || '待补充' }}</strong>
                </div>
                <div class="car-meta-item">
                  <span>车型</span>
                  <strong>{{ carTypeName(car.typeId) }}</strong>
                </div>
              </div>
              <div class="car-actions" @click.stop>
                <el-button v-if="!isAdmin && car.status === 'AVAILABLE'" type="primary" @click="openRent(car)">立即租车</el-button>
                <template v-if="isAdmin">
                  <el-button @click="openEdit(car)">编辑资料</el-button>
                  <el-button type="danger" plain @click="removeCar(car.id)">删除</el-button>
                </template>
              </div>
            </div>
          </article>
        </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="currentId ? '编辑车辆' : '新增车辆'" width="680px" append-to-body>
      <el-form ref="carFormRef" :model="form" :rules="carRules" label-width="96px">
        <el-form-item label="车辆编号" prop="carNo"><el-input v-model="form.carNo" :readonly="!currentId" /></el-form-item>
        <el-form-item label="车辆类型" prop="typeId">
          <el-select v-model="form.typeId" style="width: 100%" placeholder="请选择车辆类型">
            <el-option v-for="item in carTypes" :key="item.id" :label="item.typeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车辆品牌" prop="brand"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="车辆型号" prop="model"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="车牌号" prop="plateNumber"><el-input v-model="form.plateNumber" placeholder="例如 鲁A12345 / LUA12345" /></el-form-item>
        <el-form-item label="日租金" prop="dayPrice"><el-input-number v-model="form.dayPrice" :min="0.01" :step="10" style="width: 100%" /></el-form-item>
        <el-form-item label="公里数" prop="mileage"><el-input-number v-model="form.mileage" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="取车地址"><el-input v-model="form.pickupAddress" /></el-form-item>
        <el-form-item label="图片链接" prop="carImage"><el-input v-model="form.carImage" placeholder="https://..." /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="空闲可租" value="AVAILABLE" />
            <el-option label="已出租" value="RENTED" />
            <el-option label="检修中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveCar">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="车辆详情" width="900px" append-to-body destroy-on-close>
      <div v-if="detailCar" v-loading="detailLoading">
        <div class="detail-top">
          <div class="detail-top-img">
            <img :src="displayImage(detailCar)" :alt="`${detailCar.brand} ${detailCar.model}`" @error="(e) => e.target.src = placeholderImage" />
          </div>
          <div class="detail-top-info">
            <h3>{{ detailCar.brand }} {{ detailCar.model }}</h3>
            <div class="detail-top-badges">
              <span class="plate-chip">{{ detailCar.plateNumber }}</span>
              <span class="status-chip" :class="statusClass(detailCar.status)">{{ formatCarStatus(detailCar.status) }}</span>
            </div>
            <div class="detail-top-meta">
              <div><span>车辆编号</span><strong>{{ detailCar.carNo }}</strong></div>
              <div><span>车型</span><strong>{{ carTypeName(detailCar.typeId) }}</strong></div>
              <div><span>日租金</span><strong>￥{{ detailCar.dayPrice }}/天</strong></div>
              <div><span>公里数</span><strong>{{ detailCar.mileage }} km</strong></div>
              <div><span>取车地点</span><strong>{{ detailCar.pickupAddress || '待补充' }}</strong></div>
            </div>
          </div>
        </div>

        <div class="detail-stats-row">
          <div class="detail-stat-card">
            <span>出租次数</span>
            <strong>{{ detailRentCount }}</strong>
          </div>
          <div class="detail-stat-card">
            <span>累计收入</span>
            <strong>￥{{ detailTotalIncome }}</strong>
          </div>
          <div class="detail-stat-card">
            <span>故障工单</span>
            <strong>{{ detailFaultCount }}</strong>
          </div>
          <div class="detail-stat-card">
            <span>已修复</span>
            <strong>{{ detailResolvedFaults }}</strong>
          </div>
        </div>

        <div class="detail-section">
          <h4>租车记录</h4>
          <el-table :data="detailOrders" stripe size="small" max-height="240">
            <el-table-column prop="orderNo" label="订单编号" min-width="140" />
            <el-table-column prop="rentDate" label="租车日期" width="110" />
            <el-table-column prop="expectedReturnDate" label="预计还车" width="110" />
            <el-table-column prop="rentDays" label="天数" width="70" />
            <el-table-column label="总租金" width="100">
              <template #default="{ row }">{{ formatMoney(row.totalPrice) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="status-badge" :class="row.orderStatus === 'RENTED' ? 'warning' : 'success'">
                  {{ formatOrderStatus(row.orderStatus) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="detailOrders.length === 0" class="detail-empty">暂无租车记录</div>
        </div>

        <div class="detail-section">
          <h4>维修记录</h4>
          <el-table :data="detailFaults" stripe size="small" max-height="200">
            <el-table-column prop="faultContent" label="故障内容" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="status-badge" :class="row.faultStatus === 'PENDING' ? 'warning' : row.faultStatus === 'REPAIRING' ? 'danger' : 'success'">
                  {{ formatFaultStatus(row.faultStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="reportTime" label="上报时间" width="170" />
            <el-table-column prop="handleResult" label="处理结果" min-width="140" />
          </el-table>
          <div v-if="detailFaults.length === 0" class="detail-empty">暂无维修记录</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeDetail">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rentVisible" title="提交租车订单" width="520px" append-to-body>
      <el-form ref="rentFormRef" :model="rentForm" :rules="rentRules" label-width="100px">
        <el-form-item label="租车日期" prop="rentDate"><el-date-picker v-model="rentForm.rentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="预计还车" prop="expectedReturnDate"><el-date-picker v-model="rentForm.expectedReturnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="rentForm.remark" type="textarea" placeholder="例如：周末郊游、机场接送" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeRentDialog">取消</el-button>
        <el-button type="primary" @click="submitRent">确认租车</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-top {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-top-img {
  border-radius: 18px;
  overflow: hidden;
  background: rgba(255, 248, 240, 0.92);
}

.detail-top-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.detail-top-info h3 {
  margin: 0 0 10px;
  font-size: 22px;
}

.detail-top-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.detail-top-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.detail-top-meta div {
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(191, 108, 47, 0.06);
}

.detail-top-meta span {
  display: block;
  font-size: 12px;
  color: var(--subtext);
}

.detail-top-meta strong {
  display: block;
  margin-top: 4px;
}

.detail-stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.detail-stat-card {
  padding: 14px;
  border-radius: 16px;
  text-align: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(252, 244, 236, 0.92));
  border: 1px solid rgba(191, 108, 47, 0.12);
}

.detail-stat-card span {
  display: block;
  font-size: 12px;
  color: var(--subtext);
}

.detail-stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--brand-deep);
}

.detail-section {
  margin-bottom: 18px;
}

.detail-section h4 {
  margin: 0 0 10px;
  font-size: 15px;
  color: var(--subtext);
}

.detail-empty {
  padding: 20px;
  text-align: center;
  color: var(--subtext);
  font-size: 13px;
  border: 1px dashed rgba(191, 108, 47, 0.2);
  border-radius: 14px;
  background: rgba(255, 249, 242, 0.5);
}

@media (max-width: 680px) {
  .detail-top {
    grid-template-columns: 1fr;
  }

  .detail-stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-top-meta {
    grid-template-columns: 1fr;
  }
}
</style>
