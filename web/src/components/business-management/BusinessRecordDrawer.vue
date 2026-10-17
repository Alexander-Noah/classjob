<!--
  Web 管理平台的业务记录抽屉视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
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
    /**
     * 构造当前类型的默认状态。
     * @returns 生成的数组。
     */
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  /**
   * 获取当前操作。
   * @returns props.modelValue 的当前值。
   */
  get: () => props.modelValue,
  /**
   * 设置当前操作。
   * 入参：value（值）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
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
