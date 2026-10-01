<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'LabAssetPreviewPanel',
})

const props = defineProps({
  lab: {
    type: Object,
    default: null,
  },
  assets: {
    type: Array,
    default: () => [],
  },
  dialogMode: {
    type: Boolean,
    default: false,
  },
})

const getTagType = (value, typeMap) => typeMap[value] || 'info'

const statusTagTypes = {
  正常: 'success',
  借用中: 'primary',
  维护中: 'warning',
  故障: 'danger',
  已盘点: 'success',
  停用: 'info',
}

const healthTagTypes = {
  优: 'success',
  良: 'primary',
  关注: 'warning',
  风险: 'danger',
}

const assetOverview = computed(() => {
  const total = props.assets.length
  const onlineCount = props.assets.filter((asset) => asset.online).length
  const alertCount = props.assets.filter((asset) => ['故障', '维护中'].includes(asset.status)).length
  const attentionCount = props.assets.filter((asset) => ['关注', '风险'].includes(asset.health)).length

  return [
    { label: '设备记录', value: String(total), trend: total ? '弹窗内直接查看' : '等待补录设备' },
    {
      label: '在线设备',
      value: String(onlineCount),
      trend: total ? `在线率 ${Math.round((onlineCount / total) * 100)}%` : '暂无在线记录',
    },
    { label: '异常设备', value: String(alertCount), trend: alertCount ? '优先安排巡检' : '运行稳定' },
    { label: '待关注', value: String(attentionCount), trend: attentionCount ? '建议复核健康状态' : '健康状态正常' },
  ]
})

const assetTableMaxHeight = computed(() => (props.dialogMode ? 'min(38vh, 360px)' : undefined))
</script>

<template>
  <el-card :class="['asset-preview-card', { 'is-dialog-mode': dialogMode }]" shadow="never">
    <template #header>
      <div class="asset-header">
        <div>
          <h2>{{ lab ? `${lab.name} 设备信息查看` : '设备信息查看' }}</h2>
          <span>
            {{
              lab
                ? '在弹窗中查看该实验室对应设备数据，不再占用主页面空间。'
                : '先从实验室列表中选择一个实验室。'
            }}
          </span>
        </div>

        <div v-if="lab" class="asset-header-tags">
          <el-tag effect="light" type="primary">实验室 {{ lab.code }}</el-tag>
          <el-tag effect="light">{{ lab.type || '未分类' }}</el-tag>
        </div>
      </div>
    </template>

    <template v-if="lab">
      <div class="asset-overview-grid">
        <div v-for="item in assetOverview" :key="item.label" class="overview-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.trend }}</p>
        </div>
      </div>

      <el-table
        :data="assets"
        class="asset-table"
        empty-text="当前实验室暂无已登记设备"
        :max-height="assetTableMaxHeight"
        row-key="id"
      >
        <el-table-column label="设备编号" min-width="135" prop="code" />

        <el-table-column label="设备名称" min-width="190" prop="name" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="asset-name-cell">
              <strong>{{ row.name }}</strong>
              <span>{{ row.specs || '暂无规格说明' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类" min-width="110" prop="category" />
        <el-table-column label="位置" min-width="140" prop="location" show-overflow-tooltip />
        <el-table-column label="责任人" min-width="90" prop="owner" />

        <el-table-column label="在线状态" min-width="95">
          <template #default="{ row }">
            <el-tag :type="row.online ? 'success' : 'info'" effect="light">
              {{ row.online ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="设备状态" min-width="95" prop="status">
          <template #default="{ row }">
            <el-tag :type="row.tagType || getTagType(row.status, statusTagTypes)" effect="light">
              {{ row.status || '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="健康度" min-width="95" prop="health">
          <template #default="{ row }">
            <el-tag :type="row.healthTagType || getTagType(row.health, healthTagTypes)" effect="light">
              {{ row.health || '未知' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最近盘点" min-width="115" prop="inventoryDate" />
      </el-table>
    </template>

    <el-empty v-else class="asset-empty" description="选择实验室后，这里会展示对应设备信息。" />
  </el-card>
</template>

<style scoped>
.asset-preview-card {
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: var(--surface-color);
  overflow: hidden;
}

.asset-preview-card.is-dialog-mode {
  border: 0;
  border-radius: 0;
}

.asset-preview-card :deep(.el-card__header) {
  min-height: 70px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.asset-preview-card :deep(.el-card__body) {
  padding: 18px 20px 20px;
}

.asset-preview-card.is-dialog-mode :deep(.el-card__header) {
  padding-inline: 2px;
}

.asset-preview-card.is-dialog-mode :deep(.el-card__body) {
  padding: 14px 2px 2px;
}

.asset-preview-card.is-dialog-mode .asset-overview-grid {
  gap: 10px;
  margin-bottom: 12px;
}

.asset-preview-card.is-dialog-mode .overview-item {
  padding: 10px 12px;
}

.asset-preview-card.is-dialog-mode .overview-item strong {
  font-size: 22px;
}

.asset-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.asset-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  font-weight: 800;
}

.asset-header span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.asset-header-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.asset-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.overview-item {
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.overview-item span,
.overview-item p {
  color: #64748b;
  font-size: 12px;
}

.overview-item strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text-primary);
  font-size: 25px;
  font-weight: 800;
  line-height: 1;
}

.overview-item p {
  margin: 0;
}

.asset-table {
  width: 100%;
}

.asset-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.asset-table :deep(.el-table__cell) {
  padding: 12px 0;
}

.asset-name-cell {
  display: grid;
  gap: 4px;
}

.asset-name-cell strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.asset-name-cell span {
  color: var(--text-secondary);
  font-size: 12px;
}

.asset-empty {
  padding-block: 18px;
}

@media (max-width: 1180px) {
  .asset-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .asset-header {
    flex-direction: column;
  }

  .asset-overview-grid {
    grid-template-columns: 1fr;
  }

  .asset-preview-card :deep(.el-card__body) {
    padding: 14px;
  }
}
</style>
