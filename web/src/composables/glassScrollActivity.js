const listeners = new Set()
let timer = 0
let scrolling = false

/** 全应用共享一个滚动监听器；捕获模式覆盖 el-main、表格和弹窗内部滚动。 */
export function observeGlassScrolling(listener) {
  if (!listeners.size) {
    document.addEventListener('scroll', onScroll, { capture: true, passive: true })
    document.addEventListener('wheel', onScroll, { passive: true })
  }
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
    if (listeners.size) return
    document.removeEventListener('scroll', onScroll, true)
    document.removeEventListener('wheel', onScroll)
    clearTimeout(timer)
    scrolling = false
    document.documentElement.classList.remove('liquid-scrolling')
  }
}

function onScroll() {
  if (!scrolling) {
    scrolling = true
    document.documentElement.classList.add('liquid-scrolling')
    for (const listener of listeners) listener(true)
  }
  clearTimeout(timer)
  timer = setTimeout(() => {
    scrolling = false
    document.documentElement.classList.remove('liquid-scrolling')
    for (const listener of listeners) listener(false)
  }, 160)
}

/** 忽略滤镜自身的挂载/卸载，防止资源管理反过来触发页面扫描。 */
export function isGlassResourceNode(node) {
  return node.nodeType === 1 && (node.matches('.liquid-filter-host') || !!node.closest('.liquid-filter-host'))
}
