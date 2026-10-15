<script setup>
import { computed, onMounted, ref } from 'vue'
import RolePortalPage from './components/RolePortalPage.vue'
import { SYSTEM_ADMIN_PORTAL } from '../../constants/rolePortals'
import { fetchPlatformSnapshot } from '../../api/services/platformOverview'
import { buildPlatformSummary, buildPlatformAccountRows } from '../../modules/platform/platformOverview'

defineOptions({
  name: 'SystemAdminPortal',
})

const snapshot = ref({ users: null, notices: null, errors: [] })
const loading = ref(false)
const config = computed(() => ({
  ...SYSTEM_ADMIN_PORTAL,
  stats: buildPlatformSummary(snapshot.value.users, snapshot.value.notices),
  recordsTitle: '平台账号概览',
  columns: ['账号', '姓名', '角色', '创建时间', '状态'],
  records: buildPlatformAccountRows(snapshot.value.users),
}))
const loadSnapshot = async () => {
  loading.value = true
  try { snapshot.value = await fetchPlatformSnapshot() }
  finally { loading.value = false }
}
onMounted(loadSnapshot)
</script>

<template>
  <section v-loading="loading">
    <el-alert v-if="snapshot.errors.length" :title="snapshot.errors.join('；')" type="error" :closable="false" show-icon />
    <el-button v-if="snapshot.errors.length" @click="loadSnapshot">重新加载</el-button>
    <RolePortalPage :config="config" />
  </section>
</template>
