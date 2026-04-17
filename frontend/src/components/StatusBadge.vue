//цветной бейдж для отображения статуса. Например: зелёный «Доступна», красный «Просрочена», синий «Выдана». Используется на всех страницах.
<template>
  <span class="badge" :class="statusClass">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const statusMap: Record<string, { label: string; cls: string }> = {
  ACTIVE: { label: 'Активна', cls: 'active' },
  RETURNED: { label: 'Возвращена', cls: 'returned' },
  OVERDUE: { label: 'Просрочена', cls: 'overdue' },
  PENDING: { label: 'Не оплачен', cls: 'pending' },
  PAID: { label: 'Оплачен', cls: 'paid' },
  WAIVED: { label: 'Списан', cls: 'waived' },
  AVAILABLE: { label: 'Доступна', cls: 'available' },
  LOANED: { label: 'Выдана', cls: 'loaned' },
}

const label = computed(() => statusMap[props.status]?.label ?? props.status)
const statusClass = computed(() => statusMap[props.status]?.cls ?? '')
</script>

<style scoped>
.badge { display: inline-block; padding: 3px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; }
.active { background: #e8f4fd; color: #2b6cb0; }
.returned { background: #f0fff4; color: #276749; }
.overdue { background: #fff0f0; color: #e53e3e; }
.pending { background: #fff3cd; color: #856404; }
.paid { background: #f0fff4; color: #276749; }
.waived { background: #f0f0f0; color: #666; }
.available { background: #f0fff4; color: #276749; }
.loaned { background: #e8f4fd; color: #2b6cb0; }
</style>
