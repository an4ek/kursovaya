//страница каталога книг. Библиотекарь может добавлять книги и экземпляры, все могут просматривать список.
<template>
  <div>
    <div class="page-header">
      <h2>Книги</h2>
      <button v-if="auth.isLibrarian" @click="showForm = true" class="btn-primary">+ Добавить книгу</button>
    </div>

    <!-- Форма добавления -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <h3>Новая книга</h3>
        <div class="field">
          <label>Название</label>
          <input v-model="form.title" placeholder="Введите название" />
        </div>
        <div class="field">
          <label>Автор</label>
          <input v-model="form.author" placeholder="Введите автора" />
        </div>
        <div class="field">
          <label>ISBN</label>
          <input v-model="form.isbn" placeholder="Введите ISBN" />
        </div>
        <div class="field">
          <label>Год издания</label>
          <input v-model="form.publishYear" type="number" placeholder="2024" />
        </div>
        <div class="field">
          <label>Количество экземпляров</label>
          <input v-model="form.copies" type="number" placeholder="1" />
        </div>
        <div class="modal-actions">
          <button @click="showForm = false" class="btn-secondary">Отмена</button>
          <button @click="addBook" class="btn-primary">Добавить</button>
        </div>
      </div>
    </div>

    <!-- Список книг -->
    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="books.length === 0" class="empty">Книг пока нет</div>
    <div v-else class="books-grid">
      <div v-for="book in books" :key="book.id" class="book-card">
        <h3>{{ book.title }}</h3>
        <p class="author">{{ book.author }}</p>
        <p class="isbn">ISBN: {{ book.isbn }}</p>
        <p class="year">{{ book.year }} г.</p>
        <div class="copies-badge">Экз: {{ book.availableCopies }} / {{ book.totalCopies }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const auth = useAuthStore()
const books = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const form = ref({ title: '', author: '', isbn: '', publishYear: 2024, copies: 1 })

async function loadBooks() {
  loading.value = true
  try {
    const res = await api.get('/books')
    let data = res.data
    if (typeof data === 'string') data = JSON.parse(data)
    if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') {
      books.value = data[1]
    } else if (Array.isArray(data)) {
      books.value = data
    } else {
      books.value = data.content ?? []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function addBook() {
  try {
    const bookRes = await api.post('/books', {
      title: form.value.title,
      author: form.value.author,
      isbn: form.value.isbn || null,
      year: form.value.publishYear
    })
    const bookId = bookRes.data.id
    for (let i = 1; i <= form.value.copies; i++) {
      await api.post(`/books/${bookId}/copies`, {
        inventoryNumber: `${bookId}-${i}`,
        condition: 'GOOD'
      })
    }
    showForm.value = false
    form.value = { title: '', author: '', isbn: '', publishYear: 2024, copies: 1 }
    await loadBooks()
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadBooks)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { font-size: 24px; }
.books-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.book-card { background: white; border-radius: 10px; padding: 1.2rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.book-card h3 { font-size: 16px; margin-bottom: 0.4rem; color: #222; }
.author { color: #555; font-size: 14px; margin-bottom: 0.3rem; }
.isbn { color: #888; font-size: 12px; }
.year { color: #888; font-size: 12px; margin-bottom: 0.5rem; }
.copies-badge { display: inline-block; background: #e8f4fd; color: #2b6cb0; padding: 3px 10px; border-radius: 20px; font-size: 13px; }
.loading, .empty { text-align: center; color: #888; padding: 3rem; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: white; border-radius: 12px; padding: 2rem; width: 420px; }
.modal h3 { margin-bottom: 1.2rem; font-size: 18px; }
.field { margin-bottom: 1rem; }
.field label { display: block; margin-bottom: 4px; color: #666; font-size: 14px; }
.field input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 0.8rem; justify-content: flex-end; margin-top: 1.2rem; }
.btn-primary { padding: 10px 20px; background: #4f6ef7; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-secondary { padding: 10px 20px; background: #f0f2f5; color: #333; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
</style>
