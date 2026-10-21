<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStoredRole } from '../../api/services/auth'
import { listAccessRoles, getRolePageAccess, saveRolePageAccess, refreshPageAccess } from '../../api/services/rolePageAccess'

const roles = ref([]), role = ref('labAdmin'), pages = ref([])
const access = ref([]), menus = ref([]), baseline = ref('')
const loading = ref(false), saving = ref(false), ready = ref(false)
const currentRole = computed(() => roles.value.find(item => item.code === role.value))
const snapshot = () => JSON.stringify({ accessiblePaths: access.value, visiblePaths: menus.value })
const dirty = computed(() => ready.value && snapshot() !== baseline.value)

async function load() {
  loading.value = true
  ready.value = false
  try {
    const data = await getRolePageAccess(role.value)
    pages.value = data.pages
    access.value = [...data.policy.accessiblePaths]
    menus.value = [...data.policy.visiblePaths]
    baseline.value = snapshot()
    ready.value = true
  } catch { pages.value = [] }
  finally { loading.value = false }
}
async function selectRole(next) {
  if (dirty.value) {
    try { await ElMessageBox.confirm('当前角色的修改尚未保存，是否放弃修改并切换角色？', '切换角色') }
    catch { return }
  }
  role.value = next
  await load()
}
function toggleAccess(page, allowed) {
  access.value = access.value.filter(path => path !== page.path)
  if (allowed) access.value.push(page.path)
  else menus.value = menus.value.filter(path => path !== page.path)
}
function toggleMenu(page, visible) {
  menus.value = menus.value.filter(path => path !== page.path)
  if (visible) menus.value.push(page.path)
}
async function save() {
  if (!ready.value) return
  saving.value = true
  try {
    await saveRolePageAccess(role.value, { accessiblePaths: access.value, visiblePaths: menus.value })
    baseline.value = snapshot()
    if (getStoredRole() === role.value) await refreshPageAccess(role.value)
    ElMessage.success('角色权限已保存，成员下次进入页面时生效')
  } finally { saving.value = false }
}
function selectAll() {
  access.value = pages.value.map(page => page.path)
  menus.value = pages.value.filter(page => page.menu).map(page => page.path)
}
function homeOnly() {
  access.value = pages.value.filter(page => page.locked).map(page => page.path)
  menus.value = pages.value.filter(page => page.locked && page.menu).map(page => page.path)
}
onMounted(async () => {
  try { roles.value = await listAccessRoles(); await load() }
  catch { ElMessage.error('加载角色权限失败，请刷新重试') }
})
</script>

<template>
  <section class="role-access-page">
    <header>
      <div><h1>角色菜单权限</h1><p>统一设置角色成员能访问的页面，以及左侧导航栏显示的菜单。</p></div>
      <el-button type="primary" :loading="saving" :disabled="!ready || loading || currentRole?.retired || !dirty" @click="save">保存角色权限</el-button>
    </header>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select :model-value="role" aria-label="选择角色" :disabled="loading || saving" @update:model-value="selectRole">
          <el-option v-for="item in roles" :key="item.code" :label="item.name + (item.retired ? '（已停用）' : '')" :value="item.code" />
        </el-select>
        <span v-if="dirty" role="status">有未保存的修改</span>
        <el-button :disabled="!ready || saving || loading || currentRole?.retired" @click="selectAll">开放全部页面</el-button>
        <el-button :disabled="!ready || saving || loading || currentRole?.retired" @click="homeOnly">仅保留必要入口</el-button>
      </div>
      <el-alert v-if="currentRole?.retired" title="该角色已停用，仅供查看；本页面不会恢复其登录权限。" type="warning" :closable="false" />
      <el-alert v-else title="隐藏菜单不会自动禁止页面访问。取消“允许访问”会同时隐藏菜单，并由服务端限制对应业务接口。个人账号权限仍作为额外限制。" type="info" :closable="false" />
      <el-table v-loading="loading" :data="pages" row-key="path" empty-text="暂无页面，请重新加载">
        <el-table-column label="页面" min-width="180"><template #default="{ row }">{{ row.title }} <el-tag v-if="row.locked" size="small">必要入口</el-tag></template></el-table-column>
        <el-table-column prop="path" label="页面地址" min-width="250" />
        <el-table-column label="允许访问" width="130"><template #default="{ row }">
          <el-checkbox :aria-label="`${row.title}允许访问`" :model-value="access.includes(row.path)" :disabled="row.locked || saving || currentRole?.retired" @update:model-value="value => toggleAccess(row, value)" />
        </template></el-table-column>
        <el-table-column label="显示菜单" width="130"><template #default="{ row }">
          <el-checkbox v-if="row.menu" :aria-label="`${row.title}显示菜单`" :model-value="menus.includes(row.path)" :disabled="row.locked || !access.includes(row.path) || saving || currentRole?.retired" @update:model-value="value => toggleMenu(row, value)" />
          <span v-else>子页面</span>
        </template></el-table-column>
      </el-table>
      <el-button class="reload" :disabled="saving || loading" @click="selectRole(role)">重新加载</el-button>
    </el-card>
  </section>
</template>

<style scoped>
.role-access-page { display: grid; gap: 20px; }
header, .toolbar { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
header { justify-content: space-between; }
h1 { margin: 0 0 8px; font-size: 26px; }
p { margin: 0; color: #64748b; }
.toolbar { margin-bottom: 18px; }
.toolbar .el-select { width: 250px; }
.toolbar span { color: #b45309; }
.el-alert { margin-bottom: 16px; }
.reload { margin-top: 18px; }
</style>
