<script setup>
import { computed } from 'vue'
import { Switch } from '@element-plus/icons-vue'
import { formatLabLabel, getLabId } from '../../../api/services/deviceLabBinding'

defineOptions({
  name: 'DeviceTransferDialog',
})

const props = defineProps({
  devices: {
    type: Array,
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  labOptions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  ownerOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const isBatch = computed(() => props.devices.length > 1)
const firstDevice = computed(() => props.devices[0] || null)
const title = computed(() => (isBatch.value ? `批量调拨 ${props.devices.length} 台设备` : '设备调拨'))
const getOwnerName = (user) => user.realName || user.name || user.username
const getDeviceName = (device = {}) => device.deviceName || device.name || '未命名设备'
const getDeviceCode = (device = {}) => device.assetNo || device.code || device.deviceCode || '-'
const getDeviceLab = (device = {}) => device.labName || device.lab || device.roomNo || '未关联实验室'
const getDeviceOwner = (device = {}) => device.owner || device.ownerName || device.ownerUsername || '未设置责任人'
</script>

<template>
  <el-dialog v-model="visible" align-center destroy-on-close :title="title" width="min(720px, calc(100vw - 32px))">
    <div v-if="devices.length" class="transfer-summary">
      <el-icon>
        <Switch />
      </el-icon>
      <div>
        <h3>{{ isBatch ? `已选择 ${devices.length} 台设备` : getDeviceName(firstDevice) }}</h3>
        <span v-if="isBatch">{{ devices.map((device) => getDeviceCode(device)).join('、') }}</span>
        <span v-else>{{ getDeviceCode(firstDevice) }} · {{ getDeviceLab(firstDevice) }} · {{ getDeviceOwner(firstDevice) }}</span>
      </div>
    </div>

    <el-alert
      v-if="isBatch"
      class="batch-alert"
      :closable="false"
      show-icon
      title="批量调拨会把已勾选设备统一更新为下方填写的目标实验室、责任人或安装位置；未填写的字段保持原值。"
      type="info"
    />

    <el-form class="transfer-form" label-position="top" :model="formModel">
      <div class="form-grid">
        <el-form-item label="目标实验室">
          <el-select v-model="formModel.toLabId" class="full-control" clearable filterable placeholder="不变更实验室">
            <el-option v-for="lab in labOptions" :key="getLabId(lab)" :label="formatLabLabel(lab)" :value="getLabId(lab)" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标责任人">
          <el-select v-model="formModel.toOwnerUserId" class="full-control" clearable filterable placeholder="不变更责任人">
            <el-option
              v-for="user in ownerOptions"
              :key="user.id"
              :label="getOwnerName(user)"
              :value="user.id"
            >
              <div class="owner-option">
                <span>{{ getOwnerName(user) }}</span>
                <small v-if="user.roleName">{{ user.roleName }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="安装位置">
        <el-input v-model="formModel.toLocation" clearable placeholder="不填写则保持原安装位置" />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="调拨时间">
          <el-date-picker
            v-model="formModel.transferAt"
            class="full-control"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择调拨时间"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>

        <el-form-item label="经办人" required>
          <el-input v-model="formModel.operatorName" clearable placeholder="请输入经办人" />
        </el-form-item>
      </div>

      <el-form-item label="调拨原因">
        <el-input
          v-model="formModel.reason"
          :rows="3"
          maxlength="300"
          placeholder="填写调拨、责任人变更或安装位置变更原因"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="loading" type="primary" @click="$emit('submit')">保存调拨</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.transfer-summary {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #f8fbff;
}

.transfer-summary .el-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 20px;
}

.transfer-summary h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 16px;
  letter-spacing: 0;
}

.transfer-summary span {
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

.transfer-form {
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

.owner-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 180px;
}

.owner-option small {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
