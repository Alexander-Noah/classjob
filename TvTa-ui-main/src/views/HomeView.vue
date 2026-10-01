<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { useMonitorStore, type DeviceAction, type DeviceRecord } from '@/stores/monitor'

interface PanelDevice extends DeviceRecord {
  cpu: number
  memory: number
  disk: number
  online: boolean
  warning: boolean
}

const message = useMessage()
const monitorStore = useMonitorStore()
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const activeDevice = ref<PanelDevice | null>(null)

onMounted(() => {
  monitorStore.startPolling()
})

onBeforeUnmount(() => {
  monitorStore.stopPolling()
})

const devices = computed<PanelDevice[]>(() =>
  monitorStore.devices.map((device) => {
    const cpu = Math.round(device.metrics.cpuUsage)
    const memory = Math.round(device.metrics.memoryUsage)
    const disk = Math.round(device.metrics.diskUsage)
    return {
      ...device,
      cpu,
      memory,
      disk,
      online: device.status !== 'offline',
      warning: device.status === 'warning' || cpu >= 85 || memory >= 85 || disk >= 90
    }
  })
)

const totalCount = computed(() => devices.value.length)
const onlineCount = computed(() => devices.value.filter(device => device.online).length)
const offlineCount = computed(() => totalCount.value - onlineCount.value)
const warningCount = computed(() => devices.value.filter(device => device.warning).length)

const summaryItems = computed(() => [
  { label: '总设备数', value: totalCount.value, icon: 'i-carbon-devices' },
  { label: '在线', value: onlineCount.value, icon: 'i-carbon-checkmark-outline' },
  { label: '离线', value: offlineCount.value, icon: 'i-carbon-warning-alt' },
  { label: '高负载警报', value: warningCount.value, icon: 'i-carbon-warning-hex' }
])

const contextOptions = computed(() => {
  const disabled = !activeDevice.value
  const offline = activeDevice.value?.status === 'offline'
  return [
    { label: '查看实时状态', key: 'view', icon: renderContextIcon('i-carbon-chart-line') },
    { label: '刷新该终端', key: 'refresh', icon: renderContextIcon('i-carbon-renew') },
    { type: 'divider', key: 'divider-a' },
    { label: '远程唤醒', key: 'wake', icon: renderContextIcon('i-carbon-power'), disabled },
    { label: '重启学生机', key: 'reboot', icon: renderContextIcon('i-carbon-restart'), disabled: disabled || offline },
    { label: '关闭学生机', key: 'shutdown', icon: renderContextIcon('i-carbon-power'), disabled: disabled || offline },
    { label: '锁定屏幕', key: 'lock', icon: renderContextIcon('i-carbon-locked'), disabled: disabled || offline },
    { label: '发送提示消息', key: 'message', icon: renderContextIcon('i-carbon-chat'), disabled: disabled || offline },
    { type: 'divider', key: 'divider-b' },
    { label: '从面板移除', key: 'remove', icon: renderContextIcon('i-carbon-trash-can') }
  ]
})

function renderContextIcon(className: string) {
  return () => h('span', { class: `${className} text-15px` })
}

function openContextMenu(event: MouseEvent, device: PanelDevice) {
  event.preventDefault()
  activeDevice.value = device
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
}

async function handleContextSelect(key: string | number) {
  const action = String(key) as DeviceAction
  const device = activeDevice.value
  closeContextMenu()
  if (!device) return

  if (action === 'view') {
    message.info(`${device.location}：CPU ${device.cpu}% / 内存 ${device.memory}% / 磁盘 ${device.disk}%`)
    return
  }

  if (action === 'remove') {
    monitorStore.removeDevice(device.id)
    message.success(`已从面板移除 ${device.location}`)
    return
  }

  await monitorStore.requestDeviceAction(device.id, action)
  if (action === 'refresh') {
    await monitorStore.refresh({ silent: true })
  }
  message.success(`${device.location} 操作已记录`)
}

function statusText(device: PanelDevice) {
  if (device.status === 'offline') return '离线'
  if (device.warning) return '高负载'
  return '在线'
}

function metricWidth(value: number) {
  return `${Math.min(100, Math.max(0, value))}%`
}
</script>

<template>
  <section class="min-h-[calc(100vh-128px)] rounded-5 bg-white p-5 text-slate-900 ring-1 ring-slate-200/80">
    <header class="mb-4 overflow-hidden rounded-4 bg-#eaf3ff p-6 ring-1 ring-#d7e7ff">
      <div class="flex items-start justify-between gap-5">
        <div class="min-w-0">
        <div class="mb-2 inline-flex items-center gap-2 rounded-full bg-white/76 px-3 py-1 text-12px font-700 text-#2563eb ring-1 ring-#bfdbfe">
          <span class="i-carbon-data-vis-1 text-15px" />
          学生机实时看板
        </div>
        <h1 class="m-0 text-8 font-800 leading-tight text-slate-950">机房设备监控面板</h1>
      </div>

        <button
          class="h-12 w-12 shrink-0 rounded-4 border-0 bg-white text-#2563eb shadow-sm ring-1 ring-#dbeafe transition hover:bg-#eff6ff disabled:opacity-65"
          :disabled="monitorStore.loading"
          type="button"
          aria-label="刷新状态"
          @click="monitorStore.refresh()"
        >
          <span
            class="i-carbon-reset inline-block text-22px"
            :class="{ 'animate-spin': monitorStore.loading }"
          />
        </button>
      </div>
    </header>

    <div class="mb-4 grid grid-cols-2 gap-3 xl:grid-cols-4">
      <div
        v-for="item in summaryItems"
        :key="item.label"
        class="flex min-h-20 items-center gap-3 rounded-4 bg-slate-50 px-4 ring-1 ring-slate-200/80"
      >
        <span class="flex h-10 w-10 items-center justify-center rounded-3 bg-white text-#2563eb ring-1 ring-slate-200">
          <span :class="`${item.icon} text-19px`" />
        </span>
        <div>
          <div class="text-12px font-650 text-slate-500">{{ item.label }}</div>
          <div class="mt-1 font-mono text-6 font-800 leading-none text-slate-950">{{ item.value }}</div>
        </div>
      </div>
    </div>

    <div class="rounded-5 bg-#f4f8ff p-4 ring-1 ring-#dbeafe">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <div class="text-15px font-800 text-slate-900">学生机席位</div>
          <div class="mt-1 text-12px text-slate-500">固定网格展示已保存学生机，空位等待客户端上线。</div>
        </div>
        <div class="rounded-full bg-white px-3 py-1.5 text-12px text-slate-500 ring-1 ring-#dbeafe">
          右键设备卡操作
        </div>
      </div>

      <n-empty v-if="!devices.length" description="等待学生机上线" class="min-h-78 justify-center rounded-4 bg-white/78 ring-1 ring-#e2e8f0">
        <template #icon>
          <div class="i-carbon-laptop text-42px text-#2563eb" />
        </template>
        <template #extra>
          <div class="max-w-120 text-center text-3.5 leading-6 text-slate-500">
            启动教师端网关后，学生端通过发现协议或上报接口接入，设备会自动保存并显示在这里。
          </div>
        </template>
      </n-empty>

      <div v-else class="grid grid-cols-[repeat(auto-fill,minmax(160px,1fr))] gap-3">
        <article
          v-for="device in devices"
          :key="device.id"
          class="group relative min-h-34 cursor-context-menu rounded-4 bg-white p-3 shadow-sm ring-1 ring-slate-200 transition hover:-translate-y-0.5 hover:shadow-md"
          :class="[
            device.warning ? 'ring-#f59e0b/65 bg-#fffaf0' : '',
            !device.online ? 'bg-slate-50 opacity-75' : ''
          ]"
          @contextmenu="openContextMenu($event, device)"
        >
          <span
            class="absolute right-3 top-3 h-3 w-3 rounded-full"
            :class="device.online ? 'bg-#22c55e shadow-[0_0_12px_rgba(34,197,94,0.32)]' : 'bg-slate-300'"
          />
          <div class="mb-1 truncate pr-5 text-4.2 font-800 leading-none text-slate-950">{{ device.location }}</div>
          <div class="mb-3 truncate text-2.8 text-slate-500">{{ device.hostName }} · {{ device.ipAddress }}</div>

          <div class="flex items-center justify-between text-2.8 font-650 text-slate-600">
            <span>CPU</span>
            <span>{{ device.cpu }}%</span>
          </div>
          <div class="mt-1 h-1.6 overflow-hidden rounded-full bg-slate-200">
            <div class="h-full rounded-full bg-#3b82f6" :style="{ width: metricWidth(device.cpu) }" />
          </div>

          <div class="mt-2 flex items-center justify-between text-2.8 font-650 text-slate-600">
            <span>内存</span>
            <span>{{ device.memory }}%</span>
          </div>
          <div class="mt-1 h-1.6 overflow-hidden rounded-full bg-slate-200">
            <div class="h-full rounded-full bg-#f59e0b" :style="{ width: metricWidth(device.memory) }" />
          </div>

          <div class="mt-3 flex items-center justify-between gap-2 border-t border-slate-100 pt-2 text-2.8">
            <span class="font-700" :class="device.warning ? 'text-#b45309' : device.online ? 'text-#16a34a' : 'text-slate-400'">
              {{ statusText(device) }}
            </span>
            <span class="truncate text-slate-400">{{ device.actionState || device.lastSeen }}</span>
          </div>
        </article>
      </div>
    </div>

    <n-dropdown
      placement="bottom-start"
      trigger="manual"
      :x="contextMenuX"
      :y="contextMenuY"
      :options="contextOptions"
      :show="contextMenuVisible"
      @clickoutside="closeContextMenu"
      @select="handleContextSelect"
    />
  </section>
</template>
