const toPositiveInteger = (value) => {
  if (typeof value === 'number') return Number.isInteger(value) && value > 0 ? value : null
  if (typeof value !== 'string') return null

  const normalized = value.trim()
  if (!/^\d+$/.test(normalized)) return null

  const identifier = Number(normalized)
  return Number.isInteger(identifier) && identifier > 0 ? identifier : null
}

export const resolveReservationLabId = (lab = {}) => toPositiveInteger(lab?.id) ?? toPositiveInteger(lab?.labId)
