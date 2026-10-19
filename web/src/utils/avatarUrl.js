/*
 * 头像资源公共逻辑模块，供相关视图或服务复用。
 * 主要导出：normalizeAvatarUrl。
 */
import { API_BASE_URL } from '../api/config'
import { safeHttpUrl, safeImageUrl } from '../../../shared/safeUrl'

/**
 * 归一化头像URL。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
export function normalizeAvatarUrl(value = '') {
  const avatarUrl = String(value || '').trim()
  if (!avatarUrl) return ''
  if (/^[a-z][a-z0-9+.-]*:/i.test(avatarUrl)) return safeImageUrl(avatarUrl)
  if (avatarUrl.startsWith('//') || avatarUrl.includes('\\') || /[\u0000-\u001f\u007f-\u009f]/.test(avatarUrl)) return ''
  if (avatarUrl.startsWith('/')) return safeHttpUrl(avatarUrl)
  if (/^[\w.-]+\.[a-z]{2,}\//i.test(avatarUrl)) return safeHttpUrl(`https://${avatarUrl}`)
  const root = String(API_BASE_URL || '').replace(/\/$/, '')
  return `${root}/files/avatar-proxy?objectKey=${encodeURIComponent(avatarUrl.replace(/^\/+/, ''))}`
}
