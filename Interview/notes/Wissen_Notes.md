# Wissen Interview Notes

> Interview date: TBD
> Role: Java Developer (5-12 YOE), deployed to client (Goldman Sachs / Morgan Stanley / Flipkart)

---

## Round Structure

| Round | Focus |
|-------|-------|
| R0 | Online Assessment — MCQs (Java, SQL) + 2 coding problems |
| R1 | Core Java, OOP, Collections, DSA, SQL |
| R2 | Multithreading, Spring Boot, Java 8+, more DSA |
| R3 | Advanced concurrency, design (sometimes) |
| R4 | Director — project discussion, LLD, leadership |
| R5 | HR |

---

## Top Priority Topics (asked in every senior round)

### 1. HashMap Internals
- Array of buckets (`Node[]`), hashing, collision → chaining → treeification (Java 8, threshold 8)
- `put()` / `get()` → O(1) average, O(n) worst case
- **ConcurrentHashMap** — Java 7: segmented locking. Java 8: CAS + synchronized per bucket
- Can multiple threads read simultaneously? Yes. What during a write? Other threads can read unaffected segments.

### 2. Immutable Class Design
Rules:
1. Class `final`
2. All fields `private final`
3. No setters
4. Deep copy mutable inputs in constructor
5. Return defensive copies from getters

```java
// WRONG — caller can mutate the list
public List<String> getItems() { return items; }

// CORRECT
public List<String> getItems() { return Collections.unmodifiableList(items); }
```

### 3. Multithreading — Custom Implementations

**Custom CountDownLatch:**
```java
synchronized void await() throws InterruptedException {
    while (count > 0) wait();   // while NOT if — spurious wakeups
}
synchronized void countDown() {
    if (--count == 0) notifyAll();
}
```

**Custom ReentrantLock:**
```java
synchronized void lock() throws InterruptedException {
    while (isLocked && owner != Thread.currentThread()) wait();
    isLocked = true;
    owner = Thread.currentThread();
    holdCount++;
}
synchronized void unlock() {
    if (--holdCount == 0) { isLocked = false; owner = null; notifyAll(); }
}
```

**Key differences:**
- `wait()` releases the lock. `sleep()` does NOT release the lock.
- Always use `while` not `if` with `wait()` — spurious wakeups.
- `notifyAll()` not `notify()` — multiple threads may be waiting.

**CountDownLatch vs CyclicBarrier:**
- CountDownLatch: one-shot, count goes down, threads wait for count = 0
- CyclicBarrier: reusable, threads wait for each other at barrier point, then all proceed

### 4. Coding Drills

**Print 1–20, two threads (T1: odd, T2: even):**
- Shared `count` variable, `synchronized`, `wait()`/`notifyAll()`

**Print 1–12, four threads (T1:1,5,9 | T2:2,6,10 | T3:3,7,11 | T4:4,8,12):**
- Each thread has a `threadNumber`, prints when `count % 4 == threadNumber`

---

## DSA — Frequently Asked Problems

| Problem | Approach |
|---------|---------|
| Merge two sorted arrays | Two pointers |
| Stock Buy and Sell max profit | One pass, track min price |
| Trapping Rain Water | Two pointers or monotonic stack |
| Number of Islands | BFS / DFS |
| Find min in rotated sorted array | Binary search |
| Subarray with target sum | Sliding window |
| LRU Cache | HashMap + DoublyLinkedList |
| Character frequency | Array O(1) space or streams |

---

## SQL — Frequently Asked

```sql
-- Top 3 customers by total spend
SELECT c.customer_id, c.customer_name, SUM(o.amount) AS total
FROM Customers c
JOIN Orders o ON c.customer_id = o.customer_id
GROUP BY c.customer_id
ORDER BY total DESC
LIMIT 3;
```

- Clustered index: data rows stored in index order (one per table, usually PK)
- Non-clustered index: separate structure with pointer to data row (many allowed)
- ACID: Atomicity, Consistency, Isolation, Durability

---

## Spring Boot (Surface Level for Wissen's Own Rounds)

- `@Valid` + `@ControllerAdvice` + `@ExceptionHandler`
- DI types: constructor (preferred), setter, field
- `@Component` vs `@Service` vs `@Repository` vs `@Controller`
- Spring Boot pros over legacy Spring: auto-config, embedded Tomcat, starter deps, Actuator

---

## Mistakes Made During Practice

*(Add mistakes here after each practice session)*
