# Система управления библиотекой

Веб-приложение для автоматизации библиотечных процессов: учёт книжного фонда, выдач и возвратов экземпляров, контроль задолженностей и штрафов.

## Быстрый старт

```bash
docker-compose up -d
```

| Сервис | URL |
|--------|-----|
| Фронтенд | http://localhost |
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| Health check | http://localhost:8081/actuator/health |

### Тестовые аккаунты

| Логин | Пароль | Роль |
|-------|--------|------|
| admin | admin123 | LIBRARIAN |
| test | test123 | READER |

---

## Тема: Система управления библиотекой

**Бизнес-контекст:** учет книжного фонда, выдач и возвратов экземпляров.

### Роли и права

| Роль | Права | Реализация |
|------|-------|------------|
| LIBRARIAN | Управляет каталогом и выдачами | SecurityConfig + @PreAuthorize |
| READER | Просматривает каталог, берет и возвращает книги | JWT токен с ролью READER |

### Сущности

| Сущность | Описание | Файл |
|----------|----------|------|
| BookTitle | Название, автор, ISBN | model/BookTitle.kt |
| BookCopy | Инвентарный номер, статус, ссылка на BookTitle | model/BookTitle.kt |
| Reader | Профиль читателя | model/Reader.kt |
| Loan | Выдача, срок возврата, статус | model/Loan.kt |
| Fine | Просрочка/повреждение, сумма, статус оплаты | model/Loan.kt |
| LoanHistory | История изменений выдачи | model/Loan.kt |

### Бизнес-правила

| Правило | Реализация |
|---------|------------|
| Экземпляр в статусе LOANED нельзя выдать повторно | LoanService — проверка статуса BookCopy перед выдачей |
| Возврат закрывает активную выдачу и обновляет статус экземпляра | LoanService.returnLoan() |
| Просрочка формирует штраф | OverdueScheduler — запускается каждые 30 минут |
| У одного читателя ограничено число активных выдач | LoanService — проверка maxActiveLoans |
| Все операции выдачи и возврата трассируемы | LoanHistory — запись при каждой операции |

### Пользовательские сценарии (MVP)

| Сценарий | Реализация |
|----------|------------|
| Библиотекарь добавляет название и экземпляры книги | POST /api/v1/books + POST /api/v1/books/{id}/copies |
| Читатель получает книгу на руки | POST /api/v1/loans/issue |
| Читатель возвращает книгу в срок и с просрочкой | POST /api/v1/loans/{id}/return |
| Библиотекарь получает список активных просрочек | GET /api/v1/loans/overdue |

### Минимальный API-контур

| Метод | URL | Описание |
|-------|-----|----------|
| GET/POST/PUT/DELETE | `/api/v1/books` | CRUD для BookTitle |
| GET/POST | `/api/v1/books/{id}/copies` | CRUD для BookCopy |
| GET/PUT | `/api/v1/readers` | CRUD для Reader |
| POST | `/api/v1/loans/issue` | Выдача книги |
| POST | `/api/v1/loans/{id}/return` | Возврат книги |
| GET | `/api/v1/loans/overdue` | Список просрочек |
| GET | `/api/v1/readers/{id}/loans` | Выдачи читателя |
| GET | `/api/v1/fines` | Список штрафов |

### Критерии приёмки

| Критерий | Статус |
|----------|--------|
| Нельзя выдать занятый экземпляр | Реализовано — проверка в LoanService |
| Возврат корректно меняет статусы выдачи и экземпляра | Реализовано — LoanService.returnLoan() |
| Просрочки и штрафы рассчитываются и отдаются через API | Реализовано — OverdueScheduler + GET /fines |

---

## Технологический стек

| Требование ТЗ | Реализация |
|---------------|------------|
| Kotlin + Spring Boot | Spring Boot 3.2.5, Kotlin 1.9.23, Maven |
| Vue 3 + TypeScript | Vue 3, TypeScript, Pinia, Vue Router |
| Controller -> Service -> Repository, DTO, @Valid, @ControllerAdvice | Controllers.kt, Services, Repositories.kt, Requests.kt, GlobalExceptionHandler.kt |
| REST + OpenAPI/Swagger | SpringDoc 2.5.0 — http://localhost:8081/swagger-ui/index.html |
| PostgreSQL + Spring Data JPA/Hibernate | PostgreSQL 16, JPA репозитории с JOIN FETCH |
| Flyway миграции | V1__init_users.sql, V2__books.sql, V3__loans_fines.sql |
| OneToMany связи, без N+1 | BookTitle->BookCopy, Reader->Loan, JOIN FETCH в LoanRepository |
| Spring Security + JWT + роли | JwtTokenProvider, JwtAuthenticationFilter, роли LIBRARIAN/READER |
| Unit + интеграционные тесты Testcontainers | BookServiceTest (MockK), AuthIntegrationTest (Testcontainers) |
| Dockerfile backend + frontend + docker-compose | Dockerfile, frontend/Dockerfile, docker-compose.yml |
| Redis + @Cacheable | RedisConfig, @Cacheable в BookService.findAll() |
| @Async + @Scheduled | NotificationService (@Async), OverdueScheduler (@Scheduled каждые 30 мин) |
| GitHub Actions CI/CD | .github/workflows/ci.yml — 4 джоба: test-backend, build-backend, lint-frontend, build-frontend |
| Spring Actuator | /actuator/health, /actuator/metrics |

---

## Структура проекта

src/
├── main/kotlin/com/library/
│   ├── config/          # AsyncConfig, RedisConfig, SecurityConfig
│   ├── controller/      # REST контроллеры
│   ├── dto/             # Request/Response DTO
│   ├── exception/       # Исключения и GlobalExceptionHandler
│   ├── model/           # JPA сущности
│   ├── repository/      # Spring Data репозитории
│   ├── scheduler/       # OverdueScheduler
│   ├── security/        # JWT фильтр и провайдер
│   └── service/         # Бизнес-логика
└── main/resources/
    └── db/migration/    # Flyway миграции V1-V3

frontend/src/
├── api/             # HTTP клиенты
├── components/      # StatusBadge
├── stores/          # Pinia (auth)
├── router/          # Маршруты
└── views/           # Страницы

Dockerfile               # Backend образ
docker-compose.yml       # Все сервисы
.github/workflows/ci.yml # CI/CD pipeline

## Тесты

```bash
# Unit тесты
./mvnw test -Dtest="BookServiceTest"

# Все тесты (нужен Docker)
./mvnw test
```