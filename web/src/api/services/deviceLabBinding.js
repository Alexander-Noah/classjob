/*
 * 设备管理的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getLabId、getLabName、getLabRoom、getDeviceLabId、getDeviceLabName 等。
 */
/**
 * 裁剪文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const trimText = (value) => String(value ?? '').trim()

/**
 * 获取实验室标识。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getLabId = (lab = {}) => lab.id ?? lab.labId ?? ''

/**
 * 获取实验室名称。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getLabName = (lab = {}) => trimText(lab.labName || lab.name || lab.lab || lab.laboratoryName)

/**
 * 获取实验室Room。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getLabRoom = (lab = {}) => trimText(lab.roomNo || lab.roomNumber || lab.room || lab.location)

/**
 * 获取设备实验室标识。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getDeviceLabId = (device = {}) => device.labId ?? device.lab_id ?? ''

/**
 * 获取设备实验室名称。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getDeviceLabName = (device = {}) =>
  trimText(device.labName || device.lab_name || device.lab || device.laboratoryName)

/**
 * 获取设备Location。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getDeviceLocation = (device = {}) =>
  trimText(device.roomNo || device.room_no || device.location || device.installLocation)

/**
 * 归一化文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeText = (value) => trimText(value).replace(/\s+/g, '').toLowerCase()

/**
 * same标识的处理逻辑。
 * 入参：left（left）；right（right）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const sameId = (left, right) => trimText(left) && trimText(left) === trimText(right)

/**
 * same文本的处理逻辑。
 * 入参：left（left）；right（right）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const sameText = (left, right) => normalizeText(left) && normalizeText(left) === normalizeText(right)

/**
 * includes文本的处理逻辑。
 * 入参：text（文本）；value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const includesText = (text, value) => {
  const normalizedText = normalizeText(text)
  const normalizedValue = normalizeText(value)
  return Boolean(normalizedText && normalizedValue && normalizedText.includes(normalizedValue))
}

/**
 * 设备Search文本的处理逻辑。
 * 入参：device（设备）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const deviceSearchText = (device = {}) =>
  [
    device.deviceName,
    device.name,
    device.specification,
    device.specs,
    device.standardRequirement,
    device.remark,
    getDeviceLabName(device),
    getDeviceLocation(device),
  ]
    .map(trimText)
    .filter(Boolean)
    .join(' ')

/**
 * 格式化实验室标签。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function formatLabLabel(lab = {}) {
  const room = getLabRoom(lab)
  const name = getLabName(lab)
  return [room, name].filter(Boolean).join(' ') || trimText(getLabId(lab)) || '未命名实验室'
}

/**
 * 匹配设备实验室。
 * 入参：device（设备）；labs（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function matchDeviceLab(device = {}, labs = []) {
  if (!Array.isArray(labs) || !labs.length) return null

  const deviceLabId = getDeviceLabId(device)
  if (trimText(deviceLabId)) {
    const matchedById = labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => sameId(getLabId(lab), deviceLabId))
    if (matchedById) return matchedById
  }

  const location = getDeviceLocation(device)
  if (location) {
    const matchedByRoom = labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => sameText(getLabRoom(lab), location))
    if (matchedByRoom) return matchedByRoom
  }

  const labName = getDeviceLabName(device)
  if (labName) {
    const matchedByName = labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => sameText(getLabName(lab), labName))
    if (matchedByName) return matchedByName
  }

  if (labName) {
    const matchedByFuzzyName = labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => {
      const optionName = getLabName(lab)
      return includesText(labName, optionName) || includesText(optionName, labName)
    })
    if (matchedByFuzzyName) return matchedByFuzzyName
  }

  if (location) {
    const matchedByLocation = labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => includesText(location, getLabRoom(lab)))
    if (matchedByLocation) return matchedByLocation
  }

  const searchText = deviceSearchText(device)
  return (
    labs.find(/** 判断当前条目是否为查找目标。 入参：lab。 */ (lab) => includesText(searchText, getLabRoom(lab)) || includesText(searchText, getLabName(lab))) || null
  )
}

/**
 * 应用设备实验室Binding。
 * 入参：device（设备）；lab（实验室）。
 * @returns 组装的键值对象。
 */
export function applyDeviceLabBinding(device = {}, lab = null) {
  if (!lab) return { ...device }

  const labId = getLabId(lab)
  const labName = getLabName(lab)
  const room = getLabRoom(lab)

  return {
    ...device,
    labId,
    labName,
    laboratoryName: labName,
    roomNo: room,
    location: room,
    installLocation: room,
  }
}
