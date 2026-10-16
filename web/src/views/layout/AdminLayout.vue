<script setup>
import { computed, onBeforeUnmount, provide, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, Iphone, Message, OfficeBuilding, UploadFilled, UserFilled } from '@element-plus/icons-vue'
import { getStoredRole, getStoredUser, logout, updateCurrentUserProfile, updateStoredUser } from '../../api/services/auth'
import { uploadUserAvatar } from '../../api/services/users'
import { API_BASE_URL } from '../../api/config'
import { DEFAULT_AUTH_ROLE } from '../../constants/auth'
import { getRoleLayout, getRoleMenus } from '../../constants/navigation'
import { useCurrentTime } from '../../composables/useCurrentTime'
import { normalizeAvatarUrl } from '../../utils/avatarUrl'

defineOptions({
  name: 'AdminLayout',
})

const isSidebarCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const profileDialogVisible = ref(false)
const profileSubmitting = ref(false)
const sessionUser = ref(getStoredUser() || {})
const avatarPreviewUrl = ref('')
const pendingAvatarFile = ref(null)
const profileForm = reactive({
  username: '',
  realName: '',
  roleName: '',
  department: '',
  phone: '',
  email: '',
  avatarUrl: '',
})
const route = useRoute()
const router = useRouter()
const { currentTime } = useCurrentTime()

const currentRole = computed(() => route.meta?.roleScope || getStoredRole() || DEFAULT_AUTH_ROLE)
const roleLayout = computed(() => getRoleLayout(currentRole.value))
const currentUser = computed(() => sessionUser.value || {})
provide('sessionUserProfile', currentUser)
const currentUserName = computed(() => currentUser.value.realName || currentUser.value.name || currentUser.value.username || roleLayout.value.user.name)
const currentUserRole = computed(() => currentUser.value.roleName || currentUser.value.role || roleLayout.value.user.role)
const API_ROOT = String(API_BASE_URL || '').replace(/\/$/, '')
const buildAvatarProxyUrl = (objectKey = '') => {
  const normalizedObjectKey = String(objectKey || '').trim().replace(/^\/+/, '')
  if (!normalizedObjectKey) return ''
  return `${API_ROOT}/files/avatar-proxy?objectKey=${encodeURIComponent(normalizedObjectKey)}`
}
const resolveUploadedAvatarUrl = (uploaded = {}) => {
  const directUrl = normalizeAvatarUrl(uploaded.avatarUrl || uploaded.url || '')
  if (/^(data|blob):/i.test(directUrl)) return directUrl
  return directUrl || buildAvatarProxyUrl(uploaded.objectKey) || normalizeAvatarUrl(uploaded.proxyUrl)
}
const currentAvatarUrl = computed(() => avatarPreviewUrl.value || normalizeAvatarUrl(currentUser.value.avatarUrl || currentUser.value.avatar || ''))
const displayedProfileAvatarUrl = computed(() => avatarPreviewUrl.value || normalizeAvatarUrl(profileForm.avatarUrl))
const navMenus = computed(() => getRoleMenus(currentRole.value, currentUser.value))
const defaultOpenMenus = computed(() => navMenus.value.filter((menu) => menu.children?.length).map((menu) => menu.index))
const sidebarWidth = computed(() => (isSidebarCollapsed.value ? '72px' : '236px'))
const activeMenu = computed(() => route.meta?.activeMenu || route.path)
const breadcrumbItems = computed(() => {
  if (Array.isArray(route.meta?.breadcrumb) && route.meta.breadcrumb.length) {
    return route.meta.breadcrumb.map((breadcrumb, index) => ({
      title: breadcrumb.title,
      to: index === route.meta.breadcrumb.length - 1 ? undefined : { path: breadcrumb.path },
    }))
  }

  return route.matched
    .filter((matchedRoute) => matchedRoute.meta?.title)
    .map((matchedRoute) => ({
      title: matchedRoute.meta.title,
      to: undefined,
    }))
})

const handleToggleSidebar = () => {
  if (window.innerWidth <= 900) {
    isMobileMenuOpen.value = !isMobileMenuOpen.value
    return
  }

  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const handleMenuSelect = (index) => {
  isMobileMenuOpen.value = false

  if (index !== route.path) {
    router.push(index)
  }
}

const clearAvatarPreview = () => {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
    avatarPreviewUrl.value = ''
  }
  pendingAvatarFile.value = null
}

const openProfileDialog = () => {
  clearAvatarPreview()
  const user = currentUser.value
  Object.assign(profileForm, {
    username: user.username || '',
    realName: user.realName || user.name || '',
    roleName: user.roleName || user.role || currentUserRole.value,
    department: user.department || '',
    phone: user.phone || '',
    email: user.email || '',
    avatarUrl: normalizeAvatarUrl(user.avatarUrl || user.avatar || ''),
  })
  profileDialogVisible.value = true
}

const saveProfile = async ({ closeDialog = true, successMessage = '个人信息已保存' } = {}) => {
  const realName = profileForm.realName.trim()
  if (!realName) {
    ElMessage.warning('请填写姓名')
    return
  }

  profileSubmitting.value = true
  try {
    if (pendingAvatarFile.value) {
      const uploaded = await uploadUserAvatar(pendingAvatarFile.value)
      profileForm.avatarUrl = resolveUploadedAvatarUrl(uploaded)
      pendingAvatarFile.value = null
    }

    const payload = {
      ...currentUser.value,
      username: profileForm.username,
      name: realName,
      realName,
      role: profileForm.roleName,
      roleName: profileForm.roleName,
      department: profileForm.department.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      avatarUrl: normalizeAvatarUrl(profileForm.avatarUrl),
    }

    const savedUser = await updateCurrentUserProfile(payload)

    sessionUser.value = updateStoredUser(savedUser, currentRole.value)
    if (closeDialog) {
      profileDialogVisible.value = false
      clearAvatarPreview()
    }
    ElMessage.success(successMessage)
  } catch (error) {
    if (!error?.__handled) {
      ElMessage.error(error?.userMessage || error?.message || '保存个人信息失败')
    }
  } finally {
    profileSubmitting.value = false
  }
}

const validateAvatarFile = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('头像仅支持 JPG、PNG、WEBP、GIF 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像文件不能超过2MB')
    return false
  }
  return true
}

const handleAvatarSelection = (uploadFile) => {
  const file = uploadFile?.raw || uploadFile
  if (!validateAvatarFile(file)) return

  clearAvatarPreview()
  pendingAvatarFile.value = file
  avatarPreviewUrl.value = URL.createObjectURL(file)
}

const handleUserCommand = async (command) => {
  if (command === 'profile') {
    openProfileDialog()
    return
  }

  if (command !== 'logout') return

  await logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onBeforeUnmount(clearAvatarPreview)
</script>

<template>
  <el-container class="admin-layout" :style="{ '--admin-sidebar-width': sidebarWidth }">
    <el-aside
      :width="sidebarWidth"
      class="admin-sidebar"
      :class="{
        'is-collapsed': isSidebarCollapsed,
        'is-mobile-open': isMobileMenuOpen,
      }"
    >
      <div class="sidebar-brand">
        <span class="brand-icon" aria-hidden="true">
          <el-icon>
            <OfficeBuilding />
          </el-icon>
        </span>
        <div class="brand-copy">
          <strong>智云实验室</strong>
          <span>{{ roleLayout.subtitle }}</span>
        </div>
      </div>

      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :collapse="isSidebarCollapsed"
          :default-active="activeMenu"
          :default-openeds="defaultOpenMenus"
          class="sidebar-menu"
          background-color="#0f172a"
          text-color="#cbd5e1"
          active-text-color="#ffffff"
          @select="handleMenuSelect"
        >
          <template v-for="menu in navMenus" :key="menu.index">
            <el-sub-menu v-if="menu.children?.length" :index="menu.index">
              <template #title>
                <el-icon>
                  <component :is="menu.icon" />
                </el-icon>
                <span>{{ menu.title }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.index" :index="child.index">
                <el-icon>
                  <component :is="child.icon" />
                </el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menu.index">
              <el-icon>
                <component :is="menu.icon" />
              </el-icon>
              <template #title>{{ menu.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <button
      v-if="isMobileMenuOpen"
      class="sidebar-mask"
      type="button"
      aria-label="关闭菜单"
      @click="isMobileMenuOpen = false"
    ></button>

    <el-container class="admin-main">
      <el-header class="admin-header">
        <div class="header-left">
          <button class="collapse-button" type="button" aria-label="切换菜单" @click="handleToggleSidebar">
            <span></span>
            <span></span>
            <span></span>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="breadcrumb in breadcrumbItems" :key="breadcrumb.title" :to="breadcrumb.to">
              {{ breadcrumb.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <div class="header-time">
            <el-icon aria-hidden="true">
              <Clock />
            </el-icon>
            <span>{{ currentTime }}</span>
          </div>
          <el-dropdown @command="handleUserCommand">
            <div class="header-user" tabindex="0">
              <el-avatar :size="34" :src="currentAvatarUrl">
                <el-icon>
                  <UserFilled />
                </el-icon>
              </el-avatar>
              <div class="header-user-copy">
                <strong>{{ currentUserName }}</strong>
                <span>{{ currentUserRole }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-content">
        <RouterView />
      </el-main>
    </el-container>

    <el-dialog
      v-model="profileDialogVisible"
      align-center
      class="profile-dialog"
      title="个人中心"
      width="min(640px, calc(100vw - 32px))"
      destroy-on-close
      @closed="clearAvatarPreview"
    >
      <div class="profile-panel">
        <div class="profile-hero">
          <div class="profile-avatar-wrap">
            <el-avatar :size="72" :src="displayedProfileAvatarUrl" class="profile-avatar">
              <el-icon>
                <UserFilled />
              </el-icon>
            </el-avatar>
            <el-upload
              accept="image/jpeg,image/png,image/webp,image/gif"
              :auto-upload="false"
              :on-change="handleAvatarSelection"
              :show-file-list="false"
            >
              <button class="avatar-upload-button" type="button" title="上传头像">
                <el-icon>
                  <UploadFilled />
                </el-icon>
              </button>
            </el-upload>
          </div>
          <div class="profile-identity">
            <strong>{{ currentUserName }}</strong>
            <span>{{ currentUserRole }}</span>
          </div>
          <div class="profile-badge">账号信息</div>
        </div>

        <div class="profile-summary">
          <div>
            <el-icon><OfficeBuilding /></el-icon>
            <span>{{ profileForm.department || '暂未填写部门' }}</span>
          </div>
          <div>
            <el-icon><Iphone /></el-icon>
            <span>{{ profileForm.phone || '暂未填写手机号' }}</span>
          </div>
          <div>
            <el-icon><Message /></el-icon>
            <span>{{ profileForm.email || '暂未填写邮箱' }}</span>
          </div>
        </div>

        <el-form :model="profileForm" label-position="top" class="profile-form">
          <div class="profile-section">
            <h3>基础信息</h3>
            <div class="profile-grid">
              <el-form-item label="登录账号">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
              <el-form-item label="姓名" required>
                <el-input v-model="profileForm.realName" maxlength="20" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="角色">
                <el-input v-model="profileForm.roleName" disabled />
              </el-form-item>
              <el-form-item label="所属部门">
                <el-input v-model="profileForm.department" maxlength="40" placeholder="请输入所属部门" />
              </el-form-item>
            </div>
          </div>

          <div class="profile-section">
            <h3>联系方式</h3>
            <div class="profile-grid">
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" maxlength="20" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" maxlength="60" placeholder="请输入邮箱" />
              </el-form-item>
            </div>
          </div>
        </el-form>
      </div>

      <template #footer>
        <div class="profile-footer">
          <el-button @click="profileDialogVisible = false">取消</el-button>
          <el-button :loading="profileSubmitting" type="primary" @click="saveProfile">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: var(--app-bg);
  color: var(--text-primary);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 100vh;
  overflow: hidden;
  background: var(--sidebar-bg);
  box-shadow: var(--shadow-sidebar);
  transition: width 0.2s ease;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  color: #ffffff;
  white-space: nowrap;
}

.brand-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--primary-color);
  font-size: 21px;
}

.brand-copy {
  overflow: hidden;
}

.brand-copy strong,
.brand-copy span {
  display: block;
  line-height: 1.25;
}

.brand-copy strong {
  font-size: 16px;
}

.brand-copy span {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
}

.is-collapsed .brand-copy {
  display: none;
}

.sidebar-scroll {
  height: calc(100vh - 64px);
}

.sidebar-menu {
  border-right: 0;
  padding: 12px 10px;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 46px;
  margin: 4px 0;
  border-radius: 8px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(148, 163, 184, 0.12);
}

.sidebar-menu :deep(.el-menu--inline) {
  background: transparent;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 40px;
  margin: 2px 0 2px 12px;
  font-size: 13px;
}

.sidebar-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #ffffff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--primary-color);
}

.sidebar-mask {
  display: none;
}

.admin-main {
  min-width: 0;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--surface-color);
}

.header-left,
.header-right,
.header-time,
.header-user {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 16px;
  min-width: 0;
}

.collapse-button {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  cursor: pointer;
}

.collapse-button:hover,
.collapse-button:focus-visible {
  border-color: var(--primary-color);
  outline: none;
}

.collapse-button span {
  display: block;
  width: 16px;
  height: 2px;
  margin: 2px 0;
  border-radius: 999px;
  background: #475569;
}

.header-right {
  gap: 18px;
}

.header-time {
  gap: 7px;
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.header-user {
  gap: 10px;
  cursor: pointer;
}

.header-user:focus-visible {
  border-radius: 8px;
  outline: 2px solid var(--primary-color);
  outline-offset: 3px;
}

.header-user strong,
.header-user span {
  display: block;
  line-height: 1.2;
  white-space: nowrap;
}

.header-user strong {
  color: var(--text-primary);
  font-size: 14px;
}

.header-user span {
  margin-top: 3px;
  color: var(--text-secondary);
  font-size: 12px;
}

.admin-content {
  width: 100%;
  min-width: 0;
  padding: 20px 24px 28px;
  overflow-x: hidden;
}

.admin-content > :deep(*) {
  min-width: 0;
}

.profile-dialog :deep(.el-dialog) {
  border-radius: 10px;
  overflow: hidden;
}

.profile-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border-light);
}

.profile-dialog :deep(.el-dialog__title) {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.profile-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.profile-dialog :deep(.el-dialog__footer) {
  padding: 14px 22px 18px;
  border-top: 1px solid var(--border-light);
  background: #fbfdff;
}

.profile-panel {
  padding: 20px 22px 22px;
}

.profile-hero {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 116px;
  padding: 20px;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.14) 0%, rgba(20, 184, 166, 0.12) 48%, rgba(255, 255, 255, 0.95) 100%),
    #f8fbff;
}

.profile-hero::after {
  position: absolute;
  right: -44px;
  top: -64px;
  width: 168px;
  height: 168px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.1);
  content: '';
}

.profile-avatar-wrap {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
}

.profile-avatar {
  position: relative;
  z-index: 1;
  border: 3px solid rgba(255, 255, 255, 0.82);
  background: #dbeafe;
  color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.16);
  font-size: 30px;
}

.profile-avatar :deep(img),
.header-user :deep(.el-avatar img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-button {
  position: absolute;
  right: -2px;
  bottom: -2px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 2px solid #ffffff;
  border-radius: 50%;
  background: var(--primary-color);
  color: #ffffff;
  cursor: pointer;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.25);
}

.avatar-upload-button:hover,
.avatar-upload-button:focus-visible {
  background: #1d4ed8;
  outline: none;
}

.profile-identity {
  position: relative;
  z-index: 1;
}

.profile-identity strong,
.profile-identity span {
  display: block;
  line-height: 1.35;
}

.profile-identity strong {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 800;
}

.profile-identity span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-badge {
  position: absolute;
  right: 18px;
  top: 18px;
  z-index: 1;
  padding: 5px 10px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--primary-color);
  font-size: 12px;
  font-weight: 600;
}

.profile-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0;
}

.profile-summary div {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  background: #ffffff;
  color: var(--text-secondary);
  font-size: 12px;
}

.profile-summary .el-icon {
  flex: 0 0 auto;
  color: var(--primary-color);
  font-size: 16px;
}

.profile-summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-form {
  display: grid;
  gap: 14px;
}

.profile-section {
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: #ffffff;
}

.profile-section h3 {
  margin: 0 0 12px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 14px;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.profile-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.profile-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.profile-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    transform: translateX(-100%);
    width: 236px !important;
    transition: transform 0.2s ease;
  }

  .admin-sidebar.is-mobile-open {
    transform: translateX(0);
  }

  .admin-sidebar.is-collapsed .brand-copy {
    display: block;
  }

  .sidebar-menu {
    width: 100%;
  }

  .sidebar-mask {
    position: fixed;
    inset: 0;
    z-index: 10;
    display: block;
    border: 0;
    background: rgba(15, 23, 42, 0.36);
  }

  .admin-header {
    padding: 0 16px;
  }

  .header-time,
  .header-user-copy {
    display: none;
  }

  .admin-content {
    padding: 14px;
  }

  .profile-summary,
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
