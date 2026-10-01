<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'

defineOptions({
  name: 'PermissionUserDialog',
})

const props = defineProps({
  departmentOptions: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  roleOptions: {
    type: Array,
    default: () => [],
  },
  user: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['save', 'update:modelValue'])

const formRef = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const isCreateMode = computed(() => !props.user?.id)

const formModel = reactive({
  account: '',
  department: '',
  email: '',
  enabled: true,
  initialPassword: '',
  name: '',
  phone: '',
  remark: '',
  role: '',
})

const validatePhone = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }

  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('请输入 11 位手机号'))
    return
  }

  callback()
}

const rules = computed(() => ({
  account: [{ required: true, message: '请输入账号 / 工号', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }],
  initialPassword: isCreateMode.value
    ? [
        { required: true, message: '请输入初始密码', trigger: 'blur' },
        { min: 6, message: '初始密码不少于 6 位', trigger: 'blur' },
      ]
    : [],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}))

const resetForm = () => {
  formModel.account = props.user?.account || props.user?.username || props.user?.employeeNo || ''
  formModel.department = props.user?.department || ''
  formModel.email = props.user?.email || ''
  formModel.enabled = props.user?.enabled ?? true
  formModel.initialPassword = ''
  formModel.name = props.user?.name || props.user?.realName || ''
  formModel.phone = props.user?.phone || ''
  formModel.remark = props.user?.remark || ''
  formModel.role = props.user?.role || props.user?.roleName || props.roleOptions[0] || ''

  nextTick(() => formRef.value?.clearValidate())
}

watch(
  () => [props.modelValue, props.user],
  () => {
    if (props.modelValue) resetForm()
  },
  { immediate: true, deep: true },
)

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    emit('save', { ...formModel })
  } catch {
    // Element Plus 已经在表单项上展示校验信息。
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    align-center
    :title="isCreateMode ? '新增用户' : '编辑用户'"
    width="min(720px, calc(100vw - 32px))"
  >
    <el-form ref="formRef" :model="formModel" :rules="rules" label-position="top" class="user-form">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="formModel.name" placeholder="请输入姓名" />
      </el-form-item>

      <el-form-item label="账号 / 工号" prop="account">
        <el-input v-model="formModel.account" placeholder="请输入账号或工号" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="formModel.phone" maxlength="11" placeholder="请输入手机号" />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formModel.email" placeholder="请输入邮箱" />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-select v-model="formModel.role" class="full-width" placeholder="请选择角色">
          <el-option v-for="item in roleOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item label="部门" prop="department">
        <el-select v-model="formModel.department" allow-create class="full-width" filterable placeholder="请选择或输入部门">
          <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="isCreateMode" label="初始密码" prop="initialPassword">
        <el-input v-model="formModel.initialPassword" placeholder="不少于 6 位" show-password />
      </el-form-item>

      <el-form-item label="状态" prop="enabled">
        <el-radio-group v-model="formModel.enabled">
          <el-radio-button :value="true">启用</el-radio-button>
          <el-radio-button :value="false">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="备注" prop="remark" class="full-span">
        <el-input v-model="formModel.remark" :rows="3" placeholder="填写岗位说明、账号来源或维护备注" type="textarea" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存用户</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.user-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.user-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.full-width {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

@media (max-width: 760px) {
  .user-form {
    grid-template-columns: 1fr;
  }
}
</style>
