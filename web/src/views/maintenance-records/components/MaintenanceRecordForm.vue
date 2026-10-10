<script setup>
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createMaintenanceRecord, updateMaintenanceRecord } from '../../../api/services/maintenanceRecords'

defineOptions({
  name: 'MaintenanceRecordForm',
})

const props = defineProps({
  record: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['saved'])

const emptyRecord = () => ({
  id: '',
  recordNo: '',
  lab: '',
  device: '',
  maintenanceType: '',
  maintenanceDate: '',
  maintainer: '',
  content: '',
  result: '',
  remark: '',
})

const formRef = ref()
const saving = ref(false)
const form = reactive(emptyRecord())

const rules = {
  lab: [{ required: true, message: '请填写实验室', trigger: 'blur' }],
  device: [{ required: true, message: '请填写设备', trigger: 'blur' }],
  maintenanceType: [{ required: true, message: '请填写维护类型', trigger: 'blur' }],
  maintenanceDate: [{ required: true, message: '请选择维护日期', trigger: 'change' }],
  maintainer: [{ required: true, message: '请填写维护人员', trigger: 'blur' }],
  content: [{ required: true, message: '请填写维护内容', trigger: 'blur' }],
  result: [{ required: true, message: '请填写处理结果', trigger: 'blur' }],
}

const applyRecord = (record = {}) => {
  Object.assign(form, emptyRecord(), record, {
    maintenanceDate: String(record.maintenanceDate || '').slice(0, 10),
  })
}

watch(
  () => props.record,
  (record) => applyRecord(record),
  { immediate: true, deep: true },
)

const toPayload = () => ({
  lab: form.lab.trim(),
  device: form.device.trim(),
  maintenanceType: form.maintenanceType.trim(),
  maintenanceDate: form.maintenanceDate,
  maintainer: form.maintainer.trim(),
  content: form.content.trim(),
  result: form.result.trim(),
  remark: form.remark.trim(),
})

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = toPayload()
    const identifier = form.recordNo || form.id
    const savedRecord = identifier
      ? await updateMaintenanceRecord(identifier, payload)
      : await createMaintenanceRecord(payload)

    emit('saved', savedRecord)
    ElMessage.success(identifier ? '维护记录已更新' : '维护记录已保存')
  } catch (error) {
    ElMessage.error(error?.message || '维护记录保存失败，请检查填写内容')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="record-form">
    <div v-if="form.recordNo" class="record-number">记录编号：{{ form.recordNo }}</div>
    <div class="form-grid">
      <el-form-item label="实验室" prop="lab">
        <el-input v-model="form.lab" placeholder="填写实验室名称、编号或 ID" />
      </el-form-item>
      <el-form-item label="设备" prop="device">
        <el-input v-model="form.device" placeholder="填写设备名称、编号或 ID" />
      </el-form-item>
      <el-form-item label="维护类型" prop="maintenanceType">
        <el-input v-model="form.maintenanceType" placeholder="如：巡检、保养、故障处理" />
      </el-form-item>
      <el-form-item label="维护日期" prop="maintenanceDate">
        <el-date-picker
          v-model="form.maintenanceDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择维护日期"
          class="full-width"
        />
      </el-form-item>
      <el-form-item label="维护人员" prop="maintainer">
        <el-input v-model="form.maintainer" placeholder="填写维护人员姓名" />
      </el-form-item>
    </div>
    <el-form-item label="维护内容" prop="content">
      <el-input v-model="form.content" type="textarea" :rows="4" placeholder="描述本次维护内容" />
    </el-form-item>
    <el-form-item label="处理结果" prop="result">
      <el-input v-model="form.result" type="textarea" :rows="4" placeholder="填写处理结果" />
    </el-form-item>
    <el-form-item label="备注" prop="remark">
      <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选填写补充说明" />
    </el-form-item>
    <div class="form-actions">
      <el-button type="primary" :loading="saving" @click="submit">保存维护记录</el-button>
    </div>
  </el-form>
</template>

<style scoped>
.record-form { max-width: 900px; }
.record-number { margin-bottom: 18px; color: var(--text-secondary); font-size: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.full-width { width: 100%; }
.form-actions { display: flex; justify-content: flex-end; margin-top: 20px; }
@media (max-width: 680px) { .form-grid { grid-template-columns: 1fr; } }
</style>
