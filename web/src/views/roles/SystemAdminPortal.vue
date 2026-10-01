<script setup>
import RolePortalPage from './components/RolePortalPage.vue'
import { SYSTEM_ADMIN_PORTAL } from '../../constants/rolePortals'

defineOptions({
  name: 'SystemAdminPortal',
})
</script>

<template>
  <RolePortalPage :config="SYSTEM_ADMIN_PORTAL" />
</template>
