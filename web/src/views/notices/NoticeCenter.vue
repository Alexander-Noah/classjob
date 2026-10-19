<!--
  Web 管理平台的通知公告视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Clock, User, DocumentChecked, Delete, Edit, Message, Position, Refresh, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHero from '../../components/common/PageHero.vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import { AUTH_ROLE_DEFAULT_PATH, DEFAULT_AUTH_ROLE } from '../../constants/auth'
import { getStoredRole, getStoredUser } from '../../api/services/auth'
import { noticeService } from '../../api/services/notices'

defineOptions({
  name: 'NoticeCenter',
})

const TARGET_OPTIONS = [
  { label: '全部用户', value: 'all' },
  { label: '实验室管理员', value: 'labAdmin' },
  { label: '系统管理员', value: 'systemAdmin' },
  { label: '教师', value: 'teacher' },
]

const TYPE_OPTIONS = ['系统公告', '开放安排', '维护通知', '库存预警', '预约审批', '设备维修', '环境确认']
const PRIORITY_OPTIONS = ['普通', '高', '紧急']
const STATUS_TYPES = {
  草稿: 'info',
  已发布: 'success',
  已撤回: 'warning',
  已归档: 'info',
}
const READ_TYPES = {
  未读: 'warning',
  已读: 'success',
}

const route = useRoute()
const router = useRouter()
const activeTab = ref('manage')
const loading = ref(false)
const submitting = ref(false)
const syncing = ref(false)
const notices = ref([])
const myNotices = ref([])
const recipients = ref([])
const stats = ref({})
const keyword = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const selectedNotice = ref(null)
const detailVisible = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const editingId = ref('')
const formRef = ref(null)
const formModel = reactive({
  title: '',
  noticeType: '系统公告',
  targetRole: 'labAdmin',
  priority: '普通',
  content: '',
  sourceModule: '',
  sourceId: '',
})

const pageTitle = computed(/** 计算派生状态。 */ () => (route.meta?.roleScope === 'systemAdmin' ? '平台通知公告' : '通知公告'))
const pageDescription = computed(/** 计算派生状态。 */ () =>
  route.meta?.roleScope === 'systemAdmin'
    ? '管理全平台通知公告、提醒策略和消息分发，跟踪已读未读状态。'
    : '发布实验室开放安排、维护通知和业务提醒，查看个人未读通知。',
)
const pageActions = [
  { label: '新增通知', type: 'primary', command: 'create' },
  { label: '同步业务提醒', type: 'default', command: 'sync' },
]

const targetLabelMap = Object.fromEntries(TARGET_OPTIONS.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => [item.value, item.label]))
const roleScope = computed(/** 计算派生状态。 */ () => route.meta?.roleScope || getStoredRole() || DEFAULT_AUTH_ROLE)
/**
 * 获取目标标签。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getTargetLabel = (value) => targetLabelMap[value] || value || '-'
/**
 * 获取状态类型。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getStatusType = (status) => STATUS_TYPES[status] || 'info'
/**
 * 获取Read类型。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const getReadType = (status) => READ_TYPES[status] || 'info'
/**
 * 归一化日期时间。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeDateTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '-')

/**
 * 归一化通知。
 * 入参：notice（通知）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeNotice = (notice = {}) => ({
  ...notice,
  title: notice.title || '系统提醒',
  noticeType: notice.noticeType || notice.type || '系统公告',
  type: notice.type || notice.noticeType || '系统公告',
  targetRole: notice.targetRole || notice.target || 'labAdmin',
  target: notice.target || notice.targetRole || 'labAdmin',
  priority: notice.priority || '普通',
  status: notice.status || notice.publishStatus || '草稿',
  publishStatus: notice.publishStatus || notice.status || '草稿',
  publishTimeText: normalizeDateTime(notice.publishTime),
  recipientCount: Number(notice.recipientCount || 0),
  readCount: Number(notice.readCount || 0),
  unreadCount: Number(notice.unreadCount || 0),
})

const publisherLabel = computed(() => {
  const notice = selectedNotice.value || {}
  if (notice.publisherName || notice.createdByName) return notice.publisherName || notice.createdByName
  const user = getStoredUser()
  if (notice.createdBy != null && user?.id != null && String(notice.createdBy) === String(user.id)) {
    return user.realName || user.name || user.username
  }
  return notice.createdBy != null ? '用户 ' + notice.createdBy : '未记录'
})

const filteredNotices = computed(/** 计算派生状态。 */ () => {
  const searchText = keyword.value.trim().toLowerCase()

  return notices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：notice。 */ (notice) => {
    const matchesStatus = statusFilter.value === 'all' || notice.status === statusFilter.value
    const matchesType = typeFilter.value === 'all' || notice.noticeType === typeFilter.value
    const matchesKeyword =
      !searchText ||
      [notice.title, notice.noticeType, notice.targetRole, notice.content, notice.sourceModule]
        .filter(Boolean)
        .some(/** 判断当前条目是否满足任一匹配条件。 入参：value。 */ (value) => String(value).toLowerCase().includes(searchText))

    return matchesStatus && matchesType && matchesKeyword
  })
})

const unreadNotices = computed(/** 计算派生状态。 */ () => myNotices.value.filter(/** 判断当前条目是否符合筛选条件。 入参：notice。 */ (notice) => notice.readStatus === '未读'))

const metrics = computed(/** 计算派生状态。 */ () => [
  { label: '通知总数', value: String(stats.value.total ?? notices.value.length), trend: `已发布 ${stats.value.publishedCount ?? 0} 条` },
  { label: '草稿箱', value: String(stats.value.draftCount ?? 0), trend: '可继续编辑发布' },
  { label: '未读通知', value: String(stats.value.myUnread ?? unreadNotices.value.length), trend: `已读 ${stats.value.myRead ?? 0} 条` },
  { label: '触达记录', value: String(stats.value.deliveryCount ?? 0), trend: `阅读 ${stats.value.readCount ?? 0} 次` },
])

/**
 * 重置Form。
 */
const resetForm = () => {
  Object.assign(formModel, {
    title: '',
    noticeType: '系统公告',
    targetRole: roleScope.value === 'systemAdmin' ? 'labAdmin' : 'labAdmin',
    priority: '普通',
    content: '',
    sourceModule: '',
    sourceId: '',
  })
}

/**
 * 安全Load的处理逻辑。
 * 入参：request（请求）；fallback（fallback）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const safeLoad = async (request, fallback) => {
  try {
    return await request()
  } catch (error) {
    console.warn('[NoticeCenter] optional notice data failed to load', error)
    return fallback
  }
}

/**
 * 加载数据。
 */
const loadData = async () => {
  loading.value = true
  try {
    const [noticeRows, userRows, statsData] = await Promise.all([
      safeLoad(/** 处理 safeLoad 调用中的回调逻辑。 */ () => noticeService.list(), []),
      safeLoad(/** 处理 safeLoad 调用中的回调逻辑。 */ () => noticeService.userNotices(), []),
      safeLoad(/** 处理 safeLoad 调用中的回调逻辑。 */ () => noticeService.stats(), {}),
    ])
    notices.value = noticeRows.map(normalizeNotice)
    myNotices.value = userRows.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => ({
      ...normalizeNotice(item),
      id: item.id,
      noticeId: item.noticeId || item.id,
      readStatus: item.readStatus || '未读',
      readTimeText: normalizeDateTime(item.readTime),
    }))
    stats.value = statsData || {}
  } finally {
    loading.value = false
  }
}

/**
 * 加载Recipients。
 * 入参：noticeId（通知标识）。
 */
const loadRecipients = async (noticeId) => {
  if (!noticeId) {
    recipients.value = []
    return
  }
  recipients.value = await safeLoad(/** 处理 safeLoad 调用中的回调逻辑。 */ () => noticeService.recipients(noticeId), [])
}

/**
 * 处理HeroAction。
 * 入参：action（action）。
 */
const handleHeroAction = (action) => {
  if (action.command === 'create') {
    openCreateDialog()
    return
  }
  if (action.command === 'sync') {
    syncBusinessReminders()
  }
}

/**
 * 打开CreateDialog。
 */
const openCreateDialog = () => {
  dialogMode.value = 'create'
  editingId.value = ''
  resetForm()
  dialogVisible.value = true
}

/**
 * 打开EditDialog。
 * 入参：notice（通知）。
 */
const openEditDialog = (notice) => {
  dialogMode.value = 'edit'
  editingId.value = notice.id
  Object.assign(formModel, {
    title: notice.title,
    noticeType: notice.noticeType,
    targetRole: notice.targetRole,
    priority: notice.priority,
    content: notice.content || '',
    sourceModule: notice.sourceModule || '',
    sourceId: notice.sourceId || '',
  })
  dialogVisible.value = true
}

/**
 * 构建载荷。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const buildPayload = (status) => ({
  title: formModel.title.trim(),
  type: formModel.noticeType,
  noticeType: formModel.noticeType,
  target: formModel.targetRole,
  targetRole: formModel.targetRole,
  priority: formModel.priority,
  content: formModel.content.trim(),
  sourceModule: formModel.sourceModule.trim(),
  sourceId: formModel.sourceId.trim(),
  status,
  publishStatus: status,
})

/**
 * 校验Form。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const validateForm = async () => {
  if (!formRef.value) return true
  return formRef.value.validate().catch(/** 处理异步失败结果。 */ () => false)
}

/**
 * 保存通知。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const saveNotice = async (status = '草稿') => {
  if (!(await validateForm())) return null

  submitting.value = true
  try {
    const payload = buildPayload(status)
    const savedNotice = editingId.value
      ? await noticeService.update(editingId.value, payload)
      : await noticeService.create(payload)
    editingId.value = savedNotice.id
    ElMessage.success(status === '草稿' ? '草稿已保存' : '通知已保存')
    await loadData()
    return normalizeNotice(savedNotice)
  } finally {
    submitting.value = false
  }
}

/**
 * 保存Draft。
 */
const saveDraft = async () => {
  await saveNotice('草稿')
  dialogVisible.value = false
}

/**
 * publish来源Dialog的处理逻辑。
 */
const publishFromDialog = async () => {
  const savedNotice = await saveNotice('已发布')
  if (!savedNotice?.id) return

  submitting.value = true
  try {
    await noticeService.publish(savedNotice.id)
    dialogVisible.value = false
    ElMessage.success('通知已发布并写入通知对象')
    await loadData()
  } finally {
    submitting.value = false
  }
}

/**
 * publish通知的处理逻辑。
 * 入参：notice（通知）。
 */
const publishNotice = async (notice) => {
  await noticeService.publish(notice.id)
  ElMessage.success('通知已发布并写入通知对象')
  await loadData()
  if (selectedNotice.value?.id === notice.id) {
    selectedNotice.value = notices.value.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.id === notice.id) || null
    await loadRecipients(notice.id)
  }
}

/**
 * withdraw通知的处理逻辑。
 * 入参：notice（通知）。
 */
const withdrawNotice = async (notice) => {
  await noticeService.withdraw(notice.id)
  ElMessage.success('通知已撤回')
  await loadData()
}

/**
 * 归档通知的处理逻辑。
 * 入参：notice（通知）。
 */
const archiveNotice = async (notice) => {
  await noticeService.archive(notice.id)
  ElMessage.success('通知已归档')
  await loadData()
}

/**
 * 删除通知。
 * 入参：notice（通知）。
 */
const deleteNotice = async (notice) => {
  try {
    await ElMessageBox.confirm(`确认删除“${notice.title}”吗？删除后保留审计痕迹，不做物理删除。`, '删除通知', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  await noticeService.remove(notice.id)
  ElMessage.success('通知已删除')
  if (selectedNotice.value?.id === notice.id) {
    selectedNotice.value = null
    detailVisible.value = false
  }
  await loadData()
}

/**
 * 同步BusinessReminders。
 */
const syncBusinessReminders = async () => {
  syncing.value = true
  try {
    const result = await noticeService.syncBusinessReminders()
    ElMessage.success(`已同步 ${Number(result.created || 0)} 条业务提醒`)
    await loadData()
  } finally {
    syncing.value = false
  }
}

/**
 * 视图通知的处理逻辑。
 * 入参：notice（通知）。
 */
const viewNotice = async (notice) => {
  selectedNotice.value = notice
  detailVisible.value = true
  await loadRecipients(notice.id)
}

/**
 * 读取My通知。
 * 入参：notice（通知）。
 */
const readMyNotice = async (notice) => {
  selectedNotice.value = notice
  detailVisible.value = true
  recipients.value = []
}

const markSelectedRead = async () => {
  const notice = selectedNotice.value
  if (!notice || notice.readStatus !== '未读' || submitting.value) return
  submitting.value = true
  try {
  await noticeService.markRead(notice.noticeId)
  selectedNotice.value = { ...notice, readStatus: '已读' }
  ElMessage.success('已标记为已读')
  await loadData()
  } finally { submitting.value = false }
}

/**
 * 处理Back。
 */
const handleBack = () => {
  router.push(AUTH_ROLE_DEFAULT_PATH[roleScope.value] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE])
}

onMounted(loadData)
</script>

<template>
  <section class="notice-page">
    <PageHero
      :actions="pageActions"
      badge="通知公告"
      :page-description="pageDescription"
      :page-icon="Bell"
      :page-title="pageTitle"
      @action="handleHeroAction"
      @back="handleBack"
    />

    <MetricGrid :metrics="metrics" />

    <el-card class="notice-toolbar" shadow="never">
      <div class="toolbar-left">
        <el-radio-group v-model="activeTab" size="small">
          <el-radio-button value="manage">通知管理</el-radio-button>
          <el-radio-button value="inbox">我的通知</el-radio-button>
        </el-radio-group>
        <el-tag type="warning" effect="light">未读 {{ unreadNotices.length }}</el-tag>
      </div>
      <div class="toolbar-actions">
        <el-input v-model="keyword" clearable placeholder="搜索标题、类型、内容" />
        <el-select v-model="typeFilter" placeholder="全部类型">
          <el-option label="全部类型" value="all" />
          <el-option v-for="item in TYPE_OPTIONS" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态">
          <el-option label="全部状态" value="all" />
          <el-option label="草稿" value="草稿" />
          <el-option label="已发布" value="已发布" />
          <el-option label="已撤回" value="已撤回" />
          <el-option label="已归档" value="已归档" />
        </el-select>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        <el-button :icon="Position" :loading="syncing" @click="syncBusinessReminders">同步业务提醒</el-button>
      </div>
    </el-card>

    <div class="notice-workbench">
      <el-card class="notice-list-card" shadow="never">
        <template #header>
          <div class="section-header">
            <div>
              <h2>{{ activeTab === 'manage' ? '通知主表' : '用户端通知' }}</h2>
            </div>
            <el-button v-if="activeTab === 'manage'" type="primary" @click="openCreateDialog">新增通知</el-button>
          </div>
        </template>

        <el-table
          v-if="activeTab === 'manage'"
          v-loading="loading"
          :data="filteredNotices"
          empty-text="暂无通知"
          row-key="id"
          @row-click="viewNotice"
        >
          <el-table-column prop="title" label="标题" min-width="190" show-overflow-tooltip />
          <el-table-column label="对象" min-width="120">
            <template #default="{ row }">{{ getTargetLabel(row.targetRole) }}</template>
          </el-table-column>
          <el-table-column prop="noticeType" label="类型" min-width="110" />
          <el-table-column prop="priority" label="优先级" min-width="90" />
          <el-table-column label="阅读" min-width="120">
            <template #default="{ row }">{{ row.readCount }}/{{ row.recipientCount }}</template>
          </el-table-column>
          <el-table-column prop="publishTimeText" label="发布时间" min-width="150" />
          <el-table-column label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="270">
            <template #default="{ row }">
              <el-button link type="primary" :icon="Edit" @click.stop="openEditDialog(row)">编辑</el-button>
              <el-button link type="success" :icon="Position" :disabled="row.status === '已发布'" @click.stop="publishNotice(row)">发布</el-button>
              <el-button link type="warning" :disabled="row.status !== '已发布'" @click.stop="withdrawNotice(row)">撤回</el-button>
              <el-button link :disabled="row.status === '已归档'" @click.stop="archiveNotice(row)">归档</el-button>
              <el-button link type="danger" :icon="Delete" @click.stop="deleteNotice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table
          v-else
          v-loading="loading"
          :data="myNotices"
          empty-text="暂无个人通知"
          row-key="id"
          @row-click="readMyNotice"
        >
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="noticeType" label="类型" min-width="120" />
          <el-table-column prop="priority" label="优先级" min-width="90" />
          <el-table-column label="阅读状态" min-width="110">
            <template #default="{ row }">
              <el-tag :type="getReadType(row.readStatus)" effect="light">{{ row.readStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publishTimeText" label="发布时间" min-width="150" />
          <el-table-column label="操作" fixed="right" width="130">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click.stop="readMyNotice(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>


    </div>

    <el-dialog v-model="detailVisible" title="通知详情" width="min(540px, calc(100vw - 32px))" align-center class="notice-detail-dialog" append-to-body>
      <template #header><div class="notice-dialog-heading"><el-icon><Position /></el-icon><span>通知详情</span></div></template>
      <article v-if="selectedNotice" class="notice-letter">
        <h2>{{ selectedNotice.title }}</h2>
        <el-tag size="small" type="primary" effect="light">{{ selectedNotice.noticeType }}</el-tag>
        <div class="letter-meta">
          <div><el-icon><Clock /></el-icon><div><span>发布时间</span><strong>{{ selectedNotice.publishTimeText || '尚未发布' }}</strong></div></div>
          <div><el-icon><User /></el-icon><div><span>发布人</span><strong>{{ publisherLabel }}</strong></div></div>
          <div><el-icon><DocumentChecked /></el-icon><div><span>{{ activeTab === 'manage' ? '发布状态' : '阅读状态' }}</span><el-tag size="small" :type="activeTab === 'manage' ? getStatusType(selectedNotice.status) : getReadType(selectedNotice.readStatus)">{{ activeTab === 'manage' ? selectedNotice.status : selectedNotice.readStatus }}</el-tag></div></div>
        </div>
        <div class="letter-content">{{ selectedNotice.content || '暂无内容' }}</div>
        <el-collapse v-if="activeTab === 'manage' && recipients.length" class="letter-recipients">
          <el-collapse-item :title="'阅读记录（' + recipients.length + '）'" name="recipients">
            <el-table :data="recipients" size="small" max-height="180">
              <el-table-column prop="realName" label="接收人" min-width="90" />
              <el-table-column prop="readStatus" label="状态" width="80" />
              <el-table-column label="已读时间" min-width="140"><template #default="{ row }">{{ normalizeDateTime(row.readTime) }}</template></el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </article>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="activeTab !== 'manage' && selectedNotice?.readStatus === '未读'" type="primary" :loading="submitting" @click="markSelectedRead">标记为已读</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      align-center
      :title="dialogMode === 'create' ? '新增通知' : '编辑通知'"
      width="min(680px, calc(100vw - 32px))"
    >
      <el-form ref="formRef" :model="formModel" label-position="top">
        <div class="form-grid">
          <el-form-item label="通知标题" prop="title" :rules="[{ required: true, message: '请输入通知标题' }]">
            <el-input v-model="formModel.title" maxlength="80" placeholder="请输入通知标题" />
          </el-form-item>
          <el-form-item label="通知类型" prop="noticeType" :rules="[{ required: true, message: '请选择通知类型' }]">
            <el-select v-model="formModel.noticeType">
              <el-option v-for="item in TYPE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="通知对象" prop="targetRole" :rules="[{ required: true, message: '请选择通知对象' }]">
            <el-select v-model="formModel.targetRole">
              <el-option v-for="item in TARGET_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级" prop="priority">
            <el-select v-model="formModel.priority">
              <el-option v-for="item in PRIORITY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="通知内容" prop="content" :rules="[{ required: true, message: '请输入通知内容' }]">
          <el-input v-model="formModel.content" type="textarea" :rows="6" maxlength="500" show-word-limit />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="来源模块">
            <el-input v-model="formModel.sourceModule" placeholder="如 consumable / reservation / repair" />
          </el-form-item>
          <el-form-item label="来源ID">
            <el-input v-model="formModel.sourceId" placeholder="业务记录 ID" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="submitting" @click="saveDraft">保存草稿</el-button>
        <el-button :loading="submitting" type="primary" @click="publishFromDialog">发布通知</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.notice-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.notice-page :deep(.page-hero) {
  border-color: #dbe7f5;
}

.notice-page :deep(.metric-grid) {
  gap: 14px;
}

.notice-page :deep(.metric-card) {
  min-height: 108px;
  border-color: #dbe7f5;
  border-radius: 8px;
  background:
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.notice-page :deep(.metric-card .el-card__body) {
  padding: 16px;
}

.notice-toolbar,
.notice-list-card,
.notice-detail-card {
  overflow: hidden;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: var(--surface-color);
}

.notice-toolbar :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  background: #ffffff;
}

.toolbar-left,
.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar-actions {
  justify-content: flex-end;
}

.toolbar-left :deep(.el-radio-button__inner) {
  font-weight: 700;
}

.toolbar-actions :deep(.el-input__wrapper),
.toolbar-actions :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

.toolbar-actions :deep(.el-input__wrapper:hover),
.toolbar-actions :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #93c5fd inset;
}

.toolbar-actions .el-input {
  width: 280px;
}

.toolbar-actions .el-select {
  width: 130px;
}

.notice-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.notice-list-card,
.notice-detail-card {
  min-width: 0;
}

.notice-list-card :deep(.el-card__body) {
  padding: 0;
}

.notice-list-card :deep(.el-card__header),
.notice-detail-card :deep(.el-card__header) {
  min-height: 68px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5edf6;
  background: #fbfdff;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.section-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
}

.detail-grid span,
.read-summary span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
}

.notice-list-card :deep(.el-table) {
  --el-table-border-color: #e5edf6;
  --el-table-header-bg-color: #f8fbff;
  --el-table-row-hover-bg-color: #eff6ff;
}

.notice-list-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.notice-list-card :deep(th.el-table__cell) {
  height: 46px;
  background: #f8fbff !important;
  color: #475569;
  font-weight: 700;
}

.notice-list-card :deep(.el-table__body td) {
  height: 56px;
  color: var(--text-primary);
}

.notice-list-card :deep(.el-button.is-link) {
  font-weight: 700;
}

.detail-stack {
  display: grid;
  gap: 22px;
}

.detail-section h3 {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
}

.detail-title .el-tag { flex-shrink: 0; }

.detail-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-title strong {
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.notice-content {
  margin: 0;
  padding: 0;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.detail-grid,
.read-summary,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid > div,
.read-summary > div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #f8fafc;
}

.read-summary {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-grid {
  padding: 18px 0;
  border-top: 1px solid #edf0f5;
  border-bottom: 1px solid #edf0f5;
  gap: 18px 16px;
}

.detail-grid > div {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
}

.detail-grid strong {
  font-size: 13px;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.read-summary > div {
  gap: 8px;
  padding: 12px 14px;
  border: 0;
  border-radius: 10px;
  background: #f4f7fb;
}

.read-summary > div:nth-child(2) { background: #f0f9f4; }
.read-summary > div:nth-child(3) { background: #fff8ed; }
.read-summary > div strong { font-size: 24px; line-height: 1.2; }

.detail-grid strong,
.read-summary strong {
  color: var(--text-primary);
  font-size: 15px;
}

.notice-detail-dialog :deep(.el-table) {
  --el-table-border-color: #e5edf6;
}

/* Teleport 弹窗独立覆盖全局玻璃材质，保持小尺寸下的阅读清晰度。 */
:global(body .el-dialog.notice-detail-dialog) {
  padding: 0;
  border: 1px solid #e5eaf2;
  border-radius: 16px;
  background: #fff;
  backdrop-filter: none;
  box-shadow: 0 20px 64px rgba(15, 23, 42, .16);
}

.notice-detail-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 16px 48px 16px 22px;
  border-bottom: 1px solid #eef1f6;
}

.notice-detail-dialog :deep(.el-dialog__body) {
  max-height: calc(80svh - 72px);
  overflow-y: auto;
  padding: 18px 22px 0;
}

.notice-detail-dialog .detail-stack { gap: 14px; }
.notice-detail-dialog .detail-title strong { font-size: 19px; }
.notice-detail-dialog .detail-section h3 { margin-bottom: 6px; }
.notice-detail-dialog .detail-grid { padding: 12px 0; gap: 10px 16px; }
.notice-detail-dialog .detail-grid > div { gap: 3px; }
.notice-detail-dialog .detail-grid span,
.notice-detail-dialog .read-summary span { margin-top: 0; font-size: 12px; }
.notice-detail-dialog .read-summary { gap: 8px; }
.notice-detail-dialog .read-summary > div { padding: 8px 12px; gap: 4px; }
.notice-detail-dialog .read-summary > div strong { font-size: 20px; }

.notice-dialog-heading { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #263449; }
.notice-dialog-heading .el-icon { color: #287aff; font-size: 20px; }
.notice-letter h2 { margin: 0 0 8px; font-size: 19px; line-height: 1.5; color: #1e293b; overflow-wrap: anywhere; }
.letter-meta { display: grid; grid-template-columns: 1.15fr 1fr .8fr; margin: 16px 0; }
.letter-meta > div { display: flex; gap: 10px; align-items: center; padding: 0 12px; min-width: 0; border-left: 1px solid #e9edf5; }
.letter-meta > div:first-child { border: 0; padding-left: 0; }
.letter-meta .el-icon { color: #7182a0; font-size: 20px; flex-shrink: 0; }
.letter-meta span { display: block; color: #8190a5; font-size: 11px; margin-bottom: 4px; }
.letter-meta strong { display: block; font-size: 11px; font-weight: 500; color: #334155; overflow-wrap: anywhere; }
.letter-meta .el-tag { display: inline-flex; margin: 0; }
.letter-content { padding: 16px; border-radius: 8px; background: #f4f7fc; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 13px; line-height: 1.9; color: #334155; }
.letter-recipients { margin-top: 12px; border: 0; }
.letter-recipients :deep(.el-collapse-item__header) { min-height: 36px; height: auto; font-size: 12px; border: 0; background: transparent; }
.letter-recipients :deep(.el-collapse-item__wrap) { border: 0; }
.letter-recipients :deep(.el-collapse-item__content) { padding-bottom: 8px; }
.notice-detail-dialog :deep(.el-dialog__footer) { padding: 16px 22px; border-top: 0; }
@media (max-width: 480px) { .letter-meta { grid-template-columns: 1fr; gap: 12px; } .letter-meta > div { padding: 0; border: 0; } }

.notice-page :deep(.el-dialog:not(.notice-detail-dialog)) {
  border-radius: 8px;
}

.notice-page :deep(.el-dialog:not(.notice-detail-dialog) .el-dialog__header) {
  padding: 18px 20px;
  border-bottom: 1px solid #e5edf6;
}

.notice-page :deep(.el-dialog:not(.notice-detail-dialog) .el-dialog__body) {
  padding: 18px 20px;
}

.notice-page :deep(.el-dialog:not(.notice-detail-dialog) .el-dialog__footer) {
  padding: 14px 20px 18px;
  border-top: 1px solid #e5edf6;
}

.notice-page :deep(.el-form .el-input__wrapper),
.notice-page :deep(.el-form .el-select__wrapper),
.notice-page :deep(.el-form .el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe7f5 inset;
}

@media (max-width: 1180px) {
  .notice-workbench {
    grid-template-columns: 1fr;
  }

  .notice-toolbar :deep(.el-card__body) {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 720px) {
  .detail-grid,
  .read-summary,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-actions .el-input,
  .toolbar-actions .el-select {
    width: 100%;
  }

  .toolbar-actions,
  .toolbar-left {
    display: grid;
    grid-template-columns: 1fr;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
