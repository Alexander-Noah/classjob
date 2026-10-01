import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createEmptyDeviceAsset,
  deviceAssetService,
  normalizeDeviceAsset,
  resolveCategory,
  toDeviceAssetPayload,
} from '../api/services/deviceAssets'
import {
  applyDeviceLabBinding,
  getDeviceLabName,
  getDeviceLocation,
  getLabId,
  getLabName,
  matchDeviceLab,
} from '../api/services/deviceLabBinding'
import { listLabs } from '../api/services/lab'
import { listUserOptions } from '../api/services/users'

const trimText = (value) => String(value ?? '').trim()
const nowDateTime = () => new Date().toISOString().slice(0, 19).replace('T', ' ')

const inventoryResultOptions = [
  { label: '已盘点', type: 'success' },
  { label: '盘点异常', type: 'warning' },
  { label: '设备缺失', type: 'danger' },
  { label: '设备损坏', type: 'danger' },
]

const createInventoryForm = () => ({
  inspectorName: '',
  inspectedAt: nowDateTime(),
  resultStatus: '已盘点',
  normal: true,
  missing: false,
  damaged: false,
  remark: '',
})

const createTransferForm = () => ({
  toLabId: '',
  toOwnerUserId: '',
  toLocation: '',
  transferAt: nowDateTime(),
  operatorName: '',
  reason: '',
})

export function useDeviceManagement() {
  const keyword = ref('')
  const labFilter = ref('')
  const categoryFilter = ref('')
  const statusFilter = ref('')
  const currentPage = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const devices = ref([])
  const stats = ref({ total: 0, normal: 0, repairing: 0, idle: 0, scrapped: 0 })
  const loading = ref(false)
  const importingDevices = ref(false)
  const formSubmitting = ref(false)
  const dialogVisible = ref(false)
  const dialogMode = ref('create')
  const detailVisible = ref(false)
  const inventoryDialogVisible = ref(false)
  const inventorySubmitting = ref(false)
  const transferDialogVisible = ref(false)
  const transferSubmitting = ref(false)
  const selectedDevices = ref([])
  const selectedDevice = ref(null)
  const inventoryDevice = ref(null)
  const inventoryDevices = ref([])
  const transferDevices = ref([])
  const formModel = reactive(createEmptyDeviceAsset())
  const inventoryFormModel = reactive(createInventoryForm())
  const transferFormModel = reactive(createTransferForm())
  const labOptions = ref([])
  const labOptionsLoaded = ref(false)
  const ownerOptions = ref([])
  const ownerOptionsLoaded = ref(false)

  const categoryOptions = deviceAssetService.categoryOptions
  const statusOptions = deviceAssetService.statusOptions

  const queryParams = computed(() => ({
    pageNum: currentPage.value,
    pageSize: pageSize.value,
    keyword: trimText(keyword.value) || undefined,
    labId: trimText(labFilter.value) || undefined,
    labName: getLabName(labOptions.value.find((lab) => String(getLabId(lab)) === String(labFilter.value))) || undefined,
    category: trimText(categoryFilter.value) || undefined,
    status: trimText(statusFilter.value) || undefined,
  }))

  const metrics = computed(() => [
    { label: '设备总数', value: stats.value.total ?? 0, trend: '设备资产条目' },
    { label: '正常设备', value: stats.value.normal ?? 0, trend: '状态为正常' },
    { label: '维修中', value: stats.value.repairing ?? 0, trend: '状态为维修中' },
    { label: '闲置', value: stats.value.idle ?? 0, trend: '状态为闲置' },
    { label: '已报废', value: stats.value.scrapped ?? 0, trend: '状态为已报废' },
  ])

  const dialogTitle = computed(() => (dialogMode.value === 'edit' ? '编辑设备资产' : '新增设备资产'))

  const resetForm = (device = createEmptyDeviceAsset()) => {
    Object.assign(formModel, createEmptyDeviceAsset(), normalizeDeviceAsset(device))
  }

  const hasLabSignal = (device = {}) =>
    trimText(getDeviceLabName(device)) || trimText(getDeviceLocation(device)) || trimText(device.deviceName) || trimText(device.remark)

  const bindFormToMatchedLab = (showUnmatchedHint = false) => {
    const matchedLab = matchDeviceLab(formModel, labOptions.value)
    if (matchedLab) {
      Object.assign(formModel, applyDeviceLabBinding(formModel, matchedLab))
      return true
    }
    formModel.labId = ''
    if (showUnmatchedHint && hasLabSignal(formModel)) {
      ElMessage.info('未自动匹配到实验室，请手动选择所属实验室')
    }
    return false
  }

  const loadLabOptions = async (force = false) => {
    if (labOptionsLoaded.value && !force) return labOptions.value
    try {
      labOptions.value = await listLabs()
      labOptionsLoaded.value = true
      return labOptions.value
    } catch (error) {
      labOptions.value = []
      labOptionsLoaded.value = false
      if (!error?.__handled) {
        ElMessage.error('实验室列表加载失败')
      }
      return []
    }
  }

  const loadOwnerOptions = async (force = false) => {
    if (ownerOptionsLoaded.value && !force) return ownerOptions.value
    try {
      ownerOptions.value = await listUserOptions()
      ownerOptionsLoaded.value = true
      return ownerOptions.value
    } catch {
      ownerOptions.value = []
      ownerOptionsLoaded.value = false
      return []
    }
  }

  const loadDevices = async () => {
    loading.value = true
    try {
      const result = await deviceAssetService.page(queryParams.value)
      devices.value = result.records
      total.value = result.total
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '加载设备资产列表失败')
      }
    } finally {
      loading.value = false
    }
  }

  const loadStats = async () => {
    try {
      stats.value = await deviceAssetService.stats()
    } catch {
      stats.value = {
        total: total.value || devices.value.length,
        normal: devices.value.filter((device) => device.status === '正常').length,
        repairing: devices.value.filter((device) => device.status === '维修中').length,
        idle: devices.value.filter((device) => device.status === '闲置').length,
        scrapped: devices.value.filter((device) => device.status === '已报废').length,
      }
    }
  }

  const reload = async () => {
    await loadDevices()
    await loadStats()
  }

  const handleSelectionChange = (selection = []) => {
    selectedDevices.value = selection.map(normalizeDeviceAsset)
  }

  const search = async () => {
    currentPage.value = 1
    await reload()
  }

  const resetFilters = async () => {
    keyword.value = ''
    labFilter.value = ''
    categoryFilter.value = ''
    statusFilter.value = ''
    currentPage.value = 1
    await reload()
  }

  const openCreateDialog = async () => {
    await loadLabOptions()
    dialogMode.value = 'create'
    resetForm()
    dialogVisible.value = true
  }

  const openEditDialog = async (device) => {
    if (!device) return
    await loadLabOptions()
    dialogMode.value = 'edit'
    resetForm(device)
    bindFormToMatchedLab(true)
    dialogVisible.value = true
  }

  const openDetailDialog = async (device) => {
    if (!device) return
    selectedDevice.value = device
    detailVisible.value = true

    try {
      selectedDevice.value = await deviceAssetService.detail(device.id)
    } catch {
      selectedDevice.value = device
    }
  }

  const openInventoryDialog = (device) => {
    if (!device) return
    inventoryDevice.value = normalizeDeviceAsset(device)
    inventoryDevices.value = []
    Object.assign(inventoryFormModel, createInventoryForm())
    inventoryDialogVisible.value = true
  }

  const openBatchInventoryDialog = () => {
    if (!selectedDevices.value.length) {
      ElMessage.warning('请先选择需要盘点的设备资产')
      return
    }
    inventoryDevice.value = null
    inventoryDevices.value = selectedDevices.value.map(normalizeDeviceAsset)
    Object.assign(inventoryFormModel, createInventoryForm())
    inventoryDialogVisible.value = true
  }

  const openTransferDialog = async (device) => {
    if (!device) return
    await Promise.all([loadLabOptions(), loadOwnerOptions()])
    transferDevices.value = [normalizeDeviceAsset(device)]
    Object.assign(transferFormModel, createTransferForm())
    transferDialogVisible.value = true
  }

  const openBatchTransferDialog = async () => {
    if (!selectedDevices.value.length) {
      ElMessage.warning('请先选择需要调拨的设备资产')
      return
    }
    await Promise.all([loadLabOptions(), loadOwnerOptions()])
    transferDevices.value = selectedDevices.value.map(normalizeDeviceAsset)
    Object.assign(transferFormModel, createTransferForm())
    transferDialogVisible.value = true
  }

  const submitDevice = async () => {
    if (!trimText(formModel.deviceName)) {
      ElMessage.warning('请填写设备名称')
      return
    }
    if (!trimText(formModel.category)) {
      ElMessage.warning('请选择设备分类')
      return
    }
    if (!trimText(formModel.labName)) {
      ElMessage.warning('请填写所属实验室')
      return
    }
    if (!Number(formModel.quantity) || Number(formModel.quantity) <= 0) {
      ElMessage.warning('请填写大于 0 的设备数量')
      return
    }
    if (!trimText(formModel.unit)) {
      ElMessage.warning('请填写单位')
      return
    }

    formSubmitting.value = true
    try {
      await loadLabOptions()
      const matchedLab = matchDeviceLab(formModel, labOptions.value)
      if (matchedLab) {
        Object.assign(formModel, applyDeviceLabBinding(formModel, matchedLab))
      }
      const payload = toDeviceAssetPayload({
        ...formModel,
        category: formModel.category || resolveCategory(formModel.deviceName),
      })
      if (dialogMode.value === 'edit') {
        await deviceAssetService.update(formModel.id, payload, labOptions.value)
        ElMessage.success('设备资产已更新')
      } else {
        await deviceAssetService.create(payload, labOptions.value)
        ElMessage.success('设备资产已新增')
      }
      dialogVisible.value = false
      await reload()
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '保存设备资产失败')
      }
    } finally {
      formSubmitting.value = false
    }
  }

  const deleteDevice = async (device) => {
    if (!device) return

    try {
      await ElMessageBox.confirm('确定删除该设备资产吗？', '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })
      await deviceAssetService.remove(device.id)
      ElMessage.success('设备资产已删除')
      await reload()
    } catch (error) {
      if (!['cancel', 'close'].includes(error) && !error?.__handled) {
        ElMessage.error(error?.message || '删除设备资产失败')
      }
    }
  }

  const batchDeleteDevices = async () => {
    if (!selectedDevices.value.length) {
      ElMessage.warning('请先选择需要删除的设备资产')
      return
    }

    try {
      await ElMessageBox.confirm(`确定删除已选择的 ${selectedDevices.value.length} 条设备资产吗？`, '批量删除确认', {
        type: 'warning',
        confirmButtonText: '批量删除',
        cancelButtonText: '取消',
      })
      await Promise.all(selectedDevices.value.map((device) => deviceAssetService.remove(device.id)))
      ElMessage.success(`已删除 ${selectedDevices.value.length} 条设备资产`)
      selectedDevices.value = []
      await reload()
    } catch (error) {
      if (!['cancel', 'close'].includes(error) && !error?.__handled) {
        ElMessage.error(error?.message || '批量删除设备资产失败')
      }
    }
  }

  const submitInventory = async () => {
    const targetDevices = inventoryDevices.value.length ? inventoryDevices.value : [inventoryDevice.value].filter(Boolean)
    if (!targetDevices.length) return
    if (!trimText(inventoryFormModel.inspectorName)) {
      ElMessage.warning('请填写盘点人')
      return
    }
    if (!trimText(inventoryFormModel.inspectedAt)) {
      ElMessage.warning('请选择盘点时间')
      return
    }
    if (!trimText(inventoryFormModel.resultStatus)) {
      ElMessage.warning('请选择盘点结果')
      return
    }

    inventorySubmitting.value = true
    try {
      await Promise.all(
        targetDevices.map((device) =>
          deviceAssetService.createInventoryRecord(device.id, {
            ...inventoryFormModel,
            deviceCode: device.assetNo,
            deviceName: device.deviceName,
            currentDevice: device,
          }),
        ),
      )
      ElMessage.success(targetDevices.length > 1 ? `已保存 ${targetDevices.length} 条设备盘点记录` : '设备盘点记录已保存')
      inventoryDialogVisible.value = false
      inventoryDevices.value = []
      await reload()
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '保存盘点记录失败')
      }
    } finally {
      inventorySubmitting.value = false
    }
  }

  const submitTransfer = async () => {
    if (!transferDevices.value.length) return
    if (!trimText(transferFormModel.operatorName)) {
      ElMessage.warning('请填写经办人')
      return
    }

    const selectedLab = labOptions.value.find((lab) => String(getLabId(lab)) === String(transferFormModel.toLabId || ''))
    const selectedOwner = ownerOptions.value.find((user) => String(user.id) === String(transferFormModel.toOwnerUserId || ''))
    const ownerName = selectedOwner?.realName || selectedOwner?.name || selectedOwner?.username || ''
    const payload = {
      ...transferFormModel,
      toLabName: selectedLab ? getLabName(selectedLab) : '',
      toOwnerName: ownerName,
    }

    transferSubmitting.value = true
    try {
      await Promise.all(
        transferDevices.value.map((device) =>
          deviceAssetService.transfer(device.id, {
            ...payload,
            currentDevice: device,
            deviceCode: device.assetNo,
            deviceName: device.deviceName,
          }),
        ),
      )
      ElMessage.success('设备调拨记录已保存')
      transferDialogVisible.value = false
      selectedDevices.value = []
      await reload()
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '保存调拨记录失败')
      }
    } finally {
      transferSubmitting.value = false
    }
  }

  const importDevicesFromFile = async (file) => {
    if (!file) return

    importingDevices.value = true
    try {
      await loadLabOptions()
      const result = await deviceAssetService.importFile(file, labOptions.value)
      ElMessage.success(`导入完成，共处理 ${result?.count ?? 0} 条设备资产`)
      await reload()
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '导入设备资产失败')
      }
    } finally {
      importingDevices.value = false
    }
  }

  const exportDevices = async () => {
    try {
      await deviceAssetService.export(queryParams.value)
      ElMessage.success('设备资产导出已开始')
    } catch (error) {
      if (!error?.__handled) {
        ElMessage.error(error?.message || '导出设备资产失败')
      }
    }
  }

  const downloadDeviceImportTemplate = () => {
    deviceAssetService.downloadTemplate()
    ElMessage.success('设备资产导入模板已下载')
  }

  const handlePageChange = async (page) => {
    currentPage.value = page
    await reload()
  }

  const handlePageSizeChange = async (size) => {
    pageSize.value = size
    currentPage.value = 1
    await reload()
  }

  void loadLabOptions()
  void reload()

  return {
    categoryFilter,
    categoryOptions,
    currentPage,
    batchDeleteDevices,
    deleteDevice,
    detailVisible,
    dialogTitle,
    dialogVisible,
    downloadDeviceImportTemplate,
    exportDevices,
    formModel,
    formSubmitting,
    handlePageChange,
    handlePageSizeChange,
    handleSelectionChange,
    importDevicesFromFile,
    importingDevices,
    inventoryDevice,
    inventoryDevices,
    inventoryDialogVisible,
    inventoryFormModel,
    inventoryResultOptions,
    inventorySubmitting,
    keyword,
    labFilter,
    labOptions,
    loadLabOptions,
    loading,
    metrics,
    openCreateDialog,
    openDetailDialog,
    openEditDialog,
    openBatchInventoryDialog,
    openBatchTransferDialog,
    openInventoryDialog,
    openTransferDialog,
    ownerOptions,
    pageSize,
    resetFilters,
    search,
    selectedDevice,
    selectedDevices,
    statusFilter,
    statusOptions,
    submitDevice,
    submitInventory,
    submitTransfer,
    total,
    transferDevices,
    transferDialogVisible,
    transferFormModel,
    transferSubmitting,
    devices,
  }
}
