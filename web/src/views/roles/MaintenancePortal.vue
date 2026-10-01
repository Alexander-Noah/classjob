<script setup>
import RolePortalPage from './components/RolePortalPage.vue'
import { MAINTENANCE_PORTAL } from '../../constants/rolePortals'

defineOptions({
  name: 'MaintenancePortal',
})
</script>

<template>
  <RolePortalPage :config="MAINTENANCE_PORTAL" />
</template>
