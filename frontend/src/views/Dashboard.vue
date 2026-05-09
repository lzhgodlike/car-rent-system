<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'

const stats = ref({})
const chartData = ref({ dates: [], rentCounts: [], returnCounts: [], totalIncomeSeries: [], carStatus: [] })
const period = ref('day')
const range = ref('7d')

const rentReturnChartRef = ref(null)
const carStatusChartRef = ref(null)

let rentReturnChart = null
let carStatusChart = null

const statusLabelMap = {
  AVAILABLE: '空闲可租',
  RENTED: '已出租',
  MAINTENANCE: '检修中',
}

const statusColorMap = {
  AVAILABLE: '#16a34a',
  RENTED: '#f59e0b',
  MAINTENANCE: '#dc2626',
}

const formattedIncome = computed(() => Number(stats.value.totalIncome || 0).toFixed(2))

const rentalRate = computed(() => {
  const total = stats.value.carCount || 0
  const rented = stats.value.rentedCarCount || 0
  return total > 0 ? Math.round((rented / total) * 100) : 0
})

const rangeOptions = computed(() => {
  if (period.value === 'month') {
    return [
      { label: '近6个月', value: '6m' },
      { label: '近12个月', value: '12m' },
      { label: '全部', value: 'all' },
    ]
  }
  if (period.value === 'year') {
    return [
      { label: '近3年', value: '3y' },
      { label: '近5年', value: '5y' },
      { label: '全部', value: 'all' },
    ]
  }
  return [
    { label: '近7天', value: '7d' },
    { label: '近30天', value: '30d' },
    { label: '近90天', value: '90d' },
    { label: '全部', value: 'all' },
  ]
})

const ensureRange = () => {
  const values = rangeOptions.value.map((item) => item.value)
  if (!values.includes(range.value)) {
    range.value = values[0]
  }
}

const buildRentReturnOption = () => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 12, right: 12, top: 36, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    data: chartData.value.dates || [],
    axisLine: { lineStyle: { color: '#e7e5e4' } },
    axisLabel: { color: '#78716c', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f5f5f4' } },
    axisLabel: { color: '#a8a29e', fontSize: 11 },
  },
  series: [
    {
      name: '租车',
      type: 'bar',
      data: chartData.value.rentCounts || [],
      barWidth: '35%',
      itemStyle: { color: '#b45309', borderRadius: [3, 3, 0, 0] },
    },
    {
      name: '还车',
      type: 'bar',
      data: chartData.value.returnCounts || [],
      barWidth: '35%',
      itemStyle: { color: '#86efac', borderRadius: [3, 3, 0, 0] },
    },
  ],
  legend: {
    top: 0,
    right: 0,
    textStyle: { color: '#78716c', fontSize: 11 },
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 16,
  },
})

const buildCarStatusOption = () => {
  const total = (chartData.value.carStatus || []).reduce((sum, item) => sum + item.value, 0)
  return {
    tooltip: { trigger: 'item' },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '40%',
        style: {
          text: `${total}`,
          textAlign: 'center',
          fill: '#292524',
          fontSize: 22,
          fontWeight: 700,
        },
      },
      {
        type: 'text',
        left: 'center',
        top: '52%',
        style: {
          text: '辆',
          textAlign: 'center',
          fill: '#78716c',
          fontSize: 12,
        },
      },
    ],
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: { show: false },
        data: (chartData.value.carStatus || []).map((item) => ({
          name: statusLabelMap[item.name] || item.name,
          value: item.value,
          itemStyle: { color: statusColorMap[item.name] || '#a8a29e' },
        })),
      },
    ],
  }
}

const renderCharts = async () => {
  await nextTick()
  if (rentReturnChartRef.value) {
    rentReturnChart?.dispose()
    rentReturnChart = echarts.init(rentReturnChartRef.value)
    rentReturnChart.setOption(buildRentReturnOption())
  }
  if (carStatusChartRef.value) {
    carStatusChart?.dispose()
    carStatusChart = echarts.init(carStatusChartRef.value)
    carStatusChart.setOption(buildCarStatusOption())
  }
}

const handleResize = () => {
  rentReturnChart?.resize()
  carStatusChart?.resize()
}

const loadCharts = async () => {
  chartData.value = await request.get('/dashboard/charts', {
    params: { period: period.value, range: range.value },
  })
  await renderCharts()
}

const handlePeriodChange = async () => {
  ensureRange()
  await loadCharts()
}

const handleRangeChange = async () => {
  await loadCharts()
}

onMounted(async () => {
  stats.value = await request.get('/dashboard/overview')
  ensureRange()
  await loadCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  rentReturnChart?.dispose()
  carStatusChart?.dispose()
})
</script>

<template>
  <div class="dash-page">
    <div class="top-row">
      <div>
        <h1>运营看板</h1>
        <p>订单、收入和车队状态概览</p>
      </div>
      <div class="top-actions">
        <el-select v-model="period" size="small" style="width:90px" @change="handlePeriodChange">
          <el-option label="按日" value="day" />
          <el-option label="按月" value="month" />
          <el-option label="按年" value="year" />
        </el-select>
        <el-select v-model="range" size="small" style="width:100px" @change="handleRangeChange">
          <el-option v-for="item in rangeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
    </div>

    <div class="stats">
      <div class="stat">
        <div class="stat-label">总订单</div>
        <div class="stat-val">{{ stats.rentOrderCount || 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">总收入</div>
        <div class="stat-val">￥{{ formattedIncome }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">出租率</div>
        <div class="stat-val">{{ rentalRate }}%</div>
        <div class="stat-note">{{ stats.rentedCarCount || 0 }} / {{ stats.carCount || 0 }} 辆在租</div>
      </div>
      <div class="stat">
        <div class="stat-label">用户数</div>
        <div class="stat-val">{{ stats.userCount || 0 }}</div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-box">
        <div class="chart-box-title">订单趋势</div>
        <div ref="rentReturnChartRef" class="chart-canvas"></div>
      </div>
      <div class="chart-box">
        <div class="chart-box-title">车辆状态分布</div>
        <div ref="carStatusChartRef" class="chart-canvas-sm"></div>
        <div class="status-legend">
          <div class="status-legend-item" v-for="item in (chartData.carStatus || [])" :key="item.name">
            <span class="status-dot" :style="{ background: statusColorMap[item.name] }"></span>
            <span class="status-legend-label">{{ statusLabelMap[item.name] || item.name }}</span>
            <span class="status-legend-val">{{ item.value }} 辆</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dash-page {
  display: grid;
  gap: 20px;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-row h1 {
  font-size: 20px;
  font-weight: 700;
  color: var(--gray-900);
  margin: 0;
}

.top-row p {
  font-size: 13px;
  color: var(--gray-500);
  margin: 2px 0 0;
}

.top-actions {
  display: flex;
  gap: 8px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat {
  background: var(--white);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
}

.stat-label {
  font-size: 12px;
  color: var(--gray-500);
}

.stat-val {
  font-size: 24px;
  font-weight: 700;
  color: var(--gray-900);
  margin: 4px 0 2px;
}

.stat-note {
  font-size: 11px;
  color: var(--gray-400);
}

.chart-row {
  display: grid;
  grid-template-columns: 5fr 3fr;
  gap: 16px;
}

.chart-box {
  background: var(--white);
  border: 1px solid var(--gray-200);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-sm);
}

.chart-box-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
  margin-bottom: 14px;
}

.chart-canvas {
  width: 100%;
  height: 200px;
}

.chart-canvas-sm {
  width: 100%;
  height: 160px;
}

.status-legend {
  margin-top: 12px;
}

.status-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-legend-label {
  flex: 1;
  color: var(--gray-600);
}

.status-legend-val {
  font-weight: 600;
  color: var(--gray-800);
}

@media (max-width: 900px) {
  .stats { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
