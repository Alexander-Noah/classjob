<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

defineOptions({
  name: 'TeacherEnvironmentRequestDialog',
})

const props = defineProps({
  labTypeOptions: {
    type: Array,
    default: () => ['开发实验室', '数据库实验室', 'GPU 实验室', '网络安全实验室', '物联网实验室'],
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  prefill: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['save', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const resolvedLabTypeOptions = computed(() => {
  const options = [...props.labTypeOptions]
  if (props.prefill?.labType && !options.includes(props.prefill.labType)) {
    options.unshift(props.prefill.labType)
  }
  return options
})

const formModel = reactive({
  courseName: '',
  className: '',
  useTime: '',
  labType: '',
  softwareRequirements: '',
  systemRequirements: '',
  specialRequirements: '',
})

const resetForm = () => {
  formModel.courseName = props.prefill?.courseName || ''
  formModel.className = props.prefill?.className || ''
  formModel.useTime = props.prefill?.useTime || ''
  formModel.labType = props.prefill?.labType || resolvedLabTypeOptions.value[0] || ''
  formModel.softwareRequirements = props.prefill?.softwareRequirements || ''
  formModel.systemRequirements = ''
  formModel.specialRequirements = props.prefill?.specialRequirements || ''
}

watch(
  () => [props.modelValue, props.prefill],
  (value) => {
    if (value[0]) {
      resetForm()
    }
  },
  { immediate: true, deep: true },
)

const handleSave = () => {
  if (!formModel.courseName.trim()) {
    ElMessage.warning('请填写课程名称')
    return
  }

  if (!formModel.className.trim()) {
    ElMessage.warning('请填写授课班级')
    return
  }

  if (!formModel.useTime.trim()) {
    ElMessage.warning('请填写使用时间')
    return
  }

  if (!formModel.labType.trim()) {
    ElMessage.warning('请选择实验室类型')
    return
  }

  if (!formModel.softwareRequirements.trim()) {
    ElMessage.warning('请填写软件环境要求')
    return
  }

  emit('save', { ...formModel })
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" align-center title="提交课程环境需求" width="min(680px, calc(100vw - 32px))">
    <el-form label-position="top" class="environment-request-form">
      <el-form-item label="课程名称" required>
        <el-input v-model="formModel.courseName" clearable placeholder="例如：Java Web 程序设计" />
      </el-form-item>

      <el-form-item label="授课班级" required>
        <el-input v-model="formModel.className" clearable placeholder="例如：软件工程 2 班" />
      </el-form-item>

      <el-form-item label="使用时间" required>
        <el-input v-model="formModel.useTime" clearable placeholder="例如：第 10 周 周三 08:00-09:40" />
      </el-form-item>

      <el-form-item label="实验室类型" required>
        <el-select v-model="formModel.labType" class="full-width" placeholder="请选择实验室类型">
          <el-option v-for="item in resolvedLabTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="软件环境要求" required class="full-span">
        <el-input
          v-model="formModel.softwareRequirements"
          :rows="3"
          maxlength="180"
          placeholder="例如：JDK 21、IDEA、MySQL 8、Tomcat 10，需准备课程项目模板"
          show-word-limit
          type="textarea"
        />
      </el-form-item>

      <el-form-item label="系统/镜像要求">
        <el-input v-model="formModel.systemRequirements" clearable placeholder="例如：Windows 11 或 Ubuntu 22.04" />
      </el-form-item>

      <el-form-item label="特殊说明">
        <el-input v-model="formModel.specialRequirements" clearable placeholder="例如：需要开放外网、预置数据集或学生账号" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">提交需求</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.environment-request-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.environment-request-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 760px) {
  .environment-request-form {
    grid-template-columns: 1fr;
  }
}
</style>
