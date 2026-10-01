<script setup>
import { Check, CircleClose, DocumentChecked, Finished, WarningFilled } from '@element-plus/icons-vue'

defineOptions({
  name: 'UsageRecordDetailPanel',
})

defineProps({
  record: {
    type: Object,
    default: null,
  },
})

defineEmits(['archive', 'close-using', 'mark-abnormal', 'review'])

const isActionDisabled = (record) => record?.status === '已归档' || Boolean(record?.readonly)
const isAbnormal = (record) => ['异常', '待复核'].includes(record?.status)
</script>

<template>
  <el-card v-if="record" class="detail-card" shadow="never">
    <template #header>
      <div class="detail-header">
        <div>
          <h2>{{ record.person }}</h2>
          <span>{{ record.displayId || record.id }} · {{ record.type }} · {{ record.sourceType || '台账' }}</span>
        </div>
        <el-tag :type="record.tagType" effect="dark">{{ record.status }}</el-tag>
      </div>
    </template>

    <div v-if="isAbnormal(record)" class="warning-alert">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ record.abnormalReason }}</span>
    </div>

    <div class="detail-actions">
      <el-button size="small" type="primary" :disabled="isActionDisabled(record)" :icon="Finished" @click="$emit('close-using', record)">
        结束使用
      </el-button>
      <el-button size="small" type="success" plain :disabled="isActionDisabled(record)" :icon="Check" @click="$emit('review', record)">
        复核正常
      </el-button>
      <el-button size="small" type="danger" plain :disabled="isActionDisabled(record)" :icon="CircleClose" @click="$emit('mark-abnormal', record)">
        标记异常
      </el-button>
      <el-button size="small" :disabled="isActionDisabled(record)" :icon="DocumentChecked" @click="$emit('archive', record)">
        归档
      </el-button>
    </div>

    <div class="detail-kpis">
      <div>
        <span>进入时间</span>
        <strong>{{ record.entryTime }}</strong>
      </div>
      <div>
        <span>离开时间</span>
        <strong>{{ record.leaveTime }}</strong>
      </div>
      <div>
        <span>使用时长</span>
        <strong>{{ record.duration }}</strong>
      </div>
    </div>

    <div class="detail-section">
      <h3>记录信息</h3>
      <dl>
        <div>
          <dt>角色</dt>
          <dd>{{ record.role }}</dd>
        </div>
        <div>
          <dt>资源</dt>
          <dd>{{ record.resource }}</dd>
        </div>
        <div>
          <dt>实验室</dt>
          <dd>{{ record.lab }}</dd>
        </div>
        <div>
          <dt>通行/审批结果</dt>
          <dd>{{ record.accessResult }}</dd>
        </div>
        <div>
          <dt>来源</dt>
          <dd>{{ record.sourceType || '台账' }}</dd>
        </div>
        <div>
          <dt>复核人</dt>
          <dd>{{ record.reviewer }}</dd>
        </div>
      </dl>
    </div>

    <div class="detail-section">
      <h3>异常说明与备注</h3>
      <p>{{ record.abnormalReason }}</p>
      <p class="note-text">{{ record.note }}</p>
    </div>
  </el-card>

  <el-card v-else class="detail-card empty-card" shadow="never">
    <el-empty description="请选择一条使用记录" :image-size="88" />
  </el-card>
</template>

<style scoped>
.detail-card {
  position: sticky;
  top: 16px;
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
}

.detail-card :deep(.el-card__header) {
  min-height: 72px;
  padding: 16px 18px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.detail-card :deep(.el-card__body) {
  max-height: calc(100vh - 196px);
  overflow: auto;
  padding: 18px;
}

.empty-card :deep(.el-card__body) {
  padding: 46px 18px;
}

.detail-header,
.detail-actions,
.warning-alert {
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
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.detail-header span,
.detail-section dt,
.detail-section p {
  color: #64748b;
  font-size: 13px;
}

.warning-alert {
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #b91c1c;
  background: #fef2f2;
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

.detail-kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-kpis div {
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #fbfdff;
}

.detail-kpis span {
  color: #64748b;
  font-size: 12px;
}

.detail-kpis strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.35;
}

.detail-section {
  margin-top: 18px;
}

.detail-section dl {
  display: grid;
  margin: 12px 0 0;
  overflow: hidden;
  border: 1px solid #e5edf6;
  border-radius: 8px;
}

.detail-section dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  border-bottom: 1px solid #e5edf6;
}

.detail-section dl div:last-child {
  border-bottom: 0;
}

.detail-section dt {
  flex: 0 0 110px;
}

.detail-section dd {
  margin: 0;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.6;
  text-align: right;
  word-break: break-word;
}

.detail-section p {
  margin: 10px 0 0;
  line-height: 1.65;
}

.note-text {
  padding: 10px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

@media (max-width: 1200px) {
  .detail-card {
    position: static;
  }

  .detail-card :deep(.el-card__body) {
    max-height: none;
  }
}

@media (max-width: 520px) {
  .detail-kpis {
    grid-template-columns: 1fr;
  }

  .detail-section dl div {
    display: grid;
    gap: 4px;
  }

  .detail-section dd {
    text-align: left;
  }
}
</style>
