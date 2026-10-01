<script setup>
import { Download, Plus, Tools } from '@element-plus/icons-vue'

defineOptions({
  name: 'RepairManagementHeader',
})

defineProps({
  statusSummary: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['create', 'export'])
</script>

<template>
  <el-card class="repair-header" shadow="never">
    <div class="header-content">
      <span class="header-icon" aria-hidden="true">
        <el-icon><Tools /></el-icon>
      </span>
      <div class="header-copy">
        <el-tag effect="light">维修派单台</el-tag>
        <h1>报修管理</h1>
        <div class="status-strip">
          <span v-for="status in statusSummary" :key="status.label">{{ status.label }} {{ status.count }}</span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="$emit('create')">登记报修</el-button>
        <el-button :icon="Download" @click="$emit('export')">导出工单</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.repair-header {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.repair-header :deep(.el-card__body) {
  padding: 22px 24px;
  background:
    linear-gradient(135deg, #ffffff 0%, #f8fbff 56%, #eef6ff 100%);
}

.header-content,
.header-actions,
.status-strip {
  display: flex;
  align-items: center;
}

.header-content {
  justify-content: space-between;
  gap: 20px;
}

.header-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eaf2ff;
  font-size: 28px;
}

.header-copy {
  flex: 1 1 auto;
  min-width: 0;
}

.header-copy h1 {
  margin: 8px 0 0;
  color: var(--text-primary);
  font-size: 25px;
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
  padding: 5px 9px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  color: #1e40af;
  background: #ffffff;
  font-size: 12px;
  font-weight: 600;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  max-width: 260px;
}

.header-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .header-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
    max-width: none;
    width: 100%;
  }
}

@media (max-width: 560px) {
  .repair-header :deep(.el-card__body) {
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

  .header-actions :deep(.el-button) {
    flex: 1 1 128px;
    margin-left: 0;
  }
}
</style>
