<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'LabEditorDialog',
})

const props = defineProps({
  formModel: {
    type: Object,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  managerOptions: {
    type: Array,
    default: () => [],
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  safetyOptions: {
    type: Array,
    default: () => [],
  },
  statusOptions: {
    type: Array,
    default: () => [],
  },
  title: {
    type: String,
    required: true,
  },
  typeOptions: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['submit', 'update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

</script>

<template>
  <el-dialog v-model="visible" align-center :title="title" width="min(720px, calc(100vw - 32px))">
    <el-form class="lab-form" label-position="top">
      <el-form-item label="实验室编号" required>
        <el-input v-model="formModel.code" clearable placeholder="如 A101" />
      </el-form-item>
      <el-form-item label="实验室名称" required>
        <el-input v-model="formModel.name" clearable placeholder="请输入实验室名称" />
      </el-form-item>
      <el-form-item label="所在楼宇" required>
        <el-input v-model="formModel.building" clearable placeholder="如 信息楼 A 区" />
      </el-form-item>
      <el-form-item label="楼层">
        <el-input v-model="formModel.floor" clearable placeholder="如 3F" />
      </el-form-item>
      <el-form-item label="房间">
        <el-input v-model="formModel.room" clearable placeholder="如 A101" />
      </el-form-item>
      <el-form-item label="负责人" required>
        <el-select
          v-model="formModel.managerUserId"
          class="full-control"
          filterable
          placeholder="请选择负责人"
        >
          <el-option
            v-for="user in managerOptions"
            :key="user.id"
            :label="user.realName || user.username"
            :value="user.id"
          >
            <div class="manager-option">
              <span>{{ user.realName || user.username }}</span>
              <small>{{ user.username }} · {{ user.roleName || '未分配角色' }}</small>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="实验室类型">
        <el-select v-model="formModel.type" class="full-control">
          <el-option v-for="type in typeOptions" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item label="开放状态">
        <el-select v-model="formModel.status" class="full-control">
          <el-option v-for="status in statusOptions" :key="status.label" :label="status.label" :value="status.label" />
        </el-select>
      </el-form-item>
      <el-form-item label="总容量">
        <el-input-number v-model="formModel.capacity" :min="0" class="full-control" />
      </el-form-item>
      <el-form-item label="开放时段">
        <el-input v-model="formModel.openTime" clearable placeholder="如 08:00-22:00" />
      </el-form-item>
      <el-form-item label="安全状态">
        <el-select v-model="formModel.safety" class="full-control" placeholder="请选择安全状态">
          <el-option v-for="safety in safetyOptions" :key="safety" :label="safety" :value="safety" />
        </el-select>
      </el-form-item>
      <el-form-item class="wide-field" label="实验室标签">
        <el-input v-model="formModel.featuresText" clearable placeholder="多个标签用逗号分隔" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :loading="loading" type="primary" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.lab-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px 16px;
}

.lab-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.full-control {
  width: 100%;
}

.wide-field {
  grid-column: 1 / -1;
}

.manager-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.manager-option small {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .lab-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .lab-form {
    grid-template-columns: 1fr;
  }
}
</style>
