<!--
  Web 管理平台的实验室筛选栏视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabFilterBar',
})

const props = defineProps({
  availableOnly: {
    type: Boolean,
    default: false,
  },
  buildingFilter: {
    type: String,
    default: 'all',
  },
  buildingOptions: {
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  typeFilter: {
    type: String,
    default: 'all',
  },
  typeOptions: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
})

const emit = defineEmits([
  'update:availableOnly',
  'update:buildingFilter',
  'update:keyword',
  'update:statusFilter',
  'update:typeFilter',
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

const buildingModel = computed({
  /**
   * 获取当前操作。
   * @returns props.buildingFilter 的当前值。
   */
  get: () => props.buildingFilter,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:buildingFilter', value),
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

const typeModel = computed({
  /**
   * 获取当前操作。
   * @returns props.typeFilter 的当前值。
   */
  get: () => props.typeFilter,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:typeFilter', value),
})

const availableModel = computed({
  /**
   * 获取当前操作。
   * @returns props.availableOnly 的当前值。
   */
  get: () => props.availableOnly,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  set: (value) => emit('update:availableOnly', value),
})
</script>

<template>
  <el-card class="filter-card" shadow="never">
    <div class="filter-bar">
      <div>
        <h2>实验室筛选</h2>
      </div>
      <div class="filter-controls">
        <el-input v-model="keywordModel" class="keyword-input" clearable placeholder="搜索名称、编号、负责人">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>
        <el-select v-model="buildingModel" class="filter-select" placeholder="全部楼宇">
          <el-option label="全部楼宇" value="all" />
          <el-option v-for="building in buildingOptions" :key="building" :label="building" :value="building" />
        </el-select>
        <el-select v-model="statusModel" class="filter-select" placeholder="全部状态">
          <el-option label="全部状态" value="all" />
          <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
        </el-select>
        <el-select v-model="typeModel" class="filter-select" placeholder="全部类型">
          <el-option label="全部类型" value="all" />
          <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
        </el-select>
        <el-switch v-model="availableModel" active-text="仅可预约" />
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.filter-card {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
  overflow: hidden;
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
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #f8fafc;
}

.keyword-input {
  width: 280px;
}

.filter-select {
  width: 130px;
}

@media (max-width: 1000px) {
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
  .filter-select {
    width: 100%;
  }
}
</style>
