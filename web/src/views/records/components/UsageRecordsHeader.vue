<script setup>
import { DocumentChecked, Download, Filter } from '@element-plus/icons-vue'

defineOptions({
  name: 'UsageRecordsHeader',
})

defineProps({
  statusSummary: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['export', 'filter-abnormal'])
</script>

<template>
  <el-card class="usage-header" shadow="never">
    <div class="header-content">
      <span class="header-icon" aria-hidden="true">
        <el-icon><DocumentChecked /></el-icon>
      </span>
      <div class="header-copy">
        <el-tag effect="light">过程留痕</el-tag>
        <h1>使用记录</h1>
        <div class="status-strip">
          <span v-for="status in statusSummary" :key="status.label">{{ status.label }} {{ status.count }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Filter" @click="$emit('filter-abnormal')">筛选异常</el-button>
        <el-button :icon="Download" @click="$emit('export')">导出记录</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.usage-header {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
}

.usage-header :deep(.el-card__body) {
  padding: 22px 24px;
}

.header-content,
.header-actions,
.status-strip {
  display: flex;
  align-items: center;
}

.header-content {
  position: relative;
  justify-content: space-between;
  gap: 18px;
}

.header-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  color: #2563eb;
  background: #eff6ff;
  font-size: 28px;
}

.header-copy {
  flex: 1 1 auto;
  min-width: 0;
}

.header-copy h1 {
  margin: 7px 0 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.3;
}

.status-strip {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-strip span {
  padding: 5px 10px;
  border: 1px solid #dbe7f5;
  border-radius: 7px;
  color: #475569;
  background: #ffffff;
  font-size: 12px;
  font-weight: 600;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

.header-actions :deep(.el-button) {
  min-height: 36px;
  border-radius: 7px;
  font-weight: 700;
}

.header-actions :deep(.el-button--primary) {
  border-color: #2563eb;
  background: #2563eb;
}

.header-copy :deep(.el-tag) {
  height: 26px;
  border-color: #bfdbfe;
  border-radius: 7px;
  color: #1d4ed8;
  background: #eff6ff;
  font-weight: 700;
}

@media (max-width: 900px) {
  .header-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 640px) {
  .usage-header :deep(.el-card__body) {
    padding: 18px;
  }

  .header-icon {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }

  .header-copy h1 {
    font-size: 21px;
  }
}
</style>
