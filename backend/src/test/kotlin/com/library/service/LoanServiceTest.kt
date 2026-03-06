package com.library.service

import com.library.dto.request.IssueLoanRequest
import com.library.exception.BusinessRuleException
import com.library.model.*
import com.library.repository.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class LoanServiceTest {

    @MockK lateinit var loanRepository: LoanRepository
    @MockK lateinit var loanHistoryRepository: LoanHistoryRepository
    @MockK lateinit var bookCopyRepository: BookCopyRepository
    @MockK lateinit var readerRepository: ReaderRepository
    @MockK lateinit var userAccountRepository: UserAccountRepository
    @MockK lateinit var fineRepository: FineRepository

    @InjectMockKs
    lateinit var loanService: LoanService

    private lateinit var account: UserAccount
    private lateinit var reader: Reader
    private lateinit var bookTitle: BookTitle
    private lateinit var bookCopy: BookCopy

    @BeforeEach
    fun setup() {
        account = UserAccount(login = "librarian", passwordHash = "hash", role = UserRole.LIBRARIAN)
        reader = Reader(userAccount = UserAccount(login = "reader1", passwordHash = "h", role = UserRole.READER), fullName = "Иван Иванов")
        bookTitle = BookTitle(title = "Война и мир", author = "Толстой")
        bookCopy = BookCopy(bookTitle = bookTitle, inventoryNumber = "INV-001")
    }

    @Test
    fun `issue - success when copy is available and reader has no violations`() {
        val request = IssueLoanRequest(
            readerId = reader.id,
            bookCopyId = bookCopy.id,
            dueDate = LocalDate.now().plusDays(14)
        )

        every { bookCopyRepository.findById(bookCopy.id) } returns Optional.of(bookCopy)
        every { readerRepository.findById(reader.id) } returns Optional.of(reader)
        every { userAccountRepository.findByLogin("librarian") } returns account
        every { fineRepository.hasUnpaidFines(reader.id) } returns false
        every { readerRepository.countActiveLoans(reader.id) } returns 0
        every { loanRepository.save(any()) } answers { firstArg() }
        every { bookCopyRepository.save(any()) } answers { firstArg() }
        every { loanHistoryRepository.save(any()) } answers { firstArg() }

        val result = loanService.issue(request, "librarian")

        assertEquals("ACTIVE", result.status)
        verify { bookCopyRepository.save(match { it.status == BookCopyStatus.LOANED }) }
        verify { loanHistoryRepository.save(any()) }
    }

    @Test
    fun `issue - throws when copy is already loaned`() {
        bookCopy.status = BookCopyStatus.LOANED
        val request = IssueLoanRequest(
            readerId = reader.id,
            bookCopyId = bookCopy.id,
            dueDate = LocalDate.now().plusDays(14)
        )

        every { bookCopyRepository.findById(bookCopy.id) } returns Optional.of(bookCopy)
        every { readerRepository.findById(reader.id) } returns Optional.of(reader)
        every { userAccountRepository.findByLogin("librarian") } returns account

        assertThrows<BusinessRuleException> {
            loanService.issue(request, "librarian")
        }
    }

    @Test
    fun `issue - throws when reader has unpaid fines`() {
        val request = IssueLoanRequest(
            readerId = reader.id,
            bookCopyId = bookCopy.id,
            dueDate = LocalDate.now().plusDays(14)
        )

        every { bookCopyRepository.findById(bookCopy.id) } returns Optional.of(bookCopy)
        every { readerRepository.findById(reader.id) } returns Optional.of(reader)
        every { userAccountRepository.findByLogin("librarian") } returns account
        every { fineRepository.hasUnpaidFines(reader.id) } returns true

        assertThrows<BusinessRuleException> {
            loanService.issue(request, "librarian")
        }
    }

    @Test
    fun `issue - throws when reader exceeds active loan limit`() {
        val request = IssueLoanRequest(
            readerId = reader.id,
            bookCopyId = bookCopy.id,
            dueDate = LocalDate.now().plusDays(14)
        )

        every { bookCopyRepository.findById(bookCopy.id) } returns Optional.of(bookCopy)
        every { readerRepository.findById(reader.id) } returns Optional.of(reader)
        every { userAccountRepository.findByLogin("librarian") } returns account
        every { fineRepository.hasUnpaidFines(reader.id) } returns false
        every { readerRepository.countActiveLoans(reader.id) } returns 5L // достигнут лимит

        assertThrows<BusinessRuleException> {
            loanService.issue(request, "librarian")
        }
    }

    @Test
    fun `returnLoan - sets status RETURNED and frees copy`() {
        val loan = Loan(
            bookCopy = bookCopy,
            reader = reader,
            dueDate = LocalDate.now().plusDays(5),
            issuedBy = account
        )
        bookCopy.status = BookCopyStatus.LOANED

        every { loanRepository.findById(loan.id) } returns Optional.of(loan)
        every { userAccountRepository.findByLogin("librarian") } returns account
        every { loanRepository.save(any()) } answers { firstArg() }
        every { bookCopyRepository.save(any()) } answers { firstArg() }
        every { loanHistoryRepository.save(any()) } answers { firstArg() }

        val result = loanService.returnLoan(loan.id, "librarian")

        assertEquals("RETURNED", result.status)
        assertNotNull(result.returnedAt)  // нет в LoanResponse — проверяем через save
        verify { bookCopyRepository.save(match { it.status == BookCopyStatus.AVAILABLE }) }
    }
}
