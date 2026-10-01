<script setup>
import { Calendar, Download, Plus, WarningFilled } from '@element-plus/icons-vue'

defineOptions({
  name: 'ReservationManagementHeader',
})

defineProps({
  statusSummary: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['create', 'detect', 'export'])
</script>

<template>
  <el-card class="reservation-header" shadow="never">
    <div class="header-content">
      <span class="header-icon" aria-hidden="true">
        <el-icon><Calendar /></el-icon>
      </span>
      <div class="header-copy">
        <el-tag effect="light">预约审核台</el-tag>
        <h1>预约管理</h1>
        <el-space wrap class="status-strip">
          <el-tag v-for="status in statusSummary" :key="status.label" effect="plain" round>
            {{ status.label }} {{ status.count }}
          </el-tag>
        </el-space>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="$emit('create')">新建预约</el-button>
        <el-button :icon="WarningFilled" @click="$emit('detect')">冲突检测</el-button>
        <el-button :icon="Download" @click="$emit('export')">导出审核表</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.reservation-header {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background:
    linear-gradient(135deg, #ffffff 0%, #f8fbff 54%, #eef6ff 100%);
}

.reservation-header :deep(.el-card__body) {
  padding: 22px 24px;
}

.header-content,
.header-actions {
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
  width: 58px;
  height: 58px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 30px;
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
  margin-top: 12px;
}

.status-strip :deep(.el-tag) {
  border-color: #bfdbfe;
  background: #ffffff;
  color: #1e40af;
  font-weight: 600;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  max-width: 360px;
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 980px) {
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
  .reservation-header :deep(.el-card__body) {
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
    flex: 1 1 138px;
    margin-left: 0;
  }
}
</style>
