<!--
  Web 管理平台的实验室详情面板视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { Delete, Edit, FolderOpened, Tickets, Timer } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabDetailPanel',
})

defineProps({
  lab: {
    type: Object,
    default: null,
  },
  assetRecordCount: {
    type: Number,
    default: 0,
  },
})

defineEmits(['delete', 'edit', 'reservation-qr', 'status', 'view-assets'])
</script>

<template>
  <el-card v-if="lab" class="detail-card" shadow="never">
    <template #header>
      <div class="detail-header">
        <div>
          <h2>{{ lab.name }}</h2>
          <span>{{ lab.code }} · {{ lab.building }} · {{ lab.room }}</span>
        </div>
        <el-tag :type="lab.tagType" effect="dark">{{ lab.status }}</el-tag>
      </div>
    </template>

    <div class="detail-actions">
      <el-button size="small" type="primary" :icon="FolderOpened" @click="$emit('view-assets', lab)">
        设备信息
      </el-button>
      <el-button size="small" plain type="success" :icon="Tickets" @click="$emit('reservation-qr', lab)">
        预约码
      </el-button>
      <el-button size="small" plain :icon="Edit" @click="$emit('edit', lab)">编辑</el-button>
      <el-button size="small" plain @click="$emit('status', '开放')">设为开放</el-button>
      <el-button size="small" plain type="warning" @click="$emit('status', '使用中')">设为使用中</el-button>
      <el-button size="small" plain @click="$emit('status', '维护中')">设为维护中</el-button>
      <el-button size="small" plain type="danger" :icon="Delete" @click="$emit('delete', lab)">删除</el-button>
    </div>

    <div class="detail-hint">
      <span>已登记设备 {{ assetRecordCount }} 条，点击“设备信息”可在当前页直接查看。</span>
    </div>

    <div class="detail-kpis">
      <div>
        <span>设备总数</span>
        <strong>{{ assetRecordCount }}</strong>
      </div>
      <div>
        <span>容量</span>
        <strong>{{ lab.capacity }}</strong>
      </div>
    </div>

    <div class="detail-section">
      <h3>基础信息</h3>
      <dl>
        <div>
          <dt>负责人</dt>
          <dd>{{ lab.manager }}</dd>
        </div>
        <div>
          <dt>实验室类型</dt>
          <dd>{{ lab.type }}</dd>
        </div>
        <div>
          <dt>开放时段</dt>
          <dd>{{ lab.openTime }}</dd>
        </div>
        <div>
          <dt>安全状态</dt>
          <dd>{{ lab.safety }}</dd>
        </div>
      </dl>
    </div>

    <div class="detail-section">
      <h3>今日安排</h3>
      <div class="schedule-list">
        <div v-for="item in lab.schedule" :key="item">
          <el-icon aria-hidden="true">
            <Timer />
          </el-icon>
          <span>{{ item }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.detail-card {
  position: sticky;
  top: 84px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
  overflow: hidden;
}

.detail-card :deep(.el-card__header) {
  min-height: 64px;
  padding: 15px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}

.detail-card :deep(.el-card__body) {
  padding: 16px 18px 18px;
}

.detail-header,
.detail-actions,
.schedule-list div {
  display: flex;
  align-items: center;
}

.detail-header {
  justify-content: space-between;
  gap: 12px;
}

.detail-header h2,
.detail-section h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0;
}

.detail-header span {
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-actions {
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #f8fafc;
}

.detail-actions .el-button + .el-button {
  margin-left: 0;
}

.detail-hint {
  padding: 11px 12px;
  border: 1px solid rgba(37, 99, 235, 0.1);
  border-radius: 8px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.55;
}

.detail-kpis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.detail-kpis div {
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.detail-kpis span,
.detail-section dt {
  color: var(--text-secondary);
  font-size: 12px;
}

.detail-kpis strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 19px;
}

.detail-section {
  margin-top: 18px;
}

.detail-section dl {
  display: grid;
  gap: 10px;
  margin: 12px 0 0;
}

.detail-section dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}

.detail-section dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  text-align: right;
}

.schedule-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.schedule-list div {
  position: relative;
  gap: 9px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: #334155;
  background: #f8fbff;
  font-size: 13px;
  line-height: 1.45;
}

.schedule-list div::before {
  flex: 0 0 auto;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #2f7cf6;
  content: "";
}

.schedule-list :deep(.el-icon) {
  flex: 0 0 auto;
  color: #64748b;
}

@media (max-width: 1180px) {
  .detail-card {
    position: static;
  }
}
</style>
