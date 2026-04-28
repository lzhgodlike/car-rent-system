<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'
import { getAuth } from '../utils/auth'

const auth = getAuth()
const isAdmin = computed(() => auth?.userInfo?.role === 'ADMIN')
const orders = ref([])
const cars = ref([])

const orderStatusMap = {
  RENTED: '租赁中',
  RETURNED: '已归还',
}

const activeCount = computed(() => orders.value.filter((item) => item.orderStatus === 'RENTED').length)
const returnedCount = computed(() => orders.value.filter((item) => item.orderStatus === 'RETURNED').length)
const totalIncome = computed(() =>
  orders.value.reduce((sum, item) => sum + Number(item.totalPrice || 0), 0).toFixed(2),
)
const statusTone = (status) => (status === 'RENTED' ? 'warning' : 'success')

const formatOrderStatus = (status) => orderStatusMap[status] || status || '-'

const loadData = async () => {
  const [orderData, carData] = await Promise.all([request.get('/rent-orders'), request.get('/cars')])
  orders.value = orderData
  cars.value = carData
}

onMounted(loadData)

const carNoMap = computed(() => {
  const map = new Map()
  cars.value.forEach((car) => {
    map.set(car.id, car.carNo)
  })
  return map
})

const formatCarNo = (carId) => carNoMap.value.get(carId) || carId || '-'
</script>

<template>
  <div class="page-stack">
    <section class="hero-panel">
      <div class="hero-eyebrow">Orders</div>
      <h1 class="hero-title">租车订单</h1>
      <p class="hero-desc">这里集中查看每一笔租车订单，能快速确认车辆编号、租期、状态和订单金额。</p>
      <div class="metric-strip">
        <div class="metric-pill"><span>订单总数</span><strong>{{ orders.length }}</strong></div>
        <div class="metric-pill"><span>租赁中</span><strong>{{ activeCount }}</strong></div>
        <div class="metric-pill"><span>已归还</span><strong>{{ returnedCount }}</strong></div>
        <div class="metric-pill"><span>累计租金</span><strong>￥{{ totalIncome }}</strong></div>
      </div>
    </section>

    <div class="page-card">
      <!-- <div class="page-header compact-page-head">
        <div>
          <h2 class="page-title">租车信息管理</h2>
          <p class="page-desc">展示租车订单、租车日期、预计还车日期、租金和订单状态。</p>
        </div>
      </div>

      <div class="summary-grid">
        <div class="summary-card"><span>订单总数</span><strong>{{ orders.length }}</strong></div>
        <div class="summary-card"><span>当前租赁中</span><strong>{{ activeCount }}</strong></div>
        <div class="summary-card"><span>已完成归还</span><strong>{{ returnedCount }}</strong></div>
        <div class="summary-card"><span>累计总租金</span><strong>￥{{ totalIncome }}</strong></div>
      </div> -->

      <!-- <div class="section-card"> -->
        <!-- <div class="section-head"> -->
          <!-- <div>
            <h3>订单明细</h3>
            <p>支持核对订单编号、车辆编号、租期长度与订单状态。</p>
          </div> -->
        <!-- </div> -->

        <div class="table-shell">
          <el-table :data="orders" stripe>
            <el-table-column prop="orderNo" label="订单编号" width="180" />
            <el-table-column prop="userId" label="用户ID" width="90" />
            <el-table-column label="车辆编号" width="140">
              <template #default="scope">
                {{ formatCarNo(scope.row.carId) }}
              </template>
            </el-table-column>
            <el-table-column prop="rentDate" label="租车日期" />
            <el-table-column prop="expectedReturnDate" label="预计还车日期" />
            <el-table-column prop="rentDays" label="天数" width="90" />
            <el-table-column label="总租金" width="120">
              <template #default="scope">
                ￥{{ scope.row.totalPrice || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <span class="status-badge" :class="statusTone(scope.row.orderStatus)">
                  {{ formatOrderStatus(scope.row.orderStatus) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      <!-- </div> -->

      <p></p>
      <div v-if="isAdmin" class="soft-note">
        订单归还请到还车信息管理页面处理，系统会同时更新还车记录和车辆状态。
      </div>
    </div>
  </div>
</template>

