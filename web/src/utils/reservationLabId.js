/*
 * 实验室预约公共逻辑模块，供相关视图或服务复用。
 * 主要导出：resolveReservationLabId。
 */
/**
 * 转换为PositiveInteger。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const toPositiveInteger = (value) => {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0 ? value : null
  if (typeof value !== 'string') return null

  const normalized = value.trim()
  if (!/^\d+$/.test(normalized)) return null

  const identifier = Number(normalized)
  return Number.isSafeInteger(identifier) && identifier > 0 ? identifier : null
}

/**
 * 解析预约实验室标识。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const resolveReservationLabId = (lab = {}) => toPositiveInteger(lab?.id) ?? toPositiveInteger(lab?.labId)
