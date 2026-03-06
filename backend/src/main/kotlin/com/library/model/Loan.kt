package com.library.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class LoanStatus { ACTIVE, RETURNED, OVERDUE, LOST }
enum class FineStatus { PENDING, PAID, WAIVED }
enum class FineReason { OVERDUE, DAMAGE, LOSS }

@Entity
@Table(name = "loan")
class Loan(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_copy_id", nullable = false)
    val bookCopy: BookCopy,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    val reader: Reader,

    @Column(name = "issued_at", nullable = false, updatable = false)
    val issuedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "due_date", nullable = false)
    val dueDate: LocalDate,

    @Column(name = "returned_at")
    var returnedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LoanStatus = LoanStatus.ACTIVE,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by", nullable = false)
    val issuedBy: UserAccount,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @OneToMany(mappedBy = "loan", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val history: MutableList<LoanHistory> = mutableListOf(),

    @OneToOne(mappedBy = "loan", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var fine: Fine? = null
) {
    val isOverdue: Boolean
        get() = status == LoanStatus.ACTIVE && dueDate.isBefore(LocalDate.now())
}

// ──────────────────────────────────────────

@Entity
@Table(name = "loan_history")
class LoanHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    val loan: Loan,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    val previousStatus: LoanStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    val newStatus: LoanStatus,

    @Column(name = "changed_at", nullable = false, updatable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    val changedBy: UserAccount,

    @Column(columnDefinition = "TEXT")
    val comment: String? = null
)

// ──────────────────────────────────────────

@Entity
@Table(name = "fine")
class Fine(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    val loan: Loan,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val reason: FineReason,

    @Column(nullable = false, precision = 10, scale = 2)
    var amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FineStatus = FineStatus.PENDING,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null
)
