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
