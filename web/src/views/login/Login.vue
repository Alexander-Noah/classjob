<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRememberedLogin, loginByPassword, persistRememberedLogin } from '../../api/services/auth'
import {
  AUTH_ROLE_DEFAULT_PATH,
  DEFAULT_AUTH_ROLE,
} from '../../constants/auth'

defineOptions({
  name: 'Login',
})

const rememberedLogin = getRememberedLogin()

const form = reactive({
  username: rememberedLogin.username,
  password: '',
  remember: rememberedLogin.remember,
})

const errors = reactive({
  username: '',
  password: '',
})

const isSubmitting = ref(false)
const showPassword = ref(false)
const router = useRouter()

const platformFeatures = [
  '实验室设备统一管理',
  '多实验室协同调度',
  '数据可视化分析',
  '安全监控与预警',
]

const metrics = [
  { value: '24/7', label: '安全监测' },
  { value: '98%', label: '设备在线率' },
  { value: '12+', label: '协同场景' },
]

const passwordInputType = computed(() => (showPassword.value ? 'text' : 'password'))

const validateField = (field) => {
  if (field === 'username') {
    errors.username = form.username.trim() ? '' : '请输入用户名'
  }

  if (field === 'password') {
    errors.password = form.password ? '' : '请输入密码'
  }
}

const validateForm = () => {
  validateField('username')
  validateField('password')
  return !errors.username && !errors.password
}

const getRedirectPath = (role = DEFAULT_AUTH_ROLE) => {
  return AUTH_ROLE_DEFAULT_PATH[role] || AUTH_ROLE_DEFAULT_PATH[DEFAULT_AUTH_ROLE]
}

const handleLogin = async () => {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    const username = form.username.trim()
    const session = await loginByPassword({
      username,
      password: form.password,
      remember: form.remember,
    })

    persistRememberedLogin({
      username,
      remember: form.remember,
    })

    isSubmitting.value = false
    ElMessage.success('登录成功')
    router.replace(getRedirectPath(session.role))
  } catch (error) {
    isSubmitting.value = false
    if (!error?.__handled) {
      ElMessage.error(error?.message || '登录失败')
    }
  }
}
</script>

<template>
  <main class="login-page">
    <div class="tech-grid" aria-hidden="true"></div>
    <div class="data-streams" aria-hidden="true">
      <span class="stream stream-a"></span>
      <span class="stream stream-b"></span>
      <span class="stream stream-c"></span>
    </div>
    <div class="particle-layer" aria-hidden="true">
      <span v-for="item in 18" :key="item" class="particle"></span>
    </div>

    <section class="login-shell" aria-label="智云实验室登录">
      <div class="brand-panel">
        <div class="system-badge">
          <span class="badge-dot"></span>
          Smart Lab Data Platform
        </div>

        <div class="brand-heading">
          <span class="eyebrow">智慧实验室数字化平台</span>
          <h1>智云实验室</h1>
          <p class="subtitle">基于数据驱动的实验室协同管控平台</p>
        </div>

        <p class="intro">
          面向实验室设备、人员、空间与安全事件的统一管控入口，构建可感知、可协同、可预警的智能化运营体系。
        </p>

        <div class="feature-grid" aria-label="平台能力">
          <div v-for="feature in platformFeatures" :key="feature" class="feature-item">
            <span class="feature-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <path d="M5 12.6 9.2 16.8 19 7" />
              </svg>
            </span>
            <span>{{ feature }}</span>
          </div>
        </div>

        <div class="metrics-row" aria-label="平台运行指标">
          <div v-for="metric in metrics" :key="metric.label" class="metric-card">
            <strong>{{ metric.value }}</strong>
            <span>{{ metric.label }}</span>
          </div>
        </div>
      </div>

      <form class="login-card" novalidate @submit.prevent="handleLogin">
        <div class="card-header">
          <div>
            <span class="card-kicker">Secure Access</span>
            <h2>平台登录</h2>
          </div>
          <div class="status-chip">
            <span></span>
            安全通道
          </div>
        </div>

        <label class="field-group" :class="{ invalid: errors.username }" for="username">
          <span class="field-label">用户名</span>
          <span class="input-wrap">
            <svg class="field-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M20 21a8 8 0 0 0-16 0" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input
              id="username"
              v-model.trim="form.username"
              autocomplete="username"
              placeholder="请输入用户名"
              type="text"
              @blur="validateField('username')"
              @input="errors.username && validateField('username')"
            />
          </span>
          <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
        </label>

        <label class="field-group" :class="{ invalid: errors.password }" for="password">
          <span class="field-label">密码</span>
          <span class="input-wrap">
            <svg class="field-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <rect x="4" y="10" width="16" height="10" rx="2" />
              <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            </svg>
            <input
              id="password"
              v-model="form.password"
              autocomplete="current-password"
              placeholder="请输入密码"
              :type="passwordInputType"
              @blur="validateField('password')"
              @input="errors.password && validateField('password')"
            />
            <button
              class="ghost-icon"
              type="button"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M3 3l18 18" />
                <path d="M10.7 10.7a2 2 0 0 0 2.6 2.6" />
                <path d="M7.4 7.6C5.5 8.7 4 10.2 3 12c2.1 3.6 5.1 5.5 9 5.5 1.4 0 2.7-.3 3.8-.8" />
                <path d="M18.2 14.5A11 11 0 0 0 21 12c-2.1-3.6-5.1-5.5-9-5.5-.6 0-1.2 0-1.8.1" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M3 12c2.1-3.6 5.1-5.5 9-5.5s6.9 1.9 9 5.5c-2.1 3.6-5.1 5.5-9 5.5S5.1 15.6 3 12Z" />
                <circle cx="12" cy="12" r="2.5" />
              </svg>
            </button>
          </span>
          <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
        </label>

        <div class="form-tools">
          <label class="remember-me">
            <input v-model="form.remember" type="checkbox" />
            <span class="checkmark"></span>
            <span>记住我</span>
          </label>
          <a class="forgot-link" href="#">忘记密码？</a>
        </div>

        <button class="login-button" type="submit" :disabled="isSubmitting">
          <span>{{ isSubmitting ? '登录中...' : '登录平台' }}</span>
          <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M5 12h14" />
            <path d="m13 6 6 6-6 6" />
          </svg>
        </button>

        <p class="security-note">
          已启用身份校验、访问审计与安全会话保护
        </p>
      </form>
    </section>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  min-width: 320px;
  background: #050914;
}

:global(#app) {
  width: 100%;
  max-width: none;
  min-height: 100svh;
  margin: 0;
  border: 0;
  text-align: initial;
}

.login-page {
  position: relative;
  display: flex;
  min-height: 100svh;
  overflow: hidden;
  color: #eff7ff;
  background:
    radial-gradient(circle at 18% 18%, rgba(23, 106, 213, 0.32), transparent 34%),
    radial-gradient(circle at 86% 26%, rgba(113, 75, 255, 0.28), transparent 32%),
    linear-gradient(135deg, #040815 0%, #071529 44%, #0b1232 100%);
  font-family:
    Inter, 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', system-ui, sans-serif;
  isolation: isolate;
}

.login-page::before,
.login-page::after {
  position: absolute;
  inset: 0;
  z-index: -2;
  pointer-events: none;
  content: '';
}

.login-page::before {
  background:
    linear-gradient(115deg, transparent 0 42%, rgba(32, 229, 255, 0.08) 44%, transparent 47%),
    linear-gradient(65deg, transparent 0 58%, rgba(126, 86, 255, 0.1) 60%, transparent 63%);
  animation: beamShift 12s ease-in-out infinite alternate;
}

.login-page::after {
  background:
    repeating-linear-gradient(
      90deg,
      rgba(112, 219, 255, 0.08) 0,
      rgba(112, 219, 255, 0.08) 1px,
      transparent 1px,
      transparent 88px
    ),
    repeating-linear-gradient(
      0deg,
      rgba(112, 219, 255, 0.06) 0,
      rgba(112, 219, 255, 0.06) 1px,
      transparent 1px,
      transparent 88px
    );
  mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 82%, transparent 100%);
  opacity: 0.62;
}

.tech-grid {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    linear-gradient(rgba(18, 246, 255, 0.13), transparent 1px),
    linear-gradient(90deg, rgba(18, 246, 255, 0.13), transparent 1px);
  background-size: 44px 44px;
  transform: perspective(700px) rotateX(62deg) translateY(18%);
  transform-origin: bottom center;
  opacity: 0.22;
}

.data-streams {
  position: absolute;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
}

.stream {
  position: absolute;
  width: min(44vw, 540px);
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(76, 222, 255, 0.95), transparent);
  box-shadow: 0 0 18px rgba(76, 222, 255, 0.6);
  opacity: 0.6;
  animation: streamMove 8s linear infinite;
}

.stream-a {
  top: 16%;
  left: -24%;
}

.stream-b {
  top: 56%;
  left: -36%;
  width: min(36vw, 420px);
  animation-delay: -2.6s;
}

.stream-c {
  top: 77%;
  left: -28%;
  width: min(50vw, 620px);
  animation-delay: -5.2s;
}

.particle-layer {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 999px;
  background: rgba(150, 236, 255, 0.78);
  box-shadow: 0 0 10px rgba(92, 225, 255, 0.78);
  animation: particleFloat 7s ease-in-out infinite;
}

.particle:nth-child(1) { top: 12%; left: 8%; animation-delay: -0.4s; }
.particle:nth-child(2) { top: 22%; left: 32%; animation-delay: -1.8s; }
.particle:nth-child(3) { top: 15%; left: 71%; animation-delay: -3.1s; }
.particle:nth-child(4) { top: 37%; left: 18%; animation-delay: -5s; }
.particle:nth-child(5) { top: 42%; left: 82%; animation-delay: -2.3s; }
.particle:nth-child(6) { top: 60%; left: 9%; animation-delay: -4.6s; }
.particle:nth-child(7) { top: 68%; left: 47%; animation-delay: -0.9s; }
.particle:nth-child(8) { top: 78%; left: 76%; animation-delay: -2.9s; }
.particle:nth-child(9) { top: 89%; left: 22%; animation-delay: -6s; }
.particle:nth-child(10) { top: 28%; left: 91%; animation-delay: -1.2s; }
.particle:nth-child(11) { top: 52%; left: 64%; animation-delay: -3.8s; }
.particle:nth-child(12) { top: 84%; left: 55%; animation-delay: -5.7s; }
.particle:nth-child(13) { top: 8%; left: 52%; animation-delay: -2s; }
.particle:nth-child(14) { top: 34%; left: 43%; animation-delay: -4.1s; }
.particle:nth-child(15) { top: 72%; left: 31%; animation-delay: -6.4s; }
.particle:nth-child(16) { top: 46%; left: 6%; animation-delay: -1.4s; }
.particle:nth-child(17) { top: 62%; left: 91%; animation-delay: -4.8s; }
.particle:nth-child(18) { top: 92%; left: 84%; animation-delay: -3.4s; }

.login-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 440px);
  gap: clamp(32px, 5vw, 84px);
  align-items: center;
  width: min(1180px, calc(100% - 48px));
  margin: auto;
  padding: 56px 0;
}

.brand-panel {
  max-width: 680px;
}

.system-badge {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  color: #9fecff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
  text-transform: uppercase;
  background: rgba(13, 43, 79, 0.46);
  border: 1px solid rgba(90, 221, 255, 0.28);
  border-radius: 8px;
  box-shadow: 0 0 26px rgba(44, 197, 255, 0.12);
  backdrop-filter: blur(14px);
}

.badge-dot {
  width: 8px;
  height: 8px;
  background: #39f2d5;
  border-radius: 50%;
  box-shadow: 0 0 14px #39f2d5;
}

.brand-heading {
  margin-top: 34px;
}

.eyebrow {
  display: block;
  margin-bottom: 14px;
  color: #6ee7ff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0;
}

.brand-heading h1 {
  margin: 0;
  color: #f5fbff;
  font-size: clamp(46px, 7vw, 86px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
  text-shadow: 0 0 34px rgba(59, 193, 255, 0.36);
}

.subtitle {
  width: fit-content;
  max-width: 100%;
  margin: 18px 0 0;
  color: #d5edff;
  font-size: clamp(18px, 2.2vw, 26px);
  font-weight: 500;
  line-height: 1.45;
  letter-spacing: 0;
}

.intro {
  max-width: 620px;
  margin: 26px 0 0;
  color: rgba(219, 240, 255, 0.76);
  font-size: 16px;
  line-height: 1.9;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 610px;
  margin-top: 34px;
}

.feature-item {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 52px;
  padding: 0 16px;
  color: rgba(238, 249, 255, 0.88);
  font-size: 15px;
  background: linear-gradient(135deg, rgba(7, 37, 70, 0.58), rgba(18, 26, 66, 0.4));
  border: 1px solid rgba(100, 218, 255, 0.18);
  border-radius: 8px;
  backdrop-filter: blur(16px);
}

.feature-icon {
  display: inline-grid;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  place-items: center;
  color: #54f3e2;
  background: rgba(84, 243, 226, 0.1);
  border: 1px solid rgba(84, 243, 226, 0.24);
  border-radius: 6px;
}

.feature-icon svg {
  width: 16px;
  height: 16px;
}

.feature-icon path {
  fill: none;
  stroke: currentColor;
  stroke-width: 2.3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.metrics-row {
  display: flex;
  gap: 14px;
  max-width: 610px;
  margin-top: 18px;
}

.metric-card {
  flex: 1 1 0;
  min-height: 88px;
  padding: 16px;
  background: rgba(4, 15, 34, 0.44);
  border: 1px solid rgba(133, 180, 255, 0.18);
  border-radius: 8px;
}

.metric-card strong {
  display: block;
  color: #ffffff;
  font-size: 25px;
  line-height: 1.1;
}

.metric-card span {
  display: block;
  margin-top: 8px;
  color: rgba(204, 230, 255, 0.68);
  font-size: 13px;
}

.login-card {
  position: relative;
  width: 100%;
  padding: 34px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(12, 34, 70, 0.74), rgba(22, 20, 61, 0.58)),
    rgba(8, 16, 35, 0.62);
  border: 1px solid rgba(139, 219, 255, 0.24);
  border-radius: 8px;
  box-shadow:
    0 28px 80px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(24px);
}

.login-card::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background:
    linear-gradient(90deg, transparent, rgba(91, 222, 255, 0.18), transparent),
    linear-gradient(rgba(255, 255, 255, 0.04), transparent 28%);
  transform: translateX(-100%);
  animation: cardScan 7s ease-in-out infinite;
}

.card-header {
  position: relative;
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 30px;
}

.card-kicker {
  display: block;
  margin-bottom: 8px;
  color: #70e7ff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.card-header h2 {
  margin: 0;
  color: #ffffff;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.status-chip {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  color: rgba(226, 244, 255, 0.82);
  font-size: 12px;
  background: rgba(57, 242, 213, 0.08);
  border: 1px solid rgba(57, 242, 213, 0.2);
  border-radius: 8px;
}

.status-chip span {
  width: 7px;
  height: 7px;
  background: #39f2d5;
  border-radius: 50%;
  box-shadow: 0 0 12px rgba(57, 242, 213, 0.9);
}

.field-group {
  position: relative;
  display: block;
  margin-bottom: 20px;
}

.field-label {
  display: block;
  margin-bottom: 9px;
  color: rgba(233, 246, 255, 0.88);
  font-size: 14px;
  font-weight: 600;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 52px;
  background: rgba(5, 18, 39, 0.74);
  border: 1px solid rgba(128, 200, 255, 0.22);
  border-radius: 8px;
  transition:
    border-color 0.22s ease,
    box-shadow 0.22s ease,
    background 0.22s ease;
}

.input-wrap:focus-within {
  background: rgba(6, 24, 54, 0.88);
  border-color: rgba(82, 228, 255, 0.84);
  box-shadow:
    0 0 0 3px rgba(82, 228, 255, 0.11),
    0 0 24px rgba(82, 228, 255, 0.18);
}

.field-group.invalid .input-wrap {
  border-color: rgba(255, 113, 139, 0.82);
  box-shadow: 0 0 0 3px rgba(255, 113, 139, 0.1);
}

.field-icon {
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  margin-left: 16px;
  color: rgba(113, 231, 255, 0.86);
}

.field-icon path,
.field-icon rect,
.field-icon circle,
.ghost-icon path,
.ghost-icon circle,
.login-button path {
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.input-wrap input {
  width: 100%;
  min-width: 0;
  height: 50px;
  padding: 0 14px;
  color: #f4fbff;
  font: inherit;
  font-size: 15px;
  line-height: 50px;
  background: transparent;
  border: 0;
  outline: 0;
}

.input-wrap input::placeholder {
  color: rgba(183, 211, 234, 0.42);
}

.ghost-icon {
  display: inline-grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  margin-right: 7px;
  padding: 0;
  place-items: center;
  color: rgba(190, 226, 250, 0.72);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 8px;
  transition:
    color 0.2s ease,
    background 0.2s ease;
}

.ghost-icon:hover {
  color: #6ee7ff;
  background: rgba(110, 231, 255, 0.08);
}

.ghost-icon:focus-visible {
  outline: 2px solid rgba(110, 231, 255, 0.7);
  outline-offset: 2px;
}

.ghost-icon svg {
  width: 20px;
  height: 20px;
}

.error-text {
  display: block;
  min-height: 18px;
  margin-top: 7px;
  color: #ff8da1;
  font-size: 12px;
}

.form-tools {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 24px;
}

.remember-me {
  display: inline-flex;
  gap: 9px;
  align-items: center;
  color: rgba(222, 240, 255, 0.78);
  font-size: 14px;
  cursor: pointer;
}

.remember-me input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}

.checkmark {
  position: relative;
  display: inline-block;
  flex: 0 0 18px;
  width: 18px;
  height: 18px;
  background: rgba(6, 24, 54, 0.9);
  border: 1px solid rgba(119, 207, 255, 0.42);
  border-radius: 5px;
  transition:
    background 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.checkmark::after {
  position: absolute;
  top: 3px;
  left: 6px;
  width: 4px;
  height: 8px;
  content: '';
  border: solid #051423;
  border-width: 0 2px 2px 0;
  opacity: 0;
  transform: rotate(45deg);
}

.remember-me input:checked + .checkmark {
  background: linear-gradient(135deg, #48f4dd, #4eb8ff);
  border-color: transparent;
  box-shadow: 0 0 14px rgba(72, 244, 221, 0.35);
}

.remember-me input:checked + .checkmark::after {
  opacity: 1;
}

.remember-me input:focus-visible + .checkmark {
  outline: 2px solid rgba(110, 231, 255, 0.7);
  outline-offset: 3px;
}

.forgot-link {
  color: #7ce8ff;
  font-size: 14px;
  text-decoration: none;
  transition:
    color 0.2s ease,
    text-shadow 0.2s ease;
}

.forgot-link:hover {
  color: #ffffff;
  text-shadow: 0 0 14px rgba(124, 232, 255, 0.52);
}

.login-button {
  position: relative;
  display: inline-flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 54px;
  padding: 0 22px;
  overflow: hidden;
  color: #061226;
  font: inherit;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(135deg, #55f1e2 0%, #4cb8ff 48%, #8c73ff 100%);
  border: 0;
  border-radius: 8px;
  box-shadow: 0 16px 34px rgba(48, 185, 255, 0.28);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;
}

.login-button::before {
  position: absolute;
  inset: 0;
  content: '';
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.45), transparent);
  transform: translateX(-100%);
  transition: transform 0.45s ease;
}

.login-button:hover {
  box-shadow:
    0 18px 38px rgba(48, 185, 255, 0.34),
    0 0 28px rgba(94, 236, 255, 0.22);
  filter: saturate(1.08);
  transform: translateY(-2px);
}

.login-button:hover::before {
  transform: translateX(100%);
}

.login-button:active {
  transform: translateY(0);
}

.login-button:disabled {
  cursor: wait;
  filter: grayscale(0.18) brightness(0.92);
  transform: none;
}

.login-button svg {
  position: relative;
  width: 20px;
  height: 20px;
}

.login-button span {
  position: relative;
}

.security-note {
  margin: 18px 0 0;
  color: rgba(201, 224, 244, 0.58);
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

@keyframes streamMove {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(150vw);
  }
}

@keyframes particleFloat {
  0%,
  100% {
    opacity: 0.24;
    transform: translate3d(0, 0, 0);
  }
  50% {
    opacity: 0.9;
    transform: translate3d(12px, -18px, 0);
  }
}

@keyframes beamShift {
  from {
    transform: translate3d(-2%, 0, 0);
  }
  to {
    transform: translate3d(2%, 0, 0);
  }
}

@keyframes cardScan {
  0%,
  58% {
    transform: translateX(-120%);
  }
  78%,
  100% {
    transform: translateX(120%);
  }
}

@media (max-width: 980px) {
  .login-shell {
    grid-template-columns: 1fr;
    gap: 34px;
    width: min(720px, calc(100% - 36px));
    padding: 38px 0;
  }

  .brand-panel {
    max-width: none;
  }

  .brand-heading {
    margin-top: 28px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .metrics-row {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .login-page {
    overflow-y: auto;
  }

  .login-shell {
    width: min(100% - 28px, 520px);
    padding: 28px 0;
  }

  .system-badge {
    min-height: 34px;
    font-size: 12px;
  }

  .intro {
    font-size: 15px;
    line-height: 1.75;
  }

  .metrics-row {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 74px;
  }

  .login-card {
    padding: 24px 20px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    margin-bottom: 24px;
  }

  .card-header h2 {
    font-size: 24px;
  }

  .form-tools {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
