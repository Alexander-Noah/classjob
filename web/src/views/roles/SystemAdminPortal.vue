<!--
  Web 管理平台的系统管理工作台视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
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
const config = computed(/** 计算派生状态。 */ () => ({
  ...SYSTEM_ADMIN_PORTAL,
  stats: buildPlatformSummary(snapshot.value.users, snapshot.value.notices),
  recordsTitle: '平台账号概览',
  columns: ['账号', '姓名', '角色', '创建时间', '状态'],
  records: buildPlatformAccountRows(snapshot.value.users),
}))
/**
 * 加载快照。
 */
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
