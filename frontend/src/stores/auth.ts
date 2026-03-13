import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const login = ref(localStorage.getItem('login') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isAuthenticated = computed(() => !!accessToken.value)
  const isLibrarian = computed(() => role.value === 'LIBRARIAN')

  async function loginUser(credentials: { login: string; password: string }) {
    const response = await api.post('/auth/login', credentials)
    accessToken.value = response.data.accessToken
    login.value = response.data.login
    role.value = response.data.role
    localStorage.setItem('accessToken', accessToken.value)
    localStorage.setItem('login', login.value)
    localStorage.setItem('role', role.value)
  }

  function logout() {
    accessToken.value = ''
    login.value = ''
    role.value = ''
    localStorage.clear()
  }

  return { accessToken, login, role, isAuthenticated, isLibrarian, loginUser, logout }
})
