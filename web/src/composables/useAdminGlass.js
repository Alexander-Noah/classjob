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
  /**
   * 同步当前操作。
   */
  const sync = () => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    const current = new Set(root.querySelectorAll('.el-card, .page-panel, .hero-actions'))
    document.querySelectorAll('.el-dialog, .el-message-box').forEach(/** 处理遍历到的当前条目。 入参：element。 */ (element) => {
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
  onMounted(/** 在组件挂载后初始化相关状态。 */ () => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    observer = new MutationObserver(/** 处理当前调用传入的回调逻辑。 入参：records。 */ records => {
      const changed = records.some(/** 判断当前条目是否满足任一匹配条件。 入参：record。 */ record => !isGlassResourceNode(record.target) &&
        [...record.addedNodes, ...record.removedNodes].some(/** 判断当前条目是否满足任一匹配条件。 入参：node。 */ node => node.nodeType === 1 && !isGlassResourceNode(node)))
      if (changed && !frame) frame = requestAnimationFrame(/** 在下一绘制帧更新界面。 */ () => { frame = 0; sync() })
    })
    // 只监听节点增删；滤镜绘制会修改属性，监听属性会造成不必要的重复同步。
    observer.observe(document.body, { childList: true, subtree: true })
    sync()
  })
  onBeforeUnmount(/** 在组件卸载前释放相关资源。 */ () => {
    observer?.disconnect()
    cancelAnimationFrame(frame)
    panels.forEach(/** 处理遍历到的当前条目。 入参：cleanup。 */ (cleanup) => cleanup())
    panels.clear()
  })
  return { glassRoot }
}
