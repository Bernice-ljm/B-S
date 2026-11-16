<script setup lang="ts">
import { DocumentChecked } from '@element-plus/icons-vue'
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">
        <el-icon :size="22" aria-hidden="true"><DocumentChecked /></el-icon>
        <span>企业审批系统</span>
      </div>
      <el-tag effect="plain" type="info">开发环境</el-tag>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>
