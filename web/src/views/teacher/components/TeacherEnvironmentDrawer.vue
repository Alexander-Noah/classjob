<!--
  Web 管理平台的教师工作台抽屉视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'TeacherEnvironmentDrawer',
})

const props = defineProps({
  environment: {
    type: Object,
    default: null,
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['confirm', 'return', 'update:modelValue'])

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
  <el-drawer v-model="visible" title="课程环境详情" size="520px">
    <template v-if="environment">
      <el-descriptions :column="1" border class="environment-descriptions">
        <el-descriptions-item label="课程">{{ environment.course }}</el-descriptions-item>
        <el-descriptions-item label="实验室">{{ environment.lab }}</el-descriptions-item>
        <el-descriptions-item label="上课时间">{{ environment.schedule }}</el-descriptions-item>
        <el-descriptions-item label="管理员">{{ environment.manager }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="environment.tagType || 'warning'" effect="light">{{ environment.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="软件清单">{{ environment.software }}</el-descriptions-item>
      </el-descriptions>

      <section class="drawer-section">
        <div class="section-title">
          <h3>准备步骤</h3>
          <span>按当前环境配置进度展示</span>
        </div>
        <el-steps :active="environment.step" direction="vertical" finish-status="success">
          <el-step
            v-for="item in environment.steps"
            :key="item.title"
            :title="item.title"
            :description="item.description"
          />
        </el-steps>
      </section>

      <section class="drawer-section">
        <div class="section-title">
          <h3>截图与说明</h3>
          <span>教师可据此远程确认环境</span>
        </div>
        <div class="asset-list">
          <article v-for="asset in environment.assets" :key="asset.title" class="asset-card">
            <strong>{{ asset.title }}</strong>
            <p>{{ asset.description }}</p>
          </article>
        </div>
      </section>

      <div class="drawer-actions">
        <el-button v-if="environment.status !== '已确认'" @click="$emit('return', environment)">退回调整</el-button>
        <el-button v-if="environment.status !== '已确认'" type="primary" @click="$emit('confirm', environment)">确认环境</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.environment-descriptions {
  margin-bottom: 20px;
}

.drawer-section + .drawer-section {
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

.asset-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.asset-card {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #f8fbff;
}

.asset-card strong {
  color: var(--text-primary);
  font-size: 14px;
}

.asset-card p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}
</style>
