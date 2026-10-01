<script setup>
import { computed } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { formatLabLabel, getLabId } from '../../../api/services/deviceLabBinding'

defineOptions({
  name: 'DeviceFilterBar',
})

const props = defineProps({
  categoryFilter: {
    type: String,
    default: '',
  },
  categoryOptions: {
    type: Array,
    default: () => [],
  },
  keyword: {
    type: String,
    default: '',
  },
  labFilter: {
    type: String,
    default: '',
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
    default: '',
  },
  statusOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits([
  'reset',
  'search',
  'update:categoryFilter',
  'update:keyword',
  'update:labFilter',
  'update:statusFilter',
])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const categoryModel = computed({
  get: () => props.categoryFilter,
  set: (value) => emit('update:categoryFilter', value),
})

const labModel = computed({
  get: () => props.labFilter,
  set: (value) => emit('update:labFilter', value),
})

const statusModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})
</script>

<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-bar">
      <div>
        <h2>条件查询</h2>
      </div>
      <div class="filter-controls">
        <el-input
          v-model="keywordModel"
          class="keyword-input"
          clearable
          placeholder="搜索资产编号、设备名称"
          @keyup.enter="$emit('search')"
        >
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-select
          v-model="labModel"
          class="filter-select wide-select"
          clearable
          filterable
          placeholder="所属实验室"
        >
          <el-option label="全部实验室" value="" />
          <el-option
            v-for="lab in labOptions"
            :key="getLabId(lab)"
            :label="formatLabLabel(lab)"
            :value="getLabId(lab)"
          />
        </el-select>
        <el-select v-model="categoryModel" class="filter-select" clearable placeholder="设备分类">
          <el-option v-for="category in categoryOptions" :key="category" :label="category" :value="category" />
        </el-select>
        <el-select v-model="statusModel" class="filter-select" clearable placeholder="设备状态">
          <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="$emit('search')">查询</el-button>
        <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.filter-card {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
}

.filter-card :deep(.el-card__body) {
  padding: 16px 18px;
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
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
}

.filter-controls {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.keyword-input {
  width: 260px;
}

.filter-select {
  width: 132px;
}

.wide-select {
  width: 180px;
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
  .filter-controls {
    display: grid;
    grid-template-columns: 1fr;
  }

  .keyword-input,
  .filter-select,
  .wide-select {
    width: 100%;
  }
}
</style>
