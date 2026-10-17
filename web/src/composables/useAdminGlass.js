/*
 * 玻璃主题的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useAdminGlass。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { attachLiquidGlass } from './useLiquidGlass'
import { isGlassResourceNode } from './glassScrollActivity'

/**
 * 在管理布局内跟踪路由动态插入的卡片，同时纳入 Teleport 到 body 的弹窗。
 * Map 保证同一个节点只挂载一次折射效果，离开页面时统一释放。
 * 按钮交由应用根组件统一管理，不对表格单元格或文字创建滤镜。
 */
export function useAdminGlass() {
  const glassRoot = ref(null)
  const panels = new Map()
  let observer
  let frame
  const sync = () => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    const current = new Set(root.querySelectorAll('.el-card, .page-panel, .hero-actions'))
    document.querySelectorAll('.el-dialog, .el-message-box').forEach((element) => {
      current.add(element)
    })
    for (const [element, cleanup] of panels) {
      if (!current.has(element)) {
        cleanup()
        panels.delete(element)
      }
    }
    for (const element of current) {
      if (!panels.has(element)) {
        const overlay = element.matches('.el-dialog, .el-message-box')
        panels.set(element, attachLiquidGlass(element, {
          strength: 42,
          blur: overlay ? 4 : 0.65,
          radius: 18,
          aberration: 0.7,
        }))
      }
    }
  }
  onMounted(() => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    observer = new MutationObserver(records => {
      const changed = records.some(record => !isGlassResourceNode(record.target) &&
        [...record.addedNodes, ...record.removedNodes].some(node => node.nodeType === 1 && !isGlassResourceNode(node)))
      if (changed && !frame) frame = requestAnimationFrame(() => { frame = 0; sync() })
    })
    // 只监听节点增删；滤镜绘制会修改属性，监听属性会造成不必要的重复同步。
    observer.observe(document.body, { childList: true, subtree: true })
    sync()
  })
  onBeforeUnmount(() => {
    observer?.disconnect()
    cancelAnimationFrame(frame)
    panels.forEach((cleanup) => cleanup())
    panels.clear()
  })
  return { glassRoot }
}
