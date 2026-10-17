export const glassButtonSelector = 'button, [role="button"], .el-pager > li'

/** 复合控件只增强内部操作按钮，避免父滤镜改变绝对定位子按钮的包含块。 */
export function isGlassButtonTarget(element) {
  if (element.matches('button, .el-pager > li')) return true
  return element.matches('[role="button"]') && !element.querySelector('button, [role="button"]')
}
