//отвечает за требование ТЗ «интеграционные тесты через Testcontainers». Поднимает настоящий PostgreSQL в Docker-контейнере и проверяет что регистрация и вход работают с реальной базой данных.
package com.library.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("library_test")
            withUsername("library")
            withPassword("library")
        }
    }

    @Autowired
    lateinit var mockMvc: MockMvc
// проверяет что вход с правильным логином и паролем возвращает JWT-токены.
    @Test
    fun `login with valid credentials should return 200 and tokens`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"login":"admin","password":"admin123"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }
    }
//проверяет что неправильный пароль возвращает ошибку 401, а не пускает в систему.
    @Test
    fun `login with invalid credentials should return 401`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"login":"admin","password":"wrongpassword"}"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }
//проверяет что без токена нельзя получить список книг.
    @Test
    fun `get books without auth should return 401`() {
        mockMvc.get("/api/v1/books").andExpect {
            status { isUnauthorized() }
        }
    }
//проверяет что регистрация нового читателя проходит успешно и возвращает токен.
    @Test
    fun `register new user should return 201`() {
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"login":"testuser","password":"test123","fullName":"Test User"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { exists() }
        }
    }
//проверяет валидацию: пустые поля должны вернуть ошибку 400, а не сохранить мусор в базу.
    @Test
    fun `register with invalid data should return 400`() {
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"login":"","password":"123","fullName":""}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
