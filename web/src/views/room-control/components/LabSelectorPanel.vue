<script setup>
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

defineOptions({
  name: 'LabSelectorPanel',
})

const props = defineProps({
  labs: {
    type: Array,
    default: () => [],
  },
  selectedLabId: {
    type: String,
    default: '',
  },
})

defineEmits(['select'])

const keyword = ref('')
const statusFilter = ref('all')

const statusOptions = computed(() => {
  const statusMap = new Map()

  props.labs.forEach((lab) => {
    statusMap.set(lab.status, {
      label: lab.status,
      type: lab.statusType,
    })
  })

  return [...statusMap.values()]
})

const visibleLabs = computed(() => {
  const searchText = keyword.value.trim().toLowerCase()

  return props.labs.filter((lab) => {
    const matchesKeyword =
      !searchText ||
      [lab.name, lab.location, lab.capacity, lab.door.doorState, lab.sensor.temperature].some((value) =>
        String(value).toLowerCase().includes(searchText),
      )
    const matchesStatus = statusFilter.value === 'all' || lab.status === statusFilter.value

    return matchesKeyword && matchesStatus
  })
})
</script>

<template>
  <el-card class="module-card" shadow="never">
    <template #header>
      <div class="card-header">
        <div>
          <h2>机房列表</h2>
          <span>当前显示 {{ visibleLabs.length }} / {{ labs.length }} 间</span>
        </div>
      </div>
    </template>

    <div class="selector-tools">
      <el-input v-model="keyword" clearable placeholder="搜索机房">
        <template #prefix>
          <el-icon>
            <Search />
          </el-icon>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="全部状态">
        <el-option label="全部状态" value="all" />
        <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
      </el-select>
    </div>

    <el-scrollbar class="lab-selector-scroll">
      <div class="lab-selector">
        <button
          v-for="lab in visibleLabs"
          :key="lab.id"
          class="lab-select-card"
          :class="{ active: lab.id === selectedLabId }"
          type="button"
          @click="$emit('select', lab)"
        >
          <span class="lab-card-top">
            <strong>{{ lab.name }}</strong>
            <el-tag :type="lab.statusType" effect="light">{{ lab.status }}</el-tag>
          </span>
          <span class="lab-card-meta">{{ lab.location }} · {{ lab.capacity }}</span>
          <span class="lab-card-data">
            <span>门禁 {{ lab.door.doorState }}</span>
            <span>温度 {{ lab.sensor.temperature }}</span>
            <span>烟雾 {{ lab.sensor.smoke }}ppm</span>
          </span>
        </button>
        <el-empty v-if="!visibleLabs.length" description="未匹配到机房" />
      </div>
    </el-scrollbar>
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
  min-height: 62px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.module-card :deep(.el-card__body) {
  padding: 14px;
}

.card-header {
  display: flex;
  align-items: center;
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

.selector-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 116px;
  gap: 10px;
  margin-bottom: 14px;
}

.selector-tools :deep(.el-input__wrapper),
.selector-tools :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.selector-tools :deep(.el-input__wrapper:hover),
.selector-tools :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.lab-selector {
  display: grid;
  gap: 10px;
  padding-right: 6px;
}

.lab-selector-scroll {
  height: min(620px, calc(100vh - 300px));
  min-height: 320px;
}

.lab-select-card {
  display: block;
  min-width: 0;
  padding: 13px;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease;
}

.lab-select-card:hover,
.lab-select-card:focus-visible,
.lab-select-card.active {
  border-color: #2563eb;
  outline: none;
  background: #eff6ff;
}

.lab-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.lab-card-top strong {
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lab-card-meta {
  display: block;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.lab-card-data {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.lab-card-data span {
  min-width: 0;
  padding: 8px;
  overflow: hidden;
  border-radius: 7px;
  color: #475569;
  background: #f8fbff;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .selector-tools {
    grid-template-columns: 1fr;
  }

  .lab-selector-scroll {
    height: auto;
    max-height: 520px;
  }

  .lab-card-data {
    grid-template-columns: 1fr;
  }
}
</style>
