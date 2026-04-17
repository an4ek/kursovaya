//запросы для штрафов: получить список, оплатить, списать.
import api from './axios'

export interface Fine {
  id: string
  loan: any
  reason: string
  amount: any
  status: string
  createdAt: string
  readerName?: string
}

function parseList(data: any): any[] {
  if (typeof data === 'string') data = JSON.parse(data)
  if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') return data[1]
  if (Array.isArray(data)) return data
  return data.content ?? []
}

export const finesApi = {
  getAll: () => api.get('/fines').then(r => parseList(r.data) as Fine[]),
  pay: (id: string) => api.patch(`/fines/${id}/pay`).then(r => r.data),
  waive: (id: string) => api.patch(`/fines/${id}/waive`).then(r => r.data),
}
