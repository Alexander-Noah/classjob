<!--
  Web 管理平台的MetricGrid视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'MetricGrid',
})

const props = defineProps({
  metrics: {
    type: Array,
    default: () => [],
  },
})

const metricColumnCount = computed(() => Math.min(Math.max(props.metrics.length, 1), 4))
</script>

<template>
  <div class="metric-grid" :style="{ '--metric-column-count': metricColumnCount }">
    <article v-for="metric in metrics" :key="metric.label" class="metric-card">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
      <small>{{ metric.trend }}</small>
    </article>
  </div>
</template>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(var(--metric-column-count), minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 112px;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.metric-card span,
.metric-card small {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1;
}

.metric-card small {
  display: block;
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
  }
}

@media (max-width: 760px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
