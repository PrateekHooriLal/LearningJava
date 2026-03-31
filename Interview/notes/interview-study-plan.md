# SDE 3 / Senior Java Backend — Technical Deep-Dive Study Plan

> **Role:** SDE 3 / Senior Java Backend Engineer
> **Round:** Technical Deep-Dive (9 questions analysed)
> **Duration:** 4 weeks · ~1–1.5 hrs/day

---

## Index

### Weeks at a Glance

| Week | Theme | Questions | Days |
|------|-------|-----------|------|
| Week 1 | Java Concurrency | Q1 (Async), Q6 (Parallelism) | Day 1–5 |
| Week 2 | Spring AOP + Exception Handling | Q2 (Cross-Cutting), Q8 (AOP), Q9 (Production) | Day 6–10 |
| Week 3 | Distributed Systems + Real-Time | Q3 (Real-Time), Q4 (CAP) | Day 11–14 |
| Week 4 | DB Internals + Mock Interview | Q5 (Locking), Q7 (Indexes) | Day 15–20 |

### Topic Index

```
HOW TOPICS CONNECT ............................................. (below)

WEEK 1 — JAVA CONCURRENCY
  Day 1  Thread Model + ExecutorService ...................... Future, Callable, thread pools
  Day 2  Fork/Join + Parallel Streams ........................ work-stealing, commonPool pitfall
  Day 3  CompletableFuture ................................... thenApply/Compose/Combine, allOf, exceptionally
  Day 4  @Async + Virtual Threads ............................ Spring @Async, Java 21 VT, StructuredConcurrency
  Day 5  Week 1 Revision + Practice

WEEK 2 — SPRING AOP + EXCEPTION HANDLING
  Day 6  Spring AOP Fundamentals ............................. @Aspect, @Around, @AfterThrowing, pointcuts
  Day 7  Filters vs Interceptors vs AOP ...................... execution order, JWT placement, MDC setup
  Day 8  @ControllerAdvice + Exception Hierarchy ............. RuntimeException vs checked, global handler
  Day 9  Production Exception Handling ....................... MDC, structured logging, distributed tracing
  Day 10 Week 2 Revision

WEEK 3 — DISTRIBUTED SYSTEMS
  Day 11 CAP Theorem + PACELC ................................ CP vs AP, real system examples
  Day 12 Real-Time Communication ............................. WebSocket, SSE, Long Polling, gRPC, scaling
  Day 13 Kafka Deep Dive ..................................... partitions, consumer groups, ordering, guarantees
  Day 14 Week 3 Revision

WEEK 4 — DB INTERNALS + MOCK
  Day 15 Index Types in RDBMS ................................ Clustered, Non-Clustered, Composite, Hash, Full-Text
  Day 16 Optimistic vs Pessimistic Locking ................... @Version, @Lock, BigDecimal.compareTo(), retry
  Day 17 Full Revision I ..................................... Concurrency + Spring
  Day 18 Full Revision II .................................... Distributed + DB
  Day 19 Mock Interview ...................................... 5 questions, timed, spoken answers
  Day 20 Weak Spots + Final Revision

MOCK QUESTION BANK (20 Questions) ............................. SDE 3 difficulty
RECOMMENDED BOOKS
```

---
> **Philosophy:** Foundational first. Each week builds on the last.

---

## How Topics Connect

```
┌─────────────────────────────────────────────────────────────────┐
│                     THREAD MODEL                                │
│  Thread → ExecutorService → Fork/Join → Virtual Threads         │
│                    ↓                                            │
│             CompletableFuture  ←──────  @Async                  │
│                    ↓                                            │
│         Parallelism Patterns (Q1, Q6)                           │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SPRING INTERNALS                               │
│   AOP Proxy → @Aspect → @Around / @AfterThrowing               │
│       ↓               ↓                                         │
│  @ControllerAdvice   Filters / Interceptors (Q2, Q8)            │
│       ↓                                                         │
│  Exception Hierarchy → Structured Logging → MDC (Q9)            │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│             DISTRIBUTED SYSTEMS                                 │
│   CAP Theorem → CP vs AP → PACELC → real systems (Q4)          │
│       ↓                                                         │
│   Real-Time: WebSocket · SSE · Kafka · gRPC · Long Poll (Q3)    │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│               DB INTERNALS                                      │
│   Index types → Query plans → Locking strategies (Q5, Q7)      │
│   Optimistic (@Version) ←→ Exception Handling (Q9)             │
│   Pessimistic (@Lock) ←→ Parallelism / Deadlock risk (Q6)      │
└─────────────────────────────────────────────────────────────────┘
```

**Key cross-topic connections to mention in interviews:**
- `CompletableFuture` + `@Async` + Virtual Threads → all solve async but at different abstraction levels
- `@Around` AOP → can wrap any method with retry/logging/tracing → ties into production exception handling
- Optimistic locking failure → `ObjectOptimisticLockingFailureException` → ties into exception resolution strategies
- Kafka → async real-time → parallelism (consumer groups) → partition ordering → CAP (AP system)
- Index type choice → directly affects query performance under concurrent load → ties into locking strategies

---

## Week 1 — Java Concurrency Foundations

> **Theme:** Thread model, parallelism patterns, async programming
> **Questions covered:** Q1 (Async), Q6 (Parallelism)
> **Daily time:** 1–1.5 hrs

---

### Day 1 — Thread Model + ExecutorService (1 hr)

**Concept Summary**
Java threads map 1:1 to OS threads (platform threads). Creating thousands is expensive.
`ExecutorService` pools threads so you reuse them instead of creating/destroying on every task.

**Key Terms**
```
Thread                  → OS-level execution unit, expensive to create
Runnable / Callable     → task abstraction (Callable returns a value + throws checked exceptions)
ExecutorService         → thread pool manager
Executors.newFixedThreadPool(n)      → fixed N threads
Executors.newCachedThreadPool()      → grows unboundedly, reuses idle threads
Executors.newSingleThreadExecutor()  → 1 thread, tasks run sequentially
ScheduledExecutorService             → run tasks after delay or on schedule
Future<T>               → handle to async result: get() blocks, isDone() polls, cancel() cancels
```

**Coding Exercise**
```java
// Submit 5 tasks to a fixed pool, collect all results
ExecutorService pool = Executors.newFixedThreadPool(3);
List<Future<Integer>> futures = new ArrayList<>();

for (int i = 1; i <= 5; i++) {
    final int taskId = i;
    futures.add(pool.submit(() -> {
        Thread.sleep(100 * taskId);
        return taskId * taskId;
    }));
}

for (Future<Integer> f : futures) {
    System.out.println(f.get()); // blocks until result ready
}
pool.shutdown();
```

**Real-World Scenario**
> An image processing service receives 1000 resize requests per minute.
> Each resize takes ~200ms. How do you choose thread pool size?
>
> Answer: For I/O-bound tasks: pool size = CPU cores × (1 + wait_time/compute_time).
> For CPU-bound tasks: pool size = CPU cores (+ 1 to avoid idle core during GC pause).

**Resources**
- Java Concurrency in Practice — Goetz (Chapter 6: Task Execution)
- Oracle docs: `java.util.concurrent` package
- Baeldung: "Guide to Java ExecutorService"

---

### Day 2 — Fork/Join Framework + Parallel Streams (1 hr)

**Concept Summary**
Fork/Join uses work-stealing: idle threads steal tasks from busy threads' queues.
Parallel Streams use the common ForkJoinPool under the hood.

**Key Terms**
```
ForkJoinPool            → specialised pool with work-stealing
RecursiveTask<T>        → returns a value (use compute() method)
RecursiveAction         → no return value
fork()                  → submit subtask asynchronously
join()                  → wait for subtask result
commonPool()            → shared pool used by parallel streams (size = CPU cores - 1)
parallelStream()        → turns any Collection into a parallel stream
```

**Coding Exercise**
```java
// Merge sort using Fork/Join
class MergeSortTask extends RecursiveAction {
    int[] arr; int lo, hi;

    protected void compute() {
        if (hi - lo < 2) return;
        int mid = (lo + hi) / 2;
        invokeAll(
            new MergeSortTask(arr, lo, mid),
            new MergeSortTask(arr, mid, hi)
        );
        merge(arr, lo, mid, hi);
    }
}

// Parallel stream sum
long sum = LongStream.rangeClosed(1, 1_000_000)
    .parallel()
    .sum();
```

**Real-World Scenario**
> Your team uses `parallelStream()` everywhere for performance. In production, all parallel stream
> tasks start blocking each other. What happened?
>
> Answer: All parallel streams share the common ForkJoinPool. A blocking task (DB call, HTTP)
> occupies a thread and starves other streams. Fix: use a custom ForkJoinPool per heavy operation,
> or switch to CompletableFuture with a dedicated ExecutorService.

**Resources**
- Java docs: `ForkJoinPool`, `RecursiveTask`
- Baeldung: "Guide to the Fork/Join Framework"
- "When to use parallel streams" — Stuart Marks (JEP discussion)

---

### Day 3 — CompletableFuture Deep Dive (1.5 hrs)

**Concept Summary**
`CompletableFuture` enables non-blocking async pipelines. Unlike `Future`, you can chain operations,
combine results, and handle exceptions without blocking a thread on `get()`.

**Key Terms**
```
supplyAsync(Supplier, executor)     → run task async, returns value
thenApply(fn)                       → transform result (like map) — same thread
thenApplyAsync(fn, executor)        → transform on different executor
thenAccept(consumer)                → consume result, no return
thenCompose(fn)                     → flatMap — fn returns another CF
thenCombine(cf2, fn)                → combine two independent CFs
allOf(cf1, cf2, ...)                → wait for ALL to complete
anyOf(cf1, cf2, ...)                → complete when ANY completes
exceptionally(fn)                   → handle exception, provide fallback
handle(fn)                          → handle both result and exception
whenComplete(fn)                    → side-effect on completion (no transform)
```

**Coding Exercise**
```java
ExecutorService pool = Executors.newFixedThreadPool(4);

// Fetch user + fetch orders in parallel, combine
CompletableFuture<User> userFuture =
    CompletableFuture.supplyAsync(() -> userService.findById(1L), pool);

CompletableFuture<List<Order>> ordersFuture =
    CompletableFuture.supplyAsync(() -> orderService.findByUserId(1L), pool);

CompletableFuture<UserOrderDto> combined =
    userFuture.thenCombine(ordersFuture, UserOrderDto::new);

// Exception handling
CompletableFuture<String> safe = combined
    .thenApply(dto -> dto.toString())
    .exceptionally(ex -> "fallback: " + ex.getMessage());

System.out.println(safe.get());
pool.shutdown();
```

**Real-World Scenario**
> A product page needs data from 3 microservices: inventory, pricing, reviews.
> Each call takes ~300ms. Sequential = 900ms. How do you get it under 350ms?
>
> Answer: `CompletableFuture.allOf(inventoryCF, pricingCF, reviewsCF).join()`
> runs all 3 in parallel. Total time ≈ max of the three (~300ms).
> Add `exceptionally` per call for graceful degradation if one service fails.

**Resources**
- Baeldung: "Guide to CompletableFuture"
- Java docs: `java.util.concurrent.CompletableFuture`
- JEP 266 (reactive streams integration context)

---

### Day 4 — @Async + Virtual Threads (1 hr)

**Concept Summary**
`@Async` delegates Spring method calls to an executor — simple but magic (works via AOP proxy).
Virtual Threads (Java 21) are lightweight threads managed by the JVM, not the OS — millions can exist simultaneously.

**Key Terms**
```
@Async                       → runs method in a separate thread (returns Future/CF/void)
@EnableAsync                 → must be on @Configuration class to activate @Async
ThreadPoolTaskExecutor       → Spring's configurable executor for @Async
Virtual Thread               → JVM-managed, extremely lightweight (~1KB stack vs ~1MB platform)
Thread.ofVirtual().start()   → create virtual thread directly
Executors.newVirtualThreadPerTaskExecutor() → pool that creates virtual thread per task
Structured Concurrency       → JEP 453: manage groups of virtual threads as a unit (Java 21)
Carrier thread               → platform thread that "mounts" a virtual thread
```

**Coding Exercise**
```java
// @Async configuration
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("emailExecutor")
    public TaskExecutor emailExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(10);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("email-");
        exec.initialize();
        return exec;
    }
}

@Service
public class EmailService {
    @Async("emailExecutor")     // runs on emailExecutor, not caller's thread
    public CompletableFuture<Void> sendWelcomeEmail(String to) {
        // send email...
        return CompletableFuture.completedFuture(null);
    }
}

// Virtual Thread (Java 21)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var userTask   = scope.fork(() -> userService.findById(1L));
    var orderTask  = scope.fork(() -> orderService.findByUserId(1L));
    scope.join().throwIfFailed();
    return new UserOrderDto(userTask.get(), orderTask.get());
}
```

**Real-World Scenario**
> You have a Spring Boot service making 500 concurrent HTTP calls to a slow third-party API.
> Platform threads are exhausted. What options do you have?
>
> Answer:
> Option 1 (easy): Switch HTTP client to WebClient (reactive, non-blocking).
> Option 2 (Java 21): Use Virtual Threads — `spring.threads.virtual.enabled=true` in Boot 3.2+.
> Option 3: `CompletableFuture` with a properly sized executor.
> Virtual Threads are best here — they block cheaply, no callback complexity.

**Resources**
- JEP 444: Virtual Threads (official)
- Spring docs: Task Execution and Scheduling
- "Java Virtual Threads — A Deep Dive" — Inside Java blog

---

### Day 5 — Week 1 Revision + Practice (1 hr)

**Revision checklist:**
- [ ] Explain difference between `thenApply` and `thenCompose` in one sentence
- [ ] When does `parallelStream()` hurt performance?
- [ ] What is work-stealing?
- [ ] What @Async pitfall causes methods to run synchronously despite the annotation?
  *(Answer: calling `@Async` method from within the same class — Spring proxy is bypassed)*
- [ ] Virtual Threads vs Platform Threads: 3 differences

**Practice problem:**
Write a service that calls 3 external APIs in parallel using `CompletableFuture`,
times out each call at 2 seconds, and falls back to a default value on timeout or error.

---

## Week 2 — Spring Internals: AOP + Exception Handling

> **Theme:** Cross-cutting concerns, exception hierarchy, production logging
> **Questions covered:** Q2 (Cross-Cutting), Q8 (AOP), Q9 (Exception Handling)
> **Daily time:** 1–1.5 hrs

---

### Day 6 — Spring AOP Fundamentals (1.5 hrs)

**Concept Summary**
AOP separates cross-cutting concerns (logging, security, transactions) from business logic.
Spring implements AOP via JDK dynamic proxies (interface-based) or CGLIB proxies (class-based).

**Key Terms**
```
Aspect          → class containing cross-cutting logic (@Aspect)
Join Point      → a point in execution (method call, exception, field access)
                  Spring AOP only supports method-level join points
Pointcut        → expression that matches join points (@Pointcut)
Advice          → action taken at a join point
  @Before       → runs before method
  @After        → runs after method (always, even on exception)
  @AfterReturning → runs after method returns normally
  @AfterThrowing  → runs when method throws exception
  @Around       → wraps method — most powerful, can modify args/return
ProceedingJoinPoint → used in @Around to call the actual method (proceed())
Weaving         → process of applying aspects to target objects (Spring: runtime)
```

**Coding Exercise**
```java
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Pointcut: any method in any class under com.interview.service
    @Pointcut("execution(* com.interview.service..*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        log.info("START {}", method);
        try {
            Object result = pjp.proceed();   // call the actual method
            log.info("END {} in {}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("FAILED {} after {}ms — {}", method, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;   // re-throw so caller gets the exception
        }
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void logException(JoinPoint jp, Exception ex) {
        log.error("Exception in {}: {}", jp.getSignature(), ex.getMessage());
        // Note: AfterThrowing does NOT suppress the exception — it still propagates
    }
}
```

**Real-World Scenario**
> Your team wants to add execution time logging to every service method without touching any service class.
> How do you do it? What if a service method calls another service method in the same class — will it be logged?
>
> Answer: @Around with a service-level pointcut. Intra-class calls (this.method()) bypass the proxy
> → NOT logged. Fix: inject self-reference or restructure so cross-service calls happen between beans.

**Resources**
- Spring docs: Aspect Oriented Programming with Spring
- Baeldung: "Introduction to Spring AOP"
- AspectJ Pointcut Expressions cheat sheet

---

### Day 7 — Filters vs Interceptors vs AOP (1 hr)

**Concept Summary**
Three different interception layers in Spring, each at a different level of the request lifecycle.

**Key Terms**
```
Filter (javax.servlet.Filter)
  → Servlet level, before Spring even sees the request
  → Access to raw HttpServletRequest/Response
  → Use for: CORS, auth tokens, request logging, rate limiting
  → Registered via FilterRegistrationBean or @Component

HandlerInterceptor (Spring MVC)
  → Spring level, after DispatcherServlet but before/after controller
  → preHandle() / postHandle() / afterCompletion()
  → Access to HandlerMethod (knows which controller method will run)
  → Use for: auth checks, locale, model attributes

@Aspect / AOP
  → Spring bean level, any method in any Spring bean
  → Not tied to HTTP at all — works for @Service, @Repository too
  → Use for: logging, transactions, retry, caching, metrics

Order of execution:
  Request → Filter → DispatcherServlet → Interceptor.preHandle()
          → Controller → Interceptor.postHandle() → Interceptor.afterCompletion()
          → Filter → Response
  AOP wraps the Controller or Service method wherever the pointcut matches.
```

**Coding Exercise**
```java
// Interceptor — log each request with correlation ID
@Component
public class CorrelationIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String correlationId = req.getHeader("X-Correlation-Id");
        if (correlationId == null) correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);   // MDC: discussed in Day 9
        res.setHeader("X-Correlation-Id", correlationId);
        return true;   // true = continue processing, false = abort
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        MDC.clear();   // IMPORTANT: clean up MDC to avoid ThreadLocal leak
    }
}

// Register it
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CorrelationIdInterceptor())
                .addPathPatterns("/api/**");
    }
}
```

**Real-World Scenario**
> Where would you implement JWT authentication — Filter, Interceptor, or AOP?
>
> Answer: Filter. Auth must happen before Spring routing. If auth fails, we reject at the
> servlet container level without involving Spring MVC at all. Spring Security implements
> auth as a FilterChain for exactly this reason.

**Resources**
- Spring docs: Filters vs Interceptors
- Baeldung: "Spring MVC HandlerInterceptor vs Filter"

---

### Day 8 — @ControllerAdvice + Exception Hierarchy (1 hr)

**Concept Summary**
Spring's global exception handling separates error responses from controller logic.
Java exception hierarchy determines how Spring handles each type.

**Key Terms**
```
Throwable
  ├── Error               → JVM-level (OutOfMemoryError) — do NOT catch
  └── Exception
        ├── RuntimeException (unchecked) — no forced handling
        │     ├── NullPointerException
        │     ├── IllegalArgumentException
        │     └── DataAccessException (Spring's unchecked DB exceptions)
        └── Checked Exception   — compiler forces handling
              ├── IOException
              └── SQLException (wrapped by Spring into DataAccessException)

@ControllerAdvice    → applies @ExceptionHandler globally to ALL controllers
@RestControllerAdvice → @ControllerAdvice + @ResponseBody (for REST APIs)
@ExceptionHandler    → maps exception type to response
@ResponseStatus      → sets HTTP status on exception class or handler method
ResponseEntityExceptionHandler → base class with handlers for Spring MVC exceptions
  (MethodArgumentNotValidException → 400, HttpMessageNotReadableException → 400, etc.)
```

**Coding Exercise**
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Handle custom business exceptions → 400
    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientBalance(InsufficientBalanceException ex,
                                                    HttpServletRequest req) {
        return ErrorResponse.of(400, ex.getMessage(), req.getRequestURI());
    }

    // Handle JPA optimistic locking failure → 409 Conflict
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                               HttpServletRequest req) {
        return ErrorResponse.of(409, "Data was modified by another request. Please retry.",
                                req.getRequestURI());
    }

    // Catch-all for unexpected exceptions → 500
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return ErrorResponse.of(500, "Internal server error", req.getRequestURI());
    }
}

record ErrorResponse(int status, String message, String path, Instant timestamp) {
    static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(status, message, path, Instant.now());
    }
}
```

**Resources**
- Spring docs: Exception Handling for REST with Spring
- Baeldung: "@ControllerAdvice and @ExceptionHandler"

---

### Day 9 — Production Exception Handling: Logging, MDC, Tracing (1.5 hrs)

**Concept Summary**
In production, exceptions need structured logging, correlation IDs, and distributed tracing
so engineers can find the root cause across 10+ microservices.

**Key Terms**
```
MDC (Mapped Diagnostic Context)
  → ThreadLocal map that SLF4J injects into every log line automatically
  → MDC.put("requestId", id) → all log lines in that thread show requestId
  → MDC.clear() in afterCompletion() — avoid ThreadLocal leak

Structured Logging
  → Log as JSON instead of plain text → indexable in Kibana/Splunk
  → Use Logstash Logback encoder or similar

Distributed Tracing
  → Track a single request across multiple microservices
  → Every request gets a traceId (one per user request) + spanId (one per service hop)
  → Tools: Zipkin, Jaeger, AWS X-Ray
  → Spring Boot + Micrometer Tracing auto-propagates trace headers (B3 format or W3C)

Log levels:
  ERROR → something failed, requires attention
  WARN  → unexpected but handled, may need attention
  INFO  → normal business events (order placed, transfer successful)
  DEBUG → detailed flow (for local debugging only)
  TRACE → very detailed (SQL, byte-level — never in production)

Exception logging rules:
  log.error("Transfer failed for accountId={}", id, ex)  ← always pass exception as last arg
  NEVER: log.error("Failed: " + ex.getMessage())         ← loses stack trace
```

**Coding Exercise**
```java
// application.yml — structured JSON logging
logging:
  pattern:
    console: "%d{ISO8601} [%X{traceId}/%X{spanId}] [%X{requestId}] %-5level %logger{36} - %msg%n"

// Logback JSON encoder (logstash-logback-encoder)
// Every log line: {"timestamp":"...","level":"ERROR","traceId":"abc","requestId":"xyz","message":"..."}

// Service with proper logging
@Service
@RequiredArgsConstructor
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    @Transactional
    public TransferResponse transfer(TransferRequest req) {
        log.info("Transfer initiated: debit={} credit={} amount={}",
                 req.getDebitAccountId(), req.getCreditAccountId(), req.getAmount());
        try {
            // ... transfer logic
            log.info("Transfer completed: debit={} credit={} amount={}",
                     req.getDebitAccountId(), req.getCreditAccountId(), req.getAmount());
            return TransferResponse.success(...);
        } catch (InsufficientBalanceException ex) {
            log.warn("Transfer rejected — insufficient balance: accountId={} balance={}",
                     ex.getAccountId(), ex.getCurrentBalance());
            throw ex;   // let @ControllerAdvice handle the HTTP response
        }
    }
}
```

**Real-World Scenario**
> A customer reports a failed transaction. You have 50 microservices and millions of daily requests.
> How do you find what went wrong?
>
> Answer:
> 1. Customer provides transactionId or timestamp.
> 2. Search Kibana/Splunk by transactionId (in MDC) → find the ERROR log + stack trace.
> 3. Note the traceId from that log.
> 4. Search Zipkin/Jaeger by traceId → see the full call chain across all services.
> 5. Find which service returned an error and why.

**Resources**
- SLF4J MDC docs
- Micrometer Tracing docs (Spring Boot 3)
- "Production Ready Spring Boot" — Baeldung series
- Zipkin quickstart

---

### Day 10 — Week 2 Revision (1 hr)

**Revision checklist:**
- [ ] Difference between `@AfterThrowing` and `@Around` with try-catch
- [ ] When does `@Async` silently run synchronously? (3 cases)
- [ ] Filter vs Interceptor vs AOP: which runs first?
- [ ] What happens if you don't call `MDC.clear()`?
- [ ] `@ControllerAdvice` vs `@ExceptionHandler` in controller — when to use each?

---

## Week 3 — Distributed Systems + Real-Time

> **Theme:** CAP theorem, consistency models, real-time communication patterns
> **Questions covered:** Q3 (Real-Time), Q4 (CAP Theorem)
> **Daily time:** 1–1.5 hrs

---

### Day 11 — CAP Theorem + PACELC (1.5 hrs)

**Concept Summary**
CAP: a distributed system can guarantee at most 2 of: Consistency, Availability, Partition Tolerance.
Since network partitions always happen in practice, the real choice is C vs A during a partition.

**Key Terms**
```
Consistency    → every read gets the most recent write (or an error)
Availability   → every request gets a response (not necessarily the latest data)
Partition      → network split — some nodes can't reach others

CP systems     → prefer consistency during partition (may return error or block)
               → Examples: Zookeeper, HBase, etcd, MongoDB (with strong read concern)

AP systems     → prefer availability during partition (may return stale data)
               → Examples: Cassandra, CouchDB, DynamoDB, Kafka (for message delivery)

CA systems     → possible only without partitions (not realistic for distributed systems)
               → Single-node RDBMS (MySQL standalone) is effectively CA

PACELC (extension):
  If Partition → choose C or A
  Else (no partition) → choose Latency or Consistency
  Most systems are either PC/EC (high consistency) or PA/EL (high availability + low latency)

Eventual Consistency
  → AP systems converge to same state eventually (no partition case)
  → Cassandra: writes go to N nodes, reads from R nodes, consistent when R+W > N

Strong Consistency
  → All nodes see same value at same time
  → Achieved with: 2-phase commit, Raft consensus, Paxos

Read-Your-Writes Consistency
  → After a write, the same client always reads their own write
  → Common in user-facing features (profile update visible immediately to self)
```

**Real System Examples**

| System | Model | Why |
|---|---|---|
| MySQL (single node) | CA | No partition possible on single node |
| MySQL (Galera cluster) | CP | Synchronous replication, blocks on partition |
| Cassandra | AP / EL | Tunable: reads from 1 node = fast + AP |
| Kafka | AP | Messages available even if some brokers down |
| Zookeeper | CP | Leader election requires quorum — blocks if no quorum |
| Redis (Cluster) | AP | Accepts writes, sync later — may lose data on crash |
| etcd | CP | Uses Raft — strong consistency for config store |

**Real-World Scenario**
> Your team is building a bank ledger. Should you use Cassandra or PostgreSQL?
>
> Answer: PostgreSQL (CP). Bank transactions need strong consistency — you cannot show
> a user a stale balance and risk a double-spend. Cassandra's AP model is great for
> product catalogues or analytics where eventual consistency is acceptable.

**Resources**
- "CAP Theorem Revisited" — Robert Greiner
- "PACELC" — Daniel Abadi paper
- Martin Kleppmann: "Designing Data-Intensive Applications" (Ch. 5, 9)

---

### Day 12 — Real-Time Communication Patterns (1.5 hrs)

**Concept Summary**
Five ways to push data to clients in real time, each with different trade-offs.

**Key Terms + When to Use**

```
1. Long Polling
   Client sends request → server holds it until data is ready → client immediately re-requests
   Use: simple real-time notifications, legacy system support
   Downside: high connection overhead, latency = polling interval

2. Server-Sent Events (SSE)
   One-way: server pushes events to client over a persistent HTTP connection
   Client uses EventSource API, automatic reconnect built-in
   Use: live dashboards, news feeds, stock tickers
   Limitation: HTTP/1.1 = 6 connections per browser; HTTP/2 = multiplexed (no limit)

3. WebSocket
   Full-duplex: both client and server can send at any time, single TCP connection
   Use: chat, collaborative editing, multiplayer games, real-time trading
   Spring: @EnableWebSocket, @MessageMapping (STOMP over WebSocket)
   Downside: stateful — hard to scale horizontally, need sticky sessions or pub/sub

4. Kafka (async messaging)
   Not client-facing — service-to-service async event streaming
   Use: order events, audit logs, inter-microservice communication
   Consumer groups: multiple services read same topic independently
   Replay: offset-based — can replay any historical event

5. gRPC (Google Remote Procedure Call)
   HTTP/2 + Protobuf — binary, typed, fast
   Supports streaming: unary, server-stream, client-stream, bidirectional
   Use: internal microservice communication requiring low latency + schema enforcement
   Downside: harder to debug (binary), browser support limited (needs gRPC-Web)
```

**Coding Exercise — SSE in Spring Boot**
```java
@RestController
@RequestMapping("/api/events")
public class LiveFeedController {

    // Returns a stream that sends an event every second for 10 seconds
    @GetMapping(value = "/feed", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> liveFeed() {
        return Flux.interval(Duration.ofSeconds(1))
                   .take(10)
                   .map(tick -> "Event #" + tick + " at " + Instant.now());
    }
}
```

**Real-World Scenario**
> You are building a food delivery app. The customer needs to see their order status update
> in real time (Placed → Preparing → On the Way → Delivered). What do you use?
>
> Answer:
> - Delivery driver location updates (frequent, bidirectional) → WebSocket
> - Order status changes (infrequent, server-to-client only) → SSE (simpler, auto-reconnect)
> - Internal service events (order service → notification service) → Kafka
> Interviewer follow-up: How do you scale WebSocket to multiple pods?
> Answer: Use Redis Pub/Sub or Kafka as a message broker between pods. Each pod
> subscribes to the customer's channel and forwards to the connected WebSocket session.

**Resources**
- Spring docs: WebSocket support
- Baeldung: "Server-Sent Events in Spring"
- Apache Kafka docs: Consumer Groups
- gRPC.io: Core Concepts

---

### Day 13 — Kafka Deep Dive (1 hr)

**Key Terms**
```
Topic         → named stream of events (like a table in a DB)
Partition     → ordered log within a topic, unit of parallelism
Offset        → position of a message in a partition (immutable, sequential)
Consumer Group → set of consumers that share a topic — each partition goes to ONE consumer
Replication   → each partition has N replicas on different brokers (fault tolerance)
Leader        → partition replica that handles all reads and writes
ISR           → In-Sync Replicas — replicas caught up with leader

Delivery guarantees:
  At-most-once  → consumer commits offset before processing (may lose messages on crash)
  At-least-once → consumer commits offset after processing (may duplicate on retry)
  Exactly-once  → Kafka transactions + idempotent producer (most complex)

Ordering:
  Guaranteed within a partition.
  NOT guaranteed across partitions.
  Same key → always same partition → ordered delivery for that key.
```

**Real-World Scenario**
> Your order service publishes to a Kafka topic. Two consumer services (notification + analytics)
> both need every event. How do you configure this?
>
> Answer: Two separate consumer groups. Each group gets all messages independently.
> Consumer groups do NOT share partitions — each group gets its own copy of the full topic.

**Resources**
- Kafka docs: Introduction
- Confluent: "Kafka: The Definitive Guide" (free PDF)

---

### Day 14 — Week 3 Revision (1 hr)

**Revision checklist:**
- [ ] Can a system be both CP and AP? Explain.
- [ ] CAP choice for: payment ledger / social feed / config store / chat messages
- [ ] SSE vs WebSocket: 3 differences and when to choose each
- [ ] How does Kafka guarantee ordering?
- [ ] How do you scale WebSocket connections across multiple pods?

---

## Week 4 — DB Internals + Mock Interview

> **Theme:** Indexes, locking, full revision
> **Questions covered:** Q5 (Locking), Q7 (Indexes)
> **Daily time:** 1–1.5 hrs

---

### Day 15 — Index Types in RDBMS (1.5 hrs)

**Concept Summary**
Indexes speed up reads at the cost of write overhead and storage.
Wrong index choice is one of the most common causes of production slowdowns.

**Key Terms**

```
Clustered Index
  → Rows stored in index order on disk (the table IS the index)
  → Only ONE per table (can't store rows in two orders at once)
  → Primary key in MySQL/InnoDB is always clustered
  → Fast for range scans on primary key: WHERE id BETWEEN 100 AND 200
  → Slow inserts of random PKs (UUID) → page splits → fragmentation
  → Tip: use sequential PKs (BIGINT AUTO_INCREMENT) for InnoDB

Non-Clustered (Secondary) Index
  → Separate data structure, stores indexed column + pointer to actual row
  → Multiple allowed per table
  → "Double lookup": index → PK → row (InnoDB)

Composite Index
  → Index on multiple columns: CREATE INDEX idx ON orders(status, created_at)
  → Left-prefix rule: usable for queries on (status), (status, created_at)
                       NOT for queries on (created_at) alone
  → Column order matters: put equality columns first, range columns last

Partial Index (Filtered Index)
  → Index only a subset of rows: CREATE INDEX idx ON orders(status) WHERE status = 'PENDING'
  → Smaller, faster — only when filter condition matches

Hash Index
  → Exact equality lookups only (=, IN) — O(1)
  → Cannot do range scans (>, <, BETWEEN, LIKE)
  → Memory engine in MySQL, used internally by PostgreSQL hash joins

Bitmap Index
  → Stores bitmask per distinct value — very efficient for low-cardinality columns
  → e.g., gender (M/F), status (PENDING/ACTIVE/CLOSED)
  → Used in data warehouses (Oracle, PostgreSQL partial), NOT for OLTP (write contention)

Full-Text Index
  → Tokenises text for keyword search — "MATCH ... AGAINST ..." in MySQL
  → Supports relevance scoring, stemming, stop words
  → Use when: search box queries, document search
  → Alternative: Elasticsearch for heavy text search workloads
```

**Real-World Scenario**
> A query `SELECT * FROM orders WHERE customer_id = 5 AND status = 'PENDING' ORDER BY created_at`
> is slow. How do you index it?
>
> Answer: Composite index on `(customer_id, status, created_at)`.
> Equality columns (customer_id, status) first, range/sort column (created_at) last.
> This covers the WHERE clause and avoids a filesort for ORDER BY.
> Verify with `EXPLAIN` — look for "Using index" in Extra column, no "filesort".

**Resources**
- "Use The Index, Luke" — Markus Winand (free online)
- MySQL docs: How MySQL Uses Indexes
- Percona blog: Composite Index Best Practices

---

### Day 16 — Optimistic vs Pessimistic Locking (1.5 hrs)

**Concept Summary**
Both prevent lost updates in concurrent scenarios. Choice depends on conflict probability.

**Key Terms**
```
Lost Update Problem:
  Thread A reads balance=100, Thread B reads balance=100
  Thread A writes balance=90 (deducted 10)
  Thread B writes balance=80 (deducted 20) ← OVERWRITES Thread A's write
  Final: 80 (should be 70)

Optimistic Locking
  → No DB lock held during the transaction
  → Detects conflict at write time using a version field
  → If version mismatch → throw OptimisticLockingFailureException → caller retries
  → JPA: @Version on entity field (int or Long) — Hibernate auto-increments + checks
  → Use when: low contention, reads >> writes (e.g., product catalog, user profile)
  → Advantage: no lock timeout, high concurrency for reads
  → Disadvantage: retry logic needed, bad for high-write contention

Pessimistic Locking
  → DB lock held for the duration of the transaction (no other transaction can read/write)
  → PESSIMISTIC_WRITE: SELECT ... FOR UPDATE — exclusive lock
  → PESSIMISTIC_READ: SELECT ... FOR SHARE — shared lock (others can read, not write)
  → Use when: high contention, write-heavy (e.g., bank account balance, seat reservation)
  → Advantage: no retry needed, deterministic
  → Disadvantage: deadlock risk, reduced throughput, lock timeout needed

JPA annotations:
  @Version              → enables optimistic locking (on entity field)
  @Lock(PESSIMISTIC_WRITE) → on repository method → SELECT FOR UPDATE
```

**Coding Exercise**
```java
// Optimistic locking
@Entity
public class BankAccount {
    @Id Long id;
    BigDecimal balance;

    @Version
    int version;    // Hibernate checks: UPDATE WHERE id=? AND version=?
                    // If 0 rows affected → someone else updated → throws exception
}

// Pessimistic locking in repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BankAccount a WHERE a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@Param("id") Long id);
    // Generates: SELECT ... FROM bank_account WHERE id = ? FOR UPDATE
}

// Retry on optimistic locking failure (Spring Retry)
@Retryable(
    retryFor = ObjectOptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
@Transactional
public void updateBalance(Long id, BigDecimal amount) {
    BankAccount account = repo.findById(id).orElseThrow();
    account.setBalance(account.getBalance().add(amount));
    repo.save(account);
}
```

**Real-World Scenario**
> Two users simultaneously try to book the last seat on a flight.
> Should you use optimistic or pessimistic locking?
>
> Answer: Pessimistic locking (`SELECT FOR UPDATE`). Seat reservation has HIGH contention —
> multiple users competing for same row simultaneously. Optimistic would cause many retries and
> poor UX. Pessimistic ensures first transaction wins, second gets a clean "seat unavailable".
>
> For user profile updates (low contention): optimistic is better — rarely do two requests
> update the same profile simultaneously.

**Resources**
- Vlad Mihalcea: "How does Optimistic Locking work in JPA"
- Hibernate docs: Locking
- Baeldung: "Pessimistic Locking in JPA"

---

### Day 17 + 18 — Full Revision (2 × 1 hr)

**Cross-topic review prompts (say answers out loud):**

**Concurrency:**
- Difference between `thenApply` and `thenCompose` with example
- Three ways to achieve parallelism — when to use each
- What is work-stealing? Which Java class uses it?
- Virtual Thread vs platform thread: 3 differences + one use case for each

**Spring AOP:**
- Filter vs Interceptor vs AOP: order, use case, access level
- AOP proxy types: JDK dynamic proxy vs CGLIB — when does Spring use each?
- `@Around` vs `@AfterThrowing`: can both suppress exceptions?

**Distributed:**
- CAP: classify Kafka, Cassandra, Zookeeper, Redis
- WebSocket vs SSE: when does SSE fail?
- Kafka: how do you guarantee ordered processing of all events for one user?

**DB:**
- Left-prefix rule: index on (a, b, c) — which queries use it?
- Bitmap index: why can't you use it for OLTP writes?
- Optimistic locking: what HTTP status code should you return on version conflict?

---

### Day 19 — Mock Interview (1.5 hrs)

Set a timer. Answer each question as if speaking to an interviewer. Aim for 3–5 minutes each.

Pick 5 questions from the mock question bank below and answer them out loud or write them down.

---

### Day 20 — Weak Spots + Final Revision (1 hr)

Revisit whichever topics felt weakest during the mock. Re-read that day's section.
Run the coding exercises from Day 3 (CompletableFuture), Day 6 (AOP), Day 16 (locking) from memory.

---

## Mock Interview Question Bank — SDE 3 Level (20 Questions)

### Java Concurrency (Q1, Q6)

1. `CompletableFuture.thenApply` vs `thenCompose` — explain the difference with a real use case.
2. You have a method annotated with `@Async` that is called from the same class. Does it run asynchronously? Why?
3. A service uses `parallelStream()` and experiences thread starvation in production. What is the root cause and how do you fix it?
4. Explain work-stealing in ForkJoinPool. How does it improve throughput compared to a standard thread pool?
5. Compare platform threads vs virtual threads on: memory cost, blocking behaviour, and recommended use case.

### Spring AOP + Cross-Cutting (Q2, Q8)

6. You want to log the execution time of every method in `com.company.service`. Write the `@Aspect` class.
7. A `@Transactional` method throws a checked exception. Does the transaction roll back? How do you fix it if you want rollback?
8. Where in the request lifecycle does a `Filter` run vs a `HandlerInterceptor`? Give one use case for each.
9. `@ControllerAdvice` vs inline `@ExceptionHandler`: when would you prefer one over the other?
10. Your AOP `@Around` advice is not intercepting a method call. Name three reasons why this could happen.

### Distributed Systems (Q3, Q4)

11. Classify these systems as CP or AP and justify: Kafka, Cassandra, Zookeeper, Redis Cluster.
12. You need real-time order tracking in a mobile app. Walk through your tech choice: WebSocket, SSE, or Kafka. What are the scaling implications?
13. A Kafka consumer is processing messages slowly and lag is growing. What strategies do you apply?
14. How does eventual consistency work in Cassandra? What tuning parameters control the consistency level?
15. Explain PACELC. Give an example of a PA/EL system and a PC/EC system you have worked with or studied.

### DB Internals + Locking (Q5, Q7)

16. Explain the left-prefix rule for composite indexes with an example query that uses the index and one that does not.
17. Why does using UUID as a primary key cause performance issues in MySQL InnoDB? What is the fix?
18. Two concurrent transactions are trying to update the same bank account balance. Walk through optimistic locking: what happens at the DB level and what does your Java code do on conflict?
19. When would you use a partial (filtered) index? Give a concrete table and query example.
20. A senior engineer suggests using pessimistic locking for a high-traffic e-commerce cart. What are the risks? How do you decide between optimistic and pessimistic for this case?

---

## Recommended Books

| Book | Covers |
|---|---|
| Java Concurrency in Practice — Goetz | Threads, executors, CompletableFuture foundations |
| Designing Data-Intensive Applications — Kleppmann | CAP, replication, Kafka, consistency models |
| Use The Index, Luke — Winand (free online) | All index types, query planning, composite indexes |
| Effective Java — Bloch | Best practices including static factories, exception handling |
| Spring in Action — Walls | Spring AOP, MVC, Data JPA in depth |
