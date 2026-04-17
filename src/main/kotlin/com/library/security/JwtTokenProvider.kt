//создаёт JWT-токен при входе (содержит логин и роль пользователя, живёт 24 часа) и проверяет его подлинность при каждом запросе с помощью HMAC-SHA256 подписи.
package com.library.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long,
    @Value("\${app.jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(login: String, role: String): String =
        buildToken(login, role, expirationMs)

    fun generateRefreshToken(login: String, role: String): String =
        buildToken(login, role, refreshExpirationMs, isRefresh = true)

    private fun buildToken(
        login: String,
        role: String,
        ttlMs: Long,
        isRefresh: Boolean = false
    ): String = Jwts.builder()
        .subject(login)
        .claim("role", role)
        .claim("type", if (isRefresh) "refresh" else "access")
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + ttlMs))
        .signWith(signingKey)
        .compact()

    fun validateToken(token: String): Boolean = runCatching {
        parseClaims(token)
        true
    }.getOrDefault(false)

    fun getLogin(token: String): String =
        parseClaims(token).subject

    fun getRole(token: String): String =
        parseClaims(token)["role"] as String

    fun isRefreshToken(token: String): Boolean =
        parseClaims(token)["type"] == "refresh"

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
