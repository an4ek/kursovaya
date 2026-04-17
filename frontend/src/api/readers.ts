//запросы для читателей: получить список, зарегистрировать нового.
import api from './axios'

export interface Reader {
  id: string
  login: string
  fullName: string
  email: string | null
  phone: string | null
  maxActiveLoans: number
  registeredAt: string
}

function parseList(data: any): any[] {
  if (typeof data === 'string') data = JSON.parse(data)
  if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') return data[1]
  if (Array.isArray(data)) return data
  return data.content ?? []
}

export const readersApi = {
  getAll: () => api.get('/readers').then(r => parseList(r.data) as Reader[]),
  register: (data: { login: string; password: string; fullName: string; email?: string; phone?: string }) =>
    api.post('/auth/register', data).then(r => r.data),
}
