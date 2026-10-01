<script setup>
import {
  Key,
  Monitor,
  Operation,
  Refresh,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue'

defineOptions({
  name: 'DeviceControlPanel',
})

defineProps({
  capabilityState: {
    type: Object,
    required: true,
  },
  devices: {
    type: Array,
    default: () => [],
  },
  labName: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  metrics: {
    type: Object,
    default: () => ({
      total: 0,
      onlineCount: 0,
      configuredCount: 0,
      categoryCount: 0,
    }),
  },
})

defineEmits(['capability'])

const iconMap = {
  'access-control': Key,
  light: SwitchButton,
  'air-conditioner': Setting,
  sensor: Monitor,
}

const getDeviceIcon = (device) => iconMap[device.type] || Operation
const isMockGateway = (device) => device.gatewayMode === 'mock_gateway' || device.sourceType === 'mock' || device.protocol === 'mock'
const getGatewayModeLabel = (device) => (isMockGateway(device) ? '模拟网关演示' : '待配置真实网关')
const getGatewayModeType = (device) => (isMockGateway(device) ? 'warning' : 'info')
const getCapabilityStateKey = (device, capability) => `${device.code || device.id}::${capability.key}`
const getCapabilityState = (capabilityState, device, capability) =>
  capabilityState[getCapabilityStateKey(device, capability)] || { enabled: false, value: '' }
const setCapabilityEnabled = (capabilityState, device, capability, enabled) => {
  const key = getCapabilityStateKey(device, capability)
  capabilityState[key] = {
    ...getCapabilityState(capabilityState, device, capability),
    enabled,
  }
}
const setCapabilityValue = (capabilityState, device, capability, value) => {
  const key = getCapabilityStateKey(device, capability)
  capabilityState[key] = {
    ...getCapabilityState(capabilityState, device, capability),
    value,
  }
}
</script>

<template>
  <el-card class="module-card device-panel-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div>
          <h2>实验室硬件控制</h2>
          <span>{{ labName }} 当前仅开放门禁、灯光、空调与温湿度控制</span>
        </div>
        <div class="control-metrics" aria-label="设备统计">
          <span>
            <strong>{{ metrics.total }}</strong>
            设备
          </span>
          <span>
            <strong>{{ metrics.onlineCount }}</strong>
            在线
          </span>
          <span>
            <strong>{{ metrics.configuredCount }}</strong>
            网关
          </span>
          <span>
            <strong>{{ metrics.categoryCount }}</strong>
            类型
          </span>
        </div>
      </div>
    </template>

    <el-skeleton v-if="loading" :rows="5" animated />

    <el-scrollbar v-else class="device-control-scroll">
      <el-alert
        v-if="devices.some((device) => device.sourceType === 'mock' || device.protocol === 'mock' || !device.configured)"
        class="mock-control-alert"
        title="模拟网关演示：控制命令会写入日志；真实网关待配置后可替换适配器"
        type="warning"
        :closable="false"
        show-icon
      />

      <div class="device-control-grid">
        <article v-for="device in devices" :key="device.code || device.id" class="device-control-card">
          <div class="item-heading">
            <span class="status-icon" :class="device.type" aria-hidden="true">
              <el-icon>
                <component :is="getDeviceIcon(device)" />
              </el-icon>
            </span>
            <div>
              <h3>{{ device.name }}</h3>
              <p>{{ device.location || device.category || '未填写位置' }}</p>
            </div>
            <el-tag :type="device.tagType" effect="light">{{ device.status }}</el-tag>
          </div>

          <div class="device-meta">
            <el-tag :type="getGatewayModeType(device)" effect="plain">
              {{ getGatewayModeLabel(device) }}
            </el-tag>
            <el-tag :type="device.online ? 'success' : 'info'" effect="plain">
              {{ device.online ? '在线' : '离线' }}
            </el-tag>
          </div>

          <div v-if="device.capabilities?.length" class="capability-list">
            <template v-for="capability in device.capabilities" :key="capability.key">
              <div
                v-if="capability.type === 'switch'"
                class="capability-row"
                :class="{ disabled: !capability.enabled }"
              >
                <span>{{ capability.label }}</span>
                <el-switch
                  :model-value="getCapabilityState(capabilityState, device, capability).enabled"
                  :disabled="!capability.enabled"
                  @change="(value) => {
                    setCapabilityEnabled(capabilityState, device, capability, value)
                    $emit('capability', device, capability)
                  }"
                />
              </div>

              <div
                v-else-if="capability.type === 'slider'"
                class="capability-block"
                :class="{ disabled: !capability.enabled }"
              >
                <div class="capability-row">
                  <span>{{ capability.label }}</span>
                  <strong>
                    {{ getCapabilityState(capabilityState, device, capability).value }}{{ capability.unit || '' }}
                  </strong>
                </div>
                <el-slider
                  :model-value="getCapabilityState(capabilityState, device, capability).value"
                  :disabled="!capability.enabled"
                  :max="capability.max ?? 100"
                  :min="capability.min ?? 0"
                  @change="(value) => {
                    setCapabilityValue(capabilityState, device, capability, value)
                    $emit('capability', device, capability)
                  }"
                />
              </div>

              <div
                v-else-if="capability.type === 'select'"
                class="capability-row"
                :class="{ disabled: !capability.enabled }"
              >
                <span>{{ capability.label }}</span>
                <el-select
                  :model-value="getCapabilityState(capabilityState, device, capability).value"
                  :disabled="!capability.enabled"
                  size="small"
                  @change="(value) => {
                    setCapabilityValue(capabilityState, device, capability, value)
                    $emit('capability', device, capability)
                  }"
                >
                  <el-option v-for="option in capability.options || []" :key="option" :label="option" :value="option" />
                </el-select>
              </div>

              <el-button
                v-else
                class="capability-button"
                :disabled="!capability.enabled"
                size="small"
                type="primary"
                @click="$emit('capability', device, capability)"
              >
                <el-icon>
                  <Refresh v-if="capability.key === 'status' || capability.key === 'read-environment'" />
                  <Operation v-else />
                </el-icon>
                {{ capability.label }}
              </el-button>
            </template>
          </div>

          <el-empty v-else class="empty-capability" description="暂无硬件控制项" />
        </article>

        <el-empty v-if="!devices.length" description="该实验室暂未接入物联设备" />
      </div>
    </el-scrollbar>
  </el-card>
</template>

<style scoped>
.module-card {
  width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.module-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.module-card :deep(.el-card__body) {
  padding: 16px;
}

.card-header,
.control-metrics,
.item-heading,
.device-meta,
.capability-row {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.card-header h2,
.item-heading h3 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
  letter-spacing: 0;
}

.card-header h2 {
  font-size: 16px;
}

.card-header span,
.item-heading p,
.capability-row span {
  color: #64748b;
  font-size: 13px;
}

.control-metrics {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.control-metrics span {
  min-width: 64px;
  padding: 7px 10px;
  border: 1px solid #dbe7f5;
  border-radius: 7px;
  background: #ffffff;
  text-align: center;
}

.control-metrics strong {
  display: block;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.1;
}

.device-control-scroll {
  max-height: 650px;
}

.mock-control-alert {
  margin: 0 6px 12px 0;
  border-radius: 8px;
}

.device-control-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr));
  gap: 12px;
  padding-right: 6px;
}

.device-control-card {
  min-width: 0;
  width: 100%;
  padding: 14px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.item-heading {
  justify-content: space-between;
  gap: 12px;
}

.item-heading > div {
  flex: 1 1 auto;
  min-width: 0;
}

.item-heading h3 {
  font-size: 15px;
  line-height: 1.35;
  word-break: keep-all;
}

.item-heading p {
  margin: 6px 0 0;
  line-height: 1.45;
}

.status-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 20px;
}

.status-icon.light {
  color: #ca8a04;
  background: #fefce8;
}

.status-icon.air-conditioner {
  color: #0284c7;
  background: #eff6ff;
}

.status-icon.sensor {
  color: #059669;
  background: #ecfdf5;
}

.device-meta {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.capability-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

.capability-row {
  justify-content: space-between;
  gap: 12px;
}

.capability-row strong {
  color: var(--text-primary);
  font-size: 14px;
}

.capability-row :deep(.el-select) {
  width: 126px;
}

.capability-block {
  display: grid;
  gap: 6px;
}

.capability-button.el-button {
  justify-content: center;
  width: 100%;
  height: 34px;
  margin-left: 0;
  border-radius: 6px;
  font-weight: 700;
}

.disabled {
  opacity: 0.62;
}

.empty-capability {
  --el-empty-padding: 10px 0 0;
}

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .control-metrics {
    justify-content: flex-start;
  }
}
</style>
