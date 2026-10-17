/*
 * 当前时间显示的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useCurrentTime。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'

function formatDateTime(date) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

/**
 * 在组件 setup 中创建本地时钟；挂载后立即显示并每秒刷新，卸载时释放定时器。
 * Intl 使用浏览器默认时区，此显示时钟不能代替服务器预约/采集任务的时间判定。
 */
export function useCurrentTime() {
  const currentTime = ref('')
  let timer = null

  const updateCurrentTime = () => {
    currentTime.value = formatDateTime(new Date())
  }

  onMounted(() => {
    updateCurrentTime()
    timer = window.setInterval(updateCurrentTime, 1000)
  })

  onBeforeUnmount(() => {
    if (timer) {
      window.clearInterval(timer)
    }
  })

  return {
    currentTime,
  }
}
