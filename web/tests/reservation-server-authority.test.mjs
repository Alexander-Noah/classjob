import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test.skip('real reservation create approve and reject responses are server authoritative: enabled when server-authority implementation lands in Task 8', () => {
  const collectionService = readSource('../src/api/services/collectionService.js')

  assert.doesNotMatch(collectionService, /normalizeRecordResponse\(data,\s*(?:nextRecord|payload)\)/)
})

test.skip('real approval does not create a client-side usage record: enabled when server-authority implementation lands in Task 8', () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')

  assert.doesNotMatch(
    reservationManagement,
    /const approvedReservation = await reservationResourceService\.approve\([\s\S]*?await upsertUsageRecordForReservation\(/,
  )
})

test('reservation API failures retain or restore the previous reservations array', () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')

  assert.match(
    reservationManagement,
    /const previousReservations = \[\.\.\.reservations\.value\][\s\S]*?catch \(error\) \{\s*reservations\.value = previousReservations/,
  )
  assert.doesNotMatch(
    reservationManagement,
    /const approveReservation = async[\s\S]*?reservationResourceService\.approve[\s\S]*?reservations\.value\s*=[\s\S]*?catch \(error\)/,
  )
  assert.doesNotMatch(
    reservationManagement,
    /const rejectReservation = async[\s\S]*?reservationResourceService\.reject[\s\S]*?reservations\.value\s*=[\s\S]*?catch \(error\)/,
  )
})
