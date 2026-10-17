/*
 * locales index公共逻辑模块，供相关视图或服务复用。
 * 主要导出：t。
 */
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN
  }
})

export default i18n

/**
 * t的处理逻辑。
 * 入参：key（键）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const t = (key: string) => i18n.global.t(key)
