<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TablePagination from '../../components/common/TablePagination.vue'
import { getLabDevices } from '../../api/services/lab'
import { reservationResourceService } from '../../api/services/basicResources'
import {
  getClassTimetableCrawlerConfig,
  getClassTimetableSemesters,
  listClassTimetables,
} from '../../api/services/classTimetable'
import { useLabManagement } from '../../composables/useLabManagement'
import { useTablePagination } from '../../composables/useTablePagination'
import { calculateAcademicWeek } from '../../utils/academicWeek'

import LabAssetPreviewPanel from './components/LabAssetPreviewPanel.vue'
import LabDetailPanel from './components/LabDetailPanel.vue'
import LabEditorDialog from './components/LabEditorDialog.vue'
import LabFilterBar from './components/LabFilterBar.vue'
import LabListTable from './components/LabListTable.vue'
import LabManagementHeader from './components/LabManagementHeader.vue'
import LabReservationQrDialog from './components/LabReservationQrDialog.vue'

defineOptions({
  name: 'LabManagement',
})

const labAssets = ref([])
const assetDialogVisible = ref(false)
const assetDialogLab = ref(null)
const qrDialogVisible = ref(false)
const qrLab = ref(null)
const scheduleRows = ref([])
const reservationRows = ref([])
const semesters = ref([])
const scheduleLoading = ref(false)
const semesterStartDate = ref('')
const now = ref(new Date())
let clockTimer = null
const router = useRouter()
const configuredPublicAppUrl = String(import.meta.env.VITE_PUBLIC_APP_URL || '').trim().replace(/\/+$/, '')

const sectionTimeRanges = [
  { value: 1, start: 8 * 60 + 30, end: 10 * 60 },
  { value: 2, start: 10 * 60 + 20, end: 11 * 60 + 50 },
  { value: 3, start: 14 * 60, end: 15 * 60 + 30 },
  { value: 4, start: 15 * 60 + 50, end: 17 * 60 + 20 },
  { value: 5, start: 18 * 60 + 40, end: 20 * 60 + 10 },
  { value: 6, start: 20 * 60 + 20, end: 21 * 60 + 50 },
]

const getCurrentAcademicWeek = (date = now.value) => {
  return calculateAcademicWeek(date, semesterStartDate.value)
}

const currentWeekday = (date = now.value) => date.getDay() || 7
const todayText = computed(() => {
  const date = now.value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
})
const currentMinute = computed(() => now.value.getHours() * 60 + now.value.getMinutes())
const activeTimeUnit = computed(() => {
  const minute = currentMinute.value
  return sectionTimeRanges.find((item) => minute >= item.start && minute < item.end)?.value || 0
})
const isCurrentScheduleDay = computed(
  () => Number(scheduleFilters.week) === getCurrentAcademicWeek() && Number(scheduleFilters.weekday) === currentWeekday(),
)

const scheduleFilters = reactive({
  semester: '',
  week: getCurrentAcademicWeek(),
  weekday: currentWeekday(),
  section: '',
})

const {
  availableOnly,
  bindLabSchedules,
  buildingFilter,
  buildingOptions,
  changeLabStatus,
  deleteLab,
  dialogTitle,
  dialogVisible,
  exportLabs,
  filteredLabs,
  formModel,
  formSubmitting,
  keyword,
  managerOptions,
  openCreateDialog,
  openEditDialog,
  selectLab,
  selectedLab,
  selectedLabId,
  safetyOptions,
  statusFilter,
  statusOptions,
  statusSummary,
  submitLab,
  typeFilter,
  typeOptions,
} = useLabManagement()

const publicAppOrigin = computed(() => configuredPublicAppUrl || window.location.origin)
const reservationQrLink = computed(() => {
  if (!qrLab.value?.id) return ''
  const href = router.resolve({
    path: '/scan/reserve',
    query: {
      labId: qrLab.value.id,
      code: qrLab.value.code,
    },
  }).href
  return `${publicAppOrigin.value}${href}`
})
const scheduleContext = computed(() => ({
  activeTimeUnit: activeTimeUnit.value,
  currentMinute: currentMinute.value,
  isToday: isCurrentScheduleDay.value,
  date: todayText.value,
  week: scheduleFilters.week,
  weekday: scheduleFilters.weekday,
  section: scheduleFilters.section,
}))
const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedLabs,
} = useTablePagination(filteredLabs)

/**
 * 把后端设备字段转换成 LabAssetPreviewPanel 能识别的字段
 * 后端可能返回 deviceCode、deviceName、ownerUsername
 * 组件需要 code、name、owner
 */
const normalizeAsset = (item) => {
  return {
    ...item,
    code: item.code ?? item.deviceCode ?? item.assetNo ?? '',
    name: item.name ?? item.deviceName ?? '',
    owner: item.owner ?? item.ownerName ?? item.ownerUsername ?? '',
    inventoryDate: item.inventoryDate ?? item.inventory_date ?? '',
    online: item.online === true || item.online === 1,
  }
}

/**
 * 根据当前实验室 ID 请求设备数据
 */
const loadLabAssets = async (lab) => {
  if (!lab || !lab.id) {
    labAssets.value = []
    return
  }

  try {
    const data = await getLabDevices(lab.id)

    labAssets.value = Array.isArray(data)
        ? data.map(normalizeAsset)
        : []
  } catch (error) {
    console.error('加载实验室设备信息失败：', error)
    labAssets.value = []
  }
}

/**
 * 点击实验室行时触发
 */
const handleSelectLab = async (lab) => {
  selectLab(lab)
  await loadLabAssets(lab)
}

/**
 * 点击“设备信息查看”按钮时触发
 */
const handleViewLabAssets = async (lab) => {
  if (!lab) return

  selectLab(lab)
  assetDialogLab.value = lab
  assetDialogVisible.value = true
  await loadLabAssets(lab)
}

const handleOpenReservationQr = (lab) => {
  if (!lab) return
  qrLab.value = lab
  qrDialogVisible.value = true
}

const loadSemesters = async () => {
  try {
    semesters.value = await getClassTimetableSemesters()
    if (!scheduleFilters.semester && semesters.value.length) {
      scheduleFilters.semester = semesters.value[0]
    }
  } catch {
    semesters.value = []
  }
}

const loadScheduleConfiguration = async () => {
  const config = (await getClassTimetableCrawlerConfig()) || {}
  semesterStartDate.value = String(config.semesterStartDate || '').trim()
  scheduleFilters.week = getCurrentAcademicWeek()
}

const loadScheduleRows = async () => {
  scheduleLoading.value = true
  try {
    const [nextScheduleRows, nextReservationRows] = await Promise.all([
      listClassTimetables({
        semester: scheduleFilters.semester || undefined,
        week: scheduleFilters.week || undefined,
        limit: 5000,
      }),
      reservationResourceService.list({ fallbackRecords: [] }),
    ])
    scheduleRows.value = nextScheduleRows
    reservationRows.value = Array.isArray(nextReservationRows) ? nextReservationRows : []
    bindLabSchedules(scheduleRows.value, scheduleContext.value, reservationRows.value)
  } finally {
    scheduleLoading.value = false
  }
}

watch(
  () => [
    scheduleFilters.week,
    scheduleFilters.weekday,
    scheduleFilters.section,
    selectedLabId.value,
    activeTimeUnit.value,
    currentMinute.value,
  ],
  () => bindLabSchedules(scheduleRows.value, scheduleContext.value, reservationRows.value),
)

onMounted(async () => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 60000)
  await loadScheduleConfiguration()
  await loadSemesters()
  await loadScheduleRows()
})

onUnmounted(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
})
</script>

<template>
  <section class="lab-management-page">
    <LabManagementHeader
        :status-summary="statusSummary"
        @create="openCreateDialog"
        @export="exportLabs"
    />

    <LabFilterBar
        v-model:available-only="availableOnly"
        v-model:building-filter="buildingFilter"
        v-model:keyword="keyword"
        v-model:status-filter="statusFilter"
        v-model:type-filter="typeFilter"
        :building-options="buildingOptions"
        :result-count="filteredLabs.length"
        :status-options="statusOptions"
        :type-options="typeOptions"
    />

    <div class="lab-workbench">
      <el-card class="lab-list-card" shadow="never">
        <template #header>
          <div class="section-header">
            <div>
              <h2>实验室清单</h2>
            </div>
          </div>
        </template>

        <LabListTable
            :labs="pagedLabs"
            :selected-id="selectedLabId == null ? '' : String(selectedLabId)"
            @edit="openEditDialog"
            @reservation-qr="handleOpenReservationQr"
            @select="handleSelectLab"
            @view-assets="handleViewLabAssets"
        />

        <TablePagination
            :current-page="currentPage"
            :page-size="pageSize"
            :total="filteredLabs.length"
            @page-change="handleCurrentPageChange"
            @page-size-change="handlePageSizeChange"
        />
      </el-card>

      <LabDetailPanel
          :asset-record-count="labAssets.length"
          :lab="selectedLab"
          @delete="deleteLab"
          @edit="openEditDialog"
          @reservation-qr="handleOpenReservationQr"
          @status="changeLabStatus"
          @view-assets="handleViewLabAssets"
      />
    </div>

    <LabEditorDialog
        v-model="dialogVisible"
        :form-model="formModel"
        :loading="formSubmitting"
        :manager-options="managerOptions"
        :safety-options="safetyOptions"
        :status-options="statusOptions"
        :title="dialogTitle"
        :type-options="typeOptions"
        @submit="submitLab"
    />

    <LabReservationQrDialog
        v-model="qrDialogVisible"
        :lab="qrLab"
        :reservation-link="reservationQrLink"
    />

    <el-dialog
        v-model="assetDialogVisible"
        align-center
        class="lab-asset-dialog"
        destroy-on-close
        :title="assetDialogLab ? `${assetDialogLab.name} 设备信息` : '设备信息'"
        width="min(920px, calc(100vw - 32px))"
    >
      <LabAssetPreviewPanel
          :assets="labAssets"
          :lab="assetDialogLab"
          dialog-mode
      />
    </el-dialog>
  </section>
</template>

<style scoped>
.lab-management-page {
  display: grid;
  gap: 16px;
  min-height: 100%;
  padding-bottom: 20px;
}

.lab-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 360px);
  gap: 14px;
  align-items: start;
}

.lab-list-card {
  min-width: 0;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
  overflow: hidden;
}

.lab-list-card :deep(.el-card__header) {
  min-height: 66px;
  padding: 15px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.lab-list-card :deep(.el-card__body) {
  padding: 18px 20px 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
}

.lab-list-card :deep(.pagination-row),
.lab-list-card :deep(.table-pagination),
.lab-list-card :deep(.pagination-container) {
  padding-top: 12px;
}

.lab-management-page :deep(.lab-asset-dialog) {
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 12vh);
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.lab-management-page :deep(.lab-asset-dialog .el-dialog__header) {
  flex: 0 0 auto;
  padding: 16px 18px 12px;
}

.lab-management-page :deep(.lab-asset-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 0 16px 16px;
}

@media (max-width: 1180px) {
  .lab-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .lab-list-card :deep(.el-card__body) {
    padding: 14px;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
