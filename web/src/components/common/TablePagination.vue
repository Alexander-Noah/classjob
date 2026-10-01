<script setup>
defineOptions({
  name: 'TablePagination',
})

defineProps({
  currentPage: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
  total: {
    type: Number,
    default: 0,
  },
})

defineEmits(['page-change', 'page-size-change'])
</script>

<template>
  <div class="table-pagination">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="[5, 10, 20, 50]"
      :total="total"
      background
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="$emit('page-change', $event)"
      @size-change="$emit('page-size-change', $event)"
    />
  </div>
</template>

<style scoped>
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px 16px;
  border-top: 1px solid var(--border-light);
}

@media (max-width: 760px) {
  .table-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
