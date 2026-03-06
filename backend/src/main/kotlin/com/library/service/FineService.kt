package com.library.service

import com.library.dto.response.FineResponse
import com.library.exception.BusinessRuleException
import com.library.exception.EntityNotFoundException
import com.library.model.Fine
import com.library.model.FineReason
import com.library.model.FineStatus
import com.library.model.Loan
import com.library.repository.FineRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class FineService(
    private val fineRepository: FineRepository,
    @Value("\${app.fine.rate-per-day}") private val fineRatePerDay: Double
) {

    /**
     * Создать штраф за просрочку — вызывается из OverdueScheduler.
     * Не создаёт дубликат, если штраф уже существует.
     */
    @Transactional
    fun createOverdueFine(loan: Loan): Fine? {
        if (fineRepository.existsByLoanIdAndStatus(loan.id, FineStatus.PENDING)) {
            return null // штраф уже создан
        }

        val daysOverdue = LocalDate.now().toEpochDay() - loan.dueDate.toEpochDay()
        if (daysOverdue <= 0) return null

        val amount = BigDecimal.valueOf(daysOverdue * fineRatePerDay)

        return fineRepository.save(
            Fine(
                loan = loan,
                reason = FineReason.OVERDUE,
                amount = amount
            )
        )
    }

    @Transactional(readOnly = true)
    fun findAll(status: FineStatus? = null): List<FineResponse> {
        val fines = if (status != null) fineRepository.findByStatus(status)
        else fineRepository.findAll()
        return fines.map { FineResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findByReader(readerId: UUID): List<FineResponse> =
        fineRepository.findByLoanReaderId(readerId).map { FineResponse.from(it) }

    @Transactional
    fun pay(fineId: UUID): FineResponse {
        val fine = fineRepository.findById(fineId)
            .orElseThrow { EntityNotFoundException("Fine", fineId) }

        if (fine.status != FineStatus.PENDING) {
            throw BusinessRuleException("Fine is already ${fine.status}")
        }

        fine.status = FineStatus.PAID
        fine.paidAt = LocalDateTime.now()
        return FineResponse.from(fineRepository.save(fine))
    }

    @Transactional
    fun waive(fineId: UUID): FineResponse {
        val fine = fineRepository.findById(fineId)
            .orElseThrow { EntityNotFoundException("Fine", fineId) }

        if (fine.status != FineStatus.PENDING) {
            throw BusinessRuleException("Only PENDING fines can be waived")
        }

        fine.status = FineStatus.WAIVED
        return FineResponse.from(fineRepository.save(fine))
    }
}
