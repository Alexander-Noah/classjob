<script setup>
import { Edit, FolderOpened, Tickets } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabListTable',
})

const props = defineProps({
  labs: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: String,
    default: '',
  },
})

defineEmits(['edit', 'reservation-qr', 'select', 'view-assets'])

const getRowClassName = ({ row }) => (row.id === props.selectedId ? 'is-active-row' : '')
</script>

<template>
  <el-table
    :data="labs"
    border
    fit
    highlight-current-row
    :row-class-name="getRowClassName"
    stripe
    class="lab-table"
    empty-text="暂无匹配的实验室"
    row-key="id"
    @row-click="$emit('select', $event)"
  >
    <el-table-column label="编号" min-width="80" prop="code" />

    <el-table-column label="实验室名称" min-width="220" prop="name" show-overflow-tooltip>
      <template #default="{ row }">
        <div class="lab-name-cell">
          <strong>{{ row.name }}</strong>
          <span>{{ row.building }} · {{ row.room }}</span>
        </div>
      </template>
    </el-table-column>

    <el-table-column label="楼层" min-width="80" prop="floor" />
    <el-table-column label="类型" min-width="110" prop="type" show-overflow-tooltip />
    <el-table-column label="容量" min-width="80" prop="capacity" />
    <el-table-column label="负责人" min-width="90" prop="manager" />

    <el-table-column label="状态" min-width="100" prop="status">
      <template #default="{ row }">
        <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>

    <el-table-column label="操作" min-width="280" align="center" header-align="center">
      <template #default="{ row }">
        <el-space :size="8" wrap>
          <el-button plain size="small" type="primary" :icon="FolderOpened" @click.stop="$emit('view-assets', row)">
            设备信息
          </el-button>

          <el-button plain size="small" type="success" :icon="Tickets" @click.stop="$emit('reservation-qr', row)">
            预约码
          </el-button>

          <el-button plain size="small" :icon="Edit" @click.stop="$emit('edit', row)">
            编辑
          </el-button>
        </el-space>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.lab-table {
  width: 100%;
}

.lab-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.lab-table :deep(.el-table__cell) {
  padding: 12px 0;
}

.lab-table :deep(.el-table__row) {
  cursor: pointer;
}

.lab-table :deep(.is-active-row > td.el-table__cell) {
  background: #eff6ff;
}

.lab-table :deep(.is-active-row .lab-name-cell strong) {
  color: var(--primary-color);
}

.lab-name-cell {
  display: grid;
}

.lab-name-cell {
  gap: 4px;
}

.lab-name-cell strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
}

.lab-name-cell span {
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
