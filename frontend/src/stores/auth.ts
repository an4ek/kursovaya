//хранит токен и данные текущего пользователя (логин, роль). Именно отсюда приложение знает кто сейчас вошёл и библиотекарь это или читатель.
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'
export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const login = ref(localStorage.getItem('login') || '')
  const role = ref(localStorage.getItem('role') || '')
  const readerId = ref(localStorage.getItem('readerId') || '')
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
    if (response.data.readerId) {
      readerId.value = response.data.readerId
      localStorage.setItem('readerId', response.data.readerId)
    }
  }
  function logout() {
    accessToken.value = ''
    login.value = ''
    role.value = ''
    readerId.value = ''
    localStorage.clear()
  }
  return { accessToken, login, role, readerId, isAuthenticated, isLibrarian, loginUser, logout }
})
