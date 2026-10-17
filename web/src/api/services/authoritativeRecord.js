/*
 * 服务端权威记录校验与前端列表更新；此模块不负责身份认证或网络请求。
 * 主要导出：requireAuthoritativeRecord、applyAuthoritativeReservationRecord。
 */
/**
 * 判断是否具有Authoritative标识。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const hasAuthoritativeId = (value) => {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) return false

  const identifier = Number(value)
  return Number.isSafeInteger(identifier) && String(identifier) === value
}

/**
 * 判断是否具有有效Application标识。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const hasValidApplicationId = (value) =>
  (typeof value === 'string' && value.trim().length > 0) || (typeof value === 'number' && Number.isFinite(value))

/**
 * 从直接记录或 { data: record } 响应中提取已落库对象，不回退到客户端提交数据。
 * @param {object} response 调用方已解包到业务层的响应
 * @param {string} resourceName 用于诊断的资源名称
 * @param {string} operation 用于诊断的操作名称
 * @returns {object} 带正安全整数 ID 的原记录，字符串 ID 必须是无前导零的十进制整数
 * @throws {Error} 缺少记录、数组响应或 ID 无效；异常应保留原页面状态
 */
export const requireAuthoritativeRecord = (response, resourceName, operation) => {
  const directRecord = response && typeof response === 'object' && !Array.isArray(response) && hasAuthoritativeId(response.id)
  const record = directRecord ? response : response && typeof response === 'object' && !Array.isArray(response) ? response.data : null

  if (record && typeof record === 'object' && !Array.isArray(record) && hasAuthoritativeId(record.id)) return record

  throw new Error(`${resourceName} ${operation} requires a record response from the server`)
}

/**
 * 以服务端返回的整条记录替换列表项，返回新数组，不修改入参。
 * 此处允许 mock 场景的非空字符串 ID，正式接口的数值标识先由上一个函数严格验证。
 * @param {Array<object>} records 当前列表
 * @param {object} record 已通过对应场景校验的记录
 * @param {{insertIfMissing?: boolean, expectedId?: string|number}} options 创建时显式允许插入；更新时核对目标 ID
 * @returns {Array<object>} 已替换或插入后的列表，其他项保持原对象引用
 * @throws {Error} 目标 ID 错配、记录 ID 无效或默认更新模式下不存在目标项
 */
export const applyAuthoritativeReservationRecord = (records, record, { insertIfMissing = false, expectedId } = {}) => {
  if (!hasValidApplicationId(record?.id)) throw new Error('预约服务未返回有效记录')
  if (expectedId !== undefined && String(record.id) !== String(expectedId)) throw new Error('预约服务返回了错误的目标记录')

  const hasExistingReservation = records.some(/** 判断当前条目是否满足任一匹配条件。 入参：item。 */ (item) => String(item.id) === String(record.id))
  if (!insertIfMissing && !hasExistingReservation) throw new Error('预约服务未返回目标记录')
  if (hasExistingReservation) return records.map(/** 将当前条目转换为目标结果。 入参：item。 */ (item) => (String(item.id) === String(record.id) ? record : item))
  return [record, ...records]
}
