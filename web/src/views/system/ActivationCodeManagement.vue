<!--
  Web 管理平台的激活码视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CopyDocument,
  Delete,
  Download,
  Edit,
  Key,
  Link,
  MoreFilled,
  Refresh,
  Search,
  SwitchButton,
  View,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHero from '../../components/common/PageHero.vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import {
  batchOperateActivationCodes,
  createActivationCodes,
  getActivationCodePage,
  listActivationAuditLogs,
  listActivationCodeBindings,
  remoteBindActivationDevice,
  remoteUnbindActivationDevice,
  updateActivationCodeRemark,
  updateActivationCodeStatus,
} from '../../api/services/activationCodes'

defineOptions({
  name: 'ActivationCodeManagement',
})

const router = useRouter()
const createFormRef = ref()
const tableRef = ref()
const remoteBindFormRef = ref()
const remoteUnbindFormRef = ref()
const remarkFormRef = ref()
const batchFormRef = ref()

const isLoading = ref(false)
const isGenerating = ref(false)
const actionLoading = ref(false)
const deviceLoading = ref(false)
const auditLoading = ref(false)

const activationCodes = ref([])
const statistics = ref({})
const auditLogs = ref([])
const selectionRows = ref([])
const deviceBindings = ref([])
const generatedCodes = ref([])
const selectedCode = ref(null)
const selectedBinding = ref(null)

const keyword = ref('')
const statusFilter = ref('all')
const expireStatusFilter = ref('all')

const deviceDrawerVisible = ref(false)
const remoteBindDialogVisible = ref(false)
const remoteUnbindDialogVisible = ref(false)
const remarkDialogVisible = ref(false)
const batchDialogVisible = ref(false)
const batchDialogTitle = ref('批量操作')
const batchAction = ref('')

const createForm = reactive({
  deviceLimit: 1,
  ownerName: '',
  expireAt: '',
  remark: '',
  enabled: true,
  quantity: 1,
})

const remoteBindForm = reactive({
  machineCode: '',
  macAddress: '',
  hostname: '',
  clientSource: 'tauri.localhost',
  remark: '',
})

const remoteUnbindForm = reactive({
  reason: '',
})

const remarkForm = reactive({
  ownerName: '',
  remark: '',
  expireAt: '',
})

const batchForm = reactive({
  ownerName: '',
  expireAt: '',
  reason: '',
})

const createRules = {
  deviceLimit: [{ required: true, type: 'number', min: 1, message: '设备额度必须大于 0', trigger: 'change' }],
  quantity: [{ required: true, type: 'number', min: 1, message: '生成数量必须大于 0', trigger: 'change' }],
}

const remoteBindRules = {
  machineCode: [{ required: true, message: '设备标识必填', trigger: 'blur' }],
}

const remoteUnbindRules = {
  reason: [{ required: true, message: '远程解绑原因必填', trigger: 'blur' }],
}

const batchRules = {
  ownerName: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  expireAt: [{ required: true, message: '请选择新的到期时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入操作原因', trigger: 'blur' }],
}

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' },
  { label: '已过期', value: 'expired' },
  { label: '额度已满', value: 'quota_full' },
]

const expireStatusOptions = [
  { label: '全部', value: 'all' },
  { label: '长期有效', value: 'long_term' },
  { label: '即将到期', value: 'expiring_soon' },
  { label: '已过期', value: 'expired' },
]

const selectedIds = computed(() => selectionRows.value.map((row) => row.id || row.code).filter(Boolean))

const authorizationUsage = computed(() => {
  const totalLimit =
    Number(statistics.value.totalDeviceLimit ?? activationCodes.value.reduce((sum, row) => sum + Number(row.deviceLimit || 0), 0)) || 0
  const bound =
    Number(statistics.value.boundDevices ?? activationCodes.value.reduce((sum, row) => sum + Number(row.boundCount || 0), 0)) || 0
  const available = Number(statistics.value.availableDevices ?? Math.max(totalLimit - bound, 0)) || 0
  const percent = totalLimit > 0 ? Math.min(Math.round((bound / totalLimit) * 100), 100) : 0

  return { available, bound, percent, totalLimit }
})

const healthStatusText = computed(() => {
  if (!activationCodes.value.length) return '等待授权数据'
  if (authorizationUsage.value.available <= 0 && authorizationUsage.value.totalLimit > 0) return '设备额度已满'
  if (statistics.value.disabledCodes > statistics.value.enabledCodes) return '停用授权偏多'
  return '授权运行正常'
})

const healthStatusType = computed(() => {
  if (authorizationUsage.value.available <= 0 && authorizationUsage.value.totalLimit > 0) return 'warning'
  if (statistics.value.disabledCodes > statistics.value.enabledCodes) return 'danger'
  return 'success'
})

const quickStatusFilters = computed(() => [
  { label: '全部授权', status: 'all', expireStatus: 'all', value: statistics.value.totalCodes ?? activationCodes.value.length },
  { label: '可用授权', status: 'enabled', expireStatus: 'all', value: statistics.value.enabledCodes ?? 0 },
  { label: '停用授权', status: 'disabled', expireStatus: 'all', value: statistics.value.disabledCodes ?? 0 },
  { label: '即将到期', status: 'all', expireStatus: 'expiring_soon', value: statistics.value.expiringSoonCodes ?? 0 },
])

const normalizeText = (value) => String(value ?? '').trim()

const maskActivationCode = (recordOrCode = '') => {
  const value = typeof recordOrCode === 'object' ? recordOrCode.maskedCode || recordOrCode.code : recordOrCode
  const text = normalizeText(value)
  if (!text) return '-'
  if (text.includes('****')) return text
  if (text.length <= 10) return `${text.slice(0, 2)}****${text.slice(-2)}`
  return `${text.slice(0, 4)}****${text.slice(-4)}`
}

const formatDateTime = (value, emptyText = '-') => {
  if (!value) return emptyText
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const formatExpireAt = (value) => formatDateTime(value, '长期有效')

const getStatusKey = (record = {}) => record.statusKey || (record.enabled === false || record.status === 'disabled' ? 'disabled' : 'enabled')

const getStatusText = (record = {}) => {
  if (record.statusText) return record.statusText
  const key = getStatusKey(record)
  if (key === 'disabled') return '停用'
  if (key === 'expired') return '已过期'
  if (key === 'quota_full') return '额度已满'
  return '启用'
}

const getStatusType = (record = {}) => {
  const key = getStatusKey(record)
  if (key === 'disabled') return 'danger'
  if (key === 'expired') return 'warning'
  if (key === 'quota_full') return 'info'
  return 'success'
}

const getQuotaPercent = (record = {}) => {
  const limit = Number(record.deviceLimit || 0)
  if (!limit) return 0
  return Math.min(Math.round((Number(record.boundCount || 0) / limit) * 100), 100)
}

const getBindingStatusText = (status) => {
  const value = normalizeText(status)
  if (value === 'REMOTE_UNBOUND') return '被强制解绑'
  if (value === 'UNBOUND') return '已解绑'
  if (value === 'EXPIRED') return '已过期'
  return '正常'
}

const getBindingStatusType = (status) => {
  const value = normalizeText(status)
  if (value === 'REMOTE_UNBOUND') return 'danger'
  if (value === 'UNBOUND') return 'info'
  if (value === 'EXPIRED') return 'warning'
  return 'success'
}

const loadActivationCodes = async () => {
  isLoading.value = true
  try {
    const page = await getActivationCodePage({
      keyword: keyword.value,
      status: statusFilter.value,
      expireStatus: expireStatusFilter.value,
      page: 1,
      size: 500,
    })
    activationCodes.value = page.records || []
    statistics.value = page.statistics || {}
  } catch (error) {
    ElMessage.error(error?.message || '激活码列表加载失败')
  } finally {
    isLoading.value = false
  }
}

const loadAuditLogs = async () => {
  auditLoading.value = true
  try {
    auditLogs.value = await listActivationAuditLogs()
  } catch (error) {
    ElMessage.error(error?.message || '审计日志加载失败')
  } finally {
    auditLoading.value = false
  }
}

const reloadAll = async () => {
  await Promise.all([loadActivationCodes(), loadAuditLogs()])
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = 'all'
  expireStatusFilter.value = 'all'
  loadActivationCodes()
}

const applyStatusShortcut = (status, expireStatus = 'all') => {
  statusFilter.value = status
  expireStatusFilter.value = expireStatus
  loadActivationCodes()
}

const clearTableSelection = () => {
  selectionRows.value = []
  tableRef.value?.clearSelection?.()
}

const resetCreateForm = () => {
  Object.assign(createForm, {
    deviceLimit: 1,
    ownerName: '',
    expireAt: '',
    remark: '',
    enabled: true,
    quantity: 1,
  })
}

const handleGenerate = async () => {
  await createFormRef.value?.validate()
  isGenerating.value = true
  try {
    const result = await createActivationCodes({
      deviceLimit: createForm.deviceLimit,
      ownerName: normalizeText(createForm.ownerName),
      expireAt: createForm.expireAt || '',
      remark: normalizeText(createForm.remark),
      enabled: createForm.enabled,
      quantity: createForm.quantity,
    })
    generatedCodes.value = result.codes || (result.record ? [result.record] : [])
    resetCreateForm()
    await reloadAll()
    ElMessage.success(`已生成 ${generatedCodes.value.length || 1} 个激活码，完整码仅本次显示`)
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    isGenerating.value = false
  }
}

const fallbackCopyText = (text) => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  return copied
}

const copyText = async (text, message = '已复制') => {
  const content = normalizeText(text)
  if (!content) return

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(content)
    } else if (!fallbackCopyText(content)) {
      throw new Error('copy failed')
    }
    ElMessage.success(message)
  } catch {
    if (fallbackCopyText(content)) {
      ElMessage.success(message)
      return
    }
    ElMessage.warning('当前浏览器不允许自动复制，请手动复制')
  }
}

const copyActivationCode = (row) => {
  const code = row.code || row.maskedCode
  const onlyMasked = !row.code || row.code === row.maskedCode
  copyText(code || row.maskedCode, onlyMasked ? '已复制脱敏码，完整激活码仅生成时展示一次' : '已复制激活码')
}

const copySelectedMaskedCodes = () => {
  const text = selectionRows.value.map((row) => row.maskedCode || maskActivationCode(row)).join('\n')
  copyText(text, '已复制所选脱敏码列表')
}

const buildActivationSummary = (row) =>
  [
    row.maskedCode || maskActivationCode(row),
    getStatusText(row),
    `设备 ${Number(row.boundCount || 0)}/${Number(row.deviceLimit || 0)}`,
    `负责人 ${row.ownerName || row.owner || '-'}`,
    `到期 ${formatExpireAt(row.expireAt || row.expiresAt)}`,
  ].join(' | ')

const copyRowSummary = (row) => {
  copyText(buildActivationSummary(row), '已复制授权摘要')
}

const copySelectedSummary = () => {
  if (!selectionRows.value.length) {
    ElMessage.warning('请先勾选激活码')
    return
  }
  copyText(selectionRows.value.map(buildActivationSummary).join('\n'), '已复制所选授权摘要')
}

const exportRows = (rows = activationCodes.value, filename = 'activation-codes.csv') => {
  const header = ['激活码', '状态', '设备额度', '负责人', '到期时间', '创建时间', '最近校验', '备注']
  const body = rows.map((row) => [
    row.maskedCode || maskActivationCode(row),
    getStatusText(row),
    `${Number(row.boundCount || 0)}/${Number(row.deviceLimit || 0)}`,
    row.ownerName || row.owner || '',
    formatExpireAt(row.expireAt || row.expiresAt),
    formatDateTime(row.createdAt),
    formatDateTime(row.lastCheckAt),
    row.remark || '',
  ])
  const csv = [header, ...body].map((line) => line.map((item) => `"${String(item).replaceAll('"', '""')}"`).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const openDeviceDrawer = async (row) => {
  selectedCode.value = row
  deviceDrawerVisible.value = true
  deviceLoading.value = true
  try {
    deviceBindings.value = await listActivationCodeBindings(row.id || row.code)
  } catch (error) {
    ElMessage.error(error?.message || '绑定设备加载失败')
  } finally {
    deviceLoading.value = false
  }
}

const openRemoteBindDialog = (row) => {
  selectedCode.value = row
  Object.assign(remoteBindForm, {
    machineCode: '',
    macAddress: '',
    hostname: '',
    clientSource: 'tauri.localhost',
    remark: '',
  })
  remoteBindDialogVisible.value = true
}

const submitRemoteBind = async (force = false) => {
  await remoteBindFormRef.value?.validate()
  if (!selectedCode.value) return
  actionLoading.value = true
  try {
    const result = await remoteBindActivationDevice(selectedCode.value.id || selectedCode.value.code, {
      ...remoteBindForm,
      force,
    })

    if (result.conflict && !force) {
      await ElMessageBox.confirm(`该设备已绑定到其他激活码 ${result.activationCode || ''}，是否迁移到当前激活码？`, '绑定冲突确认', {
        confirmButtonText: '迁移绑定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await submitRemoteBind(true)
      return
    }

    remoteBindDialogVisible.value = false
    await reloadAll()
    if (deviceDrawerVisible.value && selectedCode.value) await openDeviceDrawer(selectedCode.value)
    ElMessage.success('远程绑定成功')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error?.message) ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

const openRemoteUnbindDialog = (binding) => {
  selectedBinding.value = binding
  remoteUnbindForm.reason = ''
  remoteUnbindDialogVisible.value = true
}

const submitRemoteUnbind = async () => {
  await remoteUnbindFormRef.value?.validate()
  if (!selectedCode.value || !selectedBinding.value) return
  actionLoading.value = true
  try {
    await remoteUnbindActivationDevice(selectedCode.value.id || selectedCode.value.code, selectedBinding.value.id || selectedBinding.value.machineCode, {
      reason: remoteUnbindForm.reason,
    })
    remoteUnbindDialogVisible.value = false
    await reloadAll()
    await openDeviceDrawer(selectedCode.value)
    ElMessage.success('远程解绑成功，客户端下次启动或心跳校验时将失效')
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

const toggleStatus = async (row) => {
  const nextEnabled = getStatusKey(row) === 'disabled'
  const title = nextEnabled ? '启用激活码' : '停用激活码'
  const reason = nextEnabled ? '管理员启用授权' : '管理员停用授权'

  try {
    if (!nextEnabled) {
      await ElMessageBox.confirm('停用后所有客户端下次校验都会返回 disabled，确认继续？', title, {
        confirmButtonText: '停用',
        cancelButtonText: '取消',
        type: 'warning',
      })
    }
    await updateActivationCodeStatus(row.id || row.code, { enabled: nextEnabled, reason })
    await reloadAll()
    ElMessage.success(nextEnabled ? '激活码已启用' : '激活码已停用')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error?.message) ElMessage.error(error.message)
  }
}

const openRemarkDialog = (row) => {
  selectedCode.value = row
  Object.assign(remarkForm, {
    ownerName: row.ownerName || row.owner || '',
    remark: row.remark || '',
    expireAt: row.expireAt || row.expiresAt || '',
  })
  remarkDialogVisible.value = true
}

const submitRemark = async () => {
  if (!selectedCode.value) return
  actionLoading.value = true
  try {
    await updateActivationCodeRemark(selectedCode.value.id || selectedCode.value.code, {
      ownerName: normalizeText(remarkForm.ownerName),
      remark: normalizeText(remarkForm.remark),
      expireAt: remarkForm.expireAt || '',
      reason: '管理员修改备注',
    })
    remarkDialogVisible.value = false
    await reloadAll()
    ElMessage.success('激活码信息已更新')
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

const deleteCode = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除激活码 ${row.maskedCode || maskActivationCode(row)}？该操作将软删除记录。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await batchOperateActivationCodes({
      ids: [row.id || row.code],
      action: 'DELETE',
      reason: '管理员删除激活码',
    })
    await reloadAll()
    ElMessage.success('激活码已删除')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error?.message) ElMessage.error(error.message)
  }
}

const runSimpleBatch = async (action) => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选激活码')
    return
  }

  const actionText = action === 'ENABLE' ? '启用' : action === 'DISABLE' ? '停用' : '删除'
  try {
    if (['DISABLE', 'DELETE'].includes(action)) {
      await ElMessageBox.confirm(`确认批量${actionText} ${selectedIds.value.length} 个激活码？`, `批量${actionText}`, {
        confirmButtonText: actionText,
        cancelButtonText: '取消',
        type: 'warning',
      })
    }
    await batchOperateActivationCodes({
      ids: selectedIds.value,
      action,
      reason: `批量${actionText}`,
    })
    selectionRows.value = []
    await reloadAll()
    ElMessage.success(`批量${actionText}成功`)
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    if (error?.message) ElMessage.error(error.message)
  }
}

const openBatchDialog = (action) => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先勾选激活码')
    return
  }
  batchAction.value = action
  batchDialogTitle.value = action === 'UPDATE_OWNER' ? '批量修改负责人' : '批量延长到期时间'
  Object.assign(batchForm, {
    ownerName: '',
    expireAt: '',
    reason: '',
  })
  batchDialogVisible.value = true
}

const submitBatchDialog = async () => {
  await batchFormRef.value?.validate()
  actionLoading.value = true
  try {
    await batchOperateActivationCodes({
      ids: selectedIds.value,
      action: batchAction.value,
      ownerName: batchForm.ownerName,
      expireAt: batchForm.expireAt,
      reason: batchForm.reason,
    })
    batchDialogVisible.value = false
    selectionRows.value = []
    await reloadAll()
    ElMessage.success('批量操作成功')
  } catch (error) {
    if (error?.message) ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

const heroActions = [
  { label: '刷新', type: 'default', command: 'refresh' },
  { label: '生成激活码', type: 'primary', command: 'generate' },
]

const handleHeroAction = (action) => {
  if (action.command === 'refresh') {
    reloadAll()
    return
  }
  if (action.command === 'generate') {
    handleGenerate()
  }
}

const handleBack = () => {
  router.push(AUTH_ROLE_DEFAULT_PATH.systemAdmin)
}

onMounted(reloadAll)
</script>

<template>
  <section class="activation-page">
    <PageHero
      :actions="heroActions"
      badge="激活管理"
      page-title="客户端授权与设备绑定管理中心"
      :page-icon="Key"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <section class="activation-overview" aria-label="授权态势总览">
      <article class="overview-status-card" :class="healthStatusType">
        <div class="overview-heading">
          <div>
            <span>授权态势</span>
            <strong>{{ healthStatusText }}</strong>
          </div>
          <el-tag class="status-badge" :type="healthStatusType" effect="light">实时</el-tag>
        </div>
        <div class="usage-panel">
          <div class="usage-number">
            <strong>{{ authorizationUsage.percent }}%</strong>
            <span>设备额度占用</span>
          </div>
          <span class="usage-track" aria-hidden="true">
            <i :style="{ width: `${authorizationUsage.percent}%` }" />
          </span>
        </div>
        <div class="overview-facts">
          <div>
            <span>总额度</span>
            <strong>{{ authorizationUsage.totalLimit }}</strong>
          </div>
          <div>
            <span>已绑定</span>
            <strong>{{ authorizationUsage.bound }}</strong>
          </div>
          <div>
            <span>可分配</span>
            <strong>{{ authorizationUsage.available }}</strong>
          </div>
        </div>
        <div class="status-shortcuts">
          <el-button
            v-for="item in quickStatusFilters"
            :key="item.label"
            size="small"
            @click="applyStatusShortcut(item.status, item.expireStatus)"
          >
            {{ item.label }} · {{ item.value }}
          </el-button>
        </div>
      </article>
    </section>

    <div class="activation-workbench activation-layout">
      <el-card class="activation-card generate-card is-primary" shadow="never">
        <template #header>
          <div class="section-title">
            <h2>生成激活码</h2>
            <span>完整激活码仅在生成成功后展示一次，列表默认显示脱敏码。</span>
          </div>
        </template>

        <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top" class="activation-form">
          <el-form-item label="可绑定设备数量" prop="deviceLimit" required>
            <el-input-number v-model="createForm.deviceLimit" class="full-width" :min="1" :max="10000" :step="1" />
          </el-form-item>
          <el-form-item label="生成数量" prop="quantity" required>
            <el-input-number v-model="createForm.quantity" class="full-width" :min="1" :max="100" :step="1" />
          </el-form-item>
          <el-form-item label="授权负责人">
            <el-input v-model="createForm.ownerName" clearable placeholder="例如 陈老师" />
          </el-form-item>
          <el-form-item label="到期时间">
            <el-date-picker
              v-model="createForm.expireAt"
              class="full-width"
              format="YYYY-MM-DD HH:mm"
              placeholder="长期有效可不填"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="生成后立即启用">
            <el-switch v-model="createForm.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
          <el-form-item label="授权用途 / 机房说明 / 备注" class="full-span">
            <el-input v-model="createForm.remark" :rows="3" placeholder="例如 软件部署实训室客户端授权" type="textarea" />
          </el-form-item>
        </el-form>

        <div class="form-actions">
          <el-button :icon="Refresh" @click="resetCreateForm">重置</el-button>
          <el-button type="primary" :icon="Key" :loading="isGenerating" @click="handleGenerate">生成激活码</el-button>
        </div>

        <div v-if="generatedCodes.length" class="generated-panel">
          <div class="generated-panel__head">
            <strong>本次生成结果</strong>
            <span>完整码仅本次显示</span>
          </div>
          <div v-for="item in generatedCodes" :key="item.id || item.code" class="generated-code">
            <code>{{ item.code || item.maskedCode }}</code>
            <el-button :icon="CopyDocument" text type="primary" @click="copyText(item.code || item.maskedCode, '已复制完整激活码')">
              复制
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="activation-card management-card is-expanded" shadow="never">
        <template #header>
          <div class="management-header">
            <div class="section-title">
              <h2>激活码管理</h2>
              <span>按授权码、设备指纹、MAC、主机名或负责人筛选，并执行设备绑定运维。</span>
            </div>
            <el-button :icon="Download" @click="exportRows()">导出授权</el-button>
          </div>
        </template>

        <div class="filter-bar">
          <el-input v-model="keyword" clearable placeholder="搜索激活码 / MAC / 设备指纹 / 主机名 / 负责人" @keyup.enter="loadActivationCodes">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="statusFilter" placeholder="全部状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="expireStatusFilter" placeholder="到期状态">
            <el-option v-for="item in expireStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadActivationCodes">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div class="batch-bar" :class="{ active: selectionRows.length }">
          <span>已选择 {{ selectionRows.length }} 个激活码</span>
          <div class="batch-actions">
            <el-button :disabled="!selectionRows.length" size="small" @click="runSimpleBatch('ENABLE')">批量启用</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="runSimpleBatch('DISABLE')">批量停用</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="openBatchDialog('UPDATE_OWNER')">批量修改负责人</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="openBatchDialog('EXTEND_EXPIRE_AT')">批量延长到期</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="copySelectedMaskedCodes">复制脱敏码列表</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="copySelectedSummary">复制授权摘要</el-button>
            <el-button :disabled="!selectionRows.length" size="small" @click="exportRows(selectionRows, 'selected-activation-codes.csv')">批量导出</el-button>
            <el-button v-if="selectionRows.length" size="small" text @click="clearTableSelection">清空选择</el-button>
            <el-button :disabled="!selectionRows.length" size="small" type="danger" plain @click="runSimpleBatch('DELETE')">批量删除</el-button>
          </div>
        </div>

        <div class="table-shell">
          <el-table
            ref="tableRef"
            v-loading="isLoading"
            :data="activationCodes"
            class="activation-table"
            row-key="id"
            @selection-change="selectionRows = $event"
          >
            <el-table-column type="selection" width="44" />
            <el-table-column label="激活码" min-width="190">
              <template #default="{ row }">
                <div class="code-cell">
                  <strong>{{ row.maskedCode || maskActivationCode(row) }}</strong>
                  <span>完整码仅生成时展示一次</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="104" align="center">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row)" effect="light">{{ getStatusText(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="设备额度" width="128">
              <template #default="{ row }">
                <div class="quota-cell">
                  <strong>{{ Number(row.boundCount || 0) }} / {{ Number(row.deviceLimit || 0) }}</strong>
                  <span class="quota-track" aria-hidden="true">
                    <i :style="{ width: `${getQuotaPercent(row)}%` }" />
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="负责人" min-width="110" show-overflow-tooltip>
              <template #default="{ row }">{{ row.ownerName || row.owner || '-' }}</template>
            </el-table-column>
            <el-table-column label="备注" min-width="160" prop="remark" show-overflow-tooltip />
            <el-table-column label="到期时间" min-width="150">
              <template #default="{ row }">{{ formatExpireAt(row.expireAt || row.expiresAt) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="最近校验" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.lastCheckAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="236" align="center">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button :icon="View" link type="primary" @click="openDeviceDrawer(row)">查看设备</el-button>
                  <el-button :icon="Link" link type="primary" @click="openRemoteBindDialog(row)">远程绑定</el-button>
                  <el-dropdown trigger="click">
                    <el-button link type="primary">
                      更多<el-icon class="el-icon--right"><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :icon="SwitchButton" @click="toggleStatus(row)">
                          {{ getStatusKey(row) === 'disabled' ? '启用' : '停用' }}
                        </el-dropdown-item>
                        <el-dropdown-item :icon="CopyDocument" @click="copyActivationCode(row)">复制激活码</el-dropdown-item>
                        <el-dropdown-item :icon="CopyDocument" @click="copyRowSummary(row)">复制摘要</el-dropdown-item>
                        <el-dropdown-item :icon="Edit" @click="openRemarkDialog(row)">编辑备注</el-dropdown-item>
                        <el-dropdown-item :icon="Delete" class="danger-item" @click="deleteCode(row)">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无激活码数据" />
            </template>
          </el-table>
        </div>
      </el-card>
    </div>

    <el-card class="activation-card audit-card is-quiet" shadow="never">
      <template #header>
        <div class="management-header">
          <div class="section-title">
            <h2>操作记录 / 审计日志</h2>
            <span>记录生成、启停、远程绑定、远程解绑、删除、批量操作等关键动作。</span>
          </div>
          <el-button :icon="Refresh" :loading="auditLoading" @click="loadAuditLogs">刷新日志</el-button>
        </div>
      </template>
      <el-table v-loading="auditLoading" :data="auditLogs" class="audit-table" empty-text="暂无审计日志">
        <el-table-column label="操作时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="110" prop="operatorName" />
        <el-table-column label="操作类型" min-width="120" prop="action" />
        <el-table-column label="激活码" min-width="150" prop="activationCode" />
        <el-table-column label="设备标识" min-width="160" prop="machineCode" show-overflow-tooltip />
        <el-table-column label="操作原因" min-width="180" prop="reason" show-overflow-tooltip />
        <el-table-column label="结果" width="90" prop="result" />
      </el-table>
    </el-card>

    <el-drawer v-model="deviceDrawerVisible" size="760px" class="activation-device-drawer" title="绑定设备详情">
      <template #default>
        <div v-if="selectedCode" class="drawer-summary">
          <strong>{{ selectedCode.maskedCode || maskActivationCode(selectedCode) }}</strong>
          <el-tag :type="getStatusType(selectedCode)" effect="light">{{ getStatusText(selectedCode) }}</el-tag>
          <span>设备额度 {{ Number(selectedCode.boundCount || 0) }} / {{ Number(selectedCode.deviceLimit || 0) }}</span>
        </div>
        <el-table v-loading="deviceLoading" :data="deviceBindings" class="device-table">
          <el-table-column label="设备标识" min-width="180" prop="machineCode" show-overflow-tooltip />
          <el-table-column label="MAC 地址" min-width="150" prop="macAddress" />
          <el-table-column label="主机名" min-width="130" prop="hostname" show-overflow-tooltip />
          <el-table-column label="客户端来源" min-width="130" prop="clientSource" show-overflow-tooltip />
          <el-table-column label="客户端版本" min-width="110" prop="clientVersion" />
          <el-table-column label="IP 地址" min-width="120" prop="ipAddress" />
          <el-table-column label="绑定时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.boundAt || row.activatedAt) }}</template>
          </el-table-column>
          <el-table-column label="最近在线" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.lastSeenAt) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="112">
            <template #default="{ row }">
              <el-tag :type="getBindingStatusType(row.status)" effect="light">{{ getBindingStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="116">
            <template #default="{ row }">
              <el-button
                :disabled="row.status !== 'ACTIVE'"
                link
                type="danger"
                @click="openRemoteUnbindDialog(row)"
              >
                远程解绑
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="该激活码暂无绑定设备" />
          </template>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="remoteBindDialogVisible" title="远程绑定设备" width="520px">
      <el-form ref="remoteBindFormRef" :model="remoteBindForm" :rules="remoteBindRules" label-position="top">
        <el-alert
          class="dialog-alert"
          :closable="false"
          title="请确认该设备标识来自可信客户端，若设备已绑定其他激活码，系统会提示迁移确认。"
          type="info"
          show-icon
        />
        <el-form-item label="设备标识 machineCode / deviceFingerprint" prop="machineCode" required>
          <el-input v-model="remoteBindForm.machineCode" clearable placeholder="请输入设备标识" />
        </el-form-item>
        <el-form-item label="MAC 地址">
          <el-input v-model="remoteBindForm.macAddress" clearable placeholder="xx:xx:xx:xx:xx:xx" />
        </el-form-item>
        <el-form-item label="主机名">
          <el-input v-model="remoteBindForm.hostname" clearable placeholder="LAB-PC-01" />
        </el-form-item>
        <el-form-item label="客户端来源">
          <el-input v-model="remoteBindForm.clientSource" clearable placeholder="tauri.localhost" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remoteBindForm.remark" :rows="2" type="textarea" placeholder="管理员预绑定说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remoteBindDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitRemoteBind(false)">确认绑定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remoteUnbindDialogVisible" title="远程解绑设备" width="520px">
      <div v-if="selectedBinding" class="unbind-device-info">
        <div><span>主机名</span><strong>{{ selectedBinding.hostname || '-' }}</strong></div>
        <div><span>MAC</span><strong>{{ selectedBinding.macAddress || '-' }}</strong></div>
        <div><span>设备指纹</span><strong>{{ selectedBinding.machineCode || '-' }}</strong></div>
        <div><span>激活码</span><strong>{{ selectedCode?.maskedCode || maskActivationCode(selectedCode) }}</strong></div>
      </div>
      <el-form ref="remoteUnbindFormRef" :model="remoteUnbindForm" :rules="remoteUnbindRules" label-position="top">
        <el-form-item label="解绑原因" prop="reason" required>
          <el-input
            v-model="remoteUnbindForm.reason"
            :rows="3"
            type="textarea"
            placeholder="例如：设备更换、误绑定、重装系统、授权回收"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remoteUnbindDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="submitRemoteUnbind">确认远程解绑</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkDialogVisible" title="编辑授权信息" width="520px">
      <el-form ref="remarkFormRef" :model="remarkForm" label-position="top">
        <el-form-item label="授权负责人">
          <el-input v-model="remarkForm.ownerName" clearable />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="remarkForm.expireAt"
            class="full-width"
            format="YYYY-MM-DD HH:mm"
            placeholder="长期有效可不填"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remarkForm.remark" :rows="3" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitRemark">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" :title="batchDialogTitle" width="520px">
      <el-form ref="batchFormRef" :model="batchForm" :rules="batchRules" label-position="top">
        <el-alert class="dialog-alert" :closable="false" :title="`已选择 ${selectionRows.length} 个激活码`" type="info" show-icon />
        <el-form-item v-if="batchAction === 'UPDATE_OWNER'" label="目标负责人" prop="ownerName" required>
          <el-input v-model="batchForm.ownerName" clearable placeholder="请输入新的负责人" />
        </el-form-item>
        <el-form-item v-if="batchAction === 'EXTEND_EXPIRE_AT'" label="新的到期时间" prop="expireAt" required>
          <el-date-picker
            v-model="batchForm.expireAt"
            class="full-width"
            format="YYYY-MM-DD HH:mm"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="操作原因" prop="reason" required>
          <el-input v-model="batchForm.reason" :rows="3" type="textarea" placeholder="请输入批量调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitBatchDialog">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.activation-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.activation-page :deep(.module-hero) {
  border-color: #dbe7f5;
}

.activation-page :deep(.module-hero .el-card__body) {
  padding: 18px 22px;
}

.activation-page :deep(.module-icon) {
  width: 52px;
  height: 52px;
  background: #eff6ff;
  font-size: 26px;
}

.activation-page :deep(.module-heading-copy h1) {
  font-size: 22px;
}

.activation-overview {
  display: block;
  min-width: 0;
}

.overview-status-card {
  display: grid;
  grid-template-columns: minmax(190px, 0.85fr) minmax(200px, 0.85fr) minmax(260px, 1fr) minmax(220px, 1.1fr);
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #f8fbff;
}

.overview-status-card.warning {
  border-color: #fde68a;
  background: #fffdf4;
}

.overview-status-card.danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.overview-heading,
.usage-number,
.overview-facts,
.status-shortcuts {
  display: flex;
  align-items: center;
}

.overview-heading {
  justify-content: space-between;
  gap: 12px;
}

.overview-heading div,
.usage-panel {
  min-width: 0;
}

.overview-heading span,
.overview-facts span {
  color: #64748b;
  font-size: 13px;
}

.overview-heading strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.status-badge {
  flex: 0 0 auto;
}

.usage-panel {
  display: grid;
  gap: 10px;
}

.usage-number {
  justify-content: space-between;
  gap: 12px;
}

.usage-number strong {
  color: var(--text-primary);
  font-size: 34px;
  line-height: 1;
}

.usage-number span {
  color: #475569;
  font-size: 13px;
}

.usage-track,
.quota-track {
  display: block;
  overflow: hidden;
  height: 7px;
  border-radius: 999px;
  background: #e2e8f0;
}

.usage-track i,
.quota-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
}

.overview-status-card.warning .usage-track i {
  background: #f59e0b;
}

.overview-status-card.danger .usage-track i {
  background: #ef4444;
}

.overview-facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.overview-facts div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.overview-facts strong {
  color: var(--text-primary);
  font-size: 18px;
}

.status-shortcuts {
  flex-wrap: wrap;
  gap: 8px;
}

.status-shortcuts :deep(.el-button + .el-button) {
  margin-left: 0;
}

.activation-card {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.activation-card :deep(.el-card__header) {
  padding: 16px 18px;
  border-bottom: 1px solid #e8eef7;
  background: #fbfdff;
}

.activation-card :deep(.el-card__body) {
  padding: 18px;
}

.activation-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.activation-workbench {
  min-width: 0;
}

.generate-card.is-primary {
  position: sticky;
  top: 84px;
}

.management-card.is-expanded {
  min-width: 0;
}

.audit-card.is-quiet {
  background: #ffffff;
}

.section-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.section-title span {
  display: block;
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.management-header,
.filter-bar,
.batch-bar,
.batch-actions,
.form-actions,
.generated-panel__head,
.generated-code,
.row-actions,
.drawer-summary {
  display: flex;
  align-items: center;
}

.management-header {
  justify-content: space-between;
  gap: 14px;
}

.activation-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.full-width,
.full-span {
  width: 100%;
}

.full-span {
  grid-column: 1 / -1;
}

.form-actions {
  justify-content: flex-end;
  gap: 10px;
  padding-top: 2px;
}

.generated-panel {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.generated-panel__head {
  justify-content: space-between;
  gap: 12px;
}

.generated-panel__head strong {
  color: #1d4ed8;
  font-size: 13px;
}

.generated-panel__head span,
.code-cell span {
  color: #64748b;
  font-size: 12px;
}

.generated-code {
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #ffffff;
}

.generated-code code,
.code-cell strong,
.drawer-summary strong {
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 13px;
  letter-spacing: 0;
}

.filter-bar {
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.filter-bar .el-input {
  flex: 1 1 320px;
  max-width: 480px;
}

.filter-bar .el-select {
  width: 144px;
}

.batch-bar {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.batch-bar.active {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.batch-bar > span {
  flex: 0 0 auto;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.batch-actions {
  flex: 1 1 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.batch-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.activation-table,
.audit-table,
.device-table {
  width: 100%;
}

.table-shell {
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.table-shell :deep(.el-table__header-wrapper th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.table-shell :deep(.el-table__fixed-right) {
  box-shadow: -8px 0 14px rgb(15 23 42 / 8%);
}

.code-cell {
  display: grid;
  gap: 4px;
}

.quota-cell {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.quota-cell strong {
  color: var(--text-primary);
  font-size: 13px;
}

.quota-track {
  width: 72px;
  height: 6px;
}

.row-actions {
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.danger-item {
  color: #ef4444;
}

.audit-card :deep(.el-card__body) {
  padding-top: 8px;
}

.audit-card :deep(.el-table__header-wrapper th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.drawer-summary {
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.drawer-summary span {
  color: #64748b;
  font-size: 13px;
}

.dialog-alert {
  margin-bottom: 14px;
}

.unbind-device-info {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.unbind-device-info div {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.unbind-device-info span {
  color: #64748b;
  font-size: 12px;
}

.unbind-device-info strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .activation-layout {
    grid-template-columns: 1fr;
  }

  .overview-status-card {
    grid-template-columns: 1fr;
  }

  .generate-card.is-primary {
    position: static;
  }
}

@media (max-width: 760px) {
  .activation-page :deep(.module-hero .el-card__body),
  .activation-card :deep(.el-card__body) {
    padding: 14px;
  }

  .overview-facts {
    grid-template-columns: 1fr;
  }

  .management-header,
  .batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .activation-form,
  .unbind-device-info {
    grid-template-columns: 1fr;
  }

  .filter-bar .el-input,
  .filter-bar .el-select {
    width: 100%;
    max-width: none;
  }

  .batch-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .overview-heading,
  .usage-number {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
