/*
 * 主题的 Pinia 状态模块，集中维护桌面页面共用状态。
 * 主要导出：useThemeStore。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', /** 处理 defineStore 调用中的回调逻辑。 */ () => {
  const isDark = ref(false)

  /**
   * 切换主题。
   */
  function toggleTheme() {
    isDark.value = !isDark.value
  }

  return { isDark, toggleTheme }
})