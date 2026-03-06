package com.library.security

import com.library.repository.UserAccountRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class LibraryUserDetailsService(
    private val userAccountRepository: UserAccountRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val account = userAccountRepository.findByLogin(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return User(
            account.login,
            account.passwordHash,
            listOf(SimpleGrantedAuthority("ROLE_${account.role}"))
        )
    }
}
