<!--
  Web 管理平台的业务记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useBusinessModuleRecords。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DocumentChecked } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH, DEFAULT_AUTH_ROLE } from '../../constants/auth'
import { useBusinessModuleRecords } from '../../composables/useBusinessModuleRecords'
import PageHero from '../common/PageHero.vue'
import MetricGrid from '../common/MetricGrid.vue'
import BusinessRecordDialog from './BusinessRecordDialog.vue'
import BusinessRecordDrawer from './BusinessRecordDrawer.vue'
import BusinessRecordTable from './BusinessRecordTable.vue'
import BusinessStatusSummary from './BusinessStatusSummary.vue'

defineOptions({
  name: 'BusinessManagementPage',
})

const props = defineProps({
  moduleConfig: {
    type: Object,
    required: true,
  },
})

const route = useRoute()
const router = useRouter()
const moduleConfig = computed(() => props.moduleConfig)
const pageTitle = computed(() => moduleConfig.value.pageTitle || route.meta?.title || '业务管理')
const pageIcon = computed(() => moduleConfig.value.pageIcon || route.meta?.module?.icon || DocumentChecked)
const metrics = computed(() => moduleConfig.value.metrics || [])
const featureCards = computed(() => moduleConfig.value.featureCards || [])

const business = useBusinessModuleRecords({
  route,
  moduleConfig,
  pageTitle,
})

const handleBackToDashboard = () => {
  const roleScope = route.meta?.roleScope || DEFAULT_AUTH_ROLE
  router.push(AUTH_ROLE_DEFAULT_PATH[roleScope] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE])
}
</script>

<template>
  <section class="module-page">
    <PageHero
      :actions="business.actions.value"
      :badge="moduleConfig.badge"
      :page-icon="pageIcon"
      :page-title="pageTitle"
      @action="business.handleModuleAction"
      @back="handleBackToDashboard"
    />

    <MetricGrid :metrics="metrics" />

    <div v-if="featureCards.length" class="feature-grid">
      <article v-for="feature in featureCards" :key="feature.title" class="feature-card">
        <div class="feature-card-heading">
          <strong>{{ feature.title }}</strong>
          <el-tag v-if="feature.tag" :type="feature.type || 'primary'" effect="light" size="small">
            {{ feature.tag }}
          </el-tag>
        </div>
      </article>
    </div>

    <el-row class="module-content-row" :gutter="18">
      <el-col :xs="24" :xl="17">
        <BusinessRecordTable
          v-model:keyword="business.keyword.value"
          v-model:status-filter="business.statusFilter.value"
          :batch-target-status="business.batchTargetStatus.value"
          :current-page="business.currentPage.value"
          :filtered-record-count="business.filteredRecordCount.value"
          :filtered-records="business.pagedRecords.value"
          :get-row-tag-type="business.getRowTagType"
          :page-size="business.pageSize.value"
          :record-name="business.recordName.value"
          :selected-count="business.selectedRows.value.length"
          :status-options="business.statusOptions.value"
          :table-columns="business.tableColumns.value"
          :table-title="moduleConfig.tableTitle || '业务台账'"
          :total-record-count="business.totalRecordCount.value"
          @batch-status="business.applyBatchStatus()"
          @create="business.openCreateDialog"
          @delete="business.deleteRecord"
          @delete-selected="business.deleteSelectedRecords"
          @edit="business.openEditDialog"
          @export="business.exportRecords"
          @page-size-change="business.handlePageSizeChange"
          @page-change="business.handleCurrentPageChange"
          @row-click="business.viewRecordDetail"
          @selection-change="business.handleSelectionChange"
        />
      </el-col>

      <el-col :xs="24" :xl="7">
        <div class="side-stack">
          <BusinessStatusSummary
            v-model:status-filter="business.statusFilter.value"
            :status-summary="business.statusSummary.value"
          />
        </div>
      </el-col>
    </el-row>

    <BusinessRecordDialog
      v-model="business.dialogVisible.value"
      :dialog-title="business.dialogTitle.value"
      :editable-columns="business.editableColumns.value"
      :field-options="business.formFieldOptions.value"
      :form-model="business.formModel"
      :form-submitting="business.formSubmitting.value"
      :status-options="business.statusOptions.value"
      :status-prop="business.statusProp.value"
      @submit="business.submitRecord"
    />

    <BusinessRecordDrawer
      v-model="business.detailVisible.value"
      :active-record="business.activeRecord.value"
      :get-row-tag-type="business.getRowTagType"
      :record-name="business.recordName.value"
      :table-columns="business.tableColumns.value"
    />
  </section>
</template>

<style scoped>
.module-page {
  display: grid;
  gap: 18px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
  gap: 14px;
}

.feature-card {
  min-height: 72px;
  padding: 16px 18px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.feature-card-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feature-card-heading strong {
  color: var(--text-primary);
  font-size: 15px;
}

.module-content-row :deep(.el-col) {
  display: flex;
  margin-bottom: 16px;
}

.side-stack {
  display: grid;
  width: 100%;
  gap: 16px;
  align-content: start;
}
</style>
