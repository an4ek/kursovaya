//отвечает за требование ТЗ «валидация входа (@Valid)». Описывает как должны выглядеть входящие данные. Например, при выдаче книги обязательно нужны readerId, bookCopyId и dueDate — если что-то не передать, система вернёт ошибку валидации ещё до вызова бизнес-логики
package com.library.dto.request

import jakarta.validation.constraints.*
import java.time.LocalDate
import java.util.UUID

// AUTH

data class RegisterRequest(
    @field:NotBlank(message = "Login is required")
    @field:Size(min = 3, max = 50, message = "Login must be 3-50 characters")
    val login: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    val phone: String? = null,
    val email: String? = null
)

data class LoginRequest(
    @field:NotBlank val login: String,
    @field:NotBlank val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

// BOOK

data class CreateBookTitleRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 500)
    val title: String,

    @field:NotBlank(message = "Author is required")
    val author: String,

    @field:Size(max = 20)
    val isbn: String? = null,

    val genre: String? = null,

    @field:Min(1000) @field:Max(2100)
    val year: Int? = null,

    val description: String? = null
)

data class UpdateBookTitleRequest(
    val title: String? = null,
    val author: String? = null,
    val isbn: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val description: String? = null
)

data class CreateBookCopyRequest(
    @field:NotBlank(message = "Inventory number is required")
    val inventoryNumber: String,

    val condition: String = "GOOD"
)

data class UpdateCopyStatusRequest(
    @field:NotBlank val status: String
)

// READER

data class UpdateReaderRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val maxActiveLoans: Int? = null
)

// LOAN

data class IssueLoanRequest(
    @field:NotNull val readerId: UUID,
    @field:NotNull val bookCopyId: UUID,

    @field:NotNull(message = "Due date is required")
    @field:Future(message = "Due date must be in the future")
    val dueDate: LocalDate,

    val notes: String? = null
)

data class ReturnLoanRequest(
    val notes: String? = null
)

// FINE

data class WaiveFineRequest(
    val reason: String? = null
)
