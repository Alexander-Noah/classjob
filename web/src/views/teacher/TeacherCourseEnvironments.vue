<!--
  Web 管理平台的教师工作台视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection } from '@element-plus/icons-vue'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
import PageHero from '../../components/common/PageHero.vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import TeacherEnvironmentDrawer from './components/TeacherEnvironmentDrawer.vue'
import TeacherEnvironmentRequestDialog from './components/TeacherEnvironmentRequestDialog.vue'
import http from '../../api/http'
import { API_ENDPOINTS } from '../../api/endpoints'
import { getStoredRole, getStoredUser } from '../../api/services/auth'
import { exportTableToExcel } from '../../utils/exportExcel'

defineOptions({
  name: 'TeacherCourseEnvironments',
})

const router = useRouter()
const route = useRoute()
const roleHomePath = computed(/** 计算派生状态。 */ () => AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || AUTH_ROLE_DEFAULT_PATH.teacher)

const pageActions = [
  { label: '提交需求', type: 'primary', command: 'create' },
  { label: '同步环境', type: 'default', command: 'remind' },
  { label: '导出清单', type: 'default', command: 'export' },
]

const DEFAULT_ENVIRONMENT_RECORDS = [
  {
    id: 'env-1',
    course: 'Java Web 程序设计',
    lab: 'A101 开发实验室',
    schedule: '周三 08:00-09:40',
    software: 'JDK 21、IDEA、MySQL',
    manager: '张明',
    status: '待确认',
    tagType: 'warning',
    step: 2,
    reminder: '课前 24 小时内需要完成确认',
    assets: [
      { title: '环境截图', description: '已上传 IDE、数据库连接和 Tomcat 部署截图。' },
      { title: '配置说明', description: '包含 JDK 版本、项目模板与数据库账号说明。' },
    ],
    steps: [
      { title: '管理员接收需求', description: '已根据课程模板分配 A101 机房。' },
      { title: '软件安装完成', description: 'JDK、IDEA、MySQL 已校验。' },
      { title: '教师远程确认', description: '请在上课前完成确认或退回调整。' },
    ],
  },
  {
    id: 'env-2',
    course: '人工智能基础',
    lab: 'C305 GPU 实验室',
    schedule: '周四 10:00-11:40',
    software: 'Python、PyTorch、CUDA',
    manager: '王磊',
    status: '已确认',
    tagType: 'success',
    step: 3,
    reminder: '环境已通过教师确认，可直接授课',
    assets: [
      { title: '环境截图', description: '已展示 GPU 驱动状态、CUDA 版本和 Notebook 页面。' },
      { title: '配置说明', description: '附带训练数据目录与依赖包版本说明。' },
    ],
    steps: [
      { title: '管理员接收需求', description: '课程环境需求已确认。' },
      { title: '软件安装完成', description: 'CUDA 与 PyTorch 版本测试通过。' },
      { title: '教师远程确认', description: '已确认，可进入授课阶段。' },
    ],
  },
  {
    id: 'env-3',
    course: '数据库原理',
    lab: 'A102 机房',
    schedule: '周五 14:00-15:40',
    software: 'MySQL、Navicat、ER 建模工具',
    manager: '张明',
    status: '需调整',
    tagType: 'danger',
    step: 1,
    reminder: 'Navicat 版本与课程要求不一致，需要管理员调整',
    assets: [
      { title: '环境截图', description: '已上传当前软件版本和数据库连接界面。' },
      { title: '调整说明', description: '教师反馈需升级至指定版本并补充建模插件。' },
    ],
    steps: [
      { title: '管理员接收需求', description: '已收到课程模板和调整意见。' },
      { title: '软件安装完成', description: '当前仍在更新软件版本。' },
      { title: '教师远程确认', description: '等待管理员重新提交。' },
    ],
  },
]

/**
 * 归一化列表响应。
 * 入参：data（数据）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeListResponse = (data) => {
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data)) return data
  return []
}

/**
 * 归一化记录响应。
 * 入参：data（数据）；fallbackRecord（fallback记录）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeRecordResponse = (data, fallbackRecord) => {
  if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) return data.data
  if (data && typeof data === 'object' && !Array.isArray(data) && !('code' in data && !('id' in data))) return data
  return fallbackRecord
}

/**
 * 解析环境状态。
 * 入参：record（记录）。
 * @returns 文本结果。
 */
const resolveEnvironmentStatus = (record = {}) => {
  const statusText = [record.confirmStatus, record.status, record.processStatus].filter(Boolean).join(' ')
  if (statusText.includes('已确认')) return '已确认'
  if (statusText.includes('调整') || statusText.includes('退回')) return '需调整'
  return '待确认'
}

/**
 * 归一化环境记录。
 * 入参：record（记录）。
 * @returns 组装的键值对象。
 */
const normalizeEnvironmentRecord = (record = {}) => {
  const status = resolveEnvironmentStatus(record)
  const labName = record.lab || [record.assignedLabCode, record.assignedLabName].filter(Boolean).join(' ') || record.labType || '待分配实验室'
  const software = record.software || record.softwareRequirements || '按课程模板配置'
  const course = record.course || record.courseName || '未命名课程'
  const step = status === '已确认' ? 3 : status === '需调整' ? 1 : record.assignedLabId || record.assignedLabName ? 2 : 1

  return {
    ...record,
    id: record.id,
    course,
    lab: labName,
    schedule: record.schedule || record.useTime || '待确认',
    software,
    manager: record.manager || record.assignedLabName || '实验室管理员',
    status,
    tagType: status === '已确认' ? 'success' : status === '需调整' ? 'danger' : 'warning',
    step,
    reminder:
      record.reminder ||
      (status === '已确认'
        ? '环境已通过教师确认，可直接授课'
        : status === '需调整'
          ? record.specialRequirements || '教师已退回环境，请管理员按意见调整'
          : '课前请完成环境确认，异常可退回调整'),
    assets:
      record.assets ||
      [
        { title: '软件环境', description: software },
        { title: '系统与特殊要求', description: [record.systemRequirements, record.specialRequirements].filter(Boolean).join('；') || '按课程要求完成配置。' },
      ],
    steps:
      record.steps ||
      [
        { title: '管理员接收需求', description: '课程环境需求已进入配置流程。' },
        { title: '软件与实验室配置', description: `${labName} · ${software}` },
        { title: '教师确认闭环', description: status === '已确认' ? '已确认并沉淀为可复用模板。' : '等待教师确认或退回调整。' },
      ],
  }
}

const environmentRecords = ref(DEFAULT_ENVIRONMENT_RECORDS.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => normalizeEnvironmentRecord(item)))
const isLoading = ref(false)

const keyword = ref('')
const activeStatus = ref('全部')
const drawerVisible = ref(false)
const requestDialogVisible = ref(false)
const returnDialogVisible = ref(false)
const selectedEnvironment = ref(environmentRecords.value[0] || null)
const requestPrefill = ref(null)
const returnForm = reactive({
  reason: '',
})

const statusOptions = ['全部', '待确认', '已确认', '需调整']

const metrics = computed(/** 计算派生状态。 */ () => {
  const total = environmentRecords.value.length
  const pending = environmentRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '待确认').length
  const confirmed = environmentRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '已确认').length
  const returned = environmentRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => item.status === '需调整').length

  return [
    { label: '课程环境总数', value: String(total), trend: '本周教师可见环境清单' },
    { label: '待确认环境', value: String(pending), trend: '建议课前 24 小时完成确认' },
    { label: '已确认环境', value: String(confirmed), trend: '可直接进入授课' },
    { label: '需调整环境', value: String(returned), trend: '已有退回意见待处理' },
  ]
})

const filteredEnvironments = computed(/** 计算派生状态。 */ () =>
  environmentRecords.value.filter(/** 判断当前条目是否符合筛选条件。 入参：item。 */ (item) => {
    const matchStatus = activeStatus.value === '全部' || item.status === activeStatus.value
    const matchKeyword =
      !keyword.value ||
      [item.course, item.lab, item.software].some(/** 判断当前条目是否满足任一匹配条件。 入参：field。 */ (field) =>
        String(field).toLowerCase().includes(keyword.value.toLowerCase()),
      )

    return matchStatus && matchKeyword
  }),
)

/**
 * 加载环境记录集合。
 */
const loadEnvironmentRecords = async () => {
  isLoading.value = true
  try {
    const { data } = await http.get(API_ENDPOINTS.courseEnvironmentRequests.list)
    const records = normalizeListResponse(data)
    if (records.length) {
      environmentRecords.value = records.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => normalizeEnvironmentRecord(item))
      selectedEnvironment.value = environmentRecords.value[0] || null
    }
  } catch (error) {
    ElMessage.warning(error?.message || '后端课程环境列表暂不可用，已保留当前页面数据')
  } finally {
    isLoading.value = false
  }
}

/**
 * replace环境记录的处理逻辑。
 * 入参：record（记录）。
 * @returns normalizedRecord 的当前值。
 */
const replaceEnvironmentRecord = (record) => {
  const normalizedRecord = normalizeEnvironmentRecord(record)
  const hasRecord = environmentRecords.value.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ (item) => String(item.id) === String(normalizedRecord.id))
  environmentRecords.value = hasRecord
    ? environmentRecords.value.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => (String(item.id) === String(normalizedRecord.id) ? normalizedRecord : item))
    : [normalizedRecord, ...environmentRecords.value]
  selectedEnvironment.value = normalizedRecord
  return normalizedRecord
}

/**
 * 导出环境记录集合。
 */
const exportEnvironmentRecords = () => {
  const exportRows = filteredEnvironments.value

  if (!exportRows.length) {
    ElMessage.warning('暂无可导出的课程环境清单')
    return
  }

  const exportedCount = exportTableToExcel({
    filename: '课程环境清单',
    sheetName: '课程环境清单',
    rows: exportRows,
    columns: [
      { label: '课程', prop: 'course' },
      { label: '实验室', prop: 'lab' },
      { label: '上课时间', prop: 'schedule' },
      { label: '软件环境', prop: 'software' },
      { label: '负责人', prop: 'manager' },
      { label: '状态', prop: 'status' },
      { label: '流程阶段', prop: 'step' },
      { label: '提醒', prop: 'reminder' },
      { label: '交付材料', prop: 'assets' },
      { label: '处理步骤', prop: 'steps' },
    ],
  })
  ElMessage.success(`已导出 ${exportedCount} 条课程环境记录`)
}

/**
 * 处理HeroAction。
 * 入参：action（action）。
 */
const handleHeroAction = async (action) => {
  if (action.command === 'create') {
    requestPrefill.value = null
    requestDialogVisible.value = true
    return
  }

  if (action.command === 'remind') {
    await loadEnvironmentRecords()
    ElMessage.success('已同步后端课程环境需求')
    return
  }

  if (action.command === 'export') {
    exportEnvironmentRecords()
  }
}

/**
 * 处理Back。
 */
const handleBack = () => {
  router.push(roleHomePath.value)
}

/**
 * 处理OpenDrawer。
 * 入参：record（记录）。
 */
const handleOpenDrawer = (record) => {
  selectedEnvironment.value = record
  drawerVisible.value = true
}

/**
 * 构建请求Prefill来源查询。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildRequestPrefillFromQuery = () => ({
  courseName: String(route.query.course || ''),
  className: String(route.query.className || ''),
  useTime: String(route.query.time || ''),
  labType: String(route.query.lab || ''),
  softwareRequirements: String(route.query.environment || ''),
  specialRequirements: route.query.lab ? `工作台带入实验室：${route.query.lab}` : '',
})

/**
 * 应用Route课程上下文。
 */
const applyRouteCourseContext = () => {
  const course = String(route.query.course || '').trim()
  if (!course) return

  keyword.value = course
  const matchedRecord = environmentRecords.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.course === course)
  if (matchedRecord) {
    selectedEnvironment.value = matchedRecord
    return
  }

  requestPrefill.value = buildRequestPrefillFromQuery()
  requestDialogVisible.value = true
}

/**
 * 提交环境请求。
 * 入参：payload（载荷）。
 */
const submitEnvironmentRequest = async (payload) => {
  const currentUser = getStoredUser() || {}
  const numericTeacherId = Number(currentUser.id)
  const requestPayload = {
    ...payload,
    course: payload.courseName,
    teacherUserId: Number.isFinite(numericTeacherId) ? numericTeacherId : undefined,
    teacherName: currentUser.realName || currentUser.name || currentUser.username || '任课教师',
    teacher: currentUser.realName || currentUser.name || currentUser.username || '任课教师',
    processStatus: '待配置',
    status: '待确认',
    confirmStatus: '待确认',
    tagType: 'warning',
  }

  try {
    const { data } = await http.post(API_ENDPOINTS.courseEnvironmentRequests.list, requestPayload)
    const savedRecord = replaceEnvironmentRecord(normalizeRecordResponse(data, requestPayload))
    ElMessage.success(`已提交 ${savedRecord.course} 的课程环境需求`)
  } catch (error) {
    ElMessage.error(error?.message || '课程环境需求提交失败')
  }
}

/**
 * 处理Confirm。
 * 入参：record（记录）。
 */
const handleConfirm = async (record) => {
  try {
    const { data } = await http.post(`${API_ENDPOINTS.courseEnvironmentRequests.detail}/${record.id}/confirm`)
    const savedRecord = replaceEnvironmentRecord(normalizeRecordResponse(data, record))
    drawerVisible.value = false
    ElMessage.success(`已确认 ${savedRecord.course} 的课程环境`)
  } catch (error) {
    ElMessage.error(error?.message || '课程环境确认失败')
  }
}

/**
 * 处理Return。
 * 入参：record（记录）。
 */
const handleReturn = (record) => {
  selectedEnvironment.value = record
  returnForm.reason = ''
  returnDialogVisible.value = true
}

/**
 * 提交Return。
 */
const submitReturn = async () => {
  if (!selectedEnvironment.value) return

  const requestPayload = {
    ...selectedEnvironment.value,
    courseName: selectedEnvironment.value.course,
    softwareRequirements: selectedEnvironment.value.software,
    useTime: selectedEnvironment.value.schedule,
    processStatus: '需调整',
    status: '需调整',
    confirmStatus: '待确认',
    specialRequirements: returnForm.reason || selectedEnvironment.value.reminder || '教师退回环境，请按意见调整',
  }

  try {
    const { data } = await http.put(`${API_ENDPOINTS.courseEnvironmentRequests.detail}/${selectedEnvironment.value.id}`, requestPayload)
    replaceEnvironmentRecord(normalizeRecordResponse(data, requestPayload))
    returnDialogVisible.value = false
    drawerVisible.value = false
    ElMessage.warning('已退回课程环境并附带调整说明')
  } catch (error) {
    ElMessage.error(error?.message || '退回课程环境失败')
  }
}

onMounted(/** 在组件挂载后初始化相关状态。 */ async () => {
  await loadEnvironmentRecords()
  applyRouteCourseContext()
})
</script>

<template>
  <section class="teacher-page">
    <PageHero
      :actions="pageActions"
      badge="课程环境"
      page-title="我的课程环境"
      page-description="教师可按课程查看环境准备进度、截图说明，并在线确认或退回调整。"
      :page-icon="Collection"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <MetricGrid :metrics="metrics" />

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar-content">
        <el-tabs v-model="activeStatus" class="status-tabs">
          <el-tab-pane v-for="item in statusOptions" :key="item" :label="item" :name="item" />
        </el-tabs>

        <el-input v-model="keyword" clearable class="toolbar-search" placeholder="搜索课程、实验室或软件环境" />
      </div>
    </el-card>

    <el-row v-loading="isLoading" :gutter="18">
      <el-col :xs="24" :xl="15">
        <div class="environment-grid">
          <article v-for="item in filteredEnvironments" :key="item.id" class="environment-card">
            <div class="card-top">
              <div>
                <h3>{{ item.course }}</h3>
                <p>{{ item.lab }} · {{ item.schedule }}</p>
              </div>
              <el-tag :type="item.tagType" effect="light">{{ item.status }}</el-tag>
            </div>

            <div class="detail-line">
              <span>软件环境</span>
              <strong>{{ item.software }}</strong>
            </div>
            <div class="detail-line">
              <span>负责管理员</span>
              <strong>{{ item.manager }}</strong>
            </div>

            <el-progress :percentage="Math.round((item.step / 3) * 100)" :stroke-width="8" />

            <el-alert :closable="false" :title="item.reminder" type="info" show-icon />

            <div class="card-actions">
              <el-button @click="handleOpenDrawer(item)">查看详情</el-button>
              <el-button v-if="item.status !== '已确认'" type="primary" @click="handleConfirm(item)">确认环境</el-button>
              <el-button v-if="item.status !== '已确认'" type="danger" plain @click="handleReturn(item)">退回调整</el-button>
            </div>
          </article>

          <el-empty v-if="!filteredEnvironments.length" description="暂无匹配的课程环境" />
        </div>
      </el-col>

      <el-col :xs="24" :xl="9">
        <el-card class="focus-card" shadow="never">
          <template #header>
            <div class="section-title">
              <div>
                <h2>当前关注课程</h2>
                <span>展示最近一次点击的环境详情摘要</span>
              </div>
            </div>
          </template>

          <template v-if="selectedEnvironment">
            <h3 class="focus-title">{{ selectedEnvironment.course }}</h3>
            <p class="focus-copy">{{ selectedEnvironment.reminder }}</p>

            <el-descriptions :column="1" class="focus-descriptions">
              <el-descriptions-item label="实验室">{{ selectedEnvironment.lab }}</el-descriptions-item>
              <el-descriptions-item label="上课时间">{{ selectedEnvironment.schedule }}</el-descriptions-item>
              <el-descriptions-item label="软件">{{ selectedEnvironment.software }}</el-descriptions-item>
            </el-descriptions>

            <div class="step-list">
              <article
                v-for="(step, index) in selectedEnvironment.steps"
                :key="step.title"
                class="step-card"
                :class="{ active: index < selectedEnvironment.step }"
              >
                <strong>{{ step.title }}</strong>
                <p>{{ step.description }}</p>
              </article>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <TeacherEnvironmentDrawer
      v-model="drawerVisible"
      :environment="selectedEnvironment"
      @confirm="handleConfirm"
      @return="handleReturn"
    />

    <TeacherEnvironmentRequestDialog
      v-model="requestDialogVisible"
      :prefill="requestPrefill"
      @save="submitEnvironmentRequest"
    />

    <el-dialog v-model="returnDialogVisible" align-center title="退回调整说明" width="min(520px, calc(100vw - 32px))">
      <el-form label-position="top">
        <el-form-item label="退回原因" required>
          <el-input
            v-model="returnForm.reason"
            :rows="4"
            maxlength="120"
            placeholder="请说明不满足授课需求的地方，例如软件版本、插件、截图内容等"
            show-word-limit
            type="textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">提交退回</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.teacher-page {
  display: grid;
  gap: 18px;
}

.toolbar-card,
.environment-card,
.focus-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.toolbar-card :deep(.el-card__body),
.focus-card :deep(.el-card__body) {
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

.toolbar-search {
  width: 320px;
}

.environment-grid {
  display: grid;
  gap: 14px;
}

.environment-card {
  padding: 18px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-top h3,
.section-title h2,
.focus-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.card-top p,
.section-title span,
.focus-copy {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-line strong {
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  text-align: right;
}

.environment-card :deep(.el-progress) {
  margin: 14px 0;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.focus-descriptions {
  margin: 16px 0;
}

.step-list {
  display: grid;
  gap: 12px;
}

.step-card {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #fbfdff;
}

.step-card.active {
  border-color: #93c5fd;
  background: #f5f9ff;
}

.step-card strong {
  color: var(--text-primary);
  font-size: 14px;
}

.step-card p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 760px) {
  .toolbar-content {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-search {
    width: 100%;
  }
}
</style>
