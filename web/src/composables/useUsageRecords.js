/*
 * 使用记录的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useUsageRecords。
 */
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { labResourceService, usageRecordResourceService } from '../api/services/basicResources'
import { exportTableToExcel } from '../utils/exportExcel'
import { formatLabDisplayName } from '../utils/labDisplay'

const LOCAL_PATCH_STORAGE_KEY = 'smart-lab:usage-records:local-patches:v1'

const STATUS = {
  normal: '正常',
  using: '使用中',
  pending: '待复核',
  abnormal: '异常',
  archived: '已归档',
}

const statusOptions = [
  { label: STATUS.normal, type: 'success' },
  { label: STATUS.using, type: 'primary' },
  { label: STATUS.pending, type: 'warning' },
  { label: STATUS.abnormal, type: 'danger' },
  { label: STATUS.archived, type: 'info' },
]

const typeOptions = ['课程上机', '自主使用', '设备借用', '安全巡检', '门禁通行']
const fallbackLabOptions = ['A101 云计算实验室', 'B203 网络安全实验室', 'C305 人工智能实验室', 'D407 物联网实验室']

const initialRecords = [
  {
    id: 5,
    person: '李四',
    role: '使用对象',
    resource: '2-209 WEB后端开发实训室',
    lab: '2-206 移动互联实训室',
    type: '自主使用',
    entryTime: '05-15 08:30',
    leaveTime: '-',
    duration: '-',
    status: STATUS.normal,
    accessResult: '系统记录正常',
    reviewer: '系统',
    abnormalReason: '-',
    note: '由预约审批通过自动生成，预约编号13',
  },
  {
    id: 4,
    person: '创新创业团队',
    role: '团队',
    resource: 'D407 物联网实验室',
    lab: 'D407 物联网实验室',
    type: '竞赛',
    entryTime: '04-25 19:00',
    leaveTime: '-',
    duration: '-',
    status: STATUS.abnormal,
    accessResult: '非预约时段进入',
    reviewer: '系统预警',
    abnormalReason: '非预约时段刷卡',
    note: '需管理员复核门禁记录',
  },
  {
    id: 3,
    person: '网络工程 1 班',
    role: '课程班级',
    resource: 'B203 网络安全实验室',
    lab: 'B203 网络安全实验室',
    type: '课程上机',
    entryTime: '04-25 10:00',
    leaveTime: '-',
    duration: '-',
    status: STATUS.normal,
    accessResult: '课程预约匹配',
    reviewer: '系统',
    abnormalReason: '-',
    note: '课程使用记录完整',
  },
  {
    id: 2,
    person: '人工智能协会',
    role: '社团',
    resource: 'C305 人工智能实验室',
    lab: 'C305 人工智能实验室',
    type: '自主使用',
    entryTime: '04-26 14:00',
    leaveTime: '-',
    duration: '-',
    status: STATUS.pending,
    accessResult: '待管理员复核',
    reviewer: '待复核',
    abnormalReason: '使用结束材料未上传',
    note: '需补充活动记录',
  },
  {
    id: 1,
    person: '软件工程 2 班',
    role: '课程班级',
    resource: 'A101 云计算实验室',
    lab: 'A101 云计算实验室',
    type: '课程上机',
    entryTime: '04-26 08:00',
    leaveTime: '-',
    duration: '-',
    status: STATUS.normal,
    accessResult: '课程预约匹配',
    reviewer: '系统',
    abnormalReason: '-',
    note: '课程使用记录完整',
  },
]

/**
 * 读取本地Patches。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readLocalPatches = () => {
  if (typeof window === 'undefined') return {}

  try {
    const patches = JSON.parse(window.localStorage.getItem(LOCAL_PATCH_STORAGE_KEY) || '{}')
    return patches && typeof patches === 'object' && !Array.isArray(patches) ? patches : {}
  } catch {
    return {}
  }
}

/**
 * 写入本地Patches。
 * 入参：patches（patches）。
 */
const writeLocalPatches = (patches) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(LOCAL_PATCH_STORAGE_KEY, JSON.stringify(patches))
}

/**
 * pad数值的处理逻辑。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const padNumber = (value) => String(value).padStart(2, '0')

/**
 * now标签的处理逻辑。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const nowLabel = () => {
  const now = new Date()
  return `${padNumber(now.getMonth() + 1)}-${padNumber(now.getDate())} ${padNumber(now.getHours())}:${padNumber(now.getMinutes())}`
}

/**
 * 转换为Display时间。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const toDisplayTime = (value) => {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  const match = text.match(/(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/)
  if (match) return `${match[2]}-${match[3]} ${match[4]}:${match[5]}`
  return text
}

/**
 * 获取记录日期。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRecordDate = (value) => {
  if (!value || value === '-' || value === STATUS.using) return ''
  const text = String(value).replace(/\//g, '-')
  const fullMatch = text.match(/(\d{4})-(\d{2})-(\d{2})/)
  if (fullMatch) return `${fullMatch[1]}-${fullMatch[2]}-${fullMatch[3]}`
  const shortMatch = text.match(/(\d{2})-(\d{2})/)
  return shortMatch ? `${new Date().getFullYear()}-${shortMatch[1]}-${shortMatch[2]}` : ''
}

/**
 * 解析记录时间。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const parseRecordTime = (value) => {
  const date = getRecordDate(value)
  if (!date) return null
  const timeMatch = String(value).match(/(\d{2}):(\d{2})/)
  const time = timeMatch ? `${timeMatch[1]}:${timeMatch[2]}:00` : '00:00:00'
  const parsed = new Date(`${date}T${time}`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

/**
 * 格式化时长。
 * 入参：entryTime（条目时间）；leaveTime（移出时间）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatDuration = (entryTime, leaveTime) => {
  const start = parseRecordTime(entryTime)
  const end = parseRecordTime(leaveTime)
  if (!start || !end || end.getTime() < start.getTime()) return '已结束'

  const totalMinutes = Math.max(Math.round((end.getTime() - start.getTime()) / 60000), 1)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (!hours) return `${minutes}分钟`
  if (!minutes) return `${hours}小时`
  return `${hours}小时${minutes}分钟`
}

/**
 * 归一化状态。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeStatus = (status) => {
  const text = String(status || '').trim()
  if (Object.values(STATUS).includes(text)) return text
  if (text.includes('归档')) return STATUS.archived
  if (text.includes('异常')) return STATUS.abnormal
  if (text.includes('复核')) return STATUS.pending
  if (text.includes('使用')) return STATUS.using
  return STATUS.normal
}

/**
 * 获取状态类型。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getStatusType = (status) => statusOptions.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.label === status)?.type || 'info'
/**
 * 判断是否为Abnormal状态。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const isAbnormalStatus = (status) => [STATUS.abnormal, STATUS.pending].includes(status)
/**
 * 判断是否为PersistableBackend标识。
 * 入参：id（标识）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const isPersistableBackendId = (id) => /^\d+$/.test(String(id ?? ''))
/**
 * 判断是否为Readonly记录。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const isReadonlyRecord = (record = {}) => Boolean(record.readonly) || !isPersistableBackendId(record.id)

/**
 * infer实验室的处理逻辑。
 * 入参：record（记录）；availableLabs（可用实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const inferLab = (record, availableLabs = fallbackLabOptions) => {
  if (record.lab) return record.lab
  const resource = String(record.resource || '')
  return availableLabs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => resource.includes(lab.slice(0, 4)) || resource.includes(lab)) || resource || '-'
}

/**
 * 格式化Usage记录No。
 * 入参：record（记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatUsageRecordNo = (record = {}) => {
  const existingNo = record.recordNo || record.recordCode || record.usageNo || record.usageCode
  if (existingNo) return String(existingNo)

  const rawId = String(record.id ?? '').trim()
  if (/^UR-/i.test(rawId)) return rawId
  if (/^\d+$/.test(rawId)) return `UR-${rawId.padStart(4, '0')}`
  return rawId || `UR-${Date.now()}`
}

/**
 * 归一化Usage记录。
 * 入参：record（记录）；availableLabs（可用实验室）。
 * @returns 组装的键值对象。
 */
const normalizeUsageRecord = (record = {}, availableLabs = fallbackLabOptions) => {
  const id = String(record.id || `UR-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`)
  const status = normalizeStatus(record.status)
  const entryTime = record.entryTime || toDisplayTime(record.time || record.useTime)
  const isUsing = status === STATUS.using
  const isAbnormal = isAbnormalStatus(status)
  const leaveTime = record.leaveTime || (isUsing ? STATUS.using : '-')

  return {
    ...record,
    id,
    displayId: formatUsageRecordNo({ ...record, id }),
    person: record.person || '-',
    role: record.role || '使用对象',
    resource: record.resource || '-',
    lab: inferLab(record, availableLabs),
    type: record.type || record.scene || '使用记录',
    entryTime,
    leaveTime,
    duration: record.duration || (isUsing ? '进行中' : '-'),
    status,
    tagType: getStatusType(status),
    accessResult: record.accessResult || (isAbnormal ? '待管理员处理' : '系统记录正常'),
    reviewer: record.reviewer || (isAbnormal ? '待复核' : '系统'),
    abnormalReason: record.abnormalReason || (isAbnormal ? record.remark || '待补充异常原因' : '-'),
    note: record.note || record.remark || '暂无备注',
    sourceType: record.sourceType || '台账',
    readonly: isReadonlyRecord({ ...record, id }),
  }
}

/**
 * 应用本地Patches。
 * 入参：loadedRecords（loaded记录集合）；availableLabs（可用实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const applyLocalPatches = (loadedRecords, availableLabs) => {
  const patches = readLocalPatches()
  return loadedRecords.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => {
    const id = String(record.id)
    return normalizeUsageRecord({ ...record, ...(patches[id] || {}) }, availableLabs)
  })
}

/**
 * 提供Usage记录集合。
 * @returns 组装的键值对象。
 */
export function useUsageRecords() {
  const records = ref([])
  const labOptions = ref([...fallbackLabOptions])
  const selectedRecordId = ref('')
  const keyword = ref('')
  const statusFilter = ref('all')
  const typeFilter = ref('all')
  const labFilter = ref('all')
  const dateRange = ref([])
  const abnormalOnly = ref(false)

  const selectedRecord = computed(
    /**
     * 计算派生状态。
     */
    () => records.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => String(item.id) === String(selectedRecordId.value)) || records.value[0] || null,
  )

  const effectiveTypeOptions = computed(/** 计算派生状态。 */ () => {
    const dynamicTypes = records.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => item.type).filter(Boolean)
    return [...new Set([...typeOptions, ...dynamicTypes])]
  })

  const effectiveLabOptions = computed(/** 计算派生状态。 */ () => {
    const dynamicLabs = records.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => item.lab).filter(Boolean)
    return [...new Set([...labOptions.value, ...dynamicLabs])]
  })

  const statusSummary = computed(/** 计算派生状态。 */ () =>
    statusOptions.map(/** 将当前条目转换为目标结果。 入参：status。 */ (status) => ({
      ...status,
      count: records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === status.label).length,
    })),
  )

  const metrics = computed(/** 计算派生状态。 */ () => {
    const usingCount = records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === STATUS.using).length
    const abnormalCount = records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => isAbnormalStatus(item.status)).length
    const archivedCount = records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === STATUS.archived).length

    return [
      { label: '记录总数', value: String(records.value.length), trend: '门禁与设备联动采集' },
      { label: '使用中', value: String(usingCount), trend: usingCount ? '需要关注离场' : '暂无占用' },
      { label: '待复核/异常', value: String(abnormalCount), trend: abnormalCount ? '请管理员处理' : '运行正常' },
      { label: '已归档', value: String(archivedCount), trend: '可导出审计' },
    ]
  })

  const filteredRecords = computed(/** 计算派生状态。 */ () => {
    const searchText = keyword.value.trim().toLowerCase()
    const [startDate, endDate] = dateRange.value || []

    return records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => {
      const matchesKeyword =
        !searchText ||
        [
          item.id,
          item.displayId,
          item.person,
          item.role,
          item.resource,
          item.lab,
          item.type,
          item.accessResult,
          item.abnormalReason,
          item.note,
          item.sourceType,
        ].some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) => String(value ?? '').toLowerCase().includes(searchText))
      const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
      const matchesType = typeFilter.value === 'all' || item.type === typeFilter.value
      const matchesLab = labFilter.value === 'all' || item.lab === labFilter.value
      const matchesAbnormal = !abnormalOnly.value || isAbnormalStatus(item.status)
      const recordDate = getRecordDate(item.entryTime)
      const matchesDate = (!startDate || recordDate >= startDate) && (!endDate || recordDate <= endDate)

      return matchesKeyword && matchesStatus && matchesType && matchesLab && matchesAbnormal && matchesDate
    })
  })

  /**
   * 保存本地Patch。
   * 入参：record（记录）。
   */
  const saveLocalPatch = (record) => {
    const patches = readLocalPatches()
    patches[String(record.id)] = {
      ...record,
      tagType: getStatusType(record.status),
    }
    writeLocalPatches(patches)
  }

  /**
   * 同步服务端状态。
   * 入参：record（记录）；actionName（action名称）；payload（载荷）。
   */
  const syncServerStatus = async (record, actionName, payload = {}) => {
    if (isReadonlyRecord(record)) return
    try {
      await usageRecordResourceService.action(record.id, actionName, payload)
    } catch {
      ElMessage.warning('页面状态已更新，后端状态同步稍后可重试')
    }
  }

  /**
   * patch记录的处理逻辑。
   * 入参：record（记录）；patch（patch）；successMessage（成功消息）；actionName（action名称）。
   */
  const patchRecord = async (record, patch, successMessage, actionName = '') => {
    if (!record) return
    if (isReadonlyRecord(record)) {
      ElMessage.warning('课表和预约来源记录为只读展示，请在对应业务模块处理')
      return
    }
    const updatedRecord = normalizeUsageRecord(
      {
        ...record,
        ...patch,
        tagType: patch.status ? getStatusType(patch.status) : record.tagType,
      },
      labOptions.value,
    )

    records.value = records.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => (String(item.id) === String(record.id) ? updatedRecord : item))
    selectedRecordId.value = updatedRecord.id
    saveLocalPatch(updatedRecord)

    if (actionName) {
      void syncServerStatus(updatedRecord, actionName, {
        status: updatedRecord.status,
        remark: updatedRecord.abnormalReason === '-' ? updatedRecord.note : updatedRecord.abnormalReason,
      })
    }

    if (successMessage) ElMessage.success(successMessage)
  }

  /**
   * 标记Reviewed。
   * 入参：record（记录）。
   */
  const markReviewed = (record = selectedRecord.value) => {
    patchRecord(
      record,
      {
        status: STATUS.normal,
        reviewer: '机房管理员',
        abnormalReason: '-',
        accessResult: '复核通过',
        note: `已于 ${nowLabel()} 完成复核`,
      },
      '已复核为正常记录',
      'review',
    )
  }

  /**
   * 标记Abnormal。
   * 入参：record（记录）。
   */
  const markAbnormal = async (record = selectedRecord.value) => {
    if (!record) return

    try {
      const { value } = await ElMessageBox.prompt('请填写异常原因', '标记异常', {
        confirmButtonText: '标记异常',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：非预约时段进入、设备未归还、门禁识别异常',
        /**
         * 输入校验器的处理逻辑。
         * 入参：value（值）。
         * @returns 由当前表达式或分支生成的处理结果。
         */
        inputValidator: (value) => Boolean(String(value || '').trim()) || '异常原因不能为空',
      })
      patchRecord(
        record,
        {
          status: STATUS.abnormal,
          reviewer: '机房管理员',
          abnormalReason: String(value || '').trim(),
          accessResult: '管理员标记异常',
          note: `异常已于 ${nowLabel()} 记录`,
        },
        '已标记为异常记录',
        'abnormal',
      )
    } catch {
      // user cancelled
    }
  }

  /**
   * 归档记录的处理逻辑。
   * 入参：record（记录）。
   */
  const archiveRecord = async (record = selectedRecord.value) => {
    if (!record) return

    try {
      await ElMessageBox.confirm(`确认归档“${record.displayId}”？归档后仍可筛选和导出。`, '归档记录', {
        type: 'info',
        confirmButtonText: '归档',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    patchRecord(
      record,
      {
        status: STATUS.archived,
        reviewer: '机房管理员',
        accessResult: '记录已归档',
        note: `已于 ${nowLabel()} 归档`,
      },
      '使用记录已归档',
      'archive',
    )
  }

  /**
   * 关闭Using记录。
   * 入参：record（记录）。
   */
  const closeUsingRecord = (record = selectedRecord.value) => {
    if (!record) return
    const leaveTime = nowLabel()

    patchRecord(
      record,
      {
        status: STATUS.normal,
        leaveTime,
        duration: formatDuration(record.entryTime, leaveTime),
        reviewer: '机房管理员',
        accessResult: '管理员确认资源已释放',
        abnormalReason: '-',
        note: `已于 ${leaveTime} 结束使用`,
      },
      '已结束使用并写入离开时间',
      'review',
    )
  }

  /**
   * 筛选Abnormal记录集合。
   */
  const filterAbnormalRecords = () => {
    abnormalOnly.value = true
    statusFilter.value = 'all'
    ElMessage.warning('已筛选待复核与异常记录')
  }

  /**
   * 重置筛选条件。
   */
  const resetFilters = () => {
    keyword.value = ''
    statusFilter.value = 'all'
    typeFilter.value = 'all'
    labFilter.value = 'all'
    dateRange.value = []
    abnormalOnly.value = false
  }

  /**
   * 重置记录集合。
   */
  const resetRecords = async () => {
    try {
      await ElMessageBox.confirm('确认恢复使用记录初始数据吗？', '恢复数据', {
        type: 'info',
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    resetFilters()
    writeLocalPatches({})
    const resetResult = await usageRecordResourceService.reset({
      fallbackRecords: initialRecords.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => ({ ...item })),
    })
    records.value = applyLocalPatches(Array.isArray(resetResult) && resetResult.length ? resetResult : initialRecords, labOptions.value)
    selectedRecordId.value = records.value[0]?.id || ''
    ElMessage.success('已恢复使用记录初始数据')
  }

  /**
   * 导出记录集合。
   */
  const exportRecords = () => {
    const exportRows = filteredRecords.value

    if (!exportRows.length) {
      ElMessage.warning('暂无可导出的使用记录')
      return
    }

    const exportedCount = exportTableToExcel({
      filename: '使用记录',
      sheetName: '使用记录',
      rows: exportRows,
      columns: [
        { label: '记录编号', prop: 'displayId' },
        { label: '人员/班级', prop: 'person' },
        { label: '角色', prop: 'role' },
        { label: '资源', prop: 'resource' },
        { label: '实验室', prop: 'lab' },
        { label: '类型', prop: 'type' },
        { label: '进入时间', prop: 'entryTime' },
        { label: '离开时间', prop: 'leaveTime' },
        { label: '使用时长', prop: 'duration' },
        { label: '状态', prop: 'status' },
        { label: '通行/审批结果', prop: 'accessResult' },
        { label: '复核人', prop: 'reviewer' },
        { label: '异常原因', prop: 'abnormalReason' },
        { label: '备注', prop: 'note' },
      ],
    })
    ElMessage.success(`已导出 ${exportedCount} 条使用记录`)
  }

  /**
   * 加载实验室选项。
   */
  const loadLabOptions = async () => {
    try {
      const labs = await labResourceService.list({ fallbackRecords: [] })
      const displayNames = (Array.isArray(labs) ? labs : []).map(/** 将当前条目转换为目标结果。 入参：lab。 */ (lab) => formatLabDisplayName(lab)).filter(Boolean)
      labOptions.value = displayNames.length ? displayNames : [...fallbackLabOptions]
    } catch {
      labOptions.value = [...fallbackLabOptions]
    }
  }

  /**
   * 加载记录集合。
   */
  const loadRecords = async () => {
    const loadedRecords = await usageRecordResourceService.list({
      fallbackRecords: initialRecords.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => ({ ...item })),
    })
    records.value = applyLocalPatches(Array.isArray(loadedRecords) && loadedRecords.length ? loadedRecords : initialRecords, labOptions.value)
    selectedRecordId.value = records.value[0]?.id || ''
  }

  /**
   * 选择记录。
   * 入参：record（记录）。
   */
  const selectRecord = (record) => {
    selectedRecordId.value = record?.id || ''
  }

  watch(filteredRecords, /** 响应被监听状态的变化。 入参：nextRecords。 */ async (nextRecords) => {
    if (!nextRecords.length) return
    if (!nextRecords.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ (item) => String(item.id) === String(selectedRecordId.value))) {
      await nextTick()
      selectedRecordId.value = nextRecords[0].id
    }
  })

  void Promise.all([loadLabOptions()]).then(loadRecords)

  return {
    abnormalOnly,
    dateRange,
    filteredRecords,
    keyword,
    labFilter,
    labOptions: effectiveLabOptions,
    metrics,
    records,
    selectedRecord,
    selectedRecordId,
    statusFilter,
    statusOptions,
    statusSummary,
    typeFilter,
    typeOptions: effectiveTypeOptions,
    archiveRecord,
    closeUsingRecord,
    exportRecords,
    filterAbnormalRecords,
    markAbnormal,
    markReviewed,
    resetFilters,
    resetRecords,
    selectRecord,
  }
}
