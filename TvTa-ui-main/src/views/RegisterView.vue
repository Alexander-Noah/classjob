<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const formRef = ref<FormInst | null>(null)
const formValue = ref({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'teacher' as 'admin' | 'teacher'
})

const roleOptions = [
  { label: '教师', value: 'teacher' },
  { label: '管理员', value: 'admin' }
]

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度需在 2-20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value) => value === formValue.value.password,
      message: '两次输入的密码不一致',
      trigger: ['blur', 'input']
    }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

function handleRegister() {
  formRef.value?.validate((errors) => {
    if (errors) return

    const success = authStore.register(
      formValue.value.username,
      formValue.value.password,
      formValue.value.role
    )

    if (success) {
      message.success('注册成功，请返回登录')
      formValue.value = {
        username: '',
        password: '',
        confirmPassword: '',
        role: 'teacher'
      }
      return
    }

    message.error('注册失败，用户名已存在')
  })
}
</script>

<template>
  <main class="account-shell">
    <section class="account-panel">
      <div class="account-aside">
        <div class="brand-lockup">
          <div class="brand-mark">
            <span class="i-carbon-user-multiple" />
          </div>
          <div>
            <div class="brand-title">账号开通</div>
            <div class="brand-subtitle">为教师或管理员创建本地访问账号</div>
          </div>
        </div>
        <div class="aside-note">
          <h1>保持账号权限清晰</h1>
          <p>注册页用于本地演示和课堂环境快速建账。正式部署时可对接学校统一身份认证。</p>
        </div>
      </div>

      <div class="account-content">
        <header class="account-header">
          <h2>注册账号</h2>
          <p>填写账号信息后即可在本机登录使用。</p>
        </header>

        <n-form ref="formRef" class="account-form" :model="formValue" :rules="rules" label-placement="top">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="formValue.username" size="large" placeholder="请输入用户名，2-20 个字符" />
          </n-form-item>
          <n-form-item label="密码" path="password">
            <n-input
              v-model:value="formValue.password"
              size="large"
              type="password"
              show-password-on="click"
              placeholder="请输入密码，至少 6 位"
            />
          </n-form-item>
          <n-form-item label="确认密码" path="confirmPassword">
            <n-input
              v-model:value="formValue.confirmPassword"
              size="large"
              type="password"
              show-password-on="click"
              placeholder="请再次输入密码"
            />
          </n-form-item>
          <n-form-item label="角色" path="role">
            <n-select
              v-model:value="formValue.role"
              :options="roleOptions"
              size="large"
              placeholder="请选择角色"
            />
          </n-form-item>
        </n-form>

        <footer class="account-actions">
          <n-button tertiary @click="router.push('/')">返回登录</n-button>
          <n-button tertiary @click="router.push('/batch-register')">批量注册</n-button>
          <n-button type="primary" @click="handleRegister">注册</n-button>
        </footer>
      </div>
    </section>
  </main>
</template>

<style scoped>
.account-shell {
  display: grid;
  width: 100vw;
  min-height: 100vh;
  place-items: center;
  box-sizing: border-box;
  padding: 28px;
  background: #eef3f8;
}

.account-panel {
  display: grid;
  width: min(920px, 100%);
  min-height: 560px;
  grid-template-columns: 340px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #dbe4ef;
  border-radius: 14px;
  background: #ffffff;
}

.account-aside {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 32px;
  background: #0f172a;
  color: #ffffff;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border-radius: 10px;
  background: #2563eb;
  color: #ffffff;
  font-size: 24px;
}

.brand-title {
  font-size: 18px;
  font-weight: 800;
}

.brand-subtitle {
  margin-top: 3px;
  color: #b6c3d1;
  font-size: 12px;
  line-height: 1.5;
}

.aside-note h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  text-wrap: balance;
}

.aside-note p {
  margin: 14px 0 0;
  color: #cbd5e1;
  font-size: 14px;
  line-height: 1.7;
}

.account-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 34px 38px;
}

.account-header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.account-header p {
  margin: 8px 0 22px;
  color: #64748b;
  font-size: 14px;
}

.account-form :deep(.n-form-item-label) {
  color: #111827;
  font-size: 14px;
  font-weight: 650;
}

.account-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 18px;
}

@media (max-width: 760px) {
  .account-panel {
    grid-template-columns: 1fr;
  }

  .account-aside {
    gap: 28px;
  }

  .account-content {
    padding: 28px;
  }
}
</style>
