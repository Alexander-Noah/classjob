<script setup>
import { Check, CircleClose, DocumentChecked, Finished } from '@element-plus/icons-vue'

defineOptions({
  name: 'UsageRecordListTable',
})

defineProps({
  records: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['archive', 'close-using', 'mark-abnormal', 'review'])

const isArchived = (row) => row.status === '已归档'
const isReadonly = (row) => Boolean(row.readonly)
const isActionDisabled = (row) => isArchived(row) || isReadonly(row)
</script>

<template>
  <el-table
    :data="records"
    class="usage-table"
    empty-text="未匹配到使用记录"
    row-key="id"
  >
    <el-table-column label="记录编号" min-width="130" prop="displayId" show-overflow-tooltip />
    <el-table-column label="人员/班级" min-width="140" prop="person" show-overflow-tooltip />
    <el-table-column label="资源" min-width="170" prop="resource" show-overflow-tooltip />
    <el-table-column label="实验室" min-width="170" prop="lab" show-overflow-tooltip />
    <el-table-column label="类型" min-width="105" prop="type" />
    <el-table-column label="来源" min-width="90" prop="sourceType">
      <template #default="{ row }">
        <el-tag :type="row.readonly ? 'info' : 'success'" effect="light">{{ row.sourceType || '台账' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="进入时间" min-width="120" prop="entryTime" />
    <el-table-column label="离开时间" min-width="120" prop="leaveTime" />
    <el-table-column label="时长" min-width="110" prop="duration" />
    <el-table-column label="状态" min-width="95" prop="status">
      <template #default="{ row }">
        <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column fixed="right" label="处理操作" min-width="285">
      <template #default="{ row }">
        <el-button :disabled="isActionDisabled(row)" :icon="Finished" link type="primary" @click.stop="$emit('close-using', row)">
          结束
        </el-button>
        <el-button :disabled="isActionDisabled(row)" :icon="Check" link type="success" @click.stop="$emit('review', row)">
          复核
        </el-button>
        <el-button :disabled="isActionDisabled(row)" :icon="CircleClose" link type="danger" @click.stop="$emit('mark-abnormal', row)">
          异常
        </el-button>
        <el-button :disabled="isActionDisabled(row)" :icon="DocumentChecked" link @click.stop="$emit('archive', row)">
          归档
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.usage-table {
  width: 100%;
}

.usage-table :deep(.el-table__header-wrapper th.el-table__cell) {
  height: 48px;
  border-bottom: 1px solid #e5edf6;
  background: #f8fbff;
  color: #334155;
  font-weight: 700;
}

.usage-table :deep(.el-table__body-wrapper td.el-table__cell) {
  height: 56px;
  border-bottom: 1px solid #eef3f8;
  color: #334155;
}

.usage-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #eff6ff;
}

.usage-table :deep(.el-tag) {
  min-width: 54px;
  justify-content: center;
  border-radius: 6px;
  font-weight: 700;
}

.usage-table :deep(.el-button.is-link) {
  height: 28px;
  padding: 0 4px;
  border-radius: 6px;
  font-weight: 700;
}

.usage-table :deep(.el-table__empty-block) {
  min-height: 220px;
}

.usage-table :deep(.el-table__empty-text) {
  color: #64748b;
  font-size: 13px;
}
</style>
