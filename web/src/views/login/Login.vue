<!--
  Web 管理平台的登录视图组件；模板、响应式状态与局部样式在本文件内协同维护。
  业务状态复用：../../composables/useLiquidGlass。
-->
<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLiquidGlass } from '../../composables/useLiquidGlass'
import { ElMessage } from 'element-plus'
import { User, Lock, Hide, View, Right } from '@element-plus/icons-vue'
import { getRememberedLogin, loginByPassword, persistRememberedLogin } from '../../api/services/auth'
import {
  AUTH_ROLE_DEFAULT_PATH,
  DEFAULT_AUTH_ROLE,
} from '../../constants/auth'

defineOptions({
  name: 'Login',
})

const { glassElement } = useLiquidGlass()
const activeTab = ref('login')
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
    <div class="scene-wash" aria-hidden="true"></div>
    <header class="brand-logo" aria-label="智云实验室 Smart Lab Management">
      <img src="/login-brand-transparent.png" alt="智云实验室 Smart Lab Management" />
    </header>
    <section class="brand-panel">
      <h1>智云实验室</h1>
      <p class="subtitle">让实验室管理更智能、更高效</p>
      <div class="brand-rule"></div>
      <p class="intro">数据驱动管理<br />协同创造更好的实验环境。</p>
    </section>
    <section ref="glassElement" class="login-card" aria-label="账户登录">
      <div class="liquid-glass" aria-hidden="true"></div>
      <div class="tabs" role="tablist" aria-label="账户操作">
        <button id="login-tab" type="button" role="tab" :aria-selected="activeTab === 'login'" :class="{ active: activeTab === 'login' }" @click="activeTab = 'login'">登录</button>
        <button id="register-tab" type="button" role="tab" :aria-selected="activeTab === 'register'" :class="{ active: activeTab === 'register' }" @click="activeTab = 'register'">注册</button>
      </div>
      <template v-if="activeTab === 'login'">
        <h2>欢迎登录</h2>
        <p class="card-subtitle">基于数据驱动的实验室协同管控平台</p>
        <form novalidate @submit.prevent="handleLogin">
          <div class="field-group">
            <label class="input-wrap" :class="{ invalid: errors.username }">
              <User class="field-icon" />
              <input v-model.trim="form.username" aria-label="账号" :aria-invalid="!!errors.username" autocomplete="username" placeholder="请输入账号" @blur="validateField('username')" @input="errors.username && validateField('username')" />
            </label>
            <span v-if="errors.username" class="error-text">{{ errors.username }}</span>
          </div>
          <div class="field-group">
            <label class="input-wrap" :class="{ invalid: errors.password }">
              <Lock class="field-icon" />
              <input v-model="form.password" aria-label="密码" :aria-invalid="!!errors.password" :type="passwordInputType" autocomplete="current-password" placeholder="请输入密码" @blur="validateField('password')" @input="errors.password && validateField('password')" />
              <button class="ghost-icon" type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><View v-if="showPassword" /><Hide v-else /></button>
            </label>
            <span v-if="errors.password" class="error-text">{{ errors.password }}</span>
          </div>
          <div class="form-tools">
            <label class="remember-me"><input v-model="form.remember" type="checkbox" /><span>记住我</span></label>
            <button class="forgot-link" type="button" @click="ElMessage.info('请联系系统管理员重置密码')">忘记密码?</button>
          </div>
          <button class="login-button" type="submit" :disabled="isSubmitting"><span>{{ isSubmitting ? '登录中...' : '登录并进入' }}</span><Right /></button>
        </form>
      </template>
      <div v-else class="registration" role="tabpanel" aria-labelledby="register-tab">
        <h2>账号注册</h2>
        <p class="card-subtitle">欢迎加入智云实验室</p>
        <p class="registration-copy">平台账号由系统管理员统一创建。<br />请联系管理员开通账号后登录。</p>
        <button class="login-button" type="button" @click="activeTab = 'login'">返回登录<Right /></button>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { position: relative; width: 100%; min-height: 100svh; overflow: hidden; color: #202c80; background: url('/login-background.png') center center / cover no-repeat; font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; }
.login-page * { box-sizing: border-box; }
.scene-wash { position: absolute; inset: 0; pointer-events: none; background: linear-gradient(90deg, rgba(255,255,255,.67), rgba(255,248,253,.39) 50%, rgba(255,255,255,.08) 75%); }
.brand-logo { position: absolute; left: 6.58vw; top: 6.05vh; width: 214px; height: 58px; overflow: hidden; }
.brand-logo img { display: block; width: 214px; height: 58px; object-fit: contain; object-position: left center; }
.brand-panel { position: absolute; left: 8.37vw; top: 19.4vh; }
h1 { margin: 0; font-family: 'STZhongsong', 'SimSun', serif; font-size: clamp(46px, 4.78vw, 96px); font-weight: 900; font-synthesis: weight; letter-spacing: 1px; line-height: 1.2; color: #263486; background: linear-gradient(125deg, #1e2d87 15%, #3b68b3 50%, #242773 78%, #d378b5); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.subtitle { margin: 19px 0 0; font-family: 'SimSun', serif; font-size: clamp(20px, 2.03vw, 40px); letter-spacing: 1px; white-space: nowrap; }
.brand-rule { width: 45px; height: 1px; background: #5e6ba1; margin-top: 24px; }
.intro { margin: 23px 0 0; font-family: 'SimSun', serif; color: #7377a6; font-size: 20px; line-height: 1.55; }
.login-card { position: relative; margin-left: 36.54%; margin-top: 19.77vh; width: 446px; min-height: 506px; padding: 34px 36px 42px; border: 1px solid rgba(255,255,255,.85); border-radius: 24px; background: transparent; box-shadow: 0 12px 36px rgba(106,78,125,.12); }
.liquid-glass { position: absolute; inset: 0; border-radius: inherit; pointer-events: none; background: rgba(255,250,255,.24); -webkit-backdrop-filter: var(--glass-backdrop, blur(12px)); backdrop-filter: var(--glass-backdrop, blur(12px)); box-shadow: inset 0 1px 1px rgba(255,255,255,.95), inset 1px 0 2px rgba(255,255,255,.65), inset 0 -8px 22px rgba(121,100,158,.12); }
.login-card > :not(.liquid-glass) { position: relative; }
.tabs { display: flex; justify-content: center; gap: 75px; width: 218px; margin: 0 auto; border-bottom: 1px solid rgba(124,116,163,.12); }
.tabs button { position: relative; padding: 0 10px 12px; border: 0; background: transparent; font: inherit; font-size: 21px; color: #5d6091; cursor: pointer; white-space: nowrap; }
.tabs button.active { color: #287aff; font-weight: 700; }
.tabs button.active::after { content: ''; position: absolute; height: 2px; background: #287aff; bottom: -1px; left: 0; right: 0; }
h2 { margin: 31px 0 8px; font-size: 28px; line-height: 1.4; font-weight: 700; text-align: center; color: #15216f; }
.card-subtitle { margin: 0 0 30px; text-align: center; white-space: nowrap; font-size: 18px; color: #61649a; }
.field-group { position: relative; margin-bottom: 16px; }
.input-wrap { display: flex; align-items: center; height: 56px; background: rgba(255,255,255,.84); border: 1px solid rgba(239,232,244,.9); border-radius: 14px; transition: box-shadow .2s; }
.input-wrap:focus-within { box-shadow: 0 0 0 3px #3682ff26; }
.input-wrap.invalid { border-color: #d85278; }
.field-icon { width: 22px; height: 22px; margin-left: 17px; flex-shrink: 0; }
.input-wrap input { width: 100%; min-width: 0; height: 100%; border: 0; outline: 0; padding: 0 18px; background: transparent; color: #252e79; font: inherit; font-size: 17px; }
.input-wrap input::placeholder { color: #9496bc; }
.ghost-icon { display: grid; place-items: center; flex-shrink: 0; width: 40px; height: 40px; padding: 10px; margin-right: 7px; border: 0; background: transparent; color: #303886; cursor: pointer; }
.ghost-icon svg { width: 20px; height: 20px; }
.error-text { font-size: 12px; color: #bd3860; display: block; margin: 3px 8px -10px; }
.form-tools { display: flex; align-items: center; justify-content: space-between; margin: 21px 0; font-size: 17px; }
.remember-me { display: flex; align-items: center; gap: 11px; cursor: pointer; }
.remember-me input { width: 23px; height: 23px; margin: 0 0 0 3px; accent-color: #2e7fff; cursor: pointer; }
.forgot-link { border: 0; padding: 0; background: transparent; font: inherit; color: #397fff; cursor: pointer; }
/* 此处只定义布局与文字尺寸；玻璃背景、边框和交互状态由 button-glass.css 统一提供。
   scoped 选择器优先级高于公共皮肤，不能在这里再次设置不透明渐变或白色文字。 */
.login-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  height: 59px;
  padding: 0;
  font-family: 'SimSun', serif;
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 0;
  cursor: pointer;
}
.login-button svg { width: 20px; height: 20px; opacity: .9; transition: transform .2s; }
.login-button:hover:not(:disabled) svg { transform: translateX(3px); }
@media (prefers-reduced-motion: reduce) { .login-button svg { transition: none; transform: none; } }
.login-button:disabled { cursor: wait; }
button:focus-visible { outline: 2px solid #287aff; outline-offset: 4px; }
.registration-copy { margin: 48px 0; text-align: center; font-size: 16px; line-height: 2; color: #61649a; }
@media (min-width: 1900px) { .login-card { margin-left: calc(50% - 223px); } }
@media (max-width: 1100px) { .brand-panel { left: 5%; } h1 { font-size: 48px; } .subtitle { font-size: 22px; } .login-card { margin-left: 46%; width: 420px; padding: 30px; } }
@media (max-width: 760px) { .login-page { min-height: 100svh; padding: 170px 20px 45px; background-position: 66% center; } .brand-logo { top: 30px; left: 24px; } .brand-panel { top: 110px; left: 24px; } h1 { font-size: 34px; } .subtitle,.brand-rule,.intro { display: none; } .login-card { width: min(100%,446px); margin: 0 auto; padding: 30px 25px 36px; min-height: auto; } .card-subtitle { font-size: 14px; } .scene-wash { background: rgba(255,248,253,.25); } }
</style>
