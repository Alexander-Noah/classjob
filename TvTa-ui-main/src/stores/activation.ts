/*
 * activation的 Pinia 状态模块，集中维护桌面页面共用状态。
 * 主要导出：useActivationStore。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import request from '@/utils/request'
import { getMacAddress } from '@/sys-methods/system'

// 本地缓存用于启动时恢复显示，设备许可的最终状态仍需服务端验证。
const ACTIVATION_KEY = 'tvta-ui-activation-token'
const TRIAL_LOCK_KEY = 'tvta-ui-trial-lock'
// 本地试用只允许在 test 模式显式开启，正式构建不能以缓存替代远程激活。
const LOCAL_TRIAL_ENABLED = import.meta.env.MODE === 'test' && import.meta.env.VITE_ENABLE_LOCAL_TRIAL === 'true'
const TRIAL_DAYS = Number(import.meta.env.VITE_LOCAL_TRIAL_DAYS || 30)
const TRIAL_DEVICE_LIMIT = Number(import.meta.env.VITE_LOCAL_TRIAL_DEVICE_LIMIT || 10)

type ActivationMode = 'licensed' | 'trial'

interface ActivationPayload {
  token: string
  labName: string
  quota: number
  activatedAt: string
  labId?: number | null
  macAddress?: string
  mode?: ActivationMode
  trialStartedAt?: string
  trialExpiresAt?: string
}

interface TrialLockPayload {
  used: boolean
  startedAt: string
  expiresAt: string
}

interface ActivationVerifyResponse {
  valid?: boolean
  code?: string
  labId?: number | string
  labName?: string
  deviceLimit?: number
  boundCount?: number
  remainingQuota?: number
  expired?: boolean
  status?: string
  message?: string
  macAddress?: string
  machineCode?: string
  remark?: string
}

const ACTIVATION_CODE_PATTERN = /^(?:[0-9a-f]{8}-[0-9a-f]{8}-[0-9a-f]{8}|zylab-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4})$/i
const INVALID_REMOTE_STATUSES = new Set(['remote_unbound', 'disabled', 'expired'])

function normalizeLabId(value: unknown) {
  const numeric = Number(value || 0)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

function readActivation(): ActivationPayload | null {
  if (typeof window === 'undefined') return null

  try {
    const raw = window.localStorage.getItem(ACTIVATION_KEY)
    if (!raw) return null

    const parsed = JSON.parse(raw) as Partial<ActivationPayload>
    if (!parsed.token || !parsed.labName || !parsed.quota) return null

    return {
      token: parsed.token,
      labName: parsed.labName,
      quota: Number(parsed.quota),
      activatedAt: parsed.activatedAt || new Date().toISOString(),
      labId: normalizeLabId(parsed.labId),
      macAddress: parsed.macAddress || '',
      mode: parsed.mode || 'licensed',
      trialStartedAt: parsed.trialStartedAt || '',
      trialExpiresAt: parsed.trialExpiresAt || ''
    }
  } catch {
    return null
  }
}

function readTrialLock(): TrialLockPayload | null {
  if (typeof window === 'undefined') return null

  try {
    const raw = window.localStorage.getItem(TRIAL_LOCK_KEY)
    if (!raw) return null

    const parsed = JSON.parse(raw) as Partial<TrialLockPayload>
    if (!parsed.used || !parsed.startedAt || !parsed.expiresAt) return null

    return {
      used: true,
      startedAt: parsed.startedAt,
      expiresAt: parsed.expiresAt
    }
  } catch {
    return null
  }
}

function writeActivation(payload: ActivationPayload) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(ACTIVATION_KEY, JSON.stringify(payload))
}

function writeTrialLock(payload: TrialLockPayload) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(TRIAL_LOCK_KEY, JSON.stringify(payload))
}

function clearActivation() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(ACTIVATION_KEY)
}

function addDays(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

export const useActivationStore = defineStore('activation', () => {
  const token = ref('')
  const labName = ref('')
  const labId = ref<number | null>(null)
  const quota = ref(0)
  const activatedAt = ref('')
  const macAddress = ref('')
  const mode = ref<ActivationMode>('licensed')
  const trialStartedAt = ref('')
  const trialExpiresAt = ref('')
  const trialUsed = ref(false)
  const hydrated = ref(false)
  const now = ref(Date.now())

  const isLicensed = computed(() => Boolean(token.value && labName.value && quota.value > 0 && mode.value === 'licensed'))
  const isTrial = computed(() => mode.value === 'trial')
  const isTrialActive = computed(() => {
    if (!isTrial.value || !trialExpiresAt.value) return false
    return now.value <= new Date(trialExpiresAt.value).getTime()
  })
  const isTrialExpired = computed(() => {
    if (!trialUsed.value || !trialExpiresAt.value) return false
    return now.value > new Date(trialExpiresAt.value).getTime()
  })
  const isUsable = computed(() => isLicensed.value || isTrialActive.value)
  const isActivated = computed(() => isLicensed.value)
  const canStartTrial = computed(() => LOCAL_TRIAL_ENABLED && !isLicensed.value && !trialUsed.value)
  const trialRemainingDays = computed(() => {
    if (!isTrialActive.value || !trialExpiresAt.value) return 0
    const diff = new Date(trialExpiresAt.value).getTime() - now.value
    return Math.max(1, Math.ceil(diff / 86400000))
  })
  const maskedToken = computed(() => {
    if (!token.value) return ''
    if (isTrial.value) return 'TRIAL-****'
    if (token.value.length <= 10) return `${token.value.slice(0, 2)}****${token.value.slice(-2)}`
    return `${token.value.slice(0, 4)}****${token.value.slice(-4)}`
  })
  const statusText = computed(() => {
    if (isLicensed.value) return `Activated: ${labName.value}`
    if (isTrialActive.value) return `Trial active: ${trialRemainingDays.value} days left`
    if (trialUsed.value) return 'Trial ended. Enter an activation code.'
    return 'Not activated. Activate or start a 30-day trial.'
  })

  function refreshLicenseState() {
    now.value = Date.now()
  }

  function clearInvalidActivation(message?: string) {
    resetActivation()
    if (message && typeof window !== 'undefined') {
      window.alert(message)
    }
  }

  function applyPayload(payload: ActivationPayload) {
    token.value = payload.token
    labName.value = payload.labName
    labId.value = normalizeLabId(payload.labId)
    quota.value = payload.quota
    activatedAt.value = payload.activatedAt
    macAddress.value = payload.macAddress || ''
    mode.value = payload.mode || 'licensed'
    trialStartedAt.value = payload.trialStartedAt || ''
    trialExpiresAt.value = payload.trialExpiresAt || ''
  }

  function hydrate() {
    if (hydrated.value) {
      refreshLicenseState()
      return
    }
    hydrated.value = true
    refreshLicenseState()

    const savedLock = readTrialLock()
    if (savedLock) {
      trialUsed.value = true
      trialStartedAt.value = savedLock.startedAt
      trialExpiresAt.value = savedLock.expiresAt
    }

    const savedActivation = readActivation()
    if (!savedActivation) return

    applyPayload(savedActivation)
    if (savedActivation.mode === 'trial') {
      trialUsed.value = true
      trialStartedAt.value = savedActivation.trialStartedAt || trialStartedAt.value
      trialExpiresAt.value = savedActivation.trialExpiresAt || trialExpiresAt.value
    }
  }

  async function activate(inputCode: string) {
    const normalizedCode = inputCode.trim().toLowerCase()
    if (!ACTIVATION_CODE_PATTERN.test(normalizedCode)) return false

    const localMacAddress = await getMacAddress()
    if (!localMacAddress) {
      throw new Error('Local MAC address was not found. Please run this in the desktop client.')
    }

    const result = await request.post<any, { data?: ActivationVerifyResponse }>('/activation-codes/verify', {
      code: normalizedCode,
      macAddress: localMacAddress,
      machineCode: localMacAddress,
      hostName: typeof window !== 'undefined' ? window.location.hostname : ''
    })
    const activation = result?.data || (result as unknown as ActivationVerifyResponse)
    if (!activation?.valid) {
      if (INVALID_REMOTE_STATUSES.has(String(activation?.status || ''))) {
        clearInvalidActivation(activation?.message || 'Device authorization is no longer valid.')
      }
      return false
    }

    const payload: ActivationPayload = {
      token: activation.code || normalizedCode,
      labId: normalizeLabId(activation.labId),
      labName: activation.labName || activation.remark || 'Licensed client',
      quota: Number(activation.deviceLimit || 1),
      activatedAt: new Date().toISOString(),
      macAddress: localMacAddress,
      mode: 'licensed'
    }

    applyPayload(payload)
    writeActivation(payload)
    return true
  }

  async function verifyStoredActivation() {
    hydrate()
    if (!isLicensed.value || !token.value) return true

    const localMacAddress = macAddress.value || (await getMacAddress())
    if (!localMacAddress) return false

    try {
      const result = await request.post<any, { data?: ActivationVerifyResponse }>('/activation-codes/verify', {
        code: token.value,
        macAddress: localMacAddress,
        machineCode: localMacAddress,
        hostName: typeof window !== 'undefined' ? window.location.hostname : ''
      })
      const activation = result?.data || (result as unknown as ActivationVerifyResponse)
      const status = String(activation?.status || '')

      if (activation?.valid) {
        const payload: ActivationPayload = {
          token: activation.code || token.value,
          labId: normalizeLabId(activation.labId) || labId.value,
          labName: activation.labName || activation.remark || labName.value,
          quota: Number(activation.deviceLimit || quota.value || 1),
          activatedAt: activatedAt.value || new Date().toISOString(),
          macAddress: localMacAddress,
          mode: 'licensed'
        }
        applyPayload(payload)
        writeActivation(payload)
        refreshLicenseState()
        return true
      }

      if (INVALID_REMOTE_STATUSES.has(status) || status === 'quota_exceeded') {
        clearInvalidActivation(activation?.message || 'Device authorization is no longer valid.')
      }
      return false
    } catch {
      return false
    }
  }

  function startTrial() {
    refreshLicenseState()
    if (!LOCAL_TRIAL_ENABLED || !canStartTrial.value) return false

    const started = new Date()
    const expires = addDays(started, TRIAL_DAYS)
    const lockPayload: TrialLockPayload = {
      used: true,
      startedAt: started.toISOString(),
      expiresAt: expires.toISOString()
    }
    const payload: ActivationPayload = {
      token: `TRIAL-${crypto.randomUUID()}`,
      labName: '本地测试试用',
      labId: null,
      quota: TRIAL_DEVICE_LIMIT,
      activatedAt: started.toISOString(),
      mode: 'trial',
      trialStartedAt: lockPayload.startedAt,
      trialExpiresAt: lockPayload.expiresAt
    }

    trialUsed.value = true
    applyPayload(payload)
    writeTrialLock(lockPayload)
    writeActivation(payload)
    return true
  }

  function resetActivation() {
    token.value = ''
    labName.value = ''
    labId.value = null
    quota.value = 0
    activatedAt.value = ''
    macAddress.value = ''
    mode.value = 'licensed'
    clearActivation()

    const savedLock = readTrialLock()
    if (savedLock) {
      trialUsed.value = true
      trialStartedAt.value = savedLock.startedAt
      trialExpiresAt.value = savedLock.expiresAt
    }
    refreshLicenseState()
  }

  return {
    token,
    labName,
    labId,
    quota,
    activatedAt,
    macAddress,
    mode,
    trialStartedAt,
    trialExpiresAt,
    trialUsed,
    isActivated,
    isLicensed,
    isTrial,
    isTrialActive,
    isTrialExpired,
    isUsable,
    canStartTrial,
    trialRemainingDays,
    maskedToken,
    statusText,
    hydrate,
    refreshLicenseState,
    verifyStoredActivation,
    clearInvalidActivation,
    activate,
    startTrial,
    resetActivation
  }
})
