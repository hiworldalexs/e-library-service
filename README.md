# e-library-service

## 1. Assumptions

### Technical

- Authentication and authorization are out of scope. The `X-User-Id` header identifies the user and means the request is authenticated and authorized.
- Proof of concept. The database is in-memory and the datasource is configurable. No database migration tool. Hibernate applies the schema.
- Book and loan ids are UUID strings stored as `VARCHAR(36)`, not a native UUID column type.
- The `demo` profile inserts demo data and enables the H2 console and Swagger UI for local inspection. They are unsecured and out of scope for production. Swagger UI serves the existing OpenAPI contract.
- Title and author search is a case-insensitive substring match (`LIKE %term%`). Search performance is out of scope for this proof of concept.
- Borrow locks the book row. Return uses an optimistic lock on the loan. Reads do not lock.

### Business

- "Consume digital content such as books and journals" is ignored.
- Creating, updating, and deleting books is skipped. Focus stays on book handling.
- Reservations, renewals, and fines are skipped.
- An overdue loan stays active until returned. It still occupies a copy and counts toward the borrow limit.
- The loan period (elapsed days) and the maximum number of concurrent borrows are configurable. Defaults reference the Hong Kong Public Libraries: 14 days and 10 items.

## 2. Functional Requirements

- Browse books
  - Offset pagination
  - Provide search, filter, and sort features
- Retrieve book details
  - Include available copies (`totalCopies` minus the count of active loans for that book)
- Borrow a book
  - Check availability
  - At most one active loan per user and book. Borrowing again while that loan is active returns `BOOK_ALREADY_BORROWED`
  - A user with an overdue loan cannot borrow. There is no penalty. This returns `USER_HAS_OVERDUE_LOAN`
  - Requires `X-User-Id`
- Return a book
  - Only if the user currently holds an active loan for it
  - Sets `returnedAt`. Loan history is kept
  - Requires `X-User-Id`
- View currently borrowed books
  - List active loans with due dates
  - Requires `X-User-Id`
  - No pagination

## 3. Non-Functional Requirements

- Maintainability
  - Code is simple and easy to read
  - Clear separation of concerns across layers
  - Business rules are configurable without code changes
- Testability
  - Layers are loosely coupled and can be tested independently
  - Time-dependent logic uses an injectable Clock
- Reliability
  - Consistent error handling with structured error responses
  - Business rules (availability, borrow limits, ownership) are enforced
- Configurability
  - Persistence and business parameters can be adjusted via configuration

## 4. Entity Model Design

**Book** (`books`)

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | UUID string |
| `title` | `String` | Required |
| `author` | `String` | Required |
| `category` | `String` | Required |
| `totalCopies` | `int` | Required |
| `createdAt` | `Instant` | Set on insert |
| `updatedAt` | `Instant` | Set on insert and update |

**Loan** (`loans`)

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `String` | UUID string |
| `userId` | `String` | Caller identity from `X-User-Id` |
| `bookId` | `String` | References `Book.id` (no JPA association; join in queries when needed) |
| `borrowedAt` | `Instant` | UTC |
| `dueDate` | `Instant` | UTC |
| `returnedAt` | `Instant` | `null` = active |
| `version` | `Long` | Optimistic lock. Incremented when the loan is updated |
| `createdAt` | `Instant` | Set on insert |
| `updatedAt` | `Instant` | Set on insert and update |

Indexes: `userId`, `bookId` on `loans` for active-loan lookups.

## 5. API Design

Contract: [`src/main/resources/openapi/openapi.yaml`](src/main/resources/openapi/openapi.yaml)

| Method | Path | Auth header | Summary |
| --- | --- | --- | --- |
| `GET` | `/books` | — | Browse / search / filter / sort (paged) |
| `GET` | `/books/{bookId}` | — | Book details + `availableCopies` |
| `POST` | `/books/{bookId}/borrow` | `X-User-Id` | Borrow |
| `POST` | `/books/{bookId}/return` | `X-User-Id` | Return |
| `GET` | `/loans` | `X-User-Id` | Active loans for the user |

## 6. Tech Stack

| Piece | Choice |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot **4.1.2** |
| Build | Maven (wrapper included) |
| API | OpenAPI 3 contract-first |
| Persistence | Spring Data JPA + H2 (in-memory by default, datasource configurable) |
| Validation | `spring-boot-starter-validation` + OpenAPI Bean Validation |
| Tests | JUnit 5 + AssertJ |

## 7. Error Codes

| errorCode | HTTP |
| --- | --- |
| `INVALID_REQUEST` | 400 |
| `UNAUTHENTICATED` | 401 |
| `BOOK_NOT_FOUND` | 404 |
| `BOOK_NOT_AVAILABLE` | 409 |
| `BORROW_LIMIT_EXCEEDED` | 409 |
| `USER_HAS_OVERDUE_LOAN` | 409 |
| `BOOK_ALREADY_BORROWED` | 409 |
| `BOOK_NOT_BORROWED_BY_USER` | 409 |
| `OPTIMISTIC_LOCK_ERROR` | 409 |
| `INTERNAL_SERVER_ERROR` | 500 |

## 8. How to Run

Normal run:

```bash
./mvnw spring-boot:run
```

The `demo` profile inserts demo data (`classpath:data-demo.sql`), enables the H2 console, and enables Swagger UI:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

- Server: `http://localhost:8080`
- Swagger UI (`demo` profile): `http://localhost:8080/swagger-ui.html`
- H2 console (`demo` profile): `http://localhost:8080/h2-console`
- JDBC URL (default in-memory DB): `jdbc:h2:mem:e-library-service`

The default database is in-memory, so each start is empty and the demo data loads cleanly.

## 9. Tests

```bash
./mvnw test
```