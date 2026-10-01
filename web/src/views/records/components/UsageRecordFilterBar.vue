<script setup>
import { computed } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'UsageRecordFilterBar',
})

const props = defineProps({
  abnormalOnly: {
    type: Boolean,
    default: false,
  },
  dateRange: {
    type: Array,
    default: () => [],
  },
  keyword: {
    type: String,
    default: '',
  },
  labFilter: {
    type: String,
    default: 'all',
  },
  labOptions: {
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
  typeFilter: {
    type: String,
    default: 'all',
  },
  typeOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits([
  'reset',
  'update:abnormalOnly',
  'update:dateRange',
  'update:keyword',
  'update:labFilter',
  'update:statusFilter',
  'update:typeFilter',
])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const statusModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})

const typeModel = computed({
  get: () => props.typeFilter,
  set: (value) => emit('update:typeFilter', value),
})

const labModel = computed({
  get: () => props.labFilter,
  set: (value) => emit('update:labFilter', value),
})

const dateRangeModel = computed({
  get: () => props.dateRange,
  set: (value) => emit('update:dateRange', value || []),
})

const abnormalModel = computed({
  get: () => props.abnormalOnly,
  set: (value) => emit('update:abnormalOnly', value),
})
</script>

<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-bar">
      <div>
        <h2>记录筛选</h2>
      </div>
      <div class="filter-controls">
        <el-input v-model="keywordModel" class="keyword-input" clearable placeholder="搜索人员、资源、异常原因">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusModel" class="filter-select" placeholder="全部状态">
          <el-option label="全部状态" value="all" />
          <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
        </el-select>
        <el-select v-model="typeModel" class="filter-select" placeholder="全部类型">
          <el-option label="全部类型" value="all" />
          <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
        </el-select>
        <el-select v-model="labModel" class="lab-select" placeholder="全部实验室">
          <el-option label="全部实验室" value="all" />
          <el-option v-for="lab in labOptions" :key="lab" :label="lab" :value="lab" />
        </el-select>
        <el-date-picker
          v-model="dateRangeModel"
          class="date-range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-switch v-model="abnormalModel" active-text="仅异常/待复核" />
        <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
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
  padding: 16px 20px;
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
  color: #0f172a;
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
.filter-controls :deep(.el-select__wrapper),
.filter-controls :deep(.el-date-editor) {
  min-height: 36px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.filter-controls :deep(.el-input__wrapper:hover),
.filter-controls :deep(.el-select__wrapper:hover),
.filter-controls :deep(.el-date-editor:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.filter-controls :deep(.el-button) {
  min-height: 36px;
  border-radius: 7px;
  font-weight: 700;
}

.filter-controls :deep(.el-switch__label) {
  color: #475569;
  font-weight: 600;
}

.keyword-input {
  width: 280px;
}

.filter-select {
  width: 130px;
}

.lab-select {
  width: 190px;
}

.date-range {
  width: 260px;
}

@media (max-width: 1280px) {
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
  .keyword-input,
  .filter-select,
  .lab-select,
  .date-range {
    width: 100%;
  }

  .filter-controls :deep(.el-switch) {
    width: 100%;
  }
}
</style>
