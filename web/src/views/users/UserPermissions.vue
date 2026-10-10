<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, MoreFilled, Plus, Search, Upload, User } from '@element-plus/icons-vue'
import MetricGrid from '../../components/common/MetricGrid.vue'
import TablePagination from '../../components/common/TablePagination.vue'
import { useTablePagination } from '../../composables/useTablePagination'
import { exportTableToExcel } from '../../utils/exportExcel'
import {
  batchDeleteUsers,
  batchDisableUsers,
  batchEnableUsers,
  batchUpdateDepartment,
  batchUpdateRole,
  createUser,
  deleteUser,
  importUsers,
  listUsers,
  resetUserPassword,
  updateUser,
  updateUserStatus,
} from '../../api/services/users'
import { getStoredUser, updateStoredUser } from '../../api/services/auth'
import PermissionDetailDrawer from './components/PermissionDetailDrawer.vue'
import PermissionUserDialog from './components/PermissionUserDialog.vue'

defineOptions({
  name: 'UserManagement',
})

const ROLE_OPTIONS = ['系统管理员', '实验室管理员', '教师', '教务人员']
const DEPARTMENT_OPTIONS = ['信息中心', '实验中心', '软件学院', '信息工程学院', '运维组', '教务处']

const ROLE_CODE_MAP = {
  系统管理员: 'systemAdmin',
  实验室管理员: 'labAdmin',
  教师: 'teacher',
  教务人员: 'academic',
}

const ROLE_NAME_MAP = Object.fromEntries(Object.entries(ROLE_CODE_MAP).map(([name, code]) => [code, name]))

const PERMISSIONS_BY_ROLE = {
  系统管理员: [{ group: '平台治理', items: ['用户管理', '系统设置', '激活管理', '平台统计', '通知公告'] }],
  实验室管理员: [
    { group: '资源管理', items: ['实验室管理', '课表数据', '预约管理'] },
    { group: '资产管理', items: ['设备资产', '耗材库存', '设备状态'] },
    { group: '环境运维', items: ['环境管理', '维护记录填写', '维护记录查看', '物联网扩展'] },
    { group: '数据分析', items: ['使用记录', '统计分析', 'AI智能助手'] },
    { group: '系统通知', items: ['通知公告'] },
  ],
  教师: [{ group: '个人工作', items: ['教师课表', '课程环境'] }],
  教务人员: [{ group: '教务管理', items: ['课表数据', '通知公告'] }],
}

const normalizePermissionLabel = (item) => (item === '用户权限' ? '用户管理' : item)

const parsePermissions = (permissions, role) => {
  try {
    const parsed = typeof permissions === 'string' ? JSON.parse(permissions) : permissions
    if (Array.isArray(parsed) && parsed.length) {
      return parsed.map((group) => ({
        group: group.group || group.label || '权限范围',
        items: Array.isArray(group.items) ? group.items.map(normalizePermissionLabel) : [],
      }))
    }
  } catch {
    // 使用默认角色权限兜底。
  }

  return PERMISSIONS_BY_ROLE[role] || PERMISSIONS_BY_ROLE.教师
}

const serializePermissions = (role) => JSON.stringify(PERMISSIONS_BY_ROLE[role] || PERMISSIONS_BY_ROLE.教师)

const normalizeUserRecord = (record = {}) => {
  const role = record.roleName || ROLE_NAME_MAP[record.roleCode] || record.role || '教师'
  const username = record.username || record.account || record.employeeNo || ''
  const employeeNo = record.employeeNo || record.jobNo || username
  const enabled = Number(record.status ?? (record.enabled === false ? 0 : 1)) === 1

  return {
    ...record,
    account: record.account || username || employeeNo,
    builtIn: Boolean(record.builtIn || username === 'sysadmin'),
    contact: record.phone || record.email || '-',
    createdAt: record.createdAt || '2026-06-01 09:00',
    department: record.department || '未设置',
    email: record.email || '',
    employeeNo,
    enabled,
    id: record.id || username || Date.now(),
    lastLogin: record.lastLogin || '暂无登录记录',
    logs: record.logs || [{ time: '刚刚', content: '账号信息已从用户中心同步', type: 'primary' }],
    name: record.realName || record.name || username || '未命名账号',
    permissions: parsePermissions(record.permissions, role),
    phone: record.phone || '',
    remark: record.remark || '',
    role,
    roleCode: ROLE_CODE_MAP[role] || record.roleCode || 'teacher',
    statusText: enabled ? '启用' : '停用',
    tagType: enabled ? 'success' : 'info',
    username,
  }
}

const buildUserPayload = (formModel) => ({
  username: String(formModel.account || '').trim(),
  employeeNo: String(formModel.account || '').trim(),
  password: formModel.initialPassword || undefined,
  realName: String(formModel.name || '').trim(),
  phone: String(formModel.phone || '').trim(),
  email: String(formModel.email || '').trim(),
  roleName: formModel.role,
  roleCode: ROLE_CODE_MAP[formModel.role] || 'teacher',
  department: formModel.department,
  permissions: serializePermissions(formModel.role),
  status: formModel.enabled ? 1 : 0,
  remark: String(formModel.remark || '').trim(),
})

const tableRef = ref(null)
const uploadRef = ref(null)
const isLoading = ref(false)
const userList = ref([])
const selectedRows = ref([])
const selectedUser = ref(null)
const detailVisible = ref(false)
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const departmentDialogVisible = ref(false)
const importDialogVisible = ref(false)
const importFile = ref(null)

const filterModel = reactive({
  department: '全部',
  keyword: '',
  role: '全部',
  status: '全部',
})

const queryModel = reactive({
  department: '全部',
  keyword: '',
  role: '全部',
  status: '全部',
})

const batchRoleForm = reactive({
  role: '',
})

const batchDepartmentForm = reactive({
  department: '',
})

const departmentOptions = computed(() => [
  ...new Set([...DEPARTMENT_OPTIONS, ...userList.value.map((item) => item.department).filter(Boolean)]),
])

const currentUser = computed(() => getStoredUser())

const systemAdminCount = computed(() => userList.value.filter((item) => item.role === '系统管理员').length)

const metrics = computed(() => {
  const enabledCount = userList.value.filter((item) => item.enabled).length
  const disabledCount = userList.value.length - enabledCount
  const teacherCount = userList.value.filter((item) => item.role === '教师').length
  const adminCount = userList.value.filter((item) => ['系统管理员', '实验室管理员'].includes(item.role)).length

  return [
    { label: '用户总数', value: String(userList.value.length), trend: '覆盖平台全部角色' },
    { label: '启用账号', value: String(enabledCount), trend: '可正常登录系统' },
    { label: '教师账号', value: String(teacherCount), trend: '教学侧账号规模' },
    { label: '管理员账号', value: String(adminCount), trend: '系统与实验室管理' },
    { label: '禁用账号', value: String(disabledCount), trend: '需复核或归档账号' },
  ]
})

const filteredUsers = computed(() =>
  userList.value.filter((item) => {
    const keyword = queryModel.keyword.trim().toLowerCase()
    const matchKeyword =
      !keyword ||
      [item.name, item.phone, item.email, item.employeeNo, item.username, item.department]
        .filter(Boolean)
        .some((field) => String(field).toLowerCase().includes(keyword))
    const matchRole = queryModel.role === '全部' || item.role === queryModel.role
    const matchStatus = queryModel.status === '全部' || item.statusText === queryModel.status
    const matchDepartment = queryModel.department === '全部' || item.department === queryModel.department

    return matchKeyword && matchRole && matchStatus && matchDepartment
  }),
)

const {
  currentPage,
  handleCurrentPageChange,
  handlePageSizeChange,
  pageSize,
  pagedRecords: pagedUsers,
} = useTablePagination(filteredUsers)

const isCurrentUser = (user) => {
  const storedUser = currentUser.value
  if (!storedUser || !user) return false

  return (
    (storedUser.id && String(storedUser.id) === String(user.id)) ||
    (storedUser.username && String(storedUser.username).toLowerCase() === String(user.username).toLowerCase())
  )
}

const isLastSystemAdmin = (user) => user?.role === '系统管理员' && systemAdminCount.value <= 1

const canDeleteUser = (user) => !user?.builtIn && !isLastSystemAdmin(user)

const clearSelection = () => {
  selectedRows.value = []
  nextTick(() => tableRef.value?.clearSelection())
}

const syncSelectedUser = (updatedUser) => {
  if (selectedUser.value && String(selectedUser.value.id) === String(updatedUser.id)) {
    selectedUser.value = updatedUser
  }
}

const syncCurrentSessionUser = (updatedUser) => {
  if (!isCurrentUser(updatedUser)) return

  updateStoredUser(
    {
      id: updatedUser.id,
      username: updatedUser.username,
      name: updatedUser.name,
      realName: updatedUser.name,
      role: updatedUser.role,
      roleCode: updatedUser.roleCode,
      roleName: updatedUser.role,
      department: updatedUser.department,
      contact: updatedUser.phone || updatedUser.email,
      enabled: updatedUser.enabled,
      permissions: updatedUser.permissions,
    },
    updatedUser.roleCode,
  )
}

const showActionError = (error, fallback) => {
  if (error?.__handled) {
    return
  }

  ElMessage.error(error?.userMessage || error?.response?.data?.message || error?.message || fallback)
}

const refreshUserList = async () => {
  isLoading.value = true
  try {
    const records = await listUsers()
    userList.value = records.map(normalizeUserRecord)
  } catch (error) {
    showActionError(error, '用户列表加载失败')
  } finally {
    isLoading.value = false
  }
}

const handleSearch = () => {
  Object.assign(queryModel, filterModel)
  currentPage.value = 1
}

const handleReset = () => {
  Object.assign(filterModel, {
    department: '全部',
    keyword: '',
    role: '全部',
    status: '全部',
  })
  handleSearch()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleOpenCreate = () => {
  selectedUser.value = null
  userDialogVisible.value = true
}

const handleOpenEdit = (user) => {
  selectedUser.value = user
  userDialogVisible.value = true
}

const handleOpenDetail = (user) => {
  selectedUser.value = user
  detailVisible.value = true
}

const handleSaveUser = async (formModel) => {
  const payload = buildUserPayload(formModel)
  const nextRole = formModel.role

  if (selectedUser.value?.id && isLastSystemAdmin(selectedUser.value) && nextRole !== '系统管理员') {
    ElMessage.error('最后一个系统管理员不能降级')
    return
  }

  try {
    if (selectedUser.value?.id) {
      const savedUser = await updateUser(selectedUser.value.id, payload)
      const normalizedUser = normalizeUserRecord({ ...selectedUser.value, ...payload, ...savedUser })
      userList.value = userList.value.map((item) => (String(item.id) === String(normalizedUser.id) ? normalizedUser : item))
      syncSelectedUser(normalizedUser)
      syncCurrentSessionUser(normalizedUser)
      ElMessage.success('用户信息已更新')
    } else {
      const savedUser = await createUser(payload)
      const normalizedUser = normalizeUserRecord({ ...payload, ...savedUser })
      userList.value.unshift(normalizedUser)
      ElMessage.success('用户已新增')
    }

    userDialogVisible.value = false
  } catch (error) {
    showActionError(error, '用户保存失败')
  }
}

const handleToggleStatus = async (user) => {
  const nextStatus = user.enabled ? '停用' : '启用'

  if (nextStatus === '停用' && isCurrentUser(user)) {
    ElMessage.warning('当前登录的系统管理员不能停用自己')
    return
  }

  try {
    const savedUser = await updateUserStatus(user.id, nextStatus)
    const normalizedUser = normalizeUserRecord({
      ...user,
      ...savedUser,
      status: nextStatus === '启用' ? 1 : 0,
    })
    userList.value = userList.value.map((item) => (String(item.id) === String(user.id) ? normalizedUser : item))
    syncSelectedUser(normalizedUser)
    syncCurrentSessionUser(normalizedUser)
    ElMessage.success(`用户已${nextStatus}`)
  } catch (error) {
    showActionError(error, '状态更新失败')
  }
}

const confirmDanger = (message, title = '操作确认') =>
  ElMessageBox.confirm(message, title, {
    cancelButtonText: '取消',
    confirmButtonText: '确认',
    type: 'warning',
  })

const handleResetPassword = async (user) => {
  try {
    await confirmDanger(`确认重置“${user.name}”的登录密码？`, '重置密码')
    await resetUserPassword(user.id)
    await refreshUserList()
    ElMessage.success('密码已重置')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showActionError(error, '重置密码失败')
    }
  }
}

const handleDeleteUser = async (user) => {
  if (user.builtIn) {
    ElMessage.warning('系统内置管理员账号不能删除')
    return
  }

  if (isLastSystemAdmin(user)) {
    ElMessage.warning('最后一个系统管理员不能删除')
    return
  }

  try {
    await confirmDanger(`确认删除用户“${user.name}”？删除后不可直接恢复。`, '删除用户')
    await deleteUser(user.id)
    userList.value = userList.value.filter((item) => String(item.id) !== String(user.id))
    clearSelection()
    ElMessage.success('用户已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showActionError(error, '删除用户失败')
    }
  }
}

const handleRowCommand = (command, user) => {
  if (command === 'status') handleToggleStatus(user)
  if (command === 'delete') handleDeleteUser(user)
}

const getSelectedIds = (rows = selectedRows.value) => rows.map((item) => item.id)

const warnSkippedUsers = (skippedUsers) => {
  if (skippedUsers.length) {
    ElMessage.warning(`已跳过 ${skippedUsers.length} 个受保护账号`)
  }
}

const handleBatchEnable = async () => {
  if (!selectedRows.value.length) return

  try {
    const ids = getSelectedIds()
    await batchEnableUsers(ids)
    userList.value = userList.value.map((item) => (ids.includes(item.id) ? normalizeUserRecord({ ...item, status: 1 }) : item))
    clearSelection()
    ElMessage.success('批量启用完成')
  } catch (error) {
    showActionError(error, '批量启用失败')
  }
}

const handleBatchDisable = async () => {
  const skippedUsers = selectedRows.value.filter(isCurrentUser)
  const operableUsers = selectedRows.value.filter((item) => !isCurrentUser(item))

  if (!operableUsers.length) {
    ElMessage.warning('没有可停用的用户')
    return
  }

  try {
    await confirmDanger(`确认批量停用 ${operableUsers.length} 个用户？`, '批量停用')
    const ids = getSelectedIds(operableUsers)
    await batchDisableUsers(ids)
    userList.value = userList.value.map((item) => (ids.includes(item.id) ? normalizeUserRecord({ ...item, status: 0 }) : item))
    warnSkippedUsers(skippedUsers)
    clearSelection()
    ElMessage.success('批量停用完成')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showActionError(error, '批量停用失败')
    }
  }
}

const handleBatchDelete = async () => {
  const skippedUsers = selectedRows.value.filter((item) => !canDeleteUser(item))
  const operableUsers = selectedRows.value.filter(canDeleteUser)

  if (!operableUsers.length) {
    ElMessage.warning('没有可删除的用户')
    return
  }

  try {
    await confirmDanger(`确认批量删除 ${operableUsers.length} 个用户？该操作不可直接恢复。`, '批量删除')
    const ids = getSelectedIds(operableUsers)
    await batchDeleteUsers(ids)
    userList.value = userList.value.filter((item) => !ids.includes(item.id))
    warnSkippedUsers(skippedUsers)
    clearSelection()
    ElMessage.success('批量删除完成')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showActionError(error, '批量删除失败')
    }
  }
}

const handleBatchResetPassword = async () => {
  if (!selectedRows.value.length) return

  try {
    await confirmDanger(`确认批量重置 ${selectedRows.value.length} 个用户的密码？`, '批量重置密码')
    await Promise.all(selectedRows.value.map((item) => resetUserPassword(item.id)))
    await refreshUserList()
    clearSelection()
    ElMessage.success('批量重置密码完成')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showActionError(error, '批量重置密码失败')
    }
  }
}

const openBatchRoleDialog = () => {
  batchRoleForm.role = ''
  roleDialogVisible.value = true
}

const openBatchDepartmentDialog = () => {
  batchDepartmentForm.department = ''
  departmentDialogVisible.value = true
}

const submitBatchRole = async () => {
  if (!batchRoleForm.role) {
    ElMessage.warning('请选择目标角色')
    return
  }

  if (batchRoleForm.role !== '系统管理员' && selectedRows.value.some(isLastSystemAdmin)) {
    ElMessage.error('最后一个系统管理员不能降级')
    return
  }

  try {
    const ids = getSelectedIds()
    await batchUpdateRole(ids, batchRoleForm.role)
    userList.value = userList.value.map((item) =>
      ids.includes(item.id)
        ? normalizeUserRecord({
            ...item,
            roleName: batchRoleForm.role,
            roleCode: ROLE_CODE_MAP[batchRoleForm.role],
            permissions: serializePermissions(batchRoleForm.role),
          })
        : item,
    )
    clearSelection()
    roleDialogVisible.value = false
    ElMessage.success('批量分配角色完成')
  } catch (error) {
    showActionError(error, '批量分配角色失败')
  }
}

const submitBatchDepartment = async () => {
  if (!batchDepartmentForm.department) {
    ElMessage.warning('请选择目标部门')
    return
  }

  try {
    const ids = getSelectedIds()
    await batchUpdateDepartment(ids, batchDepartmentForm.department)
    userList.value = userList.value.map((item) =>
      ids.includes(item.id) ? normalizeUserRecord({ ...item, department: batchDepartmentForm.department }) : item,
    )
    clearSelection()
    departmentDialogVisible.value = false
    ElMessage.success('批量调整部门完成')
  } catch (error) {
    showActionError(error, '批量调整部门失败')
  }
}

const openImportDialog = () => {
  importFile.value = null
  importDialogVisible.value = true
  nextTick(() => uploadRef.value?.clearFiles())
}

const handleImportFileChange = (file) => {
  importFile.value = file.raw || file
}

const handleImportFileRemove = () => {
  importFile.value = null
}

const downloadImportTemplate = () => {
  exportTableToExcel({
    filename: '用户导入模板',
    columns: [
      { label: '姓名', prop: 'name' },
      { label: '账号', prop: 'account' },
      { label: '手机号', prop: 'phone' },
      { label: '部门', prop: 'department' },
      { label: '角色', prop: 'role' },
      { label: '状态', prop: 'status' },
    ],
    rows: [],
  })
}

const submitImportUsers = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择 xlsx 文件')
    return
  }

  try {
    await importUsers(importFile.value)
    importDialogVisible.value = false
    ElMessage.success('导入功能待后端接口接入，已完成前端文件校验')
  } catch (error) {
    showActionError(error, '导入失败')
  }
}

const handleExportUsers = async () => {
  const exportedCount = exportTableToExcel({
    filename: '用户管理',
    columns: [
      { label: '姓名', prop: 'name' },
      { label: '账号', prop: 'username' },
      { label: '工号', prop: 'employeeNo' },
      { label: '角色', prop: 'role' },
      { label: '部门', prop: 'department' },
      { label: '手机号', prop: 'phone' },
      { label: '邮箱', prop: 'email' },
      { label: '状态', prop: 'statusText' },
      { label: '最近登录', prop: 'lastLogin' },
      { label: '创建时间', prop: 'createdAt' },
    ],
    rows: filteredUsers.value,
  })
  ElMessage.success(`已导出 ${exportedCount} 条真实用户数据`)
}

onMounted(refreshUserList)
</script>

<template>
  <section class="user-management-page">
    <el-card class="page-intro-card" shadow="never">
      <div class="page-intro">
        <div class="page-title-group">
          <span class="page-icon" aria-hidden="true">
            <el-icon><User /></el-icon>
          </span>
          <div>
            <h1>用户管理</h1>
          </div>
        </div>
        <div class="page-actions">
          <el-button type="primary" :icon="Plus" @click="handleOpenCreate">新增用户</el-button>
          <el-button :icon="Upload" @click="openImportDialog">批量导入</el-button>
          <el-button :icon="Download" @click="handleExportUsers">导出用户</el-button>
        </div>
      </div>
    </el-card>

    <MetricGrid :metrics="metrics" />

    <el-card class="filter-card" shadow="never">
      <el-form class="filter-form" :model="filterModel" label-position="top">
        <el-form-item label="关键词搜索">
          <el-input v-model="filterModel.keyword" clearable placeholder="姓名 / 手机号 / 工号 / 部门" />
        </el-form-item>
        <el-form-item label="角色筛选">
          <el-select v-model="filterModel.role" class="full-width">
            <el-option label="全部" value="全部" />
            <el-option v-for="item in ROLE_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态筛选">
          <el-select v-model="filterModel.status" class="full-width">
            <el-option label="全部" value="全部" />
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门筛选">
          <el-select v-model="filterModel.department" class="full-width" filterable>
            <el-option label="全部" value="全部" />
            <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-buttons">
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div v-if="selectedRows.length" class="batch-toolbar">
        <span>已选择 {{ selectedRows.length }} 个用户</span>
        <div class="batch-actions">
          <el-button type="primary" plain @click="handleBatchEnable">批量启用</el-button>
          <el-button type="warning" plain @click="handleBatchDisable">批量停用</el-button>
          <el-button plain @click="openBatchRoleDialog">批量分配角色</el-button>
          <el-button plain @click="openBatchDepartmentDialog">批量调整部门</el-button>
          <el-button plain @click="handleBatchResetPassword">批量重置密码</el-button>
          <el-button type="danger" plain @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="isLoading"
        :data="pagedUsers"
        class="user-table"
        empty-text="暂无用户数据"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="姓名" min-width="110" show-overflow-tooltip />
        <el-table-column label="账号 / 工号" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="account-cell">
              <strong>{{ row.username }}</strong>
              <span>{{ row.employeeNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" min-width="125">
          <template #default="{ row }">
            <el-tag effect="light">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" min-width="130" show-overflow-tooltip />
        <el-table-column label="联系方式" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.phone || row.email || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="95">
          <template #default="{ row }">
            <el-tag :type="row.tagType" effect="light">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最近登录" min-width="130" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" min-width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleOpenDetail(row)">查看</el-button>
            <el-button link type="primary" @click="handleOpenEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
            <el-dropdown @command="(command) => handleRowCommand(command, row)">
              <el-button link type="primary">
                更多
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="status" :disabled="row.enabled && isCurrentUser(row)">
                    {{ row.enabled ? '停用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" :disabled="!canDeleteUser(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <TablePagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredUsers.length"
        @page-change="handleCurrentPageChange"
        @page-size-change="handlePageSizeChange"
      />
    </el-card>

    <PermissionUserDialog
      v-model="userDialogVisible"
      :department-options="departmentOptions"
      :role-options="ROLE_OPTIONS"
      :user="selectedUser"
      @save="handleSaveUser"
    />

    <PermissionDetailDrawer v-model="detailVisible" :user="selectedUser" />

    <el-dialog v-model="roleDialogVisible" align-center title="批量分配角色" width="420px">
      <p class="dialog-tip">已选择 {{ selectedRows.length }} 个用户</p>
      <el-form label-position="top">
        <el-form-item label="目标角色">
          <el-select v-model="batchRoleForm.role" class="full-width" placeholder="请选择角色">
            <el-option v-for="item in ROLE_OPTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchRole">确认分配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="departmentDialogVisible" align-center title="批量调整部门" width="420px">
      <p class="dialog-tip">已选择 {{ selectedRows.length }} 个用户</p>
      <el-form label-position="top">
        <el-form-item label="目标部门">
          <el-select v-model="batchDepartmentForm.department" allow-create class="full-width" filterable placeholder="请选择部门">
            <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchDepartment">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" align-center title="批量导入用户" width="520px">
      <div class="import-panel">
        <el-button plain @click="downloadImportTemplate">下载导入模板</el-button>
        <el-upload
          ref="uploadRef"
          accept=".xlsx,.xls"
          :auto-upload="false"
          drag
          :limit="1"
          :on-change="handleImportFileChange"
          :on-remove="handleImportFileRemove"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或点击选择 xlsx 文件</div>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImportUsers">开始导入</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.user-management-page {
  display: grid;
  gap: 16px;
}

.page-intro-card,
.filter-card,
.table-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.page-intro-card :deep(.el-card__body),
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) {
  padding: 18px 20px;
}

.page-intro,
.page-title-group,
.page-actions,
.batch-toolbar,
.batch-actions {
  display: flex;
  align-items: center;
}

.page-intro {
  justify-content: space-between;
  gap: 18px;
}

.page-title-group {
  min-width: 0;
  gap: 14px;
}

.page-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  border-radius: 8px;
  color: var(--primary-color);
  background: #eff6ff;
  font-size: 26px;
}

.page-title-group h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
}

.page-actions,
.batch-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.page-actions .el-button + .el-button,
.batch-actions .el-button + .el-button {
  margin-left: 0;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) repeat(3, minmax(160px, 1fr)) auto;
  gap: 14px;
  align-items: end;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.full-width {
  width: 100%;
}

.filter-buttons :deep(.el-form-item__content) {
  flex-wrap: nowrap;
  gap: 8px;
}

.filter-buttons .el-button + .el-button {
  margin-left: 0;
}

.batch-toolbar {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.batch-toolbar span {
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 700;
}

.user-table {
  width: 100%;
}

.user-table :deep(.el-table__header-wrapper th.el-table__cell) {
  height: 48px;
  border-bottom: 1px solid #e5edf6;
  background: #f8fbff;
  color: #334155;
  font-weight: 700;
}

.user-table :deep(.el-table__body-wrapper td.el-table__cell) {
  height: 58px;
  border-bottom: 1px solid #eef3f8;
  color: #334155;
}

.user-table :deep(.el-tag) {
  border-radius: 6px;
  font-weight: 700;
}

.user-table :deep(.el-button.is-link) {
  height: 28px;
  padding: 0 4px;
  font-weight: 700;
}

.account-cell {
  display: grid;
  gap: 2px;
}

.account-cell strong {
  color: var(--text-primary);
  font-size: 13px;
}

.account-cell span {
  color: var(--text-muted);
  font-size: 12px;
}

.dialog-tip {
  margin: 0 0 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.import-panel {
  display: grid;
  gap: 14px;
}

@media (max-width: 1180px) {
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .page-intro,
  .batch-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-actions,
  .batch-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .filter-form {
    grid-template-columns: 1fr;
  }

  .page-title-group {
    align-items: flex-start;
  }
}
</style>
