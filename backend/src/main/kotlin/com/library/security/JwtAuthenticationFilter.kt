package com.library.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        extractToken(request)
            ?.takeIf { jwtTokenProvider.validateToken(it) }
            ?.takeIf { !jwtTokenProvider.isRefreshToken(it) }
            ?.let { token ->
                val login = jwtTokenProvider.getLogin(token)
                val role  = jwtTokenProvider.getRole(token)
                val auth  = UsernamePasswordAuthenticationToken(
                    login,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_$role"))
                )
                SecurityContextHolder.getContext().authentication = auth
            }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
}
