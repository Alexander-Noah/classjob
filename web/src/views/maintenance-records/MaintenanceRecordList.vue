<!--
  Web 管理平台的维护记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useTablePagination。
-->
<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete, Download, EditPen, Plus, Refresh, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { deleteMaintenanceRecord, getMaintenanceRecord, listMaintenanceRecords } from '../../api/services/maintenanceRecords'
import { getStoredUser } from '../../api/services/auth'
import { canWriteMaintenanceRecords, filterMaintenanceRecords } from '../../modules/maintenance-records/maintenanceRecordEditor'
import MaintenanceRecordForm from './components/MaintenanceRecordForm.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import { exportTableToExcel } from '../../utils/exportExcel'
import MaintenanceRecordDetail from './components/MaintenanceRecordDetail.vue'
import MaintenanceRecordFilters from './components/MaintenanceRecordFilters.vue'
import { toMaintenanceRecordExportRow } from './maintenanceRecordExport'

defineOptions({ name: 'MaintenanceRecordList' })

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadError = ref('')
const canWrite = computed(() => canWriteMaintenanceRecords(getStoredUser()))
const editorVisible = ref(false)
const editorRecord = ref({})
const editorKey = ref(0)
const editorRef = ref()
const formSaving = ref(false)
const actionId = ref('')
let requestToken = 0
const records = ref([])
const selectedRecord = ref(null)
const detailVisible = ref(false)
const filters = reactive({
  keyword: '',
  lab: '',
  device: '',
  maintenanceType: '',
  maintainer: '',
  dateRange: [],
})

const filterOptions = computed(() => ({
  labs: [...new Set(records.value.map((item) => item.lab).filter(Boolean))],
  devices: [...new Set(records.value.map((item) => item.device).filter(Boolean))],
  types: [...new Set(records.value.map((item) => item.maintenanceType).filter(Boolean))],
  maintainers: [...new Set(records.value.map((item) => item.maintainer).filter(Boolean))],
}))

const filteredRecords = computed(() => filterMaintenanceRecords(records.value, filters))

const { currentPage, pageSize, total, pagedRecords, handleCurrentPageChange, handlePageSizeChange } = useTablePagination(filteredRecords)

const loadRecords = async () => {
  const token = ++requestToken
  loading.value = true
  loadError.value = ''
  try {
    const rows = await listMaintenanceRecords()
    if (token === requestToken) records.value = rows
  } catch (error) {
    if (token === requestToken) loadError.value = '维护记录加载失败，请重新加载'
  } finally {
    if (token === requestToken) loading.value = false
  }
}

const resetFilters = () => {
  Object.assign(filters, { keyword: '', lab: '', device: '', maintenanceType: '', maintainer: '', dateRange: [] })
}

const showError = (error, fallback) => {
  if (!error?.__handled) ElMessage.error(error?.userMessage || error?.message || fallback)
}
const recordId = record => String(record.id || record.recordNo)
const openDetail = async record => {
  if (actionId.value) return
  actionId.value = recordId(record)
  try {
    selectedRecord.value = await getMaintenanceRecord(actionId.value)
    detailVisible.value = true
  } catch (error) { showError(error, '维护记录详情加载失败') }
  finally { actionId.value = '' }
}
const openCreate = () => {
  if (!canWrite.value) return
  editorRecord.value = {}
  editorKey.value++
  editorVisible.value = true
}
const editRecord = async record => {
  if (!canWrite.value || actionId.value) return
  actionId.value = recordId(record)
  try {
    editorRecord.value = await getMaintenanceRecord(actionId.value)
    detailVisible.value = false
    editorKey.value++
    editorVisible.value = true
  } catch (error) { showError(error, '维护记录加载失败') }
  finally { actionId.value = '' }
}
const closeEditor = async done => {
  if (formSaving.value) return
  if (editorRef.value?.isDirty()) {
    try {
      await ElMessageBox.confirm('尚有未保存的修改，确认放弃吗？', '放弃修改', { confirmButtonText: '放弃修改', cancelButtonText: '继续填写', type: 'warning' })
    } catch { return }
  }
  if (typeof done === 'function') done()
  else editorVisible.value = false
}
const recordSaved = async saved => {
  const index = records.value.findIndex(record => recordId(record) === recordId(saved))
  if (index === -1) records.value.unshift(saved)
  else records.value.splice(index, 1, saved)
  formSaving.value = false
  editorVisible.value = false
  await loadRecords()
}
const removeRecord = async record => {
  if (!canWrite.value || actionId.value) return
  try {
    await ElMessageBox.confirm(`确认删除维护记录 ${record.recordNo}？删除后将从列表移除，历史信息仍会保留。`, '删除维护记录', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  actionId.value = recordId(record)
  try {
    await deleteMaintenanceRecord(actionId.value)
    records.value = records.value.filter(item => recordId(item) !== actionId.value)
    if (selectedRecord.value && recordId(selectedRecord.value) === actionId.value) detailVisible.value = false
    ElMessage.success('维护记录已删除')
    await loadRecords()
  } catch (error) { showError(error, '维护记录删除失败') }
  finally { actionId.value = '' }
}
const openLegacyEntry = async () => {
  const edit = route.query.edit
  const create = route.query.create
  if (!edit && !create) return
  if (canWrite.value) {
    if (edit) await editRecord({ id: String(edit) })
    else openCreate()
  } else ElMessage.warning('当前账号只有查看权限')
  const query = { ...route.query }
  delete query.edit
  delete query.create
  await router.replace({ path: '/lab-admin/maintenance-records', query })
}

const exportRecords = () => {
  if (!filteredRecords.value.length) {
    ElMessage.warning('暂无可导出的维护记录')
    return
  }

  const count = exportTableToExcel({
    filename: '维护记录',
    rows: filteredRecords.value.map(toMaintenanceRecordExportRow),
    columns: [
      { label: '记录编号', prop: 'recordNo' },
      { label: '实验室', prop: 'lab' },
      { label: '关联设备', prop: 'device' },
      { label: '维护类型', prop: 'maintenanceType' },
      { label: '维护日期', prop: 'maintenanceDate' },
      { label: '维护人员', prop: 'maintainer' },
      { label: '维护内容', prop: 'content' },
      { label: '维护结果', prop: 'result' },
      { label: '备注', prop: 'remark' },
    ],
  })
  ElMessage.success(`已导出 ${count} 条维护记录`)
}

watch(filteredRecords, () => { currentPage.value = 1 })
watch(() => [route.query.edit, route.query.create], openLegacyEntry)
onMounted(async () => { await loadRecords(); await openLegacyEntry() })
</script>

<template>
  <section class="list-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">实验室维护管理</p>
        <h1>维护记录管理</h1>
        <p>记录实验室的日常巡检、环境清洁、设施保养与安全检查，设备可按需关联。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadRecords">刷新</el-button>
        <el-button :icon="Download" :disabled="!filteredRecords.length || loading" @click="exportRecords">导出筛选结果</el-button>
        <el-button v-if="canWrite" type="primary" :icon="Plus" :disabled="loading" @click="openCreate">新增维护记录</el-button>
      </div>
    </header>

    <section class="page-panel">
      <MaintenanceRecordFilters :filters="filters" :options="filterOptions" @reset="resetFilters" />
    </section>

    <section class="page-panel table-panel">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon><el-button link type="primary" @click="loadRecords">重新加载</el-button></el-alert>
      <div class="section-heading">
        <h2>维护记录</h2>
        <span>共 {{ total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="pagedRecords" empty-text="暂无维护记录">
        <el-table-column prop="recordNo" label="记录编号" min-width="175" />
        <el-table-column prop="lab" label="实验室" min-width="130" />
        <el-table-column prop="device" label="关联设备" min-width="150" />
        <el-table-column prop="maintenanceType" label="维护类型" min-width="120" />
        <el-table-column prop="maintenanceDate" label="维护日期" min-width="130" />
        <el-table-column prop="maintainer" label="维护人员" min-width="115" />
        <el-table-column label="操作" :width="canWrite ? 230 : 95" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" :disabled="Boolean(actionId)" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canWrite" link type="primary" :icon="EditPen" :disabled="Boolean(actionId)" @click="editRecord(row)">编辑</el-button>
            <el-button v-if="canWrite" link type="danger" :icon="Delete" :loading="actionId === recordId(row)" :disabled="Boolean(actionId) && actionId !== recordId(row)" @click="removeRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <TablePagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @page-change="handleCurrentPageChange"
        @page-size-change="handlePageSizeChange"
      />
    </section>

    <MaintenanceRecordDetail v-model="detailVisible" :record="selectedRecord" :editable="canWrite" @edit="editRecord" />
    <el-dialog v-model="editorVisible" :title="editorRecord.id || editorRecord.recordNo ? '编辑维护记录' : '新增维护记录'" width="min(780px, calc(100vw - 32px))" align-center destroy-on-close :close-on-click-modal="false" :close-on-press-escape="!formSaving" :show-close="!formSaving" :before-close="closeEditor">
      <MaintenanceRecordForm :key="editorKey" ref="editorRef" :record="editorRecord" @saved="recordSaved" @cancel="closeEditor" @saving-change="formSaving = $event" />
    </el-dialog>
  </section>
</template>

<style scoped>
.list-page { display: grid; gap: 16px; padding-bottom: 20px; }
.header-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.header-actions .el-button { margin-left: 0; }
.page-header, .page-panel { border: 1px solid rgba(148, 163, 184, .28); border-radius: 8px; background: var(--surface-color); }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 24px 28px; background: linear-gradient(135deg, rgba(15, 118, 110, .12), rgba(37, 99, 235, .06) 48%, var(--surface-color)); }
.page-header h1, .section-heading h2 { margin: 0; color: var(--text-primary); }
.page-header h1 { font-size: 26px; }
.page-header p:not(.eyebrow) { margin: 8px 0 0; color: var(--text-secondary); }
.eyebrow { margin: 0 0 8px; color: #0f766e; font-size: 13px; font-weight: 700; }
.page-panel { padding: 18px 20px; }
.table-panel { overflow: hidden; padding: 18px 0 0; }
.table-panel > :not(.table-pagination) { margin-right: 20px; margin-left: 20px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-heading span { color: var(--text-secondary); font-size: 14px; }
@media (max-width: 680px) { .page-header { align-items: flex-start; flex-direction: column; padding: 18px; } }
</style>
