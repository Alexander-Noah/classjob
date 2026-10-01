import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const AUTH_KEY = 'tvta-ui-auth-session'

function clearSessionAuth() {
  if (typeof window === 'undefined') return
  window.sessionStorage.removeItem(AUTH_KEY)
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

    const validAccount =
      (normalizedUsername === 'admin' && normalizedPassword === 'admin123') ||
      (normalizedUsername === 'teacher' && normalizedPassword === '123456')

    if (!validAccount) {
      return false
    }

    isAuthenticated.value = true
    username.value = normalizedUsername
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
    logout
  }
})
