const limits = {
  username: 100, realname: 100, teachername: 100, applicant: 100, borrower: 100, reporter: 100,
  password: 128, oldpassword: 128, newpassword: 128, confirmpassword: 128,
  phone: 40, email: 255, department: 160, avatarurl: 500, captchaid: 36, captchacode: 4,
  name: 160, devicename: 160, labname: 160, coursename: 160, course: 160, classname: 160,
  code: 100, devicecode: 100, labcode: 100, category: 100, unit: 100, type: 100, labtype: 100,
  title: 255, status: 40, processstatus: 40, confirmstatus: 40, publishstatus: 40, tagtype: 40,
  remark: 4000, description: 4000, reason: 4000, purpose: 4000, reviewremark: 4000,
  html: 2 * 1024 * 1024,
  images: 8 * 1024 * 1024, image: 8 * 1024 * 1024, imageurl: 8 * 1024 * 1024,
  image_url: 8 * 1024 * 1024, dataurl: 8 * 1024 * 1024, audio: 8 * 1024 * 1024, data: 8 * 1024 * 1024,
  content: 100000, text: 100000, answer: 100000, question: 100000, systemprompt: 100000, system_prompt: 100000,
}

/** Validate without trimming passwords, modifying records, or rejecting ordinary punctuation. */
export function validateRequestData(data) {
  if (!data || typeof data !== 'object' || (Object.getPrototypeOf(data) !== Object.prototype && !Array.isArray(data))) return
  let count = 0
  const seen = new WeakSet()
  const visit = (value, field, path, depth) => {
    if (depth > 20 || ++count > 50000) throw new Error('提交内容过于复杂，请分批提交')
    if (value != null && ['quantity', 'deviceLimit', 'port', 'capacity', 'stock', 'warnThreshold', 'progress', 'flowStep', 'returnedQuantity', 'remainingQuantity', 'usageHours'].includes(field)
      && (!Number.isInteger(value) || value < -2147483648 || value > 2147483647)) throw new Error(`${path}必须填写范围内的整数`)
    if (typeof value === 'string') {
      const max = path.endsWith('.image_url.url') || path.endsWith('.images[].url') ? 8 * 1024 * 1024 : limits[field.toLowerCase()] || 16000
      if (value.length > max) throw new Error(`${path}不能超过 ${max} 个字符`)
      if (/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f-\u009f]/.test(value)) throw new Error(`${path}包含非法控制字符`)
    } else if (typeof value === 'number' && !Number.isFinite(value)) {
      throw new Error(`${path}必须是有效数值`)
    } else if (value && typeof value === 'object') {
      if (seen.has(value)) throw new Error('提交内容不能包含循环引用')
      seen.add(value)
      if (Array.isArray(value)) {
        if (value.length > 1000) throw new Error('单次最多提交 1000 项，请分批提交')
        value.forEach(item => visit(item, field, `${path}[]`, depth + 1))
      } else {
        const keys = Object.keys(value)
        if (keys.length > 200) throw new Error('提交字段过多')
        keys.forEach(key => {
          if (key.length > 100 || /[\u0000-\u001f\u007f-\u009f]/.test(key) || ['__proto__', 'constructor', 'prototype'].includes(key)) throw new Error('提交内容包含无效字段名')
          visit(value[key], key, `${path}.${key}`, depth + 1)
        })
      }
      seen.delete(value)
    }
  }
  visit(data, '', '表单', 0)
}
