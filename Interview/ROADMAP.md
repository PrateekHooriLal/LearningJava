# Implementation Roadmap — Pending Modules

> These 3 modules are the only items from earlier plans **not yet implemented**.
> Everything else (62 LearnJava files, LLD, Design Patterns, Java 21, Concurrency) is done.

---

## Module A — Hibernate Deep Dive (extend FruitWareHouse)

**Project:** `FruitWareHouse`
**Goal:** Learn Hibernate entity relationships, N+1 problem, fetch strategies, JPQL, and @Transactional internals through hands-on code — not just theory.

### Step 1 — Entity Relationships
Add two new entities to `FruitWareHouse` to demonstrate all relationship types:

**NEW: `model/Category.java`**
```
@Entity
@OneToMany(mappedBy="category", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
List<Fruit> fruits
```

**Update: `model/Fruit.java`**
```
@ManyToOne(fetch=FetchType.LAZY)
@JoinColumn(name="category_id")
Category category
```

**NEW: `model/Supplier.java`**
```
@ManyToMany with Fruit via @JoinTable
```

Key concepts to explain in Javadoc:
- `mappedBy` — which side owns the FK column
- `CascadeType`: PERSIST, MERGE, REMOVE, ALL
- `orphanRemoval=true` — delete child when removed from parent collection

### Step 2 — N+1 Problem (most-asked Hibernate interview question)
**File:** `repository/FruitRepository.java`

Demonstrate the problem:
```java
// BAD: 1 query for all fruits + N queries for each fruit's category = N+1
List<Fruit> fruits = fruitRepository.findAll();
fruits.forEach(f -> System.out.println(f.getCategory().getName()));
```

Three fixes to implement:
1. `@Query("SELECT f FROM Fruit f JOIN FETCH f.category")` — JPQL JOIN FETCH
2. `@EntityGraph(attributePaths = {"category"})` on repository method
3. `@BatchSize(size=10)` on the collection

### Step 3 — JPQL Queries
Add to `FruitRepository`:
```java
// Named param
@Query("SELECT f FROM Fruit f WHERE f.price > :price ORDER BY f.name")
List<Fruit> findByPriceGreaterThan(@Param("price") double price);

// Aggregation
@Query("SELECT f.name, SUM(t.amount) FROM Fruit f JOIN f.transactions t GROUP BY f.name")
List<Object[]> fruitTransactionSummary();

// Bulk update
@Modifying @Transactional
@Query("UPDATE Fruit f SET f.stockQuantity = :qty WHERE f.id = :id")
void updateStock(@Param("id") Long id, @Param("qty") int qty);
```

### Step 4 — @Transactional Deep Dive
**File:** New `service/TransactionDemoService.java`

Cover:
- **Propagation** (most asked): REQUIRED, REQUIRES_NEW, NESTED, NOT_SUPPORTED
- **Isolation levels**: READ_UNCOMMITTED → SERIALIZABLE (dirty read, phantom read)
- **rollbackFor**: default only rolls back RuntimeException — show checked exception pitfall
- **L1 vs L2 Cache**: Session cache (always on) vs Ehcache/Redis (opt-in)

### Step 5 — Locking Strategies
- **Optimistic** (`@Version`) — already in `Interview/assignment/lead/domain/Wallet.java` — link to it
- **Pessimistic** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) — add to `WalletRepository`

**Interview questions to answer after this module:**
- What is the N+1 problem? How do you detect it (enable SQL logging)? How do you fix it?
- LAZY vs EAGER: what's the default for each relationship type?
- What is LazyInitializationException and how do you fix it?
- What does `mappedBy` mean?
- What is the difference between PROPAGATION.REQUIRED and REQUIRES_NEW?
- What does `@Transactional(rollbackFor = Exception.class)` do differently?

---

## Module B — Spring Security + JWT (fix TransacionManager)

**Project:** `TransacionManager`
**Goal:** Replace the broken `SecurityConfigurer.java` with a complete working JWT implementation.
Dependencies already in pom.xml: `spring-security` + `jjwt 0.9.1`

### Filter Chain Flow (understand this first)
```
Request
  → DelegatingFilterProxy
  → SecurityFilterChain
      → CorsFilter
      → CsrfFilter
      → JwtAuthFilter  ← our custom filter
      → UsernamePasswordAuthenticationFilter
  → DispatcherServlet
  → Controller
```

### Files to Create

**1. `security/UserEntity.java`**
```java
@Entity
String username, password, role  // role: "ROLE_USER", "ROLE_ADMIN"
```

**2. `security/UserRepository.java`**
```java
extends JpaRepository<UserEntity, Long>
Optional<UserEntity> findByUsername(String username);
```

**3. `security/CustomUserDetailsService.java`**
```java
implements UserDetailsService
loadUserByUsername(String username) // throws UsernameNotFoundException
```

**4. `security/JwtUtil.java`** (using jjwt 0.9.1)
```java
String generateToken(UserDetails user)      // Jwts.builder(), HS256, 15min expiry
String extractUsername(String token)        // parse claims
boolean isTokenValid(String token, UserDetails user)  // check username + expiry
Date extractExpiration(String token)
```

**5. `security/JwtAuthFilter.java`** extends `OncePerRequestFilter`
```java
// Extract "Authorization: Bearer <token>" header
// Validate via JwtUtil
// Set UsernamePasswordAuthenticationToken in SecurityContextHolder
```

**6. `security/SecurityConfig.java`** (replace broken SecurityConfigurer.java)
```java
@Bean SecurityFilterChain — disable CSRF, enable CORS
@Bean BCryptPasswordEncoder
@Bean AuthenticationManager
addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
permit: /auth/**, secure: everything else
```

**7. `controller/AuthController.java`**
```
POST /auth/register — save user with BCrypt-encoded password
POST /auth/login    — authenticate, return JWT token
```

**8. `security/AuthEntryPoint.java`** implements `AuthenticationEntryPoint`
```java
// Return 401 JSON instead of redirect on unauthenticated request
```

**9. `security/AccessDeniedHandlerImpl.java`**
```java
// Return 403 JSON for role violations
```

### Method-Level Security
```java
@PreAuthorize("hasRole('ADMIN')")         // on sensitive endpoints
@PostAuthorize("returnObject.username == authentication.name")  // ownership check
```

### Refresh Token Pattern (bonus)
- Access token: 15 min TTL
- Refresh token: 7 days TTL, stored in DB
- `POST /auth/refresh` — validate refresh token, issue new access token
- Token blacklist: store invalidated tokens in Redis Set (or ConcurrentHashSet for in-memory)

**Interview questions to answer after this module:**
- How does Spring Security's filter chain work?
- What is the difference between Authentication and Authorization?
- How does JWT work? What are the 3 parts? How do you invalidate a JWT?
- What is the difference between @PreAuthorize and @Secured?
- How does BCrypt work? Why not MD5/SHA-1?
- What is CSRF? When should you disable it (stateless REST APIs)?

---

## Module C — Wallet Module 2 (Resilience4j + REST layer)

**Project:** `Interview/assignment/lead/`
**Status:** Module 1 complete (Wallet entity + Optimistic Locking + TransferService + Tests)
**Goal:** Complete Module 2 — expose REST API and wrap ExchangeRateService in Circuit Breaker

### What's Already Done (Module 1)
- `domain/Wallet.java` — entity with `@Version` optimistic locking ✅
- `service/TransferServiceImpl.java` — transfer logic with `@Transactional` ✅
- `infrastructure/WalletRepository.java` ✅
- `service/TransferServiceTest.java` — concurrent race condition test ✅

### Step 1 — WalletController (REST Layer)
**File:** `api/WalletController.java` (skeleton exists — fill it in)

```
POST /api/wallets/{fromId}/transfer  — body: TransferRequest (toId, amount, currency)
GET  /api/wallets/{id}/balance       — return balance + currency
GET  /api/wallets/{id}/transactions  — return transaction history
```

Use existing DTOs: `TransferRequest.java`, `TransferResponse.java`

### Step 2 — Resilience4j Circuit Breaker on ExchangeRateService
Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

**File:** `service/MockExchangeRateService.java` (exists — wrap with circuit breaker)
```java
@CircuitBreaker(name = "exchangeRate", fallbackMethod = "fallbackRate")
public BigDecimal getRate(String from, String to) { ... }

// Fallback: use cached rate or default 1:1
public BigDecimal fallbackRate(String from, String to, Exception ex) {
    return cachedRates.getOrDefault(from + to, BigDecimal.ONE);
}
```

**`application.properties` config:**
```properties
resilience4j.circuitbreaker.instances.exchangeRate.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.exchangeRate.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.exchangeRate.sliding-window-size=10
```

### Step 3 — Global Exception Handler
**File:** `api/GlobalExceptionHandler.java`
```java
@RestControllerAdvice
// Handle: InsufficientFundsException → 400
//         WalletNotFoundException    → 404
//         OptimisticLockException    → 409 (concurrent modification)
//         CallNotPermittedException  → 503 (circuit open)
```

### Step 4 — Update PROGRESS.md
Mark Module 2 complete, add Module 3 (observability with Micrometer/Actuator) as optional.

**Interview talking points after this module:**
- What is a Circuit Breaker pattern? What are its 3 states (CLOSED / OPEN / HALF_OPEN)?
- Why not just use try-catch instead of a circuit breaker?
- How do you test circuit breaker behavior?
- What is the difference between Circuit Breaker and Retry?

---

## Suggested Order

| Order | Module | Project | Effort |
|-------|--------|---------|--------|
| 1st | Module C — Wallet Module 2 | Interview/assignment/lead | ~3 hrs (smallest scope, immediate value) |
| 2nd | Module A — Hibernate Deep Dive | FruitWareHouse | ~4 hrs |
| 3rd | Module B — Spring Security + JWT | TransacionManager | ~5 hrs |
