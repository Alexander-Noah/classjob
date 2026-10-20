// @vitest-environment happy-dom
import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import NoticeCenter from '../../src/views/notices/NoticeCenter.vue'
import { noticeService as api } from '../../src/api/services/notices'
import { ElMessage, ElMessageBox } from 'element-plus'
import { panelStubs as tableStubs } from '../support/formStubs'
const route = vi.hoisted(() => ({ meta: { roleScope: 'systemAdmin' } }))
const router = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => router }))
vi.mock('../../src/api/services/auth', () => ({ getStoredRole: () => 'labAdmin',getStoredUser:()=>({id:7,realName:'管理员甲'}) }))
vi.mock('../../src/api/services/notices', () => ({ noticeService: Object.fromEntries(['list','userNotices','stats','recipients','create','update','publish','withdraw','archive','remove','markRead','syncBusinessReminders'].map(key => [key, vi.fn()])) }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const wrappers = []
const notice = () => ({ id: 1, title: '开放安排', noticeType: '开放安排', content: '正常开放', status: '草稿' })
beforeEach(() => {
  vi.resetAllMocks()
  api.list.mockResolvedValue([notice()]); api.userNotices.mockResolvedValue([{ ...notice(), noticeId: 1 }]); api.stats.mockResolvedValue({})
  api.recipients.mockResolvedValue([{ name: '教师甲' }]); api.create.mockResolvedValue(notice()); api.update.mockResolvedValue(notice())
  api.syncBusinessReminders.mockResolvedValue({ created: 2 }); ElMessageBox.confirm.mockResolvedValue(true)
})
afterEach(() => { wrappers.splice(0).forEach(wrapper => wrapper.unmount()) })
async function setup(render = false) {
  const names = ['button','card','tabs','tab-pane','input','select','option','table','table-column','tag','dialog','drawer','form','form-item','radio-group','radio-button','empty','icon','alert']
  const wrapper = shallowMount(NoticeCenter, { global: { directives: { loading: {} }, stubs: render ? tableStubs() : Object.fromEntries(names.map(name => [`el-${name}`, true])) } }); wrappers.push(wrapper); await flushPromises(); return wrapper.vm
}
it('rendered notice editor binds title and content and saves a draft through its footer',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1)
  expect(wrapper.text()).toContain('开放安排')
  vm.openCreateDialog();await flushPromises()
  await wrapper.find('input[placeholder="请输入通知标题"]').setValue('课表同步通知')
  await wrapper.find('textarea').setValue('请查看最新课表')
  await wrapper.findAll('button').find(button=>button.text()==='保存草稿').trigger('click');await flushPromises()
  expect(api.create).toHaveBeenCalledWith(expect.objectContaining({title:'课表同步通知',content:'请查看最新课表',publishStatus:'草稿'}))
})
it('rendered notice actions support publish, archived rows, explicit read and all editor fields',async()=>{const vm=await setup(true),w=wrappers.at(-1);const click=async label=>{await w.findAll('button').find(b=>b.text()===label).trigger('click');await flushPromises()};await click('编辑');for(const select of w.findAll('select')){const option=select.findAll('option')[0];if(option)await select.setValue(option.element.value)}await w.get('input[placeholder="如 consumable / reservation / repair"]').setValue('reservation');await w.get('input[placeholder="业务记录 ID"]').setValue('7');expect(vm.formModel).toMatchObject({sourceModule:'reservation',sourceId:'7'});await click('发布通知');expect(api.publish).toHaveBeenCalled();vm.notices[0].status='已发布';await flushPromises();await click('撤回');expect(api.withdraw).toHaveBeenCalled();await click('归档');expect(api.archive).toHaveBeenCalled();await click('删除');expect(api.remove).toHaveBeenCalled();vm.activeTab='my';await flushPromises();await click('查看');expect(w.text()).toContain('正常开放');await click('标记为已读');expect(api.markRead).toHaveBeenCalled();await click('关闭');expect(vm.detailVisible).toBe(false);await click('同步业务提醒');expect(api.syncBusinessReminders).toHaveBeenCalled()})
it('loads notices with safe defaults, unread metrics and combined filters', async () => {
  const vm = await setup()
  expect(vm.pageTitle).toBe('平台通知公告'); expect(vm.pageDescription).toContain('全平台')
  expect(vm.notices[0]).toMatchObject({ targetRole: 'labAdmin', publishStatus: '草稿', priority: '普通' })
  expect(vm.unreadNotices).toHaveLength(1); expect(vm.metrics[2].value).toBe('1')
  vm.keyword = '开放'; vm.statusFilter = '草稿'; vm.typeFilter = '开放安排'; expect(vm.filteredNotices).toHaveLength(1)
  vm.typeFilter = '其他'; expect(vm.filteredNotices).toEqual([])
  expect(vm.getTargetLabel('all')).toBe('全部用户'); expect(vm.getTargetLabel('')).toBe('-')
  expect(vm.getStatusType('已发布')).toBe('success'); expect(vm.getReadType('未读')).toBe('warning')
})
it('draft create and edit trim payloads and publishing distributes the saved record', async () => {
  const vm = await setup(); vm.openCreateDialog(); Object.assign(vm.formModel, { title: ' 新通知 ', content: ' 正文 ' })
  await vm.saveDraft(); expect(api.create).toHaveBeenCalledWith(expect.objectContaining({ title: '新通知', content: '正文', publishStatus: '草稿' })); expect(vm.dialogVisible).toBe(false)
  vm.openEditDialog(vm.notices[0]); await vm.publishFromDialog()
  expect(api.update).toHaveBeenCalledWith(1, expect.objectContaining({ status: '已发布' })); expect(api.publish).toHaveBeenCalledWith(1); expect(vm.submitting).toBe(false)
})
it('form validation blocks save and backend failure clears submitting state', async () => {
  const vm = await setup(); vm.formRef = { validate: vi.fn().mockRejectedValue('invalid') }
  expect(await vm.saveNotice()).toBeNull(); expect(api.create).not.toHaveBeenCalled()
  vm.formRef = null; api.create.mockRejectedValueOnce(new Error('保存失败'))
  await expect(vm.saveNotice()).rejects.toThrow('保存失败'); expect(vm.submitting).toBe(false)
})
it('detail, publish, withdraw, archive and delete refresh data and close deleted detail', async () => {
  const vm = await setup(); await vm.viewNotice(vm.notices[0]); expect(vm.recipients).toHaveLength(1)
  await vm.publishNotice(vm.selectedNotice); expect(api.recipients).toHaveBeenCalledTimes(2)
  await vm.withdrawNotice(vm.notices[0]); expect(api.withdraw).toHaveBeenCalledWith(1)
  await vm.archiveNotice(vm.notices[0]); expect(api.archive).toHaveBeenCalledWith(1)
  ElMessageBox.confirm.mockRejectedValueOnce('cancel'); await vm.deleteNotice(vm.notices[0]); expect(api.remove).not.toHaveBeenCalled()
  await vm.deleteNotice(vm.notices[0]); expect(api.remove).toHaveBeenCalledWith(1); expect(vm.detailVisible).toBe(false); expect(vm.selectedNotice).toBeNull()
})
it('read receipts are sent once for unread notifications and reminder sync clears busy state', async () => {
  const vm = await setup(); await vm.readMyNotice(vm.myNotices[0]); expect(api.markRead).not.toHaveBeenCalled()
  await vm.markSelectedRead(); expect(api.markRead).toHaveBeenCalledWith(1)
  await vm.readMyNotice({ ...vm.myNotices[0], readStatus: '已读' }); await vm.markSelectedRead(); expect(api.markRead).toHaveBeenCalledTimes(1)
  await vm.syncBusinessReminders(); expect(ElMessage.success).toHaveBeenCalledWith('已同步 2 条业务提醒'); expect(vm.syncing).toBe(false)
  api.syncBusinessReminders.mockRejectedValueOnce(new Error('同步失败')); await expect(vm.syncBusinessReminders()).rejects.toThrow('同步失败'); expect(vm.syncing).toBe(false)
  vm.handleHeroAction({ command: 'create' }); expect(vm.dialogVisible).toBe(true)
  vm.handleHeroAction({ command: 'sync' }); await flushPromises(); vm.handleBack(); expect(router.push).toHaveBeenCalledWith('/system-admin')
})
it('optional source failures return empty data without leaving loading state', async () => {
  const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
  api.list.mockRejectedValueOnce(new Error('offline')); api.stats.mockRejectedValueOnce(new Error('offline')); api.userNotices.mockRejectedValueOnce(new Error('offline'))
  const vm = await setup(); expect(vm.notices).toEqual([]); expect(vm.myNotices).toEqual([]); expect(vm.loading).toBe(false)
  await vm.loadRecipients(null); expect(vm.recipients).toEqual([]); warn.mockRestore()
})


it('normalizes legacy notices, authoritative metrics and publisher identities',async()=>{
 route.meta={};api.list.mockResolvedValue([{id:2,type:'系统公告',target:'teacher',publishStatus:'已发布',publishTime:'2026-10-19T09:00:00',recipientCount:3,readCount:2,unreadCount:1},{}]);api.userNotices.mockResolvedValue([{id:2,readStatus:'已读',readTime:'2026-10-19T10:00:00'}]);api.stats.mockResolvedValue({total:9,publishedCount:8,draftCount:1,myUnread:0,myRead:2,deliveryCount:3,readCount:2})
 const vm=await setup();expect(vm.pageTitle).toBe('通知公告');expect(vm.pageDescription).toContain('实验室');expect(vm.roleScope).toBe('labAdmin');expect(vm.notices[0]).toMatchObject({title:'系统提醒',noticeType:'系统公告',targetRole:'teacher',status:'已发布',publishTimeText:'2026-10-19 09:00'});expect(vm.myNotices[0]).toMatchObject({noticeId:2,readTimeText:'2026-10-19 10:00'});expect(vm.metrics.map(m=>m.value)).toEqual(['9','1','0','3'])
 for(const [notice,label] of [[{publisherName:'发布人'},'发布人'],[{createdByName:'创建人'},'创建人'],[{createdBy:7},'管理员甲'],[{createdBy:8},'用户 8'],[null,'未记录']]){vm.selectedNotice=notice;expect(vm.publisherLabel).toBe(label)}
 vm.statusFilter='草稿';expect(vm.filteredNotices).toHaveLength(1);vm.statusFilter='all';vm.keyword='notfound';expect(vm.filteredNotices).toEqual([]);expect(vm.getTargetLabel('custom')).toBe('custom');expect(vm.getStatusType('unknown')).toBe('info');expect(vm.getReadType('unknown')).toBe('info');vm.openCreateDialog();expect(vm.formModel.targetRole).toBe('labAdmin');vm.handleBack();expect(router.push).toHaveBeenCalledWith('/lab-admin');route.meta={roleScope:'systemAdmin'}
})
it('publication handles validation rejection and stale selected detail without duplicate reads',async()=>{
 const vm=await setup();vm.formRef={validate:vi.fn().mockResolvedValue(false)};await vm.publishFromDialog();expect(api.publish).not.toHaveBeenCalled();vm.formRef=null;vm.selectedNotice={id:99};await vm.publishNotice({id:1});expect(api.recipients).not.toHaveBeenCalled();vm.selectedNotice={id:1};api.list.mockResolvedValueOnce([]);await vm.publishNotice({id:1});expect(vm.selectedNotice).toBeNull()
 await vm.markSelectedRead();vm.selectedNotice={noticeId:1,readStatus:'未读'};vm.submitting=true;await vm.markSelectedRead();expect(api.markRead).not.toHaveBeenCalled();vm.submitting=false;api.markRead.mockRejectedValueOnce(new Error('receipt failed'));await expect(vm.markSelectedRead()).rejects.toThrow('receipt failed');expect(vm.submitting).toBe(false)
 api.syncBusinessReminders.mockResolvedValueOnce({});await vm.syncBusinessReminders();expect(ElMessage.success).toHaveBeenCalledWith('已同步 0 条业务提醒');api.stats.mockResolvedValueOnce(null);await vm.loadData();expect(vm.stats).toEqual({});await vm.deleteNotice({id:8,title:'其他'});expect(api.remove).toHaveBeenCalledWith(8)
})
