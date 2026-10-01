<script setup>
import { computed, ref } from 'vue'
import { formatLabOptionLabel } from '../../../utils/labDisplay'
import { createContactValidator, createMeaningfulTextValidator } from '../../../utils/reservationValidation'

defineOptions({
  name: 'ReservationEditorDialog',
})

const props = defineProps({
  availability: {
    type: Object,
    default: () => ({
      capacity: 0,
      isFree: false,
      isReady: false,
      messages: [],
      reservationConflicts: [],
      timetableConflicts: [],
    }),
  },
  formModel: {
    type: Object,
    required: true,
  },
  labMetaMap: {
    type: Object,
    default: () => ({}),
  },
  labOptions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  sceneOptions: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['submit'])

const visible = defineModel({ type: Boolean, default: false })
const formRef = ref()

const validateTimeRange = (_rule, value, callback) => {
  const [startTime, endTime] = String(value || '').split('-')

  if (!startTime || !endTime) {
    callback(new Error('请选择预约时段'))
    return
  }

  if (startTime >= endTime) {
    callback(new Error('结束时间需晚于开始时间'))
    return
  }

  if (props.availability.timetableConflicts?.length) {
    callback(new Error('该时段已有课表占用'))
    return
  }

  if (props.availability.reservationConflicts?.length) {
    callback(new Error('该时段已有人工预约'))
    return
  }

  callback()
}

const validateAttendees = (_rule, value, callback) => {
  const count = Number(value)

  if (!Number.isInteger(count) || count < 1) {
    callback(new Error('预约人数至少为 1 人'))
    return
  }

  if (attendeeMax.value && count > attendeeMax.value) {
    callback(new Error(`预约人数不能超过 ${attendeeMax.value} 人`))
    return
  }

  callback()
}

const timeRangeModel = computed({
  get() {
    const [startTime, endTime] = String(props.formModel.timeRange || '').split('-')
    return startTime && endTime ? [startTime, endTime] : []
  },
  set(value) {
    props.formModel.timeRange = Array.isArray(value) && value[0] && value[1] ? `${value[0]}-${value[1]}` : ''
  },
})

const rules = {
  applicant: [{ validator: createMeaningfulTextValidator({ label: '申请人', min: 2, max: 50 }), trigger: 'blur' }],
  department: [{ validator: createMeaningfulTextValidator({ label: '学院/组织', min: 2, max: 80, required: false }), trigger: 'blur' }],
  contact: [{ validator: createContactValidator({ label: '联系人', strict: false }), trigger: 'blur' }],
  lab: [{ required: true, message: '请选择预约实验室', trigger: 'change' }],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeRange: [{ validator: validateTimeRange, trigger: 'change' }],
  scene: [{ required: true, message: '请选择预约类型', trigger: 'change' }],
  attendees: [{ validator: validateAttendees, trigger: 'change' }],
  reason: [{ validator: createMeaningfulTextValidator({ label: '预约用途', min: 4, max: 160 }), trigger: 'blur' }],
  note: [{ validator: createMeaningfulTextValidator({ label: '审核备注', min: 4, max: 120, required: false }), trigger: 'blur' }],
}

const attendeeMax = computed(() => Number(props.availability.capacity) || 120)

const availabilityAlert = computed(() => {
  if (!props.formModel.lab || !props.formModel.date || !props.formModel.timeRange) return null

  if (!props.availability.isReady) {
    return {
      type: props.availability.isFree ? 'warning' : 'error',
      title: props.availability.isFree ? '课表数据暂未完成同步' : '当前时段存在冲突',
    }
  }

  if (props.availability.isFree) {
    return {
      type: 'success',
      title: `当前时段空闲${props.availability.capacity ? `，容量 ${props.availability.capacity} 人` : ''}`,
    }
  }

  return {
    type: 'error',
    title: '当前时段不可预约',
  }
})

const handleSubmit = async () => {
  if (!formRef.value) {
    emit('submit')
    return
  }

  try {
    await formRef.value.validate()
    emit('submit')
  } catch {
    // Element Plus has already rendered field-level validation messages.
  }
}

const handleClosed = () => {
  formRef.value?.clearValidate()
}
</script>

<template>
  <el-dialog v-model="visible" align-center class="reservation-editor-dialog" :title="title" width="min(820px, calc(100vw - 32px))" @closed="handleClosed">
    <el-form ref="formRef" :model="formModel" :rules="rules" label-position="top">
      <el-row :gutter="14">
        <el-col :sm="12" :xs="24">
          <el-form-item label="申请人" prop="applicant">
            <el-input v-model="formModel.applicant" placeholder="班级、教师或学生" />
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="学院/组织" prop="department">
            <el-input v-model="formModel.department" placeholder="所属学院或组织" />
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="联系人" prop="contact">
            <el-input v-model="formModel.contact" placeholder="教师或联系电话" />
          </el-form-item>
        </el-col>
        <el-col :sm="12" :xs="24">
          <el-form-item label="预约类型" prop="scene">
            <el-select v-model="formModel.scene" class="full-field" clearable placeholder="请选择预约类型">
              <el-option v-for="scene in sceneOptions" :key="scene" :label="scene" :value="scene" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="预约实验室" prop="lab">
            <el-select v-model="formModel.lab" class="full-field" clearable filterable placeholder="请选择预约实验室">
              <el-option
                v-for="lab in labOptions"
                :key="lab"
                :label="formatLabOptionLabel(labMetaMap[lab] || lab)"
                :value="lab"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :sm="8" :xs="24">
          <el-form-item label="预约日期" prop="date">
            <el-date-picker
              v-model="formModel.date"
              class="full-field"
              clearable
              format="YYYY-MM-DD"
              placeholder="请选择预约日期"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
        <el-col :sm="8" :xs="24">
          <el-form-item label="预约时段" prop="timeRange">
            <el-time-picker
              v-model="timeRangeModel"
              class="full-field"
              clearable
              end-placeholder="请选择结束时间"
              format="HH:mm"
              is-range
              range-separator="至"
              start-placeholder="请选择开始时间"
              value-format="HH:mm"
              @change="formRef?.validateField('timeRange')"
            />
          </el-form-item>
        </el-col>
        <el-col :sm="8" :xs="24">
          <el-form-item label="预约人数" prop="attendees">
            <el-input-number v-model="formModel.attendees" class="full-field" :min="1" :max="attendeeMax" :precision="0" :step="1" />
          </el-form-item>
        </el-col>
        <el-col v-if="availabilityAlert" :span="24">
          <el-alert class="availability-alert" :closable="false" show-icon :title="availabilityAlert.title" :type="availabilityAlert.type" />
          <div v-if="availability.messages?.length" class="availability-messages">
            <el-tag v-for="message in availability.messages.slice(0, 4)" :key="message" effect="plain" type="warning">
              {{ message }}
            </el-tag>
          </div>
        </el-col>
        <el-col :span="24">
          <el-form-item label="预约用途" prop="reason">
            <el-input v-model="formModel.reason" :rows="3" type="textarea" placeholder="填写课程、自主实验或活动说明" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="审核备注" prop="note">
            <el-input v-model="formModel.note" :rows="2" type="textarea" placeholder="管理员备注，可留空" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">保存预约</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.full-field {
  width: 100%;
}

.availability-alert {
  margin-bottom: 10px;
}

.availability-messages {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
