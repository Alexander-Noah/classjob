import { computed, ref, unref, watch } from 'vue'

export function useTablePagination(sourceRecords, options = {}) {
  const currentPage = ref(options.initialPage || 1)
  const pageSize = ref(options.initialPageSize || 10)

  const total = computed(() => unref(sourceRecords)?.length || 0)
  const pagedRecords = computed(() => {
    const records = unref(sourceRecords) || []
    const startIndex = (currentPage.value - 1) * pageSize.value
    return records.slice(startIndex, startIndex + pageSize.value)
  })

  const handleCurrentPageChange = (page) => {
    currentPage.value = page
  }

  const handlePageSizeChange = (size) => {
    pageSize.value = size
    currentPage.value = 1
  }

  watch(total, (count) => {
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
