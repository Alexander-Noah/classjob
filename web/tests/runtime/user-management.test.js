// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import UserManagement from '../../src/views/users/UserPermissions.vue'
import * as api from '../../src/api/services/users'
import { getStoredUser, updateStoredUser } from '../../src/api/services/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { exportTableToExcel } from '../../src/utils/exportExcel'
import { panelStubs as tableStubs } from '../support/formStubs'

vi.mock('../../src/api/services/users', () => Object.fromEntries(['batchDeleteUsers', 'batchDisableUsers', 'batchEnableUsers', 'batchUpdateDepartment', 'batchUpdateRole', 'createUser', 'deleteUser', 'importUsers', 'listUsers', 'resetUserPassword', 'updateUser', 'updateUserStatus'].map(key => [key, vi.fn()])))
vi.mock('../../src/api/services/auth', () => ({ getStoredUser: vi.fn(), updateStoredUser: vi.fn() }))
vi.mock('../../src/utils/exportExcel', () => ({ exportTableToExcel: vi.fn(() => 2) }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const wrappers = []
it.each(['cancel','close'])('batch confirmation %s preserves selections and accounts',async(reason)=>{
  const vm=await setup();const selected=vm.userList[1]
  for(const handler of ['handleBatchDisable','handleBatchDelete','handleBatchResetPassword']){
    vm.handleSelectionChange([selected]);ElMessageBox.confirm.mockRejectedValueOnce(reason)
    await vm[handler]()
    expect(vm.selectedRows).toHaveLength(1)
  }
  expect(api.batchDisableUsers).not.toHaveBeenCalled();expect(api.batchDeleteUsers).not.toHaveBeenCalled();expect(api.resetUserPassword).not.toHaveBeenCalled()
  expect(ElMessage.error).not.toHaveBeenCalled()
})
it('batch failures preserve role, department and reset state with actionable fallbacks',async()=>{
  const vm=await setup();const original=vm.userList[1]
  vm.handleSelectionChange([original]);api.resetUserPassword.mockRejectedValueOnce({})
  await vm.handleBatchResetPassword();expect(ElMessage.error).toHaveBeenCalledWith('批量重置密码失败')
  vm.openBatchRoleDialog();vm.batchRoleForm.role='系统管理员';api.batchUpdateRole.mockRejectedValueOnce({response:{data:{message:'角色冲突'}}})
  await vm.submitBatchRole();expect(ElMessage.error).toHaveBeenCalledWith('角色冲突');expect(vm.roleDialogVisible).toBe(true)
  vm.openBatchDepartmentDialog();vm.batchDepartmentForm.department='信息中心';api.batchUpdateDepartment.mockRejectedValueOnce(new Error('部门不存在'))
  await vm.submitBatchDepartment();expect(ElMessage.error).toHaveBeenCalledWith('部门不存在');expect(vm.departmentDialogVisible).toBe(true)
  expect(vm.userList[1]).toEqual(original);expect(vm.selectedRows).toHaveLength(1)
  vm.openImportDialog();vm.handleImportFileChange(new File(['bad'],'users.xlsx'));api.importUsers.mockRejectedValueOnce({})
  await vm.submitImportUsers();expect(ElMessage.error).toHaveBeenCalledWith('导入失败');expect(vm.importDialogVisible).toBe(true)
})
it('protected accounts are skipped when batch operations include the current administrator',async()=>{
  const vm=await setup();vm.handleSelectionChange(vm.userList.slice(0,2));await vm.handleBatchDisable()
  expect(api.batchDisableUsers).toHaveBeenCalledWith([2]);expect(ElMessage.warning).toHaveBeenCalledWith('已跳过 1 个受保护账号')
  expect(vm.userList[0].enabled).toBe(true)
  vm.handleSelectionChange(vm.userList.slice(0,2));await vm.handleBatchDelete()
  expect(api.batchDeleteUsers).toHaveBeenCalledWith([2]);expect(vm.userList.map(user=>user.id)).toContain(1)
})
it('rendered batch dialogs bind role and department and submit only selected users',async()=>{const vm=await setup(true),w=wrappers.at(-1);vm.selectedRows=[vm.userList[1]];await flushPromises();const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()};await click('批量分配角色');await w.get('select[aria-label="请选择角色"]').setValue('实验室管理员');await click('确认分配');expect(api.batchUpdateRole).toHaveBeenCalledWith([2],'实验室管理员');vm.selectedRows=[vm.userList[1]];await flushPromises();await click('批量调整部门');await w.get('select[aria-label="请选择部门"]').setValue('实验中心');await click('确认调整');expect(api.batchUpdateDepartment).toHaveBeenCalledWith([2],'实验中心');await click('查看');expect(vm.detailVisible).toBe(true);w.findComponent({name:'PermissionDetailDrawer'}).vm.$emit('update:modelValue',false);expect(vm.detailVisible).toBe(false);await click('编辑');w.findComponent({name:'PermissionUserDialog'}).vm.$emit('update:modelValue',false);expect(vm.userDialogVisible).toBe(false);await click('下载导入模板');expect(exportTableToExcel).toHaveBeenCalled()})
const records = () => [
  { id: 1, username: 'sysadmin', roleCode: 'systemAdmin', realName: '系统管理员', department: '信息中心', status: 1 },
  { id: 2, username: 'labadmin', roleCode: 'labAdmin', realName: '实验室管理员', department: '实验中心', phone: '13800138000', status: 1 },
  { id: 3, username: 'teacher', roleCode: 'teacher', realName: '教师甲', status: 1 },
]
const form = () => ({ account: 'newadmin', name: '管理员乙', role: '实验室管理员', department: '实验中心', enabled: true, initialPassword: 'test-only-password' })
beforeEach(() => {
  vi.resetAllMocks()
  api.listUsers.mockResolvedValue(records())
  getStoredUser.mockReturnValue({ id: 1, username: 'sysadmin' })
  api.createUser.mockImplementation(async data => ({ ...data, id: 4 }))
  api.updateUser.mockImplementation(async (id, data) => ({ ...data, id }))
  api.updateUserStatus.mockResolvedValue({})
  ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { for (const wrapper of wrappers.splice(0)) wrapper.unmount() })
async function setup(render = false) {
  const stubs = Object.fromEntries(['el-button','el-card','el-dialog','el-dropdown','el-dropdown-item','el-dropdown-menu','el-form','el-form-item','el-icon','el-input','el-option','el-select','el-table','el-table-column','el-tag','el-upload'].map(name => [name, true]))
  const wrapper = shallowMount(UserManagement, { global: { directives: { loading: {} }, stubs: render ? tableStubs() : stubs } }); wrappers.push(wrapper)
  await flushPromises()
  return wrapper.vm
}

it('rendered user rows show suspended status and search/edit buttons use their actual row',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1)
  expect(wrapper.text()).toContain('labadmin');expect(wrapper.text()).toContain('角色已停用')
  await wrapper.find('input[placeholder="姓名 / 手机号 / 工号 / 部门"]').setValue('labadmin')
  await wrapper.findAll('button').find(button=>button.text()==='查询').trigger('click');await flushPromises()
  expect(vm.filteredUsers).toHaveLength(1)
  await wrapper.findAll('button').find(button=>button.text()==='编辑').trigger('click');await flushPromises()
  expect(vm.selectedUser.id).toBe(2)
})

it('loads real users, marks suspended roles, derives metrics and searches only on submit', async () => {
  const vm = await setup()
  expect(vm.isLoading).toBe(false)
  expect(vm.userList[0].builtIn).toBe(true)
  expect(vm.userList[2].statusText).toBe('角色已停用')
  expect(vm.ROLE_OPTIONS).not.toContain('教师')
  expect(vm.metrics.find(metric => metric.label === '启用账号').value).toBe('2')
  expect(vm.departmentOptions).toContain('实验中心')
  vm.filterModel.keyword = 'labadmin'
  expect(vm.filteredUsers).toHaveLength(3)
  vm.handleSearch()
  expect(vm.filteredUsers.map(user => user.id)).toEqual([2])
  vm.filterModel.role = '系统管理员'; vm.handleSearch()
  expect(vm.filteredUsers).toEqual([])
  vm.handleReset()
  expect(vm.filteredUsers).toHaveLength(3)
  vm.filterModel.status = '角色已停用'; vm.filterModel.department = '未设置'; vm.handleSearch()
  expect(vm.filteredUsers.map(user => user.id)).toEqual([3])
})

it('permission parser normalizes old labels and safely falls back for malformed data', async () => {
  const vm = await setup()
  expect(vm.parsePermissions('[{"label":"治理","items":["用户权限"]}]', '系统管理员')).toEqual([{ group: '治理', items: ['用户管理'] }])
  expect(vm.parsePermissions([{ group: '空分组' }], '系统管理员')).toEqual([{ group: '空分组', items: [] }])
  expect(vm.parsePermissions('broken', '未知')).toEqual([])
  expect(vm.normalizeUserRecord({ roleName: '实验室管理员', account: 'legacy', enabled: false })).toMatchObject({ account: 'legacy', enabled: false, roleCode: 'labAdmin', statusText: '停用' })
})

it('create and edit persist normalized contracts and update selected and current session users', async () => {
  const vm = await setup()
  vm.handleOpenCreate(); await vm.handleSaveUser(form())
  expect(api.createUser).toHaveBeenCalledWith(expect.objectContaining({ username: 'newadmin', realName: '管理员乙', roleCode: 'labAdmin', status: 1 }))
  expect(vm.userList[0].id).toBe(4)
  vm.handleOpenDetail(vm.userList[0]); expect(vm.detailVisible).toBe(true)
  vm.handleOpenEdit(vm.userList[0]); await vm.handleSaveUser({ ...form(), name: '已更新' })
  expect(vm.selectedUser.name).toBe('已更新')
  expect(vm.userDialogVisible).toBe(false)
  vm.handleOpenEdit(vm.userList.find(user => user.id === 1))
  await vm.handleSaveUser({ ...form(), role: '系统管理员', account: 'sysadmin' })
  expect(updateStoredUser).toHaveBeenCalledWith(expect.objectContaining({ id: 1, roleCode: 'systemAdmin' }), 'systemAdmin')
  await vm.handleSaveUser(form())
  expect(ElMessage.error).toHaveBeenCalledWith('最后一个系统管理员不能降级')
})

it('built-in and last-admin protections block deletion and self-disable', async () => {
  const vm = await setup()
  await vm.handleDeleteUser(vm.userList[0]); await vm.handleToggleStatus(vm.userList[0])
  expect(api.deleteUser).not.toHaveBeenCalled()
  expect(api.updateUserStatus).not.toHaveBeenCalled()
  await vm.handleDeleteUser({ id: 9, role: '系统管理员', builtIn: false })
  expect(ElMessage.warning).toHaveBeenCalledWith('最后一个系统管理员不能删除')
  expect(vm.canDeleteUser(vm.userList[1])).toBe(true)
})

it('status toggles update displayed records and deletion honors cancel then removes the selected user', async () => {
  const vm = await setup()
  vm.handleOpenDetail(vm.userList[1])
  await vm.handleToggleStatus(vm.userList[1])
  expect(api.updateUserStatus).toHaveBeenCalledWith(2, '停用')
  expect(vm.selectedUser.enabled).toBe(false)
  await vm.handleToggleStatus(vm.selectedUser)
  expect(vm.selectedUser.enabled).toBe(true)
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await vm.handleDeleteUser(vm.selectedUser)
  expect(api.deleteUser).not.toHaveBeenCalled()
  await vm.handleDeleteUser(vm.selectedUser)
  expect(vm.userList.some(user => user.id === 2)).toBe(false)
  expect(vm.selectedRows).toEqual([])
})

it('batch status and deletion skip protected users and only mutate selected accounts', async () => {
  const vm = await setup()
  await vm.handleBatchEnable(); await vm.handleBatchResetPassword()
  vm.handleSelectionChange(vm.userList.slice(0, 2))
  await vm.handleBatchDisable()
  expect(api.batchDisableUsers).toHaveBeenCalledWith([2])
  expect(vm.userList[0].enabled).toBe(true)
  expect(vm.userList[1].enabled).toBe(false)
  expect(ElMessage.warning).toHaveBeenCalledWith('已跳过 1 个受保护账号')
  vm.handleSelectionChange([vm.userList[1]])
  await vm.handleBatchEnable()
  expect(vm.userList[1].enabled).toBe(true)
  vm.handleSelectionChange(vm.userList)
  await vm.handleBatchDelete()
  expect(api.batchDeleteUsers).toHaveBeenCalledWith([2, 3])
  expect(vm.userList.map(user => user.id)).toEqual([1])
  vm.handleSelectionChange(vm.userList)
  await vm.handleBatchDelete(); await vm.handleBatchDisable()
  expect(ElMessage.warning).toHaveBeenCalledWith('没有可删除的用户')
  expect(ElMessage.warning).toHaveBeenCalledWith('没有可停用的用户')
})

it('batch role and department changes validate fields and preserve unselected accounts', async () => {
  const vm = await setup()
  vm.openBatchRoleDialog(); await vm.submitBatchRole()
  expect(ElMessage.warning).toHaveBeenCalledWith('请选择目标角色')
  vm.handleSelectionChange([vm.userList[0]]); vm.batchRoleForm.role = '实验室管理员'
  await vm.submitBatchRole()
  expect(api.batchUpdateRole).not.toHaveBeenCalled()
  vm.handleSelectionChange([vm.userList[2]])
  await vm.submitBatchRole()
  expect(api.batchUpdateRole).toHaveBeenCalledWith([3], '实验室管理员')
  expect(vm.userList[2].role).toBe('实验室管理员')
  expect(vm.roleDialogVisible).toBe(false)
  vm.openBatchDepartmentDialog(); await vm.submitBatchDepartment()
  expect(ElMessage.warning).toHaveBeenCalledWith('请选择目标部门')
  vm.handleSelectionChange([vm.userList[2]]); vm.batchDepartmentForm.department = '信息中心'
  await vm.submitBatchDepartment()
  expect(vm.userList[2].department).toBe('信息中心')
  expect(vm.departmentDialogVisible).toBe(false)
})

it('password reset supports individual and selected accounts with server list refresh', async () => {
  const vm = await setup()
  await vm.handleResetPassword(vm.userList[1])
  expect(api.resetUserPassword).toHaveBeenCalledWith(2)
  expect(ElMessage.success).toHaveBeenCalledWith('密码已重置为 123456')
  vm.handleSelectionChange(vm.userList.slice(1)); await vm.handleBatchResetPassword()
  expect(api.resetUserPassword).toHaveBeenCalledWith(3)
  expect(ElMessage.success).toHaveBeenCalledWith('批量重置密码完成，默认密码为 123456')
  expect(vm.selectedRows).toEqual([])
  ElMessageBox.confirm.mockRejectedValueOnce('close')
  const calls = api.resetUserPassword.mock.calls.length
  await vm.handleResetPassword(vm.userList[1])
  expect(api.resetUserPassword).toHaveBeenCalledTimes(calls)
})

it('import validates file presence and export uses filtered data and the import template', async () => {
  const vm = await setup()
  vm.openImportDialog(); await vm.submitImportUsers()
  expect(ElMessage.warning).toHaveBeenCalledWith('请先选择 xlsx 文件')
  const file = new File(['data'], 'users.xlsx')
  vm.handleImportFileChange({ raw: file }); await vm.submitImportUsers()
  expect(api.importUsers).toHaveBeenCalledWith(file)
  expect(vm.importDialogVisible).toBe(false)
  vm.handleImportFileRemove(); expect(vm.importFile).toBeNull()
  vm.handleImportFileChange(file); expect(vm.importFile).toEqual(file)
  vm.downloadImportTemplate()
  expect(exportTableToExcel.mock.calls[0][0]).toMatchObject({ filename: '用户导入模板', rows: [] })
  await vm.handleExportUsers()
  expect(exportTableToExcel.mock.calls[1][0].rows).toHaveLength(3)
})

it.each([
  ['createUser', 'handleSaveUser', () => form()],
  ['updateUserStatus', 'handleToggleStatus', vm => vm.userList[1]],
  ['deleteUser', 'handleDeleteUser', vm => vm.userList[1]],
  ['resetUserPassword', 'handleResetPassword', vm => vm.userList[1]],
  ['batchEnableUsers', 'handleBatchEnable', () => undefined],
  ['batchDisableUsers', 'handleBatchDisable', () => undefined],
  ['batchDeleteUsers', 'handleBatchDelete', () => undefined],
])('%s failures preserve the account list and show a server message', async (apiName, handler, argument) => {
  const vm = await setup()
  vm.handleSelectionChange([vm.userList[1]])
  api[apiName].mockRejectedValueOnce({ userMessage: '服务暂不可用' })
  const previous = [...vm.userList]
  await vm[handler](argument(vm))
  expect(vm.userList).toEqual(previous)
  expect(ElMessage.error).toHaveBeenCalledWith('服务暂不可用')
})

it('load failure clears busy state and handled errors do not show duplicate toasts', async () => {
  api.listUsers.mockRejectedValueOnce({ __handled: true })
  const vm = await setup()
  expect(vm.isLoading).toBe(false)
  expect(vm.userList).toEqual([])
  expect(ElMessage.error).not.toHaveBeenCalled()
  await vm.refreshUserList()
  expect(vm.userList).toHaveLength(3)
  vm.handleRowCommand('status', vm.userList[1]); await flushPromises()
  expect(api.updateUserStatus).toHaveBeenCalledWith(2, '停用')
  vm.handleRowCommand('delete', vm.userList[1]); await flushPromises()
  expect(api.deleteUser).toHaveBeenCalledWith(2)
})

it('legacy sparse user records preserve account identities and permission labels without inventing privileges',async()=>{
 api.listUsers.mockResolvedValue([
  {account:'oldaccount',role:'自定义角色',name:'历史用户',enabled:false,permissions:[{label:'旧组',items:['用户权限']},{}]},
  {employeeNo:'E-7',jobNo:'J-7'},
  {jobNo:'J-8'},
  {},
 ])
 getStoredUser.mockReturnValue(null)
 const vm=await setup();expect(vm.userList[0]).toMatchObject({id:'oldaccount',account:'oldaccount',name:'历史用户',enabled:false,roleCode:'',permissions:[{group:'旧组',items:['用户管理']},{group:'权限范围',items:[]}]})
 expect(vm.userList[1]).toMatchObject({username:'E-7',account:'E-7'})
 expect(vm.userList[2]).toMatchObject({account:'J-8',name:'未命名账号'})
 expect(vm.userList[3]).toMatchObject({name:'未命名账号',role:'未分配角色',permissions:[]})
 expect(vm.isCurrentUser(vm.userList[0])).toBe(false)
})
it('server validation remains authoritative for incomplete legacy user edits',async()=>{
 const vm=await setup();vm.handleOpenCreate()
 api.createUser.mockRejectedValueOnce({userMessage:'账号与姓名不能为空'})
 await vm.handleSaveUser({enabled:false,role:'自定义角色'})
 expect(api.createUser).toHaveBeenLastCalledWith(expect.objectContaining({username:'',employeeNo:'',realName:'',password:undefined,roleCode:'',permissions:'[]',status:0}))
 expect(ElMessage.error).toHaveBeenLastCalledWith('账号与姓名不能为空')
 expect(vm.userDialogVisible).toBe(true)
})
