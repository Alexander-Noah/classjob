/*
 * 主题的 Pinia 状态模块，集中维护桌面页面共用状态。
 * 主要导出：useThemeStore。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function toggleTheme() {
    isDark.value = !isDark.value
  }

  return { isDark, toggleTheme }
})