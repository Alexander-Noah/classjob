<!--
  桌面端的src App视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup lang="ts">
import { zhCN, dateZhCN, darkTheme } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { LogicalSize, getCurrentWindow } from '@tauri-apps/api/window'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { useMonitorStore } from '@/stores/monitor'
import { useActivationStore } from '@/stores/activation'
import MainLayout from '@/layouts/MainLayout.vue'
import LoginDialog from '@/components/LoginDialog.vue'
import ActivationPage from '@/components/ActivationPage.vue'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const monitorStore = useMonitorStore()
const activationStore = useActivationStore()
const theme = computed(/** 计算派生状态。 */ () => themeStore.isDark ? darkTheme : null)
const activationPromptDismissed = ref(false)
const loginVisible = computed(/** 计算派生状态。 */ () => !authStore.isAuthenticated)
const activationVisible = computed(/** 计算派生状态。 */ () => {
  if (!authStore.isAuthenticated) return false
  if (!activationStore.isLicensed) return !activationPromptDismissed.value || !activationStore.isTrialActive
  return false
})
const appVisible = computed(/** 计算派生状态。 */ () => {
  if (!authStore.isAuthenticated) return false
  if (activationStore.isLicensed) return true
  return activationStore.isTrialActive && activationPromptDismissed.value
})
const loginLoading = ref(false)
const loginError = ref('')

authStore.hydrate()
monitorStore.hydrate()
activationStore.hydrate()

const LOGIN_WINDOW_SIZE = new LogicalSize(520, 500)
const ACTIVATION_WINDOW_SIZE = new LogicalSize(560, 620)
const APP_WINDOW_SIZE = new LogicalSize(1440, 810)
let licenseTimer: number | undefined

/**
 * 同步窗口大小。
 * 入参：view（视图）。
 */
async function syncWindowSize(view: 'login' | 'activation' | 'app') {
  if (typeof window === 'undefined' || !('__TAURI_INTERNALS__' in window)) return

  try {
    const appWindow = getCurrentWindow()
    if (view === 'app') {
      await appWindow.setMinSize(new LogicalSize(800, 600))
      await appWindow.setResizable(true)
      await appWindow.setSize(APP_WINDOW_SIZE)
    } else {
      const targetSize = view === 'login' ? LOGIN_WINDOW_SIZE : ACTIVATION_WINDOW_SIZE
      await appWindow.setMinSize(targetSize)
      await appWindow.setSize(targetSize)
      await appWindow.setResizable(false)
    }
    await appWindow.center()
  } catch {
    // Browser preview has no native window to resize.
  }
}

watch(
  /**
   * 响应被监听状态的变化。
   */
  () => [loginVisible.value, activationVisible.value, appVisible.value] as const,
  /**
   * 响应被监听状态的变化。
   */
  () => {
    const view = appVisible.value ? 'app' : activationVisible.value ? 'activation' : 'login'
    void syncWindowSize(view)
  },
  { immediate: true }
)

/**
 * 处理登录。
 * 入参：payload（载荷）。
 */
async function handleLogin(payload: { username: string; password: string }) {
  loginError.value = ''
  loginLoading.value = true
  try {
    const ok = await authStore.login(payload.username, payload.password)
    if (!ok) {
      loginError.value = '请输入账号和密码'
      return
    }
    activationPromptDismissed.value = false
  } catch (error) {
    loginError.value = error instanceof Error ? error.message : '登录失败，请检查后端服务'
  } finally {
    loginLoading.value = false
  }
}

/**
 * 处理退出登录。
 */
function handleLogout() {
  activationPromptDismissed.value = false
  void authStore.logout()
}

/**
 * 处理ActivationContinue。
 */
function handleActivationContinue() {
  if (!activationStore.isTrialActive) return
  activationPromptDismissed.value = true
}

onMounted(/** 在组件挂载后初始化相关状态。 */ () => {
  licenseTimer = window.setInterval(/** 执行周期安排的任务。 */ () => {
    activationStore.verifyStoredActivation()
  }, 60000)
  activationStore.verifyStoredActivation()
})

onBeforeUnmount(/** 在组件卸载前释放相关资源。 */ () => {
  if (licenseTimer) {
    window.clearInterval(licenseTimer)
  }
})
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN" :theme="theme">
    <n-global-style />
    <n-loading-bar-provider>
      <n-message-provider>
        <n-notification-provider>
          <n-dialog-provider>
            <LoginDialog
              v-if="loginVisible"
              :visible="loginVisible"
              :loading="loginLoading"
              :error-message="loginError"
              @submit="handleLogin"
              @cancel="handleLogout"
            />
            <ActivationPage v-if="activationVisible" @logout="handleLogout" @continue="handleActivationContinue" />
            <MainLayout v-if="appVisible" />
          </n-dialog-provider>
        </n-notification-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>
