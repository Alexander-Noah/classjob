<!--
  Web 管理平台的业务记录弹窗视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed, ref } from 'vue'

defineOptions({
  name: 'BusinessRecordDialog',
})

const props = defineProps({
  dialogTitle: {
    type: String,
    required: true,
  },
  editableColumns: {
    type: Array,
    default: () => [],
  },
  formModel: {
    type: Object,
    required: true,
  },
  fieldOptions: {
    type: Object,
    default: () => ({}),
  },
  formSubmitting: {
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
  statusProp: {
    type: String,
    default: 'status',
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])
const formRef = ref(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const normalizedColumns = computed(() => props.editableColumns.filter((column) => column?.prop))

// 表格展示字段和表单提交字段可以分开：
// 例如表格展示 labName，表单提交 labId。
const getFormProp = (column) => column.formProp || column.modelProp || column.prop

const isFieldRequired = (column) => column.required !== false && !column.optional

const normalizeOption = (option, column = {}) => {
  if (option && typeof option === 'object') {
    const labelKey = column.optionLabel || column.labelKey
    const valueKey = column.optionValue || column.valueKey
    const label =
      option[labelKey] ??
      option.label ??
      option.labName ??
      option.teacherName ??
      option.courseName ??
      option.name ??
      option.realName ??
      option.username ??
      option.roomName ??
      option.classroomName ??
      option.title ??
      option.value ??
      ''
    const value =
      option[valueKey] ??
      option.value ??
      option.id ??
      option.teacherId ??
      option.courseId ??
      option.code ??
      option[labelKey] ??
      option.label ??
      option.teacherName ??
      option.courseName ??
      option.name ??
      ''

    return {
      label,
      value,
      displayValue: option.displayName ?? option.labName ?? option.name ?? label,
      type: option.type ?? option.tagType ?? '',
    }
  }

  return {
    label: option,
    value: option,
    type: '',
  }
}

const getColumnOptions = (column) => {
  const formProp = getFormProp(column)

  if ((column.prop === props.statusProp || formProp === props.statusProp) && props.statusOptions.length) {
    return props.statusOptions.map((option) => normalizeOption(option, column))
  }

  const fieldOptions = props.fieldOptions[formProp] || props.fieldOptions[column.prop]
  const columnOptions = Array.isArray(column.options) ? column.options : []
  const options = Array.isArray(fieldOptions) && fieldOptions.length ? fieldOptions : columnOptions

  // 添加调试日志
  if (formProp === 'labId' || column.prop === 'lab') {
    console.log('=== 实验室字段选项调试 ===')
    console.log('column:', column)
    console.log('formProp:', formProp)
    console.log('column.prop:', column.prop)
    console.log('props.fieldOptions:', props.fieldOptions)
    console.log('fieldOptions[formProp]:', props.fieldOptions[formProp])
    console.log('fieldOptions[column.prop]:', props.fieldOptions[column.prop])
    console.log('最终options:', options)
  }

  return options
    .map((option) => normalizeOption(option, column))
    .filter((option) => option.label !== '' && option.value !== '' && option.value !== undefined && option.value !== null)
}

const hasOptions = (column) => getColumnOptions(column).length > 0

const canCreateOption = (column) => {
  const formProp = getFormProp(column)

  if (column.allowCreate === false) return false
  if (column.prop === props.statusProp || formProp === props.statusProp) return false
  if (['labId', 'lab', 'labName', 'labs', 'teacher', 'teacherName', 'className'].includes(formProp)) return false

  return column.allowCreate === true || ['name', 'version', 'software', 'softwareList', 'course'].includes(formProp)
}

const getFieldKind = (column) => {
  const manualType = column.formType || column.fieldType
  if (manualType) return manualType

  const tableType = column.type
  if (['select', 'date', 'datetime', 'textarea', 'number', 'input'].includes(tableType)) return tableType
  if (column.prop === props.statusProp || getFormProp(column) === props.statusProp || tableType === 'tag' || hasOptions(column)) return 'select'

  const text = `${column.prop || ''} ${column.label || ''}`.toLowerCase()

  if (/(说明|备注|描述|详情|原因|内容|要求|description|remark|note|content)/.test(text)) return 'textarea'
  if (/(日期|到期|购置|质保|检修|盘点|date|expire|expiry|purchase)/.test(text)) return 'date'
  if (/(时间|datetime|timestamp)/.test(text)) return 'datetime'
  if (/(数量|人数|金额|费用|次数|时长|课时|容量|价格|count|num|amount|price|cost|duration|hour)/.test(text)) return 'number'

  return 'input'
}

const getPlaceholder = (column) => {
  const fieldKind = getFieldKind(column)
  if (fieldKind === 'select') return `请选择${column.label}`
  if (fieldKind === 'date' || fieldKind === 'datetime') return `请选择${column.label}`
  return `请输入${column.label}`
}

const shouldFullRow = (column) => {
  const fieldKind = getFieldKind(column)
  const text = `${column.prop || ''} ${column.label || ''}`.toLowerCase()
  return fieldKind === 'textarea' || /(说明|备注|描述|详情|内容|要求|description|remark|note|content)/.test(text)
}


const handleSelectChange = (column, value) => {
  const formProp = getFormProp(column)
  const option = getColumnOptions(column).find((item) => item.value === value)

  // formProp 和 prop 不一致时，同步显示字段。
  // 例如 formProp=labId 用于提交，prop=labName 用于表格/详情展示。
  if (column.prop && column.prop !== formProp && option) {
    formModelSet(column.prop, option.displayValue || option.label)
  }
}

const formModelSet = (prop, value) => {
  props.formModel[prop] = value
}

const formRules = computed(() => {
  const rules = {}

  normalizedColumns.value.forEach((column) => {
    if (!isFieldRequired(column)) return

    const fieldKind = getFieldKind(column)
    const actionText = ['select', 'date', 'datetime'].includes(fieldKind) ? '选择' : '填写'
    const trigger = ['select', 'date', 'datetime'].includes(fieldKind) ? 'change' : 'blur'

    rules[getFormProp(column)] = [
      {
        required: true,
        message: `请${actionText}${column.label}`,
        trigger,
      },
    ]
  })

  return rules
})

const handleCancel = () => {
  visible.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) {
    emit('submit')
    return
  }

  try {
    await formRef.value.validate()
    emit('submit')
  } catch {
    // Element Plus 会自动在表单项下方显示错误提示
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    align-center
    append-to-body
    class="record-dialog"
    destroy-on-close
    draggable
    width="min(720px, calc(100vw - 32px))"
  >
    <template #header>
      <div class="dialog-header">
        <div class="dialog-icon">{{ dialogTitle.slice(0, 1) }}</div>
        <div class="dialog-title-wrap">
          <h3>{{ dialogTitle }}</h3>
          <p>请完善基础信息、授权状态和所属实验室，带 * 的字段为必填项。</p>
        </div>
      </div>
    </template>

    <el-alert
      class="dialog-tip"
      :closable="false"
      show-icon
      title="建议填写真实版本号、授权情况和安装状态，便于后续环境盘点、课程适配和统计分析。"
      type="info"
    />

    <el-form
      ref="formRef"
      class="record-form"
      :model="formModel"
      :rules="formRules"
      label-position="top"
      status-icon
    >
      <div class="form-grid">
        <el-form-item
          v-for="column in normalizedColumns"
          :key="`${column.prop}-${getFormProp(column)}`"
          :class="{ 'is-wide': shouldFullRow(column) }"
          :label="column.label"
          :prop="getFormProp(column)"
          :required="isFieldRequired(column)"
        >
          <el-select
              v-if="getFieldKind(column) === 'select'"
              v-model="formModel[getFormProp(column)]"
              :allow-create="canCreateOption(column)"
              class="full-control"
              clearable
              :default-first-option="canCreateOption(column)"
              filterable
              :placeholder="getPlaceholder(column)"
              @change="handleSelectChange(column, $event)"
          >
            <el-option
                v-for="option in getColumnOptions(column)"
                :key="String(option.value)"
                :label="option.label"
                :value="option.value"
            >
              <div class="option-row">
                <span>{{ option.label }}</span>
                <el-tag
                    v-if="option.type"
                    :type="option.type"
                    size="small"
                    effect="light"
                >
                  {{ option.label }}
                </el-tag>
              </div>
            </el-option>

            <template #empty>
              <div class="select-empty">
                暂无可选数据
              </div>
            </template>
          </el-select>

          <el-date-picker
              v-else-if="getFieldKind(column) === 'date'"
              v-model="formModel[getFormProp(column)]"
              class="full-control"
              clearable
              format="YYYY-MM-DD"
              :placeholder="getPlaceholder(column)"
              type="date"
              value-format="YYYY-MM-DD"
          />

          <el-date-picker
              v-else-if="getFieldKind(column) === 'datetime'"
              v-model="formModel[getFormProp(column)]"
              class="full-control"
              clearable
              format="YYYY-MM-DD HH:mm"
              :placeholder="getPlaceholder(column)"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
          />

          <el-input-number
              v-else-if="getFieldKind(column) === 'number'"
              v-model="formModel[getFormProp(column)]"
              class="full-control"
              controls-position="right"
              :min="0"
              :placeholder="getPlaceholder(column)"
          />

          <el-input
              v-else-if="getFieldKind(column) === 'textarea'"
              v-model="formModel[getFormProp(column)]"
              :autosize="{ minRows: 3, maxRows: 6 }"
              clearable
              maxlength="200"
              :placeholder="getPlaceholder(column)"
              show-word-limit
              type="textarea"
          />

          <el-input
              v-else
              v-model="formModel[getFormProp(column)]"
              clearable
              :placeholder="getPlaceholder(column)"
          />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <span>保存后会同步刷新左侧表格数据。</span>
        <div class="footer-actions">
          <el-button @click="handleCancel">取消</el-button>
          <el-button :loading="formSubmitting" type="primary" @click="handleSubmit">保存</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.record-dialog {
  max-width: calc(100vw - 32px);
  border-radius: 14px;
  overflow: hidden;
}

.record-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #edf1f7;
}

.record-dialog :deep(.el-dialog__body) {
  padding: 18px 22px 8px;
}

.record-dialog :deep(.el-dialog__footer) {
  padding: 14px 22px 18px;
  border-top: 1px solid #edf1f7;
  background: #fafcff;
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-right: 28px;
}

.dialog-icon {
  display: inline-flex;
  flex: 0 0 40px;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1f6feb, #31c48d);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.dialog-title-wrap h3 {
  margin: 0;
  color: #111827;
  font-size: 17px;
  font-weight: 700;
}

.dialog-title-wrap p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.dialog-tip {
  margin-bottom: 16px;
  border-radius: 10px;
}

.record-form {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 17px;
}

.form-grid :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #374151;
  font-weight: 600;
  line-height: 1.2;
}

.form-grid :deep(.el-input__wrapper),
.form-grid :deep(.el-select__wrapper),
.form-grid :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.is-wide {
  grid-column: 1 / -1;
}

.full-control {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.dialog-footer span {
  color: #8a94a6;
  font-size: 12px;
}

.footer-actions {
  display: flex;
  gap: 10px;
}

.footer-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 760px) {
  .record-dialog {
    width: calc(100vw - 24px) !important;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .footer-actions {
    justify-content: flex-end;
  }
}
</style>
