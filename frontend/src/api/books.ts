//все запросы связанные с книгами: получить список, добавить книгу, добавить экземпляр.
import api from './axios'

export interface BookTitle {
  id: string
  title: string
  author: string
  isbn: string | null
  genre: string | null
  year: number | null
  totalCopies: number
  availableCopies: number
  createdAt: string
}

export interface CreateBookRequest {
  title: string
  author: string
  isbn?: string | null
  year?: number | null
}

function parseList(data: any): any[] {
  if (typeof data === 'string') data = JSON.parse(data)
  if (Array.isArray(data) && data.length === 2 && typeof data[0] === 'string') return data[1]
  if (Array.isArray(data)) return data
  return data.content ?? []
}

export const booksApi = {
  getAll: () => api.get('/books').then(r => parseList(r.data) as BookTitle[]),
  create: (req: CreateBookRequest) => api.post('/books', req).then(r => r.data as BookTitle),
  getCopies: (bookId: string) => api.get(`/books/${bookId}/copies`).then(r => parseList(r.data)),
  addCopy: (bookId: string, inventoryNumber: string) =>
    api.post(`/books/${bookId}/copies`, { inventoryNumber, condition: 'GOOD' }).then(r => r.data),
}
