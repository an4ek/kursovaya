package com.library.repository

import com.library.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun findByLogin(login: String): UserAccount?
    fun existsByLogin(login: String): Boolean
}

@Repository
interface ReaderRepository : JpaRepository<Reader, UUID> {
    fun findByUserAccount(userAccount: UserAccount): Reader?
    fun findByUserAccountId(userAccountId: UUID): Reader?

    @Query("""
        SELECT COUNT(l) FROM Loan l 
        WHERE l.reader.id = :readerId 
        AND l.status IN ('ACTIVE', 'OVERDUE')
    """)
    fun countActiveLoans(@Param("readerId") readerId: UUID): Long
}

@Repository
interface BookTitleRepository : JpaRepository<BookTitle, UUID> {
    @Query(
        value = """
            SELECT * FROM book_title bt 
            WHERE (:title IS NULL OR bt.title ILIKE CONCAT('%', CAST(:title AS text), '%'))
            AND (:author IS NULL OR bt.author ILIKE CONCAT('%', CAST(:author AS text), '%'))
        """,
        nativeQuery = true
    )
    fun findWithFilters(
        @Param("title") title: String?,
        @Param("author") author: String?
    ): List<BookTitle>

    @Query(
        value = """
            SELECT * FROM book_title bt 
            WHERE EXISTS (
                SELECT 1 FROM book_copy bc 
                WHERE bc.book_title_id = bt.id AND bc.status = 'AVAILABLE'
            )
        """,
        nativeQuery = true
    )
    fun findAllAvailable(): List<BookTitle>
}

@Repository
interface BookCopyRepository : JpaRepository<BookCopy, UUID> {
    fun findByBookTitleId(bookTitleId: UUID): List<BookCopy>
    fun findByInventoryNumber(inventoryNumber: String): BookCopy?

    @Query("""
        SELECT bc FROM BookCopy bc 
        WHERE bc.bookTitle.id = :titleId AND bc.status = 'AVAILABLE'
        ORDER BY bc.createdAt ASC
    """)
    fun findFirstAvailableByBookTitleId(@Param("titleId") titleId: UUID): List<BookCopy>

    @Query(
        value = "SELECT COUNT(*) FROM book_copy WHERE book_title_id = :titleId AND status = :status",
        nativeQuery = true
    )
    fun countAvailableCopies(
        @Param("titleId") bookTitleId: UUID,
        @Param("status") status: String
    ): Long
}

@Repository
interface LoanRepository : JpaRepository<Loan, UUID> {
    fun findByReaderId(readerId: UUID): List<Loan>

    fun findByReaderIdAndStatus(readerId: UUID, status: LoanStatus): List<Loan>

    @Query("""
        SELECT l FROM Loan l 
        JOIN FETCH l.bookCopy bc 
        JOIN FETCH bc.bookTitle
        JOIN FETCH l.reader r
        WHERE l.status = 'ACTIVE' AND l.dueDate < :today
    """)
    fun findAllOverdue(@Param("today") today: LocalDate = LocalDate.now()): List<Loan>

    @Query("""
        SELECT l FROM Loan l 
        WHERE l.bookCopy.id = :copyId 
        AND l.status IN ('ACTIVE', 'OVERDUE')
    """)
    fun findActiveLoanByBookCopyId(@Param("copyId") copyId: UUID): Loan?

    @Query("""
        SELECT l FROM Loan l 
        JOIN FETCH l.bookCopy bc 
        JOIN FETCH bc.bookTitle bt
        JOIN FETCH l.reader r
        WHERE (:readerId IS NULL OR l.reader.id = :readerId)
        AND (:status IS NULL OR l.status = :status)
    """)
    fun findWithFilters(
        @Param("readerId") readerId: UUID?,
        @Param("status") status: LoanStatus?
    ): List<Loan>
}

@Repository
interface LoanHistoryRepository : JpaRepository<LoanHistory, UUID> {
    fun findByLoanIdOrderByChangedAtAsc(loanId: UUID): List<LoanHistory>
}

@Repository
interface FineRepository : JpaRepository<Fine, UUID> {
    fun findByLoanReaderId(readerId: UUID): List<Fine>

    fun findByStatus(status: FineStatus): List<Fine>

    fun existsByLoanIdAndStatus(loanId: UUID, status: FineStatus): Boolean

    @Query("""
        SELECT COUNT(f) > 0 FROM Fine f 
        WHERE f.loan.reader.id = :readerId 
        AND f.status = 'PENDING'
    """)
    fun hasUnpaidFines(@Param("readerId") readerId: UUID): Boolean
}
