<!--
  Web 管理平台的教师工作台视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useTablePagination。
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentChecked } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import PageHero from '../../components/common/PageHero.vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import TeacherAdjustmentDialog from './components/TeacherAdjustmentDialog.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import {
  createScheduleAdjustment,
  listScheduleAdjustments,
  updateScheduleAdjustment,
} from '../../api/services/scheduleAdjustments'
import { getStoredRole } from '../../api/services/auth'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'TeacherAdjustments',
})

const router = useRouter()
const route = useRoute()
const roleHomePath = computed(/** 计算派生状态。 */ () => AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || AUTH_ROLE_DEFAULT_PATH.teacher)

const DEFAULT_REQUESTS = [
  {
    id: 'adj-1',
    course: '数据库原理',
    originalTime: '周五 14:00-15:40',
    targetTime: '下周一 08:00-09:40',
    reason: '学院活动冲突',
    status: '审核中',
    tagType: 'warning',
    flowStep: 2,
    reviewer: '教务处',
  },
  {
    id: 'adj-2',
    course: 'Java Web 程序设计',
    originalTime: '周三 08:00-09:40',
    targetTime: '周三 10:00-11:40',
    reason: '教师外出答辩',
    status: '已通过',
    tagType: 'success',
    flowStep: 3,
    reviewer: '教务处',
  },
  {
    id: 'adj-3',
    course: '人工智能基础',
    originalTime: '周四 10:00-11:40',
    targetTime: '周四 14:00-15:40',
    reason: '实验室临时维护',
    status: '已调整',
    tagType: 'primary',
    flowStep: 4,
    reviewer: '实验室管理员',
  },
]

/**
 * 解析AdjustmentMeta。
 * 入参：status（状态）。
 * @returns 组装的键值对象。
 */
const resolveAdjustmentMeta = (status = '审核中') => {
  if (status === '已调整') return { tagType: 'primary', flowStep: 4 }
  if (status === '已通过') return { tagType: 'success', flowStep: 3 }
  if (status === '已取消') return { tagType: 'info', flowStep: 1 }
  return { tagType: 'warning', flowStep: 2 }
}

/**
 * 归一化Adjustment。
 * 入参：record（记录）。
 * @returns 组装的键值对象。
 */
const normalizeAdjustment = (record = {}) => {
  const status = record.status || '审核中'
  const meta = resolveAdjustmentMeta(status)

  return {
    id: record.id || `adj-${Date.now()}`,
    course: record.course || '未命名课程',
    originalTime: record.originalTime || '待确认',
    targetTime: record.targetTime || '待确认',
    reason: record.reason || '教学安排调整',
    status,
    tagType: record.tagType || meta.tagType,
    flowStep: Number(record.flowStep || meta.flowStep),
    reviewer: record.reviewer || (status === '已取消' ? '教师撤回' : '教务处'),
  }
}

const requestList = ref(DEFAULT_REQUESTS.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => normalizeAdjustment(item)))
const isLoading = ref(false)

const activeStatus = ref('全部')
const selectedRequest = ref(requestList.value[0] || null)
const dialogVisible = ref(false)
const adjustmentPrefill = ref(null)

const statusOptions = ['全部', '审核中', '已通过', '已调整', '已取消']
const fallbackCourseOptions = ['数据库原理', 'Java Web 程序设计', '人工智能基础', '软件测试']

const courseOptions = computed(/** 计算派生状态。 */ () => {
  const courseNames = requestList.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => item.course)
  const queryCourse = route.query.course ? [String(route.query.course)] : []
  return [...new Set([...queryCourse, ...courseNames, ...fallbackCourseOptions].filter(Boolean))]
})

const metrics = computed(/** 计算派生状态。 */ () => {
  const processing = requestList.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '审核中').length
  const approved = requestList.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '已通过').length
  const adjusted = requestList.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '已调整').length

  return [
    { label: '本月申请', value: String(requestList.value.length), trend: '教师自主提交调课需求' },
    { label: '审核中', value: String(processing), trend: '等待教务或实验室确认' },
    { label: '已通过', value: String(approved), trend: '排课已完成调整' },
    { label: '已执行', value: String(adjusted), trend: '已同步到个人课表' },
  ]
})

const filteredRequests = computed(/** 计算派生状态。 */ () =>
  requestList.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => activeStatus.value === '全部' || item.status === activeStatus.value),
)

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedRequests,
} = useTablePagination(filteredRequests)

/**
 * 加载Adjustments。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const loadAdjustments = async () => {
  isLoading.value = true
  try {
    const records = await listScheduleAdjustments()
    if (records.length) {
      requestList.value = records.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => normalizeAdjustment(item))
      selectedRequest.value = requestList.value[0] || null
    }
    return true
  } catch (error) {
    ElMessage.warning(error?.message || '调课申请加载失败，已保留当前列表')
    return false
  } finally {
    isLoading.value = false
  }
}

/**
 * 同步Adjustments。
 */
const syncAdjustments = async () => {
  const synced = await loadAdjustments()
  if (synced) {
    ElMessage.success('已同步调课申请记录')
  }
}

/**
 * replaceAdjustment的处理逻辑。
 * 入参：record（记录）。
 */
const replaceAdjustment = (record) => {
  const normalized = normalizeAdjustment(record)
  requestList.value = requestList.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => (item.id === normalized.id ? normalized : item))
  if (selectedRequest.value?.id === normalized.id) {
    selectedRequest.value = normalized
  }
}

/**
 * 判断是否允许Withdraw。
 * 入参：row（行）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const canWithdraw = (row) => row?.status === '审核中'

/**
 * withdrawAdjustment的处理逻辑。
 * 入参：row（行）。
 */
const withdrawAdjustment = async (row = selectedRequest.value) => {
  if (!row) return

  if (!canWithdraw(row)) {
    ElMessage.warning('只有审核中的调课申请可以撤回')
    return
  }

  const payload = {
    ...row,
    status: '已取消',
    tagType: 'info',
    flowStep: 1,
    reviewer: '教师撤回',
  }

  if (!Number.isFinite(Number(row.id))) {
    replaceAdjustment(payload)
    ElMessage.success('调课申请已撤回')
    return
  }

  try {
    const savedAdjustment = await updateScheduleAdjustment(row.id, payload)
    replaceAdjustment({ ...payload, ...savedAdjustment })
    ElMessage.success('调课申请已撤回')
  } catch (error) {
    ElMessage.error(error?.message || '调课申请撤回失败')
  }
}

/**
 * 导出Adjustment请求。
 */
const exportAdjustmentRequests = () => {
  const exportRows = filteredRequests.value

  if (!exportRows.length) {
    ElMessage.warning('暂无可导出的调课申请')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: '调课申请',
    sheetName: '调课申请',
    rows: exportRows,
    columns: [
      { label: '申请编号', prop: 'id' },
      { label: '课程', prop: 'course' },
      { label: '原时间', prop: 'originalTime' },
      { label: '目标时间', prop: 'targetTime' },
      { label: '原因', prop: 'reason' },
      { label: '状态', prop: 'status' },
      { label: '流程阶段', prop: 'flowStep' },
      { label: '审核人', prop: 'reviewer' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条调课申请`)
}

/**
 * 处理HeroAction。
 * 入参：action（action）。
 */
const handleHeroAction = async (action) => {
  if (action.command === 'create') {
    adjustmentPrefill.value = null
    dialogVisible.value = true
    return
  }

  if (action.command === 'sync') {
    await syncAdjustments()
    return
  }

  if (action.command === 'export') {
    exportAdjustmentRequests()
  }
}

/**
 * 处理Back。
 */
const handleBack = () => {
  router.push(roleHomePath.value)
}

/**
 * 应用RouteAdjustment上下文。
 */
const applyRouteAdjustmentContext = () => {
  if (route.query.action !== 'create') return

  const course = String(route.query.course || '').trim()
  if (!course) return

  adjustmentPrefill.value = {
    course,
    originalTime: String(route.query.time || ''),
    targetTime: '',
    reason: route.query.lab ? `原实验室 ${route.query.lab} 授课安排需协调` : '',
  }
  dialogVisible.value = true
}

/**
 * 处理Save。
 * 入参：payload（载荷）。
 */
const handleSave = async (payload) => {
  try {
    const savedAdjustment = await createScheduleAdjustment({
      ...payload,
      teacherName: '任课教师',
      status: '审核中',
      tagType: 'warning',
      flowStep: 2,
      reviewer: '教务处',
    })
    requestList.value.unshift(normalizeAdjustment(savedAdjustment))
    selectedRequest.value = requestList.value[0]
    ElMessage.success('调课申请已提交')
  } catch (error) {
    ElMessage.error(error?.message || '调课申请提交失败')
  }
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  await loadAdjustments()
  applyRouteAdjustmentContext()
})
</script>

<template>
  <section class="teacher-page">
    <PageHero
      :actions="[
        { label: '新增申请', type: 'primary', command: 'create' },
        { label: '同步申请', type: 'default', command: 'sync' },
        { label: '导出申请', type: 'default', command: 'export' },
      ]"
      badge="调课申请"
      page-title="调课申请"
      page-description="教师可查看调课申请进度、审批结果和冲突处理状态，并继续补充新的调课申请。"
      :page-icon="DocumentChecked"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <MetricGrid :metrics="metrics" />

    <el-row :gutter="18">
      <el-col :xs="24" :xl="15">
        <el-card class="list-card" shadow="never">
          <template #header>
            <div class="section-title section-title-inline">
              <div>
                <h2>申请列表</h2>
                <span>按状态查看调课审批进度</span>
              </div>
              <el-tabs v-model="activeStatus" class="status-tabs">
                <el-tab-pane v-for="item in statusOptions" :key="item" :label="item" :name="item" />
              </el-tabs>
            </div>
          </template>

          <el-table v-loading="isLoading" :data="pagedRequests" empty-text="暂无调课申请" @row-click="selectedRequest = $event">
            <el-table-column prop="course" label="课程" min-width="150" />
            <el-table-column prop="originalTime" label="原时间" min-width="140" />
            <el-table-column prop="targetTime" label="目标时间" min-width="140" />
            <el-table-column prop="reviewer" label="处理方" min-width="110" />
            <el-table-column label="状态" min-width="100">
              <template #default="{ row }">
                <el-tag :type="row.tagType" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="canWithdraw(row)"
                  link
                  type="primary"
                  @click.stop="withdrawAdjustment(row)"
                >
                  撤回
                </el-button>
                <span v-else class="muted-action">已处理</span>
              </template>
            </el-table-column>
          </el-table>
          <TablePagination
            :current-page="currentPage"
            :page-size="pageSize"
            :total="filteredRequests.length"
            @page-change="handleCurrentPageChange"
            @page-size-change="handlePageSizeChange"
          />
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="9">
        <el-card class="detail-card" shadow="never">
          <template #header>
            <div class="section-title">
              <div>
                <h2>审批轨迹</h2>
                <span>查看当前选中申请的处理流程</span>
              </div>
            </div>
          </template>

          <template v-if="selectedRequest">
            <h3 class="detail-title">{{ selectedRequest.course }}</h3>
            <p class="detail-copy">{{ selectedRequest.reason }}</p>

            <el-descriptions :column="1" class="detail-descriptions">
              <el-descriptions-item label="原时间">{{ selectedRequest.originalTime }}</el-descriptions-item>
              <el-descriptions-item label="目标时间">{{ selectedRequest.targetTime }}</el-descriptions-item>
              <el-descriptions-item label="当前处理方">{{ selectedRequest.reviewer }}</el-descriptions-item>
            </el-descriptions>

            <el-steps :active="selectedRequest.flowStep" direction="vertical" finish-status="success">
              <el-step title="提交申请" description="教师填写原因并提交时间调整需求" />
              <el-step title="审批中" description="教务与实验室校验排课和机房冲突" />
              <el-step title="通过确认" description="审批完成，等待排课同步" />
              <el-step title="完成执行" description="已更新到教师课表和实验室安排" />
            </el-steps>

            <div class="detail-actions" v-if="canWithdraw(selectedRequest)">
              <el-button type="primary" @click="withdrawAdjustment()">撤回申请</el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <TeacherAdjustmentDialog
      v-model="dialogVisible"
      :course-options="courseOptions"
      :prefill="adjustmentPrefill"
      @save="handleSave"
    />
  </section>
</template>

<style scoped>
.teacher-page {
  display: grid;
  gap: 18px;
}

.list-card,
.detail-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.section-title h2,
.detail-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.section-title span,
.detail-copy {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.section-title-inline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.status-tabs {
  flex: 0 0 auto;
  max-width: 360px;
}

.status-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.detail-descriptions {
  margin: 16px 0;
}

.detail-actions {
  margin-top: 18px;
}

.muted-action {
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 760px) {
  .section-title-inline {
    flex-direction: column;
  }
}
</style>
