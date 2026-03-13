import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/books', component: () => import('@/views/BooksView.vue'), meta: { auth: true } },
    { path: '/readers', component: () => import('@/views/ReadersView.vue'), meta: { auth: true, librarian: true } },
    { path: '/loans', component: () => import('@/views/LoansView.vue'), meta: { auth: true } },
    { path: '/fines', component: () => import('@/views/FinesView.vue'), meta: { auth: true } },
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isAuthenticated) return '/login'
  if (to.meta.guest && auth.isAuthenticated) return '/books'
  if (to.meta.librarian && !auth.isLibrarian) return '/books'
})

export default router
