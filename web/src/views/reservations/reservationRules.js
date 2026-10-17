/*
 * 实验室预约公共逻辑模块，供相关视图或服务复用。
 * 主要导出：RESERVATION_STATUS、reservationStatusOptions、getReservationStatusType、getReservationStartAt、isReservationExpired 等。
 */
export const RESERVATION_STATUS = {
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  cancelled: '已取消',
  expired: '已过期',
  conflict: '冲突',
  completed: '已完成',
}

export const reservationStatusOptions = [
  { label: RESERVATION_STATUS.pending, type: 'warning' },
  { label: RESERVATION_STATUS.approved, type: 'success' },
  { label: RESERVATION_STATUS.rejected, type: 'info' },
  { label: RESERVATION_STATUS.cancelled, type: 'info' },
  { label: RESERVATION_STATUS.expired, type: 'info' },
  { label: RESERVATION_STATUS.conflict, type: 'danger' },
  { label: RESERVATION_STATUS.completed, type: 'success' },
]

/**
 * 获取预约状态类型。
 * 入参：status（状态）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getReservationStatusType = (status) =>
  reservationStatusOptions.find(/** 判断当前条目是否为查找目标。 入参：item。 */ (item) => item.label === status)?.type || 'info'

/**
 * 获取预约开始At。
 * 入参：reservation（预约）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getReservationStartAt = (reservation = {}) => {
  const dateText = reservation.date || reservation.reservationDate
  const startTime = String(reservation.timeRange || '')
    .split('-')[0]
    ?.trim()

  if (!dateText || !startTime) return null

  const startAt = new Date(`${dateText}T${startTime.length === 5 ? startTime : `${startTime}:00`}`)
  return Number.isNaN(startAt.getTime()) ? null : startAt
}

/**
 * 判断是否为预约Expired。
 * 入参：reservation（预约）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const isReservationExpired = (reservation = {}, now = new Date()) => {
  const startAt = getReservationStartAt(reservation)
  return Boolean(startAt && now.getTime() >= startAt.getTime())
}

/**
 * 归一化预约状态。
 * 入参：reservation（预约）；now（now）。
 * @returns status 的当前值。
 */
export const normalizeReservationStatus = (reservation = {}, now = new Date()) => {
  let status = reservation.status === '加签中' ? RESERVATION_STATUS.pending : reservation.status || RESERVATION_STATUS.pending

  if (status === RESERVATION_STATUS.pending && isReservationExpired(reservation, now)) {
    status = RESERVATION_STATUS.expired
  } else if (status === RESERVATION_STATUS.pending && (reservation.conflict || reservation.conflictFlag === 1)) {
    status = RESERVATION_STATUS.conflict
  }

  return status
}

/**
 * 归一化预约ForDisplay。
 * 入参：reservation（预约）；now（now）。
 * @returns 组装的键值对象。
 */
export const normalizeReservationForDisplay = (reservation = {}, now = new Date()) => {
  const status = normalizeReservationStatus(reservation, now)
  const conflict = status === RESERVATION_STATUS.conflict || Boolean(reservation.conflict || reservation.conflictFlag === 1)

  return {
    ...reservation,
    status,
    conflict: status === RESERVATION_STATUS.expired ? false : conflict,
    tagType: getReservationStatusType(status),
  }
}

/**
 * 获取预约Actions。
 * 入参：reservation（预约）；now（now）。
 * @returns 组装的键值对象。
 */
export const getReservationActions = (reservation = {}, now = new Date()) => {
  const status = normalizeReservationStatus(reservation, now)

  return {
    approve: status === RESERVATION_STATUS.pending,
    reject: status === RESERVATION_STATUS.pending || status === RESERVATION_STATUS.conflict,
    edit: status === RESERVATION_STATUS.pending || status === RESERVATION_STATUS.conflict,
    cancel:
      (status === RESERVATION_STATUS.pending || status === RESERVATION_STATUS.approved || status === RESERVATION_STATUS.conflict) &&
      !isReservationExpired(reservation, now),
    delete: true,
  }
}

/**
 * 获取Action已禁用。
 * 入参：reservation（预约）；action（action）；now（now）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const getActionDisabled = (reservation = {}, action, now = new Date()) => !getReservationActions(reservation, now)[action]

/**
 * 获取Action已禁用原因。
 * 入参：reservation（预约）；action（action）；now（now）。
 * @returns 文本结果。
 */
export const getActionDisabledReason = (reservation = {}, action, now = new Date()) => {
  if (!getActionDisabled(reservation, action, now)) return ''
  if (action === 'delete') return ''

  const status = normalizeReservationStatus(reservation, now)

  if (status === RESERVATION_STATUS.expired) return '该预约已过期，无法操作'
  if (status === RESERVATION_STATUS.rejected) return '该预约已驳回，无法重复审批'
  if (status === RESERVATION_STATUS.cancelled) return '该预约已取消，无法操作'
  if (status === RESERVATION_STATUS.completed) return '该预约已完成，无法操作'
  if (status === RESERVATION_STATUS.conflict && action === 'approve') return '该预约存在时间冲突，无法通过'
  if (status === RESERVATION_STATUS.approved && action === 'approve') return '该预约已通过，无需重复审批'
  if (status === RESERVATION_STATUS.approved && action === 'cancel' && isReservationExpired(reservation, now)) {
    return '预约已开始，无法取消'
  }
  if (status === RESERVATION_STATUS.approved) return '该预约已通过，不能执行此操作'

  return '当前状态不能执行此操作'
}
