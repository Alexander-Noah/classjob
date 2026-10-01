<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'MaintenanceWorkOrderDrawer',
})

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  workOrder: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
</script>

<template>
  <el-drawer v-model="visible" title="工单详情" size="520px">
    <template v-if="workOrder">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="工单号">{{ workOrder.ticket }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ workOrder.device }}</el-descriptions-item>
        <el-descriptions-item label="实验室">{{ workOrder.lab }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="workOrder.priorityType || 'warning'" effect="light">{{ workOrder.priority }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="workOrder.tagType || 'warning'" effect="light">{{ workOrder.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="故障描述">{{ workOrder.fault }}</el-descriptions-item>
      </el-descriptions>

      <section class="section-block">
        <div class="section-title">
          <h3>处理步骤</h3>
          <span>跟随工单处理进度同步更新</span>
        </div>
        <el-steps :active="workOrder.step" direction="vertical" finish-status="success">
          <el-step title="接收派单" description="管理员已分派至当前维修人员" />
          <el-step title="到场排查" description="检查设备、环境和故障影响范围" />
          <el-step title="维修处理" description="执行维修或更换部件，并记录过程" />
          <el-step title="提交复核" description="回填结果，等待管理员闭环确认" />
        </el-steps>
      </section>

      <section class="section-block">
        <div class="section-title">
          <h3>处理记录</h3>
          <span>用于展示维修过程留痕</span>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="item in workOrder.logs"
            :key="`${workOrder.ticket}-${item.time}`"
            :timestamp="item.time"
            :type="item.type || 'primary'"
          >
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>
      </section>
    </template>
  </el-drawer>
</template>

<style scoped>
.section-block {
  margin-top: 22px;
}

.section-title h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
}

.section-title span {
  display: block;
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
}

.section-block :deep(.el-timeline) {
  padding-left: 0;
  margin-top: 12px;
}
</style>
