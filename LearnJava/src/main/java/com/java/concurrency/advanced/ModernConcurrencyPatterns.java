package com.java.concurrency.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * =============================================================================
 * CONCEPT: Modern Concurrency Patterns — Advanced Java 21 Techniques
 * =============================================================================
 *
 * This file covers patterns that fill gaps left by basic concurrency primitives:
 * lock-free data structures, advanced synchronization barriers, and the
 * CompletableFuture vs Virtual Threads decision framework.
 *
 * PATTERNS COVERED:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. CAS (Compare-And-Swap): the foundation of all lock-free data structures.
 *    AtomicInteger.compareAndSet(expected, update) — single atomic instruction.
 *    ABA problem + AtomicStampedReference fix.
 *
 * 2. Lock-Free Stack (Treiber Stack): classical lock-free data structure.
 *    Uses AtomicReference<Node> + CAS loop. No locks, no blocking.
 *    Demonstrates how lock-free algorithms work under high contention.
 *
 * 3. Producer-Consumer with Virtual Threads (Java 21).
 *    Virtual threads block without wasting OS threads — ideal for I/O-heavy
 *    producer-consumer where threads spend most time waiting.
 *
 * 4. Phaser: the most flexible synchronization barrier.
 *    Like CyclicBarrier but supports dynamic party registration/deregistration.
 *    Supports multiple phases (run-then-barrier-then-run-again).
 *
 * 5. CF vs Virtual Threads decision guide.
 *
 * LOCK-FREE vs LOCK-BASED:
 * ┌────────────────────────────────────────────────────────────────────┐
 * │ Lock-based          │ Lock-free (CAS)                             │
 * ├────────────────────────────────────────────────────────────────────┤
 * │ Simple code         │ Complex, tricky to get right                │
 * │ Thread can wait     │ Threads always make progress                │
 * │ Risk: deadlock      │ No deadlock (no locks)                      │
 * │ Risk: starvation    │ Risk: livelock (CAS loop spins forever)     │
 * │ Good: infrequent    │ Good: high-contention short critical section │
 * └────────────────────────────────────────────────────────────────────┘
 *
 * WAIT-FREE vs LOCK-FREE:
 *   Lock-free: some thread always makes progress (others may spin).
 *   Wait-free: EVERY thread makes progress in a bounded number of steps.
 *   Java CAS primitives are lock-free. Making them wait-free is harder.
 *
 * TIME COMPLEXITY:
 *   CAS ops: O(1) per successful operation, O(k) total for k retries under contention
 *   Treiber stack: O(1) amortized push/pop
 *   Phaser: O(parties) per phase barrier
 *
 * SPACE COMPLEXITY: O(N) for N elements in lock-free structures
 *
 * INTERVIEW ANGLE:
 *   "Implement a thread-safe stack without locks" → Treiber stack
 *   "What is ABA problem?" → CAS sees A→B→A, thinks no change, but state DID change
 *   "When would you use Phaser over CyclicBarrier?" → Dynamic party count, multiple phases
 *
 * REAL-WORLD USE:
 *   CAS: Java's ConcurrentHashMap, CopyOnWriteArrayList, AtomicInteger
 *   Treiber stack: Java's ForkJoinPool work-stealing deque
 *   Phaser: multi-phase simulations, parallel iterative algorithms
 * =============================================================================
 */
public class ModernConcurrencyPatterns {

    // =========================================================================
    // PATTERN 1: CAS (Compare-And-Swap) + ABA Problem
    // =========================================================================

    /**
     * CAS DEEP DIVE:
     *
     * CAS is a single atomic CPU instruction:
     *   boolean CAS(memory_location, expected_value, new_value) {
     *     if (*memory_location == expected_value) {
     *       *memory_location = new_value;
     *       return true;  // success
     *     }
     *     return false;   // failure (someone else changed it)
     *   }
     *
     * In Java: AtomicInteger.compareAndSet(expected, update)
     * Implemented with LOCK CMPXCHG instruction on x86 (hardware-level atomic).
     *
     * CAS LOOP PATTERN (lock-free retry):
     *   do {
     *     int current = atomicVar.get();
     *     int next = computeNext(current);
     *   } while (!atomicVar.compareAndSet(current, next));
     *
     * KEY INSIGHT: If CAS fails, ANOTHER THREAD made progress → retry.
     * This is lock-free: the system as a whole always makes progress.
     * Individual threads may retry many times under high contention.
     *
     * THE ABA PROBLEM:
     *   Thread A reads value = A.
     *   Thread B changes A → B → A (back to A).
     *   Thread A's CAS(A, newValue) SUCCEEDS — it thinks nothing changed!
     *   But the intermediate state (value was B) may have been semantically significant.
     *
     * EXAMPLE (linked list): Node A is popped, popped node B, pushed A back.
     * A's CAS succeeds, but next pointer now points to freed memory → crash.
     *
     * ABA FIX: AtomicStampedReference — pair value with monotonic counter (stamp).
     * CAS on (value, stamp) pair. Stamp increments on every change.
     * Even if value returns to A, stamp is different → CAS fails correctly.
     */
    private static void demoCAS() throws InterruptedException {
        System.out.println("=== PATTERN 1: CAS and ABA Problem ===\n");

        // --- Basic CAS usage ---
        AtomicInteger atomicCounter = new AtomicInteger(0);

        // CAS-based increment (same as AtomicInteger.incrementAndGet internally)
        int current, next;
        do {
            current = atomicCounter.get();
            next = current + 1;
        } while (!atomicCounter.compareAndSet(current, next));
        System.out.println("CAS increment result: " + atomicCounter.get());  // 1

        // Concurrent CAS: 100 threads each incrementing 1000 times
        AtomicInteger sharedCounter = new AtomicInteger(0);
        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    sharedCounter.incrementAndGet();  // CAS internally
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        System.out.printf("100 threads × 1000 increments = %d (expected 100000)%n",
                sharedCounter.get());

        // --- ABA Problem Demo ---
        System.out.println("\n--- ABA Problem ---");

        // Without stamped reference: ABA goes undetected
        AtomicReference<String> value = new AtomicReference<>("A");
        String snapshot = value.get();  // Thread 1 reads "A"
        value.set("B");                 // Thread 2: A→B
        value.set("A");                 // Thread 2: B→A (back to original!)
        boolean casResult = value.compareAndSet(snapshot, "C");  // Thread 1: CAS "A"→"C"
        System.out.println("Plain CAS: sees ABA? " + casResult + " (succeeded! ABA not detected)");

        // FIX: AtomicStampedReference pairs (value, stamp)
        // stamp increments on every modification → ABA impossible
        AtomicStampedReference<String> stampedValue = new AtomicStampedReference<>("A", 0);

        int[] stampHolder = new int[1];
        String readValue = stampedValue.get(stampHolder);  // Thread 1 reads ("A", stamp=0)
        int readStamp = stampHolder[0];

        stampedValue.compareAndSet("A", "B", 0, 1);  // Thread 2: A→B (stamp 0→1)
        stampedValue.compareAndSet("B", "A", 1, 2);  // Thread 2: B→A (stamp 1→2)

        // Thread 1 tries CAS with old (value="A", stamp=0)
        boolean stampedResult = stampedValue.compareAndSet(readValue, "C", readStamp, readStamp + 1);
        System.out.println("Stamped CAS: ABA detected? " + !stampedResult + " (CAS failed — ABA prevented!)");
        System.out.println("Current value: " + stampedValue.getReference() + " (still 'A', not 'C')");

        // --- compareAndExchange (Java 9+) ---
        // Returns the WITNESS value (what was actually in memory, not just boolean)
        // Useful to avoid re-reading after failed CAS
        AtomicInteger acex = new AtomicInteger(5);
        int witness = acex.compareAndExchange(5, 10);  // expected=5, update=10
        System.out.printf("%ncompareAndExchange: witness=%d (==5 means success), new value=%d%n",
                witness, acex.get());
    }

    // =========================================================================
    // PATTERN 2: Lock-Free Stack (Treiber Stack)
    // =========================================================================

    /**
     * The Treiber Stack — canonical lock-free data structure (1986).
     *
     * ALGORITHM:
     *   push(v): Create newNode(v, null).
     *     do {
     *       top = head.get()           // read current top
     *       newNode.next = top         // link newNode → old top
     *     } while (!head.CAS(top, newNode))  // try to make newNode the new top
     *
     *   pop(): do {
     *     top = head.get()             // read current top (may be null)
     *     if (top == null) return empty
     *     next = top.next              // read what's below top
     *   } while (!head.CAS(top, next))  // try to remove top
     *   return top.value
     *
     * KEY INSIGHT: CAS on head is the only "critical section".
     * If CAS fails: another push/pop happened concurrently → retry.
     * No thread waits for another → lock-free.
     *
     * WHY NOT JUST SYNCHRONIZED STACK?
     *   Under high contention, synchronized locks cause OS-level context switches.
     *   Lock-free CAS uses pure CPU-level retry → faster under high contention.
     *   Java's ConcurrentLinkedQueue uses a similar Treiber-based approach.
     *
     * ABA RISK in POP:
     *   Thread A reads top=Node(5), next=Node(3).
     *   Thread B pops Node(5) and Node(3), pushes Node(5) back.
     *   Thread A's CAS(top, next) succeeds → but next is now freed/reused!
     *   MITIGATION: Java's GC prevents use-after-free (no raw pointers).
     *   With manual memory management (C/C++), must use hazard pointers or epochs.
     *
     * COMPLEXITY:
     *   push: O(1) amortized (O(k) retries under contention)
     *   pop:  O(1) amortized
     *   Space: O(N)
     */
    public static class LockFreeStack<T> {
        private static class Node<T> {
            final T value;
            Node<T> next;

            Node(T value, Node<T> next) {
                this.value = value;
                this.next = next;
            }
        }

        private final AtomicReference<Node<T>> head = new AtomicReference<>(null);
        private final AtomicInteger size = new AtomicInteger(0);

        /**
         * Push an item onto the stack.
         *
         * CAS LOOP: retry until we successfully become the new head.
         * GOTCHA: Always set newNode.next = currentTop BEFORE the CAS.
         * If you CAS first and then set next, another thread might pop newNode
         * before you set its next pointer → corrupted stack.
         */
        public void push(T value) {
            Node<T> newNode = new Node<>(value, null);
            Node<T> currentTop;
            do {
                currentTop = head.get();
                newNode.next = currentTop;  // Link BEFORE CAS (safe to do outside CAS)
                // KEY: If CAS fails, we just re-read head and retry.
                // The half-linked newNode is not visible to other threads yet.
            } while (!head.compareAndSet(currentTop, newNode));
            size.incrementAndGet();
        }

        /**
         * Pop an item from the stack.
         *
         * CAS LOOP: retry until we successfully update head to head.next.
         * GOTCHA: Check for empty INSIDE the loop (another thread might pop while we retry).
         * @return The top value, or empty Optional if stack is empty.
         */
        public Optional<T> pop() {
            Node<T> currentTop;
            Node<T> nextNode;
            do {
                currentTop = head.get();
                if (currentTop == null) return Optional.empty();  // empty stack
                nextNode = currentTop.next;
                // CAS: try to move head from currentTop to nextNode
            } while (!head.compareAndSet(currentTop, nextNode));
            size.decrementAndGet();
            return Optional.of(currentTop.value);
        }

        public Optional<T> peek() {
            Node<T> top = head.get();
            return top == null ? Optional.empty() : Optional.of(top.value);
        }

        public int size() { return size.get(); }
        public boolean isEmpty() { return head.get() == null; }
    }

    private static void demoLockFreeStack() throws InterruptedException {
        System.out.println("\n=== PATTERN 2: Lock-Free Stack (Treiber) ===");

        LockFreeStack<Integer> stack = new LockFreeStack<>();

        // Basic operations
        stack.push(1);
        stack.push(2);
        stack.push(3);
        System.out.println("Stack after pushing 1,2,3: size=" + stack.size());
        System.out.println("Pop: " + stack.pop() + " (expected 3)");
        System.out.println("Pop: " + stack.pop() + " (expected 2)");
        System.out.println("Pop: " + stack.pop() + " (expected 1)");
        System.out.println("Pop empty: " + stack.pop() + " (expected empty)");

        // Concurrent stress test: 50 pushers + 50 poppers
        LockFreeStack<Integer> concurrentStack = new LockFreeStack<>();
        AtomicInteger successfulPops = new AtomicInteger(0);
        int ITEMS_PER_THREAD = 1000;
        int THREADS = 50;

        Thread[] pushers = new Thread[THREADS];
        Thread[] poppers = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            final int base = i * ITEMS_PER_THREAD;
            pushers[i] = new Thread(() -> {
                for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                    concurrentStack.push(base + j);
                }
            }, "pusher-" + i);
        }

        for (int i = 0; i < THREADS; i++) {
            poppers[i] = new Thread(() -> {
                int popped = 0;
                while (popped < ITEMS_PER_THREAD) {
                    if (concurrentStack.pop().isPresent()) popped++;
                    else Thread.yield();  // give pushers a chance
                }
                successfulPops.addAndGet(popped);
            }, "popper-" + i);
        }

        // Start all concurrently
        for (Thread t : pushers) t.start();
        for (Thread t : poppers) t.start();
        for (Thread t : pushers) t.join();
        for (Thread t : poppers) t.join();

        System.out.printf("Concurrent: %d pushes, %d pops, remaining=%d%n",
                THREADS * ITEMS_PER_THREAD, successfulPops.get(), concurrentStack.size());
        // Stack should be empty (all pushed items were popped)
    }

    // =========================================================================
    // PATTERN 3: Producer-Consumer with Virtual Threads (Java 21)
    // =========================================================================

    /**
     * Virtual Thread Producer-Consumer Pattern.
     *
     * VIRTUAL THREADS (Java 21, "Project Loom"):
     *   - Created by JVM, not OS. Mapped M:N to OS threads (many VTs per OS thread).
     *   - Stack size: ~1KB heap (vs ~512KB for OS thread stack).
     *   - Blocking: when a VT blocks (sleep, I/O, lock), the OS thread is released
     *     to run other VTs — no OS thread wasted.
     *   - CPU bound: VTs don't help — CPU pinned to OS thread anyway.
     *
     * PRODUCER-CONSUMER WITH VTs:
     *   - Producer VT: generates items, puts in BlockingQueue (blocks when full → yields OS thread)
     *   - Consumer VT: takes from queue (blocks when empty → yields OS thread)
     *   - With traditional threads: 1000 consumers = 1000 OS threads = 500MB stack.
     *   - With VTs: 1000 VT consumers = ~1MB heap, same OS thread count as CPU cores.
     *
     * WHEN VTs SHINE:
     *   - HTTP handler threads blocking on DB queries
     *   - Kafka consumers blocking on message availability
     *   - Any "wait for I/O" pattern with high concurrency
     *
     * GOTCHA: VTs and synchronized:
     *   In Java 21 with synchronized blocks, if a VT blocks inside synchronized,
     *   it PINS to the OS thread (doesn't yield). Use ReentrantLock instead.
     *   Java 24 removes this pinning limitation.
     *
     * BlockingQueue + VTs: BlockingQueue uses ReentrantLock internally → VT-friendly.
     */
    private static void demoVirtualThreadProducerConsumer() throws Exception {
        System.out.println("\n=== PATTERN 3: Producer-Consumer with Virtual Threads ===");

        final int CAPACITY = 5;
        final int NUM_ITEMS = 20;
        final int NUM_CONSUMERS = 4;

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(CAPACITY);
        CountDownLatch consumedLatch = new CountDownLatch(NUM_ITEMS);
        AtomicInteger consumedCount = new AtomicInteger(0);

        try (ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            // PRODUCER virtual thread: generates NUM_ITEMS integers
            vtExecutor.submit(() -> {
                System.out.printf("[Producer VT:%s] Starting%n",
                        Thread.currentThread().getName());
                for (int i = 0; i < NUM_ITEMS; i++) {
                    try {
                        queue.put(i);  // blocks if full → VT yields OS thread (no OS thread waste!)
                        System.out.printf("[Producer] put item %d, queue size=%d%n", i, queue.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println("[Producer] Done producing");
            });

            // CONSUMER virtual threads: multiple consumers processing items
            for (int c = 0; c < NUM_CONSUMERS; c++) {
                final int consumerId = c;
                vtExecutor.submit(() -> {
                    System.out.printf("[Consumer-%d VT:%s] Starting%n",
                            consumerId, Thread.currentThread().getName());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            // poll with timeout: don't block forever (for graceful shutdown)
                            Integer item = queue.poll(500, TimeUnit.MILLISECONDS);
                            if (item != null) {
                                // Simulate processing time
                                Thread.sleep(30);  // blocks → VT yields OS thread!
                                int total = consumedCount.incrementAndGet();
                                System.out.printf("[Consumer-%d] processed item %d (total=%d)%n",
                                        consumerId, item, total);
                                consumedLatch.countDown();
                            } else if (consumedLatch.getCount() == 0) {
                                break;  // all consumed, exit
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
            }

            // Wait for all items to be consumed
            boolean finished = consumedLatch.await(10, TimeUnit.SECONDS);
            System.out.printf("All %d items consumed: %b%n", NUM_ITEMS, finished);
            System.out.println("Note: All above used virtual threads (isVirtual=true)");
        }
    }

    // =========================================================================
    // PATTERN 4: Phaser — Dynamic Synchronization Barrier
    // =========================================================================

    /**
     * PHASER vs CYCLICBARRIER vs COUNTDOWNLATCH:
     * ─────────────────────────────────────────────────────────────────────────
     * CountDownLatch:
     *   - Fixed count, single-use. Cannot be reset.
     *   - One-time event: "wait until N things happen"
     *   - USE: waiting for N services to start, N tasks to complete.
     *
     * CyclicBarrier:
     *   - Fixed party count, reusable (resets after each barrier trip).
     *   - All parties must arrive before any continue.
     *   - USE: iterative parallel algorithms ("all workers finish step N before step N+1")
     *   - LIMITATION: fixed party count — can't add/remove participants.
     *
     * Phaser (Java 7+):
     *   - DYNAMIC party count: register()/deregister() at any time.
     *   - Multiple phases: arrive, advance to next phase, repeat.
     *   - arriveAndAwaitAdvance(): like CyclicBarrier.await()
     *   - arriveAndDeregister(): leave the phaser without blocking (I'm done!)
     *   - register(): join the phaser (add a new party mid-flight)
     *   - onAdvance(): override to run custom logic at phase end (like CyclicBarrier action)
     *   - TIERING: Phasers can be hierarchical (tree of phasers) for massive scale.
     *   - USE: multi-phase simulations, partial participation in phases.
     *
     * PHASE NUMBERING: Phaser tracks current phase (0, 1, 2, ...).
     *   After Long.MAX_VALUE phases, wraps to 0. Practically unlimited.
     *
     * COMPLEXITY:
     *   arriveAndAwaitAdvance: O(parties) — waits for all parties to arrive
     *   register/deregister: O(1)
     */
    private static void demoPhaser() throws InterruptedException {
        System.out.println("\n=== PATTERN 4: Phaser — Dynamic Barrier ===");

        // Phaser with 1 initial party (the main thread)
        // We'll add worker parties dynamically
        Phaser phaser = new Phaser(1) {  // 1 = main thread registered
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                // Called at end of each phase. Return true to terminate phaser.
                System.out.printf("[Phaser] Phase %d complete! Parties remaining: %d%n",
                        phase, registeredParties);
                return phase >= 2;  // terminate after phase 2
            }
        };

        int WORKERS = 3;

        // Simulate a multi-phase computation:
        // Phase 0: Initialize data (all workers)
        // Phase 1: Process data (all workers)
        // Phase 2: Finalize (workers deregister when done)
        for (int i = 0; i < WORKERS; i++) {
            final int workerId = i;
            phaser.register();  // dynamically add this worker as a party

            new Thread(() -> {
                System.out.printf("[Worker-%d] Phase 0 (init): starting%n", workerId);
                try { Thread.sleep(50 + workerId * 20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.printf("[Worker-%d] Phase 0 (init): done, waiting at barrier%n", workerId);
                phaser.arriveAndAwaitAdvance();  // wait at Phase 0 barrier

                System.out.printf("[Worker-%d] Phase 1 (process): starting%n", workerId);
                try { Thread.sleep(30 + workerId * 10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                System.out.printf("[Worker-%d] Phase 1 (process): done, waiting at barrier%n", workerId);
                phaser.arriveAndAwaitAdvance();  // wait at Phase 1 barrier

                System.out.printf("[Worker-%d] Phase 2 (finalize): done, deregistering%n", workerId);
                phaser.arriveAndDeregister();  // arrive + leave (don't wait for others in phase 2)
                // KEY: arriveAndDeregister = "I'm done with all phases, remove me from party count"
                // Other workers can still be in phase 2; they don't need to wait for us.

            }, "phaser-worker-" + i).start();
        }

        // Main thread participates in phase 0 and 1
        System.out.println("[Main] Entering Phase 0 barrier");
        phaser.arriveAndAwaitAdvance();  // wait for all workers to finish phase 0

        System.out.println("[Main] Entering Phase 1 barrier");
        phaser.arriveAndAwaitAdvance();  // wait for all workers to finish phase 1

        System.out.println("[Main] Entering Phase 2 barrier (deregister + arrive)");
        phaser.arriveAndDeregister();    // main is done, deregister

        // Wait for phaser to terminate (all parties deregistered)
        while (!phaser.isTerminated()) {
            Thread.sleep(50);
        }
        System.out.println("[Main] Phaser terminated. All phases complete.");

        // --- BULK REGISTRATION DEMO ---
        System.out.println("\n--- Phaser: dynamic registration ---");
        Phaser dynamicPhaser = new Phaser(0);  // start with 0 parties
        // Parties register themselves (useful when you don't know worker count upfront)
        new Thread(() -> {
            dynamicPhaser.register();
            System.out.println("[Dynamic Worker] registered and arriving");
            dynamicPhaser.arrive();
        }).start();

        // Main waits and registers to observe
        dynamicPhaser.register();
        Thread.sleep(200);  // give dynamic worker time to register
        dynamicPhaser.arriveAndDeregister();
        System.out.println("Dynamic phaser phase: " + dynamicPhaser.getPhase());
    }

    // =========================================================================
    // PATTERN 5: CF vs Virtual Threads Decision Guide
    // =========================================================================

    /**
     * DECISION GUIDE: CompletableFuture vs Virtual Threads
     *
     * USE COMPLETABLEFUTURE WHEN:
     *   ✅ Complex async pipelines with transformations (thenApply, thenCompose)
     *   ✅ Fan-out: start N parallel tasks and combine results (allOf, anyOf)
     *   ✅ Fan-in: aggregate results from multiple sources with timeouts
     *   ✅ Non-blocking reactive style is required (Spring WebFlux context)
     *   ✅ You need individual timeouts per async step (orTimeout)
     *   ✅ Integrating with reactive code (Project Reactor, RxJava)
     *
     * USE VIRTUAL THREADS WHEN:
     *   ✅ Code is sequential and blocking (result = doStep1(); doStep2(result))
     *   ✅ High-concurrency I/O bound: thousands of concurrent DB queries, HTTP calls
     *   ✅ Existing code with Thread/Runnable — drop-in replacement (just change executor)
     *   ✅ Readability matters — stack traces are normal, no callback hell
     *   ✅ Structured concurrency (Java 21 preview: StructuredTaskScope)
     *   ✅ Each "request" is an independent unit of work (web servers, gRPC handlers)
     *
     * THEY COMPOSE: Use VT executor with CF!
     *   CompletableFuture.supplyAsync(() -> blockingOperation(), vtExecutor)
     *   → CF pipeline semantics + VT non-blocking I/O efficiency
     *
     * GOTCHA: VTs DON'T help with:
     *   - CPU-bound work (VT still needs an OS thread to compute)
     *   - synchronized blocks (Java 21: VT pins to OS thread inside synchronized)
     *   - ThreadLocal (VTs have ThreadLocals but ScopedValue is preferred in Java 21)
     */
    private static void demoCFvsVirtualThreads() throws Exception {
        System.out.println("\n=== PATTERN 5: CF vs Virtual Threads Comparison ===");

        // --- CF approach: fan-out with aggregation ---
        System.out.println("--- CF: parallel fan-out ---");
        long cfStart = System.currentTimeMillis();
        ExecutorService vtExec = Executors.newVirtualThreadPerTaskExecutor();

        // 5 parallel "DB queries" via CF
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int id = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "QueryResult-" + id;
            }, vtExec));
        }
        CompletableFuture<Void> allCF = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allCF.get();
        List<String> cfResults = futures.stream().map(CompletableFuture::join).toList();
        System.out.printf("CF fan-out: %dms, results=%d%n",
                System.currentTimeMillis() - cfStart, cfResults.size());
        // Expected: ~100ms (all run in parallel)

        // --- Virtual Thread approach: sequential but concurrent ---
        System.out.println("--- Virtual Threads: concurrent sequential tasks ---");
        long vtStart = System.currentTimeMillis();
        List<Future<String>> vtFutures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int id = i;
            vtFutures.add(vtExec.submit(() -> {
                Thread.sleep(100);  // blocking — fine for VT!
                return "VTResult-" + id;
            }));
        }
        List<String> vtResults = new ArrayList<>();
        for (Future<String> f : vtFutures) vtResults.add(f.get());
        System.out.printf("VT concurrent: %dms, results=%d%n",
                System.currentTimeMillis() - vtStart, vtResults.size());
        // Also ~100ms — both approaches parallelize the blocking

        vtExec.shutdown();

        System.out.println("\nKey rule:");
        System.out.println("  Complex pipelines, composition, transforms → CompletableFuture");
        System.out.println("  Simple 'block and wait' with high concurrency → Virtual Threads");
        System.out.println("  Best of both → CF + VT executor!");
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== Modern Concurrency Patterns (Java 21) ===\n");

        demoCAS();
        demoLockFreeStack();
        demoVirtualThreadProducerConsumer();
        demoPhaser();
        demoCFvsVirtualThreads();

        System.out.println("\n=== Pattern Summary ===");
        System.out.println("CAS/AtomicXxx      : lock-free atomic ops, foundation of concurrent structures");
        System.out.println("AtomicStampedRef   : fixes ABA problem with version counter");
        System.out.println("Treiber Stack      : lock-free stack via AtomicReference + CAS loop");
        System.out.println("Virtual Threads    : Java 21, cheap blocking threads for I/O concurrency");
        System.out.println("Phaser             : dynamic barrier — add/remove parties, multiple phases");
        System.out.println("CF + VT executor   : pipeline semantics + VT I/O efficiency combined");

        /*
         * INTERVIEW Q: "What is the difference between lock-free and wait-free?"
         * A: Lock-free: the system as a whole makes progress (some thread always advances),
         *    but individual threads may spin/retry indefinitely under adversarial scheduling.
         *    Wait-free: EVERY individual thread completes in a bounded number of steps,
         *    regardless of other threads. Wait-free is stronger than lock-free.
         *    AtomicInteger.incrementAndGet is lock-free. Making it wait-free requires
         *    helping other threads complete their operations (e.g., Fetch-and-Add instruction).
         *
         * INTERVIEW Q: "When would a CAS loop cause livelock?"
         * A: If N threads all contend on the same CAS, and each failure causes an immediate retry,
         *    all threads may keep failing in sync — a livelock (all "live" but no progress).
         *    FIX: Exponential backoff (Thread.sleep/yield with random delay before retry),
         *    or use a queue-based approach to serialize access.
         *
         * INTERVIEW Q: "What is Structured Concurrency (Java 21 preview)?"
         * A: StructuredTaskScope: fork N subtasks, wait for all (like allOf but cleaner).
         *    Guarantees: if one subtask fails, others are automatically cancelled.
         *    Lifetime: subtasks cannot outlive their enclosing scope (no "leaked" threads).
         *    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *        Future<A> f1 = scope.fork(() -> fetchA());
         *        Future<B> f2 = scope.fork(() -> fetchB());
         *        scope.join().throwIfFailed();
         *        return combine(f1.resultNow(), f2.resultNow());
         *    }
         *    Much cleaner than CompletableFuture.allOf() for this pattern.
         *
         * INTERVIEW Q: "Why are virtual threads not useful for CPU-bound tasks?"
         * A: A VT is always backed by one OS thread when running.
         *    For CPU-bound: the VT never blocks → never yields the OS thread →
         *    same as using a regular thread. You still need as many OS threads
         *    as CPUs to parallelize CPU work. Use ForkJoinPool (work-stealing)
         *    or parallelStream() for CPU-bound parallelism, not VTs.
         */
    }
}
