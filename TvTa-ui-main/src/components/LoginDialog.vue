<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  (event: 'submit', payload: { username: string; password: string }): void
  (event: 'cancel'): void
}>()

const username = ref('admin')
const password = ref('')

const canSubmit = computed(() => Boolean(username.value.trim() && password.value.trim()))

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      password.value = ''
      return
    }
    username.value = 'admin'
    password.value = ''
  },
  { immediate: true }
)

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', {
    username: username.value,
    password: password.value
  })
}

function handleCancel() {
  emit('cancel')
}
</script>

<template>
  <main v-if="props.visible" class="login-shell">
    <section class="login-panel">
      <header class="login-header">
        <div class="brand-mark">
          <span class="i-carbon-screen-map" />
        </div>
        <div>
          <h1>系统登录</h1>
          <p>智云实验室教师端</p>
        </div>
      </header>

      <n-form class="login-form" label-placement="top">
        <n-form-item label="用户名">
          <n-input v-model:value="username" size="large" placeholder="admin / teacher" @keyup.enter="handleSubmit" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input
            v-model:value="password"
            size="large"
            type="password"
            show-password-on="click"
            placeholder="请输入密码"
            @keyup.enter="handleSubmit"
          />
        </n-form-item>
        <div v-if="props.errorMessage" class="login-error">
          {{ props.errorMessage }}
        </div>
        <div class="login-hint">
          默认演示账号：admin / admin123
        </div>
      </n-form>

      <footer class="login-actions">
        <n-button class="exit-button" tertiary @click="handleCancel">退出</n-button>
        <n-button
          class="primary-button"
          type="primary"
          :disabled="!canSubmit"
          :loading="props.loading"
          @click="handleSubmit"
        >
          登录
        </n-button>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login-shell {
  display: grid;
  width: 100vw;
  height: 100vh;
  place-items: center;
  overflow: hidden;
  box-sizing: border-box;
  padding: 24px;
  background: #ffffff;
}

.login-panel {
  width: min(420px, 100%);
  background: #ffffff;
}

.login-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding-bottom: 22px;
}

.brand-mark {
  display: flex;
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #ecfdf5;
  color: #52a36f;
  font-size: 25px;
}

.login-header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0;
}

.login-header p {
  margin: 10px 0 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.55;
}

.login-form :deep(.n-form-item-label) {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.login-form :deep(.n-input) {
  border-radius: 6px;
}

.login-error {
  margin-bottom: 12px;
  border-radius: 8px;
  background: #fff1f2;
  color: #e11d48;
  padding: 10px 12px;
  font-size: 13px;
}

.login-hint {
  color: #98a2b3;
  font-size: 13px;
}

.login-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 22px;
}

.exit-button,
.primary-button {
  min-width: 76px;
  height: 40px;
  border-radius: 6px;
  font-size: 15px;
}

.primary-button {
  --n-color: #9fcab1 !important;
  --n-color-hover: #90bea4 !important;
  --n-color-pressed: #82b096 !important;
  --n-color-focus: #9fcab1 !important;
  --n-border: 1px solid #9fcab1 !important;
  --n-border-hover: 1px solid #90bea4 !important;
  --n-border-pressed: 1px solid #82b096 !important;
  --n-border-focus: 1px solid #9fcab1 !important;
}
</style>
