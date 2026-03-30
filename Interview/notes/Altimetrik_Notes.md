# Altimetrik / Airbnb — Interview Notes

> **Interview date:** Wednesday 2026-04-01
> **Source:** Actual L1 + L2 feedback documents from selected candidates (Nov 2025 – Mar 2026)
> **Code file:** `Interview/src/main/java/com/interview/altimetrik/AltimetrikStreams.java`

---

## What to Read — In This Order

> Read this list top to bottom before the interview. Do NOT skip steps.

```
SUNDAY (today) — Theory only, no code
─────────────────────────────────────────────────────────────────────
 1. This file — read every section once
 2. Gap 1 (HashMap vs LinkedHashMap vs TreeMap) — say the answer out loud
 3. Gap 4 (Decorator pattern) — say the answer + BufferedReader example out loud
 4. Gap 2 (@Primary / @Qualifier) — say the rules out loud
 5. Fault Tolerant vs Resilience table — memorise the ONE key difference
 6. SQS vs Kafka vs RabbitMQ table — memorise the "use when" column

MONDAY — Hands-on coding in Eclipse, NO looking at notes
─────────────────────────────────────────────────────────────────────
 1. Write filter + groupBy + maxBy from memory (target: < 5 minutes)
 2. Print the result map using ifPresent — from memory
 3. Build Bank Transfer API all 6 layers from scratch (target: < 30 min)
 4. Write one Mockito unit test for the service layer

TUESDAY (interview day) — Quick revision only
─────────────────────────────────────────────────────────────────────
 1. Re-read "Mistakes From Practice" section (top of this file)
 2. Re-read Downstream Collector Quick Reference table
 3. Re-read Bank Transfer API layer list
 4. Do NOT read new material — only revision
```

---

## Code Files for This Interview

| File | What it contains |
|---|---|
| `com.interview.altimetrik.AltimetrikStreams.java` | All 4 practice problems — run this |
| `com.interview.entities.Employee.java` | Employee class with getSampleEmployees() |
| `com.streams.basics.StreamBasics.java` | Full stream operations reference |
| `com.streams.collectors.CollectorsGuide.java` | All collectors with examples |
| `com.streams.mistakes.CommonMistakes.java` | 10 WRONG vs CORRECT mistakes, runnable |
| `com.streams.printing.PrintingCollections.java` | How to print Map, List, Map+Optional |

---

## Round Structure

```
L1  →  Spring Boot REST · Java 8 Streams · Spring Data JPA · DSA (1-2 problems)
L2  →  Microservices · Kafka/SQS/RabbitMQ · Testing · Spring DI + Polymorphism
EL1 →  System architecture · Current project walkthrough
```

---

## ⚡ Read This First — Mistakes From Practice

> These exact mistakes were made during practice. Read before every round.

### Mistake 1 — `maxBy` needs a `Comparator`, NOT a method reference

```java
// ❌ WRONG — does not compile
Collectors.maxBy(Employee::getSalary)

// ✅ CORRECT
Collectors.maxBy(Comparator.comparing(Employee::getSalary))
```

**Why:** `maxBy` needs an object that knows HOW to compare (a `Comparator<T>`).
`Employee::getSalary` only extracts a value — it is a `Function<T,R>`, not a `Comparator`.
`Comparator.comparing(Employee::getSalary)` wraps the extractor into a Comparator.

---

### Mistake 2 — Method references never have brackets

```java
// ❌ WRONG
Employee::getDepartment()

// ✅ CORRECT
Employee::getDepartment
```

**Why:** `::` is a REFERENCE to the method — like a pointer. The stream calls it for you.
Adding `()` means calling it right now — that is not what `::` does.

---

### Mistake 3 — `counting()` takes NO arguments

```java
// ❌ WRONG
Collectors.counting(Comparator.comparing(Employee::getSalary))

// ✅ CORRECT
Collectors.counting()
```

**Why:** `counting()` just counts items in a group. No field needed.
Only `maxBy`/`minBy`/`summingInt` need an argument — they need to know WHICH field to use.

---

### Mistake 4 — Getter names are case-sensitive

```java
// ❌ WRONG
e.getexp()    e.getDept()    e.getSal()

// ✅ CORRECT
e.getExperience()    e.getDepartment()    e.getSalary()
```

**Rule:** getter = `"get"` + field name with **first letter capitalised**.

---

### Mistake 5 — Variable name inconsistency in lambda

```java
// ❌ WRONG — defined as empOut, used as empout
map.forEach((dept, empOut) -> empout.ifPresent(...))

// ✅ CORRECT — one name, used consistently
map.forEach((dept, empOpt) -> empOpt.ifPresent(...))
```

---

### Mistake 6 — Arrow syntax has no space inside

```java
// ❌ WRONG
emp - > System.out.println(emp.getName())

// ✅ CORRECT
emp -> System.out.println(emp.getName())
```

---

## Stream Collectors — Quick Reference Table

| What I want | Collector | Return type |
|---|---|---|
| List per group | `groupingBy(key)` | `Map<K, List<V>>` |
| Count per group | `groupingBy(key, counting())` | `Map<K, Long>` |
| Max per group | `groupingBy(key, maxBy(Comparator.comparing(f)))` | `Map<K, Optional<V>>` |
| Min per group | `groupingBy(key, minBy(Comparator.comparing(f)))` | `Map<K, Optional<V>>` |
| Sum per group | `groupingBy(key, summingDouble(f))` | `Map<K, Double>` |
| Max without Optional | `groupingBy(key, collectingAndThen(maxBy(...), opt -> opt.orElse(null)))` | `Map<K, V>` |
| Split true/false | `partitioningBy(predicate)` | `Map<Boolean, List<V>>` |
| Join strings | `joining(", ", "[", "]")` | `String` |

---

## Confirmed L1 Problems

### Problem 1 — Filter + GroupBy + MaxBy ⭐ HIGHEST PRIORITY

> Filter salary > 50k, group by department, find highest paid per department.

```java
Map<String, Optional<Employee>> result = employees.stream()
    .filter(e -> e.getSalary() > 50_000)
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.maxBy(Comparator.comparing(Employee::getSalary))
    ));

// Print — always use ifPresent to safely unwrap Optional
result.forEach((dept, empOpt) ->
    empOpt.ifPresent(emp ->
        System.out.println(dept + " -> " + emp.getName() + " | " + emp.getSalary())
    )
);
```

**Why `Optional<Employee>`?**
After `filter(salary > 50000)` a department may have zero employees left.
`maxBy` of an empty group has no answer → returns `Optional.empty()` instead of throwing.

**Follow-up — unwrap Optional with `collectingAndThen`:**

```java
Map<String, Double> maxSalaryPerDept = employees.stream()
    .filter(e -> e.getSalary() > 50_000)
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.collectingAndThen(
            Collectors.maxBy(Comparator.comparing(Employee::getSalary)),
            opt -> opt.map(Employee::getSalary).orElse(0.0)   // unwrap → just Double
        )
    ));
```

---

### Problem 2 — Count + Sort descending

```java
employees.stream()
    .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()))
    .entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
```

---

### Problem 3 — Common characters across all words

> Input: `["bella", "label", "roller"]` → Output: `[e, l, l]`

```
Algorithm:
  1. Build int[26] frequency array for first word
  2. For each remaining word: build its freq array, take MIN of each index
  3. Expand minFreq back to List<String>

Time: O(n * m)   Space: O(1) — array is always size 26
```

---

### Problem 4 — Character frequency of a string

> Input: `"aabbccsrrtvvaa"` → Output: `a=4 b=2 c=2 r=2 s=1 t=1 v=2`

```java
// Streams approach — show this first
Map<String, Long> freq = Arrays.stream(input.split(""))
    .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
new TreeMap<>(freq).forEach((ch, count) -> System.out.print(ch + "=" + count + " "));

// Array approach — mention as O(1) space optimisation
int[] freq = new int[26];
for (char c : input.toCharArray()) freq[c - 'a']++;
for (int i = 0; i < 26; i++)
    if (freq[i] > 0) System.out.print((char)('a' + i) + "=" + freq[i] + " ");
```

---

## Confirmed L1 Design Problem — Bank Transfer REST API

> Build this from scratch. Know every layer by heart.

```
Layer 1 — Entity
  @Entity BankAccount { id, accountNumber, ownerName, balance }

Layer 2 — Repository
  BankAccountRepository extends JpaRepository<BankAccount, Long>

Layer 3 — DTO (request body)
  AmountTransfer { debitAccountId, creditAccountId, @Positive amount }

Layer 4 — Custom Exception
  InsufficientBalanceException extends RuntimeException

Layer 5 — Service
  @Service AccountService
    @Transactional
    transfer(AmountTransfer dto) {
        fetch debit account or throw
        fetch credit account or throw
        if debit.balance < amount → throw InsufficientBalanceException
        debit.balance -= amount
        credit.balance += amount
        save both
    }

Layer 6 — Controller
  @RestController
    @PostMapping("/transfer")
    ResponseEntity<?> transfer(@Valid @RequestBody AmountTransfer dto)

    @ExceptionHandler(InsufficientBalanceException.class)
    ResponseEntity<String> handleInsufficient(...)
```

**Key talking points:**
- `@Transactional` — both debit + credit roll back atomically if anything fails
- `@Valid` on DTO — fail fast at controller layer, never reaches service
- Idempotency — client sends a UUID header; server caches result to prevent double transfer

---

## Spring Boot Validation

```java
@Valid               // triggers Bean Validation on @RequestBody
@NotNull             // field cannot be null
@NotBlank            // string cannot be blank
@Min(0) @Max(...)    // numeric range
@Positive            // > 0

@ControllerAdvice    // marks class as global exception handler
@ExceptionHandler(SomeException.class)  // handles specific exception
```

---

## Pagination in Spring Data JPA

```java
// Repository
interface EmployeeRepo extends PagingAndSortingRepository<Employee, Long> {}

// Service
Page<Employee> page = repo.findAll(
    PageRequest.of(0, 10, Sort.by("salary").descending())
);

page.getContent();       // List<Employee> on this page
page.getTotalPages();    // total number of pages
page.getTotalElements(); // total record count
```

---

## Gap 1 — HashMap vs LinkedHashMap vs TreeMap ⭐ CANDIDATE GOT THIS WRONG IN L1

| | `HashMap` | `LinkedHashMap` | `TreeMap` |
|---|---|---|---|
| Order | None (random) | Insertion order | Sorted by key |
| Null keys | 1 allowed | 1 allowed | Not allowed |
| `get` / `put` | O(1) avg | O(1) avg | O(log n) |
| Use when | Default key-value store | Need insertion order / LRU cache | Need sorted keys, range queries |

**Interview one-liner:**
> "HashMap for default performance. LinkedHashMap when insertion order matters — like an LRU cache.
> TreeMap when I need keys sorted or want to do range queries like `subMap` or `headMap`."

**Hash collision internals (bonus — shows depth):**
```
Java 8+: each bucket starts as a LinkedList.
When a bucket exceeds 8 entries → converts to Red-Black Tree.
Worst case per-bucket goes from O(n) → O(log n).
```

---

## Gap 2 — Spring DI + @Primary / @Qualifier with Polymorphism

> Interviewer scenario: "One interface, two implementations — which gets injected?"

```java
interface PaymentService { void pay(); }

@Service("creditCard")
class CreditCardService implements PaymentService { ... }

@Service("upi")
@Primary                                    // default when no @Qualifier given
class UpiService implements PaymentService { ... }

// --- Injection ---

@Autowired
PaymentService service;                     // gets UpiService (@Primary)

@Autowired
@Qualifier("creditCard")                    // explicit — overrides @Primary
PaymentService service;
```

**Rules:**
- `@Primary` = default choice when multiple beans match and no qualifier given
- `@Qualifier("name")` = explicit, always wins over `@Primary`

**Interview answer:**
> "When multiple implementations exist I mark the sensible default with @Primary.
> When a specific class needs a different one I use @Qualifier with the bean name.
> @Qualifier always beats @Primary."

---

## Gap 3 — Content Negotiation in REST

> Q: How does one endpoint serve JSON to one client and XML to another?

```java
@GetMapping(
    value = "/employee/{id}",
    produces = { MediaType.APPLICATION_JSON_VALUE,   // "application/json"
                 MediaType.APPLICATION_XML_VALUE }    // "application/xml"
)
public Employee getEmployee(@PathVariable Long id) {
    return service.findById(id);
}
```

- Client sets `Accept: application/json` → Spring returns JSON
- Client sets `Accept: application/xml`  → Spring returns XML
- Spring's `ContentNegotiatingViewResolver` picks the right `HttpMessageConverter` automatically

**XML dependency needed:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

---

## Gap 4 — Decorator Pattern

> Confirmed L1 — asked alongside Singleton and Factory.

**What it does:** Adds behaviour to an object at **runtime** without changing its class.
Wraps an existing object and adds something extra on top.

**Java example you already know:** `new BufferedReader(new FileReader(path))`
→ `BufferedReader` **decorates** `FileReader` with buffering. `FileReader` has no idea.

```java
interface Coffee { double cost(); }

class SimpleCoffee implements Coffee {
    public double cost() { return 1.0; }
}

class MilkDecorator implements Coffee {
    private Coffee coffee;
    MilkDecorator(Coffee c) { this.coffee = c; }

    public double cost() { return coffee.cost() + 0.25; }  // adds on top
}

// Usage — mix and match at runtime
Coffee c = new MilkDecorator(new SimpleCoffee());   // 1.25
```

**Decorator vs Inheritance:**
- Inheritance: adds behaviour at compile time, to ALL instances
- Decorator: adds behaviour at runtime, to ONE specific instance only

**Interview answer:**
> "Decorator wraps an object to add behaviour dynamically without changing the original class.
> The best Java example is BufferedReader wrapping FileReader — buffering is added transparently.
> I prefer it over inheritance when I need to mix and match features at runtime."

---

## Gap 5 — Testing: JUnit 5 + Mockito

### JUnit 5 annotations

```java
@Test               // marks a test method
@BeforeEach         // runs before EACH test — use for setup
@AfterEach          // runs after EACH test — use for teardown
@BeforeAll          // runs once before ALL tests (method must be static)
@DisplayName("readable name")   // shown in test report
@ParameterizedTest
@ValueSource(ints = {1, 2, 3})  // run same test with multiple inputs
```

### Mockito — the pattern every interviewer expects

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository repo;       // fake — no real DB call

    @InjectMocks
    EmployeeService service;       // injects the mock into the service

    @Test
    void findById_returnsEmployee() {
        // 1. Arrange — tell mock what to return
        when(repo.findById(1L))
            .thenReturn(Optional.of(new Employee("Alice", "Engineering", 70_000)));

        // 2. Act
        Employee result = service.findById(1L);

        // 3. Assert
        assertEquals("Alice", result.getName());
        verify(repo).findById(1L);   // confirm it was actually called
    }
}
```

### @SpringBootTest vs @DataJpaTest

| | `@SpringBootTest` | `@DataJpaTest` |
|---|---|---|
| Loads | Full application context | JPA layer only (repos + entities) |
| Speed | Slow | Fast |
| Database | Real DB / TestContainers | In-memory H2 by default |
| Use when | Integration test — full flow | Testing repository queries in isolation |

**Interview answer:**
> "@DataJpaTest for testing repository queries in isolation — it's fast and uses H2 in-memory.
> @SpringBootTest for full integration tests where I need the whole context including services and controllers."

---

## Confirmed L2 — Fault Tolerant vs Resilience

| | Fault Tolerant | Resilient |
|---|---|---|
| User impact | None — works normally | Degraded — reduced functionality |
| Mechanism | Redundancy, replicas, failover | Circuit breaker, fallback, retry |
| Cost | High — need N copies running | Lower |
| Example | DB with 3 replicas, one dies — user notices nothing | Recommendations service down — page loads without them |

**Key:** Fault Tolerance is the strongest form of Resilience.
All fault-tolerant systems are resilient. Not all resilient systems are fault-tolerant.

---

## Confirmed L2 — SQS vs Kafka vs RabbitMQ

| | SQS | Kafka | RabbitMQ |
|---|---|---|---|
| Type | Queue | Event stream | Message broker |
| Model | ONE consumer per message | Multiple consumer groups, all get messages | Push-based with routing rules |
| Replay | No | Yes (offset-based) | No |
| Throughput | Low | Very high | Medium |
| Use when | Task queue — one worker processes it | Multiple consumers need same event, high throughput, event sourcing | Complex routing, request-reply pattern |

---

## Confirmed L2 — Microservice Communication

```
Synchronous:
  REST      → HTTP/JSON — simple, widely supported
  gRPC      → HTTP/2 + Protobuf — faster, binary, strongly typed

Asynchronous:
  SQS / RabbitMQ  → message queue — one consumer per message
  Kafka            → event streaming — multiple consumers, replay supported

Use async when:
  - Caller does not need an immediate response
  - Consumer is slow and you need to absorb spikes
  - Multiple services need to react to the same event
```

---

## Stream Operations — Lazy vs Eager

```
INTERMEDIATE (lazy — nothing runs until terminal is called):
  filter()   map()   flatMap()   sorted()   distinct()   limit()   skip()   peek()

TERMINAL (eager — triggers the full pipeline):
  collect()   forEach()   count()   findFirst()   anyMatch()   allMatch()
  noneMatch() min()   max()   reduce()

Rules:
  - Chain as many intermediate ops as you want
  - ONE terminal per stream
  - After terminal, stream is consumed — cannot reuse
```

**Short-circuit ops** (`findFirst`, `anyMatch`) stop as soon as the answer is found —
they do NOT process the entire stream.
