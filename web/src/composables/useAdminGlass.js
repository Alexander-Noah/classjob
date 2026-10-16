import { onBeforeUnmount, onMounted, ref } from 'vue'
import { attachLiquidGlass } from './useLiquidGlass'

export function useAdminGlass() {
  const glassRoot = ref(null)
  const panels = new Map()
  let observer
  let frame
  const sync = () => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    const current = new Set(root.querySelectorAll('.el-card, .page-panel, .hero-actions'))
    document.querySelectorAll('.el-dialog, .el-message-box').forEach((element) => current.add(element))
    for (const [element, cleanup] of panels) {
      if (!current.has(element)) {
        cleanup()
        panels.delete(element)
      }
    }
    for (const element of current) {
      if (!panels.has(element)) {
        panels.set(element, attachLiquidGlass(element, {
          strength: 78,
          blur: 1.2,
          edgeWidth: 48,
          radius: 20,
        }))
      }
    }
  }
  onMounted(() => {
    const root = glassRoot.value?.$el || glassRoot.value
    if (!root) return
    observer = new MutationObserver(() => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(sync)
    })
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
