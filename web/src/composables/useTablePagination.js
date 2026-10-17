/*
 * 表格分页的 Vue 组合逻辑；视图通过返回的状态和方法调用本模块。
 * 主要导出：useTablePagination。
 */
import { computed, ref, unref, watch } from 'vue'

/**
 * 对已加载到浏览器的数组进行本地分页，不发送服务端分页请求。
 * @param {Array<object>|import('vue').Ref<Array<object>>} sourceRecords 过滤后的数组或响应式引用，不支持 getter 函数
 * @param {{initialPage?: number, initialPageSize?: number}} options 初始页码与页大小，调用方应传正整数
 * @returns {object} 页码/页大小 ref、总数和当前切片 computed，以及分页事件处理器
 * @example const pager = useTablePagination(filteredRecords, { initialPageSize: 10 })
 */
export function useTablePagination(sourceRecords, options = {}) {
  const currentPage = ref(options.initialPage || 1)
  const pageSize = ref(options.initialPageSize || 10)

  const total = computed(/** 计算派生状态。 */ () => unref(sourceRecords)?.length || 0)
  const pagedRecords = computed(/** 计算派生状态。 */ () => {
    const records = unref(sourceRecords) || []
    const startIndex = (currentPage.value - 1) * pageSize.value
    return records.slice(startIndex, startIndex + pageSize.value)
  })

  /**
   * 处理当前页码Change。
   * 入参：page（页码）。
   */
  const handleCurrentPageChange = (page) => {
    currentPage.value = page
  }

  /**
   * 处理页码大小Change。
   * 入参：size（大小）。
   */
  const handlePageSizeChange = (size) => {
    pageSize.value = size
    currentPage.value = 1
  }

  // 删除或过滤使列表变短时收缩页码，避免停在不存在的末页；空列表仍保留第一页。
  // watch 在组件 setup 的同步调用范围内创建，由 Vue 随组件卸载释放。
  watch(total, /** 响应被监听状态的变化。 入参：count。 */ (count) => {
    const maxPage = Math.max(1, Math.ceil(count / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  return {
    currentPage,
    handleCurrentPageChange,
    handlePageSizeChange,
    pageSize,
    pagedRecords,
    total,
  }
}
