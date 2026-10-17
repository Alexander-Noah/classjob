<!--
  Web 管理平台的Forbidden视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft, House, Lock } from '@element-plus/icons-vue'
import { getStoredRole } from '../../api/services/auth'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'

defineOptions({
  name: 'Forbidden',
})

const router = useRouter()

const handleGoHome = () => {
  const role = getStoredRole()
  router.replace(AUTH_ROLE_DEFAULT_PATH[role] || '/')
}

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  handleGoHome()
}
</script>

<template>
  <section class="forbidden-page">
    <div class="forbidden-card">
      <div class="forbidden-icon" aria-hidden="true">
        <el-icon>
          <Lock />
        </el-icon>
      </div>

      <p class="forbidden-kicker">403 Forbidden</p>
      <h1>无权访问</h1>
      <p class="forbidden-desc">当前账号没有查看该页面的权限，请联系系统管理员开通对应菜单授权。</p>

      <div class="forbidden-actions">
        <el-button type="primary" size="large" @click="handleGoHome">
          <el-icon>
            <House />
          </el-icon>
          返回工作台
        </el-button>
        <el-button size="large" @click="handleGoBack">
          <el-icon>
            <ArrowLeft />
          </el-icon>
          返回上一页
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.forbidden-page {
  display: grid;
  min-height: calc(100vh - 112px);
  place-items: center;
  padding: 32px 16px;
}

.forbidden-card {
  width: min(100%, 520px);
  padding: 40px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.forbidden-icon {
  display: inline-grid;
  width: 64px;
  height: 64px;
  margin-bottom: 18px;
  place-items: center;
  border-radius: 8px;
  background: #eef4ff;
  color: var(--primary-color);
  font-size: 30px;
}

.forbidden-kicker {
  margin: 0 0 8px;
  color: var(--primary-color);
  font-size: 13px;
  font-weight: 700;
}

.forbidden-card h1 {
  margin: 0;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 800;
  letter-spacing: 0;
}

.forbidden-desc {
  margin: 14px auto 0;
  max-width: 390px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.7;
}

.forbidden-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 28px;
}

@media (max-width: 600px) {
  .forbidden-card {
    padding: 30px 22px;
  }

  .forbidden-actions {
    flex-direction: column;
  }
}
</style>
