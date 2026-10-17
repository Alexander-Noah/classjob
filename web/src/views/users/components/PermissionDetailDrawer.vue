<!--
  Web 管理平台的权限抽屉视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  组件输入通过 defineProps 声明，父子交互以 props/emits 契约为准。
-->
<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'PermissionDetailDrawer',
})

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  user: {
    type: Object,
    default: null,
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

const permissionScopes = computed(/** 计算派生状态。 */ () => props.user?.permissions || [])
</script>

<template>
  <el-drawer v-model="visible" title="用户详情" size="520px">
    <template v-if="user">
      <section class="drawer-section">
        <h3>基本信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="姓名">{{ user.name }}</el-descriptions-item>
          <el-descriptions-item label="账号 / 工号">{{ user.account }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ user.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ user.department }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="drawer-section">
        <h3>角色信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="当前角色">
            <el-tag effect="light">{{ user.role }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限范围">
            <div class="permission-list">
              <div v-for="group in permissionScopes" :key="group.group" class="permission-item">
                <strong>{{ group.group }}</strong>
                <span>{{ group.items.join('、') }}</span>
              </div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="drawer-section">
        <h3>账号状态</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="状态">
            <el-tag :type="user.enabled ? 'success' : 'info'" effect="light">
              {{ user.enabled ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ user.lastLogin }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ user.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="drawer-section">
        <h3>最近操作记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="item in user.logs"
            :key="`${user.id}-${item.time}-${item.content}`"
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
.drawer-section + .drawer-section {
  margin-top: 20px;
}

.drawer-section h3 {
  margin: 0 0 12px;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.permission-list {
  display: grid;
  gap: 8px;
}

.permission-item {
  display: grid;
  gap: 4px;
}

.permission-item strong {
  color: var(--text-primary);
  font-size: 13px;
}

.permission-item span {
  color: var(--text-secondary);
  line-height: 1.6;
}

:deep(.el-timeline) {
  padding-left: 0;
}
</style>
