import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { applyAuthoritativeReservationRecord, requireAuthoritativeRecord } from '../src/api/services/authoritativeRecord.js'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

const getFunctionSource = (source, name) => {
  const start = source.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `${name} should exist`)
  const nextFunction = source.indexOf('\n  const ', start + 1)
  return source.slice(start, nextFunction === -1 ? source.length : nextFunction)
}

test('reservation service explicitly opts into server-authoritative mutation responses', () => {
  const resources = readSource('../src/api/services/basicResources.js')

  assert.match(
    resources,
    /reservationResourceService\s*=\s*createCollectionService\(\{[\s\S]*?resourceName:\s*'reservations',[\s\S]*?serverAuthoritativeRecords:\s*true/,
  )
})

test('authoritative real-mode record operations require an HTTP record instead of the submitted payload', () => {
  const collectionService = readSource('../src/api/services/collectionService.js')

  assert.match(collectionService, /import \{ requireAuthoritativeRecord \} from '\.\/authoritativeRecord'/)
  for (const operation of ['create', 'update', 'approve', 'reject', 'action']) {
    const source = getFunctionSource(collectionService, operation)
    assert.match(source, /serverAuthoritativeRecords \? requireAuthoritativeRecord\(data, resourceName, '[a-z]+'\) : normalizeRecordResponse\(/)
  }
})

test('authoritative record parser accepts only direct or enveloped records with valid ids', () => {
  assert.deepEqual(requireAuthoritativeRecord({ code: 0, message: 'ok', data: { id: 2 } }, 'reservations', 'create'), { id: 2 })
  for (const response of [[], null, {}, { status: 200 }, { requestId: 'x' }, { data: [] }, { data: null }, { data: {} }, { data: { id: '' } }, { data: { id: Number.NaN } }]) {
    assert.throws(() => requireAuthoritativeRecord(response, 'reservations', 'approve'), /reservations approve/)
  }
})

test('authoritative real-mode records require positive safe integer identifiers', () => {
  assert.deepEqual(requireAuthoritativeRecord({ id: 42 }, 'reservations', 'create'), { id: 42 })
  assert.deepEqual(requireAuthoritativeRecord({ data: { id: '42' } }, 'reservations', 'create'), { id: '42' })

  for (const id of [0, -1, 1.5, 'error', '007', String(Number.MAX_SAFE_INTEGER) + '0', Number.MAX_SAFE_INTEGER + 1]) {
    assert.throws(() => requireAuthoritativeRecord({ id }, 'reservations', 'create'), /reservations create/)
  }
})

test('authoritative record application replaces or inserts without mutating input', () => {
  const records = [{ id: 'RSV-1', status: '待审核' }]
  assert.deepEqual(applyAuthoritativeReservationRecord(records, { id: 'RSV-1', status: '已通过' }), [{ id: 'RSV-1', status: '已通过' }])
  assert.deepEqual(records, [{ id: 'RSV-1', status: '待审核' }])
  assert.deepEqual(applyAuthoritativeReservationRecord(records, { id: 'RSV-2' }, { insertIfMissing: true }), [{ id: 'RSV-2' }, records[0]])
  assert.deepEqual(applyAuthoritativeReservationRecord(records, { id: 'RSV-1' }, { insertIfMissing: true }), [{ id: 'RSV-1' }])
  assert.throws(() => applyAuthoritativeReservationRecord(records, { id: 'RSV-2' }, { expectedId: 'RSV-1' }), /错误的目标记录/)
  assert.throws(() => applyAuthoritativeReservationRecord(records, { id: 'RSV-2' }), /未返回目标记录/)
})

test('collection service keeps mock mutations available for opted-in resources', () => {
  const collectionService = readSource('../src/api/services/collectionService.js')

  for (const operation of ['create', 'update', 'approve', 'reject', 'action']) {
    const source = getFunctionSource(collectionService, operation)
    assert.match(source, /if \(API_USE_MOCK\) \{[\s\S]*?return /)
  }
})

test('reservation mutations replace display state with returned server records without reloading', () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')

  assert.match(reservationManagement, /const applyAuthoritativeReservation = \(reservation, \{ insertIfMissing = false, expectedId \} = \{\}\) =>/)
  for (const operation of ['submitReservation', 'approveReservation', 'rejectReservation']) {
    const source = getFunctionSource(reservationManagement, operation)
    assert.match(source, /applyAuthoritativeReservation\(/)
    assert.doesNotMatch(source, /await loadReservations\(\)/)
  }
  const submitSource = getFunctionSource(reservationManagement, 'submitReservation')
  assert.match(submitSource, /applyAuthoritativeReservation\(savedReservation, \{ insertIfMissing: true \}\)/)
  assert.match(submitSource, /applyAuthoritativeReservation\(savedReservation, \{ expectedId: editingId\.value \}\)/)
  for (const operation of ['submitReservation', 'approveReservation', 'rejectReservation', 'cancelReservation']) {
    const source = getFunctionSource(reservationManagement, operation)
    if (operation !== 'submitReservation') assert.match(source, /applyAuthoritativeReservation\([^,]+, \{ expectedId: currentReservation\.id \}\)/)
  }
  assert.match(reservationManagement, /applyAuthoritativeReservationRecord\(reservations\.value, normalizedReservation, \{ insertIfMissing, expectedId \}\)/)
})

test('approval has no browser-side usage-record write or import', () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')
  const approveSource = getFunctionSource(reservationManagement, 'approveReservation')

  assert.doesNotMatch(reservationManagement, /usageRecordResourceService/)
  assert.doesNotMatch(reservationManagement, /upsertUsageRecordForReservation/)
  assert.doesNotMatch(approveSource, /usage record|使用记录/i)
})

test('failed reservation mutations restore both the previous array and selection', () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')

  for (const operation of ['submitReservation', 'approveReservation', 'rejectReservation']) {
    const source = getFunctionSource(reservationManagement, operation)
    assert.match(source, /const previousReservations = \[\.\.\.reservations\.value\]/)
    assert.match(source, /const previousSelectedReservationId = selectedReservationId\.value/)
    assert.match(
      source,
      /catch \(error\) \{[\s\S]*?reservations\.value = previousReservations[\s\S]*?selectedReservationId\.value = previousSelectedReservationId/,
    )
  }
})
