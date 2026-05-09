<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const cars = ref([])
const carTypes = ref([])
const brands = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentId = ref(null)
const formRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const query = ref({ brand: '', typeId: '', status: '', sort: '' })

const form = ref({ carNo: '', typeId: '', brand: '', model: '', plateNumber: '', dayPrice: 0, mileage: 0, pickupAddress: '', carImage: '' })

const statusMap = { AVAILABLE: '空闲', RESERVED: '已预订', RENTED: '租赁中', AWAITING_REPAIR: '待维修', REPAIRING: '维修中', DISABLED: '停用' }
const statusClass = { AVAILABLE: 'status-available', RESERVED: 'status-reserved', RENTED: 'status-rented', AWAITING_REPAIR: 'status-awating-repair', REPAIRING: 'status-repairing', DISABLED: 'status-disabled' }

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/cars', { params: { ...query.value, pageNum: currentPage.value, pageSize: pageSize.value } })
    cars.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
const loadTypes = async () => { carTypes.value = await request.get('/car-types') }
const loadBrands = async () => { brands.value = await request.get('/cars/brands') }

watch(query, () => { currentPage.value = 1; loadData() }, { deep: true })
onMounted(async () => { await Promise.all([loadTypes(), loadBrands()]); loadData() })

const openAdd = () => { currentId.value = null; form.value = { carNo: '', typeId: '', brand: '', model: '', plateNumber: '', dayPrice: 0, mileage: 0, pickupAddress: '', carImage: '' }; dialogVisible.value = true }
const openEdit = (row) => { currentId.value = row.id; const { id, rentCount, totalIncome, ...rest } = row; form.value = { ...rest }; dialogVisible.value = true }
const saveCar = async () => {
  await formRef.value?.validate()
  if (currentId.value) { await request.put(`/cars/${currentId.value}`, form.value); ElMessage.success('修改成功') }
  else { await request.post('/cars', form.value); ElMessage.success('新增成功') }
  dialogVisible.value = false; loadData()
}
const removeCar = async (id) => {
  await ElMessageBox.confirm('确定要删除这辆车吗？', '删除确认', { type: 'warning' })
  await request.delete(`/cars/${id}`); ElMessage.success('删除成功'); loadData()
}
const disableCar = async (id) => { await request.put(`/cars/${id}/disable`); ElMessage.success('已停用'); loadData() }
const enableCar = async (id) => { await request.put(`/cars/${id}/enable`); ElMessage.success('已启用'); loadData() }
const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="filter-group">
        <el-select v-model="query.brand" placeholder="品牌" clearable size="small" style="width:140px">
          <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
        </el-select>
        <el-select v-model="query.typeId" placeholder="类型" clearable size="small" style="width:120px">
          <el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable size="small" style="width:120px">
          <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
        </el-select>
      </div>
      <el-button type="primary" size="small" @click="openAdd"><el-icon><Plus /></el-icon> 添加车辆</el-button>
    </div>

    <div class="card">
      <el-table :data="cars" v-loading="loading">
        <el-table-column label="车辆信息" min-width="200">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="car-thumb">
                <img v-if="row.carImage" :src="row.carImage" :alt="`${row.brand} ${row.model}`" @error="(e) => e.target.style.display='none'" />
                <el-icon v-else size="20"><Van /></el-icon>
              </div>
              <div>
                <div style="font-weight:500;">{{ row.brand }} {{ row.model }}</div>
                <div style="font-size:11px;color:var(--muted);">{{ row.carNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="plateNumber" label="车牌号" width="120">
          <template #default="{ row }"><span class="font-mono">{{ row.plateNumber }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="90"><template #default="{ row }">{{ carTypeName(row.typeId) }}</template></el-table-column>
        <el-table-column label="日租金" width="100"><template #default="{ row }"><span class="font-mono text-accent">¥ {{ row.dayPrice }}/天</span></template></el-table-column>
        <el-table-column label="里程" width="100"><template #default="{ row }"><span class="font-mono">{{ row.mileage }} km</span></template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><span class="status-badge" :class="statusClass[row.status]">{{ statusMap[row.status] || row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'AVAILABLE'" size="small" type="warning" plain @click="disableCar(row.id)">停用</el-button>
            <el-button v-else-if="row.status === 'DISABLED'" size="small" type="success" plain @click="enableCar(row.id)">启用</el-button>
            <el-button size="small" type="danger" plain @click="removeCar(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="currentId ? '编辑车辆' : '新增车辆'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="车辆编号" prop="carNo" :rules="[{required:true,message:'必填'}]"><el-input v-model="form.carNo" /></el-form-item>
        <el-form-item label="类型" prop="typeId" :rules="[{required:true,message:'必填'}]">
          <el-select v-model="form.typeId" style="width:100%"><el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" /></el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand" :rules="[{required:true,message:'必填'}]"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号" prop="model" :rules="[{required:true,message:'必填'}]"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="车牌号" prop="plateNumber" :rules="[{required:true,message:'必填'}]"><el-input v-model="form.plateNumber" /></el-form-item>
        <el-form-item label="日租金" prop="dayPrice" :rules="[{required:true,message:'必填'}]"><el-input-number v-model="form.dayPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="里程"><el-input-number v-model="form.mileage" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="取车地址"><el-input v-model="form.pickupAddress" /></el-form-item>
        <el-form-item label="图片链接"><el-input v-model="form.carImage" placeholder="https://..." /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCar">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.filter-group { display: flex; gap: 8px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.car-thumb { width: 40px; height: 40px; border-radius: 8px; background: var(--surface2); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; color: var(--muted); overflow: hidden; }
.car-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; border-radius: inherit; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
</style>
