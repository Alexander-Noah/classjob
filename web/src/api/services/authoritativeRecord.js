const hasAuthoritativeId = (value) => {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return false

  const identifier = Number(value)
  return Number.isSafeInteger(identifier) && String(identifier) === value
}

const hasValidApplicationId = (value) =>
  (typeof value === 'string' && value.trim().length > 0) || (typeof value === 'number' && Number.isFinite(value))

export const requireAuthoritativeRecord = (response, resourceName, operation) => {
  const directRecord = response && typeof response === 'object' && !Array.isArray(response) && hasAuthoritativeId(response.id)
  const record = directRecord ? response : response && typeof response === 'object' && !Array.isArray(response) ? response.data : null

  if (record && typeof record === 'object' && !Array.isArray(record) && hasAuthoritativeId(record.id)) return record

  throw new Error(`${resourceName} ${operation} requires a record response from the server`)
}

export const applyAuthoritativeReservationRecord = (records, record, { insertIfMissing = false, expectedId } = {}) => {
  if (!hasValidApplicationId(record?.id)) throw new Error('预约服务未返回有效记录')
  if (expectedId !== undefined && String(record.id) !== String(expectedId)) throw new Error('预约服务返回了错误的目标记录')

  const hasExistingReservation = records.some((item) => String(item.id) === String(record.id))
  if (!insertIfMissing && !hasExistingReservation) throw new Error('预约服务未返回目标记录')
  if (hasExistingReservation) return records.map((item) => (String(item.id) === String(record.id) ? record : item))
  return [record, ...records]
}
