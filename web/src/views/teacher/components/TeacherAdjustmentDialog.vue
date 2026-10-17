<!--
  Web 管理平台的教师工作台弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'TeacherAdjustmentDialog',
})

const props = defineProps({
  courseOptions: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  prefill: {
    type: Object,
    default: null,
  },
  title: {
    type: String,
    default: '提交调课申请',
  },
})

const emit = defineEmits(['save', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const formModel = reactive({
  course: '',
  originalTime: '',
  targetTime: '',
  reason: '',
})

const weekOptions = ['上一周', '5月 第10周', '下一周', '5月 第11周', '5月 第12周']
const dayOptions = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const periodOptions = [
  { label: '第1节 08:30-10:00', value: '第1节 08:30-10:00' },
  { label: '第2节 10:20-11:50', value: '第2节 10:20-11:50' },
  { label: '第3节 14:00-15:30', value: '第3节 14:00-15:30' },
  { label: '第4节 15:50-17:20', value: '第4节 15:50-17:20' },
  { label: '第5节 18:40-20:10', value: '第5节 18:40-20:10' },
  { label: '第6节 20:20-21:50', value: '第6节 20:20-21:50' },
]

const timeModel = reactive({
  original: {
    week: '5月 第10周',
    day: '',
    period: '',
  },
  target: {
    week: '下一周',
    day: '',
    period: '',
  },
})

const timeFieldMap = {
  original: 'originalTime',
  target: 'targetTime',
}

const parseTimeText = (timeText = '', fallbackWeek = '') => ({
  week: weekOptions.find((item) => timeText.includes(item)) || fallbackWeek,
  day: dayOptions.find((item) => timeText.includes(item)) || '',
  period: periodOptions.find((item) => timeText.includes(item.value))?.value || '',
})

const buildTimeText = (timeValue) => {
  if (!timeValue.week || !timeValue.day || !timeValue.period) return ''
  return `${timeValue.week} ${timeValue.day} ${timeValue.period}`
}

const syncTimeText = (field) => {
  const value = buildTimeText(timeModel[field])
  if (value) {
    formModel[timeFieldMap[field]] = value
  }
}

const resetForm = () => {
  formModel.course = props.prefill?.course || ''
  formModel.originalTime = props.prefill?.originalTime || ''
  formModel.targetTime = props.prefill?.targetTime || ''
  formModel.reason = props.prefill?.reason || ''

  Object.assign(timeModel.original, parseTimeText(formModel.originalTime, '5月 第10周'))
  Object.assign(timeModel.target, parseTimeText(formModel.targetTime, '下一周'))
}

watch(
  () => [props.modelValue, props.prefill],
  () => {
    if (props.modelValue) {
      resetForm()
    }
  },
  { immediate: true, deep: true },
)

watch(
  timeModel,
  () => {
    syncTimeText('original')
    syncTimeText('target')
  },
  { deep: true },
)

const handleSave = () => {
  if (!formModel.course) {
    ElMessage.warning('请选择课程')
    return
  }

  if (!formModel.originalTime.trim()) {
    ElMessage.warning('请填写原上课时间')
    return
  }

  if (!formModel.targetTime.trim()) {
    ElMessage.warning('请填写目标调课时间')
    return
  }

  if (!formModel.reason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }

  emit('save', { ...formModel })
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" align-center :title="title" width="min(620px, calc(100vw - 32px))">
    <el-form label-position="top" class="adjustment-form">
      <el-form-item label="课程名称" required>
        <el-select
          v-model="formModel.course"
          allow-create
          class="full-width"
          default-first-option
          filterable
          placeholder="请选择或输入课程"
        >
          <el-option v-for="item in courseOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <div class="field-tip">未同步到列表的课程可以直接输入课程名称。</div>
      </el-form-item>

      <el-form-item label="原上课时间" required class="full-span">
        <div class="time-select-group">
          <el-select v-model="timeModel.original.week" placeholder="周次">
            <el-option v-for="item in weekOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="timeModel.original.day" placeholder="星期">
            <el-option v-for="item in dayOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="timeModel.original.period" placeholder="节次">
            <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="time-preview">
          {{ formModel.originalTime || '请选择原课程所在周次、星期和节次' }}
        </div>
      </el-form-item>

      <el-form-item label="目标调课时间" required class="full-span">
        <div class="time-select-group">
          <el-select v-model="timeModel.target.week" placeholder="周次">
            <el-option v-for="item in weekOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="timeModel.target.day" placeholder="星期">
            <el-option v-for="item in dayOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="timeModel.target.period" placeholder="节次">
            <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="time-preview">
          {{ formModel.targetTime || '请选择希望调整到的周次、星期和节次' }}
        </div>
      </el-form-item>

      <el-form-item label="申请原因" required class="full-span">
        <el-input
          v-model="formModel.reason"
          :rows="4"
          maxlength="120"
          placeholder="请填写调课原因、冲突背景和希望协调的实验室安排"
          show-word-limit
          type="textarea"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.adjustment-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.adjustment-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

.time-select-group {
  display: grid;
  grid-template-columns: 0.9fr 0.8fr 1.5fr;
  gap: 8px;
  width: 100%;
}

.time-preview {
  width: 100%;
  margin-top: 8px;
  padding: 7px 10px;
  border-radius: 6px;
  background: var(--module-surface-muted, #f6f8fc);
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.field-tip {
  width: 100%;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.time-select-group :deep(.el-select) {
  width: 100%;
}

@media (max-width: 760px) {
  .adjustment-form {
    grid-template-columns: 1fr;
  }

  .time-select-group {
    grid-template-columns: 1fr;
  }
}
</style>
