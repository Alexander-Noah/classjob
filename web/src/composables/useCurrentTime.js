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
