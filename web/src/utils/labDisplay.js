/*
 * 实验室公共逻辑模块，供相关视图或服务复用。
 * 主要导出：getLabName、getLabRoomCode、formatLabDisplayName、formatLabOptionLabel、createLabSelectOption。
 */
/**
 * 获取实验室名称。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getLabName = (lab = {}) => {
  if (typeof lab === 'string') return lab
  return lab.name || lab.labName || lab.roomName || lab.classroomName || lab.label || lab.value || ''
}

/**
 * 获取实验室Room编码。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getLabRoomCode = (lab = {}) => {
  if (typeof lab === 'string') {
    return lab.match(/^[A-Z]\d{3}/i)?.[0] || ''
  }

  return lab.code || lab.labCode || lab.room || lab.roomNo || getLabName(lab).match(/^[A-Z]\d{3}/i)?.[0] || ''
}

/**
 * 格式化实验室Display名称。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const formatLabDisplayName = (lab = {}) => {
  const name = String(getLabName(lab) || '').trim()
  const roomCode = String(getLabRoomCode(lab) || '').trim()

  if (!name) return roomCode
  if (!roomCode || name.toLowerCase().startsWith(roomCode.toLowerCase())) return name

  return `${roomCode} ${name}`
}

/**
 * 格式化实验室选项标签。
 * 入参：lab（实验室）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const formatLabOptionLabel = (lab = {}) => {
  const displayName = formatLabDisplayName(lab)
  const capacity = typeof lab === 'object' ? Number(lab.capacity) || 0 : 0
  return capacity ? `${displayName} · ${capacity}人` : displayName
}

/**
 * 创建实验室Select选项。
 * 入参：lab（实验室）。
 * @returns 组装的键值对象。
 */
export const createLabSelectOption = (lab = {}) => {
  const displayName = formatLabDisplayName(lab)
  const value =
    lab && typeof lab === 'object'
      ? lab.value ?? lab.id ?? lab.labId ?? lab.name ?? lab.labName ?? displayName
      : displayName

  return {
    ...(lab && typeof lab === 'object' ? lab : {}),
    label: formatLabOptionLabel(lab),
    value,
    displayName,
    labName: displayName,
  }
}
