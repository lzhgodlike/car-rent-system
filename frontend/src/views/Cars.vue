<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const cars = ref([])
const carTypes = ref([])
const dialogVisible = ref(false)
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
  RENTED: '正在出行',
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
  cars.value = await request.get('/cars', { params: query })
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

const openAdd = () => {
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  currentId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
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
  dialogVisible.value = false
  await loadCars()
}

const removeCar = async (id) => {
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

    <div class="page-card">
      <!-- <div class="page-header compact-page-head"> -->
        <!-- <div>
          <h2 class="page-title">车辆信息管理</h2>
          <p class="page-desc">支持筛选车辆、查看展厅卡片，并继续进行新增、编辑或租车操作。</p>
        </div> -->
      <!-- </div> -->


      <!-- <div class="section-card"> -->
        <!-- <div class="section-head">
          <div>
            <h3>车辆展厅</h3>
            <p>图片化查看每辆车的品牌、编号、价格、里程和当前状态。</p>
          </div>
        </div> -->

        
      <div class="toolbar toolbar-card">
        <el-input v-model="query.brand" placeholder="按品牌查询" style="width: 220px" clearable />
        <el-select v-model="query.typeId" placeholder="车辆类型" style="width: 180px" clearable>
          <el-option v-for="item in carTypes" :key="item.id" :label="item.typeName" :value="item.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="车辆状态" style="width: 180px" clearable>
          <el-option label="空闲可租" value="AVAILABLE" />
          <el-option label="正在出行" value="RENTED" />
          <el-option label="检修中" value="MAINTENANCE" />
        </el-select>
        <el-button type="primary" plain @click="loadCars">查询</el-button>
        <el-button v-if="isAdmin" type="primary" @click="openAdd">新增车辆</el-button>
      </div>

        <div class="showcase-grid">
          <article v-for="car in cars" :key="car.id" class="car-showcase">
            <div class="car-media">
              <img :src="displayImage(car)" :alt="`${car.brand} ${car.model}`" />
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
              <div class="car-actions">
                <el-button v-if="!isAdmin && car.status === 'AVAILABLE'" type="primary" @click="openRent(car)">立即租车</el-button>
                <template v-if="isAdmin">
                  <el-button @click="openEdit(car)">编辑资料</el-button>
                  <el-button type="danger" plain @click="removeCar(car.id)">删除</el-button>
                </template>
              </div>
            </div>
          </article>
        </div>
      <!-- </div> -->
    </div>

    <el-dialog v-model="dialogVisible" :title="currentId ? '编辑车辆' : '新增车辆'" width="680px">
      <el-form ref="carFormRef" :model="form" :rules="carRules" label-width="96px">
        <el-form-item label="车辆编号" prop="carNo"><el-input v-model="form.carNo" placeholder="例如 CAR001" /></el-form-item>
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
            <el-option label="正在出行" value="RENTED" />
            <el-option label="检修中" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCar">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rentVisible" title="提交租车订单" width="520px">
      <el-form ref="rentFormRef" :model="rentForm" :rules="rentRules" label-width="100px">
        <el-form-item label="租车日期" prop="rentDate"><el-date-picker v-model="rentForm.rentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="预计还车" prop="expectedReturnDate"><el-date-picker v-model="rentForm.expectedReturnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="rentForm.remark" type="textarea" placeholder="例如：周末郊游、机场接送" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rentVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRent">确认租车</el-button>
      </template>
    </el-dialog>
  </div>
</template>


