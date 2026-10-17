<!--
  Web 管理平台的EventRecordPanel视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'EventRecordPanel',
})

const props = defineProps({
  categoryFilter: {
    type: String,
    default: 'all',
  },
  categoryOptions: {
    type: Array,
    default: () => [],
  },
  keyword: {
    type: String,
    default: '',
  },
  records: {
    type: Array,
    default: () => [],
  },
  roomFilter: {
    type: String,
    default: 'all',
  },
  roomOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:categoryFilter', 'update:keyword', 'update:roomFilter'])

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value),
})

const roomFilterModel = computed({
  get: () => props.roomFilter,
  set: (value) => emit('update:roomFilter', value),
})

const categoryFilterModel = computed({
  get: () => props.categoryFilter,
  set: (value) => emit('update:categoryFilter', value),
})
</script>

<template>
  <el-card class="module-card" shadow="never" :body-style="{ padding: '0' }">
    <template #header>
      <div class="card-header">
        <div>
          <h2>智能控制事件</h2>
          <span>灯光、温控、门禁、摄像头、传感器与场景联动记录</span>
        </div>
        <div class="record-tools">
          <el-select v-model="roomFilterModel" class="record-select" placeholder="全部机房">
            <el-option label="全部机房" value="all" />
            <el-option v-for="room in roomOptions" :key="room.value" :label="room.label" :value="room.value" />
          </el-select>
          <el-select v-model="categoryFilterModel" class="record-select" placeholder="全部类型">
            <el-option label="全部类型" value="all" />
            <el-option
              v-for="category in categoryOptions"
              :key="category.value"
              :label="category.label"
              :value="category.value"
            />
          </el-select>
          <el-input v-model="keywordModel" class="record-search" clearable placeholder="搜索记录">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
      </div>
    </template>

    <el-table :data="records" class="record-table" empty-text="暂无记录" max-height="420">
      <el-table-column label="时间" min-width="110" prop="time" />
      <el-table-column label="机房" min-width="160" prop="room" />
      <el-table-column label="类型" min-width="120" prop="category" />
      <el-table-column label="事件" min-width="180" prop="event" />
      <el-table-column label="状态" min-width="90" prop="status">
        <template #default="{ row }">
          <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>
.module-card {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.module-card :deep(.el-card__header) {
  min-height: 68px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.card-header,
.record-tools {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 16px;
}

.card-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.card-header span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.record-tools {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.record-tools :deep(.el-input__wrapper),
.record-tools :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.record-tools :deep(.el-input__wrapper:hover),
.record-tools :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.record-select {
  width: 140px;
}

.record-search {
  width: min(240px, 46vw);
}

.record-table {
  width: 100%;
  --el-table-border-color: #e5edf6;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #eff6ff;
}

.record-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.record-table :deep(th.el-table__cell) {
  height: 46px;
  background: #f8fbff !important;
  color: #475569;
  font-weight: 700;
}

.record-table :deep(.el-table__body td) {
  height: 52px;
}

@media (max-width: 900px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .record-tools {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .record-select,
  .record-search {
    width: 100%;
  }
}
</style>
