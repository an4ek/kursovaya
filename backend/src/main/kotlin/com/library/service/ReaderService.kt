package com.library.service

import com.library.dto.request.UpdateReaderRequest
import com.library.dto.response.FineResponse
import com.library.dto.response.LoanResponse
import com.library.dto.response.ReaderResponse
import com.library.exception.AccessDeniedException
import com.library.exception.EntityNotFoundException
import com.library.repository.ReaderRepository
import com.library.repository.UserAccountRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReaderService(
    private val readerRepository: ReaderRepository,
    private val userAccountRepository: UserAccountRepository,
    private val loanService: LoanService,
    private val fineService: FineService
) {

    @Transactional(readOnly = true)
    fun findAll(): List<ReaderResponse> =
        readerRepository.findAll().map { ReaderResponse.from(it) }

    @Transactional(readOnly = true)
    fun findById(id: UUID, currentUser: UserDetails): ReaderResponse {
        checkAccess(id, currentUser)
        val reader = readerRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Reader", id) }
        return ReaderResponse.from(reader)
    }

    @Transactional
    fun update(id: UUID, request: UpdateReaderRequest): ReaderResponse {
        val reader = readerRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Reader", id) }

        request.fullName?.let { reader.fullName = it }
        request.phone?.let { reader.phone = it }
        request.email?.let { reader.email = it }
        request.maxActiveLoans?.let { reader.maxActiveLoans = it }

        return ReaderResponse.from(readerRepository.save(reader))
    }

    @Transactional(readOnly = true)
    fun getLoans(id: UUID, currentUser: UserDetails): List<LoanResponse> {
        checkAccess(id, currentUser)
        readerRepository.findById(id).orElseThrow { EntityNotFoundException("Reader", id) }
        return loanService.findByReader(id)
    }

    @Transactional(readOnly = true)
    fun getFines(id: UUID, currentUser: UserDetails): List<FineResponse> {
        checkAccess(id, currentUser)
        readerRepository.findById(id).orElseThrow { EntityNotFoundException("Reader", id) }
        return fineService.findByReader(id)
    }

    /**
     * READER может видеть только свои данные.
     * LIBRARIAN видит всё.
     */
    private fun checkAccess(readerId: UUID, currentUser: UserDetails) {
        val isLibrarian = currentUser.authorities
            .any { it.authority == "ROLE_LIBRARIAN" }

        if (isLibrarian) return

        val account = userAccountRepository.findByLogin(currentUser.username)
            ?: throw EntityNotFoundException("UserAccount", currentUser.username)

        val reader = readerRepository.findByUserAccountId(account.id)
            ?: throw EntityNotFoundException("Reader", "for user ${currentUser.username}")

        if (reader.id != readerId) {
            throw AccessDeniedException("You can only access your own data")
        }
    }
}
