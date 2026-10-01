<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'RepairFilterBar',
})

const props = defineProps({
  assigneeFilter: {
    type: String,
    default: 'all',
  },
  assigneeOptions: {
    type: Array,
    default: () => [],
  },
  faultTypeFilter: {
    type: String,
    default: 'all',
  },
  faultTypeOptions: {
    type: Array,
    default: () => [],
  },
  keyword: {
    type: String,
    default: '',
  },
  priorityFilter: {
    type: String,
    default: 'all',
  },
  priorityOptions: {
    type: Array,
    default: () => [],
  },
  resultCount: {
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
  urgentOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'update:assigneeFilter',
  'update:faultTypeFilter',
  'update:keyword',
  'update:priorityFilter',
  'update:statusFilter',
  'update:urgentOnly',
])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const statusModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})

const priorityModel = computed({
  get: () => props.priorityFilter,
  set: (value) => emit('update:priorityFilter', value),
})

const faultTypeModel = computed({
  get: () => props.faultTypeFilter,
  set: (value) => emit('update:faultTypeFilter', value),
})

const assigneeModel = computed({
  get: () => props.assigneeFilter,
  set: (value) => emit('update:assigneeFilter', value),
})

const urgentModel = computed({
  get: () => props.urgentOnly,
  set: (value) => emit('update:urgentOnly', value),
})
</script>

<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-bar">
      <div>
        <h2>工单筛选</h2>
      </div>
      <div class="filter-controls">
        <el-input v-model="keywordModel" class="keyword-input" clearable placeholder="搜索工单、设备、报修人">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusModel" class="filter-select" placeholder="全部状态">
          <el-option label="全部状态" value="all" />
          <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
        </el-select>
        <el-select v-model="priorityModel" class="filter-select" placeholder="全部优先级">
          <el-option label="全部优先级" value="all" />
          <el-option v-for="priority in priorityOptions" :key="priority" :label="priority" :value="priority" />
        </el-select>
        <el-select v-model="faultTypeModel" class="filter-select" placeholder="全部类型">
          <el-option label="全部类型" value="all" />
          <el-option v-for="faultType in faultTypeOptions" :key="faultType" :label="faultType" :value="faultType" />
        </el-select>
        <el-select v-model="assigneeModel" class="filter-select" placeholder="全部维修人">
          <el-option label="全部维修人" value="all" />
          <el-option v-for="assignee in assigneeOptions" :key="assignee" :label="assignee" :value="assignee" />
        </el-select>
        <el-switch v-model="urgentModel" active-text="仅高优先级" />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.filter-card {
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.filter-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.filter-bar,
.filter-controls {
  display: flex;
  align-items: center;
}

.filter-bar {
  justify-content: space-between;
  gap: 16px;
}

.filter-bar h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.filter-controls {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.filter-controls :deep(.el-input__wrapper),
.filter-controls :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.filter-controls :deep(.el-input__wrapper:hover),
.filter-controls :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.keyword-input {
  width: 280px;
}

.filter-select {
  width: 130px;
}

@media (max-width: 1100px) {
  .filter-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .filter-controls {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .filter-card :deep(.el-card__body) {
    padding: 14px;
  }

  .filter-controls {
    display: grid;
    grid-template-columns: 1fr;
  }

  .keyword-input,
  .filter-select {
    width: 100%;
  }
}
</style>
