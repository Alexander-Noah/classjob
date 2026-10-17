<!--
  Web 管理平台的设备管理弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Tools } from '@element-plus/icons-vue'

defineOptions({
  name: 'DeviceRepairDialog',
})

const props = defineProps({
  device: {
    type: Object,
    default: null,
  },
  devices: {
    type: Array,
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
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const targetDevices = computed(() => (props.devices.length ? props.devices : [props.device].filter(Boolean)))
const isBatch = computed(() => targetDevices.value.length > 1)
const firstDevice = computed(() => targetDevices.value[0] || null)
const title = computed(() => (isBatch.value ? `批量报修 ${targetDevices.value.length} 台设备` : '设备报修登记'))
</script>

<template>
  <el-dialog v-model="visible" align-center destroy-on-close :title="title" width="min(680px, calc(100vw - 32px))">
    <div v-if="firstDevice" class="repair-summary">
      <el-icon><Tools /></el-icon>
      <div>
        <h3>{{ isBatch ? `已选择 ${targetDevices.length} 台设备` : firstDevice.name }}</h3>
        <span v-if="isBatch">{{ targetDevices.map((device) => device.code).join('、') }}</span>
        <span v-else>{{ firstDevice.code }} · {{ firstDevice.lab || '未关联实验室' }} · {{ firstDevice.owner || '未设置责任人' }}</span>
      </div>
    </div>

    <el-alert
      v-if="isBatch"
      class="batch-alert"
      :closable="false"
      show-icon
      title="批量报修会为已勾选设备写入同一故障描述，并同步把设备状态调整为已报修。"
      type="warning"
    />

    <el-form class="repair-form" label-position="top" :model="formModel">
      <div class="form-grid">
        <el-form-item label="报修人" required>
          <el-input v-model="formModel.reporter" clearable placeholder="请输入报修人" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="formModel.contact" clearable placeholder="请输入联系方式" />
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="故障类型">
          <el-select v-model="formModel.faultType" class="full-control">
            <el-option label="硬件故障" value="硬件故障" />
            <el-option label="网络故障" value="网络故障" />
            <el-option label="软件异常" value="软件异常" />
            <el-option label="环境告警" value="环境告警" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="formModel.priority" class="full-control">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="维修负责人">
          <el-input v-model="formModel.assignee" clearable placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="要求完成时间">
          <el-input v-model="formModel.deadline" clearable placeholder="如 今日 18:00" />
        </el-form-item>
      </div>

      <el-form-item label="故障描述" required>
        <el-input
          v-model="formModel.description"
          :rows="4"
          maxlength="300"
          placeholder="填写故障现象、影响范围、现场情况"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="loading" type="primary" @click="$emit('submit')">提交报修</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.repair-summary {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #fff7ed;
}

.repair-summary .el-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #c2410c;
  background: #ffedd5;
  font-size: 20px;
}

.repair-summary h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 16px;
  letter-spacing: 0;
}

.repair-summary span {
  display: block;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.batch-alert {
  margin-bottom: 14px;
}

.repair-form {
  display: grid;
  gap: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.full-control {
  width: 100%;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
