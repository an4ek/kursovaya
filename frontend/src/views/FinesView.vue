<template>
  <div class="page">
    <div class="page-header">
      <h2>Штрафы</h2>
    </div>

    <div v-if="loading" class="loading">Загрузка...</div>
    <div v-else-if="fines.length === 0" class="empty">Штрафов пока нет</div>
    <div v-else class="fines-list">
      <div v-for="fine in fines" :key="fine.id" class="fine-card">
        <div class="fine-info">
          <h3>{{ fine.readerName }}</h3>
          <p class="reason">Причина: {{ reasonText(fine.reason) }}</p>
          <p class="amount">Сумма: {{ Array.isArray(fine.amount) ? fine.amount[1] : fine.amount }} руб.</p>
          <p class="date">Создан: {{ formatDate(fine.createdAt) }}</p>
        </div>
        <div class="fine-actions">
          <span class="status-badge" :class="fine.status?.toLowerCase()">{{ statusText(fine.status) }}</span>
          <button v-if="fine.status === 'PENDING'" class="btn-pay" @click="payFine(fine.id)">Оплачен</button>
          <button v-if="fine.status === 'PENDING'" class="btn-waive" @click="waiveFine(fine.id)">Списать</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../api/axios'

const fines = ref<any[]>([])
const loading = ref(false)

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

async function loadFines() {
  loading.value = true
  try {
    const res = await api.get('/fines')
    fines.value = parseList(res.data).map((f: any) => ({
      ...f,
      readerName: f.loan?.reader?.fullName ?? f.readerName ?? ''
    }))
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function payFine(id: string) {
  try {
    await api.patch(`/fines/${id}/pay`)
    await loadFines()
  } catch (e) { console.error(e) }
}

async function waiveFine(id: string) {
  try {
    await api.patch(`/fines/${id}/waive`)
    await loadFines()
  } catch (e) { console.error(e) }
}

function reasonText(r: string) {
  if (r === 'OVERDUE') return 'Просрочка'
  if (r === 'DAMAGE') return 'Повреждение'
  if (r === 'LOSS') return 'Утеря'
  return r
}

function statusText(s: string) {
  if (s === 'PENDING') return 'Не оплачен'
  if (s === 'PAID') return 'Оплачен'
  if (s === 'WAIVED') return 'Списан'
  return s
}

onMounted(loadFines)
</script>

<style scoped>
.page { padding: 1.5rem; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.page-header h2 { font-size: 24px; font-weight: 700; }
.fines-list { display: flex; flex-direction: column; gap: 0.75rem; }
.fine-card { background: white; border-radius: 10px; padding: 1.2rem; box-shadow: 0 1px 4px rgba(0,0,0,0.08); display: flex; justify-content: space-between; align-items: center; }
.fine-card h3 { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.reason, .date { color: #666; font-size: 13px; margin-bottom: 2px; }
.amount { color: #e53e3e; font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.fine-actions { display: flex; align-items: center; gap: 0.75rem; }
.status-badge { display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 12px; background: #fff3cd; color: #856404; }
.status-badge.paid { background: #f0fff4; color: #276749; }
.status-badge.waived { background: #f0f0f0; color: #666; }
.btn-pay { background: #48bb78; color: white; border: none; padding: 6px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.btn-pay:hover { background: #38a169; }
.btn-waive { background: #a0aec0; color: white; border: none; padding: 6px 14px; border-radius: 8px; cursor: pointer; font-size: 13px; }
.btn-waive:hover { background: #718096; }
.loading, .empty { text-align: center; color: #999; margin-top: 3rem; }
</style>
