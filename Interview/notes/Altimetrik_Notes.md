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
