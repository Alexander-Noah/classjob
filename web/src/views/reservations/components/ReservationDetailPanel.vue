<!--
  Web 管理平台的实验室预约详情面板视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Check, CircleClose, Delete, Edit, WarningFilled } from '@element-plus/icons-vue'
import { getActionDisabled, getActionDisabledReason } from '../reservationRules'

defineOptions({
  name: 'ReservationDetailPanel',
})

const props = defineProps({
  reservation: {
    type: Object,
    default: null,
  },
})

defineEmits(['approve', 'cancel', 'delete', 'edit', 'reject'])

const actions = computed(/** 计算派生状态。 */ () => ({
  approve: getActionDisabled(props.reservation || {}, 'approve'),
  reject: getActionDisabled(props.reservation || {}, 'reject'),
  edit: getActionDisabled(props.reservation || {}, 'edit'),
  cancel: getActionDisabled(props.reservation || {}, 'cancel'),
}))

const actionLabels = {
  approve: '通过',
  reject: '驳回',
  edit: '编辑信息',
  cancel: '取消预约',
}

/**
 * 获取Tooltip内容。
 * 入参：action（action）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getTooltipContent = (action) => getActionDisabledReason(props.reservation || {}, action) || actionLabels[action]

/**
 * 转换为Statistic数值。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const toStatisticNumber = (value) => {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}
</script>

<template>
  <el-card v-if="reservation" class="detail-card" shadow="never">
    <template #header>
      <div class="detail-header">
        <div>
          <h2>{{ reservation.applicant }}</h2>
          <span>{{ reservation.department || '未填写组织' }}</span>
        </div>
        <el-tag :type="reservation.tagType" effect="dark">{{ reservation.status }}</el-tag>
      </div>
    </template>

    <el-alert
      v-if="reservation.conflict"
      class="conflict-alert"
      type="error"
      :closable="false"
      show-icon
      title="该申请与同实验室同日期时段存在冲突，需要管理员人工确认。"
    >
      <template #icon>
        <el-icon><WarningFilled /></el-icon>
      </template>
    </el-alert>
    <div class="detail-actions">
      <div class="detail-actions__label">审核操作</div>
      <div class="approval-group">
        <el-tooltip :content="getTooltipContent('approve')" :disabled="!actions.approve" placement="top">
          <span class="action-tooltip-wrap">
            <el-button
              class="approval-button approval-button--pass"
              type="success"
              :disabled="actions.approve"
              :icon="Check"
              @click="$emit('approve', reservation)"
            >
              通过
            </el-button>
          </span>
        </el-tooltip>
        <el-tooltip :content="getTooltipContent('reject')" :disabled="!actions.reject" placement="top">
          <span class="action-tooltip-wrap">
            <el-button
              class="approval-button approval-button--reject"
              type="danger"
              plain
              :disabled="actions.reject"
              :icon="CircleClose"
              @click="$emit('reject', reservation)"
            >
              驳回
            </el-button>
          </span>
        </el-tooltip>
      </div>
      <el-divider class="action-divider" />
      <el-space class="utility-actions" :size="8" wrap>
        <el-tooltip :content="getTooltipContent('edit')" :disabled="!actions.edit" placement="top">
          <span class="utility-tooltip-wrap">
            <el-button class="utility-button" size="small" :disabled="actions.edit" :icon="Edit" @click="$emit('edit', reservation)">
              编辑信息
            </el-button>
          </span>
        </el-tooltip>
        <el-tooltip :content="getTooltipContent('cancel')" :disabled="!actions.cancel" placement="top">
          <span class="utility-tooltip-wrap">
            <el-button class="utility-button" size="small" :disabled="actions.cancel" plain @click="$emit('cancel', reservation)">取消预约</el-button>
          </span>
        </el-tooltip>
        <el-button class="utility-button utility-button--danger" size="small" type="danger" plain :icon="Delete" @click="$emit('delete', reservation)">
          删除
        </el-button>
      </el-space>
    </div>

    <el-row :gutter="10" class="detail-kpis">
      <el-col :span="8">
        <el-card class="kpi-card" shadow="never">
          <div class="kpi-label">预约日期</div>
          <div class="kpi-value">{{ reservation.date || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi-card" shadow="never">
          <div class="kpi-label">预约时段</div>
          <div class="kpi-value">{{ reservation.timeRange || '-' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="kpi-card" shadow="never">
          <el-statistic title="预约人数" :value="toStatisticNumber(reservation.attendees)" />
        </el-card>
      </el-col>
    </el-row>

    <div class="detail-section">
      <h3>申请信息</h3>
      <el-descriptions :column="1" border class="reservation-descriptions">
        <el-descriptions-item label="实验室">{{ reservation.lab }}</el-descriptions-item>
        <el-descriptions-item label="预约类型">{{ reservation.scene }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ reservation.contact }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ reservation.submittedAt }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ reservation.reviewer }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ reservation.reviewedAt }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ reservation.status === '已通过' ? reservation.note || '-' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因">{{ reservation.status === '已驳回' ? reservation.note || '-' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ reservation.cancelReason || (reservation.status === '已取消' ? reservation.note : '-') }}</el-descriptions-item>
        <el-descriptions-item label="冲突信息">{{ reservation.conflict ? reservation.note || '存在时间冲突' : '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="detail-section">
      <h3>用途与审核备注</h3>
      <el-text class="reason-text" type="info">{{ reservation.reason }}</el-text>
      <el-card class="note-card" shadow="never">{{ reservation.note }}</el-card>
    </div>
  </el-card>
</template>

<style scoped>
.detail-card {
  position: sticky;
  top: 16px;
  overflow: hidden;
  max-height: calc(100vh - 112px);
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.detail-card :deep(.el-card__body) {
  max-height: calc(100vh - 184px);
  overflow: auto;
  padding: 16px;
}

.detail-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
  background: #fbfdff;
}

.detail-header {
  display: flex;
  align-items: center;
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

.detail-header span {
  color: var(--text-secondary);
  font-size: 13px;
}

.conflict-alert {
  margin-bottom: 14px;
}

.detail-actions {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e8eef7;
  border-radius: 8px;
  background: #f8fbff;
}

.detail-actions__label {
  margin-bottom: 10px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.approval-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;
}

.action-tooltip-wrap,
.utility-tooltip-wrap {
  display: inline-flex;
  width: 100%;
}

.approval-button {
  width: 100%;
  height: 34px;
  margin-left: 0 !important;
  border-radius: 6px;
  font-weight: 700;
}

.approval-button--pass {
  border-color: #16a34a;
  background: #16a34a;
}

.approval-button--reject {
  background: #fff7f7;
}

.action-divider {
  margin: 12px 0;
}

.utility-actions {
  width: 100%;
}

.utility-actions :deep(.el-space__item) {
  flex: 1 1 calc(33.333% - 6px);
  min-width: 92px;
}

.utility-button {
  width: 100%;
  margin-left: 0 !important;
  border-radius: 6px;
  font-weight: 600;
}

.utility-button--danger {
  background: #fffafa;
}
.detail-kpis {
  row-gap: 10px;
}

.kpi-card {
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
}

.kpi-card :deep(.el-card__body) {
  padding: 12px;
}

.kpi-card :deep(.el-statistic__head) {
  color: var(--text-secondary);
  font-size: 12px;
}

.kpi-card :deep(.el-statistic__content) {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}

.kpi-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.kpi-value {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
}

.detail-section {
  margin-top: 18px;
}

.reservation-descriptions {
  margin-top: 12px;
}

.reservation-descriptions :deep(.el-descriptions__label) {
  width: 92px;
  color: var(--text-secondary);
  font-weight: 400;
}

.reservation-descriptions :deep(.el-descriptions__content) {
  color: var(--text-primary);
  text-align: left;
}

.reason-text {
  display: block;
  margin-top: 10px;
  line-height: 1.65;
}

.note-card {
  margin-top: 10px;
  border: 1px solid #e5edf6;
  border-radius: 8px;
  background: #f8fbff;
  color: var(--text-secondary);
  font-size: 13px;
}

.note-card :deep(.el-card__body) {
  padding: 10px;
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
  .detail-kpis :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }

  .approval-group {
    grid-template-columns: 1fr;
  }
}
</style>
