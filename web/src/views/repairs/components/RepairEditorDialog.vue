<script setup>
defineOptions({
  name: 'RepairEditorDialog',
})

defineProps({
  faultTypeOptions: {
    type: Array,
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  deviceOptions: {
    type: Array,
    default: () => [],
  },
  labOptions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  priorityOptions: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    required: true,
  },
})

defineEmits(['submit'])

const visible = defineModel({ type: Boolean, default: false })
const getLabOptionLabel = (lab) => (lab && typeof lab === 'object' ? lab.label : lab)
const getLabOptionValue = (lab) => (lab && typeof lab === 'object' ? lab.value : lab)
</script>

<template>
  <el-dialog v-model="visible" align-center :title="title" width="min(680px, calc(100vw - 32px))">
    <el-form :model="formModel" label-position="top">
      <el-row :gutter="14">
        <el-col :sm="12" :xs="24">
          <el-form-item label="报修人">
            <el-input v-model="formModel.reporter" placeholder="填写报修人" />
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="联系方式">
            <el-input v-model="formModel.contact" placeholder="手机号或分机号" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="所属实验室">
            <el-select v-model="formModel.lab" class="full-field">
              <el-option v-for="lab in labOptions" :key="getLabOptionValue(lab)" :label="getLabOptionLabel(lab)" :value="getLabOptionValue(lab)" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="故障设备">
            <el-select v-model="formModel.device" allow-create class="full-field" filterable placeholder="请选择或输入故障设备">
              <el-option v-for="device in deviceOptions" :key="device" :label="device" :value="device" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="故障类型">
            <el-select v-model="formModel.faultType" class="full-field">
              <el-option v-for="faultType in faultTypeOptions" :key="faultType" :label="faultType" :value="faultType" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="优先级">
            <el-select v-model="formModel.priority" class="full-field">
              <el-option v-for="priority in priorityOptions" :key="priority" :label="priority" :value="priority" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="处理截止">
            <el-input v-model="formModel.deadline" placeholder="今日 18:00" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="故障描述">
            <el-input v-model="formModel.description" :rows="4" type="textarea" placeholder="描述现象、影响范围和已尝试操作" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="$emit('submit')">保存工单</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.full-field {
  width: 100%;
}
</style>
