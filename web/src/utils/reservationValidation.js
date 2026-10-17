/*
 * 实验室预约公共逻辑模块，供相关视图或服务复用。
 * 主要导出：normalizeInputText、hasUnsafeContent、hasMeaningfulText、isValidContactText、getMeaningfulTextError 等。
 */
const unsafePattern = /<\s*script|javascript:|on\w+\s*=|<[^>]+>/i
const textSignalPattern = /[\u4e00-\u9fa5A-Za-z]/
const mobilePattern = /^1[3-9]\d{9}$/
const landlinePattern = /^0\d{2,3}-?\d{7,8}(-?\d{1,6})?$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 归一化输入文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const normalizeInputText = (value) => String(value ?? '').trim().replace(/\s+/g, ' ')

/**
 * 判断是否具有Unsafe内容。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const hasUnsafeContent = (value) => unsafePattern.test(String(value ?? ''))

/**
 * 判断是否具有Meaningful文本。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const hasMeaningfulText = (value) => textSignalPattern.test(normalizeInputText(value))

/**
 * 判断是否为有效Contact文本。
 * 入参：value（值）；解构参数（strict、true）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const isValidContactText = (value, { strict = true } = {}) => {
  const text = normalizeInputText(value)
  if (!text) return false

  const compactText = text.replace(/\s+/g, '')
  if (mobilePattern.test(compactText) || landlinePattern.test(compactText) || emailPattern.test(text)) {
    return true
  }

  return !strict && hasMeaningfulText(text) && text.length >= 2 && text.length <= 80
}

/**
 * 获取Meaningful文本错误。
 * 入参：value（值）；options（选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 获取Contact错误。
 * 入参：value（值）；options（选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
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

/**
 * 创建Meaningful文本校验器。
 * 入参：options（选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const createMeaningfulTextValidator = (options = {}) => /** 处理当前调用传入的回调逻辑。 入参：_rule、value、callback。 */ (_rule, value, callback) => {
  const message = getMeaningfulTextError(value, options)
  callback(message ? new Error(message) : undefined)
}

/**
 * 创建Contact校验器。
 * 入参：options（选项）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export const createContactValidator = (options = {}) => /** 处理当前调用传入的回调逻辑。 入参：_rule、value、callback。 */ (_rule, value, callback) => {
  const message = getContactError(value, options)
  callback(message ? new Error(message) : undefined)
}
