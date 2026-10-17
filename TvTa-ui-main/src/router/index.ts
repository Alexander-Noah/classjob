/*
 * router index公共逻辑模块，供相关视图或服务复用。
 */
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useActivationStore } from '@/stores/activation'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: HomeView
    },
    {
      path: '/devices',
      name: 'devices',
      /**
       * component的处理逻辑。
       * @returns 由当前表达式或分支生成的处理结果。
       */
      component: () => import('../views/DevicesView.vue')
    },
    {
      path: '/logs',
      name: 'logs',
      /**
       * component的处理逻辑。
       * @returns 由当前表达式或分支生成的处理结果。
       */
      component: () => import('../views/LogsView.vue')
    },
    {
      path: '/gateway',
      name: 'gateway',
      /**
       * component的处理逻辑。
       * @returns 由当前表达式或分支生成的处理结果。
       */
      component: () => import('../views/GatewayView.vue')
    }
  ]
})

router.beforeEach(/** 处理 beforeEach 调用中的回调逻辑。 入参：to。 */ (to) => {
  const activationStore = useActivationStore()
  activationStore.hydrate()

  if (!activationStore.isUsable && to.path !== '/gateway') {
    return '/gateway'
  }

  return true
})

export default router
