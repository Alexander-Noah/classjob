<!--
  Web 管理平台的使用记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useTablePagination、../../composables/useUsageRecords。
-->
<script setup>
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import { useUsageRecords } from '../../composables/useUsageRecords'
import UsageRecordFilterBar from './components/UsageRecordFilterBar.vue'
import UsageRecordListTable from './components/UsageRecordListTable.vue'
import UsageRecordsHeader from './components/UsageRecordsHeader.vue'

defineOptions({
  name: 'UsageRecords',
})

const {
  abnormalOnly,
  archiveRecord,
  closeUsingRecord,
  dateRange,
  exportRecords,
  filterAbnormalRecords,
  filteredRecords,
  keyword,
  labFilter,
  labOptions,
  markAbnormal,
  markReviewed,
  metrics,
  resetFilters,
  statusFilter,
  statusOptions,
  statusSummary,
  typeFilter,
  typeOptions,
} = useUsageRecords()

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedUsageRecords,
} = useTablePagination(filteredRecords)
</script>

<template>
  <section class="usage-records-page">
    <UsageRecordsHeader
      :status-summary="statusSummary"
      @export="exportRecords"
      @filter-abnormal="filterAbnormalRecords"
    />

    <MetricGrid :metrics="metrics" />

    <UsageRecordFilterBar
      v-model:abnormal-only="abnormalOnly"
      v-model:date-range="dateRange"
      v-model:keyword="keyword"
      v-model:lab-filter="labFilter"
      v-model:status-filter="statusFilter"
      v-model:type-filter="typeFilter"
      :lab-options="labOptions"
      :result-count="filteredRecords.length"
      :status-options="statusOptions"
      :type-options="typeOptions"
      @reset="resetFilters"
    />

    <div class="usage-workbench">
      <el-card class="usage-list-card" shadow="never">
        <template #header>
          <div class="section-header">
            <div>
              <h2>使用记录列表</h2>
            </div>
          </div>
        </template>
        <UsageRecordListTable
          :records="pagedUsageRecords"
          @archive="archiveRecord"
          @close-using="closeUsingRecord"
          @mark-abnormal="markAbnormal"
          @review="markReviewed"
        />
        <TablePagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="filteredRecords.length"
          @page-change="handleCurrentPageChange"
          @page-size-change="handlePageSizeChange"
        />
      </el-card>
    </div>
  </section>
</template>

<style scoped>
.usage-records-page {
  display: grid;
  gap: 16px;
}

.usage-records-page :deep(.metric-card) {
  border-color: #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.usage-records-page :deep(.metric-card:hover) {
  border-color: #bfdbfe;
  transform: translateY(-1px);
}

.usage-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.usage-list-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.usage-list-card :deep(.el-card__header) {
  min-height: 72px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.usage-list-card :deep(.el-card__body) {
  padding: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.section-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

@media (max-width: 760px) {
  .usage-records-page {
    gap: 12px;
  }

  .usage-list-card :deep(.el-card__header) {
    padding: 14px 16px;
  }
}
</style>
