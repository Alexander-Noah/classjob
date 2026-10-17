<!--
  桌面端的本地网关视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { useActivationStore } from '@/stores/activation'
import { useMonitorStore } from '@/stores/monitor'

const message = useMessage()
const activationStore = useActivationStore()
const monitorStore = useMonitorStore()
const cloudEndpoint = ref(monitorStore.gateway.cloudEndpoint)

activationStore.hydrate()

watch(
  () => monitorStore.gateway.cloudEndpoint,
  (value) => {
    cloudEndpoint.value = value
  }
)

onMounted(() => {
  monitorStore.startPolling()
})

onBeforeUnmount(() => {
  monitorStore.stopPolling()
})

async function saveConfig() {
  try {
    await monitorStore.updateCloudEndpoint(cloudEndpoint.value.trim())
    message.success('网关配置已保存')
  } catch {
    message.success('网关暂不可用，配置已保存到客户本地')
  }
}

async function refreshGateway() {
  await monitorStore.refresh()
  message.success('网关状态已刷新')
}

async function toggleGateway() {
  if (monitorStore.gateway.running) {
    await monitorStore.stopGateway()
    message.success('本地教师网关已停止')
    return
  }

  await monitorStore.startGateway()
  message.success('本地教师网关已启动')
}

function resetClientActivation() {
  activationStore.resetActivation()
  message.success('已清除本机激活信息，请回到登录页重新激活')
}
</script>

<template>
  <div class="space-y-5">
    <div>
      <h2 class="m-0 text-6 font-semibold text-slate-800 dark:text-slate-100">连接设置</h2>
      <p class="mt-2 mb-0 text-3.5 text-slate-500">
        开启采集网关，并配置学生端上报入口与云端转发地址。
      </p>
    </div>

    <n-alert v-if="activationStore.isUsable" type="success" :bordered="false" class="rounded-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>
          {{ activationStore.statusText }} | 授权码：{{ activationStore.maskedToken }}
          <template v-if="activationStore.isLicensed">
            | 设备额度：{{ monitorStore.totalCount }} / {{ activationStore.quota }}
          </template>
        </span>
        <n-button size="small" tertiary type="warning" @click="resetClientActivation">清除本机激活</n-button>
      </div>
    </n-alert>

    <n-grid cols="1 900:2" :x-gap="16" :y-gap="16">
      <n-grid-item>
        <n-card title="基础状态" :bordered="false" class="rounded-4 shadow-sm">
          <n-descriptions label-placement="left" :column="1" bordered>
            <n-descriptions-item label="运行状态">
              <n-tag :type="monitorStore.gateway.running ? 'success' : 'default'" round>
                {{ monitorStore.gateway.running ? '运行中' : '未启动' }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="网关地址">
              {{ monitorStore.gateway.gatewayIp }}
            </n-descriptions-item>
            <n-descriptions-item label="最后同步">
              {{ monitorStore.gateway.lastSyncText }}
            </n-descriptions-item>
            <n-descriptions-item label="当前数据源">
              {{ monitorStore.usingDemoData ? '演示数据' : '实时接口' }}
            </n-descriptions-item>
          </n-descriptions>
          <div class="mt-4 flex gap-3">
            <n-button type="primary" :loading="monitorStore.loading" @click="refreshGateway">
              刷新状态
            </n-button>
            <n-button :loading="monitorStore.loading" @click="toggleGateway">
              {{ monitorStore.gateway.running ? '停止网关' : '启动网关' }}
            </n-button>
          </div>
        </n-card>
      </n-grid-item>

      <n-grid-item>
        <n-card title="云端转发" :bordered="false" class="rounded-4 shadow-sm">
          <div class="space-y-4">
            <n-input
              v-model:value="cloudEndpoint"
              placeholder="请输入云端转发地址，例如 https://example.com/api/host/collect"
            />
            <div class="flex items-center justify-between gap-4">
              <span class="break-all text-3.5 text-slate-500">
                当前状态：{{ monitorStore.gateway.cloudForwardingEnabled ? '已启用' : '未启用' }}
              </span>
              <n-button type="primary" :loading="monitorStore.loading" @click="saveConfig">
                保存配置
              </n-button>
            </div>
          </div>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>
