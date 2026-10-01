<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'BusinessRecordDrawer',
})

const props = defineProps({
  activeRecord: {
    type: Object,
    default: null,
  },
  getRowTagType: {
    type: Function,
    required: true,
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  recordName: {
    type: String,
    default: '记录',
  },
  tableColumns: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<template>
  <el-drawer v-model="visible" :title="`${recordName}详情`" size="420px">
    <div v-if="activeRecord" class="detail-list">
      <div v-for="column in tableColumns" :key="column.prop" class="detail-item">
        <span>{{ column.label }}</span>
        <strong v-if="column.type !== 'tag'">{{ activeRecord[column.prop] }}</strong>
        <el-tag v-else :type="getRowTagType(activeRecord)" effect="light">
          {{ activeRecord[column.prop] }}
        </el-tag>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.detail-list {
  display: grid;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.detail-item span {
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-item strong {
  min-width: 0;
  color: var(--text-primary);
  font-size: 14px;
  text-align: right;
  word-break: break-word;
}
</style>
