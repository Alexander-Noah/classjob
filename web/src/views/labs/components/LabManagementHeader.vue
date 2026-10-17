<!--
  Web 管理平台的实验室页头视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { Download, OfficeBuilding, Plus } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabManagementHeader',
})

defineProps({
  statusSummary: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
})

defineEmits(['create', 'export'])
</script>

<template>
  <el-card class="lab-header" shadow="never">
    <div class="lab-header-content">
      <span class="header-icon" aria-hidden="true">
        <el-icon>
          <OfficeBuilding />
        </el-icon>
      </span>
      <div class="header-copy">
        <el-tag effect="light">空间资源</el-tag>
        <h1>实验室管理</h1>
        <div class="status-strip">
          <span v-for="status in statusSummary" :key="status.label">
            {{ status.label }} {{ status.count }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="$emit('create')">新增实验室</el-button>
        <el-button :icon="Download" @click="$emit('export')">导出清单</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.lab-header {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(20, 184, 166, 0.06) 46%, rgba(255, 255, 255, 0.98)),
    var(--surface-color);
}

.lab-header::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.07) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(90deg, #000 0%, rgba(0, 0, 0, 0.16) 70%, transparent 100%);
}

.lab-header :deep(.el-card__body) {
  position: relative;
  z-index: 1;
  padding: 24px 28px;
}

.lab-header-content,
.header-actions,
.status-strip {
  display: flex;
  align-items: center;
}

.lab-header-content {
  justify-content: space-between;
  gap: 22px;
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
  border: 1px solid rgba(37, 99, 235, 0.16);
  background: rgba(255, 255, 255, 0.72);
  font-size: 30px;
}

.header-copy {
  flex: 1 1 auto;
  min-width: 0;
}

.header-copy h1 {
  margin: 8px 0 0;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.22;
  text-wrap: balance;
}

.status-strip {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-strip span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 5px 10px;
  border: 1px solid rgba(37, 99, 235, 0.12);
  border-radius: 7px;
  color: #1e3a8a;
  background: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  font-weight: 650;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  flex: 0 0 auto;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .lab-header-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .lab-header :deep(.el-card__body) {
    padding: 20px;
  }

  .header-copy h1 {
    font-size: 24px;
  }
}
</style>
