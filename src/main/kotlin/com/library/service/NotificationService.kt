//отвечает за требование ТЗ «@Async». Все методы аннотированы @Async — выполняются в отдельном потоке из пула AsyncConfig. Сейчас пишет уведомления в лог, в реальной системе здесь был бы email или SMS.
package com.library.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Async("taskExecutor")
    fun notifyLoanIssued(readerName: String, bookTitle: String, dueDate: LocalDate) {
        logger.info("[ASYNC] Уведомление: читатель '$readerName' получил книгу '$bookTitle', срок возврата: $dueDate")
        // В реальной системе здесь был бы email/SMS
    }

    @Async("taskExecutor")
    fun notifyLoanReturned(readerName: String, bookTitle: String) {
        logger.info("[ASYNC] Уведомление: читатель '$readerName' вернул книгу '$bookTitle'")
    }

    @Async("taskExecutor")
    fun notifyOverdue(readerName: String, bookTitle: String, daysOverdue: Long) {
        logger.warn("[ASYNC] Уведомление: читатель '$readerName' просрочил возврат книги '$bookTitle' на $daysOverdue дней")
    }
}
