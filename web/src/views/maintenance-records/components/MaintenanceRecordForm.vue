<!--
  Web 管理平台的维护记录表单视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createMaintenanceRecord, updateMaintenanceRecord } from '../../../api/services/maintenanceRecords'
import { labResourceService } from '../../../api/services/basicResources'
import { deviceAssetService } from '../../../api/services/deviceAssets'
import { getStoredUser } from '../../../api/services/auth'
import { buildMaintenancePayload, createMaintenanceDraft, devicesForMaintenanceLab } from '../../../modules/maintenance-records/maintenanceRecordEditor'

defineOptions({ name: 'MaintenanceRecordForm' })
const props = defineProps({ record: { type: Object, /** 构造当前类型的默认状态。 @returns 由当前表达式或分支生成的处理结果。 */ default: () => ({}) } })
const emit = defineEmits(['saved', 'cancel', 'saving-change'])
const formRef = ref()
const saving = ref(false)
const loadingOptions = ref(false)
const optionError = ref('')
const labs = ref([])
const devices = ref([])
const form = reactive(createMaintenanceDraft())
const original = ref('')
const availableDevices = computed(/** 计算派生状态。 */ () => devicesForMaintenanceLab(devices.value, form.labId))
const rules = {
  labId: [{ required: true, message: '请选择实验室', trigger: 'change' }],
  maintenanceType: [{ required: true, whitespace: true, message: '请填写维护类型', trigger: 'blur' }],
  maintenanceDate: [{ required: true, message: '请选择维护日期', trigger: 'change' }],
  maintainer: [{ required: true, whitespace: true, message: '请填写维护人员', trigger: 'blur' }],
  content: [{ required: true, whitespace: true, message: '请填写维护内容', trigger: 'blur' }],
  result: [{ required: true, whitespace: true, message: '请填写维护结果', trigger: 'blur' }],
}
watch(/** 响应被监听状态的变化。 */ () => props.record, /** 响应被监听状态的变化。 入参：record。 */ async record => {
  Object.assign(form, createMaintenanceDraft(record, getStoredUser()))
  original.value = JSON.stringify(form)
  await nextTick()
  formRef.value?.clearValidate()
}, { immediate: true })
watch(saving, /** 响应被监听状态的变化。 入参：value。 */ value => emit('saving-change', value))
/**
 * 加载选项。
 */
const loadOptions = async () => {
  loadingOptions.value = true
  optionError.value = ''
  try {
    const [labRows, deviceRows] = await Promise.all([labResourceService.list(), deviceAssetService.list()])
    labs.value = labRows
    devices.value = deviceRows
  } catch {
    optionError.value = '实验室或设备列表加载失败，请重试'
  } finally { loadingOptions.value = false }
}
/**
 * 实验室变化的处理逻辑。
 */
const labChanged = () => {
  form.deviceId = ''
  formRef.value?.clearValidate('deviceId')
}
/**
 * 提交当前操作。
 */
const submit = async () => {
  if (saving.value || loadingOptions.value || optionError.value) return
  const valid = await formRef.value?.validate().catch(/** 处理异步失败结果。 */ () => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = buildMaintenancePayload(form, labs.value, devices.value)
    const identifier = form.id || form.recordNo
    const saved = identifier ? await updateMaintenanceRecord(identifier, payload) : await createMaintenanceRecord(payload)
    original.value = JSON.stringify(form)
    emit('saved', saved)
    ElMessage.success(identifier ? '维护记录已更新' : '维护记录已保存')
  } catch (error) {
    if (!error?.__handled) ElMessage.error(error?.userMessage || error?.message || '维护记录保存失败，请重试')
  } finally { saving.value = false }
}
defineExpose({ /** 判断是否为Dirty。 @returns 由当前表达式或分支生成的处理结果。 */ isDirty: () => JSON.stringify(form) !== original.value })
onMounted(loadOptions)
</script>

<template>
  <el-form ref="formRef" v-loading="loadingOptions" :model="form" :rules="rules" label-position="top" class="record-form" :disabled="saving" @submit.prevent="submit">
    <el-alert v-if="optionError" :title="optionError" type="error" :closable="false" show-icon>
      <el-button link type="primary" @click="loadOptions">重新加载</el-button>
    </el-alert>
    <el-alert v-else-if="!loadingOptions && !labs.length" title="暂无实验室，请先在实验室资源中新增实验室。" type="info" :closable="false" show-icon />
    <div v-if="form.recordNo" class="record-number">记录编号：{{ form.recordNo }}</div>
    <div class="form-grid">
      <el-form-item label="实验室" prop="labId">
        <el-select v-model="form.labId" filterable placeholder="选择实验室" class="full-width" @change="labChanged">
          <el-option v-for="lab in labs" :key="lab.id" :value="String(lab.id)" :label="[lab.code || lab.labCode, lab.name || lab.labName].filter(Boolean).join(' · ')" />
        </el-select>
      </el-form-item>
      <el-form-item label="关联设备（选填）" prop="deviceId">
        <el-select v-model="form.deviceId" filterable clearable :disabled="!form.labId" :placeholder="form.labId ? '可选择设备，也可留空' : '请先选择实验室'" class="full-width" no-data-text="该实验室暂无设备，可不选择">
          <el-option v-for="device in availableDevices" :key="device.id" :value="String(device.id)" :label="[device.assetNo, device.deviceName].filter(Boolean).join(' · ')" />
        </el-select>
      </el-form-item>
      <el-form-item label="维护类型" prop="maintenanceType">
        <el-select v-model="form.maintenanceType" filterable allow-create default-first-option placeholder="选择或填写维护类型" class="full-width">
          <el-option v-for="type in ['巡检', '环境清洁', '设施保养', '安全检查', '软件维护', '其他']" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item label="维护日期" prop="maintenanceDate">
        <el-date-picker v-model="form.maintenanceDate" type="date" value-format="YYYY-MM-DD" placeholder="选择维护日期" class="full-width" />
      </el-form-item>
      <el-form-item label="维护人员" prop="maintainer">
        <el-input v-model="form.maintainer" placeholder="填写维护人员姓名" maxlength="100" />
      </el-form-item>
    </div>
    <el-form-item label="维护内容" prop="content">
      <el-input v-model="form.content" type="textarea" :rows="3" placeholder="记录实验室环境、设施、安全检查及本次维护内容" maxlength="5000" show-word-limit />
    </el-form-item>
    <el-form-item label="维护结果" prop="result">
      <el-input v-model="form.result" type="textarea" :rows="3" placeholder="填写维护完成情况、检查结论及后续建议" maxlength="5000" show-word-limit />
    </el-form-item>
    <el-form-item label="备注" prop="remark">
      <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选填写补充说明" maxlength="100" show-word-limit />
    </el-form-item>
    <div class="form-actions">
      <el-button :disabled="saving" @click="emit('cancel')">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="loadingOptions || Boolean(optionError) || !labs.length" @click="submit">保存维护记录</el-button>
    </div>
  </el-form>
</template>

<style scoped>
.record-form { width: 100%; }
.record-form > .el-alert { margin-bottom: 16px; }
.record-number { margin-bottom: 18px; color: var(--text-secondary); font-size: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.full-width { width: 100%; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; padding-top: 8px; }
@media (max-width: 680px) { .form-grid { grid-template-columns: 1fr; } }
</style>
