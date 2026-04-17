// отвечает за требование ТЗ «unit-тесты». 4 теста с библиотекой MockK. Тестируют логику BookService полностью изолированно — без запуска Spring, без базы данных, все зависимости заменены моками.
package com.library.service

import com.library.dto.request.CreateBookTitleRequest
import com.library.exception.ConflictException
import com.library.exception.EntityNotFoundException
import com.library.model.BookTitle
import com.library.repository.BookCopyRepository
import com.library.repository.BookTitleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Optional
import java.util.UUID

class BookServiceTest {

    private val bookTitleRepository: BookTitleRepository = mockk()
    private val bookCopyRepository: BookCopyRepository = mockk()
    private val bookService = BookService(bookTitleRepository, bookCopyRepository)

    @Test
    fun `create book should save and return book`() {//проверяет что при создании книги она сохраняется в БД и возвращается правильный ответ с названием и автором.
        val request = CreateBookTitleRequest(
            title = "Война и мир",
            author = "Лев Толстой",
            isbn = "978-5-17-000001-1",
            year = 1869
        )
        val savedBook = BookTitle(
            title = request.title,
            author = request.author,
            isbn = request.isbn,
            year = request.year
        )

        every { bookTitleRepository.findAll() } returns emptyList()
        every { bookTitleRepository.save(any()) } returns savedBook
        every { bookCopyRepository.countAvailableCopies(any(), any()) } returns 0L

        val result = bookService.create(request)

        assertEquals("Война и мир", result.title)
        assertEquals("Лев Толстой", result.author)
        verify { bookTitleRepository.save(any()) }
    }
//проверяет бизнес-правило: нельзя добавить две книги с одинаковым ISBN. Должно выброситься исключение.
    @Test
    fun `create book with duplicate ISBN should throw ConflictException`() {
        val request = CreateBookTitleRequest(
            title = "Война и мир",
            author = "Лев Толстой",
            isbn = "978-5-17-000001-1"
        )
        val existingBook = BookTitle(
            title = "Другая книга",
            author = "Другой автор",
            isbn = "978-5-17-000001-1"
        )

        every { bookTitleRepository.findAll() } returns listOf(existingBook)

        assertThrows<ConflictException> {
            bookService.create(request)
        }
    }
//проверяет что если книга не найдена по ID, система возвращает ошибку 404 а не падает непонятно как.
    @Test
    fun `findById should throw EntityNotFoundException when book not found`() {
        val id = UUID.randomUUID()
        every { bookTitleRepository.findById(id) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            bookService.findById(id)
        }
    }
//проверяет что если книг нет, метод возвращает пустой список а не null или ошибку.
    @Test
    fun `findAll should return empty list when no books`() {
        every { bookTitleRepository.findWithFilters(null, null, null) } returns emptyList()

        val result = bookService.findAll()

        assertTrue(result.isEmpty())
    }
}
