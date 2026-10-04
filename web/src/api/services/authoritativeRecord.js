const hasValidId = (value) =>
  (typeof value === 'string' && value.trim().length > 0) || (typeof value === 'number' && Number.isFinite(value))

export const requireAuthoritativeRecord = (response, resourceName, operation) => {
  const directRecord = response && typeof response === 'object' && !Array.isArray(response) && hasValidId(response.id)
  const record = directRecord ? response : response && typeof response === 'object' && !Array.isArray(response) ? response.data : null

  if (record && typeof record === 'object' && !Array.isArray(record) && hasValidId(record.id)) return record

  throw new Error(`${resourceName} ${operation} requires a record response from the server`)
}

export const applyAuthoritativeReservationRecord = (records, record, { insertIfMissing = false, expectedId } = {}) => {
  if (!hasValidId(record?.id)) throw new Error('预约服务未返回有效记录')
  if (expectedId !== undefined && String(record.id) !== String(expectedId)) throw new Error('预约服务返回了错误的目标记录')

  const hasExistingReservation = records.some((item) => String(item.id) === String(record.id))
  if (!insertIfMissing && !hasExistingReservation) throw new Error('预约服务未返回目标记录')
  if (hasExistingReservation) return records.map((item) => (String(item.id) === String(record.id) ? record : item))
  return [record, ...records]
}
