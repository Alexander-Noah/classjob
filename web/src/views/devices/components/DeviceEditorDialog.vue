<!--
  Web 管理平台的设备管理编辑弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { applyDeviceLabBinding, formatLabLabel, getLabId } from '../../../api/services/deviceLabBinding'

defineOptions({
  name: 'DeviceEditorDialog',
})

const props = defineProps({
  categoryOptions: {
    type: Array,
    default: () => [],
  },
  labOptions: {
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
  statusOptions: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    default: '设备资产',
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const rules = {
  deviceName: [{ required: true, message: '请填写设备名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择设备分类', trigger: 'change' }],
  labId: [{ required: true, message: '请选择所属实验室', trigger: 'change' }],
  quantity: [{ required: true, message: '请填写数量', trigger: 'blur' }],
  unit: [{ required: true, message: '请填写单位', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const handleLabChange = (labId) => {
  const selectedLab = props.labOptions.find((lab) => String(getLabId(lab)) === String(labId))
  if (!selectedLab) {
    props.formModel.labId = ''
    props.formModel.labName = ''
    props.formModel.roomNo = ''
    props.formModel.location = ''
    props.formModel.installLocation = ''
    return
  }

  Object.assign(props.formModel, applyDeviceLabBinding(props.formModel, selectedLab))
}
</script>

<template>
  <el-dialog
    v-model="visible"
    align-center
    :title="title"
    class="device-editor-dialog"
    width="min(720px, calc(100vw - 32px))"
  >
    <el-scrollbar class="dialog-scroll" max-height="min(58vh, 480px)">
      <div class="dialog-form-body">
        <el-form :model="formModel" :rules="rules" label-position="top">
          <section class="form-section">
            <div class="form-section-title">基础信息</div>
            <div class="form-grid">
              <el-form-item label="资产编号">
                <el-input v-model="formModel.assetNo" placeholder="后端可自动生成，例如 DEV-204-001" />
              </el-form-item>
              <el-form-item label="设备名称" prop="deviceName">
                <el-input v-model="formModel.deviceName" placeholder="例如 联想启天M410" />
              </el-form-item>
              <el-form-item label="设备分类" prop="category">
                <el-select v-model="formModel.category" placeholder="请选择分类">
                  <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
                </el-select>
              </el-form-item>
              <el-form-item label="所属实验室" prop="labId">
                <el-select
                  v-model="formModel.labId"
                  placeholder="请选择所属实验室"
                  filterable
                  clearable
                  style="width: 100%"
                  @change="handleLabChange"
                >
                  <el-option
                    v-for="lab in labOptions"
                    :key="getLabId(lab)"
                    :label="formatLabLabel(lab)"
                    :value="getLabId(lab)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="安装位置">
                <el-input v-model="formModel.roomNo" placeholder="例如 2-204" />
              </el-form-item>
              <el-form-item label="数量" prop="quantity">
                <el-input-number v-model="formModel.quantity" :min="1" :precision="0" controls-position="right" />
              </el-form-item>
              <el-form-item label="单位" prop="unit">
                <el-input v-model="formModel.unit" placeholder="台、套、个、件" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="formModel.status" placeholder="请选择状态">
                  <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
                </el-select>
              </el-form-item>
            </div>
          </section>

          <section class="form-section">
            <div class="form-section-title">规格与备注</div>
            <el-form-item label="规格参数">
              <el-input
                v-model="formModel.specification"
                :autosize="{ minRows: 2, maxRows: 3 }"
                placeholder="主要功能和技术要求"
                type="textarea"
              />
            </el-form-item>
            <el-form-item label="执行标准">
              <el-input
                v-model="formModel.standardRequirement"
                :autosize="{ minRows: 2, maxRows: 3 }"
                placeholder="执行标准和数量要求"
                type="textarea"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="formModel.remark" :autosize="{ minRows: 2, maxRows: 3 }" type="textarea" />
            </el-form-item>
          </section>
        </el-form>
      </div>
    </el-scrollbar>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.device-editor-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
}

.device-editor-dialog :deep(.el-dialog__header) {
  flex: 0 0 auto;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--border-light);
}

.device-editor-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
}

.device-editor-dialog :deep(.el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
}

.device-editor-dialog :deep(.el-dialog__footer) {
  flex: 0 0 auto;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--border-light);
  background: #ffffff;
}

.dialog-form-body {
  padding: 16px 20px 4px;
}

.dialog-scroll {
  min-height: 0;
}

.device-editor-dialog :deep(.el-form-item) {
  margin-bottom: 13px;
}

.form-section + .form-section {
  margin-top: 6px;
}

.form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.form-section-title::before {
  width: 4px;
  height: 14px;
  border-radius: 999px;
  background: var(--primary-color);
  content: "";
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
  row-gap: 0;
}

.form-grid :deep(.el-input-number),
.form-grid :deep(.el-select) {
  width: 100%;
}

@media (max-width: 720px) {
  .device-editor-dialog :deep(.el-dialog) {
    max-height: calc(100vh - 32px);
  }

  .dialog-form-body {
    padding: 14px 16px 2px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
