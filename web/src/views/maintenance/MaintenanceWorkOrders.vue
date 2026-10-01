<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import PageHero from '../../components/common/PageHero.vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import MaintenanceProcessDialog from './components/MaintenanceProcessDialog.vue'
import MaintenanceWorkOrderDrawer from './components/MaintenanceWorkOrderDrawer.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import { repairResourceService } from '../../api/services/basicResources'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'MaintenanceWorkOrders',
})

const router = useRouter()

const statusOptions = ['全部', '待派单', '处理中', '待验收', '已完成']
const priorityOptions = ['全部', '高', '中', '低']

const DEFAULT_WORK_ORDERS = [
  {
    id: 'wo-1',
    ticket: 'R20260424001',
    device: '核心交换机',
    lab: 'B203',
    fault: '端口掉线，网络不通',
    priority: '高',
    priorityType: 'danger',
    status: '处理中',
    tagType: 'warning',
    assignee: '周工',
    step: 2,
    logs: [
      { time: '10:10', content: '管理员已派单', type: 'primary' },
      { time: '10:25', content: '到场检查交换机端口状态', type: 'warning' },
    ],
  },
  {
    id: 'wo-2',
    ticket: 'R20260424005',
    device: '教师机',
    lab: 'A101',
    fault: '系统无法启动',
    priority: '中',
    priorityType: 'warning',
    status: '待派单',
    tagType: 'danger',
    assignee: '周工',
    step: 1,
    logs: [{ time: '11:20', content: '管理员派单待接收', type: 'primary' }],
  },
  {
    id: 'wo-3',
    ticket: 'R20260423018',
    device: 'GPU 服务器',
    lab: 'C305',
    fault: '驱动异常，训练任务中断',
    priority: '高',
    priorityType: 'danger',
    status: '待验收',
    tagType: 'primary',
    assignee: '周工',
    step: 3,
    logs: [
      { time: '09:20', content: '已重装驱动并重启服务', type: 'success' },
      { time: '09:45', content: '等待实验室管理员复核', type: 'primary' },
    ],
  },
  {
    id: 'wo-4',
    ticket: 'R20260422011',
    device: '显示器',
    lab: 'A102',
    fault: '屏幕无信号',
    priority: '低',
    priorityType: 'info',
    status: '已完成',
    tagType: 'success',
    assignee: '周工',
    step: 4,
    logs: [
      { time: '昨日 15:10', content: '更换 HDMI 线缆', type: 'success' },
      { time: '昨日 15:30', content: '管理员确认工单关闭', type: 'success' },
    ],
  },
]

const priorityTypeMap = {
  高: 'danger',
  中: 'warning',
  低: 'info',
}

const statusTagTypeMap = {
  待派单: 'danger',
  处理中: 'warning',
  待验收: 'primary',
  已完成: 'success',
}

const statusStepMap = {
  待派单: 1,
  处理中: 2,
  待验收: 3,
  已完成: 4,
}

const normalizeWorkOrder = (record = {}) => {
  const status = record.status || '待派单'
  const resultText = record.result || record.action || '等待处理记录回填'

  return {
    ...record,
    id: record.id || record.ticket,
    ticket: record.ticket || record.orderNo || record.id,
    device: record.device || '未关联设备',
    lab: record.lab || '未关联实验室',
    fault: record.fault || record.description || record.faultType || '未填写故障描述',
    priority: record.priority || '中',
    priorityType: record.priorityType || priorityTypeMap[record.priority] || 'warning',
    status,
    tagType: record.tagType || statusTagTypeMap[status] || 'info',
    assignee: record.assignee || '待分配',
    step: record.step || statusStepMap[status] || 1,
    logs:
      record.logs ||
      [
        { time: record.lastUpdate || record.submittedAt || '刚刚', content: resultText, type: statusTagTypeMap[status] || 'primary' },
      ],
  }
}

const workOrders = ref(DEFAULT_WORK_ORDERS.map((item) => normalizeWorkOrder(item)))
const isLoading = ref(false)

const activeStatus = ref('全部')
const activePriority = ref('全部')
const selectedWorkOrder = ref(workOrders.value[0] || null)
const drawerVisible = ref(false)
const processDialogVisible = ref(false)

const metrics = computed(() => {
  const waiting = workOrders.value.filter((item) => item.status === '待派单').length
  const processing = workOrders.value.filter((item) => item.status === '处理中').length
  const finished = workOrders.value.filter((item) => item.status === '已完成').length

  return [
    { label: '我的工单', value: String(workOrders.value.length), trend: '只展示本人分派任务' },
    { label: '待派单', value: String(waiting), trend: '建议先处理高优先级' },
    { label: '处理中', value: String(processing), trend: '需尽快回填过程' },
    { label: '已完成', value: String(finished), trend: '等待管理员闭环确认' },
  ]
})

const filteredOrders = computed(() =>
  workOrders.value.filter((item) => {
    const matchStatus = activeStatus.value === '全部' || item.status === activeStatus.value
    const matchPriority = activePriority.value === '全部' || item.priority === activePriority.value
    return matchStatus && matchPriority
  }),
)

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedOrders,
} = useTablePagination(filteredOrders)

const loadWorkOrders = async () => {
  isLoading.value = true
  try {
    const records = await repairResourceService.list({ fallbackRecords: DEFAULT_WORK_ORDERS })
    if (records.length) {
      workOrders.value = records.map((item) => normalizeWorkOrder(item))
      selectedWorkOrder.value = workOrders.value[0] || null
    }
  } catch (error) {
    ElMessage.warning(error?.message || '后端维修工单暂不可用，已保留当前页面数据')
  } finally {
    isLoading.value = false
  }
}

const replaceWorkOrder = (record) => {
  const normalizedRecord = normalizeWorkOrder(record)
  workOrders.value = workOrders.value.map((item) =>
    String(item.id) === String(normalizedRecord.id) || String(item.ticket) === String(normalizedRecord.ticket) ? normalizedRecord : item,
  )
  selectedWorkOrder.value = normalizedRecord
  return normalizedRecord
}

const exportWorkOrders = () => {
  const exportRows = filteredOrders.value

  if (!exportRows.length) {
    ElMessage.warning('暂无可导出的维修工单')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: '维修工单',
    sheetName: '维修工单',
    rows: exportRows,
    columns: [
      { label: '工单号', prop: 'ticket' },
      { label: '设备', prop: 'device' },
      { label: '实验室', prop: 'lab' },
      { label: '故障描述', prop: 'fault' },
      { label: '优先级', prop: 'priority' },
      { label: '状态', prop: 'status' },
      { label: '维修人', prop: 'assignee' },
      { label: '流程阶段', prop: 'step' },
      { label: '处理日志', prop: 'logs' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条维修工单`)
}

const handleHeroAction = async (action) => {
  if (action.command === 'dispatch') {
    await loadWorkOrders()
    ElMessage.success('已同步最新派单数据')
    return
  }

  if (action.command === 'export') {
    exportWorkOrders()
  }
}

const handleBack = () => {
  router.push(AUTH_ROLE_DEFAULT_PATH.maintenance)
}

const handleOpenDrawer = (row) => {
  selectedWorkOrder.value = row
  drawerVisible.value = true
}

const handleProcess = (row) => {
  selectedWorkOrder.value = row
  processDialogVisible.value = true
}

const handleSaveProcess = async (payload) => {
  if (!selectedWorkOrder.value) return

  const nextResult = [payload.action, payload.parts ? `更换部件：${payload.parts}` : '', payload.nextStep].filter(Boolean).join('；')
  const requestPayload = {
    ...selectedWorkOrder.value,
    status: payload.status,
    progress: payload.status === '已完成' ? 100 : payload.status === '待验收' ? 86 : 45,
    result: nextResult || '维修人员已更新处理记录。',
  }

  const actionName =
    payload.status === '已完成'
      ? 'complete'
      : payload.status === '待验收'
        ? 'submit-acceptance'
        : payload.status === '处理中'
          ? 'assign'
          : ''

  try {
    const savedRecord = actionName
      ? await repairResourceService.action(selectedWorkOrder.value.id, actionName, requestPayload)
      : await repairResourceService.update(selectedWorkOrder.value.id, requestPayload)
    const normalizedRecord = replaceWorkOrder({
      ...savedRecord,
      logs: [
        { time: '刚刚', content: requestPayload.result, type: statusTagTypeMap[payload.status] || 'primary' },
        ...selectedWorkOrder.value.logs,
      ],
    })
    selectedWorkOrder.value = normalizedRecord
    ElMessage.success('维修处理记录已保存')
  } catch (error) {
    ElMessage.error(error?.message || '维修处理记录保存失败')
  }
}

onMounted(loadWorkOrders)
</script>

<template>
  <section class="maintenance-page">
    <PageHero
      :actions="[
        { label: '同步派单', type: 'primary', command: 'dispatch' },
        { label: '导出工单', type: 'default', command: 'export' },
      ]"
      badge="维修工单"
      page-title="维修工单"
      page-description="维修人员可按状态查看分派给自己的工单，并通过抽屉和对话框回填处理过程。"
      :page-icon="Tools"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <MetricGrid :metrics="metrics" />

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <el-tabs v-model="activeStatus" class="status-tabs">
          <el-tab-pane v-for="item in statusOptions" :key="item" :label="item" :name="item" />
        </el-tabs>

        <el-select v-model="activePriority" class="priority-select" placeholder="全部优先级">
          <el-option v-for="item in priorityOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="isLoading" :data="pagedOrders" class="clickable-table" empty-text="暂无工单" @row-click="handleOpenDrawer">
        <el-table-column prop="ticket" label="工单号" min-width="140" />
        <el-table-column prop="device" label="设备" min-width="150" />
        <el-table-column prop="lab" label="实验室" min-width="110" />
        <el-table-column prop="fault" label="故障描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="优先级" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.priorityType" effect="light">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="handleProcess(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <TablePagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredOrders.length"
        @page-change="handleCurrentPageChange"
        @page-size-change="handlePageSizeChange"
      />
    </el-card>

    <MaintenanceWorkOrderDrawer v-model="drawerVisible" :work-order="selectedWorkOrder" />
    <MaintenanceProcessDialog
      v-model="processDialogVisible"
      :work-order="selectedWorkOrder"
      @save="handleSaveProcess"
    />
  </section>
</template>

<style scoped>
.maintenance-page {
  display: grid;
  gap: 18px;
}

.toolbar-card,
.table-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.toolbar-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 18px;
}

.toolbar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.status-tabs {
  flex: 1 1 auto;
}

.status-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.priority-select {
  width: 160px;
}

.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}

@media (max-width: 760px) {
  .toolbar-content {
    flex-direction: column;
    align-items: stretch;
  }

  .priority-select {
    width: 100%;
  }
}
</style>
