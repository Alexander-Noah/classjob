/*
 * 设备管理的前端接口适配，封装请求参数和响应数据，供页面与组合函数复用。
 * 主要导出：getLabId、getLabName、getLabRoom、getDeviceLabId、getDeviceLabName 等。
 */
const trimText = (value) => String(value ?? '').trim()

export const getLabId = (lab = {}) => lab.id ?? lab.labId ?? ''

export const getLabName = (lab = {}) => trimText(lab.labName || lab.name || lab.lab || lab.laboratoryName)

export const getLabRoom = (lab = {}) => trimText(lab.roomNo || lab.roomNumber || lab.room || lab.location)

export const getDeviceLabId = (device = {}) => device.labId ?? device.lab_id ?? ''

export const getDeviceLabName = (device = {}) =>
  trimText(device.labName || device.lab_name || device.lab || device.laboratoryName)

export const getDeviceLocation = (device = {}) =>
  trimText(device.roomNo || device.room_no || device.location || device.installLocation)

const normalizeText = (value) => trimText(value).replace(/\s+/g, '').toLowerCase()

const sameId = (left, right) => trimText(left) && trimText(left) === trimText(right)

const sameText = (left, right) => normalizeText(left) && normalizeText(left) === normalizeText(right)

const includesText = (text, value) => {
  const normalizedText = normalizeText(text)
  const normalizedValue = normalizeText(value)
  return Boolean(normalizedText && normalizedValue && normalizedText.includes(normalizedValue))
}

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

export function formatLabLabel(lab = {}) {
  const room = getLabRoom(lab)
  const name = getLabName(lab)
  return [room, name].filter(Boolean).join(' ') || trimText(getLabId(lab)) || '未命名实验室'
}

export function matchDeviceLab(device = {}, labs = []) {
  if (!Array.isArray(labs) || !labs.length) return null

  const deviceLabId = getDeviceLabId(device)
  if (trimText(deviceLabId)) {
    const matchedById = labs.find((lab) => sameId(getLabId(lab), deviceLabId))
    if (matchedById) return matchedById
  }

  const location = getDeviceLocation(device)
  if (location) {
    const matchedByRoom = labs.find((lab) => sameText(getLabRoom(lab), location))
    if (matchedByRoom) return matchedByRoom
  }

  const labName = getDeviceLabName(device)
  if (labName) {
    const matchedByName = labs.find((lab) => sameText(getLabName(lab), labName))
    if (matchedByName) return matchedByName
  }

  if (labName) {
    const matchedByFuzzyName = labs.find((lab) => {
      const optionName = getLabName(lab)
      return includesText(labName, optionName) || includesText(optionName, labName)
    })
    if (matchedByFuzzyName) return matchedByFuzzyName
  }

  if (location) {
    const matchedByLocation = labs.find((lab) => includesText(location, getLabRoom(lab)))
    if (matchedByLocation) return matchedByLocation
  }

  const searchText = deviceSearchText(device)
  return (
    labs.find((lab) => includesText(searchText, getLabRoom(lab)) || includesText(searchText, getLabName(lab))) || null
  )
}

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
