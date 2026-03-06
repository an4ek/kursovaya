package com.library.dto.response

import com.library.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// ── AUTH ──────────────────────────────────────────────────────────────────────

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val login: String,
    val role: String
)

// ── BOOK ──────────────────────────────────────────────────────────────────────

data class BookTitleResponse(
    val id: UUID,
    val title: String,
    val author: String,
    val isbn: String?,
    val genre: String?,
    val year: Int?,
    val description: String?,
    val totalCopies: Int,
    val availableCopies: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(book: BookTitle, availableCount: Long = 0): BookTitleResponse =
            BookTitleResponse(
                id = book.id,
                title = book.title,
                author = book.author,
                isbn = book.isbn,
                genre = book.genre,
                year = book.year,
                description = book.description,
                totalCopies = book.copies.size,
                availableCopies = availableCount.toInt(),
                createdAt = book.createdAt
            )
    }
}

data class BookCopyResponse(
    val id: UUID,
    val bookTitleId: UUID,
    val bookTitle: String,
    val inventoryNumber: String,
    val status: String,
    val condition: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(copy: BookCopy) = BookCopyResponse(
            id = copy.id,
            bookTitleId = copy.bookTitle.id,
            bookTitle = copy.bookTitle.title,
            inventoryNumber = copy.inventoryNumber,
            status = copy.status.name,
            condition = copy.condition.name,
            createdAt = copy.createdAt
        )
    }
}

// ── READER ────────────────────────────────────────────────────────────────────

data class ReaderResponse(
    val id: UUID,
    val login: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val maxActiveLoans: Int,
    val registeredAt: LocalDateTime
) {
    companion object {
        fun from(reader: Reader) = ReaderResponse(
            id = reader.id,
            login = reader.userAccount.login,
            fullName = reader.fullName,
            phone = reader.phone,
            email = reader.email,
            maxActiveLoans = reader.maxActiveLoans,
            registeredAt = reader.registeredAt
        )
    }
}

// ── LOAN ──────────────────────────────────────────────────────────────────────

data class LoanResponse(
    val id: UUID,
    val bookCopyId: UUID,
    val inventoryNumber: String,
    val bookTitle: String,
    val bookAuthor: String,
    val readerId: UUID,
    val readerName: String,
    val issuedAt: LocalDateTime,
    val dueDate: LocalDate,
    val returnedAt: LocalDateTime?,
    val status: String,
    val notes: String?
) {
    companion object {
        fun from(loan: Loan) = LoanResponse(
            id = loan.id,
            bookCopyId = loan.bookCopy.id,
            inventoryNumber = loan.bookCopy.inventoryNumber,
            bookTitle = loan.bookCopy.bookTitle.title,
            bookAuthor = loan.bookCopy.bookTitle.author,
            readerId = loan.reader.id,
            readerName = loan.reader.fullName,
            issuedAt = loan.issuedAt,
            dueDate = loan.dueDate,
            returnedAt = loan.returnedAt,
            status = loan.status.name,
            notes = loan.notes
        )
    }
}

data class LoanHistoryResponse(
    val id: UUID,
    val previousStatus: String?,
    val newStatus: String,
    val changedAt: LocalDateTime,
    val comment: String?
) {
    companion object {
        fun from(h: LoanHistory) = LoanHistoryResponse(
            id = h.id,
            previousStatus = h.previousStatus?.name,
            newStatus = h.newStatus.name,
            changedAt = h.changedAt,
            comment = h.comment
        )
    }
}

// ── FINE ──────────────────────────────────────────────────────────────────────

data class FineResponse(
    val id: UUID,
    val loanId: UUID,
    val readerName: String,
    val bookTitle: String,
    val reason: String,
    val amount: BigDecimal,
    val status: String,
    val createdAt: LocalDateTime,
    val paidAt: LocalDateTime?
) {
    companion object {
        fun from(fine: Fine) = FineResponse(
            id = fine.id,
            loanId = fine.loan.id,
            readerName = fine.loan.reader.fullName,
            bookTitle = fine.loan.bookCopy.bookTitle.title,
            reason = fine.reason.name,
            amount = fine.amount,
            status = fine.status.name,
            createdAt = fine.createdAt,
            paidAt = fine.paidAt
        )
    }
}
