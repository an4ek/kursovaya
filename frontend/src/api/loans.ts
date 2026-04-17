// запросы для выдач: получить список, выдать книгу, принять возврат, получить просрочки.
import api from './axios'

export interface Loan {
  id: string
  bookCopy: any
  reader: any
  issuedAt: string
  dueDate: string
  returnedAt: string | null
  status: string
  bookTitle?: string
  readerName?: string
}

function parseList(data: any): any[] {
  if (typeof data === 'string') data = JSON.parse(data)
  if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') return data[1]
  if (Array.isArray(data)) return data
  return data.content ?? []
}

export const loansApi = {
  getAll: (status?: string) => {
    const url = status ? `/loans?status=${status}` : '/loans'
    return api.get(url).then(r => parseList(r.data) as Loan[])
  },
  getOverdue: () => api.get('/loans/overdue').then(r => parseList(r.data) as Loan[]),
  issue: (readerId: string, bookCopyId: string, dueDate: string) =>
    api.post('/loans/issue', { readerId, bookCopyId, dueDate }).then(r => r.data),
  return: (loanId: string) => api.post(`/loans/${loanId}/return`).then(r => r.data),
}
