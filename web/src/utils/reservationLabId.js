/*
 * 实验室预约公共逻辑模块，供相关视图或服务复用。
 * 主要导出：resolveReservationLabId。
 */
const toPositiveInteger = (value) => {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0 ? value : null
  if (typeof value !== 'string') return null

  const normalized = value.trim()
  if (!/^\d+$/.test(normalized)) return null

  const identifier = Number(normalized)
  return Number.isSafeInteger(identifier) && identifier > 0 ? identifier : null
}

export const resolveReservationLabId = (lab = {}) => toPositiveInteger(lab?.id) ?? toPositiveInteger(lab?.labId)
