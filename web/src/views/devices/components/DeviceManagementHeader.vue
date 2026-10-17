<!--
  Web 管理平台的设备管理页头视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { Download, Plus, Upload } from '@element-plus/icons-vue'

defineOptions({
  name: 'DeviceManagementHeader',
})

defineProps({
  importLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['create', 'download-template', 'export', 'import-file'])

/**
 * 处理ImportChange。
 * 入参：uploadFile（upload文件）。
 */
const handleImportChange = (uploadFile) => {
  if (uploadFile?.raw) {
    emit('import-file', uploadFile.raw)
  }
}
</script>

<template>
  <el-card class="device-header" shadow="never">
    <div class="device-header-content">
      <div class="header-copy">
        <el-tag effect="light" type="primary">设备资产管理模块</el-tag>
        <h1>设备资产台账</h1>
      </div>

      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="$emit('create')">新增设备</el-button>
        <el-upload
          :auto-upload="false"
          :on-change="handleImportChange"
          :show-file-list="false"
          accept=".xlsx,.csv"
        >
          <el-button :icon="Upload" :loading="importLoading">导入 Excel</el-button>
        </el-upload>
        <el-button :icon="Download" @click="$emit('download-template')">下载模板</el-button>
        <el-button :icon="Download" @click="$emit('export')">导出 Excel</el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.device-header {
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
}

.device-header :deep(.el-card__body) {
  padding: 20px 24px;
}

.device-header-content,
.header-actions {
  display: flex;
  align-items: center;
}

.device-header-content {
  justify-content: space-between;
  gap: 22px;
}

.header-copy {
  min-width: 0;
}

.header-copy h1 {
  margin: 8px 0 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.25;
}

.header-actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.header-actions .el-button + .el-button {
  margin-left: 0;
}

.header-actions :deep(.el-upload) {
  display: inline-flex;
}

@media (max-width: 900px) {
  .device-header-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .device-header :deep(.el-card__body) {
    padding: 20px;
  }
}
</style>
