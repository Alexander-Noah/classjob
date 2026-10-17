<!--
  Web 管理平台的设备管理详情面板视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'DeviceDetailPanel',
})

const props = defineProps({
  device: {
    type: Object,
    default: null,
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const detailRows = computed(() => {
  const device = props.device || {}
  return [
    ['资产编号', device.assetNo],
    ['设备名称', device.deviceName],
    ['设备分类', device.category],
    ['所属实验室', device.labName],
    ['安装位置', device.roomNo],
    ['数量', device.quantity],
    ['单位', device.unit],
    ['状态', device.status],
    ['规格参数', device.specification],
    ['执行标准', device.standardRequirement],
    ['备注', device.remark],
    ['创建时间', device.createdAt],
  ]
})
</script>

<template>
  <el-dialog v-model="visible" align-center title="设备资产详情" width="min(680px, calc(100vw - 32px))">
    <el-descriptions :column="2" border class="device-detail">
      <el-descriptions-item v-for="[label, value] in detailRows" :key="label" :label="label">
        {{ value || '未填写' }}
      </el-descriptions-item>
    </el-descriptions>

    <template #footer>
      <el-button type="primary" @click="visible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.device-detail :deep(.el-descriptions__label) {
  width: 112px;
  color: #475569;
  font-weight: 700;
}

.device-detail :deep(.el-descriptions__content) {
  color: var(--text-primary);
  line-height: 1.6;
  word-break: break-word;
}

@media (max-width: 720px) {
  .device-detail :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
  }
}
</style>
