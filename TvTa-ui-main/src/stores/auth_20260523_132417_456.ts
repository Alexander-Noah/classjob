import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const AUTH_KEY = 'tvta-ui-auth-session'
const USERS_KEY = 'tvta-ui-users'

interface User {
  username: string
  password: string
  role: 'admin' | 'teacher'
  createdAt: string
}

interface UserWithoutPassword {
  username: string
  role: 'admin' | 'teacher'
  createdAt: string
}

function clearSessionAuth() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(AUTH_KEY)
}

function getDefaultUsers(): User[] {
  return [
    {
      username: 'admin',
      password: 'admin123',
      role: 'admin',
      createdAt: new Date().toISOString()
    },
    {
      username: 'teacher',
      password: '123456',
      role: 'teacher',
      createdAt: new Date().toISOString()
    }
  ]
}

function readUsers(): User[] {
  if (typeof window === 'undefined') return getDefaultUsers()

  try {
    const raw = window.localStorage.getItem(USERS_KEY)
    if (!raw) {
      const defaults = getDefaultUsers()
      window.localStorage.setItem(USERS_KEY, JSON.stringify(defaults))
      return defaults
    }

    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      const defaults = getDefaultUsers()
      window.localStorage.setItem(USERS_KEY, JSON.stringify(defaults))
      return defaults
    }

    return parsed as User[]
  } catch {
    const defaults = getDefaultUsers()
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(USERS_KEY, JSON.stringify(defaults))
    }
    return defaults
  }
}

function writeUsers(users: User[]) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

export const useAuthStore = defineStore('auth', () => {
  const isAuthenticated = ref(false)
  const username = ref('')
  const hydrated = ref(false)

  const displayName = computed(() => username.value || 'admin')

  function hydrate() {
    if (hydrated.value) return
    hydrated.value = true
    clearSessionAuth()
  }

  function login(inputUsername: string, inputPassword: string) {
    const normalizedUsername = inputUsername.trim()
    const normalizedPassword = inputPassword.trim()

    if (!normalizedUsername || !normalizedPassword) {
      return false
    }

    const users = readUsers()
    const matchedUser = users.find(
      (u) => u.username === normalizedUsername && u.password === normalizedPassword
    )

    if (!matchedUser) {
      return false
    }

    isAuthenticated.value = true
    username.value = normalizedUsername
    return true
  }

  function register(inputUsername: string, inputPassword: string, role: 'admin' | 'teacher'): boolean {
    const normalizedUsername = inputUsername.trim()
    const normalizedPassword = inputPassword.trim()

    if (!normalizedUsername || !normalizedPassword) {
      return false
    }

    const users = readUsers()
    const exists = users.some((u) => u.username === normalizedUsername)
    if (exists) {
      return false
    }

    users.push({
      username: normalizedUsername,
      password: normalizedPassword,
      role,
      createdAt: new Date().toISOString()
    })

    writeUsers(users)
    return true
  }

  function batchRegister(
    entries: Array<{ username: string; password: string; role: 'admin' | 'teacher' }>
  ): { success: number; failed: Array<{ username: string; reason: string }> } {
    const users = readUsers()
    const existingUsernames = new Set(users.map((u) => u.username))

    let success = 0
    const failed: Array<{ username: string; reason: string }> = []

    for (const entry of entries) {
      const normalizedUsername = entry.username.trim()
      const normalizedPassword = entry.password.trim()

      if (!normalizedUsername) {
        failed.push({ username: entry.username || '(空)', reason: '用户名为空' })
        continue
      }

      if (!normalizedPassword) {
        failed.push({ username: normalizedUsername, reason: '密码为空' })
        continue
      }

      if (existingUsernames.has(normalizedUsername)) {
        failed.push({ username: normalizedUsername, reason: '用户名已存在' })
        continue
      }

      users.push({
        username: normalizedUsername,
        password: normalizedPassword,
        role: entry.role,
        createdAt: new Date().toISOString()
      })
      existingUsernames.add(normalizedUsername)
      success++
    }

    if (success > 0) {
      writeUsers(users)
    }

    return { success, failed }
  }

  function getAllUsers(): UserWithoutPassword[] {
    const users = readUsers()
    return users.map(({ username, role, createdAt }) => ({
      username,
      role,
      createdAt
    }))
  }

  function deleteUser(targetUsername: string): boolean {
    const users = readUsers()
    const index = users.findIndex((u) => u.username === targetUsername)
    if (index === -1) return false

    users.splice(index, 1)
    writeUsers(users)
    return true
  }

  function updateUserPassword(targetUsername: string, newPassword: string): boolean {
    const users = readUsers()
    const user = users.find((u) => u.username === targetUsername)
    if (!user) return false

    user.password = newPassword
    writeUsers(users)
    return true
  }

  function logout() {
    isAuthenticated.value = false
    username.value = ''
    clearSessionAuth()
  }

  return {
    isAuthenticated,
    username,
    displayName,
    hydrate,
    login,
    register,
    batchRegister,
    getAllUsers,
    deleteUser,
    updateUserPassword,
    logout
  }
})