# Altimetrik / Airbnb Interview Notes

> Source: Actual L1 + L2 feedback documents from selected candidates (Nov 2025 – Mar 2026)
> Interview date: Monday 2026-03-30

---

## Round Structure

| Round | Focus |
|-------|-------|
| L1 | Spring Boot REST, Java 8 Streams, Spring Data JPA, DSA (1-2 problems) |
| L2 | Microservices, Kafka/SQS/RabbitMQ, Testing, Spring DI + Polymorphism |
| EL1 | System architecture explanation, current project walkthrough |

---

## Mistakes Made During Practice (2026-03-27) — Read Before Every Interview

### Mistake 1 — maxBy takes a Comparator, NOT a method reference directly

```java
// WRONG
Collectors.maxBy(Employee::getSalary)

// CORRECT
Collectors.maxBy(Comparator.comparing(Employee::getSalary))
```

**Why:** `maxBy` needs to know HOW to compare. `Employee::getSalary` only extracts a value.
`Comparator.comparing(Employee::getSalary)` turns that extractor into a Comparator.

---

### Mistake 2 — Method references never have brackets

```java
// WRONG
Employee::getDepartment()

// CORRECT
Employee::getDepartment
```

**Why:** `::` is a reference TO the method. Adding `()` means calling it — that's not what `::` does.

---

### Mistake 3 — counting() takes NO arguments

```java
// WRONG
Collectors.counting(Comparator.comparing(Employee::getSalary))

// CORRECT
Collectors.counting()
```

**Why:** `counting()` just counts items in a group. No field, no Comparator needed.
Only `maxBy`/`minBy` need a Comparator because they need to know WHICH field to compare.

---

### Mistake 4 — Getter names must match exactly

```java
// WRONG
e.getexp()

// CORRECT
e.getExperience()
```

**Rule:** getter = `"get"` + field name with first letter capitalised.

---

## Downstream Collector Quick Reference

| What I want | Downstream collector | Return type |
|-------------|---------------------|-------------|
| List of items per group | *(nothing — default)* | `Map<K, List<V>>` |
| Count per group | `counting()` | `Map<K, Long>` |
| Max per group | `maxBy(Comparator.comparing(field))` | `Map<K, Optional<V>>` |
| Min per group | `minBy(Comparator.comparing(field))` | `Map<K, Optional<V>>` |
| Sum per group | `summingDouble(field)` | `Map<K, Double>` |
| Average per group | `averagingDouble(field)` | `Map<K, Double>` |
| Transform before collecting | `mapping(fn, toList())` | `Map<K, List<R>>` |
| Split true/false | `partitioningBy(predicate)` | `Map<Boolean, List<V>>` |

---

## Why is the return type Map<String, Optional<Employee>>?

`maxBy` returns `Optional<Employee>` to protect against an **empty group**.

After `filter(salary > 50000)`, a department may have no employees left.
`max` of empty group = no answer → `Optional.empty()` instead of `null` → no NullPointerException.

```java
// Always unwrap with ifPresent
result.forEach((dept, empOpt) ->
    empOpt.ifPresent(emp -> System.out.println(dept + " -> " + emp.getName()))
);
```

---

## Stream Operations — Lazy vs Eager

| Type | Operations | When it runs |
|------|-----------|-------------|
| **Intermediate** (lazy) | `filter`, `map`, `flatMap`, `sorted`, `distinct`, `limit`, `skip` | Only when terminal is called |
| **Terminal** (eager) | `collect`, `forEach`, `count`, `findFirst`, `anyMatch`, `min`, `max` | Immediately — triggers full pipeline |

**Rule:** ONE terminal per stream. After terminal, stream is consumed — cannot reuse.

---

## Confirmed L1 Problems (Code File: AltimetrikStreams.java)

### Problem 1 — Filter + GroupBy + MaxBy ⭐ HIGHEST PRIORITY

```java
Map<String, Optional<Employee>> result = employees.stream()
    .filter(e -> e.getSalary() > 50_000)
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.maxBy(Comparator.comparing(Employee::getSalary))
    ));
```

### Problem 2 — Count per department

```java
Map<String, Long> countPerDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.counting()
    ));
```

### Problem 3 — Common characters across all words

**Algorithm:** For each word build a frequency array → take MIN across all words → expand back to list.

### Problem 4 — Character frequency of a string

```java
// Streams approach
Map<String, Long> freq = Arrays.stream(input.split(""))
    .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

// Array approach (O(1) space)
int[] freq = new int[26];
for (char c : input.toCharArray()) freq[c - 'a']++;
```

---

## Confirmed L2 Questions

### Fault Tolerant vs Resilience

| | Fault Tolerant | Resilient |
|---|---|---|
| User impact | None — works normally | Degraded — reduced functionality |
| Mechanism | Redundancy, replicas, failover | Circuit breaker, fallback, retry |
| Cost | High — need N copies running | Lower |
| Example | DB with 3 replicas — one dies, user notices nothing | Recommendation service down — page loads without recommendations |

**Key:** Fault Tolerance ⊆ Resilience. FT is the strongest form of resilience.

---

### SQS vs Kafka vs RabbitMQ

| | SQS | Kafka | RabbitMQ |
|---|---|---|---|
| Type | Queue | Event streaming | Message broker |
| Model | One consumer per message | Multiple consumer groups, all get messages | Push-based, routing rules |
| Replay | No | Yes (offset-based) | No |
| Throughput | Low | Very high | Medium |
| Use when | Task processed by exactly ONE worker | Multiple consumers need same data, high throughput, event sourcing | Complex routing, request-reply pattern |

---

### Ways microservices communicate

**Synchronous:** REST (HTTP/JSON), gRPC (HTTP/2 + Protobuf — faster, binary)

**Asynchronous:** Message Queue (SQS, RabbitMQ), Event Streaming (Kafka)

Use async when: caller doesn't need immediate response, consumer is slow, spike handling needed.

---

## Confirmed L1 Design Problem — Bank Transfer REST API

Layers to build from scratch:
1. `@Entity` BankAccount — id, accountNumber, name, balance
2. `BankAccountRepository extends JpaRepository`
3. DTO — `AmountTransfer { debitAccountId, creditAccountId, amount }`
4. `InsufficientBalanceException extends RuntimeException`
5. `@Service AccountService` — `@Transactional transfer()` method
6. `@RestController` — `POST /transfer` + `@ExceptionHandler`

**Key talking points:**
- `@Transactional` ensures both debit + credit roll back if anything fails
- `@Valid` on DTO — fail fast at controller, don't reach service
- Idempotency: client sends UUID header, server caches result to prevent double transfer

---

## Spring Boot Validation (Confirmed L1 Question)

```
@Valid              — triggers Bean Validation on @RequestBody
@NotNull            — field cannot be null
@NotBlank           — string cannot be blank
@Min / @Max         — numeric range
@ControllerAdvice   — global exception handler class
@ExceptionHandler   — handles specific exception type, returns custom response
```

---

## Pagination in Spring Data JPA (Confirmed L1 Question)

```java
// Repository
interface EmployeeRepo extends PagingAndSortingRepository<Employee, Long> {}

// Service
Page<Employee> page = repo.findAll(PageRequest.of(0, 10, Sort.by("salary").descending()));
page.getContent();      // List of employees on this page
page.getTotalPages();   // total pages
page.getTotalElements();// total records
```

---

## Gap 1 — HashMap vs LinkedHashMap vs TreeMap ⭐ CANDIDATE GOT THIS WRONG IN L1

This was specifically called out in feedback — candidate confused these. Know this cold.

| | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| Order | No order (random) | Insertion order | Sorted by key (natural or Comparator) |
| Null keys | 1 allowed | 1 allowed | Not allowed (needs comparison) |
| Performance | O(1) get/put | O(1) get/put | O(log n) get/put |
| Use when | Default — just need key-value | Need to preserve insertion order | Need sorted keys, range queries |

**Interview one-liner:**
> "HashMap for default performance, LinkedHashMap when order matters (like LRU cache), TreeMap when I need keys sorted."

**Trick question:** "What if two keys hash to same bucket?"
→ Java 8+: LinkedList in bucket, becomes Red-Black Tree when bucket > 8 entries (for O(log n) worst case instead of O(n)).

---

## Gap 2 — Spring DI + @Qualifier / @Primary with Polymorphism (Confirmed L2)

**Scenario the interviewer uses:** One interface, two implementations — which one gets injected?

```java
interface PaymentService { void pay(); }

@Service("creditCard")
class CreditCardService implements PaymentService { ... }

@Service("upi")
@Primary   // ← used when NO qualifier specified — this is the default
class UpiService implements PaymentService { ... }

// Injection — two ways:
@Autowired
PaymentService paymentService;  // gets UpiService because @Primary

@Autowired
@Qualifier("creditCard")        // explicit — overrides @Primary
PaymentService paymentService;
```

**Key rules:**
- `@Primary` — default when multiple beans match, no qualifier given
- `@Qualifier("beanName")` — explicit, overrides @Primary
- `@Qualifier` wins over `@Primary` always

**Interview answer pattern:**
> "When I have multiple implementations of an interface I mark the default with @Primary. When a specific class needs a different one I use @Qualifier with the bean name."

---

## Gap 3 — Content Negotiation in REST (Confirmed L2)

**Q: How does one endpoint serve JSON to one client and XML to another?**

Answer: `Accept` header + `produces` in `@GetMapping`

```java
@GetMapping(value = "/employee/{id}", produces = {
    MediaType.APPLICATION_JSON_VALUE,   // "application/json"
    MediaType.APPLICATION_XML_VALUE     // "application/xml"
})
public Employee getEmployee(@PathVariable Long id) {
    return service.findById(id);
}
```

- Client sets `Accept: application/json` → gets JSON
- Client sets `Accept: application/xml` → gets XML (need Jackson XML or JAXB on classpath)
- Spring's `ContentNegotiatingViewResolver` picks the right converter automatically

**Dependency for XML support:**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

---

## Gap 4 — Decorator Pattern (Confirmed L1 — asked alongside Singleton + Factory)

**What it does:** Adds behaviour to an object at runtime without changing its class.
Wraps an object inside another object that adds something extra.

**Real-world analogy:** Coffee + milk + sugar — each is a "decorator" that adds to the base coffee.

```java
interface Coffee { double cost(); }

class SimpleCoffee implements Coffee {
    public double cost() { return 1.0; }
}

// Decorator — wraps another Coffee
class MilkDecorator implements Coffee {
    private Coffee coffee;
    MilkDecorator(Coffee coffee) { this.coffee = coffee; }
    public double cost() { return coffee.cost() + 0.25; }  // adds milk cost
}

// Usage
Coffee c = new MilkDecorator(new SimpleCoffee());  // 1.25
```

**Java example you already know:** `BufferedReader(new FileReader(...))` — BufferedReader decorates FileReader with buffering.

**vs Inheritance:** Inheritance adds behaviour at compile time to all instances. Decorator adds at runtime to specific instances only.

**Interview answer:**
> "Decorator wraps an object to add behaviour dynamically. I use it instead of subclassing when I need to mix and match features — like Java's I/O streams where BufferedReader decorates FileReader."

---

## Gap 5 — Testing (Confirmed L2 — panelist checked hands-on ability)

### JUnit 5 annotations you must know

```java
@Test               // marks test method
@BeforeEach         // runs before EACH test (setup)
@AfterEach          // runs after EACH test (teardown)
@BeforeAll          // runs once before ALL tests (must be static)
@DisplayName("...")  // readable test name in report
@ParameterizedTest  // run same test with multiple inputs
@ValueSource(ints = {1, 2, 3})
```

### Mockito — the key pattern

```java
@ExtendWith(MockitoExtension.class)
class ServiceTest {

    @Mock
    EmployeeRepository repo;      // creates a fake — no real DB call

    @InjectMocks
    EmployeeService service;      // injects mock into service

    @Test
    void findById_returnsEmployee() {
        // 1. Arrange — tell mock what to return
        when(repo.findById(1L)).thenReturn(Optional.of(new Employee("Alice")));

        // 2. Act
        Employee result = service.findById(1L);

        // 3. Assert
        assertEquals("Alice", result.getName());
        verify(repo).findById(1L);  // confirm it was called
    }
}
```

### @SpringBootTest vs @DataJpaTest

| | @SpringBootTest | @DataJpaTest |
|---|---|---|
| Loads | Full application context | Only JPA layer (repos + entities) |
| Speed | Slow | Fast |
| Use when | Integration test — testing full flow | Unit testing repository queries only |
| DB | Real DB (or TestContainers) | In-memory H2 by default |

**Interview answer:**
> "@SpringBootTest for full integration tests, @DataJpaTest when I only want to test repository queries in isolation with an in-memory database."
