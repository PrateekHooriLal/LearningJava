# CAPCO Interview Notes

> Interview date: 2026-03-23 — **FAILED**
> Use this as a reminder of gaps — these are now your priority topics.

---

## Gaps Identified (Why It Failed)

| Topic | What was asked | Gap |
|-------|---------------|-----|
| Spring Bean Lifecycle | How Spring starts, ApplicationContext, BeanDefinition | Couldn't explain clearly |
| Parallel Streams | ForkJoinPool internals, work stealing | Weak |
| Java 17-21 Features | Articulate new features clearly | Couldn't name them fluently |
| Functional Interfaces | Function, Consumer, Supplier, BiConsumer — composition | Weak on compose vs andThen |
| CompletableFuture | `exceptionally` vs `handle` vs `whenComplete` | Weak |
| Coding | Print 2nd highest repeating character using streams | Failed to write correctly |

---

## Remediation Files Created

| File | Location |
|------|---------|
| `SpringBeanLifecycle.java` | `LearnJava/.../com/java/spring/` |
| `SpringAutoConfiguration.java` | `LearnJava/.../com/java/spring/` |
| `SpringDependencyResolution.java` | `LearnJava/.../com/java/spring/` |
| `FunctionalInterfacesDeepDive.java` | `LearnJava/.../com/java/functional/` |
| `Java17To21Features.java` | `LearnJava/.../com/java/java21/` |
| `CompletableFutureDeepDive.java` | `LearnJava/.../com/java/concurrency/advanced/` |

---

## Key Things to Be Able to Say Out Loud

### Spring Bean Lifecycle
`@PostConstruct` → `InitializingBean.afterPropertiesSet()` → custom `init-method`
→ bean is ready → `@PreDestroy` on shutdown

### CompletableFuture Exception Handling
```java
// exceptionally — only runs on exception, returns fallback value
.exceptionally(ex -> "fallback")

// handle — always runs, gets (result, exception) — one or other is null
.handle((result, ex) -> ex != null ? "error" : result)

// whenComplete — always runs, cannot change return value (side-effects only)
.whenComplete((result, ex) -> log.info("done"))
```

### 2nd Highest Repeating Character (the coding question they asked)
```java
String input = "aabbcccdddd";
// Step 1: frequency map
Map<Character, Long> freq = input.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

// Step 2: sort by frequency descending, get 2nd entry
freq.entrySet().stream()
    .sorted(Map.Entry.<Character, Long>comparingByValue().reversed())
    .skip(1)       // skip highest
    .findFirst()   // get 2nd highest
    .ifPresent(e -> System.out.println(e.getKey() + " = " + e.getValue()));
```
