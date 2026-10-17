<!--
  Web 管理平台的实验室预约列表视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { Check, CircleClose, Delete, Edit, WarningFilled } from '@element-plus/icons-vue'
import { getActionDisabled, getActionDisabledReason } from '../reservationRules'

defineOptions({
  name: 'ReservationListTable',
})

defineProps({
  reservations: {
    type: Array,
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
  rowOffset: {
    type: Number,
    default: 0,
  },
})

defineEmits(['approve', 'cancel', 'delete', 'edit', 'reject'])

const actionLabels = {
  approve: '通过',
  reject: '驳回',
  edit: '编辑',
  cancel: '取消预约',
  delete: '删除',
}

/**
 * 获取Tooltip内容。
 * 入参：row（行）；action（action）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getTooltipContent = (row, action) => getActionDisabledReason(row, action) || actionLabels[action]
</script>

<template>
  <el-table
    :data="reservations"
    border
    class="reservation-table"
    empty-text="暂无预约申请"
    row-key="id"
    size="large"
    stripe
    table-layout="fixed"
  >
    <el-table-column type="expand" width="42">
      <template #default="{ row }">
        <div class="row-expand">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="学院/组织">{{ row.department || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ row.contact || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ row.submittedAt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预约用途" :span="2">{{ row.reason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核备注">{{ row.note || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="编号" width="76" align="center">
      <template #default="{ $index }">
        {{ rowOffset + $index + 1 }}
      </template>
    </el-table-column>
    <el-table-column label="申请人" min-width="130" prop="applicant" show-overflow-tooltip />
    <el-table-column label="预约实验室" min-width="190" prop="lab" show-overflow-tooltip />
    <el-table-column label="日期" width="120" prop="date" />
    <el-table-column label="时段" width="132" prop="timeRange" />
    <el-table-column label="类型" width="86" prop="scene" align="center">
      <template #default="{ row }">
        <el-tag effect="plain">{{ row.scene || '-' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="人数" width="76" prop="attendees" align="center" />
    <el-table-column label="状态" width="104" prop="status" align="center">
      <template #default="{ row }">
        <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="冲突" width="90" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.conflict" type="danger" effect="dark">
          <el-icon><WarningFilled /></el-icon>
          冲突
        </el-tag>
        <span v-else class="muted-text">无</span>
      </template>
    </el-table-column>
    <el-table-column class="operation-column" class-name="operation-column" label="操作" width="300" align="center">
      <template #default="{ row }">
        <el-space :size="6" class="audit-actions" @click.stop>
          <el-tooltip :content="getTooltipContent(row, 'approve')" :disabled="!getActionDisabled(row, 'approve')" placement="top">
            <span class="action-tooltip-wrap">
              <el-button
                :disabled="getActionDisabled(row, 'approve')"
                :icon="Check"
                size="small"
                type="success"
                @click="$emit('approve', row)"
              >
                通过
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="getTooltipContent(row, 'reject')" :disabled="!getActionDisabled(row, 'reject')" placement="top">
            <span class="action-tooltip-wrap">
              <el-button
                :disabled="getActionDisabled(row, 'reject')"
                :icon="CircleClose"
                plain
                size="small"
                type="danger"
                @click="$emit('reject', row)"
              >
                驳回
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="getTooltipContent(row, 'edit')" :disabled="!getActionDisabled(row, 'edit')" placement="top">
            <span class="action-tooltip-wrap">
              <el-button :disabled="getActionDisabled(row, 'edit')" :icon="Edit" size="small" @click="$emit('edit', row)">编辑</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="getTooltipContent(row, 'cancel')" :disabled="!getActionDisabled(row, 'cancel')" placement="top">
            <span class="action-tooltip-wrap">
              <el-button :disabled="getActionDisabled(row, 'cancel')" plain size="small" @click="$emit('cancel', row)">取消预约</el-button>
            </span>
          </el-tooltip>
          <el-button :icon="Delete" plain size="small" type="danger" @click="$emit('delete', row)">删除</el-button>
        </el-space>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.reservation-table {
  width: 100%;
  --el-table-border-color: #e5edf6;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #eff6ff;
}

.reservation-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.reservation-table :deep(.el-table__header th) {
  height: 46px;
  background: #f8fbff !important;
}

.reservation-table :deep(.el-table__cell) {
  color: var(--text-primary);
  vertical-align: middle;
}

.reservation-table :deep(.el-table__header th) {
  color: #475569;
  font-weight: 700;
}

.reservation-table :deep(.el-table__body td) {
  height: 56px;
}

.row-expand {
  padding: 10px 18px 16px 54px;
  background: #fbfdff;
}

.row-expand :deep(.el-descriptions__label) {
  width: 86px;
  color: var(--text-secondary);
  font-weight: 500;
}

.muted-text {
  color: var(--text-muted);
  font-size: 12px;
}

.reservation-table :deep(.el-tag .el-icon) {
  margin-right: 4px;
}

.audit-actions {
  width: 100%;
  flex-wrap: wrap;
  justify-content: center;
}

.audit-actions :deep(.el-button) {
  border-radius: 6px;
  font-weight: 600;
}

.action-tooltip-wrap {
  display: inline-flex;
}

.audit-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 760px) {
  .row-expand {
    padding: 10px;
  }

  .row-expand :deep(.el-descriptions) {
    min-width: 680px;
  }
}
</style>
