<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getMaintenanceRecord } from '../../api/services/maintenanceRecords'
import MaintenanceRecordForm from './components/MaintenanceRecordForm.vue'

defineOptions({ name: 'MaintenanceRecordEntry' })

const emit = defineEmits(['saved'])
const route = useRoute()
const router = useRouter()
const record = ref({})
const loading = ref(false)
const recordId = computed(() => String(route.query.id || '').trim())
const pageTitle = computed(() => (recordId.value ? '编辑维护记录' : '填写维护记录'))

const loadRecord = async () => {
  if (!recordId.value) {
    record.value = {}
    return
  }

  loading.value = true
  try {
    record.value = await getMaintenanceRecord(recordId.value)
  } catch (error) {
    record.value = {}
    ElMessage.error(error?.message || '维护记录加载失败')
  } finally {
    loading.value = false
  }
}

const handleSaved = (savedRecord) => {
  emit('saved', savedRecord)
  router.push('/lab-admin/maintenance-records')
}

watch(recordId, loadRecord)
onMounted(loadRecord)
</script>

<template>
  <section class="entry-page" v-loading="loading">
    <header class="page-header">
      <div>
        <p class="eyebrow">设备维护管理</p>
        <h1>{{ pageTitle }}</h1>
        <p>完整记录实验室设备维护信息，保存后可在维护记录查看页追溯。</p>
      </div>
      <el-button @click="router.push('/lab-admin/maintenance-records')">返回记录查看</el-button>
    </header>
    <section class="page-panel">
      <MaintenanceRecordForm :record="record" @saved="handleSaved" />
    </section>
  </section>
</template>

<style scoped>
.entry-page { display: grid; gap: 16px; padding-bottom: 20px; }
.page-header, .page-panel { border: 1px solid rgba(148, 163, 184, .28); border-radius: 8px; background: var(--surface-color); }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 24px 28px; background: linear-gradient(135deg, rgba(15, 118, 110, .12), rgba(37, 99, 235, .06) 48%, var(--surface-color)); }
.page-header h1 { margin: 0; color: var(--text-primary); font-size: 26px; }
.page-header p:not(.eyebrow) { margin: 8px 0 0; color: var(--text-secondary); }
.eyebrow { margin: 0 0 8px; color: #0f766e; font-size: 13px; font-weight: 700; }
.page-panel { padding: 24px 28px; }
@media (max-width: 680px) { .page-header { align-items: flex-start; flex-direction: column; padding: 18px; } .page-panel { padding: 18px; } }
</style>
