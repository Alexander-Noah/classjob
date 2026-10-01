const unsafePattern = /<\s*script|javascript:|on\w+\s*=|<[^>]+>/i
const textSignalPattern = /[\u4e00-\u9fa5A-Za-z]/
const mobilePattern = /^1[3-9]\d{9}$/
const landlinePattern = /^0\d{2,3}-?\d{7,8}(-?\d{1,6})?$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const normalizeInputText = (value) => String(value ?? '').trim().replace(/\s+/g, ' ')

export const hasUnsafeContent = (value) => unsafePattern.test(String(value ?? ''))

export const hasMeaningfulText = (value) => textSignalPattern.test(normalizeInputText(value))

export const isValidContactText = (value, { strict = true } = {}) => {
  const text = normalizeInputText(value)
  if (!text) return false

  const compactText = text.replace(/\s+/g, '')
  if (mobilePattern.test(compactText) || landlinePattern.test(compactText) || emailPattern.test(text)) {
    return true
  }

  return !strict && hasMeaningfulText(text) && text.length >= 2 && text.length <= 80
}

export function getMeaningfulTextError(value, options = {}) {
  const { label = '内容', max = 80, min = 2, required = true } = options
  const text = normalizeInputText(value)

  if (!text) return required ? `请填写${label}` : ''
  if (hasUnsafeContent(text)) return `${label}不能包含脚本或 HTML 标签`
  if (text.length < min) return `${label}至少填写 ${min} 个字符`
  if (text.length > max) return `${label}不能超过 ${max} 个字符`
  if (!hasMeaningfulText(text)) return `${label}不能只填写数字或符号`

  return ''
}

export function getContactError(value, options = {}) {
  const { label = '联系方式', required = true, strict = true } = options
  const text = normalizeInputText(value)

  if (!text) return required ? `请填写${label}` : ''
  if (hasUnsafeContent(text)) return `${label}不能包含脚本或 HTML 标签`
  if (text.length > 80) return `${label}不能超过 80 个字符`
  if (!isValidContactText(text, { strict })) {
    return strict ? `${label}请填写正确的手机号、邮箱或座机` : `${label}请填写姓名、手机号、邮箱或座机`
  }

  return ''
}

export const createMeaningfulTextValidator = (options = {}) => (_rule, value, callback) => {
  const message = getMeaningfulTextError(value, options)
  callback(message ? new Error(message) : undefined)
}

export const createContactValidator = (options = {}) => (_rule, value, callback) => {
  const message = getContactError(value, options)
  callback(message ? new Error(message) : undefined)
}
