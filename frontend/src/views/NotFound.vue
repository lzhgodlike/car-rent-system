<template>
  <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:var(--bg);">
    <div style="text-align:center;">
      <div style="font-size:80px;font-weight:900;color:var(--accent);font-family:var(--font-mono);">404</div>
      <p style="font-size:16px;color:var(--muted);margin:12px 0 24px;">页面不存在</p>
      <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>
