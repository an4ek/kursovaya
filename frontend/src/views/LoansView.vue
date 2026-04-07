<template>
  <div class="page">
    <div class="page-header">
      <h2>Выдачи</h2>
      <button class="btn-primary" @click="openForm">+ Выдать книгу</button>
    </div>

    <div v-if="showForm" class="modal-overlay">
      <div class="modal">
        <h3>Выдача книги</h3>
        <div class="form-group">
          <label>Читатель</label>
          <select v-model="form.readerId">
            <option value="">-- Выберите читателя --</option>
            <option v-for="r in readers" :key="r.id" :value="r.id">{{ r.fullName }} (@{{ r.login }})</option>
          </select>
        </div>
        <div class="form-group">
          <label>Книга</label>
          <select v-model="form.bookId" @change="loadCopies">
            <option value="">-- Выберите книгу --</option>
            <option v-for="b in books" :key="b.id" :value="b.id">{{ b.title }} — {{ b.author }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>Экземпляр</label>
          <select v-model="form.copyId">
            <option value="">-- Выберите экземпляр --</option>
            <option v-for="c in copies" :key="c.id" :value="c.id">{{ c.inventoryNumber }} ({{ c.condition }})</option>
          </select>
        </div>
        <div class="form-group">
          <label>Срок возврата (дней)</label>
          <input v-model.number="form.days" type="number" min="1" max="60" />
        </div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showForm = false">Отмена</button>
          <button class="btn-primary" @click="issueLoan">Выдать</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="loans.length === 0" class="empty">Выдач пока нет</div>
    <div v-else class="loans-list">
      <div v-for="loan in loans" :key="loan.id" class="loan-card">
        <div class="loan-info">
          <h3>{{ loan.bookTitle }}</h3>
          <p class="reader">Читатель: {{ loan.readerName }}</p>
          <p class="dates">Выдано: {{ formatDate(loan.issuedAt) }} | Срок: {{ formatDate(loan.dueDate) }}</p>
        </div>
        <div class="loan-actions">
          <span class="status-badge" :class="loan.status?.toLowerCase()">{{ loan.status }}</span>
          <button v-if="loan.status === 'ACTIVE' || loan.status === 'OVERDUE'" class="btn-return" @click="returnLoan(loan.id)">Вернуть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const loans = ref<any[]>([])
const readers = ref<any[]>([])
const books = ref<any[]>([])
const copies = ref<any[]>([])
const loading = ref(false)
const showForm = ref(false)
const form = ref({ readerId: '', bookId: '', copyId: '', days: 14 })

function parseList(data: any) {
  if (typeof data === 'string') data = JSON.parse(data)
  if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') return data[1]
  if (Array.isArray(data)) return data
  return data.content ?? []
}

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ru-RU')
}

async function loadLoans() {
  loading.value = true
  try {
    const res = await api.get('/loans')
    loans.value = parseList(res.data).map((l: any) => ({
      ...l,
      bookTitle: l.bookCopy?.bookTitle?.title ?? '',
      readerName: l.reader?.fullName ?? ''
    }))
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function openForm() {
  showForm.value = true
  const [rRes, bRes] = await Promise.all([api.get('/readers'), api.get('/books')])
  readers.value = parseList(rRes.data)
  books.value = parseList(bRes.data)
}

async function loadCopies() {
  if (!form.value.bookId) return
  try {
    const res = await api.get(`/books/${form.value.bookId}/copies`)
    copies.value = parseList(res.data).filter((c: any) => c.status === 'AVAILABLE')
  } catch (e) { console.error(e) }
}

async function issueLoan() {
  try {
    const due = new Date()
    due.setDate(due.getDate() + form.value.days)
    const dueDate = due.toISOString().split('T')[0]
    await api.post('/loans/issue', {
      readerId: form.value.readerId,
      bookCopyId: form.value.copyId,
      dueDate: dueDate
    })
    showForm.value = false
    form.value = { readerId: '', bookId: '', copyId: '', days: 14 }
    await loadLoans()
  } catch (e) { console.error(e) }
}

async function returnLoan(id: string) {
  try {
    await api.post(`/loans/${id}/return`)
    await loadLoans()
  } catch (e) { console.error(e) }
}

onMounted(loadLoans)
</script>

<style scoped>
.page { padding: 1.5rem; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { font-size: 24px; font-weight: 700; }
.loans-list { display: flex; flex-direction: column; gap: 0.75rem; }
.loan-card { background: white; border-radius: 10px; padding: 1.2rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); display: flex; justify-content: space-between; align-items: center; }
.loan-card h3 { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.reader, .dates { color: #666; font-size: 13px; margin-bottom: 2px; }
.loan-actions { display: flex; align-items: center; gap: 0.75rem; }
.status-badge { display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 12px; background: #e8f4fd; color: #2b6cb0; }
.status-badge.overdue { background: #fff0f0; color: #e53e3e; }
.status-badge.returned { background: #f0fff4; color: #276749; }
.btn-return { background: #48bb78; color: white; border: none; padding: 6px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.btn-return:hover { background: #38a169; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal { background: white; border-radius: 12px; padding: 2rem; width: 420px; max-width: 90vw; }
.modal h3 { font-size: 18px; font-weight: 700; margin-bottom: 1.5rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; font-size: 13px; color: #555; margin-bottom: 4px; }
.form-group input, .form-group select { width: 100%; padding: 8px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
.modal-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
.btn-primary { background: #4f6ef7; color: white; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-primary:hover { background: #3a57d6; }
.btn-secondary { background: #f0f0f0; color: #333; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.loading, .empty { text-align: center; color: #999; margin-top: 3rem; }
</style>
