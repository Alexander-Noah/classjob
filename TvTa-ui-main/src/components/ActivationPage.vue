<script setup lang="ts">
import { computed, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { useActivationStore } from '@/stores/activation'

const emit = defineEmits<{
  (event: 'logout'): void
  (event: 'continue'): void
}>()

const message = useMessage()
const activationStore = useActivationStore()
const activationCode = ref('')
const activating = ref(false)

activationStore.hydrate()

const canActivate = computed(() => Boolean(activationCode.value.trim()) && !activating.value)
const activationDescription = computed(() => {
  if (activationStore.isTrialExpired) return '30 天试用已结束，请输入正式激活码后继续使用。'
  if (activationStore.trialUsed) return '本机已经使用过试用，请输入正式激活码完成授权。'
  return '请输入激活码完成授权，也可以先试用 30 天。'
})

async function handleActivate() {
  if (!canActivate.value) return

  activating.value = true
  try {
    const ok = await activationStore.activate(activationCode.value)
    if (!ok) {
      message.error('激活码无效、未绑定实验室、已停用、已过期或设备数量已达上限')
      return
    }
    activationCode.value = ''
    message.success('客户端已激活')
  } catch (error: any) {
    message.error(error?.message || '激活失败，请检查云端服务连接')
  } finally {
    activating.value = false
  }
}

function handleStartTrial() {
  const ok = activationStore.startTrial()
  if (!ok) {
    message.warning('本机已使用过 30 天试用，请输入激活码')
    return
  }
  message.success('已开启 30 天试用')
}

</script>

<template>
  <main class="activation-shell">
    <section class="activation-panel">
      <header class="activation-header">
        <div class="brand-mark">
          <span class="i-carbon-screen-map" />
        </div>
        <div>
          <h1>客户端激活</h1>
          <p>{{ activationDescription }}</p>
        </div>
      </header>

      <div class="status-line" :class="{ 'status-line--expired': activationStore.trialUsed }">
        <span :class="activationStore.trialUsed ? 'i-carbon-warning' : 'i-carbon-information'" />
        {{ activationStore.statusText }}
      </div>

      <n-form class="activation-form" label-placement="top">
        <n-form-item label="激活码">
          <n-input
            v-model:value="activationCode"
            size="large"
            type="password"
            show-password-on="click"
            placeholder="请输入激活码"
            @keyup.enter="handleActivate"
          />
        </n-form-item>
        <n-button
          class="primary-button"
          type="primary"
          size="large"
          block
          :disabled="!canActivate"
          :loading="activating"
          @click="handleActivate"
        >
          激活客户端
        </n-button>
      </n-form>

      <div class="trial-row">
        <span>{{ activationStore.canStartTrial ? '可试用 30 天，结束后必须激活。' : '本机已使用过试用，不能再次开启。' }}</span>
        <n-button
          secondary
          type="primary"
          :disabled="!activationStore.canStartTrial || activating"
          @click="handleStartTrial"
        >
          开始试用
        </n-button>
      </div>

      <footer class="activation-actions">
        <n-button tertiary @click="emit('logout')">返回登录</n-button>
        <n-button
          v-if="activationStore.isTrialActive"
          secondary
          type="primary"
          @click="emit('continue')"
        >
          暂不激活，进入系统
        </n-button>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.activation-shell {
  display: grid;
  width: 100vw;
  height: 100vh;
  place-items: center;
  overflow: hidden;
  box-sizing: border-box;
  padding: 24px;
  background: #ffffff;
}

.activation-panel {
  width: min(460px, 100%);
  background: #ffffff;
}

.activation-header {
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

.activation-header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0;
}

.activation-header p {
  margin: 10px 0 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.55;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  background: #f8fafc;
  color: #475467;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.4;
}

.status-line--expired {
  background: #fff7ed;
  color: #b45309;
}

.activation-form {
  margin-top: 18px;
}

.activation-form :deep(.n-form-item-label) {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.activation-form :deep(.n-input) {
  border-radius: 6px;
}

.trial-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
  color: #667085;
  font-size: 13px;
  line-height: 1.45;
}

.activation-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 22px;
}

.primary-button {
  min-width: 76px;
  height: 40px;
  border-radius: 6px;
  font-size: 15px;
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
