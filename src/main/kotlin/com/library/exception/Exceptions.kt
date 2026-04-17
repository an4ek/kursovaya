//описывает два типа ошибок: EntityNotFoundException (книга не найдена — 404) и BusinessRuleException (книга уже выдана, есть штрафы — 400).
package com.library.exception

sealed class LibraryException(message: String) : RuntimeException(message)

class EntityNotFoundException(entity: String, id: Any) :
    LibraryException("$entity with id '$id' not found")

class BusinessRuleException(message: String) : LibraryException(message)

class AccessDeniedException(message: String) : LibraryException(message)

class ConflictException(message: String) : LibraryException(message)
