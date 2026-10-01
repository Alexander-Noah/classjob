<script setup>
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import { useDeviceManagement } from '../../composables/useDeviceManagement'
import DeviceDetailPanel from './components/DeviceDetailPanel.vue'
import DeviceEditorDialog from './components/DeviceEditorDialog.vue'
import DeviceFilterBar from './components/DeviceFilterBar.vue'
import DeviceInventoryDialog from './components/DeviceInventoryDialog.vue'
import DeviceListTable from './components/DeviceListTable.vue'
import DeviceManagementHeader from './components/DeviceManagementHeader.vue'
import DeviceTransferDialog from './components/DeviceTransferDialog.vue'

defineOptions({
  name: 'DeviceManagement',
})

const {
  batchDeleteDevices,
  categoryFilter,
  categoryOptions,
  currentPage,
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
} = useDeviceManagement()
</script>

<template>
  <section class="device-management-page">
    <DeviceManagementHeader
      :import-loading="importingDevices"
      @create="openCreateDialog"
      @download-template="downloadDeviceImportTemplate"
      @export="exportDevices"
      @import-file="importDevicesFromFile"
    />

    <MetricGrid :metrics="metrics" />

    <DeviceFilterBar
      v-model:category-filter="categoryFilter"
      v-model:keyword="keyword"
      v-model:lab-filter="labFilter"
      v-model:status-filter="statusFilter"
      :category-options="categoryOptions"
      :lab-options="labOptions"
      :result-count="total"
      :status-options="statusOptions"
      @reset="resetFilters"
      @search="search"
    />

    <el-card class="device-list-card" shadow="never">
      <template #header>
        <div class="section-header">
          <div>
            <h2>设备资产台账</h2>
          </div>
          <el-tag effect="light" type="primary">共 {{ total }} 条</el-tag>
        </div>
      </template>

      <div class="batch-action-bar">
        <span>已选择 {{ selectedDevices.length }} 条资产</span>
        <div>
          <el-button :disabled="!selectedDevices.length" @click="openBatchInventoryDialog">批量盘点</el-button>
          <el-button :disabled="!selectedDevices.length" @click="openBatchTransferDialog">批量调拨</el-button>
          <el-button :disabled="!selectedDevices.length" type="danger" plain @click="batchDeleteDevices">批量删除</el-button>
        </div>
      </div>

      <DeviceListTable
        v-loading="loading"
        :devices="devices"
        @delete="deleteDevice"
        @edit="openEditDialog"
        @inventory="openInventoryDialog"
        @selection-change="handleSelectionChange"
        @transfer="openTransferDialog"
        @view="openDetailDialog"
      />

      <TablePagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      />
    </el-card>

    <DeviceEditorDialog
      v-model="dialogVisible"
      :category-options="categoryOptions"
      :form-model="formModel"
      :lab-options="labOptions"
      :loading="formSubmitting"
      :status-options="statusOptions"
      :title="dialogTitle"
      @submit="submitDevice"
    />

    <DeviceInventoryDialog
      v-model="inventoryDialogVisible"
      :device="inventoryDevice"
      :devices="inventoryDevices"
      :form-model="inventoryFormModel"
      :loading="inventorySubmitting"
      :result-options="inventoryResultOptions"
      @submit="submitInventory"
    />

    <DeviceTransferDialog
      v-model="transferDialogVisible"
      :devices="transferDevices"
      :form-model="transferFormModel"
      :lab-options="labOptions"
      :loading="transferSubmitting"
      :owner-options="ownerOptions"
      @submit="submitTransfer"
    />

    <DeviceDetailPanel v-model="detailVisible" :device="selectedDevice" />
  </section>
</template>

<style scoped>
.device-management-page {
  display: grid;
  gap: 16px;
  min-height: 100%;
  padding-bottom: 20px;
}

.device-management-page :deep(.metric-grid) {
  gap: 12px;
}

.device-management-page :deep(.metric-card) {
  min-height: 104px;
  border-color: rgba(148, 163, 184, 0.32);
}

.device-management-page :deep(.metric-card strong) {
  font-size: 26px;
  font-weight: 800;
}

.device-list-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
}

.device-list-card :deep(.el-card__header) {
  min-height: 56px;
  padding: 15px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.86);
  background: #fff;
}

.device-list-card :deep(.el-card__body) {
  overflow-x: auto;
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

.batch-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: #f8fafc;
}

.batch-action-bar span {
  color: var(--text-secondary);
  font-size: 13px;
}

.batch-action-bar > div {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 720px) {
  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .device-list-card :deep(.el-card__body) {
    padding: 14px;
  }

  .batch-action-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
