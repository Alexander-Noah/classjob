/*
 * 业务记录的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useBusinessModuleRecords。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBusinessModuleEndpoints } from '../api/endpoints'
import { createCollectionService } from '../api/services/collectionService'
import { exportTableToExcel } from '../utils/exportExcel'

/**
 * 提供BusinessModule记录集合。
 * @returns 组装的键值对象。
 */
export function useBusinessModuleRecords({ route, moduleConfig, pageTitle }) {
  const keyword = ref('')
  const statusFilter = ref('all')
  const recordsState = ref([])
  const selectedRows = ref([])
  const detailVisible = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const activeRecord = ref(null)
  const formSubmitting = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const formModel = reactive({})

  const config = computed(/** 计算派生状态。 */ () => moduleConfig.value || {})
  const actions = computed(/** 计算派生状态。 */ () => config.value.actions || [])
  const tableColumns = computed(/** 计算派生状态。 */ () => config.value.tableColumns || [])
  const initialRecords = computed(/** 计算派生状态。 */ () => config.value.records || [])
  const recordName = computed(/** 计算派生状态。 */ () => config.value.recordName || '记录')
  const storageKey = computed(/** 计算派生状态。 */ () => config.value.storageKey || `smart-lab:${route.name}:records`)
  const statusColumn = computed(
    /**
     * 计算派生状态。
     */
    () => tableColumns.value.find(/** 判断当前条目是否为查找目标。 入参：column。 */ (column) => column.type === 'tag') || tableColumns.value.find(/** 判断当前条目是否为查找目标。 入参：column。 */ (column) => column.prop === 'status'),
  )
  const statusProp = computed(/** 计算派生状态。 */ () => statusColumn.value?.prop || 'status')
  const editableColumns = computed(/** 计算派生状态。 */ () => tableColumns.value.filter(/** 判断当前条目是否符合筛选条件。 入参：column。 */ (column) => !column.readonly))
  /**
   * 获取FormProp。
   * 入参：column（column）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const getFormProp = (column) => column.formProp || column.modelProp || column.prop
  const selectLikeProps = ['status', 'type', 'category', 'priority', 'role', 'owner', 'assignee', 'manager', 'lab', 'scope']
  const primaryKey = computed(/** 计算派生状态。 */ () => config.value.primaryKey || tableColumns.value[0]?.prop || 'name')
  const configuredStatusOptions = computed(/** 计算派生状态。 */ () => config.value.statusOptions || [])
  const singleRecordMode = computed(/** 计算派生状态。 */ () => config.value.persistMode === 'single')
  const collectionService = computed(/** 计算派生状态。 */ () =>
    createCollectionService({
      storageKey: storageKey.value,
      resourceName: route.name || 'module-records',
      endpoints: config.value.apiEndpoints || getBusinessModuleEndpoints(route.name),
    }),
  )

  /**
   * 创建记录标识。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const createRecordId = () => `${route.name || 'module'}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

  /**
   * 归一化实验室Display名称。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const normalizeLabDisplayName = (value) => {
    const text = String(value ?? '').trim()
    return text.replace(/^([A-Za-z]+\d+)\s+\1\s+/, '$1 ')
  }

  /**
   * 归一化记录Fields。
   * 入参：record（记录）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const normalizeRecordFields = (record) => {
    if (!record || typeof record !== 'object') return record

    return {
      ...record,
      ...(record.lab ? { lab: normalizeLabDisplayName(record.lab) } : {}),
    }
  }

  /**
   * stripInternalFields的处理逻辑。
   * 入参：record（记录）。
   * @returns restRecord 的当前值。
   */
  const stripInternalFields = (record) => {
    const { _recordId, ...restRecord } = record
    return restRecord
  }

  /**
   * 归一化记录集合。
   * 入参：records（记录集合）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const normalizeRecords = (records) =>
    records.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => ({
      ...normalizeRecordFields(record),
      _recordId: record._recordId || createRecordId(),
    }))

  /**
   * 持久化记录集合。
   */
  const persistRecords = async () => {
    const nextRecords = recordsState.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => stripInternalFields(record))
    const persistedRecords = await collectionService.value.replace(nextRecords)
    recordsState.value = normalizeRecords(persistedRecords)
  }

  /**
   * 重置Selection。
   */
  const resetSelection = () => {
    selectedRows.value = []
  }

  /**
   * 加载记录集合。
   */
  const loadRecords = async () => {
    const loadedRecords = await collectionService.value.list({
      fallbackRecords: initialRecords.value,
    })

    recordsState.value = normalizeRecords(loadedRecords)
    resetSelection()
  }

  const statusOptions = computed(/** 计算派生状态。 */ () => {
    const optionMap = new Map()

    configuredStatusOptions.value.forEach(/** 处理遍历到的当前条目。 入参：option。 */ (option) => {
      optionMap.set(option.label, option)
    })

    recordsState.value.forEach(/** 处理遍历到的当前条目。 入参：record。 */ (record) => {
      const statusValue = record[statusProp.value]

      if (statusValue && !optionMap.has(statusValue)) {
        optionMap.set(statusValue, {
          label: statusValue,
          type: record.tagType || 'info',
        })
      }
    })

    return [...optionMap.values()]
  })

  const formFieldOptions = computed(/** 计算派生状态。 */ () => {
    const optionMap = {}

    editableColumns.value.forEach(/** 处理遍历到的当前条目。 入参：column。 */ (column) => {
      if (column.prop === statusProp.value) {
        optionMap[column.prop] = statusOptions.value.map(/** 将当前条目转换为目标结果。 入参：option。 */ (option) => option.label)
        return
      }

      if (Array.isArray(column.options) && column.options.length) {
        optionMap[column.prop] = column.options.map(/** 将当前条目转换为目标结果。 入参：option。 */ (option) => option.label || option.value || option)
        return
      }

      const propName = String(column.prop || '').toLowerCase()
      const shouldSuggestOptions = selectLikeProps.some(/** 判断当前条目是否满足任一匹配条件。 入参：keyword。 */ (keyword) => propName.includes(keyword))
      if (!shouldSuggestOptions) return

      const values = [
        ...new Set(
          recordsState.value
            .map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => record[column.prop])
            .filter(/** 判断当前条目是否符合筛选条件。 入参：value。 */ (value) => value !== undefined && value !== null && String(value).trim()),
        ),
      ]

      if (values.length > 1 && values.length <= 12) {
        optionMap[column.prop] = values
      }
    })

    return optionMap
  })

  const totalRecordCount = computed(/** 计算派生状态。 */ () => recordsState.value.length)
  const statusSummary = computed(/** 计算派生状态。 */ () =>
    statusOptions.value.map(/** 将当前条目转换为目标结果。 入参：option。 */ (option) => {
      const count = recordsState.value.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => record[statusProp.value] === option.label).length
      const percentage = totalRecordCount.value ? Math.round((count / totalRecordCount.value) * 100) : 0

      return {
        ...option,
        count,
        percentage,
      }
    }),
  )

  const filteredRecords = computed(/** 计算派生状态。 */ () => {
    const searchText = keyword.value.trim().toLowerCase()

    return recordsState.value.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => {
      const matchesStatus = statusFilter.value === 'all' || record[statusProp.value] === statusFilter.value
      const matchesKeyword =
        !searchText ||
        Object.entries(record).some(/** 判断当前条目是否满足任一匹配条件。 */ ([key, value]) => {
          if (key.startsWith('_')) return false
          return String(value ?? '').toLowerCase().includes(searchText)
        })

      return matchesStatus && matchesKeyword
    })
  })

  const filteredRecordCount = computed(/** 计算派生状态。 */ () => filteredRecords.value.length)
  const pagedRecords = computed(/** 计算派生状态。 */ () => {
    const startIndex = (currentPage.value - 1) * pageSize.value
    return filteredRecords.value.slice(startIndex, startIndex + pageSize.value)
  })

  /**
   * 处理当前页码Change。
   * 入参：page（页码）。
   */
  const handleCurrentPageChange = (page) => {
    currentPage.value = page
  }

  /**
   * 处理页码大小Change。
   * 入参：size（大小）。
   */
  const handlePageSizeChange = (size) => {
    pageSize.value = size
    currentPage.value = 1
  }

  const batchTargetStatus = computed(/** 计算派生状态。 */ () => {
    const batchAction = actions.value.find(/** 判断当前条目是否为查找目标。 入参：action。 */ (action) => action.command === 'batchStatus')
    return batchAction?.targetStatus || statusOptions.value[1]?.label || statusOptions.value[0]?.label || ''
  })

  const dialogTitle = computed(/** 计算派生状态。 */ () => (dialogMode.value === 'create' ? `新增${recordName.value}` : `编辑${recordName.value}`))

  /**
   * 获取状态选项。
   * 入参：statusValue（状态值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const getStatusOption = (statusValue) =>
    statusOptions.value.find(/** 判断当前条目是否为查找目标。 入参：option。 */ (option) => option.label === statusValue) || {
      label: statusValue,
      type: 'info',
    }

  /**
   * 获取行Tag类型。
   * 入参：row（行）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const getRowTagType = (row) => row.tagType || getStatusOption(row[statusProp.value]).type || 'info'
  /**
   * 获取记录Display名称。
   * 入参：record（记录）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const getRecordDisplayName = (record) => record?.[primaryKey.value] || record?.[tableColumns.value[0]?.prop] || recordName.value

  /**
   * assignForm模型的处理逻辑。
   * 入参：record（记录）。
   */
  const assignFormModel = (record = {}) => {
    Object.keys(formModel).forEach(/** 处理遍历到的当前条目。 入参：key。 */ (key) => {
      delete formModel[key]
    })

    editableColumns.value.forEach(/** 处理遍历到的当前条目。 入参：column。 */ (column) => {
      const formProp = getFormProp(column)
      formModel[column.prop] = record[column.prop] ?? ''

      if (formProp !== column.prop) {
        formModel[formProp] = record[formProp] ?? record[column.prop] ?? ''
      }
    })

    if (statusColumn.value && !formModel[statusProp.value]) {
      formModel[statusProp.value] = statusOptions.value[0]?.label || ''
    }
  }

  /**
   * 构建默认记录。
   * @returns 组装的键值对象。
   */
  const buildDefaultRecord = () => {
    const emptyRecord = editableColumns.value.reduce(/** 合并当前条目与累计结果。 入参：record、column。 */ (record, column) => {
      record[column.prop] = ''
      return record
    }, {})

    return {
      ...emptyRecord,
      ...(config.value.defaultRecord || {}),
    }
  }

  /**
   * 归一化Submitted记录。
   * @returns record 的当前值。
   */
  const normalizeSubmittedRecord = () => {
    const record = editableColumns.value.reduce(/** 合并当前条目与累计结果。 入参：nextRecord、column。 */ (nextRecord, column) => {
      const formProp = getFormProp(column)
      nextRecord[column.prop] = formModel[column.prop]

      if (formProp !== column.prop) {
        nextRecord[formProp] = formModel[formProp]
      }

      return nextRecord
    }, {})

    if (statusColumn.value) {
      record.tagType = getStatusOption(record[statusProp.value]).type
    }

    return record
  }

  /**
   * 校验Form模型。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const validateFormModel = () => {
    const invalidColumn = editableColumns.value.find(/** 判断当前条目是否为查找目标。 入参：column。 */ (column) => {
      if (column.required === false) return false
      const formProp = getFormProp(column)
      return !String(formModel[formProp] ?? formModel[column.prop] ?? '').trim()
    })

    if (invalidColumn) {
      ElMessage.warning(`请填写${invalidColumn.label}`)
      return false
    }

    return true
  }

  /**
   * 打开CreateDialog。
   */
  const openCreateDialog = () => {
    dialogMode.value = 'create'
    activeRecord.value = null
    assignFormModel(buildDefaultRecord())
    dialogVisible.value = true
  }

  /**
   * 打开EditDialog。
   * 入参：record（记录）。
   */
  const openEditDialog = (record) => {
    dialogMode.value = 'edit'
    activeRecord.value = record
    assignFormModel(record)
    dialogVisible.value = true
  }

  /**
   * 提交记录。
   */
  const submitRecord = async () => {
    if (!validateFormModel()) return

    formSubmitting.value = true

    try {
      const submittedRecord = normalizeSubmittedRecord()

      if (singleRecordMode.value) {
        if (dialogMode.value === 'create') {
          const createdRecord = await collectionService.value.create(submittedRecord)
          recordsState.value.unshift(normalizeRecords([createdRecord])[0])
        } else if (activeRecord.value?.id) {
          const updatedRecord = await collectionService.value.update(activeRecord.value.id, submittedRecord)
          recordsState.value = recordsState.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) =>
            record._recordId === activeRecord.value._recordId
              ? {
                  ...record,
                  ...normalizeRecords([updatedRecord])[0],
                  _recordId: record._recordId,
                }
              : record,
          )
        }
      } else {
        if (dialogMode.value === 'create') {
          recordsState.value.unshift({
            ...submittedRecord,
            _recordId: createRecordId(),
          })
        } else if (activeRecord.value) {
          recordsState.value = recordsState.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) =>
            record._recordId === activeRecord.value._recordId
              ? {
                  ...record,
                  ...submittedRecord,
                }
              : record,
          )
        }

        await persistRecords()
      }

      dialogVisible.value = false
      ElMessage.success(`${dialogMode.value === 'create' ? '新增' : '更新'}${recordName.value}成功`)
    } finally {
      formSubmitting.value = false
    }
  }

  /**
   * 视图记录详情的处理逻辑。
   * 入参：record（记录）。
   */
  const viewRecordDetail = (record) => {
    activeRecord.value = record
    detailVisible.value = true
  }

  /**
   * 删除记录。
   * 入参：record（记录）。
   */
  const deleteRecord = async (record) => {
    try {
      await ElMessageBox.confirm(`确认删除“${getRecordDisplayName(record)}”吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    if (singleRecordMode.value && record.id) {
      const nextRecords = await collectionService.value.remove(record.id)
      recordsState.value = normalizeRecords(nextRecords)
    } else {
      recordsState.value = recordsState.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item._recordId !== record._recordId)
      await persistRecords()
    }

    resetSelection()
    ElMessage.success(`已删除${recordName.value}`)
  }

  /**
   * 处理SelectionChange。
   * 入参：rows（行集合）。
   */
  const handleSelectionChange = (rows) => {
    selectedRows.value = rows
  }

  /**
   * 应用Batch状态。
   * 入参：targetStatus（目标状态）。
   */
  const applyBatchStatus = async (targetStatus = batchTargetStatus.value) => {
    if (!selectedRows.value.length) {
      ElMessage.warning(`请先选择需要处理的${recordName.value}`)
      return
    }

    if (!targetStatus) {
      ElMessage.warning('当前业务暂无可流转状态')
      return
    }

    const selectedIds = new Set(selectedRows.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => record._recordId))
    const targetOption = getStatusOption(targetStatus)

    recordsState.value = recordsState.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) =>
      selectedIds.has(record._recordId)
        ? {
            ...record,
            [statusProp.value]: targetStatus,
            tagType: targetOption.type,
          }
        : record,
    )

    if (singleRecordMode.value) {
      await Promise.all(
        recordsState.value
          .filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => selectedIds.has(record._recordId) && record.id)
          .map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => collectionService.value.update(record.id, stripInternalFields(record))),
      )
    } else {
      await persistRecords()
    }

    resetSelection()
    ElMessage.success(`已将所选${recordName.value}标记为${targetStatus}`)
  }

  /**
   * 删除已选择记录集合。
   */
  const deleteSelectedRecords = async () => {
    if (!selectedRows.value.length) {
      ElMessage.warning(`请先选择需要删除的${recordName.value}`)
      return
    }

    try {
      await ElMessageBox.confirm(`确认删除所选 ${selectedRows.value.length} 条${recordName.value}吗？`, '批量删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    const selectedIds = new Set(selectedRows.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => record._recordId))
    if (singleRecordMode.value) {
      await Promise.all(
        recordsState.value
          .filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => selectedIds.has(record._recordId) && record.id)
          .map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) => collectionService.value.remove(record.id)),
      )
      recordsState.value = recordsState.value.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => !selectedIds.has(record._recordId))
    } else {
      recordsState.value = recordsState.value.filter(/** 判断当前条目是否符合筛选条件。 入参：record。 */ (record) => !selectedIds.has(record._recordId))
      await persistRecords()
    }

    resetSelection()
    ElMessage.success(`已删除所选${recordName.value}`)
  }

  /**
   * 导出记录集合。
   */
  const exportRecords = () => {
    const exportRows = filteredRecords.value.length ? filteredRecords.value : recordsState.value

    if (!exportRows.length) {
      ElMessage.warning('暂无可导出的数据')
      return
    }

    const configuredProps = new Set(tableColumns.value.map(/** 将当前条目转换为目标结果。 入参：column。 */ (column) => column.prop))
    const extraColumns = [
      ...new Set(
        exportRows.flatMap(/** 转换并展开当前条目。 入参：record。 */ (record) =>
          Object.keys(record).filter(/** 判断当前条目是否符合筛选条件。 入参：key。 */ (key) => !key.startsWith('_') && key !== 'tagType' && !configuredProps.has(key)),
        ),
      ),
    ].map(/** 将当前条目转换为目标结果。 入参：key。 */ (key) => ({ label: key, prop: key }))
    const exportColumns = [
      ...tableColumns.value.map(/** 将当前条目转换为目标结果。 入参：column。 */ (column) => ({
        label: column.label,
        prop: column.prop,
      })),
      ...extraColumns,
    ]
    const exportedCount = exportTableToExcel({
      filename: pageTitle.value,
      sheetName: pageTitle.value,
      rows: exportRows,
      columns: exportColumns,
    })
    ElMessage.success(`已导出 ${exportedCount} 条${recordName.value}`)
  }

  /**
   * 重置Module记录集合。
   */
  const resetModuleRecords = async () => {
    try {
      await ElMessageBox.confirm('确认恢复当前页面的初始数据吗？', '恢复数据', {
        type: 'info',
        confirmButtonText: '恢复',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }

    keyword.value = ''
    statusFilter.value = 'all'
    recordsState.value = normalizeRecords(await collectionService.value.reset({ fallbackRecords: initialRecords.value }))
    resetSelection()
    ElMessage.success('已恢复初始数据')
  }

  /**
   * 检测Conflicts。
   */
  const detectConflicts = async () => {
    const conflictIds = new Set()
    const seen = new Map()

    recordsState.value.forEach(/** 处理遍历到的当前条目。 入参：record。 */ (record) => {
      const conflictKey = [record.lab, record.time].filter(Boolean).join('|')

      if (conflictKey && seen.has(conflictKey)) {
        conflictIds.add(record._recordId)
        conflictIds.add(seen.get(conflictKey))
      }

      if (conflictKey) {
        seen.set(conflictKey, record._recordId)
      }

      if (String(record[statusProp.value] ?? '').includes('冲突')) {
        conflictIds.add(record._recordId)
      }
    })

    if (!conflictIds.size) {
      ElMessage.success('未发现预约时间或资源冲突')
      return
    }

    const conflictOption = getStatusOption('冲突')
    recordsState.value = recordsState.value.map(/** 将当前条目转换为目标结果。 入参：record。 */ (record) =>
      conflictIds.has(record._recordId)
        ? {
            ...record,
            [statusProp.value]: '冲突',
            tagType: conflictOption.type || 'danger',
          }
        : record,
    )
    statusFilter.value = '冲突'
    await persistRecords()
    ElMessage.warning(`发现 ${conflictIds.size} 条冲突记录，已自动筛选`)
  }

  /**
   * 筛选Abnormal记录集合。
   */
  const filterAbnormalRecords = () => {
    const dangerStatus = statusSummary.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.count && item.type === 'danger')
    const warningStatus = statusSummary.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.count && item.type === 'warning')
    const targetStatus = dangerStatus?.label || warningStatus?.label

    if (!targetStatus) {
      ElMessage.success('当前没有异常或待复核记录')
      return
    }

    statusFilter.value = targetStatus
    ElMessage.warning(`已筛选 ${targetStatus}${recordName.value}`)
  }

  /**
   * show课表的处理逻辑。
   */
  const showSchedule = () => {
    keyword.value = '课程'
    statusFilter.value = 'all'
    ElMessage.success('已按课程相关数据筛选课表视图')
  }

  /**
   * 校验权限。
   */
  const validatePermissions = () => {
    const reviewStatus = statusSummary.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.label.includes('复核') && item.count)

    if (reviewStatus) {
      statusFilter.value = reviewStatus.label
      ElMessage.warning(`发现 ${reviewStatus.count} 项权限配置待复核`)
      return
    }

    ElMessage.success('权限配置校验通过')
  }

  /**
   * infer命令的处理逻辑。
   * 入参：action（action）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  const inferCommand = (action) => {
    if (action.command) return action.command
    if (action.label.includes('新增') || action.label.includes('新建') || action.label.includes('登记')) return 'create'
    if (action.label.includes('导出') || action.label.includes('报表')) return 'export'
    return 'message'
  }

  /**
   * 处理ModuleAction。
   * 入参：action（action）。
   */
  const handleModuleAction = (action) => {
    const command = inferCommand(action)

    if (command === 'create') {
      openCreateDialog()
      return
    }

    if (command === 'export') {
      exportRecords()
      return
    }

    if (command === 'batchStatus') {
      void applyBatchStatus(action.targetStatus)
      return
    }

    if (command === 'detectConflicts') {
      void detectConflicts()
      return
    }

    if (command === 'filterAbnormal') {
      filterAbnormalRecords()
      return
    }

    if (command === 'showSchedule') {
      showSchedule()
      return
    }

    if (command === 'validatePermissions') {
      validatePermissions()
      return
    }

    ElMessage.success(`${action.label}操作已完成`)
  }

  watch(
    [/** 响应被监听状态的变化。 */ () => route.name, config],
    /**
     * 响应被监听状态的变化。
     */
    () => {
      void loadRecords()
    },
    { immediate: true },
  )

  watch([keyword, statusFilter], /** 响应被监听状态的变化。 */ () => {
    currentPage.value = 1
    resetSelection()
  })

  watch(filteredRecordCount, /** 响应被监听状态的变化。 入参：count。 */ (count) => {
    const maxPage = Math.max(1, Math.ceil(count / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  return {
    records: computed(/** 计算派生状态。 */ () => recordsState.value),
    keyword,
    statusFilter,
    currentPage,
    pageSize,
    selectedRows,
    detailVisible,
    dialogVisible,
    activeRecord,
    formSubmitting,
    formModel,
    actions,
    tableColumns,
    recordName,
    statusProp,
    editableColumns,
    formFieldOptions,
    statusOptions,
    statusSummary,
    totalRecordCount,
    filteredRecords,
    filteredRecordCount,
    pagedRecords,
    batchTargetStatus,
    dialogTitle,
    getRowTagType,
    handleModuleAction,
    openCreateDialog,
    openEditDialog,
    submitRecord,
    viewRecordDetail,
    deleteRecord,
    handleSelectionChange,
    handleCurrentPageChange,
    handlePageSizeChange,
    applyBatchStatus,
    deleteSelectedRecords,
    exportRecords,
    resetModuleRecords,
  }
}
