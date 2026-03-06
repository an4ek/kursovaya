package com.library.service

import com.library.config.RedisConfig
import com.library.dto.request.CreateBookCopyRequest
import com.library.dto.request.CreateBookTitleRequest
import com.library.dto.request.UpdateBookTitleRequest
import com.library.dto.response.BookCopyResponse
import com.library.dto.response.BookTitleResponse
import com.library.exception.BusinessRuleException
import com.library.exception.ConflictException
import com.library.exception.EntityNotFoundException
import com.library.model.BookCopy
import com.library.model.BookCopyCondition
import com.library.model.BookCopyStatus
import com.library.model.BookTitle
import com.library.repository.BookCopyRepository
import com.library.repository.BookTitleRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class BookService(
    private val bookTitleRepository: BookTitleRepository,
    private val bookCopyRepository: BookCopyRepository
) {

    @Cacheable(cacheNames = [RedisConfig.BOOKS_CACHE], key = "'all:' + #title + ':' + #author + ':' + #genre + ':' + #availableOnly")
    @Transactional(readOnly = true)
    fun findAll(
        title: String? = null,
        author: String? = null,
        genre: String? = null,
        availableOnly: Boolean = false
    ): List<BookTitleResponse> {
        val books = if (availableOnly) {
            bookTitleRepository.findAllAvailable()
        } else {
            bookTitleRepository.findWithFilters(title, author, genre)
        }

        return books.map { book ->
            val availableCount = bookCopyRepository
                .countByBookTitleIdAndStatus(book.id, BookCopyStatus.AVAILABLE)
            BookTitleResponse.from(book, availableCount)
        }
    }

    @Cacheable(cacheNames = [RedisConfig.BOOK_DETAIL_CACHE], key = "#id")
    @Transactional(readOnly = true)
    fun findById(id: UUID): BookTitleResponse {
        val book = bookTitleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("BookTitle", id) }
        val availableCount = bookCopyRepository
            .countByBookTitleIdAndStatus(book.id, BookCopyStatus.AVAILABLE)
        return BookTitleResponse.from(book, availableCount)
    }

    @Caching(evict = [
        CacheEvict(cacheNames = [RedisConfig.BOOKS_CACHE], allEntries = true)
    ])
    @Transactional
    fun create(request: CreateBookTitleRequest): BookTitleResponse {
        if (request.isbn != null && bookTitleRepository.findAll()
                .any { it.isbn == request.isbn }) {
            throw ConflictException("Book with ISBN '${request.isbn}' already exists")
        }

        val book = bookTitleRepository.save(
            BookTitle(
                title = request.title,
                author = request.author,
                isbn = request.isbn,
                genre = request.genre,
                year = request.year,
                description = request.description
            )
        )
        return BookTitleResponse.from(book, 0)
    }

    @Caching(evict = [
        CacheEvict(cacheNames = [RedisConfig.BOOKS_CACHE], allEntries = true),
        CacheEvict(cacheNames = [RedisConfig.BOOK_DETAIL_CACHE], key = "#id")
    ])
    @Transactional
    fun update(id: UUID, request: UpdateBookTitleRequest): BookTitleResponse {
        val book = bookTitleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("BookTitle", id) }

        request.title?.let { book.title = it }
        request.author?.let { book.author = it }
        request.isbn?.let { book.isbn = it }
        request.genre?.let { book.genre = it }
        request.year?.let { book.year = it }
        request.description?.let { book.description = it }
        book.updatedAt = LocalDateTime.now()

        val saved = bookTitleRepository.save(book)
        val availableCount = bookCopyRepository
            .countByBookTitleIdAndStatus(book.id, BookCopyStatus.AVAILABLE)
        return BookTitleResponse.from(saved, availableCount)
    }

    @Caching(evict = [
        CacheEvict(cacheNames = [RedisConfig.BOOKS_CACHE], allEntries = true),
        CacheEvict(cacheNames = [RedisConfig.BOOK_DETAIL_CACHE], key = "#id")
    ])
    @Transactional
    fun delete(id: UUID) {
        val book = bookTitleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("BookTitle", id) }

        val hasLoaned = bookCopyRepository.findByBookTitleId(id)
            .any { it.status == BookCopyStatus.LOANED }

        if (hasLoaned) {
            throw BusinessRuleException("Cannot delete book with active loans")
        }

        bookTitleRepository.delete(book)
    }

    @Transactional
    fun addCopy(bookId: UUID, request: CreateBookCopyRequest): BookCopyResponse {
        val book = bookTitleRepository.findById(bookId)
            .orElseThrow { EntityNotFoundException("BookTitle", bookId) }

        if (bookCopyRepository.findByInventoryNumber(request.inventoryNumber) != null) {
            throw ConflictException("Copy with inventory number '${request.inventoryNumber}' already exists")
        }

        val copy = bookCopyRepository.save(
            BookCopy(
                bookTitle = book,
                inventoryNumber = request.inventoryNumber,
                condition = BookCopyCondition.valueOf(request.condition)
            )
        )
        return BookCopyResponse.from(copy)
    }

    @Transactional(readOnly = true)
    fun getCopies(bookId: UUID): List<BookCopyResponse> {
        bookTitleRepository.findById(bookId)
            .orElseThrow { EntityNotFoundException("BookTitle", bookId) }

        return bookCopyRepository.findByBookTitleId(bookId)
            .map { BookCopyResponse.from(it) }
    }

    @Caching(evict = [
        CacheEvict(cacheNames = [RedisConfig.BOOKS_CACHE], allEntries = true)
    ])
    @Transactional
    fun updateCopyStatus(copyId: UUID, newStatus: String): BookCopyResponse {
        val copy = bookCopyRepository.findById(copyId)
            .orElseThrow { EntityNotFoundException("BookCopy", copyId) }

        if (copy.status == BookCopyStatus.LOANED) {
            throw BusinessRuleException("Cannot change status of a loaned copy")
        }

        copy.status = BookCopyStatus.valueOf(newStatus)
        return BookCopyResponse.from(bookCopyRepository.save(copy))
    }
}
