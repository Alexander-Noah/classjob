<!--
  Web 管理平台的业务记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'BusinessStatusSummary',
})

const props = defineProps({
  statusFilter: {
    type: String,
    default: 'all',
  },
  statusSummary: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:statusFilter'])

const statusFilterModel = computed({
  get: () => props.statusFilter,
  set: (value) => emit('update:statusFilter', value),
})
</script>

<template>
  <el-card class="module-card status-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div>
          <h2>状态分布</h2>
          <span>按当前台账实时汇总</span>
        </div>
      </div>
    </template>

    <div class="status-list">
      <button
        v-for="status in statusSummary"
        :key="status.label"
        class="status-line"
        :class="{ active: statusFilterModel === status.label }"
        type="button"
        @click="statusFilterModel = status.label"
      >
        <span class="status-copy">
          <el-tag :type="status.type" effect="light">{{ status.label }}</el-tag>
          <strong>{{ status.count }} 条</strong>
        </span>
        <el-progress :percentage="status.percentage" :show-text="false" :stroke-width="8" />
      </button>
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
.status-copy {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.card-header span {
  color: var(--text-secondary);
  font-size: 13px;
}

.status-card :deep(.el-card__body) {
  padding: 18px;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-line {
  display: grid;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.status-line:hover,
.status-line:focus-visible,
.status-line.active {
  border-color: #93c5fd;
  outline: none;
  background: #f8fbff;
}

.status-copy {
  justify-content: space-between;
  gap: 10px;
}

.status-copy strong {
  color: var(--text-primary);
  font-size: 13px;
}
</style>
