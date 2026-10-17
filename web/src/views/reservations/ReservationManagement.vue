<!--
  Web 管理平台的实验室预约视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useReservationManagement、../../composables/useTablePagination。
-->
<script setup>
import { useReservationManagement } from '../../composables/useReservationManagement'
import { useTablePagination } from '../../composables/useTablePagination'
import ReservationEditorDialog from './components/ReservationEditorDialog.vue'
import ReservationFilterBar from './components/ReservationFilterBar.vue'
import ReservationListTable from './components/ReservationListTable.vue'
import ReservationManagementHeader from './components/ReservationManagementHeader.vue'

defineOptions({
  name: 'ReservationManagement',
})

const {
  approveReservation,
  cancelReservation,
  conflictOnly,
  deleteReservation,
  detectConflicts,
  dialogTitle,
  dialogVisible,
  exportReservations,
  filteredReservations,
  formAvailability,
  formModel,
  formSubmitting,
  keyword,
  labFilter,
  labMetaMap,
  labOptions,
  openCreateDialog,
  openEditDialog,
  rejectReservation,
  sceneFilter,
  sceneOptions,
  statusFilter,
  statusOptions,
  statusSummary,
  submitReservation,
} = useReservationManagement()

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedReservations,
} = useTablePagination(filteredReservations)
</script>

<template>
  <section class="reservation-management-page">
    <ReservationManagementHeader
      :status-summary="statusSummary"
      @create="openCreateDialog"
      @detect="detectConflicts"
      @export="exportReservations"
    />

    <ReservationFilterBar
      v-model:conflict-only="conflictOnly"
      v-model:keyword="keyword"
      v-model:lab-filter="labFilter"
      v-model:scene-filter="sceneFilter"
      v-model:status-filter="statusFilter"
      :lab-meta-map="labMetaMap"
      :lab-options="labOptions"
      :result-count="filteredReservations.length"
      :scene-options="sceneOptions"
      :status-options="statusOptions"
    />

    <div class="reservation-workbench">
      <el-card class="reservation-list-card" shadow="never">
        <template #header>
          <div class="section-header">
            <div>
              <h2>预约审核列表</h2>
            </div>
          </div>
        </template>
        <ReservationListTable
          :reservations="pagedReservations"
          :row-offset="(currentPage - 1) * pageSize"
          @approve="approveReservation"
          @cancel="cancelReservation"
          @delete="deleteReservation"
          @edit="openEditDialog"
          @reject="rejectReservation"
        />
        <div class="table-pagination">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="filteredReservations.length"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleCurrentPageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-card>
    </div>

    <ReservationEditorDialog
      v-model="dialogVisible"
      :availability="formAvailability"
      :form-model="formModel"
      :lab-meta-map="labMetaMap"
      :lab-options="labOptions"
      :loading="formSubmitting"
      :scene-options="sceneOptions"
      :title="dialogTitle"
      @submit="submitReservation"
    />
  </section>
</template>

<style scoped>
.reservation-management-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.reservation-workbench {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: start;
}

.reservation-list-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.reservation-list-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border-light);
  background: #fbfdff;
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
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--border-light);
  background: #fbfdff;
}

@media (max-width: 760px) {
  .reservation-management-page {
    gap: 12px;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
