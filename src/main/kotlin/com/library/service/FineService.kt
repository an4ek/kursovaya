//отвечает за управление штрафами. Создаёт штраф при просрочке и пересчитывает его сумму при каждом запуске планировщика.
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
     * Создать или обновить штраф за просрочку — вызывается из OverdueScheduler.
     * Если штраф уже существует — пересчитывает сумму по актуальному числу дней.
     */
    @Transactional
    fun createOverdueFine(loan: Loan): Fine? {
        val daysOverdue = LocalDate.now().toEpochDay() - loan.dueDate.toEpochDay()
        if (daysOverdue <= 0) return null
        val amount = BigDecimal.valueOf(daysOverdue * fineRatePerDay)
        val existing = fineRepository.findByLoanIdAndStatus(loan.id, FineStatus.PENDING)
        return if (existing != null) {
            existing.amount = amount
            fineRepository.save(existing)
        } else {
            fineRepository.save(
                Fine(
                    loan = loan,
                    reason = FineReason.OVERDUE,
                    amount = amount
                )
            )
        }
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