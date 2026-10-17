<!--
  桌面端的设备管理视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup lang="ts">
import { h, onBeforeUnmount, onMounted } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import { useMonitorStore, type DeviceRecord } from '@/stores/monitor'

const monitorStore = useMonitorStore()

onMounted(() => {
  monitorStore.startPolling()
})

onBeforeUnmount(() => {
  monitorStore.stopPolling()
})

const statusTypeMap = {
  online: 'success',
  warning: 'warning',
  offline: 'error'
} as const

const statusLabelMap = {
  online: '在线',
  warning: '预警',
  offline: '离线'
} as const

const columns: DataTableColumns<DeviceRecord> = [
  { title: '主机名', key: 'hostName' },
  { title: '设备 ID', key: 'id' },
  { title: '位置', key: 'location' },
  {
    title: '状态',
    key: 'status',
    render: row =>
      h(
        NTag,
        {
          type: statusTypeMap[row.status],
          round: true,
          size: 'small'
        },
        { default: () => statusLabelMap[row.status] }
      )
  },
  { title: 'IP 地址', key: 'ipAddress' },
  {
    title: 'CPU',
    key: 'cpuUsage',
    render: row => `${row.metrics.cpuUsage.toFixed(1)}%`
  },
  {
    title: '内存',
    key: 'memoryUsage',
    render: row => `${row.metrics.memoryUsage.toFixed(1)}%`
  },
  {
    title: '磁盘',
    key: 'diskUsage',
    render: row => `${row.metrics.diskUsage.toFixed(1)}%`
  },
  { title: '最近上报', key: 'lastSeen' }
]
</script>

<template>
  <div class="space-y-5">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="m-0 text-6 font-semibold text-slate-800 dark:text-slate-100">在线终端</h2>
        <p class="mt-2 mb-0 text-3.5 text-slate-500">查看学生端在线情况、终端位置与资源占用。</p>
      </div>
      <div class="flex items-center gap-3">
        <n-tag :type="monitorStore.usingDemoData ? 'warning' : 'success'" round>
          {{ monitorStore.usingDemoData ? '演示数据' : '实时数据' }}
        </n-tag>
        <n-button :loading="monitorStore.loading" @click="monitorStore.refresh()">刷新</n-button>
      </div>
    </div>

    <n-card :bordered="false" class="rounded-4 shadow-sm">
      <n-data-table :columns="columns" :data="monitorStore.devices" :pagination="{ pageSize: 8 }" />
    </n-card>
  </div>
</template>
