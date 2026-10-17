/*
 * 实验室公共逻辑模块，供相关视图或服务复用。
 * 主要导出：getLabName、getLabRoomCode、formatLabDisplayName、formatLabOptionLabel、createLabSelectOption。
 */
export const getLabName = (lab = {}) => {
  if (typeof lab === 'string') return lab
  return lab.name || lab.labName || lab.roomName || lab.classroomName || lab.label || lab.value || ''
}

export const getLabRoomCode = (lab = {}) => {
  if (typeof lab === 'string') {
    return lab.match(/^[A-Z]\d{3}/i)?.[0] || ''
  }

  return lab.code || lab.labCode || lab.room || lab.roomNo || getLabName(lab).match(/^[A-Z]\d{3}/i)?.[0] || ''
}

export const formatLabDisplayName = (lab = {}) => {
  const name = String(getLabName(lab) || '').trim()
  const roomCode = String(getLabRoomCode(lab) || '').trim()

  if (!name) return roomCode
  if (!roomCode || name.toLowerCase().startsWith(roomCode.toLowerCase())) return name

  return `${roomCode} ${name}`
}

export const formatLabOptionLabel = (lab = {}) => {
  const displayName = formatLabDisplayName(lab)
  const capacity = typeof lab === 'object' ? Number(lab.capacity) || 0 : 0
  return capacity ? `${displayName} · ${capacity}人` : displayName
}

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
