<!--
  Web 管理平台的耗材库存视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Download,
  EditPen,
  Plus,
  Search,
  SortDown,
  SortUp,
  Upload,
} from '@element-plus/icons-vue'
import http from '../../api/http'
import { API_ENDPOINTS } from '../../api/endpoints'
import { exportTableToExcel } from '../../utils/exportExcel'
import { readSpreadsheetRows } from '../../utils/importExcel'
import ConsumableLoans from './components/ConsumableLoans.vue'

defineOptions({ name: 'ConsumableInventory' })

const loading = ref(false)
const saving = ref(false)
const records = ref([])
const stockRecords = ref([])
const selectedId = ref('')
const inventoryDialogVisible = ref(false)
const movementDialogVisible = ref(false)
const movementType = ref('in')
const inventoryDialogMode = ref('create')
const fileInputRef = ref(null)
const loanPanelRef = ref()

const filters = reactive({
  keyword: '',
  category: '',
  status: '',
})

const recordFilters = reactive({
  keyword: '',
  type: '',
  consumableId: '',
  dateRange: [],
})

const inventoryForm = reactive({
  id: '',
  name: '',
  category: '',
  stock: 0,
  unit: '件',
  location: '',
  warnThreshold: 5,
})

const movementForm = reactive({
  consumableId: '',
  quantity: 1,
  operator: '张明',
  source: '',
  reason: '',
  remark: '',
})

/**
 * 获取响应数据。
 * 入参：payload（载荷）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getResponseData = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.records)) return payload.data.records
  if (Array.isArray(payload?.records)) return payload.records
  return []
}

/**
 * 转换为数值。
 * 入参：value（值）；fallback（fallback）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const toNumber = (value, fallback = 0) => {
  const number = Number(String(value ?? '').replace(/[^\d.-]/g, ''))
  return Number.isFinite(number) ? number : fallback
}

/**
 * pad数值的处理逻辑。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const padNumber = (value) => String(value).padStart(2, '0')

/**
 * 格式化日期时间。
 * 入参：date（日期）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const formatDateTime = (date = new Date()) =>
  `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(
    date.getHours(),
  )}:${padNumber(date.getMinutes())}:${padNumber(date.getSeconds())}`

/**
 * 获取记录日期。
 * 入参：time（时间）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getRecordDate = (time = '') => String(time).slice(0, 10).replace(/\//g, '-')

/**
 * 获取库存状态。
 * 入参：stock（库存）；warnThreshold（warnThreshold）。
 * @returns 组装的键值对象。
 */
const getStockState = (stock, warnThreshold) => {
  if (stock <= 0) return { status: '待补货', tagType: 'danger' }
  if (stock <= warnThreshold) return { status: '低库存', tagType: 'warning' }
  return { status: '库存充足', tagType: 'success' }
}

/**
 * 归一化耗材库存。
 * 入参：item（条目）。
 * @returns 组装的键值对象。
 */
const normalizeConsumable = (item = {}) => {
  const stock = Math.max(toNumber(item.stock), 0)
  const warnThreshold = Math.max(toNumber(item.warnThreshold, 5), 0)
  const state = getStockState(stock, warnThreshold)

  return {
    id: item.id,
    name: item.name || '未命名耗材',
    category: item.category || '未分类',
    stock,
    unit: item.unit || '件',
    location: item.location || '未设置',
    warnThreshold,
    status: item.status || state.status,
    tagType: item.tagType || state.tagType,
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
  }
}

/**
 * 归一化库存记录。
 * 入参：item（条目）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeStockRecord = (item = {}) => ({
  id: item.id || `SR-${Date.now()}`,
  type: item.type || item.movementType || '入库',
  consumableId: item.consumableId,
  consumableName: item.consumableName || item.name || '-',
  category: item.category || '-',
  quantity: Math.max(toNumber(item.quantity), 0),
  unit: item.unit || '件',
  beforeStock: toNumber(item.beforeStock),
  afterStock: toNumber(item.afterStock),
  operator: item.operator || item.operatorName || '系统',
  source: String(item.source || item.sourceTarget || '-').replace(/^(借用归还#\d+):[0-9a-f-]{36}$/i, '$1'),
  reason: item.reason || '-',
  remark: item.remark || '',
  time: item.time || item.operatedAt || item.createdAt || formatDateTime(),
})

const selectedRecord = computed(/** 计算派生状态。 */ () => records.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => String(item.id) === String(selectedId.value)) || null)

const categoryOptions = computed(/** 计算派生状态。 */ () => [...new Set(records.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => item.category).filter(Boolean))])
const defaultCategories = ['网络耗材', '电脑配件', '外设配件', '电子元器件', '实验材料', '办公耗材', '打印耗材', '清洁用品', '安全防护用品', '其他']
const categorySelectOptions = computed(/** 计算派生状态。 */ () => [...new Set([
  ...defaultCategories,
  ...categoryOptions.value,
  inventoryForm.category,
].filter(Boolean))])

const filteredRecords = computed(/** 计算派生状态。 */ () => {
  const keyword = filters.keyword.trim().toLowerCase()

  return records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => {
    const text = [item.name, item.category, item.location, item.status].join(' ').toLowerCase()
    return (
      (!keyword || text.includes(keyword)) &&
      (!filters.category || item.category === filters.category) &&
      (!filters.status || item.status === filters.status)
    )
  })
})

const warningItems = computed(/** 计算派生状态。 */ () => records.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.tagType === 'warning' || item.tagType === 'danger'))

const dashboard = computed(/** 计算派生状态。 */ () => {
  const totalStock = records.value.reduce(/** 合并当前条目与累计结果。 入参：sum、item。 */ (sum, item) => sum + item.stock, 0)
  const today = new Date().toISOString().slice(0, 10)
  const todayMovements = stockRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => getRecordDate(item.time) === today).length

  return [
    { label: '耗材种类', value: records.value.length, desc: `${categoryOptions.value.length || 0} 个分类`, tone: 'blue' },
    { label: '库存总量', value: totalStock, desc: '所有耗材合计', tone: 'green' },
    { label: '库存预警', value: warningItems.value.length, desc: '低库存/待补货', tone: warningItems.value.length ? 'orange' : 'green' },
    { label: '今日出入库', value: todayMovements, desc: '自动记录流水', tone: 'purple' },
  ]
})

const filteredStockRecords = computed(/** 计算派生状态。 */ () => {
  const keyword = recordFilters.keyword.trim().toLowerCase()
  const [start, end] = recordFilters.dateRange || []

  return stockRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => {
    const text = [item.consumableName, item.category, item.operator, item.source, item.reason, item.remark]
      .join(' ')
      .toLowerCase()
    const itemDate = getRecordDate(item.time)

    return (
      (!keyword || text.includes(keyword)) &&
      (!recordFilters.type || item.type === recordFilters.type) &&
      (!recordFilters.consumableId || String(item.consumableId) === String(recordFilters.consumableId)) &&
      (!start || itemDate >= start) &&
      (!end || itemDate <= end)
    )
  })
})

/**
 * 加载库存记录集合。
 */
const loadStockRecords = async () => {
  const response = await http.get(API_ENDPOINTS.consumables.records)
  stockRecords.value = getResponseData(response.data).map(normalizeStockRecord)
}

/**
 * 借用变化的处理逻辑。
 */
const loansChanged = async () => {
  try { await Promise.all([loadRecords(), loadStockRecords()]) }
  catch { ElMessage.warning('借还已登记，库存显示刷新失败，请刷新页面查看') }
}

/**
 * 加载记录集合。
 */
const loadRecords = async () => {
  loading.value = true
  try {
    const response = await http.get(API_ENDPOINTS.consumables.list)
    records.value = getResponseData(response.data).map(normalizeConsumable)
    if (records.value.length && !records.value.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ (item) => String(item.id) === String(selectedId.value))) {
      selectedId.value = records.value[0].id
    }
    if (warningItems.value.length) {
      ElMessage.warning(`当前有 ${warningItems.value.length} 项耗材触发库存预警`)
    }
  } finally {
    loading.value = false
  }
}

/**
 * 重置库存Form。
 * 入参：record（记录）。
 */
const resetInventoryForm = (record = {}) => {
  inventoryForm.id = record.id || ''
  inventoryForm.name = record.name || ''
  inventoryForm.category = record.category || ''
  inventoryForm.stock = record.stock ?? 0
  inventoryForm.unit = record.unit || '件'
  inventoryForm.location = record.location || ''
  inventoryForm.warnThreshold = record.warnThreshold ?? 5
}

/**
 * 打开CreateDialog。
 */
const openCreateDialog = () => {
  inventoryDialogMode.value = 'create'
  resetInventoryForm()
  inventoryDialogVisible.value = true
}

/**
 * 打开EditDialog。
 * 入参：record（记录）。
 */
const openEditDialog = (record) => {
  inventoryDialogMode.value = 'edit'
  resetInventoryForm(record)
  inventoryDialogVisible.value = true
}

/**
 * 获取库存载荷。
 * @returns 组装的键值对象。
 */
const getInventoryPayload = () => {
  const stock = Math.max(toNumber(inventoryForm.stock), 0)
  const warnThreshold = Math.max(toNumber(inventoryForm.warnThreshold, 5), 0)
  const state = getStockState(stock, warnThreshold)

  return {
    name: inventoryForm.name.trim(),
    category: inventoryForm.category.trim() || '未分类',
    stock,
    unit: inventoryForm.unit.trim() || '件',
    location: inventoryForm.location.trim() || '未设置',
    warnThreshold,
    status: state.status,
    tagType: state.tagType,
  }
}

/**
 * 提交库存。
 */
const submitInventory = async () => {
  const payload = getInventoryPayload()
  if (!payload.name) {
    ElMessage.warning('请填写耗材名称')
    return
  }
  if (!inventoryForm.category.trim()) {
    ElMessage.warning('请选择耗材分类')
    return
  }

  saving.value = true
  try {
    if (inventoryDialogMode.value === 'edit') {
      await http.put(`${API_ENDPOINTS.consumables.list}/${inventoryForm.id}`, payload)
      ElMessage.success('耗材信息已更新')
    } else {
      await http.post(API_ENDPOINTS.consumables.list, payload)
      ElMessage.success('耗材已新增')
    }
    inventoryDialogVisible.value = false
    await loadRecords()
  } finally {
    saving.value = false
  }
}

/**
 * 删除记录。
 * 入参：record（记录）。
 */
const deleteRecord = async (record) => {
  await ElMessageBox.confirm(`确认删除“${record.name}”？删除后不可恢复。`, '删除耗材', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await http.delete(`${API_ENDPOINTS.consumables.list}/${record.id}`)
  ElMessage.success('耗材已删除')
  const wasSelected = String(selectedId.value) === String(record.id)
  await loadRecords()
  if (wasSelected) selectedId.value = records.value[0]?.id || ''
  await loadStockRecords()
}

/**
 * 打开MovementDialog。
 * 入参：type（类型）；record（记录）。
 */
const openMovementDialog = (type, record = selectedRecord.value) => {
  if (!record) {
    ElMessage.warning('请先选择耗材')
    return
  }
  movementType.value = type
  movementForm.consumableId = record.id
  movementForm.quantity = 1
  movementForm.operator = '张明'
  movementForm.source = type === 'in' ? '采购入库' : '教学领用'
  movementForm.reason = type === 'in' ? '补充库存' : '实验消耗'
  movementForm.remark = ''
  movementDialogVisible.value = true
}

/**
 * 提交Movement。
 */
const submitMovement = async () => {
  const record = records.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => String(item.id) === String(movementForm.consumableId))
  const quantity = Math.max(toNumber(movementForm.quantity), 0)

  if (!record || quantity <= 0) {
    ElMessage.warning('请选择耗材并填写正确数量')
    return
  }

  const beforeStock = record.stock
  const afterStock = movementType.value === 'in' ? beforeStock + quantity : beforeStock - quantity

  if (afterStock < 0) {
    ElMessage.warning('出库数量不能大于当前库存')
    return
  }

  saving.value = true
  try {
    await http.post(API_ENDPOINTS.consumables.movement(record.id), {
      type: movementType.value,
      quantity,
      unit: record.unit,
      operator: movementForm.operator.trim(),
      source: movementForm.source.trim(),
      reason: movementForm.reason.trim(),
      remark: movementForm.remark.trim(),
    })
    movementDialogVisible.value = false
    ElMessage.success(`${movementType.value === 'in' ? '入库' : '出库'}已完成`)
    await loadRecords()
    await loadStockRecords()
    selectedId.value = record.id
  } finally {
    saving.value = false
  }
}

/**
 * 导出库存。
 */
const exportInventory = () => {
  const count = exportTableToExcel({
    filename: '耗材库存清单',
    sheetName: '库存清单',
    columns: [
      { label: '耗材名称', prop: 'name' },
      { label: '分类', prop: 'category' },
      { label: '库存', prop: 'stock' },
      { label: '单位', prop: 'unit' },
      { label: '存放位置', prop: 'location' },
      { label: '预警阈值', prop: 'warnThreshold' },
      { label: '状态', prop: 'status' },
    ],
    rows: filteredRecords.value,
  })
  ElMessage.success(`已导出 ${count} 条库存数据`)
}

/**
 * 导出库存记录集合。
 */
const exportStockRecords = () => {
  const count = exportTableToExcel({
    filename: '耗材出入库记录',
    sheetName: '出入库记录',
    columns: [
      { label: '时间', prop: 'time' },
      { label: '类型', prop: 'type' },
      { label: '耗材名称', prop: 'consumableName' },
      { label: '分类', prop: 'category' },
      { label: '数量', /** formatter的处理逻辑。 入参：row（行）。 @returns 由当前表达式或分支生成的处理结果。 */ formatter: (row) => `${row.quantity}${row.unit}` },
      { label: '库存变化', /** formatter的处理逻辑。 入参：row（行）。 @returns 由当前表达式或分支生成的处理结果。 */ formatter: (row) => `${row.beforeStock} -> ${row.afterStock}` },
      { label: '经办人', prop: 'operator' },
      { label: '来源/去向', prop: 'source' },
      { label: '原因', prop: 'reason' },
      { label: '备注', prop: 'remark' },
    ],
    rows: filteredStockRecords.value,
  })
  ElMessage.success(`已导出 ${count} 条出入库记录`)
}

/**
 * pickImport文件的处理逻辑。
 */
const pickImportFile = () => {
  fileInputRef.value?.click()
}

/**
 * 获取Import值。
 * 入参：row（行）；keys（键）；fallback（fallback）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getImportValue = (row, keys, fallback = '') => {
  const key = keys.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => row[item] !== undefined && String(row[item]).trim() !== '')
  return key ? row[key] : fallback
}

/**
 * 构建Imported载荷。
 * 入参：row（行）。
 * @returns 组装的键值对象。
 */
const buildImportedPayload = (row) => {
  const stock = Math.max(toNumber(getImportValue(row, ['库存', '当前库存', 'stock'], 0)), 0)
  const warnThreshold = Math.max(toNumber(getImportValue(row, ['预警阈值', '预警线', 'warnThreshold'], 5)), 0)
  const state = getStockState(stock, warnThreshold)

  return {
    name: String(getImportValue(row, ['耗材名称', '名称', 'name'])).trim(),
    category: String(getImportValue(row, ['分类', 'category'], '未分类')).trim() || '未分类',
    stock,
    unit: String(getImportValue(row, ['单位', 'unit'], '件')).trim() || '件',
    location: String(getImportValue(row, ['存放位置', '位置', 'location'], '未设置')).trim() || '未设置',
    warnThreshold,
    status: state.status,
    tagType: state.tagType,
  }
}

/**
 * 处理Import文件。
 * 入参：event（事件）。
 */
const handleImportFile = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  try {
    const rows = await readSpreadsheetRows(file)
    const payloads = rows.map(buildImportedPayload).filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.name)
    if (!payloads.length) {
      ElMessage.warning('导入文件中没有可识别的耗材数据')
      return
    }

    saving.value = true
    for (const payload of payloads) {
      const existed = records.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.name === payload.name && item.category === payload.category)
      if (existed) {
        await http.put(`${API_ENDPOINTS.consumables.list}/${existed.id}`, payload)
      } else {
        await http.post(API_ENDPOINTS.consumables.list, payload)
      }
    }
    ElMessage.success(`已导入 ${payloads.length} 条耗材数据`)
    await loadRecords()
  } finally {
    saving.value = false
  }
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  await Promise.all([loadRecords(), loadStockRecords()])
})
</script>

<template>
  <div class="consumable-page">
    <section class="page-panel page-hero">
      <div>
        <div class="eyebrow">耗材库存</div>
        <h1>耗材库存管理</h1>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增耗材</el-button>
        <el-button type="success" :icon="SortDown" @click="openMovementDialog('in')">入库</el-button>
        <el-button type="warning" :icon="SortUp" @click="openMovementDialog('out')">出库</el-button>
        <el-button @click="loanPanelRef?.openBorrow()">借用登记</el-button>
        <el-button :icon="Upload" @click="pickImportFile">导入</el-button>
        <el-button :icon="Download" @click="exportInventory">导出库存</el-button>
        <input ref="fileInputRef" class="hidden-input" type="file" accept=".xlsx,.csv" @change="handleImportFile" />
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="item in dashboard" :key="item.label" class="metric-card" :class="`metric-${item.tone}`">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </div>
    </section>

    <section class="page-panel">
      <div class="section-title">
        <div>
          <h2>库存清单</h2>
        </div>
        <div class="filter-bar">
          <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="搜索名称、分类、位置" />
          <el-select v-model="filters.category" clearable placeholder="全部分类">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option label="库存充足" value="库存充足" />
            <el-option label="低库存" value="低库存" />
            <el-option label="待补货" value="待补货" />
          </el-select>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredRecords"
        class="inventory-table"
      >
        <el-table-column prop="name" label="耗材名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="130" />
        <el-table-column label="库存" width="120">
          <template #default="{ row }">
            <strong>{{ row.stock }}</strong>
            <span class="unit-text">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="warnThreshold" label="预警线" width="100" />
        <el-table-column prop="location" label="存放位置" min-width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.tagType">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :icon="SortDown" @click.stop="openMovementDialog('in', row)">入库</el-button>
            <el-button link type="warning" :icon="SortUp" @click.stop="openMovementDialog('out', row)">出库</el-button>
            <el-button link type="primary" :disabled="row.stock <= 0" @click.stop="loanPanelRef?.openBorrow(row)">借用</el-button>
            <el-button link type="primary" :icon="EditPen" @click.stop="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click.stop="deleteRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <ConsumableLoans ref="loanPanelRef" :consumables="records" @changed="loansChanged" />

    <section class="page-panel">
      <div class="section-title">
        <div>
          <h2>出入库记录</h2>
        </div>
        <div class="filter-bar record-filter">
          <el-input v-model="recordFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索经办人、原因" />
          <el-select v-model="recordFilters.type" clearable placeholder="全部类型">
            <el-option label="入库" value="入库" />
            <el-option label="出库" value="出库" />
          </el-select>
          <el-select v-model="recordFilters.consumableId" clearable placeholder="全部耗材">
            <el-option v-for="item in records" :key="item.id" :label="item.name" :value="String(item.id)" />
          </el-select>
          <el-date-picker
            v-model="recordFilters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
          <el-button :icon="Download" @click="exportStockRecords">导出记录</el-button>
        </div>
      </div>
      <el-table :data="filteredStockRecords" max-height="360" empty-text="暂无出入库记录">
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === '入库' ? 'success' : 'warning'">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consumableName" label="耗材" min-width="160" />
        <el-table-column label="数量" width="110">
          <template #default="{ row }">{{ row.quantity }}{{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="库存变化" width="130">
          <template #default="{ row }">{{ row.beforeStock }} -> {{ row.afterStock }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="经办人" width="110" />
        <el-table-column prop="source" label="来源/去向" min-width="140" />
        <el-table-column prop="reason" label="原因" min-width="140" />
      </el-table>
    </section>

    <el-dialog
      v-model="inventoryDialogVisible"
      align-center
      :title="inventoryDialogMode === 'edit' ? '编辑耗材' : '新增耗材'"
      width="min(560px, calc(100vw - 32px))"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="耗材名称" required>
          <el-input v-model="inventoryForm.name" placeholder="如：网线、鼠标、固态硬盘" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="inventoryForm.category" filterable placeholder="请选择耗材分类" style="width: 100%">
            <el-option v-for="category in categorySelectOptions" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="库存数量">
              <el-input-number v-model="inventoryForm.stock" :min="0" controls-position="right" class="full-number" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="inventoryForm.unit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="预警阈值">
              <el-input-number v-model="inventoryForm.warnThreshold" :min="0" controls-position="right" class="full-number" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放位置">
              <el-input v-model="inventoryForm.location" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="inventoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitInventory">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="movementDialogVisible"
      align-center
      :title="movementType === 'in' ? '耗材入库' : '耗材出库'"
      width="min(560px, calc(100vw - 32px))"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="耗材" required>
          <el-select v-model="movementForm.consumableId" filterable placeholder="请选择耗材" class="full-select">
            <el-option
              v-for="item in records"
              :key="item.id"
              :label="`${item.name}（当前 ${item.stock}${item.unit}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="movementForm.quantity" :min="1" controls-position="right" class="full-number" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经办人">
              <el-input v-model="movementForm.operator" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="movementType === 'in' ? '来源' : '去向'">
              <el-input v-model="movementForm.source" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="原因">
          <el-input v-model="movementForm.reason" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="movementForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="movementDialogVisible = false">取消</el-button>
        <el-button :type="movementType === 'in' ? 'success' : 'warning'" :loading="saving" @click="submitMovement">
          确认{{ movementType === 'in' ? '入库' : '出库' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.consumable-page {
  display: grid;
  gap: 16px;
  min-height: 100%;
  padding-bottom: 20px;
  color: var(--text-primary);
}

.page-panel,
.metric-card {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
}

.page-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  min-height: 154px;
  border-color: rgba(15, 118, 110, 0.16);
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.12), rgba(37, 99, 235, 0.06) 48%, rgba(255, 255, 255, 0.98)),
    var(--surface-color);
  padding: 24px 28px;
}

.page-hero::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  background-image:
    linear-gradient(rgba(15, 118, 110, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 118, 110, 0.07) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, 0.16) 70%, transparent 100%);
}

.page-hero > * {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  margin-bottom: 10px;
  padding: 5px 10px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  border-radius: 7px;
  color: #115e59;
  background: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  font-weight: 700;
}

.page-hero h1,
.section-title h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 800;
  line-height: 1.28;
}

.page-hero h1 {
  font-size: 28px;
  text-wrap: balance;
}

.hero-actions {
  display: flex;
  flex: 0 1 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  max-width: 100%;
  margin-left: auto;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
}

.hero-actions :deep(.el-button) {
  margin-left: 0;
}

.hidden-input {
  display: none;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  position: relative;
  min-height: 108px;
  padding: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.metric-card::after {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  content: "";
  background: radial-gradient(circle, rgba(15, 118, 110, 0.15), rgba(15, 118, 110, 0) 68%);
}

.metric-card span,
.metric-card small {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 10px 0 8px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

.page-panel:not(.page-hero) {
  padding: 18px 20px 20px;
}

.section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  max-width: 100%;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #f8fafc;
}

.filter-bar .el-input,
.filter-bar .el-select {
  width: 210px;
}

.record-filter .el-date-editor {
  width: 260px;
}

.inventory-table {
  width: 100%;
}

.inventory-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.inventory-table :deep(.el-table__cell) {
  padding: 12px 0;
}

.unit-text {
  margin-left: 4px;
  color: var(--text-secondary);
}

.full-number,
.full-select {
  width: 100%;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 900px) {
  .page-hero,
  .section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-actions,
  .filter-bar {
    justify-content: flex-start;
    width: 100%;
  }

  .filter-bar {
    display: grid;
    grid-template-columns: 1fr;
  }

  .filter-bar .el-input,
  .filter-bar .el-select,
  .record-filter .el-date-editor {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .page-hero,
  .page-panel:not(.page-hero) {
    padding: 16px;
  }
}
</style>
