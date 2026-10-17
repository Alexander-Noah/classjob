<!--
  桌面端的MainLayout视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup lang="ts">
import { computed, h } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useActivationStore } from '@/stores/activation'
import { useMonitorStore } from '@/stores/monitor'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const activationStore = useActivationStore()
const monitorStore = useMonitorStore()
const themeStore = useThemeStore()

activationStore.hydrate()

const renderIcon = (icon: string) => () => h('div', { class: `${icon} text-18px` })

const menuOptions = computed(() => [
  {
    label: '设备面板',
    key: '/',
    icon: renderIcon('i-carbon-dashboard'),
    disabled: !activationStore.isUsable
  },
  {
    label: '终端列表',
    key: '/devices',
    icon: renderIcon('i-carbon-laptop'),
    disabled: !activationStore.isUsable
  },
  {
    label: '本地日志',
    key: '/logs',
    icon: renderIcon('i-carbon-document-blank'),
    disabled: !activationStore.isUsable
  },
  {
    label: '连接设置',
    key: '/gateway',
    icon: renderIcon('i-carbon-settings-adjust')
  }
])

const pageTitleMap: Record<string, string> = {
  '/': '设备面板',
  '/devices': '终端列表',
  '/logs': '本地日志',
  '/gateway': '连接设置'
}

const pageTitle = computed(() => pageTitleMap[route.path] || '本地监测客户端')
const localHostStatusType = computed(() => {
  if (monitorStore.localHost.status === 'warning') return 'text-#b45309 bg-#fffbeb ring-#fde68a'
  if (monitorStore.localHost.status === 'online') return 'text-#047857 bg-#ecfdf5 ring-#a7f3d0'
  return 'text-slate-500 bg-slate-100 ring-slate-200'
})
const localHostStatusText = computed(() => {
  if (monitorStore.localHost.status === 'warning') return '高负载'
  if (monitorStore.localHost.status === 'online') return '在线'
  return '未采集'
})
const localHostMetricsText = computed(() => {
  const metrics = monitorStore.localHost.metrics
  return `CPU ${Math.round(metrics.cpuUsage)}% / 内存 ${Math.round(metrics.memoryUsage)}%`
})

function handleMenuSelect(key: string) {
  if (!activationStore.isUsable && key !== '/gateway') {
    router.push('/gateway')
    return
  }

  router.push(key)
}
</script>

<template>
  <n-layout has-sider position="absolute" class="h-screen bg-#f3f6fb dark:bg-#0f1726">
    <n-layout-sider
      bordered
      :width="248"
      content-style="display:flex;flex-direction:column;background:rgba(255,255,255,0.94);"
      class="dark:bg-#121a2a!"
    >
      <div class="border-b border-slate-200/70 px-6 pt-6 pb-4 dark:border-slate-700/60">
        <div class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-3 bg-#1f6feb/10 text-#1f6feb">
            <div class="i-carbon-screen-map text-24px" />
          </div>
          <div>
            <div class="text-17px font-700 text-slate-800 dark:text-slate-100">智云实验室</div>
            <div class="text-12px text-slate-500 dark:text-slate-400">本地值守客户端</div>
          </div>
        </div>
      </div>

      <div class="flex-1 px-3 py-4">
        <n-menu
          :value="route.path"
          :options="menuOptions"
          :indent="20"
          @update:value="handleMenuSelect"
        />
      </div>

      <div class="mx-4 mb-3 rounded-4 bg-white p-4 ring-1 ring-slate-200 dark:bg-#172033 dark:ring-slate-800">
        <div class="mb-3 flex items-center justify-between gap-2">
          <div class="text-12px font-700 text-slate-500 dark:text-slate-400">本机状态</div>
          <span class="rounded-full px-2 py-0.8 text-11px font-700 ring-1" :class="localHostStatusType">
            {{ localHostStatusText }}
          </span>
        </div>
        <div class="truncate text-14px font-700 text-slate-800 dark:text-slate-100">
          {{ monitorStore.localHost.hostName }}
        </div>
        <div class="mt-1 truncate text-12px text-slate-500 dark:text-slate-400">
          {{ monitorStore.localHost.ipAddress }}
        </div>
        <div class="mt-3 rounded-3 bg-slate-50 px-3 py-2 text-12px text-slate-600 dark:bg-slate-900/60 dark:text-slate-300">
          {{ localHostMetricsText }}
        </div>
        <div class="mt-2 truncate text-11px text-slate-400">
          {{ monitorStore.localHost.lastSeen }}
        </div>
      </div>

      <div
        v-if="activationStore.isUsable"
        class="mx-4 mb-3 rounded-4 bg-#ecfdf5 p-4 ring-1 ring-emerald-100 dark:bg-#123126 dark:ring-emerald-900/60"
      >
        <div class="mb-2 flex items-center gap-2 text-12px font-700 text-emerald-700 dark:text-emerald-300">
          <span :class="activationStore.isTrialActive ? 'i-carbon-time text-15px' : 'i-carbon-checkmark-outline text-15px'" />
          {{ activationStore.isTrialActive ? '试用中' : '已授权' }}
        </div>
        <div class="text-14px font-700 text-slate-800 dark:text-slate-100">
          {{ activationStore.labName }}
        </div>
        <div class="mt-1 text-12px text-slate-500 dark:text-slate-400">
          {{ activationStore.isTrialActive ? `剩余 ${activationStore.trialRemainingDays} 天` : `设备额度：${monitorStore.totalCount} / ${activationStore.quota}` }}
        </div>
      </div>

      <div
        v-else
        class="mx-4 mb-3 rounded-4 bg-#fff7ed p-4 ring-1 ring-orange-100 dark:bg-#33210f dark:ring-orange-900/60"
      >
        <div class="mb-2 flex items-center gap-2 text-12px font-700 text-orange-700 dark:text-orange-300">
          <span class="i-carbon-locked text-15px" />
          未激活
        </div>
        <div class="text-12px leading-5 text-slate-600 dark:text-slate-300">
          请回到登录页激活客户端后继续使用。
        </div>
      </div>

      <div class="mx-4 mb-4 rounded-4 bg-#eff6ff p-4 dark:bg-#172033">
        <div class="mb-2 text-12px text-slate-500 dark:text-slate-400">客户端模式</div>
        <div class="flex items-center justify-between">
          <div>
            <div class="text-14px font-600 text-slate-800 dark:text-slate-100">本机值守</div>
            <div class="text-12px text-slate-500 dark:text-slate-400">采集、显示与转发本地状态</div>
          </div>
          <n-button quaternary circle @click="themeStore.toggleTheme">
            <template #icon>
              <div :class="themeStore.isDark ? 'i-carbon-moon text-18px' : 'i-carbon-screen text-18px'" />
            </template>
          </n-button>
        </div>
      </div>
    </n-layout-sider>

    <n-layout content-style="display:flex;flex-direction:column;background:transparent;">
      <n-layout-header
        bordered
        class="h-18 flex items-center justify-between bg-white/86 px-7 backdrop-blur dark:bg-#121a2a/92"
      >
        <div>
          <div class="text-18px font-700 text-slate-800 dark:text-slate-100">{{ pageTitle }}</div>
          <div class="text-12px text-slate-500 dark:text-slate-400">本机采集状态与机房终端实时看板</div>
        </div>
        <div class="flex items-center gap-3 text-slate-500 dark:text-slate-300">
          <div class="flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1.5 text-12px dark:bg-slate-800">
            <div class="i-carbon-screen text-16px" />
            <span>本地客户端</span>
          </div>
        </div>
      </n-layout-header>

      <n-layout-content content-style="padding: 20px 24px 24px;">
        <RouterView />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>
