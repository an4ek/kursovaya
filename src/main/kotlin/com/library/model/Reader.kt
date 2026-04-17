//отвечает за требование ТЗ «сущность Reader». Хранит данные читателя: ФИО, email, телефон, лимит активных выдач.
package com.library.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "reader")
class Reader(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    val userAccount: UserAccount,

    @Column(name = "full_name", nullable = false)
    var fullName: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(length = 100)
    var email: String? = null,

    @Column(name = "max_active_loans", nullable = false)
    var maxActiveLoans: Int = 5,

    @Column(name = "registered_at", nullable = false, updatable = false)
    val registeredAt: LocalDateTime = LocalDateTime.now()
)
