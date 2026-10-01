<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { formatLabOptionLabel } from '../../../utils/labDisplay'

defineOptions({
  name: 'ReservationFilterBar',
})

const props = defineProps({
  conflictOnly: {
    type: Boolean,
    default: false,
  },
  keyword: {
    type: String,
    default: '',
  },
  labFilter: {
    type: String,
    default: 'all',
  },
  labMetaMap: {
    type: Object,
    default: () => ({}),
  },
  labOptions: {
    type: Array,
    default: () => [],
  },
  resultCount: {
    type: Number,
    default: 0,
  },
  sceneFilter: {
    type: String,
    default: 'all',
  },
  sceneOptions: {
    type: Array,
    default: () => [],
  },
  statusFilter: {
    type: String,
    default: 'all',
  },
  statusOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits([
  'update:conflictOnly',
  'update:keyword',
  'update:labFilter',
  'update:sceneFilter',
  'update:statusFilter',
])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const statusModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})

const sceneModel = computed({
  get: () => props.sceneFilter,
  set: (value) => emit('update:sceneFilter', value),
})

const labModel = computed({
  get: () => props.labFilter,
  set: (value) => emit('update:labFilter', value),
})

const conflictModel = computed({
  get: () => props.conflictOnly,
  set: (value) => emit('update:conflictOnly', value),
})
</script>

<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-bar">
      <div>
        <h2>预约筛选</h2>
      </div>
      <div class="filter-controls">
        <el-input v-model="keywordModel" class="keyword-input" clearable placeholder="搜索申请人、实验室、用途">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="statusModel" class="filter-select" placeholder="全部状态">
          <el-option label="全部状态" value="all" />
          <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
        </el-select>
        <el-select v-model="sceneModel" class="filter-select" placeholder="全部类型">
          <el-option label="全部类型" value="all" />
          <el-option v-for="scene in sceneOptions" :key="scene" :label="scene" :value="scene" />
        </el-select>
        <el-select v-model="labModel" class="lab-select" placeholder="全部实验室">
          <el-option label="全部实验室" value="all" />
          <el-option v-for="lab in labOptions" :key="lab" :label="formatLabOptionLabel(labMetaMap[lab] || lab)" :value="lab" />
        </el-select>
        <el-switch v-model="conflictModel" active-text="仅冲突" />
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

.lab-select {
  width: 190px;
}

@media (max-width: 1080px) {
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
  .filter-select,
  .lab-select {
    width: 100%;
  }
}
</style>
