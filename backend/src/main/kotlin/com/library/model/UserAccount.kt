package com.library.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class UserRole { LIBRARIAN, READER }
enum class UserStatus { ACTIVE, BLOCKED }

@Entity
@Table(name = "user_account")
class UserAccount(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 100)
    val login: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
