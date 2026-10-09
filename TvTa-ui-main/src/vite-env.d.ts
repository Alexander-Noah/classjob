/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TIMEOUT: string
  readonly VITE_GATEWAY_BASE_URL: string
  readonly VITE_LOCAL_PROBE_BASE_URL: string
  readonly VITE_BUSINESS_SYSTEM_URL: string
  readonly VITE_ENABLE_LOCAL_TRIAL: string
  readonly VITE_LOCAL_TRIAL_DAYS: string
  readonly VITE_LOCAL_TRIAL_DEVICE_LIMIT: string
  readonly VITE_DEFAULT_LOCALE: string
  // 更多环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
