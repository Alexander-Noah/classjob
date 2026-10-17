<!--
  Web 管理平台的设备盘点弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Finished } from '@element-plus/icons-vue'

defineOptions({
  name: 'DeviceInventoryDialog',
})

const props = defineProps({
  device: {
    type: Object,
    default: null,
  },
  devices: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  resultOptions: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  /**
   * 获取当前操作。
   * @returns props.modelValue 的当前值。
   */
  get: () => props.modelValue,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:modelValue', value),
})

const targetDevices = computed(/** 计算派生状态。 */ () => (props.devices.length ? props.devices : [props.device].filter(Boolean)))
const isBatch = computed(/** 计算派生状态。 */ () => targetDevices.value.length > 1)
const title = computed(/** 计算派生状态。 */ () => (isBatch.value ? `批量盘点 ${targetDevices.value.length} 台设备` : '设备盘点登记'))
const summaryDevice = computed(/** 计算派生状态。 */ () => targetDevices.value[0] || null)
/**
 * 获取设备名称。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getDeviceName = (device = {}) => device.deviceName || device.name || '未命名设备'
/**
 * 获取设备编码。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getDeviceCode = (device = {}) => device.assetNo || device.code || device.deviceCode || '-'
/**
 * 获取设备实验室。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getDeviceLab = (device = {}) => device.labName || device.lab || device.roomNo || '未关联实验室'
/**
 * 获取状态类型。
 * 入参：status（状态）。
 * @returns 文本结果。
 */
const getStatusType = (status) => {
  if (status === '正常') return 'success'
  if (status === '维修中') return 'warning'
  if (status === '已报废') return 'info'
  return 'info'
}

/**
 * 处理NormalChange。
 * 入参：value（值）。
 */
const handleNormalChange = (value) => {
  if (value) {
    props.formModel.missing = false
    props.formModel.damaged = false
    props.formModel.resultStatus = '已盘点'
  } else if (props.formModel.resultStatus === '已盘点') {
    props.formModel.resultStatus = '盘点异常'
  }
}

/**
 * 处理MissingChange。
 * 入参：value（值）。
 */
const handleMissingChange = (value) => {
  if (value) {
    props.formModel.normal = false
    props.formModel.damaged = false
    props.formModel.resultStatus = '设备缺失'
  }
}

/**
 * 处理DamagedChange。
 * 入参：value（值）。
 */
const handleDamagedChange = (value) => {
  if (value) {
    props.formModel.normal = false
    props.formModel.missing = false
    props.formModel.resultStatus = '设备损坏'
  }
}

/**
 * 处理结果Change。
 * 入参：value（值）。
 */
const handleResultChange = (value) => {
  props.formModel.normal = value === '已盘点'
  props.formModel.missing = value === '设备缺失'
  props.formModel.damaged = value === '设备损坏'
}
</script>

<template>
  <el-dialog v-model="visible" align-center destroy-on-close :title="title" width="min(640px, calc(100vw - 32px))">
    <div v-if="summaryDevice" class="inventory-device-summary">
      <el-icon><Finished /></el-icon>
      <div>
        <h3>{{ isBatch ? `已选择 ${targetDevices.length} 台设备` : getDeviceName(summaryDevice) }}</h3>
        <span v-if="isBatch">{{ targetDevices.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => getDeviceCode(item)).join('、') }}</span>
        <span v-else>{{ getDeviceCode(summaryDevice) }} · {{ getDeviceLab(summaryDevice) }}</span>
      </div>
      <el-tag v-if="!isBatch" :type="summaryDevice.tagType || getStatusType(summaryDevice.status)" effect="light">{{ summaryDevice.status }}</el-tag>
    </div>

    <el-alert
      v-if="isBatch"
      class="batch-alert"
      :closable="false"
      show-icon
      title="批量盘点会为已勾选设备写入同一盘点结果、盘点人和备注。"
      type="info"
    />

    <el-form class="inventory-form" label-width="92px" :model="formModel">
      <el-form-item label="盘点人" required>
        <el-input v-model="formModel.inspectorName" clearable placeholder="请输入盘点人" />
      </el-form-item>
      <el-form-item label="盘点时间" required>
        <el-date-picker
          v-model="formModel.inspectedAt"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择盘点时间"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="盘点结果" required>
        <el-select v-model="formModel.resultStatus" placeholder="请选择盘点结果" @change="handleResultChange">
          <el-option
            v-for="option in resultOptions"
            :key="option.label"
            :label="option.label"
            :value="option.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="情况确认">
        <div class="inventory-flags">
          <el-checkbox v-model="formModel.normal" @change="handleNormalChange">设备正常</el-checkbox>
          <el-checkbox v-model="formModel.missing" @change="handleMissingChange">设备缺失</el-checkbox>
          <el-checkbox v-model="formModel.damaged" @change="handleDamagedChange">设备损坏</el-checkbox>
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="formModel.remark"
          :rows="4"
          maxlength="300"
          placeholder="填写异常说明、现场情况或处理建议"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="loading" type="primary" @click="$emit('submit')">保存盘点</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.inventory-device-summary {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #f8fbff;
}

.inventory-device-summary .el-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 20px;
}

.inventory-device-summary h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 16px;
  letter-spacing: 0;
}

.inventory-device-summary span {
  display: block;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-alert {
  margin-bottom: 16px;
}

.inventory-form :deep(.el-date-editor),
.inventory-form :deep(.el-select) {
  width: 100%;
}

.inventory-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.inventory-flags :deep(.el-checkbox) {
  margin-right: 0;
}
</style>
