package com.java.java21;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * ============================================================
 * CONCEPT: Virtual Threads (Java 21) — Structured, Scalable Concurrency
 * ============================================================
 *
 * WHY VIRTUAL THREADS?
 *   Traditional Platform Threads are mapped 1:1 to OS threads.
 *   OS threads are expensive: ~1MB stack, kernel scheduling overhead,
 *   context switches between kernel mode and user mode.
 *   A modern server needs to handle 10,000+ concurrent connections.
 *   10,000 OS threads = ~10GB RAM just for stacks + massive scheduler overhead.
 *
 *   Virtual Threads (Project Loom, Java 19 preview, Java 21 stable):
 *   JVM manages scheduling entirely in user space.
 *   Each VT uses ~few KB of heap (not 1MB stack).
 *   JVM runs millions of VTs on a small pool of carrier (OS) threads.
 *   When a VT blocks (I/O, lock), the JVM detaches it from its carrier thread,
 *   parks it on the heap, and lets another VT run on that same carrier.
 *
 * KEY MENTAL MODEL:
 *   Platform Thread = employee who sits at their desk WAITING for a file to be delivered.
 *   Virtual Thread = employee who goes on break while waiting, and another employee
 *                    takes their desk. When the file arrives, any available desk can handle it.
 *
 * WHEN TO USE VIRTUAL THREADS:
 *   ✅ I/O-bound tasks: HTTP requests, DB queries, file reads (VT shines here)
 *   ✅ High-concurrency servers: REST APIs, gRPC servers, web scrapers
 *   ✅ Simple thread-per-request model (blocking code, but VT-friendly)
 *   ✗ CPU-bound tasks: VT provides no benefit (you're never blocking, always computing)
 *   ✗ Tasks that heavily use synchronized blocks (causes PINNING — see Section 2)
 *
 * TIME / SPACE:
 *   Platform thread: ~1MB stack (configurable, minimum 512KB), OS scheduler managed
 *   Virtual thread:  ~few KB heap allocation, JVM-managed scheduler
 *   Throughput: VT can handle 10x-100x more concurrent tasks than thread pools for I/O-bound work
 *
 * INTERVIEW ANGLE:
 *   "What is a virtual thread and how does it differ from a platform thread?"
 *   "What is pinning and how do you avoid it?"
 *   "When should you NOT use virtual threads?"
 *   "What is structured concurrency in Java 21?"
 *
 * REAL-WORLD:
 *   - Web servers (Spring Boot 3.2+): Tomcat can use VT per request → thread-per-request model scales
 *   - Database connection pooling: VT waiting for DB connection doesn't block a platform thread
 *   - Microservice fan-out: call 5 services in parallel, each on its own VT, await all
 *   - Batch processing: thousands of small tasks, each blocking on I/O
 * ============================================================
 */
public class VirtualThreadsAdvanced {

    // =========================================================
    // SECTION 1: Virtual Thread vs Platform Thread
    // =========================================================

    /**
     * KEY INSIGHT: Virtual threads write the same sequential blocking code as platform threads,
     * but the JVM automatically parks/resumes the VT when it encounters a blocking operation.
     * No callback hell, no reactive chaining, no CompletableFuture nesting needed.
     *
     * Creation comparison:
     *   Platform: Thread.ofPlatform().name("worker").start(task)  [legacy: new Thread(task)]
     *   Virtual:  Thread.ofVirtual().name("worker").start(task)
     *
     * Key API difference:
     *   thread.isVirtual() → true for VT, false for platform thread
     *
     * GOTCHA: VTs are NOT always faster than PTs for single-threaded sequential code.
     *   The benefit is SCALE: you can have 1,000,000 VTs cheaply; 1,000,000 PTs would crash the JVM.
     */
    static void demoVTvsPhysicalThread() throws InterruptedException {
        System.out.println("\n=== Virtual vs Platform Thread Creation ===");

        // Platform thread (OS thread backed)
        Thread platformThread = Thread.ofPlatform()
            .name("platform-1")
            .start(() -> System.out.println(Thread.currentThread().getName()
                + " isVirtual=" + Thread.currentThread().isVirtual()));

        // Virtual thread (JVM-managed, not directly backed by OS thread)
        Thread virtualThread = Thread.ofVirtual()
            .name("virtual-1")
            .start(() -> System.out.println(Thread.currentThread().getName()
                + " isVirtual=" + Thread.currentThread().isVirtual()));

        platformThread.join();
        virtualThread.join();

        // Thread.startVirtualThread() — shorthand for one-off tasks
        Thread.startVirtualThread(() ->
            System.out.println("startVirtualThread: isVirtual=" + Thread.currentThread().isVirtual())
        ).join();
    }

    // =========================================================
    // SECTION 2: Pinning — The Most Important Gotcha
    // =========================================================

    /**
     * KEY INSIGHT — PINNING:
     *   When a virtual thread encounters a BLOCKING operation (e.g., Thread.sleep, I/O),
     *   the JVM "unmounts" it from its carrier platform thread, stores the VT's stack on the heap,
     *   and the carrier thread can then run another VT. This is the "magic" of virtual threads.
     *
     *   PINNING breaks this mechanism: the VT is "pinned" to its carrier PT and cannot be unmounted.
     *   The carrier thread is BLOCKED along with the VT — wasting an OS thread.
     *
     * TWO CAUSES OF PINNING:
     *   1. Holding a SYNCHRONIZED lock while blocking:
     *      synchronized(obj) { Thread.sleep(1000); } → PINS the carrier thread for 1 second!
     *      The JVM cannot unmount a VT that holds a synchronized monitor (Java limitation in Java 21).
     *      FIX: Use ReentrantLock instead of synchronized.
     *
     *   2. Calling a NATIVE METHOD (JNI) while blocking:
     *      Native code may have its own stack management that the JVM can't unwind.
     *      Less common, but be aware.
     *
     * DIAGNOSIS:
     *   JVM flag: -Djdk.tracePinnedThreads=full (logs when pinning occurs)
     *   JFR event: jdk.VirtualThreadPinned
     *
     * NOTE: Java 22+ may remove the synchronized pinning limitation (ongoing work).
     */
    static void demoPinning() throws InterruptedException {
        System.out.println("\n=== Pinning Demo ===");

        Object lock = new Object();
        ReentrantLock reentrantLock = new ReentrantLock();

        // BAD: synchronized block with blocking operation → PINS carrier thread
        // The carrier platform thread is held hostage during the sleep.
        Thread badVT = Thread.ofVirtual().name("pinned-vt").start(() -> {
            synchronized (lock) {
                try {
                    // WARNING: This pins the carrier thread. With many VTs doing this,
                    // all carrier threads may be consumed → starvation.
                    Thread.sleep(10); // blocks while holding synchronized lock → PIN
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        // GOOD: ReentrantLock with blocking operation → does NOT pin
        // JVM can unmount the VT while it waits for the lock or while sleeping after acquiring.
        Thread goodVT = Thread.ofVirtual().name("unpinned-vt").start(() -> {
            reentrantLock.lock();
            try {
                Thread.sleep(10); // JVM CAN unmount here — no monitor held
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                reentrantLock.unlock();
            }
        });

        badVT.join();
        goodVT.join();
        System.out.println("Pinning demo complete. In production: use ReentrantLock, not synchronized, inside VTs.");
    }

    // =========================================================
    // SECTION 3: Thread-Local vs ScopedValue
    // =========================================================

    /**
     * KEY INSIGHT — ThreadLocal Memory Pressure with VTs:
     *   ThreadLocal stores a value per thread. With platform threads (thread pool),
     *   there are maybe 200 threads → 200 ThreadLocal entries. Fine.
     *
     *   With virtual threads, you might have 1,000,000 VTs → 1,000,000 ThreadLocal entries.
     *   Each ThreadLocal value is stored in the Thread object's ThreadLocalMap.
     *   With 1M VTs, this creates 1M ThreadLocalMap entries, holding references to values
     *   that can't be GC'd as long as the VT is alive → MEMORY PRESSURE.
     *
     *   ADDITIONAL PROBLEM: VTs are cheap and often created per-request.
     *   If you use ThreadLocal for request context, you must manually clean it up
     *   (ThreadLocal.remove()) or risk memory leaks in long-running servers.
     *
     * SOLUTION — ScopedValue (Java 21, preview; stable in later versions):
     *   ScopedValue is IMMUTABLE and SCOPED — it exists only within a bounded scope.
     *   No cleanup needed (automatically released when scope exits).
     *   Better performance: no per-thread map, stored in the call stack frame.
     *   Thread-safe: can be shared across threads within the scope (read-only).
     *
     * USAGE COMPARISON:
     *   ThreadLocal: set(value) → ... (anywhere in call stack) → get() → remove() [required!]
     *   ScopedValue: ScopedValue.where(key, value).run(() -> { ... key.get() ... })
     *                Value is available anywhere within the lambda, automatically released after.
     */
    static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    static final ThreadLocal<String> REQUEST_ID_TL = new ThreadLocal<>();

    static void demoThreadLocalVsScopedValue() throws InterruptedException {
        System.out.println("\n=== ThreadLocal vs ScopedValue ===");

        // ThreadLocal approach — requires manual cleanup
        Thread tlThread = Thread.ofVirtual().start(() -> {
            REQUEST_ID_TL.set("req-001"); // must remember to call remove()
            try {
                processRequest(); // nested calls can access the ThreadLocal
            } finally {
                REQUEST_ID_TL.remove(); // CRITICAL: prevent memory leak in VT scenarios
            }
        });
        tlThread.join();

        // ScopedValue approach — automatic cleanup, immutable, safer
        Thread svThread = Thread.ofVirtual().start(() -> {
            ScopedValue.where(REQUEST_ID, "req-002").run(() -> {
                processRequestScoped(); // nested calls can access the ScopedValue
                // No cleanup needed — ScopedValue released when lambda exits
            });
        });
        svThread.join();
    }

    static void processRequest() {
        System.out.println("ThreadLocal RequestId: " + REQUEST_ID_TL.get());
    }

    static void processRequestScoped() {
        System.out.println("ScopedValue RequestId: " + REQUEST_ID.get());
    }

    // =========================================================
    // SECTION 4: ExecutorService with Virtual Threads
    // =========================================================

    /**
     * KEY INSIGHT: Executors.newVirtualThreadPerTaskExecutor() creates a NEW virtual thread
     * for EVERY submitted task. There is no thread pooling — VTs are cheap enough that
     * creating one per task is acceptable (unlike platform threads where pools are essential).
     *
     * WHEN TO USE:
     *   - I/O-bound tasks that block for significant time (DB queries, HTTP calls, file reads)
     *   - High concurrency servers where thread-per-request model is desirable
     *   - Replace Executors.newFixedThreadPool(N) when N was chosen to limit blocking overhead
     *     (with VTs, you don't need a fixed pool — JVM manages the underlying carrier threads)
     *
     * WHEN NOT TO REPLACE WITH VT EXECUTOR:
     *   - CPU-bound tasks: VTs don't help. Use ForkJoinPool or fixed platform thread pool.
     *   - Tasks with synchronized pinning: fix the pinning first, then switch to VTs.
     *   - Connection pools (DB, HTTP): the POOL itself should use VTs, not the connections.
     *     Don't wrap each connection in a VT — use a connection pool (HikariCP), let VTs await.
     *
     * GOTCHA: newVirtualThreadPerTaskExecutor() does NOT limit concurrency.
     *   If 1M tasks arrive simultaneously, 1M VTs are created.
     *   If they all need DB connections (limited pool of 20), 999,980 block on the pool.
     *   Use Semaphore to limit concurrency at the resource level if needed.
     */
    static void demoVTExecutor() throws InterruptedException {
        System.out.println("\n=== Virtual Thread Executor ===");

        int taskCount = 1000;
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);

        // VT executor: one VT per task — no pool size limit needed
        try (ExecutorService vtExec = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = System.currentTimeMillis();

            for (int i = 0; i < taskCount; i++) {
                vtExec.submit(() -> {
                    try {
                        // Simulate I/O blocking (e.g., DB query, HTTP call)
                        Thread.sleep(100); // VT parks during this sleep, doesn't hold OS thread
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Completed " + completed.get() + " tasks in " + elapsed + "ms");
            System.out.println("Expected: ~100ms (all tasks ran concurrently via virtual threads)");
            System.out.println("If using fixedThreadPool(10): would take " + (taskCount / 10 * 100) + "ms");
        }
    }

    // =========================================================
    // SECTION 5: Structured Concurrency (Java 21 Preview)
    // =========================================================

    /**
     * KEY INSIGHT: Structured Concurrency ensures that subtasks don't outlive their parent.
     * Traditional CompletableFuture: fire subtasks, handle exceptions awkwardly, easy to leak tasks.
     * StructuredTaskScope: subtasks are bounded to the scope block. When scope exits, all subtasks
     * have either completed or been cancelled. No leaks possible.
     *
     * TWO MAIN POLICIES:
     *   ShutdownOnSuccess: first successful result wins, cancel all others. Good for: racing alternatives.
     *   ShutdownOnFailure: any failure cancels all others. Good for: parallel fan-out (all must succeed).
     *
     * STRUCTURED CONCURRENCY GUARANTEES:
     *   1. COMPLETENESS: scope.join() blocks until all subtasks finish (or are cancelled).
     *   2. CANCELLATION: if scope exits (normally or via exception), all running subtasks are cancelled.
     *   3. ERROR PROPAGATION: exceptions from subtasks are collected and can be re-thrown.
     *   4. NO LEAKS: impossible to leave orphan tasks running after the scope block exits.
     *
     * GOTCHA (Preview API): StructuredTaskScope is in java.util.concurrent and marked @Preview in Java 21.
     *   Requires --enable-preview flag to compile and run in Java 21.
     *   Stable in Java 25 (expected). API may change between preview versions.
     *
     * COMPARISON:
     *   CompletableFuture.allOf(): no cancellation on failure, complex error handling, easy leaks
     *   StructuredTaskScope: automatic cancellation, clean error propagation, leak-free
     */
    static void demoStructuredConcurrency() throws InterruptedException {
        System.out.println("\n=== Structured Concurrency ===");

        // ShutdownOnFailure: all subtasks must succeed
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            StructuredTaskScope.Subtask<String> task1 = scope.fork(() -> {
                Thread.sleep(50);
                return "Result from task 1";
            });

            StructuredTaskScope.Subtask<String> task2 = scope.fork(() -> {
                Thread.sleep(30);
                return "Result from task 2";
            });

            scope.join();           // wait for all subtasks (or first failure)
            scope.throwIfFailed();  // propagate any exception from subtasks

            // Only reached if ALL tasks succeeded
            System.out.println("task1: " + task1.get());
            System.out.println("task2: " + task2.get());

        } catch (ExecutionException e) {
            System.out.println("A subtask failed: " + e.getCause().getMessage());
        }

        // ShutdownOnSuccess: race multiple approaches, use first to succeed
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {

            scope.fork(() -> { Thread.sleep(100); return "slow-service result"; });
            scope.fork(() -> { Thread.sleep(20);  return "fast-service result"; });

            scope.join();
            System.out.println("First result: " + scope.result()); // "fast-service result"

        } catch (ExecutionException e) {
            System.out.println("All alternatives failed: " + e.getMessage());
        }
    }

    // =========================================================
    // SECTION 6: 10,000 Concurrent Requests — VT vs Platform Thread Pool
    // =========================================================

    /**
     * KEY INSIGHT: The classic "thundering herd" scenario shows the VT advantage most clearly.
     * 10,000 concurrent "requests" each sleeping 1 second (simulating I/O).
     *
     * Platform thread pool (200 threads):
     *   - Only 200 requests run concurrently at a time.
     *   - 10,000 requests / 200 threads = 50 "batches" × 1 second = ~50 seconds.
     *
     * Virtual thread executor:
     *   - All 10,000 VTs created instantly (cheap), all sleep concurrently.
     *   - JVM uses ~8 carrier platform threads (= number of CPU cores).
     *   - All 10,000 complete in ~1 second (all sleeping simultaneously, each parks its VT).
     *
     * The difference: platform threads are blocked (OS thread held). VTs are PARKED (heap only).
     */
    static void demo10kConcurrentRequests() throws InterruptedException {
        System.out.println("\n=== 10,000 Concurrent Requests Simulation ===");

        int requests = 10_000;
        int simulatedIoMs = 50; // simulate 50ms I/O per request
        AtomicInteger completed = new AtomicInteger(0);

        // Virtual Thread approach
        long vtStart = System.currentTimeMillis();
        CountDownLatch vtLatch = new CountDownLatch(requests);
        try (ExecutorService vtExec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requests; i++) {
                vtExec.submit(() -> {
                    try {
                        Thread.sleep(simulatedIoMs); // park VT, don't hold carrier
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        vtLatch.countDown();
                    }
                });
            }
            vtLatch.await();
        }
        long vtElapsed = System.currentTimeMillis() - vtStart;

        // Platform thread pool approach (limited to 200 threads)
        int poolSize = 200;
        completed.set(0);
        long ptStart = System.currentTimeMillis();
        CountDownLatch ptLatch = new CountDownLatch(requests);
        try (ExecutorService ptExec = Executors.newFixedThreadPool(poolSize)) {
            for (int i = 0; i < requests; i++) {
                ptExec.submit(() -> {
                    try {
                        Thread.sleep(simulatedIoMs); // OS thread blocked during sleep
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        ptLatch.countDown();
                    }
                });
            }
            ptLatch.await();
        }
        long ptElapsed = System.currentTimeMillis() - ptStart;

        System.out.printf("Virtual threads (10k requests, %dms I/O each): %dms%n", simulatedIoMs, vtElapsed);
        System.out.printf("Platform pool  (%d threads, %dms I/O each):   %dms%n", poolSize, simulatedIoMs, ptElapsed);
        System.out.printf("Speedup: %.1fx%n", (double) ptElapsed / vtElapsed);
        System.out.println("VT expected: ~" + simulatedIoMs + "ms (all concurrent)");
        System.out.println("PT expected: ~" + (requests / poolSize * simulatedIoMs) + "ms (batched)");
    }

    // =========================================================
    // SECTION 7: When NOT to Use Virtual Threads
    // =========================================================

    /*
     * CPU-BOUND TASKS (VT doesn't help):
     *   VTs only help when a thread BLOCKS (I/O, sleep, lock wait).
     *   A CPU-bound task (sorting, encryption, image processing) never blocks →
     *   the VT never unmounts → the carrier thread is held for the full computation.
     *   No benefit over platform threads. Use ForkJoinPool for CPU-bound parallelism.
     *
     * HEAVY synchronized USAGE:
     *   If your code has many synchronized blocks around I/O or blocking ops,
     *   VTs will be pinned → no benefit, possibly worse (overhead of VT creation).
     *   Migration path: replace synchronized with ReentrantLock where blocking may occur.
     *
     * THREAD-LOCAL HEAVY FRAMEWORKS (without refactoring):
     *   Frameworks that store request context in ThreadLocal may work but create memory pressure.
     *   Refactor to ScopedValues or InheritableThreadLocal with awareness of VT lifecycle.
     *
     * VERY SHORT TASKS (< microseconds):
     *   VT creation has overhead (few microseconds). For sub-microsecond tasks,
     *   this overhead dominates. Use a direct loop or batch processing instead.
     *
     * INTERVIEW ANSWER TEMPLATE:
     *   "Virtual threads are best for I/O-bound workloads with high concurrency.
     *    I wouldn't use them for CPU-bound tasks (no blocking = no benefit),
     *    tasks with heavy synchronized usage (pinning defeats the purpose),
     *    or sub-microsecond tasks where VT creation overhead is significant."
     */

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Virtual Threads Advanced (Java 21)         ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        demoVTvsPhysicalThread();
        demoPinning();
        demoThreadLocalVsScopedValue();
        demoVTExecutor();
        demoStructuredConcurrency();
        demo10kConcurrentRequests();

        System.out.println("\n=== All virtual thread demos complete ===");
    }
}
