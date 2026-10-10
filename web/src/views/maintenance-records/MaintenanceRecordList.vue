<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Download, EditPen, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { listMaintenanceRecords } from '../../api/services/maintenanceRecords'
import TablePagination from '../../components/common/TablePagination.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import { exportTableToExcel } from '../../utils/exportExcel'
import MaintenanceRecordDetail from './components/MaintenanceRecordDetail.vue'
import MaintenanceRecordFilters from './components/MaintenanceRecordFilters.vue'

defineOptions({ name: 'MaintenanceRecordList' })

const router = useRouter()
const loading = ref(false)
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

const normalizeText = (value) => String(value || '').toLowerCase()
const filteredRecords = computed(() => {
  const keyword = normalizeText(filters.keyword).trim()
  const [fromDate, toDate] = filters.dateRange || []

  return records.value.filter((record) => {
    const keywordMatched = !keyword || [record.recordNo, record.lab, record.device, record.maintenanceType, record.maintainer, record.content, record.result]
      .some((value) => normalizeText(value).includes(keyword))
    const exactMatched = ['lab', 'device', 'maintenanceType', 'maintainer'].every(
      (field) => !filters[field] || record[field] === filters[field],
    )
    const date = String(record.maintenanceDate || '').slice(0, 10)
    const dateMatched = (!fromDate || date >= fromDate) && (!toDate || date <= toDate)
    return keywordMatched && exactMatched && dateMatched
  })
})

const { currentPage, pageSize, total, pagedRecords, handleCurrentPageChange, handlePageSizeChange } = useTablePagination(filteredRecords)

const loadRecords = async () => {
  loading.value = true
  try {
    records.value = await listMaintenanceRecords()
  } catch (error) {
    ElMessage.error(error?.message || '维护记录加载失败')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  Object.assign(filters, { keyword: '', lab: '', device: '', maintenanceType: '', maintainer: '', dateRange: [] })
}

const openDetail = (record) => {
  selectedRecord.value = record
  detailVisible.value = true
}

const editRecord = (record) => {
  router.push(`/lab-admin/maintenance-records/new?id=${encodeURIComponent(record.recordNo)}`)
}

const exportRecords = () => {
  if (!filteredRecords.value.length) {
    ElMessage.warning('暂无可导出的维护记录')
    return
  }

  const count = exportTableToExcel({
    filename: '维护记录',
    rows: filteredRecords.value,
    columns: [
      { label: '记录编号', prop: 'recordNo' },
      { label: '实验室', prop: 'lab' },
      { label: '设备', prop: 'device' },
      { label: '维护类型', prop: 'maintenanceType' },
      { label: '维护日期', prop: 'maintenanceDate' },
      { label: '维护人员', prop: 'maintainer' },
      { label: '维护内容', prop: 'content' },
      { label: '处理结果', prop: 'result' },
      { label: '备注', prop: 'remark' },
    ],
  })
  ElMessage.success(`已导出 ${count} 条维护记录`)
}

watch(filteredRecords, () => { currentPage.value = 1 })
onMounted(loadRecords)
</script>

<template>
  <section class="list-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">设备维护管理</p>
        <h1>维护记录查看</h1>
        <p>查询、筛选和导出维护记录；需要变更时进入独立填写页面。</p>
      </div>
      <el-button :icon="Download" @click="exportRecords">导出筛选结果</el-button>
    </header>

    <section class="page-panel">
      <MaintenanceRecordFilters :filters="filters" :options="filterOptions" @reset="resetFilters" />
    </section>

    <section class="page-panel table-panel">
      <div class="section-heading">
        <h2>维护记录</h2>
        <span>共 {{ total }} 条</span>
      </div>
      <el-table v-loading="loading" :data="pagedRecords" empty-text="暂无维护记录">
        <el-table-column prop="recordNo" label="记录编号" min-width="175" />
        <el-table-column prop="lab" label="实验室" min-width="130" />
        <el-table-column prop="device" label="设备" min-width="150" />
        <el-table-column prop="maintenanceType" label="维护类型" min-width="120" />
        <el-table-column prop="maintenanceDate" label="维护日期" min-width="130" />
        <el-table-column prop="maintainer" label="维护人员" min-width="115" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" :icon="EditPen" @click="editRecord(row)">编辑</el-button>
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

    <MaintenanceRecordDetail v-model="detailVisible" :record="selectedRecord" @edit="editRecord" />
  </section>
</template>

<style scoped>
.list-page { display: grid; gap: 16px; padding-bottom: 20px; }
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
