# Producer-Consumer Recap in Java

This document recaps a hands-on implementation journey for the Producer-Consumer problem in Java, starting from a naive shared buffer and ending with a cleaner `BlockingQueue`-based solution.[cite:43][cite:147]

## Problem idea

The producer-consumer problem is a classic concurrency problem with a finite buffer shared by producer threads and consumer threads.[cite:133][cite:136] A producer must wait when the buffer is full, and a consumer must wait when the buffer is empty.[cite:133][cite:141]

## Step 1: Naive single-slot buffer

The first version used a simple buffer with one `int value`, a `put()` method, and a `get()` method, without any synchronization. In that version, the producer could overwrite values before the consumer read them, so some produced values were effectively lost.

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

### What was observed

- The producer kept writing values like `1, 2, 3, 4...`.
- The consumer often saw only the latest value.
- Intermediate values were skipped because the buffer held only one slot at a time.

## Step 2: Single-slot buffer with `wait()` and `notifyAll()`

The next version added coordination with `synchronized`, a `hasValue` flag, `wait()`, and `notifyAll()`. This matches the guarded-block style used in Java concurrency tutorials, where a thread waits until a required condition becomes true.[cite:43]

```java
public class Buffer {
    private int value;
    private boolean hasValue = false;

    public synchronized void put(int v) throws InterruptedException {
        while (hasValue) {
            wait();
        }
        value = v;
        hasValue = true;
        notifyAll();
    }

    public synchronized int get() throws InterruptedException {
        while (!hasValue) {
            wait();
        }
        int result = value;
        hasValue = false;
        notifyAll();
        return result;
    }
}
```

### Why it worked

- `put()` waited if the slot was already full.
- `get()` waited if there was nothing to consume.
- Each value was produced once and consumed once.

### Important lessons

- Use `while`, not `if`, around `wait()` because the condition must be rechecked after wake-up.[cite:133][cite:136]
- `notifyAll()` is safer than `notify()` when multiple waiting threads may exist.[cite:134]

## Step 3: Replace the single slot with a queue

To allow the producer to get ahead by more than one item, the buffer was redesigned to use a `Queue<Integer>` with a fixed capacity. This converts the single-slot solution into a bounded buffer.[cite:133][cite:141]

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
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(v);
        notifyAll();
    }

    public synchronized int get() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        int result = queue.remove();
        notifyAll();
        return result;
    }
}
```

### What changed

With multiple slots, the producer no longer blocked after every item. It could continue producing until the queue reached its configured capacity, while the consumer kept consuming available items.[cite:141][cite:136]

## Step 4: Cleaner solution with `BlockingQueue`

The custom buffer was then replaced with Java's built-in `BlockingQueue`, specifically `ArrayBlockingQueue`. `BlockingQueue` is designed for exactly this pattern: `put()` blocks when the queue is full, and `take()` blocks when the queue is empty.[cite:147][cite:144]

```java
BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);
```

Producer:

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

Consumer:

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
                int v = queue.take();
                System.out.println(Thread.currentThread().getName() + ":Consumed=>" + v);
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Why this version is better

- No manual `synchronized`, `wait()`, or `notifyAll()` calls were needed.
- The code became shorter and easier to reason about.
- The JDK handled thread-safe blocking behavior directly.[cite:147][cite:145]

## Step 5: Multiple producers and consumers

The same `BlockingQueue` was shared among two producers and two consumers. `BlockingQueue` is explicitly designed for safe use by multiple producer and consumer threads.[cite:145][cite:165]

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

Consumers did not care which producer created an item. They simply took the next available element from the shared queue, distributing work naturally across multiple consumer threads.[cite:145]

## Step 6: Global unique values with `AtomicInteger`

When two producers each used their own local counter, duplicate values appeared. To make produced values unique across all producers, a shared static `AtomicInteger` was introduced inside `Producer`.[cite:149]

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
                int value = globalCounter.getAndIncrement();
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

### Important detail

The call to `globalCounter.getAndIncrement()` must happen inside the loop. If it is called only once before the loop and then followed by `value++`, each producer starts from a unique number but later drifts into overlapping local sequences.

## Common pitfalls

- Using `if` instead of `while` around `wait()`.[cite:133][cite:136]
- Using `notify()` instead of `notifyAll()` when multiple producers or consumers may wait.[cite:134]
- Forgetting that `BlockingQueue.put()` and `take()` already provide blocking behavior.[cite:147][cite:144]
- Using a local counter per producer when the requirement is global uniqueness.
- Mixing local increment logic with a shared `AtomicInteger` and accidentally reintroducing duplicates.

## Key takeaways

- Start with the mental model: shared buffer, full/empty conditions, and blocking behavior.[cite:133]
- Manual `wait()`/`notifyAll()` teaches the underlying coordination mechanism.[cite:43]
- `BlockingQueue` is the cleaner production-style solution for most Java producer-consumer use cases.[cite:147][cite:145]
- `AtomicInteger` is a simple way to create unique values safely across multiple producer threads.[cite:149]
