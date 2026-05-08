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
  AVAILABLE: '空闲',
  RENTED: '出租中',
  MAINTENANCE: '维修中',
}

const formattedIncome = computed(() => Number(stats.value.totalIncome || 0).toFixed(2))

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
  legend: { data: ['租车订单', '还车订单', '收入'] },
  grid: { left: 16, right: 20, top: 40, bottom: 12, containLabel: true },
  xAxis: { type: 'category', data: chartData.value.dates || [] },
  yAxis: [
    {
      type: 'value',
      name: '订单数',
      minInterval: 1,
      splitLine: {
        lineStyle: {
          color: 'rgba(58, 42, 25, 0.08)',
        },
      },
    },
    {
      type: 'value',
      name: '收入',
      position: 'right',
      axisLabel: {
        formatter: '￥{value}',
      },
      splitLine: {
        show: false,
      },
    },
  ],
  series: [
    {
      name: '租车订单',
      type: 'line',
      smooth: true,
      yAxisIndex: 0,
      data: chartData.value.rentCounts || [],
    },
    {
      name: '还车订单',
      type: 'line',
      smooth: true,
      yAxisIndex: 0,
      data: chartData.value.returnCounts || [],
    },
    {
      name: '收入',
      type: 'bar',
      yAxisIndex: 1,
      data: chartData.value.totalIncomeSeries || [],
    },
  ],
})

const buildCarStatusOption = () => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['35%', '68%'],
      center: ['50%', '45%'],
      data: (chartData.value.carStatus || []).map((item) => ({
        name: statusLabelMap[item.name] || item.name,
        value: item.value,
      })),
      label: { formatter: '{b}\n{d}%' },
    },
  ],
})

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
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Operations Hub</div>
      <h1 class="hero-title">运营看板</h1>
      <p class="hero-desc">把订单趋势、收入变化和车辆状态放在一个视图里，方便你快速判断今天该盯订单、收入还是库存。</p>
      <div class="metric-strip">
        <div class="metric-pill">
          <span>总收入</span>
          <strong>￥{{ formattedIncome }}</strong>
        </div>
        <div class="metric-pill">
          <span>租赁订单</span>
          <strong>{{ stats.rentOrderCount || 0 }}</strong>
        </div>
        <div class="metric-pill">
          <span>还车记录</span>
          <strong>{{ stats.returnOrderCount || 0 }}</strong>
        </div>
        <div class="metric-pill">
          <span>空闲车辆</span>
          <strong>{{ stats.availableCarCount || 0 }}</strong>
        </div>
      </div>
    </section>

    <div class="page-card">

      <div class="toolbar toolbar-card">
        <el-select v-model="period" placeholder="统计维度" style="width: 160px" @change="handlePeriodChange">
          <el-option label="按日" value="day" />
          <el-option label="按月" value="month" />
          <el-option label="按年" value="year" />
        </el-select>
        <el-select v-model="range" placeholder="时间范围" style="width: 160px" @change="handleRangeChange">
          <el-option v-for="item in rangeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>

      <div class="chart-grid">
        <div class="chart-card">
          <div class="chart-head">
            <div class="chart-title">订单与收入趋势</div>
          </div>
          <div ref="rentReturnChartRef" class="chart-canvas"></div>
        </div>
        <div class="chart-card">
          <div class="chart-head">
            <div class="chart-title">车辆状态占比</div>
          </div>
          <div ref="carStatusChartRef" class="chart-canvas"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.chart-card {
  padding: 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--line);
  background: var(--white);
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.chart-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 10px;
}

.chart-head p {
  margin: 2px 0 0;
  color: var(--subtext);
  font-size: 13px;
}

.chart-canvas {
  width: 100%;
  height: 320px;
}
</style>


