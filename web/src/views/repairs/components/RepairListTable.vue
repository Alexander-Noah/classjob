<script setup>
import { ArrowDown, Check, Edit, Finished, Promotion } from '@element-plus/icons-vue'

defineOptions({
  name: 'RepairListTable',
})

const props = defineProps({
  repairs: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['assign', 'complete', 'edit', 'select', 'submit-acceptance'])

const getRowClassName = ({ row }) => (row.id === props.selectedId ? 'is-active-row' : '')

const getPriorityType = (priority) => {
  if (priority === '高') return 'danger'
  if (priority === '中') return 'warning'
  return 'info'
}

const getPrimaryAction = (repair) => {
  if (repair.status === '待派单') {
    return { label: '派单', icon: Promotion, command: 'assign', type: 'primary' }
  }

  if (repair.status === '处理中') {
    return { label: '提验收', icon: Finished, command: 'submit-acceptance', type: 'success' }
  }

  if (repair.status === '待验收') {
    return { label: '完成', icon: Check, command: 'complete', type: 'success' }
  }

  return null
}

const getMoreActions = (repair) => {
  const actions = [{ label: '编辑工单', icon: Edit, command: 'edit' }]

  if (repair.status !== '待派单') {
    actions.unshift({ label: '重新派单', icon: Promotion, command: 'assign' })
  }

  if (repair.status === '处理中') {
    actions.unshift({ label: '提交验收', icon: Finished, command: 'submit-acceptance' })
  }

  if (repair.status === '待验收') {
    actions.unshift({ label: '完成工单', icon: Check, command: 'complete' })
  }

  return actions
}

const handleAction = (command, repair) => {
  if (!command || !repair) return
  emit(command, repair)
}
</script>

<template>
  <el-table
    :data="repairs"
    :row-class-name="getRowClassName"
    class="repair-table"
    empty-text="暂无匹配的报修工单"
    row-key="id"
    @row-click="$emit('select', $event)"
  >
    <el-table-column label="工单号" min-width="130" prop="ticket" />
    <el-table-column label="报修人" min-width="90" prop="reporter" />
    <el-table-column label="实验室" min-width="170" prop="lab" show-overflow-tooltip />
    <el-table-column label="故障设备" min-width="160" prop="device" show-overflow-tooltip />
    <el-table-column label="故障类型" min-width="110" prop="faultType" />

    <el-table-column label="优先级" min-width="85" prop="priority">
      <template #default="{ row }">
        <el-tag :type="getPriorityType(row.priority)" effect="light">{{ row.priority }}</el-tag>
      </template>
    </el-table-column>

    <el-table-column label="状态" min-width="95" prop="status">
      <template #default="{ row }">
        <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>

    <el-table-column label="进度" min-width="130">
      <template #default="{ row }">
        <div class="progress-cell">
          <el-progress :percentage="row.progress" :stroke-width="7" />
        </div>
      </template>
    </el-table-column>

    <el-table-column label="维修人" min-width="90" prop="assignee" />

    <el-table-column fixed="right" label="处理操作" min-width="210">
      <template #default="{ row }">
        <div class="action-cell">
          <el-button
            v-if="getPrimaryAction(row)"
            :icon="getPrimaryAction(row).icon"
            :type="getPrimaryAction(row).type"
            plain
            size="small"
            @click.stop="handleAction(getPrimaryAction(row).command, row)"
          >
            {{ getPrimaryAction(row).label }}
          </el-button>

          <el-dropdown trigger="click" @command="(command) => handleAction(command, row)">
            <el-button plain size="small">
              更多
              <el-icon class="dropdown-icon">
                <ArrowDown />
              </el-icon>
            </el-button>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="action in getMoreActions(row)"
                  :key="`${row.id}-${action.command}`"
                  :command="action.command"
                  :icon="action.icon"
                >
                  {{ action.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.repair-table {
  width: 100%;
  --el-table-border-color: #e5edf6;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #eff6ff;
}

.repair-table :deep(.el-table__row) {
  cursor: pointer;
}

.repair-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.repair-table :deep(th.el-table__cell) {
  height: 46px;
  background: #f8fbff !important;
  color: #475569;
  font-weight: 700;
}

.repair-table :deep(.el-table__body td) {
  height: 58px;
  color: var(--text-primary);
}

.repair-table :deep(.is-active-row > td.el-table__cell) {
  background: #f1f5ff;
}

.progress-cell {
  min-width: 90px;
}

.action-cell {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.action-cell :deep(.el-button) {
  min-width: 72px;
  padding-inline: 12px;
  border-radius: 6px;
  font-weight: 600;
}

.dropdown-icon {
  margin-left: 4px;
}
</style>
