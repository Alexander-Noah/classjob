import { onBeforeUnmount, onMounted } from 'vue'
import { attachLiquidGlass } from './liquidGlassRuntime'
import { isGlassResourceNode } from './glassScrollActivity'
import { glassButtonSelector as selector, isGlassButtonTarget } from './glassButtonTarget'

/**
 * 应用根组件统一拥有按钮滤镜，覆盖路由页面、原生按钮和 Teleport 浮层。
 * 页面组合逻辑只管理卡片，避免两个生命周期同时清理同一按钮的资源。
 * 仅观察结构与 role，忽略动画写入的 style/SVG 属性，防止观察器自触发。
 */
export function useGlobalButtonGlass() {
  const buttons = new Map()
  let observer
  let frame = 0
  const pending = new Set()
  let removed = false
  function add(element) {
    if (!isGlassButtonTarget(element)) {
      buttons.get(element)?.()
      buttons.delete(element)
      return
    }
    if (buttons.has(element) || !element.isConnected) return
    // 无逐节点 getComputedStyle，避免批量挂载时读写样式交替导致反复布局。
    buttons.set(element, attachLiquidGlass(element, {
      strength: 28, blur: .35,
      radius: element.matches('.is-circle, .is-round, .avatar-upload-button, .el-avatar') ? 999 : element.matches('.login-button') ? 17 : 12,
      // 折射与高光可动，按钮命中区域保持固定，避免边缘反复触发 pointerleave。
      aberration: 1.1, interactive: true, elasticity: 0,
    }))
  }
  function sync() {
    frame = 0
    if (removed) {
      for (const [element, release] of buttons) {
        if (!element.isConnected || !isGlassButtonTarget(element)) { release(); buttons.delete(element) }
      }
      removed = false
    }
    for (const root of pending) {
      if (!root.isConnected) continue
      if (root.matches(selector)) add(root)
      root.querySelectorAll(selector).forEach(add)
    }
    pending.clear()
  }
  onMounted(() => {
    observer = new MutationObserver(records => {
      for (const record of records) {
        if (isGlassResourceNode(record.target)) continue
        if (record.type === 'attributes') {
          pending.add(record.target)
          removed = true
        } else {
          const wrapper = record.target.closest?.('[role="button"]')
          if (wrapper) pending.add(wrapper)
          for (const node of record.addedNodes) if (node.nodeType === 1 && !isGlassResourceNode(node)) pending.add(node)
          for (const node of record.removedNodes) if (node.nodeType === 1 && !isGlassResourceNode(node)) removed = true
        }
      }
      if ((pending.size || removed) && !frame) frame = requestAnimationFrame(sync)
    })
    observer.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['role'] })
    pending.add(document.body)
    sync()
  })
  onBeforeUnmount(() => {
    observer?.disconnect()
    cancelAnimationFrame(frame)
    buttons.forEach(release => release())
    buttons.clear()
    pending.clear()
  })
}
