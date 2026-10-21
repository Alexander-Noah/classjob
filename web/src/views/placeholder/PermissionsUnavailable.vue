<script setup>
import { useRoute, useRouter } from 'vue-router'
import { getStoredRole } from '../../api/services/auth'
import { AUTH_ROLE_DEFAULT_PATH } from '../../constants/auth'
const route = useRoute(), router = useRouter()
const home = () => AUTH_ROLE_DEFAULT_PATH[getStoredRole()] || '/login'
function retry() {
  const target = route.query.redirect
  router.replace(typeof target === 'string' && target.startsWith('/') && !target.startsWith('//')
    && !target.startsWith('/permissions-unavailable') ? target : home())
}
</script>

<template>
  <section class="access-unavailable" role="status">
    <h1>暂时无法读取页面权限</h1>
    <p>权限服务暂不可用，这不代表你的账号没有访问权限。请重试，或联系管理员检查服务状态。</p>
    <div>
      <el-button type="primary" @click="retry">重新加载权限</el-button>
      <el-button @click="router.replace(home())">返回工作台</el-button>
    </div>
  </section>
</template>

<style scoped>
.access-unavailable { max-width: 680px; margin: 12vh auto; padding: 40px; border-radius: 20px; background: #ffffffdf; }
h1 { font-size: 25px; margin-top: 0; }
p { line-height: 1.8; color: #64748b; margin-bottom: 28px; }
div { display: flex; gap: 12px; flex-wrap: wrap; }
</style>
