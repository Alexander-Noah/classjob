<script setup>
import { Delete, Edit, Finished, Promotion, Tools, WarningFilled } from '@element-plus/icons-vue'

defineOptions({
  name: 'RepairDetailPanel',
})

defineProps({
  repair: {
    type: Object,
    default: null,
  },
})

defineEmits(['assign', 'close', 'complete', 'delete', 'edit', 'escalate', 'submit-acceptance'])
</script>

<template>
  <el-card v-if="repair" class="detail-card" shadow="never">
    <template #header>
      <div class="detail-header">
        <div>
          <h2>{{ repair.ticket }}</h2>
          <span>{{ repair.lab }} · {{ repair.device }}</span>
        </div>
        <el-tag :type="repair.tagType" effect="dark">{{ repair.status }}</el-tag>
      </div>
    </template>

    <div v-if="repair.priority === '高' && !['已完成', '已关闭'].includes(repair.status)" class="urgent-alert">
      <el-icon><WarningFilled /></el-icon>
      <span>高优先级工单，请优先派单并跟进维修进度。</span>
    </div>

    <div class="detail-actions">
      <el-button size="small" type="primary" :icon="Promotion" @click="$emit('assign', repair)">派单</el-button>
      <el-button size="small" type="success" plain :icon="Finished" @click="$emit('submit-acceptance', repair)">提交验收</el-button>
      <el-button size="small" type="success" :icon="Finished" @click="$emit('complete', repair)">完成</el-button>
      <el-button size="small" type="warning" plain :icon="WarningFilled" @click="$emit('escalate', repair)">升级</el-button>
      <el-button size="small" :icon="Edit" @click="$emit('edit', repair)">编辑</el-button>
      <el-button size="small" plain :icon="Tools" @click="$emit('close', repair)">关闭</el-button>
      <el-button size="small" type="danger" plain :icon="Delete" @click="$emit('delete', repair)">删除</el-button>
    </div>

    <div class="progress-box">
      <div>
        <span>处理进度</span>
        <strong>{{ repair.progress }}%</strong>
      </div>
      <el-progress :percentage="repair.progress" :stroke-width="8" />
    </div>

    <div class="detail-kpis">
      <div>
        <span>优先级</span>
        <strong>{{ repair.priority }}</strong>
      </div>
      <div>
        <span>维修人</span>
        <strong>{{ repair.assignee }}</strong>
      </div>
      <div>
        <span>截止时间</span>
        <strong>{{ repair.deadline }}</strong>
      </div>
    </div>

    <div class="detail-section">
      <h3>工单信息</h3>
      <dl>
        <div>
          <dt>报修人</dt>
          <dd>{{ repair.reporter }}</dd>
        </div>
        <div>
          <dt>联系方式</dt>
          <dd>{{ repair.contact }}</dd>
        </div>
        <div>
          <dt>故障类型</dt>
          <dd>{{ repair.faultType }}</dd>
        </div>
        <div>
          <dt>提交时间</dt>
          <dd>{{ repair.submittedAt }}</dd>
        </div>
        <div>
          <dt>最近更新</dt>
          <dd>{{ repair.lastUpdate }}</dd>
        </div>
      </dl>
    </div>

    <div class="detail-section">
      <h3>故障与处理结果</h3>
      <p>{{ repair.description }}</p>
      <p class="result-text">{{ repair.result }}</p>
    </div>
  </el-card>
</template>

<style scoped>
.detail-card {
  position: sticky;
  top: 14px;
  overflow: hidden;
  max-height: calc(100vh - 108px);
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.detail-card :deep(.el-card__body) {
  max-height: calc(100vh - 178px);
  overflow: auto;
  padding: 16px;
}

.detail-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.detail-header,
.detail-actions,
.urgent-alert,
.progress-box div {
  display: flex;
  align-items: center;
}

.detail-header,
.progress-box div {
  justify-content: space-between;
  gap: 12px;
}

.detail-header h2,
.detail-section h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.detail-header span,
.detail-section dt,
.detail-section p,
.progress-box span {
  color: #64748b;
  font-size: 13px;
}

.urgent-alert {
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  color: #c2410c;
  background: #fff7ed;
  font-size: 13px;
}

.detail-actions {
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

.detail-actions .el-button + .el-button {
  margin-left: 0;
}

.detail-actions :deep(.el-button) {
  border-radius: 6px;
  font-weight: 700;
}

.progress-box {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fbff;
}

.progress-box strong {
  color: var(--text-primary);
}

.detail-kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.detail-kpis div {
  padding: 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #dbe7f5;
}

.detail-kpis span {
  color: var(--text-secondary);
  font-size: 12px;
}

.detail-kpis strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.35;
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
  border-bottom: 1px solid #e5edf6;
}

.detail-section dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  text-align: right;
}

.detail-section p {
  margin: 10px 0 0;
  line-height: 1.65;
}

.result-text {
  padding: 10px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

@media (max-width: 1200px) {
  .detail-card {
    position: static;
    max-height: none;
  }

  .detail-card :deep(.el-card__body) {
    max-height: none;
  }
}

@media (max-width: 560px) {
  .detail-kpis {
    grid-template-columns: 1fr;
  }

  .detail-actions :deep(.el-button) {
    flex: 1 1 120px;
    margin-left: 0;
  }
}
</style>
