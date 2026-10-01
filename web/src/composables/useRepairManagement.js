import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { labResourceService, repairResourceService } from '../api/services/basicResources'
import { deviceAssetService } from '../api/services/deviceAssets'
import { exportTableToExcel } from '../utils/exportExcel'
import { createLabSelectOption, formatLabDisplayName } from '../utils/labDisplay'

const statusOptions = [
  { label: '待派单', type: 'danger' },
  { label: '处理中', type: 'warning' },
  { label: '待验收', type: 'primary' },
  { label: '已完成', type: 'success' },
  { label: '已关闭', type: 'info' },
]

const priorityOptions = ['高', '中', '低']
const faultTypeOptions = ['网络故障', '硬件故障', '软件异常', '环境告警', '耗材更换']
const assigneeOptions = ['陈工', '周工', '王工', '李工']
const fallbackLabOptions = ['A101 云计算实验室', 'B203 网络安全实验室', 'C305 人工智能实验室', 'D407 物联网实验室']
const fallbackLabSelectOptions = fallbackLabOptions.map((lab) => createLabSelectOption(lab))
const fallbackDeviceOptions = ['核心交换机', '学生机 A101-018', 'GPU 训练服务器', '温湿度传感器']

const initialRepairs = [
  {
    id: 'R20260423001',
    ticket: 'R20260423001',
    reporter: '张明',
    contact: '132****2210',
    lab: 'B203 网络安全实验室',
    device: '核心交换机',
    faultType: '网络故障',
    priority: '高',
    status: '处理中',
    tagType: 'warning',
    assignee: '陈工',
    submittedAt: '04-23 08:42',
    deadline: '今日 12:00',
    progress: 62,
    description: '机房部分终端无法访问实验平台，交换机端口告警。',
    result: '已完成链路排查，正在更换备用端口。',
    lastUpdate: '04-23 10:15',
  },
  {
    id: 'R20260423002',
    ticket: 'R20260423002',
    reporter: '李婷',
    contact: '159****6608',
    lab: 'A101 云计算实验室',
    device: '学生机 A101-018',
    faultType: '硬件故障',
    priority: '中',
    status: '待派单',
    tagType: 'danger',
    assignee: '待分配',
    submittedAt: '04-23 09:28',
    deadline: '今日 16:00',
    progress: 12,
    description: '主机无法开机，疑似电源模块异常。',
    result: '待维修人员接单。',
    lastUpdate: '04-23 09:28',
  },
  {
    id: 'R20260422012',
    ticket: 'R20260422012',
    reporter: '王磊',
    contact: '136****4921',
    lab: 'C305 人工智能实验室',
    device: 'GPU 训练服务器',
    faultType: '软件异常',
    priority: '高',
    status: '待验收',
    tagType: 'primary',
    assignee: '周工',
    submittedAt: '04-22 14:18',
    deadline: '04-23 18:00',
    progress: 88,
    description: '训练任务频繁中断，驱动日志报错。',
    result: '已重装驱动并完成压力测试，等待实验室管理员验收。',
    lastUpdate: '04-23 11:10',
  },
  {
    id: 'R20260421008',
    ticket: 'R20260421008',
    reporter: '赵老师',
    contact: '135****7008',
    lab: 'D407 物联网实验室',
    device: '温湿度传感器',
    faultType: '环境告警',
    priority: '低',
    status: '已完成',
    tagType: 'success',
    assignee: '李工',
    submittedAt: '04-21 16:05',
    deadline: '04-22 12:00',
    progress: 100,
    description: '传感器离线，环境数据缺失。',
    result: '更换电池并重新绑定网关，数据恢复正常。',
    lastUpdate: '04-22 10:30',
  },
]

const getStatusType = (status) => statusOptions.find((item) => item.label === status)?.type || 'info'

const createTicket = () => `R${new Date().toISOString().slice(0, 10).replaceAll('-', '')}${String(Math.floor(Math.random() * 900) + 100)}`

const nowLabel = () => {
  const now = new Date()
  return `${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

const normalizeLabText = (value) =>
  String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')

export function useRepairManagement() {
  const repairs = ref([])
  const selectedRepairId = ref('')
  const keyword = ref('')
  const statusFilter = ref('all')
  const priorityFilter = ref('all')
  const faultTypeFilter = ref('all')
  const assigneeFilter = ref('all')
  const urgentOnly = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const formSubmitting = ref(false)
  const editingId = ref('')
  const formModel = reactive({
    reporter: '',
    contact: '',
    lab: fallbackLabOptions[0],
    device: '',
    faultType: faultTypeOptions[0],
    priority: '中',
    deadline: '今日 18:00',
    description: '',
  })
  const labOptions = ref([...fallbackLabSelectOptions])
  const deviceOptions = ref([...fallbackDeviceOptions])

  const selectedRepair = computed(() => repairs.value.find((item) => item.id === selectedRepairId.value) || repairs.value[0] || null)
  const getCanonicalLabValue = (labName) => {
    if (!labName) return ''
    const target = normalizeLabText(labName)
    const matchedOption = labOptions.value.find((option) =>
      [
        option.value,
        option.labName,
        option.name,
        option.roomName,
        option.classroomName,
        option.code,
        option.labCode,
        option.room,
        option.roomNo,
      ]
        .map(normalizeLabText)
        .filter(Boolean)
        .includes(target),
    )

    return matchedOption?.value || formatLabDisplayName(labName)
  }
  const normalizeRepairRecord = (record = {}) => ({
    ...record,
    lab: getCanonicalLabValue(record.lab) || record.lab,
  })
  const normalizeRepairRecords = (records = []) => records.map(normalizeRepairRecord)

  const statusSummary = computed(() =>
    statusOptions.map((status) => ({
      ...status,
      count: repairs.value.filter((item) => item.status === status.label).length,
    })),
  )

  const metrics = computed(() => {
    const waitingCount = repairs.value.filter((item) => item.status === '待派单').length
    const processingCount = repairs.value.filter((item) => item.status === '处理中').length
    const highPriorityCount = repairs.value.filter((item) => item.priority === '高' && !['已完成', '已关闭'].includes(item.status)).length
    const completedCount = repairs.value.filter((item) => item.status === '已完成').length

    return [
      { label: '报修工单', value: String(repairs.value.length), trend: `高优先级 ${highPriorityCount} 单` },
      { label: '待派单', value: String(waitingCount), trend: waitingCount ? '需分配维修人' : '派单完成' },
      { label: '处理中', value: String(processingCount), trend: '跟踪维修进度' },
      { label: '已完成', value: String(completedCount), trend: '等待归档复盘' },
    ]
  })

  const filteredRepairs = computed(() => {
    const searchText = keyword.value.trim().toLowerCase()

    return repairs.value.filter((item) => {
      const matchesKeyword =
        !searchText ||
        [item.ticket, item.reporter, item.contact, item.lab, item.device, item.description, item.result, item.assignee].some((value) =>
          String(value ?? '').toLowerCase().includes(searchText),
        )
      const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
      const matchesPriority = priorityFilter.value === 'all' || item.priority === priorityFilter.value
      const matchesFaultType = faultTypeFilter.value === 'all' || item.faultType === faultTypeFilter.value
      const matchesAssignee = assigneeFilter.value === 'all' || item.assignee === assigneeFilter.value
      const matchesUrgent = !urgentOnly.value || (item.priority === '高' && !['已完成', '已关闭'].includes(item.status))

      return matchesKeyword && matchesStatus && matchesPriority && matchesFaultType && matchesAssignee && matchesUrgent
    })
  })

  const dialogTitle = computed(() => (dialogMode.value === 'create' ? '登记报修工单' : '编辑报修工单'))

  const persistRepairs = async () => {
    const persistedRecords = await repairResourceService.replace(repairs.value)
    repairs.value = normalizeRepairRecords(Array.isArray(persistedRecords) ? persistedRecords : initialRepairs.map((item) => ({ ...item })))
  }

  const loadRepairs = async () => {
    const loadedRecords = await repairResourceService.list({
      fallbackRecords: initialRepairs.map((item) => ({ ...item })),
    })
    repairs.value = normalizeRepairRecords(Array.isArray(loadedRecords) ? loadedRecords : initialRepairs.map((item) => ({ ...item })))
    selectedRepairId.value = repairs.value[0]?.id || ''
  }

  const loadReferenceOptions = async () => {
    const [loadedLabs, loadedDevices] = await Promise.allSettled([labResourceService.list(), deviceAssetService.list()])
    const nextLabOptions =
      loadedLabs.status === 'fulfilled'
        ? loadedLabs.value
            .map((lab) => ({
              ...createLabSelectOption(lab),
              value: formatLabDisplayName(lab),
            }))
            .filter((lab) => lab.label && lab.value)
        : []
    const nextDeviceOptions =
      loadedDevices.status === 'fulfilled'
        ? loadedDevices.value.map((device) => device.deviceName || device.name || device.assetNo || device.code || device.deviceCode).filter(Boolean)
        : []

    labOptions.value = nextLabOptions.length ? nextLabOptions : [...fallbackLabSelectOptions]
    deviceOptions.value = nextDeviceOptions.length ? [...new Set(nextDeviceOptions)] : [...fallbackDeviceOptions]
  }

  const selectRepair = (repair) => {
    selectedRepairId.value = repair?.id || ''
  }

  const assignFormModel = (repair = {}) => {
    formModel.reporter = repair.reporter || ''
    formModel.contact = repair.contact || ''
    formModel.lab = getCanonicalLabValue(repair.lab) || labOptions.value[0]?.value || fallbackLabOptions[0]
    formModel.device = repair.device || ''
    formModel.faultType = repair.faultType || faultTypeOptions[0]
    formModel.priority = repair.priority || '中'
    formModel.deadline = repair.deadline || '今日 18:00'
    formModel.description = repair.description || ''
  }

  const openCreateDialog = () => {
    dialogMode.value = 'create'
    editingId.value = ''
    assignFormModel()
    dialogVisible.value = true
  }

  const openEditDialog = (repair) => {
    dialogMode.value = 'edit'
    editingId.value = repair.id
    assignFormModel(repair)
    dialogVisible.value = true
  }

  const validateForm = () => {
    if (!formModel.reporter.trim()) {
      ElMessage.warning('请填写报修人')
      return false
    }

    if (!formModel.device.trim()) {
      ElMessage.warning('请填写故障设备')
      return false
    }

    if (!formModel.description.trim()) {
      ElMessage.warning('请填写故障描述')
      return false
    }

    return true
  }

  const submitRepair = async () => {
    if (!validateForm()) return

    formSubmitting.value = true
    const previousRepairs = [...repairs.value]
    const payload = {
      reporter: formModel.reporter.trim(),
      contact: formModel.contact.trim() || '未填写',
      lab: formModel.lab,
      device: formModel.device.trim(),
      faultType: formModel.faultType,
      priority: formModel.priority,
      deadline: formModel.deadline.trim() || '今日 18:00',
      description: formModel.description.trim(),
    }

    try {
      if (dialogMode.value === 'create') {
        const ticket = createTicket()
        const nextRepair = {
          id: ticket,
          ticket,
          ...payload,
          status: '待派单',
          tagType: 'danger',
          assignee: '待分配',
          submittedAt: nowLabel(),
          progress: 8,
          result: '待维修人员接单。',
          lastUpdate: nowLabel(),
        }
        repairs.value.unshift(nextRepair)
        selectedRepairId.value = nextRepair.id
      } else {
        repairs.value = repairs.value.map((item) =>
          item.id === editingId.value
            ? {
                ...item,
                ...payload,
                lastUpdate: nowLabel(),
              }
            : item,
        )
      }

      await persistRepairs()
      dialogVisible.value = false
      ElMessage.success(`${dialogMode.value === 'create' ? '登记' : '更新'}工单成功`)
    } catch (error) {
      repairs.value = previousRepairs
      ElMessage.error(error?.message || '保存工单失败')
    } finally {
      formSubmitting.value = false
    }
  }

  const patchRepair = (repair, patch, successMessage) => {
    if (!repair) return

    repairs.value = repairs.value.map((item) =>
      item.id === repair.id
        ? {
            ...item,
            ...patch,
            tagType: patch.status ? getStatusType(patch.status) : item.tagType,
            lastUpdate: nowLabel(),
          }
        : item,
    )
    selectedRepairId.value = repair.id
    persistRepairs()

    if (successMessage) {
      ElMessage.success(successMessage)
    }
  }

  const assignRepair = async (repair = selectedRepair.value) => {
    if (!repair) return

    try {
      const { value } = await ElMessageBox.prompt('请输入维修负责人', '派单处理', {
        confirmButtonText: '派单',
        cancelButtonText: '取消',
        inputValue: repair.assignee === '待分配' ? assigneeOptions[0] : repair.assignee,
      })
      patchRepair(
        repair,
        {
          assignee: value || assigneeOptions[0],
          status: '处理中',
          progress: Math.max(repair.progress, 35),
          result: '已派单，维修人员正在处理。',
        },
        '已派单给维修负责人',
      )
    } catch {
      // user cancelled
    }
  }

  const submitAcceptance = (repair = selectedRepair.value) => {
    patchRepair(
      repair,
      {
        status: '待验收',
        progress: Math.max(repair.progress, 86),
        result: '维修处理完成，等待实验室管理员验收。',
      },
      '已提交验收',
    )
  }

  const completeRepair = (repair = selectedRepair.value) => {
    patchRepair(
      repair,
      {
        status: '已完成',
        progress: 100,
        result: '管理员验收通过，故障已恢复。',
      },
      '工单已完成',
    )
  }

  const closeRepair = (repair = selectedRepair.value) => {
    patchRepair(
      repair,
      {
        status: '已关闭',
        progress: Math.max(repair.progress, 100),
        result: repair.result || '工单已关闭。',
      },
      '工单已关闭',
    )
  }

  const escalateRepair = (repair = selectedRepair.value) => {
    patchRepair(
      repair,
      {
        priority: '高',
        result: '已标记为高优先级，请优先处理。',
      },
      '已提升为高优先级',
    )
  }

  const deleteRepair = async (repair) => {
    try {
      await ElMessageBox.confirm(`确认删除工单“${repair.ticket}”吗？`, '删除工单', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    repairs.value = repairs.value.filter((item) => item.id !== repair.id)
    selectedRepairId.value = repairs.value[0]?.id || ''
    persistRepairs()
    ElMessage.success('已删除报修工单')
  }

  const resetRepairs = async () => {
    try {
      await ElMessageBox.confirm('确认恢复报修管理初始数据吗？', '恢复数据', {
        type: 'info',
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    keyword.value = ''
    statusFilter.value = 'all'
    priorityFilter.value = 'all'
    faultTypeFilter.value = 'all'
    assigneeFilter.value = 'all'
    urgentOnly.value = false
    repairResourceService
      .reset({
        fallbackRecords: initialRepairs.map((item) => ({ ...item })),
      })
      .then((resetRecords) => {
        repairs.value = normalizeRepairRecords(Array.isArray(resetRecords) ? resetRecords : initialRepairs.map((item) => ({ ...item })))
        selectedRepairId.value = repairs.value[0]?.id || ''
      })
    ElMessage.success('已恢复报修初始数据')
  }

  const exportRepairs = () => {
    const exportRows = filteredRepairs.value

    if (!exportRows.length) {
      ElMessage.warning('暂无可导出的报修工单')
      return
    }

    const exportedCount = exportTableToExcel({
      filename: '报修工单',
      sheetName: '报修工单',
      rows: exportRows,
      columns: [
        { label: '工单号', prop: 'ticket' },
        { label: '报修人', prop: 'reporter' },
        { label: '联系方式', prop: 'contact' },
        { label: '实验室', prop: 'lab' },
        { label: '故障设备', prop: 'device' },
        { label: '故障类型', prop: 'faultType' },
        { label: '优先级', prop: 'priority' },
        { label: '状态', prop: 'status' },
        { label: '维修人', prop: 'assignee' },
        { label: '提交时间', prop: 'submittedAt' },
        { label: '要求完成时间', prop: 'deadline' },
        { label: '处理进度', formatter: (item) => `${item.progress ?? 0}%` },
        { label: '故障描述', prop: 'description' },
        { label: '处理结果', prop: 'result' },
        { label: '最近更新', prop: 'lastUpdate' },
      ],
    })
    ElMessage.success(`已导出 ${exportedCount} 条工单`)
  }

  watch(filteredRepairs, async (records) => {
    if (!records.length) return
    if (!records.some((item) => item.id === selectedRepairId.value)) {
      await nextTick()
      selectedRepairId.value = records[0].id
    }
  })

  void loadReferenceOptions().then(loadRepairs)

  return {
    assigneeFilter,
    assigneeOptions,
    dialogTitle,
    dialogVisible,
    deviceOptions,
    faultTypeFilter,
    faultTypeOptions,
    filteredRepairs,
    formModel,
    formSubmitting,
    keyword,
    labOptions,
    metrics,
    priorityFilter,
    priorityOptions,
    repairs,
    selectedRepair,
    selectedRepairId,
    statusFilter,
    statusOptions,
    statusSummary,
    urgentOnly,
    assignRepair,
    closeRepair,
    completeRepair,
    deleteRepair,
    escalateRepair,
    exportRepairs,
    openCreateDialog,
    openEditDialog,
    resetRepairs,
    selectRepair,
    submitAcceptance,
    submitRepair,
  }
}
