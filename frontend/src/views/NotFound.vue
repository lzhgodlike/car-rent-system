<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="router.replace('/cars')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.not-found h1 {
  margin: 0;
  font-size: 96px;
  font-weight: 800;
  color: var(--brand-deep);
  line-height: 1;
}

.not-found p {
  margin: 0;
  font-size: 18px;
  color: var(--subtext);
}
</style>
