<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const records = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const confirmDialog = ref(false)
const currentId = ref(null)
const confirmForm = ref({ extraFee: 0 })
const currentReturn = computed(() => records.value.find(r => r.id === currentId.value))
const filterStatus = ref('')

const statusOptions = { PENDING: '待确认', CONFIRMED: '已确认' }
const carName = (row) => row.carInfo ? `${row.carInfo.brand} ${row.carInfo.model}` : '-'
const plateNo = (row) => row.carInfo?.plateNumber || '-'

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/return-orders', { params: { pageNum: currentPage.value, pageSize: pageSize.value, status: filterStatus.value || undefined } })
    records.value = page.records; total.value = page.total; summary.value = page.summary || {}
  } finally { loading.value = false }
}
const onFilterChange = () => { currentPage.value = 1; loadData() }
onMounted(loadData)

const openConfirm = (row) => { currentId.value = row.id; confirmForm.value = { extraFee: row.extraFee || 0 }; confirmDialog.value = true }
const doConfirm = async () => {
  await request.put(`/return-orders/${currentId.value}/confirm`, confirmForm.value)
  ElMessage.success('还车确认成功'); confirmDialog.value = false; loadData()
}
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="summary-strip">
        <span class="sum-item">全部 <strong>{{ total }}</strong></span>
        <span class="sum-item">待确认 <strong>{{ summary.pending ?? 0 }}</strong></span>
        <span class="sum-item">已确认 <strong>{{ summary.confirmed ?? 0 }}</strong></span>
      </div>
      <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="small" style="width:120px;" @change="onFilterChange">
        <el-option v-for="(label, key) in statusOptions" :key="key" :label="label" :value="key" />
      </el-select>
    </div>
    <div class="card">
      <el-table :data="records" v-loading="loading">
        <el-table-column label="车辆" min-width="180">
          <template #default="{row}"><div style="font-weight:500;">{{ carName(row) }}</div><div style="font-size:11px;color:var(--muted);">{{ plateNo(row) }}</div></template>
        </el-table-column>
        <el-table-column label="原租期" min-width="180">
          <template #default="{row}"><div>{{ row.rentOrderBrief?.rentDate }} 至 {{ row.rentOrderBrief?.expectedReturnDate }}</div><div style="font-size:11px;color:var(--muted);">共 {{ row.rentOrderBrief?.rentDays }} 天</div></template>
        </el-table-column>
        <el-table-column prop="actualReturnTime" label="还车时间" width="170"><template #default="{row}"><span class="font-mono">{{ row.actualReturnTime?.replace('T',' ') }}</span></template></el-table-column>
        <el-table-column prop="actualMileage" label="还车公里数" width="110"><template #default="{row}"><span class="font-mono">{{ row.actualMileage }} km</span></template></el-table-column>
        <el-table-column prop="damageDesc" label="损坏说明" min-width="120"><template #default="{row}">{{ row.damageDesc || '无' }}</template></el-table-column>
        <el-table-column label="附加费用" width="110"><template #default="{row}"><span class="font-mono text-accent">¥ {{ row.extraFee || 0 }}</span></template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}"><span class="status-badge" :class="row.status === 'PENDING' ? 'status-pending' : 'status-completed'">{{ row.status === 'PENDING' ? '待确认' : '已确认' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}"><button v-if="row.status === 'PENDING'" class="btn-sm btn-sm-primary" @click="openConfirm(row)">确认还车</button></template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <el-dialog v-model="confirmDialog" title="确认还车" width="480px">
      <div v-if="currentReturn" class="confirm-info">
        <div class="info-row"><span>车辆</span><strong>{{ carName(currentReturn) }} ({{ plateNo(currentReturn) }})</strong></div>
        <div class="info-row"><span>还车公里数</span><strong>{{ currentReturn.actualMileage }} km</strong></div>
        <div class="info-row"><span>损坏说明</span><strong>{{ currentReturn.damageDesc || '无' }}</strong></div>
      </div>
      <el-form label-width="80px" style="margin-top:16px;">
        <el-form-item label="附加费用"><el-input-number v-model="confirmForm.extraFee" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="confirmDialog = false">取消</button>
        <button class="btn-sm btn-sm-primary" @click="doConfirm">确认还车</button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.summary-strip { display: flex; gap: 16px; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
.confirm-info { padding: 16px; border-radius: 8px; background: var(--surface2); display: grid; gap: 12px; }
.info-row { display: flex; justify-content: space-between; align-items: center; }
.info-row span { font-size: 13px; color: var(--muted); }
</style>
