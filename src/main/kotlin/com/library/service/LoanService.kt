//самый важный сервис, отвечает за все бизнес-правила ТЗ. При выдаче проверяет: не занят ли экземпляр, нет ли штрафов у читателя, не превышен ли лимит выдач. При возврате обновляет статусы выдачи и экземпляра, пишет в историю. После каждой операции асинхронно вызывает NotificationService.
package com.library.service

import com.library.dto.request.IssueLoanRequest
import com.library.dto.response.LoanHistoryResponse
import com.library.dto.response.LoanResponse
import com.library.exception.BusinessRuleException
import com.library.exception.EntityNotFoundException
import com.library.model.*
import com.library.repository.*
import org.springframework.beans.factory.annotation.Value
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class LoanService(
    private val loanRepository: LoanRepository,
    private val loanHistoryRepository: LoanHistoryRepository,
    private val bookCopyRepository: BookCopyRepository,
    private val readerRepository: ReaderRepository,
    private val userAccountRepository: UserAccountRepository,
    private val fineRepository: FineRepository,
    private val notificationService: NotificationService
) {

    @Transactional
    fun issue(request: IssueLoanRequest, issuedByLogin: String): LoanResponse {
        val copy = bookCopyRepository.findById(request.bookCopyId)
            .orElseThrow { EntityNotFoundException("BookCopy", request.bookCopyId) }

        val reader = readerRepository.findById(request.readerId)
            .orElseThrow { EntityNotFoundException("Reader", request.readerId) }

        val issuedBy = userAccountRepository.findByLogin(issuedByLogin)
            ?: throw EntityNotFoundException("UserAccount", issuedByLogin)

        // Бизнес-правило: экземпляр должен быть доступен
        if (copy.status != BookCopyStatus.AVAILABLE) {
            throw BusinessRuleException(
                "Book copy '${copy.inventoryNumber}' is not available (status: ${copy.status})"
            )
        }

        // Бизнес-правило: у читателя не должно быть активных штрафов
        if (fineRepository.hasUnpaidFines(reader.id)) {
            throw BusinessRuleException(
                "Reader '${reader.fullName}' has unpaid fines. Please pay them before taking new books."
            )
        }

        // Бизнес-правило: лимит активных выдач
        val activeLoans = readerRepository.countActiveLoans(reader.id)
        if (activeLoans >= reader.maxActiveLoans) {
            throw BusinessRuleException(
                "Reader '${reader.fullName}' has reached the active loan limit (${reader.maxActiveLoans})"
            )
        }

        // Создаём выдачу
        val loan = loanRepository.save(
            Loan(
                bookCopy = copy,
                reader = reader,
                dueDate = request.dueDate,
                issuedBy = issuedBy,
                notes = request.notes
            )
        )

        // Обновляем статус экземпляра
        copy.status = BookCopyStatus.LOANED
        bookCopyRepository.save(copy)

        // Пишем историю
        recordHistory(loan, previousStatus = null, newStatus = LoanStatus.ACTIVE, changedBy = issuedBy)

        return LoanResponse.from(loan)
    }

    @Transactional
    fun returnLoan(loanId: UUID, performedByLogin: String, notes: String? = null): LoanResponse {
        val loan = loanRepository.findById(loanId)
            .orElseThrow { EntityNotFoundException("Loan", loanId) }

        val performedBy = userAccountRepository.findByLogin(performedByLogin)
            ?: throw EntityNotFoundException("UserAccount", performedByLogin)

        if (loan.status !in listOf(LoanStatus.ACTIVE, LoanStatus.OVERDUE)) {
            throw BusinessRuleException("Loan is already ${loan.status}")
        }

        val previousStatus = loan.status

        loan.status = LoanStatus.RETURNED
        loan.returnedAt = LocalDateTime.now()
        notes?.let { loan.notes = it }

        loan.bookCopy.status = BookCopyStatus.AVAILABLE
        bookCopyRepository.save(loan.bookCopy)

        loanRepository.save(loan)

        recordHistory(loan, previousStatus = previousStatus, newStatus = LoanStatus.RETURNED, changedBy = performedBy)

        return LoanResponse.from(loan)
    }

    @Transactional(readOnly = true)
    fun findAll(readerId: UUID? = null, status: LoanStatus? = null): List<LoanResponse> =
        loanRepository.findWithFilters(readerId, status).map { LoanResponse.from(it) }

    @Transactional(readOnly = true)
    fun findOverdue(): List<LoanResponse> =
        loanRepository.findAllOverdue().map { LoanResponse.from(it) }

    @Transactional(readOnly = true)
    fun findByReader(readerId: UUID): List<LoanResponse> =
        loanRepository.findByReaderId(readerId).map { LoanResponse.from(it) }

    @Transactional(readOnly = true)
    fun getHistory(loanId: UUID): List<LoanHistoryResponse> =
        loanHistoryRepository.findByLoanIdOrderByChangedAtAsc(loanId)
            .map { LoanHistoryResponse.from(it) }

    // Внутренний метод — используется также в OverdueScheduler
    @Transactional
    fun markAsOverdue(loan: Loan, systemAccount: UserAccount) {
        val previousStatus = loan.status
        loan.status = LoanStatus.OVERDUE
        loanRepository.save(loan)
        recordHistory(loan, previousStatus, LoanStatus.OVERDUE, systemAccount, "Auto-marked as overdue by scheduler")
    }

    private fun recordHistory(
        loan: Loan,
        previousStatus: LoanStatus?,
        newStatus: LoanStatus,
        changedBy: UserAccount,
        comment: String? = null
    ) {
        loanHistoryRepository.save(
            LoanHistory(
                loan = loan,
                previousStatus = previousStatus,
                newStatus = newStatus,
                changedBy = changedBy,
                comment = comment
            )
        )
    }
}
