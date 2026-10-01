<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'MaintenanceProcessDialog',
})

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  workOrder: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['save', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const formModel = reactive({
  status: '处理中',
  action: '',
  parts: '',
  nextStep: '',
})

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      formModel.status = props.workOrder?.status === '待派单' ? '处理中' : props.workOrder?.status || '处理中'
      formModel.action = ''
      formModel.parts = ''
      formModel.nextStep = ''
    }
  },
  { immediate: true },
)

const handleSave = () => {
  if (!formModel.status) {
    ElMessage.warning('请选择工单状态')
    return
  }

  if (!formModel.action.trim()) {
    ElMessage.warning('请填写处理过程')
    return
  }

  emit('save', { ...formModel })
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" align-center title="回填处理过程" width="min(620px, calc(100vw - 32px))">
    <el-form label-position="top" class="process-form">
      <el-form-item label="工单状态" required>
        <el-select v-model="formModel.status" class="full-width">
          <el-option label="处理中" value="处理中" />
          <el-option label="待验收" value="待验收" />
          <el-option label="已完成" value="已完成" />
        </el-select>
      </el-form-item>

      <el-form-item label="处理过程" required class="full-span">
        <el-input
          v-model="formModel.action"
          :rows="4"
          maxlength="160"
          placeholder="填写排查步骤、维修动作和现场处理结果"
          show-word-limit
          type="textarea"
        />
      </el-form-item>

      <el-form-item label="更换部件">
        <el-input v-model="formModel.parts" clearable placeholder="例如：更换 SSD、电源模块、网线等" />
      </el-form-item>

      <el-form-item label="后续说明">
        <el-input v-model="formModel.nextStep" clearable placeholder="例如：等待管理员复核、继续观察 24 小时" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存记录</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.process-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.process-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 760px) {
  .process-form {
    grid-template-columns: 1fr;
  }
}
</style>
