import { nextTick, onBeforeUnmount, shallowRef } from 'vue'
import { echarts } from '../utils/echartsCore'

export function useECharts(chartRef, buildOption) {
  const chartInstance = shallowRef(null)

  const renderChart = async () => {
    await nextTick()

    if (!chartRef.value) return

    if (!chartInstance.value) {
      chartInstance.value = echarts.init(chartRef.value)
    }

    chartInstance.value.setOption(buildOption(), true)
  }

  const resizeChart = () => {
    chartInstance.value?.resize()
  }

  onBeforeUnmount(() => {
    chartInstance.value?.dispose()
  })

  return {
    renderChart,
    resizeChart,
  }
}
