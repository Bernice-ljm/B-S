<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'

type HealthState = 'checking' | 'available' | 'unavailable'

const healthState = ref<HealthState>('checking')

const statusText = computed(() => ({
  checking: '正在检查',
  available: '后端可用',
  unavailable: '后端未启动',
})[healthState.value])

const statusType = computed(() => ({
  checking: 'info',
  available: 'success',
  unavailable: 'warning',
})[healthState.value] as 'info' | 'success' | 'warning')

onMounted(async () => {
  try {
    const response = await axios.get<{ status: string }>('/actuator/health', { timeout: 3_000 })
    healthState.value = response.data.status === 'UP' ? 'available' : 'unavailable'
  } catch {
    healthState.value = 'unavailable'
  }
})
</script>

<template>
  <section class="status-view" aria-labelledby="page-title">
    <div>
      <p class="eyebrow">工程状态</p>
      <h1 id="page-title">企业审批系统</h1>
      <p class="summary">前端工程已启动。业务页面将在后续阶段按设计文档逐步实现。</p>
    </div>

    <div class="status-row" aria-live="polite">
      <span>服务连接</span>
      <el-tag :type="statusType" effect="plain">{{ statusText }}</el-tag>
    </div>
  </section>
</template>
