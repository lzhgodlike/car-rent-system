<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import request from '../../utils/request'

const router = useRouter()
let resizeObserver = null

const stats = ref({})
const chartData = ref({ dates: [], rentCounts: [], returnCounts: [], totalIncomeSeries: [], carStatus: [] })
const period = ref('day')
const range = ref('7d')
const rentChartRef = ref(null)
const statusChartRef = ref(null)
let rentChart = null
let statusChart = null

// 待处理事项弹窗
const pendingVisible = ref(false)
const pendingData = ref({ pendingPickups: [], pendingReturns: [], pendingFaults: [] })

const formattedIncome = computed(() => {
  const val = stats.value.totalIncome || 0
  return `¥ ${Number(val).toLocaleString()}`
})

const totalPending = computed(() => (stats.value.pendingPickupCount || 0) + (stats.value.pendingReturnCount || 0) + (stats.value.pendingFaultCount || 0))

const statusColorMap = { AVAILABLE: '#4caf7d', RESERVED: '#c8382a', RENTED: '#5b9cf6', AWAITING_REPAIR: '#f0a140', REPAIRING: '#e25c5c', DISABLED: '#7a7d88' }
const statusLabelMap = { AVAILABLE: '空闲', RESERVED: '已预订', RENTED: '租赁中', AWAITING_REPAIR: '待维修', REPAIRING: '维修中', DISABLED: '停用' }

const rangeOptions = computed(() => {
  if (period.value === 'month') return [{ label: '近6个月', value: '6m' }, { label: '近12个月', value: '12m' }, { label: '全部', value: 'all' }]
  if (period.value === 'year') return [{ label: '近3年', value: '3y' }, { label: '近5年', value: '5y' }, { label: '全部', value: 'all' }]
  return [{ label: '近7天', value: '7d' }, { label: '近30天', value: '30d' }, { label: '近90天', value: '90d' }, { label: '全部', value: 'all' }]
})

watch(period, () => {
  range.value = rangeOptions.value[0].value
  loadData()
})

const buildRentOption = () => ({
  tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: 'rgba(0,0,0,0.08)', textStyle: { color: '#1a1a1a' } },
  grid: { left: 12, right: 12, top: 36, bottom: 4, containLabel: true },
  xAxis: { type: 'category', data: chartData.value.dates, axisLine: { lineStyle: { color: 'rgba(0,0,0,0.08)' } }, axisLabel: { color: '#999', fontSize: 10 } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(0,0,0,0.04)' } }, axisLabel: { color: '#999', fontSize: 10 } },
  series: [
    { name: '租车', type: 'bar', data: chartData.value.rentCounts, barWidth: '35%', itemStyle: { color: '#c8382a', borderRadius: [3, 3, 0, 0] } },
    { name: '还车', type: 'bar', data: chartData.value.returnCounts, barWidth: '35%', itemStyle: { color: '#3a9e6e', borderRadius: [3, 3, 0, 0] } },
  ],
  legend: { top: 0, right: 0, textStyle: { color: '#999', fontSize: 11 }, itemWidth: 8, itemHeight: 8 },
})

const buildStatusOption = () => {
  const total = (chartData.value.carStatus || []).reduce((s, i) => s + i.value, 0)
  return {
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: 'rgba(0,0,0,0.08)', textStyle: { color: '#1a1a1a' } },
    graphic: [
      { type: 'text', left: 'center', top: '38%', style: { text: `${total}`, textAlign: 'center', fill: '#1a1a1a', fontSize: 24, fontWeight: 700, fontFamily: 'Bebas Neue, sans-serif' } },
      { type: 'text', left: 'center', top: '52%', style: { text: '辆', textAlign: 'center', fill: '#999', fontSize: 12 } },
    ],
    series: [{ type: 'pie', radius: ['42%', '72%'], center: ['50%', '45%'], avoidLabelOverlap: false, label: { show: false },
      data: (chartData.value.carStatus || []).map(i => ({ name: statusLabelMap[i.name] || i.name, value: i.value, itemStyle: { color: statusColorMap[i.name] || '#999' } })),
    }],
  }
}

const renderCharts = async () => {
  await nextTick()
  if (rentChartRef.value) { rentChart?.dispose(); rentChart = echarts.init(rentChartRef.value); rentChart.setOption(buildRentOption()) }
  if (statusChartRef.value) { statusChart?.dispose(); statusChart = echarts.init(statusChartRef.value); statusChart.setOption(buildStatusOption()) }
}

const handleResize = () => {
  rentChart?.resize()
  statusChart?.resize()
}

const loadData = async () => {
  stats.value = await request.get('/dashboard/overview')
  chartData.value = await request.get('/dashboard/charts', { params: { period: period.value, range: range.value } })
  await renderCharts()
}

const openPending = async () => {
  pendingData.value = await request.get('/dashboard/pending')
  pendingVisible.value = true
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
  // 监听容器大小变化（侧栏收起/展开时触发）
  nextTick(() => {
    const container = document.querySelector('.admin-content')
    if (container) {
      resizeObserver = new ResizeObserver(() => handleResize())
      resizeObserver.observe(container)
    }
  })
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  rentChart?.dispose()
  statusChart?.dispose()
})
</script>

<template>
  <div class="dashboard">
    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-top">
          <div class="stat-icon yellow"><el-icon size="18"><Van /></el-icon></div>
        </div>
        <div class="stat-val">{{ stats.carCount || 0 }}</div>
        <div class="stat-label">车辆总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-top">
          <div class="stat-icon green"><el-icon size="18"><Document /></el-icon></div>
        </div>
        <div class="stat-val">{{ stats.activeRentCount || 0 }}</div>
        <div class="stat-label">进行中订单</div>
      </div>
      <div class="stat-card">
        <div class="stat-top">
          <div class="stat-icon blue"><el-icon size="18"><Money /></el-icon></div>
        </div>
        <div class="stat-val">{{ formattedIncome }}</div>
        <div class="stat-label">累计营收</div>
      </div>
      <div class="stat-card stat-card-clickable" :class="{ 'stat-card-blink': totalPending > 0 }" @click="openPending">
        <div class="stat-top">
          <div class="stat-icon red"><el-icon size="18"><Warning /></el-icon></div>
          <span v-if="totalPending > 0" class="stat-remind"><el-icon size="12"><Bell /></el-icon></span>
        </div>
        <div class="stat-val">{{ totalPending }}</div>
        <div class="stat-label">待处理事项 <span v-if="totalPending > 0" class="stat-click-hint">点击查看</span></div>
      </div>
    </div>

    <!-- Charts -->
    <div class="chart-row">
      <div class="card">
        <div class="card-header">
          <span class="card-title">订单趋势</span>
          <div style="display:flex;gap:8px;">
            <el-select v-model="period" size="small" style="width:80px">
              <el-option label="按日" value="day" /><el-option label="按月" value="month" /><el-option label="按年" value="year" />
            </el-select>
            <el-select v-model="range" size="small" style="width:90px" @change="loadData">
              <el-option v-for="o in rangeOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>
        </div>
        <div class="card-body"><div ref="rentChartRef" style="height:220px;"></div></div>
      </div>
      <div class="card">
        <div class="card-header"><span class="card-title">车辆状态分布</span></div>
        <div class="card-body">
          <div ref="statusChartRef" style="height:180px;"></div>
          <div class="status-legend">
            <div v-for="item in (chartData.carStatus || [])" :key="item.name" class="legend-item">
              <span class="legend-dot" :style="{ background: statusColorMap[item.name] }"></span>
              <span class="legend-label">{{ statusLabelMap[item.name] || item.name }}</span>
              <span class="legend-val">{{ item.value }} 辆</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 待处理事项弹窗 -->
    <el-dialog v-model="pendingVisible" title="待处理事项" width="560px" destroy-on-close>
      <div class="pending-content">
        <div class="pending-section">
          <div class="pending-title"><el-icon><Van /></el-icon> 待取车 ({{ pendingData.pendingPickupCount || 0 }})</div>
          <div v-if="pendingData.pendingPickups?.length" class="pending-list">
            <div v-for="o in pendingData.pendingPickups" :key="o.id" class="pending-item">
              <span class="pending-info">{{ o.orderNo }}</span>
              <span class="pending-date">{{ o.rentDate }}</span>
              <button class="btn-sm btn-sm-primary" @click="pendingVisible = false; router.push('/admin/orders')">去处理</button>
            </div>
          </div>
          <div v-else class="pending-empty">暂无</div>
        </div>
        <div class="pending-section">
          <div class="pending-title"><el-icon><SwitchButton /></el-icon> 待确认还车 ({{ pendingData.pendingReturnCount || 0 }})</div>
          <div v-if="pendingData.pendingReturns?.length" class="pending-list">
            <div v-for="o in pendingData.pendingReturns" :key="o.id" class="pending-item">
              <span class="pending-info">{{ o.orderNo }}</span>
              <span class="pending-date">{{ o.expectedReturnDate }}</span>
              <button class="btn-sm btn-sm-primary" @click="pendingVisible = false; router.push('/admin/returns')">去处理</button>
            </div>
          </div>
          <div v-else class="pending-empty">暂无</div>
        </div>
        <div class="pending-section">
          <div class="pending-title"><el-icon><Warning /></el-icon> 待处理故障 ({{ pendingData.pendingFaultCount || 0 }})</div>
          <div v-if="pendingData.pendingFaults?.length" class="pending-list">
            <div v-for="f in pendingData.pendingFaults" :key="f.id" class="pending-item">
              <span class="pending-info">{{ f.faultContent }}</span>
              <span class="pending-tag" :class="f.faultStatus === 'PENDING' ? 'tag-pending' : 'tag-repairing'">{{ f.faultStatus === 'PENDING' ? '待处理' : '维修中' }}</span>
              <button class="btn-sm btn-sm-primary" @click="pendingVisible = false; router.push('/admin/fault-reports')">去处理</button>
            </div>
          </div>
          <div v-else class="pending-empty">暂无</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard { display: grid; gap: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 12px; padding: 18px 20px; transition: border-color .18s;
}
.stat-card:hover { border-color: var(--border-hover); }
.stat-card-clickable { cursor: pointer; transition: all .2s; }
.stat-card-clickable:hover { border-color: var(--accent); transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.stat-card-blink {
  border-color: var(--accent);
  animation: cardBlink 3.5s ease-in-out infinite;
}
@keyframes cardBlink {
  0% { box-shadow: 0 0 6px rgba(200,56,42,0.08); }
  14% { box-shadow: 0 0 24px rgba(200,56,42,0.4), 0 0 8px rgba(200,56,42,0.2); }
  71% { box-shadow: 0 0 24px rgba(200,56,42,0.4), 0 0 8px rgba(200,56,42,0.2); }
  86% { box-shadow: 0 0 6px rgba(200,56,42,0.08); }
  100% { box-shadow: 0 0 6px rgba(200,56,42,0.08); }
}
.stat-remind {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--accent); color: #fff;
  display: flex; align-items: center; justify-content: center;
  animation: bellShake .8s ease-in-out infinite;
}
@keyframes bellShake {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(12deg); }
  75% { transform: rotate(-12deg); }
}
.stat-click-hint {
  font-size: 10px; color: var(--accent); font-weight: 400;
  opacity: .7; margin-left: 4px;
}
.stat-top { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-icon {
  width: 38px; height: 38px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
}
.stat-icon.yellow { background: var(--accent-dim); color: var(--accent); }
.stat-icon.green { background: rgba(76,175,125,0.12); color: var(--success); }
.stat-icon.blue { background: rgba(91,156,246,0.12); color: var(--info); }
.stat-icon.red { background: rgba(226,92,92,0.12); color: var(--danger); }
.stat-val { font-size: 28px; font-weight: 700; margin-top: 12px; font-family: 'Bebas Neue', sans-serif; }
.stat-label { font-size: 12px; color: var(--muted); margin-top: 4px; }

.chart-row { display: grid; grid-template-columns: 5fr 3fr; gap: 20px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.card-title { font-weight: 500; font-size: 14px; }
.card-body { padding: 16px 20px; }

.status-legend { margin-top: 12px; }
.legend-item { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 12px; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-label { flex: 1; color: var(--muted); }
.legend-val { font-weight: 600; font-family: 'Bebas Neue', sans-serif; }

/* 待处理事项弹窗 */
.pending-content { max-height: 50vh; overflow-y: auto; }
.pending-section { margin-bottom: 20px; }
.pending-section:last-child { margin-bottom: 0; }
.pending-title { font-size: 14px; font-weight: 500; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.pending-title .el-icon { color: var(--accent); }
.pending-list { border: 1px solid var(--border); border-radius: 10px; overflow: hidden; }
.pending-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; font-size: 13px; border-bottom: 1px solid var(--border);
}
.pending-item:last-child { border-bottom: none; }
.pending-info { font-weight: 500; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pending-date { color: var(--muted); font-size: 12px; flex-shrink: 0; }
.pending-tag { font-size: 11px; padding: 2px 8px; border-radius: 10px; flex-shrink: 0; }
.tag-pending { background: rgba(196,154,60,0.12); color: var(--warning); }
.tag-repairing { background: rgba(91,156,246,0.12); color: var(--info); }
.pending-empty { text-align: center; padding: 16px; color: var(--muted); font-size: 13px; }
</style>
