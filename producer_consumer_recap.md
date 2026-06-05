# Producer-Consumer Recap in Java

> **From:** [SDE 3 Java 21 Learning Plan](./STUDY_PLAN.md)  
> **Topic:** Concurrency, BlockingQueue, AtomicInteger  
> **Level:** Intermediate → Advanced

This document recaps a hands-on implementation journey for the Producer-Consumer problem in Java, starting from a naive shared buffer and ending with a cleaner `BlockingQueue`-based solution.

---

## Problem Idea

The producer-consumer problem is a classic concurrency problem with a finite buffer shared by producer threads and consumer threads. A producer must wait when the buffer is full, a consumer must wait when the buffer is empty, and both must coordinate safely without data loss or race conditions.

### Visual State Diagram

```
Step 1 (Naive):          P → [Slot] ← C    (No sync, data loss)
Step 2 (Manual sync):    P → [Slot] ← C    (wait/notify, 1 item max)
Step 3 (Bounded queue):  P → [1,2,3] ← C   (wait/notify, capacity N)
Step 4 (BlockingQueue):  P → [1,2,3] ← C   (JDK handles sync)
```

---

## Step 1: Naive Single-Slot Buffer

The first version used a simple buffer with one `int value`, a `put()` method, and a `get()` method, without any synchronization. In that version, the producer could overwrite values before the consumer read them.

```java
public class Buffer {
    private int value;

    public void put(int v) {
        value = v;
    }

    public int get() {
        return value;
    }
}
```

### What Was Observed

- The producer kept writing values like `1, 2, 3, 4...`.
- The consumer often saw only the latest value.
- Intermediate values were skipped because the buffer held only one slot at a time.
- **Root cause:** No synchronization meant the producer and consumer were racing; the producer could overwrite before the consumer read.

---

## Step 2: Single-Slot Buffer with `wait()` and `notifyAll()`

The next version added coordination with `synchronized`, a `hasValue` flag, `wait()`, and `notifyAll()`. This matches the guarded-block style used in Java concurrency tutorials, where a thread waits until a condition is satisfied.

```java
public class Buffer {
    private int value;
    private boolean hasValue = false;

    public synchronized void put(int v) throws InterruptedException {
        while (hasValue) {
            wait();  // Producer waits if slot is full
        }
        value = v;
        hasValue = true;
        notifyAll();  // Wake consumer
    }

    public synchronized int get() throws InterruptedException {
        while (!hasValue) {
            wait();  // Consumer waits if slot is empty
        }
        int result = value;
        hasValue = false;
        notifyAll();  // Wake producer
        return result;
    }
}
```

### Why It Worked

- `put()` waited if the slot was already full.
- `get()` waited if there was nothing to consume.
- Each value was produced once and consumed once.
- `notifyAll()` ensured sleeping threads woke up to re-check the condition.

### Important Lessons: `while` vs. `if`

**❌ WRONG: Using `if`**
```java
public synchronized int get() throws InterruptedException {
    if (!hasValue) {  // ❌ DON'T DO THIS
        wait();
    }
    int result = value;
    hasValue = false;
    notifyAll();
    return result;
}
```

**✅ CORRECT: Using `while`**
```java
public synchronized int get() throws InterruptedException {
    while (!hasValue) {  // ✅ Correct
        wait();
    }
    // ... rest of method
}
```

**Why?** **Spurious wakeups** and **multiple threads waking simultaneously**:

1. A thread may wake from `wait()` even without an explicit `notifyAll()` (spurious wakeup).
2. Multiple threads may wake at once—if two consumers wake and only one item is in the buffer, the second consumer will crash if it doesn't re-check.

**Concrete scenario:**
- Consumer C1 and C2 both call `get()`, find `!hasValue`, enter `wait()`
- Producer adds item, calls `notifyAll()`
- Both C1 and C2 wake up
- C1 acquires lock first, reads and consumes the item, sets `hasValue = false`
- C2 acquires lock, but:
  - **With `if`:** skips the condition check, tries `queue.remove()` on empty queue → `NoSuchElementException`
  - **With `while`:** re-checks `!hasValue`, finds queue empty, goes back to `wait()`

---

## Step 3: Replace Single Slot with a Queue

To allow the producer to get ahead by more than one item, the buffer was redesigned to use a `Queue<Integer>` with a fixed capacity. This converts the single-slot solution into a **bounded buffer**.

```java
import java.util.LinkedList;
import java.util.Queue;

public class Buffer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;

    public Buffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(int v) throws InterruptedException {
        while (queue.size() == capacity) {  // Wait if full
            wait();
        }
        queue.add(v);
        notifyAll();
    }

    public synchronized int get() throws InterruptedException {
        while (queue.isEmpty()) {  // Wait if empty
            wait();
        }
        int result = queue.remove();
        notifyAll();
        return result;
    }
}
```

### What Changed

With multiple slots, the producer no longer blocked after every item. It could continue producing until the queue reached its configured capacity, while the consumer kept consuming available items. This **decouples** producer and consumer speeds.

### Visual Queue Evolution

```
Capacity = 3

After P produces [1, 2, 3]:  Queue: [1, 2, 3] (Full)
After C consumes 1:          Queue: [2, 3] (Room for 1 more)
```

---

## Step 4: Cleaner Solution with `BlockingQueue`

The custom buffer was replaced with Java's built-in `BlockingQueue`, specifically `ArrayBlockingQueue`. `BlockingQueue` is designed for exactly this pattern: `put()` blocks when the queue is full, `take()` blocks when empty.

```java
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
```

### Producer

```java
public class Producer implements Runnable {
    private final BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int value = 1; // later improved
                queue.put(value);  // Blocks if full
                System.out.println(Thread.currentThread().getName() + ":Produced=>" + value);
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Consumer

```java
public class Consumer implements Runnable {
    private final BlockingQueue<Integer> queue;

    public Consumer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int v = queue.take();  // Blocks if empty
                System.out.println(Thread.currentThread().getName() + ":Consumed=>" + v);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Why This Version Is Better

| Aspect | Manual Buffer | BlockingQueue |
|--------|---------------|---------------|
| **Lines of code** | ~30 | ~15 |
| **Synchronization calls** | 5+ (`synchronized`, `wait`, `notifyAll`×2) | 0 (JDK handles it) |
| **Code clarity** | Moderate | High |
| **Error-prone** | Forgetting `while` vs `if` | Negligible |

- No manual `synchronized`, `wait()`, or `notifyAll()` calls needed.
- The code became shorter and easier to reason about.
- The JDK handled thread-safe blocking behavior directly.

---

## Step 5: Multiple Producers and Consumers

The same `BlockingQueue` was shared among two producers and two consumers. `BlockingQueue` is explicitly designed for safe use by multiple producer and consumer threads.

```java
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);

Thread p1 = new Thread(new Producer(queue), "Producer:A");
Thread p2 = new Thread(new Producer(queue), "Producer:B");
Thread c1 = new Thread(new Consumer(queue), "Consumer:X");
Thread c2 = new Thread(new Consumer(queue), "Consumer:Y");

p1.start();
p2.start();
c1.start();
c2.start();
```

### Observation

Consumers did not care which producer created an item. They simply took the next available element from the shared queue, distributing work naturally across multiple consumer threads.

**Example output:**
```
Producer:A:Produced=>1
Producer:B:Produced=>2
Consumer:X:Consumed=>1
Consumer:Y:Consumed=>2
Producer:A:Produced=>3
...
```

---

## Step 6: Global Unique Values with `AtomicInteger`

When two producers each used their own local counter, duplicate values appeared. To make produced values unique across all producers, a shared static `AtomicInteger` was introduced.

### ❌ WRONG: Local Counter Per Producer

```java
public class Producer implements Runnable {
    private int counter = 1;  // Each producer has its own!

    @Override
    public void run() {
        try {
            while (true) {
                int value = counter++;  // Producer:A: 1,2,3 | Producer:B: 1,2,3
                queue.put(value);
                System.out.println(Thread.currentThread().getName() + ":Produced=>" + value);
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**Result:** Duplicate values (1, 1, 2, 2, 3, 3, ...)

### ✅ CORRECT: Global AtomicInteger

```java
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {
    private static final AtomicInteger globalCounter = new AtomicInteger(1);
    private final BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int value = globalCounter.getAndIncrement();  // Thread-safe increment
                queue.put(value);
                System.out.println(Thread.currentThread().getName() + ":Produced=>" + value);
                Thread.sleep(300);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**Result:** Unique values (1, 2, 3, 4, ...)

### Why `AtomicInteger`?

- `getAndIncrement()` uses **Compare-And-Swap (CAS)** hardware primitive—no full lock, just atomic check+update.
- **Faster than:** `synchronized int counter++` (which would serialize all producers)
- **Thread-safe:** Multiple producers can safely call `getAndIncrement()` without race conditions.

### Important Detail: Placement Inside Loop

The call to `globalCounter.getAndIncrement()` **must happen inside the loop**:

```java
// ❌ WRONG
int value = globalCounter.getAndIncrement();  // Called ONCE before loop
while (true) {
    queue.put(value);
    value++;  // Now local increment—duplicates reappear!
    Thread.sleep(300);
}

// ✅ CORRECT
while (true) {
    int value = globalCounter.getAndIncrement();  // Called EVERY iteration
    queue.put(value);
    Thread.sleep(300);
}
```

---

## Common Pitfalls

| Pitfall | Problem | Fix |
|---------|---------|-----|
| Using `if` instead of `while` | Spurious wakeups crash with exception | Always use `while` around `wait()` |
| Using `notify()` instead of `notifyAll()` | Wrong thread may wake → deadlock | Use `notifyAll()` for multiple waiters |
| Forgetting blocking semantics | Code doesn't actually block | Remember: `BlockingQueue.put()` and `take()` block automatically |
| Local counter per producer | Duplicate values produced | Use `static AtomicInteger` shared across producers |
| Mixing local `++` with `AtomicInteger` | Reintroduces duplicates | Call `getAndIncrement()` on **every iteration** |

---

## Complexity Analysis

| Operation | Manual `wait/notify` | `BlockingQueue` |
|---|---|---|
| `put()` (not full) | O(1) avg | O(1) avg |
| `put()` (full) | Blocked until consumer drains | Blocked until consumer drains |
| `take()` (not empty) | O(1) avg | O(1) avg |
| `take()` (empty) | Blocked until producer adds | Blocked until producer adds |
| Lock contention | High (global `synchronized` block) | Lower (internal segment-based locking) |
| Memory overhead | Minimal (one flag + slot) | Higher (queue + internal bookkeeping) |

---

## Lines of Code Comparison

| Solution | LoC | Synchronized Calls | Complexity |
|----------|--------|---|---|
| Step 1 (Naive) | ~8 | 0 | Trivial (broken) |
| Step 2 (Manual wait/notify) | ~30 | 5 | Moderate |
| Step 3 (Bounded queue) | ~25 | 5 | Moderate |
| Step 4 (BlockingQueue) | ~15 | 0 | Low |
| Step 5 (Multiple P/C) | ~15 | 0 | Low |
| Step 6 (+ AtomicInteger) | ~20 | 0 | Low |

---

## Verification Checklist

When running multiple producers/consumers, verify:

- [ ] **No duplicate values** in output (sequence must be strictly increasing: 1, 2, 3, ...)
- [ ] **No skipped values** (no gaps in sequence)
- [ ] **No deadlock** (check log output continues indefinitely, doesn't freeze)
- [ ] **No exceptions thrown** when queue is full/empty (blocking should handle it)
- [ ] **`AtomicInteger.getAndIncrement()` called inside loop** (not before)
- [ ] **Thread names visible** to verify multiple producers/consumers are active

Example of **good output:**
```
Producer:A:Produced=>1
Producer:B:Produced=>2
Consumer:X:Consumed=>1
Producer:A:Produced=>3
Consumer:Y:Consumed=>2
Consumer:X:Consumed=>3
```

Example of **bad output** (duplicates):
```
Producer:A:Produced=>1
Producer:B:Produced=>1  ← DUPLICATE
Consumer:X:Consumed=>1
Consumer:Y:Consumed=>1
```

---

## Interview Follow-up Questions

### Q1: Why use `while` instead of `if` around `wait()`?

**A:** Spurious wakeups and multiple threads waking simultaneously.

A thread can wake from `wait()` without an explicit `notifyAll()` (spurious wakeup). Also, when multiple threads wait and `notifyAll()` is called, all wake up—if only one work item is available, the second thread needs to re-check the condition. With `if`, it would skip the check and crash. With `while`, it re-checks and goes back to sleep if the condition is still not met.

---

### Q2: Why use `notifyAll()` instead of `notify()`?

**A:** With multiple producers/consumers, `notify()` wakes only ONE random thread.

If you wake a producer when the queue is full, that producer stays blocked. If you wake a consumer when the queue is empty, same problem. `notifyAll()` wakes everyone; each rechecks its condition and either proceeds or waits again.

**Analogy:** `notify()` is like ringing a doorbell once—only one random person in the house wakes up. `notifyAll()` is like a fire alarm—everyone wakes and checks if they need to act.

---

### Q3: Can `ArrayBlockingQueue` have thread starvation?

**A:** Not with default settings; fair mode eliminates starvation.

By default, `ArrayBlockingQueue` uses a single lock that may favor one thread. But with **fair mode** (`new ArrayBlockingQueue<>(10, true)`), threads are granted lock access in FIFO order, guaranteeing eventual access.

---

### Q4: How is `AtomicInteger.getAndIncrement()` faster than `synchronized int counter++`?

**A:** Uses CAS (Compare-And-Swap) instead of full lock.

- `synchronized counter++`: Acquires a full lock → serializes **all** producers → contention bottleneck
- `AtomicInteger.getAndIncrement()`: Uses hardware CAS primitive → retry on conflict → lower contention → higher throughput

For very high concurrency, `AtomicInteger` is **orders of magnitude** faster.

---

### Q5: What happens if you call `globalCounter.getAndIncrement()` once before the loop instead of inside?

**A:** Duplicates reappear because you fall back to local increment.

```java
int value = globalCounter.getAndIncrement();  // Gets unique value ONCE
while (true) {
    queue.put(value);
    value++;  // Now you're incrementing locally — multiple producers get 1, 2, 3, ...
    Thread.sleep(300);
}
```

Each producer gets one unique start value, then increments locally—two producers both produce (1, 2, 3, ...).

---

### Q6: How is `BlockingQueue` internally synchronized?

**A:** In Java 8+, `ArrayBlockingQueue` uses a **single `ReentrantLock`** with two `Condition` objects: `notEmpty` and `notFull`.

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notEmpty = lock.newCondition();
private final Condition notFull = lock.newCondition();
```

- `put()`: Waits on `notFull` until queue has space, signals `notEmpty`
- `take()`: Waits on `notEmpty` until queue has items, signals `notFull`

This is similar to Step 2/3 (manual `wait`/`notify`), but optimized and battle-tested.

---

## Key Takeaways

1. **Mental model first:** Shared buffer, full/empty conditions, blocking behavior.
2. **Manual `wait()`/`notifyAll()` teaches fundamentals:** Understand the underlying mechanism before using abstractions.
3. **Always use `while`, not `if`, around `wait()`:** Protects against spurious wakeups and multiple-thread scenarios.
4. **`BlockingQueue` is production-ready:** Use it for real code; it handles all edge cases and is performant.
5. **`AtomicInteger` for global counters:** Faster and safer than `synchronized` for this use case.
6. **Test with multiple producers/consumers:** Single producer/consumer doesn't catch threading bugs; always test concurrent scenarios.

---

## References

- Java Concurrency in Practice (Goetz et al.) — Chapter 3: Sharing Objects
- [ArrayBlockingQueue Javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ArrayBlockingQueue.html)
- [AtomicInteger Javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html)
- [Object.wait() Javadoc](https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#wait--)
