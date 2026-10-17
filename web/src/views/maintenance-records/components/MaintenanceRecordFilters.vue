<!--
  Web 管理平台的维护记录筛选表单视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
defineOptions({
  name: 'MaintenanceRecordFilters',
})

defineProps({
  filters: {
    type: Object,
    required: true,
  },
  options: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['reset'])
</script>

<template>
  <div class="record-filters">
    <el-input v-model="filters.keyword" clearable placeholder="搜索记录编号、内容或结果" />
    <el-select v-model="filters.lab" clearable filterable placeholder="全部实验室">
      <el-option v-for="item in options.labs" :key="item" :label="item" :value="item" />
    </el-select>
    <el-select v-model="filters.device" clearable filterable placeholder="全部设备">
      <el-option v-for="item in options.devices" :key="item" :label="item" :value="item" />
    </el-select>
    <el-select v-model="filters.maintenanceType" clearable filterable placeholder="全部维护类型">
      <el-option v-for="item in options.types" :key="item" :label="item" :value="item" />
    </el-select>
    <el-select v-model="filters.maintainer" clearable filterable placeholder="全部维护人员">
      <el-option v-for="item in options.maintainers" :key="item" :label="item" :value="item" />
    </el-select>
    <el-date-picker
      v-model="filters.dateRange"
      type="daterange"
      value-format="YYYY-MM-DD"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
    />
    <el-button @click="emit('reset')">重置</el-button>
  </div>
</template>

<style scoped>
.record-filters { display: flex; flex-wrap: wrap; gap: 10px; }
.record-filters .el-input, .record-filters .el-select { width: 180px; }
@media (max-width: 680px) { .record-filters { display: grid; grid-template-columns: 1fr; } .record-filters .el-input, .record-filters .el-select, .record-filters .el-date-editor { width: 100%; } }
</style>
