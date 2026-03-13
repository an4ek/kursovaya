<template>
  <div class="login-container">
    <div class="login-card">
      <h1>Библиотека</h1>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label>Логин</label>
          <input v-model="form.login" type="text" placeholder="Введите логин" required />
        </div>
        <div class="field">
          <label>Пароль</label>
          <input v-model="form.password" type="password" placeholder="Введите пароль" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'Вход...' : 'Войти' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = ref({ login: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.loginUser(form.value)
    router.push('/books')
  } catch {
    error.value = 'Неверный логин или пароль'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  background: white;
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  width: 360px;
}
h1 { text-align: center; margin-bottom: 1.5rem; color: #333; }
.field { margin-bottom: 1rem; }
label { display: block; margin-bottom: 4px; color: #666; font-size: 14px; }
input {
  width: 100%; padding: 10px; border: 1px solid #ddd;
  border-radius: 8px; font-size: 14px; box-sizing: border-box;
}
button {
  width: 100%; padding: 12px; background: #4f6ef7;
  color: white; border: none; border-radius: 8px;
  font-size: 16px; cursor: pointer; margin-top: 0.5rem;
}
button:disabled { opacity: 0.6; }
.error { color: #e53e3e; font-size: 14px; text-align: center; }
</style>
