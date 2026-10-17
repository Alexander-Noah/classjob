<!--
  Web 管理平台的业务记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Delete, Download, Edit, Finished, Plus, Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'BusinessRecordTable',
})

const props = defineProps({
  activeRecordId: {
    type: String,
    default: '',
  },
  batchTargetStatus: {
    type: String,
    default: '',
  },
  currentPage: {
    type: Number,
    default: 1,
  },
  filteredRecordCount: {
    type: Number,
    default: 0,
  },
  filteredRecords: {
    type: Array,
    default: () => [],
  },
  getRowTagType: {
    type: Function,
    required: true,
  },
  keyword: {
    type: String,
    default: '',
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  recordName: {
    type: String,
    default: '记录',
  },
  selectedCount: {
    type: Number,
    default: 0,
  },
  statusFilter: {
    type: String,
    default: 'all',
  },
  statusOptions: {
    type: Array,
    default: () => [],
  },
  tableColumns: {
    type: Array,
    default: () => [],
  },
  tableTitle: {
    type: String,
    default: '业务台账',
  },
  totalRecordCount: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits([
  'batch-status',
  'create',
  'delete',
  'delete-selected',
  'edit',
  'export',
  'page-change',
  'page-size-change',
  'row-click',
  'selection-change',
  'update:keyword',
  'update:statusFilter',
])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const statusFilterModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})

const getRowClassName = ({ row }) => (row._recordId === props.activeRecordId ? 'is-active-row' : '')
</script>

<template>
  <el-card class="module-card" shadow="never" :body-style="{ padding: '0' }">
    <template #header>
      <div class="card-header table-header">
        <div>
          <h2>{{ tableTitle }}</h2>
          <span>共 {{ totalRecordCount }} 条，筛选后 {{ filteredRecordCount }} 条，当前页 {{ filteredRecords.length }} 条</span>
        </div>

        <div class="table-tools">
          <el-select v-model="statusFilterModel" class="status-filter" placeholder="全部状态">
            <el-option label="全部状态" value="all" />
            <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
          </el-select>
          <el-input v-model="keywordModel" class="record-search" clearable placeholder="搜索记录">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
      </div>
    </template>

    <div class="batch-bar" :class="{ 'is-idle': !selectedCount }">
      <span class="batch-summary">已选择 {{ selectedCount }} 条{{ recordName }}</span>
      <div class="batch-actions">
        <el-button :icon="Plus" size="small" type="primary" @click="$emit('create')">新增</el-button>
        <template v-if="selectedCount">
          <el-button :icon="Finished" size="small" @click="$emit('batch-status')">
            标记为{{ batchTargetStatus || '已处理' }}
          </el-button>
          <el-button :icon="Delete" size="small" @click="$emit('delete-selected')">
            删除所选
          </el-button>
        </template>
        <span v-else class="selection-action-hint">勾选记录后可批量标记或删除</span>
        <el-button :icon="Download" size="small" @click="$emit('export')">导出</el-button>
      </div>
    </div>

    <el-table
      :data="filteredRecords"
      :row-class-name="getRowClassName"
      class="record-table"
      empty-text="暂无记录"
      row-key="_recordId"
      @row-click="$emit('row-click', $event)"
      @selection-change="$emit('selection-change', $event)"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column
        v-for="column in tableColumns"
        :key="column.prop"
        :label="column.label"
        :min-width="column.minWidth"
        :prop="column.prop"
      >
        <template #default="{ row }">
          <el-tag v-if="column.type === 'tag'" :type="getRowTagType(row)" effect="light">
            {{ row[column.prop] }}
          </el-tag>
          <span v-else>{{ row[column.prop] }}</span>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="156" min-width="156" align="center" header-align="center">
        <template #default="{ row }">
          <div class="row-actions" @click.stop>
            <el-button :icon="Edit" link type="primary" @click="$emit('edit', row)">编辑</el-button>
            <el-button :icon="Delete" link type="danger" @click="$emit('delete', row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="filteredRecordCount"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="$emit('page-change', $event)"
        @size-change="$emit('page-size-change', $event)"
      />
    </div>
  </el-card>
</template>

<style scoped>
.module-card {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.module-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
}

.card-header,
.table-tools,
.batch-bar,
.batch-actions {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.table-header {
  align-items: flex-start;
}

.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.card-header span,
.batch-bar span {
  color: var(--text-secondary);
  font-size: 13px;
}

.table-tools {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.status-filter {
  width: 136px;
}

.record-search {
  width: min(240px, 46vw);
}

.batch-bar {
  justify-content: space-between;
  gap: 14px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  background: #f8fbff;
}

.batch-actions {
  flex: 1 1 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px 10px;
}

.batch-actions :deep(.el-button) {
  margin-left: 0;
  white-space: nowrap;
}

.batch-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.batch-summary {
  flex: 0 0 auto;
}

.selection-action-hint {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  color: var(--text-muted);
  white-space: nowrap;
}

.record-table {
  width: 100%;
}

.record-table :deep(.el-table__row) {
  cursor: pointer;
}

.record-table :deep(.is-active-row > td.el-table__cell) {
  background: #f1f5ff;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  min-height: 24px;
  padding: 0;
  font-weight: 600;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--border-light);
}

@media (max-width: 900px) {
  .card-header,
  .batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-tools,
  .batch-actions {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .record-search,
  .status-filter {
    width: 100%;
  }

  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
