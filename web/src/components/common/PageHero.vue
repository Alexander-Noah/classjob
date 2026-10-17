<!--
  Web 管理平台的PageHero视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { ArrowRight } from '@element-plus/icons-vue'

defineOptions({
  name: 'PageHero',
})

defineProps({
  actions: {
    type: Array,
    default: () => [],
  },
  badge: {
    type: String,
    default: '业务管理',
  },
  pageDescription: {
    type: String,
    default: '',
  },
  pageIcon: {
    type: [Object, Function],
    required: true,
  },
  pageTitle: {
    type: String,
    required: true,
  },
})

defineEmits(['action', 'back'])
</script>

<template>
  <el-card class="module-hero" shadow="never">
    <div class="module-hero-content">
      <div class="module-title-group">
        <span class="module-icon" aria-hidden="true">
          <component :is="pageIcon" />
        </span>
        <div class="module-heading-copy">
          <div class="module-meta">
            <el-tag effect="light">{{ badge }}</el-tag>
          </div>
          <h1>{{ pageTitle }}</h1>
        </div>
      </div>

      <div class="module-actions">
        <el-button v-for="action in actions" :key="action.label" :type="action.type" @click="$emit('action', action)">
          {{ action.label }}
        </el-button>
        <el-button @click="$emit('back')">
          返回首页
          <el-icon class="button-icon">
            <ArrowRight />
          </el-icon>
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.module-hero {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.module-hero :deep(.el-card__body) {
  padding: 22px;
}

.module-hero-content,
.module-title-group,
.module-actions,
.module-meta {
  display: flex;
  align-items: center;
}

.module-hero-content {
  justify-content: space-between;
  gap: 22px;
}

.module-title-group {
  gap: 16px;
  min-width: 0;
}

.module-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 30px;
}

.module-heading-copy {
  min-width: 0;
}

.module-meta {
  gap: 10px;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.module-heading-copy h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0;
}

.module-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.module-actions .el-button + .el-button {
  margin-left: 0;
}

.button-icon {
  margin-left: 4px;
}

@media (max-width: 900px) {
  .module-hero-content {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .module-title-group {
    align-items: flex-start;
  }

  .module-actions {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
