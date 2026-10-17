/*
 * 身份认证的 Pinia 状态模块，集中维护桌面页面共用状态。
 * 主要导出：useAuthStore。
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '@/utils/request'

const AUTH_KEY = 'tvta-ui-auth-session'

type AuthSession = {
  token: string
  username: string
  displayName: string
}

type LoginResponse = {
  code?: number
  message?: string
  data?: {
    token?: string
    user?: {
      username?: string
      realName?: string
      name?: string
    }
  }
}

/**
 * 读取会话认证。
 * @returns 由当前表达式或分支生成的处理结果。
 */
function readSessionAuth(): AuthSession | null {
  if (typeof window === 'undefined') return null
  try {
    const value = window.sessionStorage.getItem(AUTH_KEY)
    if (!value) return null
    const session = JSON.parse(value) as Partial<AuthSession>
    if (!session.token || !session.username) return null
    return {
      token: session.token,
      username: session.username,
      displayName: session.displayName || session.username
    }
  } catch {
    return null
  }
}

/**
 * 写入会话认证。
 * 入参：session（会话）。
 */
function writeSessionAuth(session: AuthSession) {
  if (typeof window === 'undefined') return
  window.sessionStorage.setItem(AUTH_KEY, JSON.stringify(session))
}

/**
 * 清理会话认证。
 */
function clearSessionAuth() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(AUTH_KEY)
}

export const useAuthStore = defineStore('auth', /** 处理 defineStore 调用中的回调逻辑。 */ () => {
  const isAuthenticated = ref(false)
  const username = ref('')
  const authenticatedDisplayName = ref('')
  const hydrated = ref(false)

  const displayName = computed(/** 计算派生状态。 */ () => authenticatedDisplayName.value || username.value || '未登录')

  /**
   * hydrate的处理逻辑。
   */
  function hydrate() {
    if (hydrated.value) return
    hydrated.value = true
    const session = readSessionAuth()
    if (!session) {
      clearSessionAuth()
      return
    }
    isAuthenticated.value = true
    username.value = session.username
    authenticatedDisplayName.value = session.displayName
  }

  /**
   * 登录的处理逻辑。
   * 入参：inputUsername（输入Username）；inputPassword（输入密码）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  async function login(inputUsername: string, inputPassword: string) {
    const normalizedUsername = inputUsername.trim()

    if (!normalizedUsername || !inputPassword) {
      return false
    }

    const response = await request.post<unknown, LoginResponse>('/auth/login', {
      username: normalizedUsername,
      password: inputPassword
    }, { silent: true } as never)
    const token = String(response?.data?.token || '').trim()
    const user = response?.data?.user
    if (response?.code !== 200 || !token || !user?.username) {
      throw new Error(response?.message || '账号或密码错误')
    }

    isAuthenticated.value = true
    username.value = user.username
    authenticatedDisplayName.value = user.realName || user.name || user.username
    writeSessionAuth({
      token,
      username: username.value,
      displayName: authenticatedDisplayName.value
    })
    return true
  }

  /**
   * 退出登录的处理逻辑。
   */
  async function logout() {
    try {
      if (isAuthenticated.value) {
        await request.post('/auth/logout', undefined, { silent: true } as never)
      }
    } finally {
      isAuthenticated.value = false
      username.value = ''
      authenticatedDisplayName.value = ''
      clearSessionAuth()
    }
  }

  return {
    isAuthenticated,
    username,
    displayName,
    hydrate,
    login,
    logout
  }
})
