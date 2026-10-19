// @vitest-environment happy-dom
import { beforeEach, expect, it, vi } from 'vitest'
import { createCollectionService } from '../../src/api/services/collectionService'
import http from '../../src/api/http'

const mode = vi.hoisted(() => ({ mock: false }))
vi.mock('../../src/api/config', () => ({ get API_USE_MOCK() { return mode.mock } }))
vi.mock('../../src/api/http', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
const options = { endpoints: { list: '/records', batch: '/records/batch', reset: '/records/reset' }, resourceName: '预约', storageKey: 'test-records' }
const service = createCollectionService(options)
const strict = createCollectionService({ ...options, serverAuthoritativeRecords: true })
beforeEach(() => { mode.mock = false; localStorage.clear(); vi.resetAllMocks() })

it.each([
  [{ data: { records: [{ id: 1 }] } }], [{ data: { items: [{ id: 1 }] } }],
  [{ data: [{ id: 1 }] }], [[{ id: 1 }]], [{ records: [{ id: 1 }] }], [{ items: [{ id: 1 }] }],
])('unwraps supported list response %j', async (data) => {
  http.get.mockResolvedValue({ data })
  expect(await service.list()).toEqual([{ id: 1 }])
  expect(http.get).toHaveBeenCalledWith('/records')
})

it('malformed list responses do not invent fallback business records', async () => {
  http.get.mockResolvedValue({ data: { code: 200 } })
  expect(await service.list({ fallbackRecords: [{ id: 'local' }] })).toEqual([])
})

it('batch replacement keeps explicit server empty lists and clones submitted data', async () => {
  const submitted = [{ id: 1, nested: { name: 'original' } }]
  http.put.mockResolvedValueOnce({ data: { data: [] } }).mockResolvedValueOnce({ data: { code: 200 } })
  expect(await service.replace(submitted)).toEqual([])
  expect(http.put).toHaveBeenCalledWith('/records/batch', { resource: '预约', records: submitted })
  const fallback = await service.replace(submitted)
  expect(fallback).toEqual(submitted)
  fallback[0].nested.name = 'changed'
  expect(submitted[0].nested.name).toBe('original')
})

it.each(['create', 'update', 'approve', 'reject', 'action'])('%s requires a saved server record in strict mode', async (operation) => {
  http.post.mockResolvedValue({ data: { code: 200 } })
  http.put.mockResolvedValue({ data: { code: 200 } })
  const invoke = () => operation === 'create' ? strict.create({ name: 'draft' })
    : operation === 'action' ? strict.action(4, 'complete', { name: 'draft' })
      : strict[operation](4, { name: 'draft' })
  await expect(invoke()).rejects.toThrow('requires a record response from the server')
  http.post.mockResolvedValue({ data: { data: { id: 4, name: 'saved' } } })
  http.put.mockResolvedValue({ data: { data: { id: 4, name: 'saved' } } })
  expect(await invoke()).toEqual({ id: 4, name: 'saved' })
})

it('CRUD uses the expected URL and returns server objects', async () => {
  http.post.mockResolvedValue({ data: { data: { id: 3, name: 'server' } } })
  http.put.mockResolvedValue({ data: { id: 3, name: 'updated' } })
  expect(await service.create({ name: 'draft' })).toEqual({ id: 3, name: 'server' })
  expect(http.post).toHaveBeenCalledWith('/records', { name: 'draft' })
  expect(await service.update(3, { name: 'updated' })).toEqual({ id: 3, name: 'updated' })
  expect(http.put).toHaveBeenCalledWith('/records/3', { name: 'updated' })
  http.get.mockResolvedValue({ data: [] })
  expect(await service.remove(3)).toEqual([])
  expect(http.delete).toHaveBeenCalledWith('/records/3')
  expect(await service.reset()).toEqual([])
  expect(http.post).toHaveBeenCalledWith('/records/reset')
})

it('optional reset endpoint still refreshes the list', async () => {
  http.get.mockResolvedValue({ data: [{ id: 8 }] })
  const withoutReset = createCollectionService({ ...options, endpoints: { list: '/records' } })
  expect(await withoutReset.reset()).toEqual([{ id: 8 }])
  expect(http.post).not.toHaveBeenCalled()
})

it.each(['approve', 'reject', 'action'])('%s posts payload to the corresponding action URL', async (operation) => {
  http.post.mockResolvedValue({ data: { data: { id: 4, status: 'server' } } })
  const payload = { reviewer: '管理员' }
  const result = operation === 'action' ? await service.action(4, 'complete', payload) : await service[operation](4, payload)
  expect(result).toEqual({ id: 4, status: 'server' })
  expect(http.post).toHaveBeenCalledWith(`/records/4/${operation === 'action' ? 'complete' : operation}`, payload)
})

it('mock CRUD persists independent copies and compares numeric and string IDs', async () => {
  mode.mock = true
  const fallback = [{ id: 1, name: 'one' }, { id: 2, name: 'two' }]
  expect(await service.list({ fallbackRecords: fallback })).toEqual(fallback)
  await service.replace(fallback)
  fallback[0].name = 'mutated'
  expect((await service.list())[0].name).toBe('one')
  const created = await service.create({ name: 'new' })
  expect(created.id).toMatch(/^预约-/)
  expect((await service.list())[0]).toEqual(created)
  expect(await service.update('1', { name: 'updated' })).toEqual({ id: '1', name: 'updated' })
  expect(await service.remove(2)).toHaveLength(2)
  expect(await service.reset({ fallbackRecords: [{ id: 9 }] })).toEqual([{ id: 9 }])
  expect(localStorage.getItem(options.storageKey)).toBeNull()
  expect(http.get).not.toHaveBeenCalled()
})

it('mock approvals and rejection update review fields while preserving other records', async () => {
  mode.mock = true
  await service.replace([{ id: 1, reviewer: 'old', note: 'old' }, { id: 2, name: 'other' }])
  expect(await service.approve('1', { reviewerName: '审核员', reviewRemark: '同意' })).toMatchObject({
    status: '已通过', tagType: 'success', conflict: false, reviewer: '审核员', note: '同意',
  })
  expect(await service.reject(1, { reviewer: '管理员', note: '冲突' })).toMatchObject({
    status: '已驳回', tagType: 'info', conflict: false, reviewer: '管理员', note: '冲突',
  })
  expect(await service.action('1', 'complete', { completed: true })).toMatchObject({ completed: true })
  expect((await service.list())[1]).toEqual({ id: 2, name: 'other' })
})

it('invalid mock storage falls back safely and missing update targets return submitted data', async () => {
  mode.mock = true
  for (const raw of ['broken json', '{}', 'null']) {
    localStorage.setItem(options.storageKey, raw)
    expect(await service.list({ fallbackRecords: [{ id: 7 }] })).toEqual([{ id: 7 }])
  }
  expect(await service.update(99, { name: 'missing' })).toEqual({ name: 'missing' })
  expect(await service.approve(99, { note: 'missing' })).toEqual({ note: 'missing' })
  expect(await service.reject(99, { note: 'missing' })).toEqual({ note: 'missing' })
  expect(await service.action(99, 'complete', { note: 'missing' })).toEqual({ note: 'missing' })
})

it('network errors propagate without mutating local records', async () => {
  localStorage.setItem(options.storageKey, '[{"id":1}]')
  http.post.mockRejectedValue(new Error('network unavailable'))
  await expect(service.create({ name: 'draft' })).rejects.toThrow('network unavailable')
  expect(localStorage.getItem(options.storageKey)).toBe('[{"id":1}]')
})
