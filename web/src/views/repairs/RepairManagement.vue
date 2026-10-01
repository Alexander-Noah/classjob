<script setup>
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import { useRepairManagement } from '../../composables/useRepairManagement'
import { useTablePagination } from '../../composables/useTablePagination'
import RepairDetailPanel from './components/RepairDetailPanel.vue'
import RepairEditorDialog from './components/RepairEditorDialog.vue'
import RepairFilterBar from './components/RepairFilterBar.vue'
import RepairListTable from './components/RepairListTable.vue'
import RepairManagementHeader from './components/RepairManagementHeader.vue'

defineOptions({
  name: 'RepairManagement',
})

const {
  assignRepair,
  assigneeFilter,
  assigneeOptions,
  closeRepair,
  completeRepair,
  deleteRepair,
  deviceOptions,
  dialogTitle,
  dialogVisible,
  escalateRepair,
  exportRepairs,
  faultTypeFilter,
  faultTypeOptions,
  filteredRepairs,
  formModel,
  formSubmitting,
  keyword,
  labOptions,
  metrics,
  openCreateDialog,
  openEditDialog,
  priorityFilter,
  priorityOptions,
  selectRepair,
  selectedRepair,
  selectedRepairId,
  statusFilter,
  statusOptions,
  statusSummary,
  submitAcceptance,
  submitRepair,
  urgentOnly,
} = useRepairManagement()

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedRepairs,
} = useTablePagination(filteredRepairs)
</script>

<template>
  <section class="repair-management-page">
    <RepairManagementHeader
      :status-summary="statusSummary"
      @create="openCreateDialog"
      @export="exportRepairs"
    />

    <MetricGrid :metrics="metrics" />

    <RepairFilterBar
      v-model:assignee-filter="assigneeFilter"
      v-model:fault-type-filter="faultTypeFilter"
      v-model:keyword="keyword"
      v-model:priority-filter="priorityFilter"
      v-model:status-filter="statusFilter"
      v-model:urgent-only="urgentOnly"
      :assignee-options="assigneeOptions"
      :fault-type-options="faultTypeOptions"
      :priority-options="priorityOptions"
      :result-count="filteredRepairs.length"
      :status-options="statusOptions"
    />

    <div class="repair-workbench">
      <el-card class="repair-list-card" shadow="never">
        <template #header>
          <div class="section-header">
            <div>
              <h2>报修工单列表</h2>
            </div>
          </div>
        </template>
        <RepairListTable
          :repairs="pagedRepairs"
          :selected-id="selectedRepairId"
          @assign="assignRepair"
          @complete="completeRepair"
          @edit="openEditDialog"
          @select="selectRepair"
          @submit-acceptance="submitAcceptance"
        />
        <TablePagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredRepairs.length"
          @page-change="handleCurrentPageChange"
          @page-size-change="handlePageSizeChange"
        />
      </el-card>

      <RepairDetailPanel
        :repair="selectedRepair"
        @assign="assignRepair"
        @close="closeRepair"
        @complete="completeRepair"
        @delete="deleteRepair"
        @edit="openEditDialog"
        @escalate="escalateRepair"
        @submit-acceptance="submitAcceptance"
      />
    </div>

    <RepairEditorDialog
      v-model="dialogVisible"
      :device-options="deviceOptions"
      :fault-type-options="faultTypeOptions"
      :form-model="formModel"
      :lab-options="labOptions"
      :loading="formSubmitting"
      :priority-options="priorityOptions"
      :title="dialogTitle"
      @submit="submitRepair"
    />
  </section>
</template>

<style scoped>
.repair-management-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.repair-management-page :deep(.metric-grid) {
  gap: 14px;
}

.repair-management-page :deep(.metric-card) {
  min-height: 108px;
  border-color: #dbe7f5;
  border-radius: 8px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.repair-management-page :deep(.metric-card .el-card__body) {
  padding: 16px;
}

.repair-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 410px);
  gap: 16px;
  align-items: start;
}

.repair-list-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.repair-list-card :deep(.el-card__header) {
  min-height: 68px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
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

@media (max-width: 1200px) {
  .repair-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .repair-management-page {
    gap: 12px;
  }
}
</style>
