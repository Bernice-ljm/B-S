import { createRouter, createWebHistory } from 'vue-router'

import DevelopmentStatusView from '@/views/DevelopmentStatusView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'development-status',
      component: DevelopmentStatusView,
    },
  ],
})

export default router
