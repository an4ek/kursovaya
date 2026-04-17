//отвечает за требование ТЗ «единый формат ошибок (@ControllerAdvice)». Перехватывает все ошибки приложения и возвращает понятный JSON вместо страшной Java-ошибки. Клиент всегда получает одинаковую структуру: код ошибки, сообщение, время.
package com.library.exception
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException as SpringAccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(ex: EntityNotFoundException) =
        error(HttpStatus.NOT_FOUND, ex.message ?: "Not found")
    @ExceptionHandler(BusinessRuleException::class)
    fun handleBusinessRule(ex: BusinessRuleException) =
        error(HttpStatus.UNPROCESSABLE_ENTITY, ex.message ?: "Business rule violated")
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException) =
        error(HttpStatus.CONFLICT, ex.message ?: "Conflict")
    @ExceptionHandler(AccessDeniedException::class, SpringAccessDeniedException::class)
    fun handleAccessDenied(ex: Exception) =
        error(HttpStatus.FORBIDDEN, ex.message ?: "Access denied")
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation failed",
                message = "Request validation error",
                details = details
            )
        )
    }
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception) =
        error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error")
    private fun error(status: HttpStatus, message: String) =
        ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), error = status.reasonPhrase, message = message)
        )
}