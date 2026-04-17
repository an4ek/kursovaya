//твечает за требование ТЗ «сущности BookTitle и BookCopy» и «связь OneToMany». BookTitle — это запись «Война и Мир Толстого», BookCopy — конкретный экземпляр с инвентарным номером и статусом (AVAILABLE, LOANED, LOST). Одна книга может иметь много экземпляров.
package com.library.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "book_title")
class BookTitle(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 500)
    var title: String,

    @Column(nullable = false)
    var author: String,

    @Column(unique = true, length = 20)
    var isbn: String? = null,

    @Column(length = 100)
    var genre: String? = null,

    var year: Int? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "bookTitle", fetch = FetchType.LAZY)
    val copies: MutableList<BookCopy> = mutableListOf()
)

// ──────────────────────────────────────────

enum class BookCopyStatus { AVAILABLE, LOANED, RESERVED, DAMAGED, WRITTEN_OFF }
enum class BookCopyCondition { NEW, GOOD, SATISFACTORY, POOR }

@Entity
@Table(name = "book_copy")
class BookCopy(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_title_id", nullable = false)
    val bookTitle: BookTitle,

    @Column(name = "inventory_number", nullable = false, unique = true)
    val inventoryNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BookCopyStatus = BookCopyStatus.AVAILABLE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var condition: BookCopyCondition = BookCopyCondition.GOOD,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
