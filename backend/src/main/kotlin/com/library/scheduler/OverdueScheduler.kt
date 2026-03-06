package com.library.scheduler

import com.library.repository.LoanRepository
import com.library.repository.UserAccountRepository
import com.library.service.FineService
import com.library.service.LoanService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OverdueScheduler(
    private val loanRepository: LoanRepository,
    private val userAccountRepository: UserAccountRepository,
    private val loanService: LoanService,
    private val fineService: FineService
) {

    private val log = LoggerFactory.getLogger(OverdueScheduler::class.java)

    /**
     * Каждые 30 минут проверяем активные выдачи с истёкшим сроком.
     * Переводим их в OVERDUE и создаём Fine.
     */
    @Scheduled(cron = "\${app.scheduler.overdue-check-cron}")
    fun checkOverdueLoans() {
        log.info("Starting overdue check at ${LocalDate.now()}")

        val overdueLoans = loanRepository.findAllOverdue()

        if (overdueLoans.isEmpty()) {
            log.info("No overdue loans found")
            return
        }

        // Системный аккаунт для записи в LoanHistory
        val systemAccount = userAccountRepository.findByLogin("admin")
            ?: run {
                log.error("System account 'admin' not found, skipping overdue check")
                return
            }

        var markedCount = 0
        var finesCreated = 0

        overdueLoans.forEach { loan ->
            runCatching {
                loanService.markAsOverdue(loan, systemAccount)
                markedCount++

                fineService.createOverdueFine(loan)?.let { finesCreated++ }
            }.onFailure { ex ->
                log.error("Failed to process overdue loan ${loan.id}: ${ex.message}")
            }
        }

        log.info("Overdue check complete: $markedCount loans marked, $finesCreated fines created")
    }
}
