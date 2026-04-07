<template>
  <div class="page">
    <div class="page-header">
      <h2>Читатели</h2>
      <button class="btn-primary" @click="showForm = true">+ Добавить читателя</button>
    </div>

    <div v-if="showForm" class="modal-overlay">
      <div class="modal">
        <h3>Новый читатель</h3>
        <div class="form-group">
          <label>Логин</label>
          <input v-model="form.login" type="text" />
        </div>
        <div class="form-group">
          <label>Пароль</label>
          <input v-model="form.password" type="password" />
        </div>
        <div class="form-group">
          <label>Имя</label>
          <input v-model="form.firstName" type="text" />
        </div>
        <div class="form-group">
          <label>Фамилия</label>
          <input v-model="form.lastName" type="text" />
        </div>
        <div class="form-group">
          <label>Email</label>
          <input v-model="form.email" type="email" />
        </div>
        <div class="form-group">
          <label>Телефон</label>
          <input v-model="form.phone" type="text" />
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showForm = false">Отмена</button>
          <button class="btn-primary" @click="addReader">Добавить</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="readers.length === 0" class="empty">Читателей пока нет</div>
    <div v-else class="readers-grid">
      <div v-for="reader in readers" :key="reader.id" class="reader-card">
        <h3>{{ reader.fullName }}</h3>
        <p class="login">@{{ reader.login }}</p>
        <p v-if="reader.email" class="email">{{ reader.email }}</p>
        <p v-if="reader.phone" class="phone">{{ reader.phone }}</p>
        <span class="status-badge" :class="reader.status?.toLowerCase()">{{ reader.status }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const readers = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const form = ref({ login: '', password: '', firstName: '', lastName: '', email: '', phone: '' })

async function loadReaders() {
  loading.value = true
  try {
    const res = await api.get('/readers')
    let data = res.data
    if (typeof data === 'string') data = JSON.parse(data)
    if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') {
      readers.value = data[1]
    } else if (Array.isArray(data)) {
      readers.value = data
    } else {
      readers.value = data.content ?? []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function addReader() {
  try {
    await api.post('/auth/register', {
      login: form.value.login,
      password: form.value.password,
      fullName: form.value.firstName + ' ' + form.value.lastName,
      email: form.value.email || null,
      phone: form.value.phone || null,
    })
    showForm.value = false
    form.value = { login: '', password: '', firstName: '', lastName: '', email: '', phone: '' }
    await loadReaders()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadReaders)
</script>

<style scoped>
.page { padding: 1.5rem; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { font-size: 24px; font-weight: 700; }
.readers-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.reader-card { background: white; border-radius: 10px; padding: 1.2rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.reader-card h3 { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.login { color: #666; font-size: 13px; margin-bottom: 4px; }
.email, .phone { color: #444; font-size: 13px; margin-bottom: 4px; }
.status-badge { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 12px; background: #e8f4fd; color: #2b6cb0; }
.status-badge.blocked { background: #fff0f0; color: #e53e3e; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: white; border-radius: 12px; padding: 2rem; width: 400px; max-width: 90vw; }
.modal h3 { font-size: 18px; font-weight: 700; margin-bottom: 1.5rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 4px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
.btn-primary { background: #4f6ef7; color: white; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-primary:hover { background: #3a57d6; }
.btn-secondary { background: #f0f0f0; color: #333; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.loading, .empty { text-align: center; color: #999; margin-top: 3rem; }
</style>
