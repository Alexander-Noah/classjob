// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { ref, nextTick, effectScope } from 'vue'
import http from '../../src/api/http'
import { fetchBusinessLoopOverview, normalizeBusinessLoopOverview } from '../../src/api/businessLoop'
import { fetchDashboardOverview } from '../../src/api/dashboard'
import { getLabDevices, listLabs } from '../../src/api/services/lab'
import * as validation from '../../src/utils/reservationValidation'
import { useTablePagination } from '../../src/composables/useTablePagination'
const mode = vi.hoisted(() => ({ mock: false }))
vi.mock('../../src/api/config', () => ({ get API_USE_MOCK() { return mode.mock } }))
vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
beforeEach(() => { mode.mock = false; vi.clearAllMocks() })
afterEach(() => vi.restoreAllMocks())

it('business overview rejects malformed collections without dropping valid metadata', async () => {
  for (const raw of [undefined, null, 3, 'invalid', { cards: null, chains: {}, actions: 1, latestEvents: '', eventCategories: 0, totals: false }]) {
    const result = normalizeBusinessLoopOverview(raw)
    expect(result.cards).toEqual([]); expect(result.chains).toEqual([])
    expect(result.actions).toEqual([]); expect(result.latestEvents).toEqual([])
    expect(result.eventCategories).toEqual({}); expect(result.totals).toEqual({})
  }
  const input = Object.fromEntries(['cards','chains','actions','latestEvents','usageTrend','labUsageRanking','repairFaultRanking'].map(key => [key, [{ id: key }]]))
  Object.assign(input, { eventCategories: { reservation: 2 }, totals: { count: 3 } })
  http.get.mockResolvedValueOnce({ data: { data: input } }).mockResolvedValueOnce({ data: input })
  expect(await fetchBusinessLoopOverview()).toEqual(input)
  expect(await fetchBusinessLoopOverview()).toEqual(input)
  mode.mock = true
  expect((await fetchBusinessLoopOverview()).cards).toEqual([])
  expect(http.get).toHaveBeenCalledTimes(2)
})

it('dashboard accepts raw and nested data and removes laboratory management shortcuts', async () => {
  const input = { shortcuts: [{ title: '实验室资源' }, { title: '实验室管理' }, { title: '预约' }, null], stats: null }
  http.get.mockResolvedValueOnce({ data: input }).mockResolvedValueOnce({ data: { data: input } }).mockResolvedValueOnce({ data: null })
  for (let index = 0; index < 2; index++) expect((await fetchDashboardOverview()).shortcuts).toEqual([{ title: '预约' }, null])
  expect((await fetchDashboardOverview()).stats).toEqual([])
  mode.mock = true
  expect(Array.isArray((await fetchDashboardOverview()).stats)).toBe(true)
})

it('laboratory adapters support bare data, empty envelopes and explicit business failures', async () => {
  for (const data of [{ data: { code: 200, data: [{ id: 4 }] } }, { data: [{ id: 4 }] }, [{ id: 4 }]]) {
    http.get.mockResolvedValueOnce(data)
    expect(await listLabs()).toEqual([{ id: 4 }])
  }
  http.get.mockResolvedValueOnce({ data: {} }).mockResolvedValueOnce({ data: { code: 200, data: { devices: [] } } })
  expect(await listLabs()).toEqual([])
  expect(await getLabDevices(4)).toEqual({ devices: [] })
  expect(http.get).toHaveBeenLastCalledWith('/labs/4/devices')
  for (const [payload, message] of [[{ code: 400, message: '禁用' }, '禁用'], [{ code: 500, msg: '异常' }, '异常'], [{ code: 500 }, '实验室接口调用失败']]) {
    http.get.mockResolvedValueOnce({ data: payload })
    await expect(listLabs()).rejects.toThrow(message)
  }
})

it('reservation validators distinguish required, unsafe, short, long and meaningless input', () => {
  expect(validation.normalizeInputText(null)).toBe('')
  expect(validation.normalizeInputText(' a\n b ')).toBe('a b')
  for (const [input, options, error] of [[null, {}, '请填写内容'], ['', { required: false }, ''], ['<img>', {}, '内容不能包含脚本或 HTML 标签'], ['a', {}, '内容至少填写 2 个字符'], ['abc', { max: 2 }, '内容不能超过 2 个字符'], ['123', {}, '内容不能只填写数字或符号'], ['课程', {}, '']]) {
    expect(validation.getMeaningfulTextError(input, options)).toBe(error)
  }
  for (const value of ['13800000001', '010-12345678', 'person@example.test']) expect(validation.isValidContactText(value)).toBe(true)
  for (const value of ['', '张', 'x'.repeat(81), '123']) expect(validation.isValidContactText(value, { strict: false })).toBe(false)
  expect(validation.isValidContactText('张老师', { strict: false })).toBe(true)
  expect(validation.isValidContactText('张老师')).toBe(false)
  for (const [value, options, error] of [[null, {}, '请填写联系方式'], ['', { required: false }, ''], ['javascript:foo', {}, '联系方式不能包含脚本或 HTML 标签'], ['a'.repeat(81), {}, '联系方式不能超过 80 个字符'], ['bad', {}, '联系方式请填写正确的手机号、邮箱或座机'], ['1', { strict: false }, '联系方式请填写姓名、手机号、邮箱或座机'], ['13800000001', {}, '']]) expect(validation.getContactError(value, options)).toBe(error)
  for (const factory of [validation.createMeaningfulTextValidator, validation.createContactValidator]) {
    const callback = vi.fn()
    factory()(null, '', callback); expect(callback).toHaveBeenLastCalledWith(expect.any(Error))
    factory({ required: false })(null, '', callback); expect(callback).toHaveBeenLastCalledWith(undefined)
  }
})

it('local pagination clamps shrinking collections and resets after page size changes', async () => {
  const scope = effectScope()
  const records = ref(Array.from({ length: 25 }, (_, i) => i))
  const pager = scope.run(() => useTablePagination(records, { initialPage: 3, initialPageSize: 10 }))
  expect(pager.pagedRecords.value).toEqual([20,21,22,23,24])
  records.value = [1,2]; await nextTick()
  expect(pager.currentPage.value).toBe(1)
  pager.handleCurrentPageChange(2); expect(pager.currentPage.value).toBe(2)
  pager.handlePageSizeChange(1); expect(pager.currentPage.value).toBe(1)
  expect(pager.pagedRecords.value).toEqual([1])
  records.value = null; await nextTick()
  expect(pager.total.value).toBe(0); expect(pager.pagedRecords.value).toEqual([])
  const empty = scope.run(() => useTablePagination(undefined))
  expect(empty.pageSize.value).toBe(10); expect(empty.total.value).toBe(0)
  scope.stop()
})
import { getAcademicCredential } from '../../src/api/services/academicSchedule'
import { listClassTimetables } from '../../src/api/services/classTimetable'
import { getScanReservationProfile } from '../../src/api/services/scanReservation'
import { listMaintenanceRecords } from '../../src/api/services/maintenanceRecords'
import { fetchPlatformSnapshot } from '../../src/api/services/platformOverview'
import { noticeService } from '../../src/api/services/notices'
import { calculateDeviceAssetStats } from '../../src/api/services/deviceAssetStats'
it('credential readers accept raw configuration and use empty objects for absent payloads',async()=>{
 for(const value of [null,{}, {username:'masked'}]){http.get.mockResolvedValueOnce({data:value});expect(await getAcademicCredential('a/b')).toEqual(value||{})}
})
it('list adapters accept bare records and reject business errors with fallback messages',async()=>{
 for(const invoke of [()=>listClassTimetables(),()=>getScanReservationProfile({labId:1,code:'x'}),()=>listMaintenanceRecords()]){
  http.get.mockResolvedValueOnce([{id:1}]);const result=await invoke();expect(result[0].id).toBe(1)
  http.get.mockResolvedValueOnce({data:{code:500}});await expect(invoke()).rejects.toThrow(/接口调用失败/)
 }
 http.get.mockResolvedValueOnce({data:{code:500,msg:'服务拒绝'}});await expect(listClassTimetables()).rejects.toThrow('服务拒绝')
})
it('notice and platform list adapters accept direct arrays without assuming pagination',async()=>{
 http.get.mockResolvedValueOnce({data:[{id:1}]});expect(await noticeService.list()).toEqual([{id:1}])
 http.get.mockResolvedValue({data:[{id:2}]});expect(await fetchPlatformSnapshot()).toEqual({users:[{id:2}],notices:[{id:2}],errors:[]})
 expect(calculateDeviceAssetStats([{status:'   '}])).toMatchObject({total:1,normal:1})
})

import { saveAcademicCredential } from '../../src/api/services/academicSchedule'
import { createActivationCodes } from '../../src/api/services/activationCodes'
import { applyAuthoritativeReservationRecord } from '../../src/api/services/authoritativeRecord'
import { API_ENDPOINTS, getBusinessModuleEndpoints } from '../../src/api/endpoints'
import { calculateAcademicWeek } from '../../src/utils/academicWeek'
it('credential envelopes preserve nested values and activation records distinguish codes from status envelopes', async () => {
  for (const value of [null, {}, { data: { configured: true } }]) {
    http.post.mockResolvedValueOnce({ data: value })
    expect(await saveAcademicCredential({ username: 'a' })).toEqual(value?.data || value || {})
  }
  http.get.mockResolvedValueOnce({ data: { data: { configured: true } } })
  expect(await getAcademicCredential('a')).toEqual({ configured: true })
  for (const value of [null, [], { code: 'ABC' }, { id: 2, code: 'ABC' }, { name: 'bare' }]) {
    const payload = { quantity: 1 }
    http.post.mockResolvedValueOnce({ data: value })
    expect(await createActivationCodes(payload)).toEqual(value && !Array.isArray(value) && (!('code' in value) || 'id' in value) ? value : payload)
  }
})
it('authoritative updates preserve unrelated rows and reject absent response identities', () => {
  const untouched = { id: 2, name: 'unchanged' }
  const updated = { id: 1, name: 'new' }
  expect(applyAuthoritativeReservationRecord([{ id: 1 }, untouched], updated)).toEqual([updated, untouched])
  expect(applyAuthoritativeReservationRecord([{ id: 1 }, untouched], updated)[1]).toBe(untouched)
  for (const record of [null, {}, { id: '' }, { id: Infinity }]) expect(() => applyAuthoritativeReservationRecord([], record)).toThrow('预约服务未返回有效记录')
})
it('device history endpoints preserve identity and module names have a safe default', () => {
  expect(API_ENDPOINTS.devices.inventoryRecords(12)).toBe('/devices/12/inventory-records')
  expect(API_ENDPOINTS.devices.transferRecords(12)).toBe('/devices/12/transfer-records')
  expect(getBusinessModuleEndpoints(null).list).toBe('/modules/module-records')
  expect(getBusinessModuleEndpoints('A/B').list).toBe('/modules/a-b')
})
it('academic week calculations reject impossible local dates and invalid current dates', () => {
  for (const start of ['0001-01-01', '2026-13-01', '2026-02-30']) expect(calculateAcademicWeek(new Date('2026-10-18'), start)).toBe(1)
  expect(calculateAcademicWeek(new Date(NaN), '2026-09-01')).toBe(1)
  expect(calculateAcademicWeek('2026-10-18', '2026-09-01')).toBe(1)
})

