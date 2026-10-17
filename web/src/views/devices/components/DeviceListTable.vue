<!--
  Web 管理平台的设备管理列表视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>

defineOptions({
  name: 'DeviceListTable',
})

defineProps({
  devices: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['delete', 'edit', 'inventory', 'selectionChange', 'transfer', 'view'])

const getStatusType = (status) => {
  if (status === '正常') return 'success'
  if (status === '维修中') return 'warning'
  if (status === '已报废') return 'info'
  return 'info'
}

const displayLabName = (row = {}) => row.labName || row.laboratoryName || row.lab || row.roomNo || row.location || '未绑定'

const displayLocation = (row = {}) => row.roomNo || row.location || row.installLocation || '未绑定'
</script>

<template>
  <el-table
    :data="devices"
    :fit="true"
    :scrollbar-always-on="true"
    border
    class="device-table"
    empty-text="暂无设备资产数据"
    row-key="id"
    stripe
    @selection-change="$emit('selectionChange', $event)"
  >
    <el-table-column type="selection" width="46" fixed="left" />
    <el-table-column label="资产编号" prop="assetNo" show-overflow-tooltip min-width="150" />
    <el-table-column label="设备名称" prop="deviceName" show-overflow-tooltip min-width="260">
      <template #default="{ row }">
        <span class="device-name-text">{{ row.deviceName }}</span>
      </template>
    </el-table-column>
    <el-table-column label="设备分类" prop="category" show-overflow-tooltip min-width="124" />
    <el-table-column label="所属实验室" prop="labName" show-overflow-tooltip min-width="220">
      <template #default="{ row }">
        <el-tag v-if="!row.labName && !row.laboratoryName && !row.lab" effect="plain" type="info">
          {{ displayLabName(row) }}
        </el-tag>
        <span v-else>{{ displayLabName(row) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="安装位置" prop="roomNo" show-overflow-tooltip min-width="130">
      <template #default="{ row }">
        <el-tag v-if="displayLocation(row) === '未绑定'" effect="plain" type="info">未绑定</el-tag>
        <span v-else>{{ displayLocation(row) }}</span>
      </template>
    </el-table-column>
    <el-table-column align="right" label="数量" prop="quantity" min-width="84" />
    <el-table-column label="单位" prop="unit" min-width="72" />
    <el-table-column label="状态" prop="status" min-width="98">
      <template #default="{ row }">
        <el-tag :type="getStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="260" fixed="right" align="center">
      <template #default="{ row }">
        <div class="table-actions">
          <el-button link type="primary" @click.stop="$emit('view', row)">查看</el-button>
          <el-button link type="primary" @click.stop="$emit('edit', row)">编辑</el-button>
          <el-button link type="primary" @click.stop="$emit('inventory', row)">盘点</el-button>
          <el-button link type="primary" @click.stop="$emit('transfer', row)">调拨</el-button>
          <el-button link type="danger" @click.stop="$emit('delete', row)">删除</el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.device-table {
  width: 100%;
  min-width: 1180px;
}

.device-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.device-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.device-name-text {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  height: 24px;
  margin-left: 0;
  padding: 0;
}
</style>
