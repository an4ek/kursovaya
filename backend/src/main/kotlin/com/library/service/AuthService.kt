package com.library.service

import com.library.dto.request.LoginRequest
import com.library.dto.request.RefreshTokenRequest
import com.library.dto.request.RegisterRequest
import com.library.dto.response.AuthResponse
import com.library.exception.BusinessRuleException
import com.library.exception.ConflictException
import com.library.model.Reader
import com.library.model.UserAccount
import com.library.model.UserRole
import com.library.repository.ReaderRepository
import com.library.repository.UserAccountRepository
import com.library.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userAccountRepository: UserAccountRepository,
    private val readerRepository: ReaderRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userAccountRepository.existsByLogin(request.login)) {
            throw ConflictException("User with login '${request.login}' already exists")
        }

        val account = userAccountRepository.save(
            UserAccount(
                login = request.login,
                passwordHash = passwordEncoder.encode(request.password),
                role = UserRole.READER
            )
        )

        readerRepository.save(
            Reader(
                userAccount = account,
                fullName = request.fullName,
                phone = request.phone,
                email = request.email
            )
        )

        return buildAuthResponse(account.login, account.role.name)
    }

    fun login(request: LoginRequest): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.login, request.password)
            )
        } catch (ex: Exception) {
            throw BusinessRuleException("Invalid login or password")
        }

        val account = userAccountRepository.findByLogin(request.login)
            ?: throw BusinessRuleException("User not found")

        if (account.status.name == "BLOCKED") {
            throw BusinessRuleException("Account is blocked")
        }

        return buildAuthResponse(account.login, account.role.name)
    }

    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val token = request.refreshToken

        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw BusinessRuleException("Invalid or expired refresh token")
        }

        val login = jwtTokenProvider.getLogin(token)
        val role  = jwtTokenProvider.getRole(token)

        return buildAuthResponse(login, role)
    }

    private fun buildAuthResponse(login: String, role: String) = AuthResponse(
        accessToken  = jwtTokenProvider.generateAccessToken(login, role),
        refreshToken = jwtTokenProvider.generateRefreshToken(login, role),
        login = login,
        role  = role
    )
}