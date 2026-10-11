import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('reservation conflict detection does not trigger contact validation or hide conflict column', () => {
  const reservationLogic = readSource('../src/composables/useReservationManagement.js')
  const table = readSource('../src/views/reservations/components/ReservationListTable.vue')
  const detectConflictBlock = reservationLogic.match(/const detectConflicts = \(showMessage = true\) => \{[\s\S]*?\n  \}/)?.[0] || ''

  assert.doesNotMatch(detectConflictBlock, /persistReservations\(\)/)
  assert.match(detectConflictBlock, /persistConflictMarks\(\)/)
  assert.doesNotMatch(table, /fixed="right" label="操作" width="388"/)
  assert.match(table, /label="操作" width="300"/)
  assert.match(table, /class="operation-column"/)
})
