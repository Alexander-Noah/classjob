<!--
  Web 管理平台的维护记录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineOptions({ name: 'MaintenanceRecordEntry' })
const route = useRoute()
const router = useRouter()
onMounted(() => {
  const query = route.query.id ? { edit: String(route.query.id) } : { create: '1' }
  router.replace({ path: '/lab-admin/maintenance-records', query })
})
</script>
<template><div>正在打开维护记录…</div></template>
