<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'MaintenanceRecordDetail',
})

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  record: { type: Object, default: null },
  editable: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'edit'])
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<template>
  <el-drawer v-model="visible" title="维护记录详情" size="min(540px, 100%)">
    <el-descriptions v-if="record" :column="1" border>
      <el-descriptions-item label="记录编号">{{ record.recordNo }}</el-descriptions-item>
      <el-descriptions-item label="实验室">{{ record.lab }}</el-descriptions-item>
      <el-descriptions-item label="设备">{{ record.device }}</el-descriptions-item>
      <el-descriptions-item label="维护类型">{{ record.maintenanceType }}</el-descriptions-item>
      <el-descriptions-item label="维护日期">{{ record.maintenanceDate }}</el-descriptions-item>
      <el-descriptions-item label="维护人员">{{ record.maintainer }}</el-descriptions-item>
      <el-descriptions-item label="维护内容">{{ record.content }}</el-descriptions-item>
      <el-descriptions-item label="处理结果">{{ record.result }}</el-descriptions-item>
      <el-descriptions-item label="备注">{{ record.remark }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ record.updatedAt }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button v-if="record && editable" type="primary" @click="emit('edit', record)">编辑记录</el-button>
    </template>
  </el-drawer>
</template>
