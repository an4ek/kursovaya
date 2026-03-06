package com.library.config

import com.library.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Публичные эндпоинты
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                    // Каталог книг — читать может любой авторизованный
                    .requestMatchers(HttpMethod.GET, "/api/v1/books/**").authenticated()

                    // Управление каталогом — только библиотекарь
                    .requestMatchers(HttpMethod.POST, "/api/v1/books/**").hasRole("LIBRARIAN")
                    .requestMatchers(HttpMethod.PUT, "/api/v1/books/**").hasRole("LIBRARIAN")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").hasRole("LIBRARIAN")
                    .requestMatchers("/api/v1/copies/**").hasRole("LIBRARIAN")

                    // Выдачи — только библиотекарь
                    .requestMatchers("/api/v1/loans/**").hasRole("LIBRARIAN")

                    // Читатели — библиотекарь (детальный контроль — в сервисах)
                    .requestMatchers("/api/v1/readers/**").authenticated()

                    // Штрафы
                    .requestMatchers("/api/v1/fines/**").authenticated()

                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:5173", "http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
