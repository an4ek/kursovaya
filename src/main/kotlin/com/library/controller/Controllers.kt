//отвечает за требование ТЗ «REST API» и «Controller → Service → Repository». Здесь живут все пять контроллеров: BookController, LoanController, ReaderController, FineController, AuthController. Каждый принимает HTTP-запросы от браузера и передаёт их в сервисы. Сам ничего не считает и не решает — только принимает запрос и возвращает ответ. Swagger-документация генерируется автоматически из этих классов.
package com.library.controller
import com.library.dto.request.*
import com.library.dto.response.*
import com.library.model.LoanStatus
import com.library.model.FineStatus
import com.library.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new reader")
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse =
        authService.refresh(request)
}

// ─────────────────────────────────────────────────────────────────────────────
// BOOKS
// ─────────────────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books")
@SecurityRequirement(name = "bearerAuth")
class BookController(private val bookService: BookService) {

    @GetMapping
    @Operation(summary = "List books with optional filters")
    fun findAll(
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) genre: String?,
        @RequestParam(required = false, defaultValue = "false") availableOnly: Boolean
    ): List<BookTitleResponse> =
        bookService.findAll(title, author, genre, availableOnly)

    @GetMapping("/{id}")
    @Operation(summary = "Get book details")
    fun findById(@PathVariable id: UUID): BookTitleResponse =
        bookService.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Add new book title")
    fun create(@Valid @RequestBody request: CreateBookTitleRequest): BookTitleResponse =
        bookService.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Update book title")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateBookTitleRequest
    ): BookTitleResponse = bookService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Delete book title")
    fun delete(@PathVariable id: UUID) = bookService.delete(id)

    @PostMapping("/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Add copy to book")
    fun addCopy(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateBookCopyRequest
    ): BookCopyResponse = bookService.addCopy(id, request)

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Get all copies of a book")
    fun getCopies(@PathVariable id: UUID): List<BookCopyResponse> =
        bookService.getCopies(id)
}

@RestController
@RequestMapping("/api/v1/copies")
@Tag(name = "Book Copies")
@SecurityRequirement(name = "bearerAuth")
class BookCopyController(private val bookService: BookService) {

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Update copy status (DAMAGED, WRITTEN_OFF)")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCopyStatusRequest
    ): BookCopyResponse = bookService.updateCopyStatus(id, request.status)
}

// ─────────────────────────────────────────────────────────────────────────────
// READERS
// ─────────────────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/readers")
@Tag(name = "Readers")
@SecurityRequirement(name = "bearerAuth")
class ReaderController(
    private val readerService: ReaderService,
    private val loanService: LoanService,
    private val fineService: FineService
) {

    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "List all readers")
    fun findAll(): List<ReaderResponse> = readerService.findAll()

    @GetMapping("/{id}")
    @Operation(summary = "Get reader profile")
    fun findById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDetails
    ): ReaderResponse = readerService.findById(id, user)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Update reader profile")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateReaderRequest
    ): ReaderResponse = readerService.update(id, request)

    @GetMapping("/{id}/loans")
    @Operation(summary = "Get reader's loans")
    fun getLoans(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDetails
    ): List<LoanResponse> = readerService.getLoans(id, user)

    @GetMapping("/{id}/fines")
    @Operation(summary = "Get reader's fines")
    fun getFines(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserDetails
    ): List<FineResponse> = readerService.getFines(id, user)
}

// ─────────────────────────────────────────────────────────────────────────────
// LOANS
// ─────────────────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans")
@SecurityRequirement(name = "bearerAuth")
class LoanController(private val loanService: LoanService) {

    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Issue book to reader")
    fun issue(
        @Valid @RequestBody request: IssueLoanRequest,
        @AuthenticationPrincipal user: UserDetails
    ): LoanResponse = loanService.issue(request, user.username)

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Accept book return")
    fun returnLoan(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: ReturnLoanRequest?,
        @AuthenticationPrincipal user: UserDetails
    ): LoanResponse = loanService.returnLoan(id, user.username, request?.notes)

    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "List loans with filters")
    fun findAll(
        @RequestParam(required = false) readerId: UUID?,
        @RequestParam(required = false) status: LoanStatus?
    ): List<LoanResponse> = loanService.findAll(readerId, status)

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Get all overdue loans")
    fun findOverdue(): List<LoanResponse> = loanService.findOverdue()

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Get loan status history")
    fun getHistory(@PathVariable id: UUID): List<LoanHistoryResponse> =
        loanService.getHistory(id)
}

// ─────────────────────────────────────────────────────────────────────────────
// FINES
// ─────────────────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/fines")
@Tag(name = "Fines")
@SecurityRequirement(name = "bearerAuth")
class FineController(private val fineService: FineService) {

    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "List all fines")
    fun findAll(@RequestParam(required = false) status: FineStatus?): List<FineResponse> =
        fineService.findAll(status)

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Mark fine as paid")
    fun pay(@PathVariable id: UUID): FineResponse = fineService.pay(id)

    @PatchMapping("/{id}/waive")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Waive a fine")
    fun waive(@PathVariable id: UUID): FineResponse = fineService.waive(id)
}
