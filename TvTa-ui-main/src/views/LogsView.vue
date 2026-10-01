<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { useMonitorStore } from '@/stores/monitor'

const monitorStore = useMonitorStore()

onMounted(() => {
  monitorStore.startPolling()
})

onBeforeUnmount(() => {
  monitorStore.stopPolling()
})
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="m-0 text-6 font-semibold text-slate-800 dark:text-slate-100">运行日志</h2>
        <p class="mt-2 mb-0 text-3.5 text-slate-500">保留最近的网关与终端状态日志，便于课堂期间排查问题。</p>
      </div>
      <n-button :loading="monitorStore.loading" @click="monitorStore.refresh()">刷新</n-button>
    </div>

    <n-card :bordered="false" class="rounded-4 shadow-sm">
      <div class="min-h-130 max-h-130 overflow-auto rounded-4 bg-slate-950 p-4 font-mono text-3.2 leading-6 text-slate-100">
        <div v-for="line in monitorStore.logs" :key="line" class="mb-2 break-all whitespace-pre-wrap">
          {{ line }}
        </div>
        <div v-if="!monitorStore.logs.length" class="text-slate-500">暂无日志输出</div>
      </div>
    </n-card>
  </div>
</template>
