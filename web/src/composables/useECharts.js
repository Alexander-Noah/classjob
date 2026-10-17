/*
 * 图表渲染的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useECharts。
 */
import { nextTick, onBeforeUnmount, shallowRef } from 'vue'
import { echarts } from '../utils/echartsCore'

/**
 * 按需创建图表实例；调用方负责在数据准备好后 renderChart，并注册/清理 resize 监听。
 * @param {import('vue').Ref<HTMLElement|null>} chartRef 挂载容器，渲染前应已有有效尺寸
 * @param {() => object} buildOption 读取当前页面状态并生成完整配置的同步函数
 * @returns {{renderChart: () => Promise<void>, resizeChart: () => void}} 手动渲染及尺寸同步入口
 * @description 在组件 setup 中调用，卸载时 dispose；不会自动订阅数据变化。
 */
export function useECharts(chartRef, buildOption) {
  const chartInstance = shallowRef(null)

  /**
   * 渲染Chart。
   */
  const renderChart = async () => {
    await nextTick()

    if (!chartRef.value) return

    if (!chartInstance.value) {
      chartInstance.value = echarts.init(chartRef.value)
    }

    // notMerge=true 用完整配置替换旧系列，避免筛选后残留已删除的图表数据。
    chartInstance.value.setOption(buildOption(), true)
  }

  /**
   * resizeChart的处理逻辑。
   */
  const resizeChart = () => {
    chartInstance.value?.resize()
  }

  onBeforeUnmount(/** 在组件卸载前释放相关资源。 */ () => {
    chartInstance.value?.dispose()
  })

  return {
    renderChart,
    resizeChart,
  }
}
