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
      component: () => import('../views/DevicesView.vue')
    },
    {
      path: '/logs',
      name: 'logs',
      component: () => import('../views/LogsView.vue')
    },
    {
      path: '/gateway',
      name: 'gateway',
      component: () => import('../views/GatewayView.vue')
    }
  ]
})

router.beforeEach((to) => {
  const activationStore = useActivationStore()
  activationStore.hydrate()

  if (!activationStore.isUsable && to.path !== '/gateway') {
    return '/gateway'
  }

  return true
})

export default router
