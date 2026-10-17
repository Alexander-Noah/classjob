<!--
  Web 管理平台的设备管理筛选栏视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
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
  /**
   * 获取当前操作。
   * @returns props.keyword 的当前值。
   */
  get: () => props.keyword,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:keyword', value),
})

const categoryModel = computed({
  /**
   * 获取当前操作。
   * @returns props.categoryFilter 的当前值。
   */
  get: () => props.categoryFilter,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:categoryFilter', value),
})

const labModel = computed({
  /**
   * 获取当前操作。
   * @returns props.labFilter 的当前值。
   */
  get: () => props.labFilter,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:labFilter', value),
})

const statusModel = computed({
  /**
   * 获取当前操作。
   * @returns props.statusFilter 的当前值。
   */
  get: () => props.statusFilter,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
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
