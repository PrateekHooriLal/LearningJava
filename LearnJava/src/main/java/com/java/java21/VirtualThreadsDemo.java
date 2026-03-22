package com.java.java21;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =====================================================================
 * JAVA 21 FEATURE: VIRTUAL THREADS (Project Loom)
 * =====================================================================
 *
 * THIS IS THE MOST IMPORTANT JAVA 21 FEATURE FOR LEAD-LEVEL INTERVIEWS.
 * Every interviewer who asks about Java 21 will ask about Virtual Threads.
 *
 * THE PROBLEM WITH TRADITIONAL (PLATFORM) THREADS:
 * -------------------------------------------------------
 * A traditional Java Thread = Platform Thread = OS Thread (1:1 mapping).
 *
 * Problems:
 *   1. EXPENSIVE: Each OS thread needs ~1 MB of stack memory.
 *      A server with 2GB heap can only support ~2000 threads.
 *   2. BLOCKING: When a thread does I/O (DB query, HTTP call), it BLOCKS.
 *      The OS thread sits idle, consuming memory, doing nothing.
 *      For a web server handling 1000 concurrent requests, 1000 OS threads
 *      are mostly just waiting on I/O. HUGE waste.
 *   3. CONTEXT SWITCHING: Switching between OS threads is expensive (kernel operations).
 *
 * THE REACTIVE SOLUTION (before Virtual Threads):
 * Spring WebFlux, Project Reactor, RxJava — non-blocking, async/callback style.
 * Problem: "Callback hell" — code is hard to read, debug, and reason about.
 *   ```
 *   userRepo.findById(id)
 *     .flatMap(user -> orderRepo.findByUser(user))
 *     .flatMap(orders -> paymentService.charge(orders))
 *     .subscribe(result -> ...);
 *   ```
 *
 * VIRTUAL THREADS — THE JAVA 21 SOLUTION:
 * -------------------------------------------------------
 * Virtual threads are LIGHTWEIGHT threads managed by the JVM (not the OS).
 * - Platform threads are the "carrier threads" — a small pool (default: # of CPU cores)
 * - Virtual threads are MOUNTED on carrier threads to run
 * - When a virtual thread BLOCKS on I/O, it is UNMOUNTED from the carrier thread
 *   and the carrier thread picks up another virtual thread
 * - When I/O completes, the virtual thread is re-mounted and resumes
 *
 * RESULT:
 *   - One carrier thread can execute MILLIONS of virtual threads
 *   - Your code looks like NORMAL synchronous code (no callbacks, no flatMap)
 *   - You get the SCALABILITY of reactive without the complexity
 *
 * M:N THREADING MODEL:
 *   M virtual threads : N platform (carrier) threads
 *   Typically N = number of CPU cores (e.g., 8 carrier threads running millions of virtual threads)
 *
 * INTERVIEW MUST-KNOW QUESTIONS:
 *   Q: "How are virtual threads different from platform threads?"
 *   A: Platform = 1:1 with OS thread (expensive, limited). Virtual = JVM-managed,
 *      mounted on carrier threads, cheap (tiny stack, ~few KB), millions possible.
 *
 *   Q: "How are virtual threads different from reactive programming (WebFlux)?"
 *   A: Both achieve high concurrency. Difference:
 *      - Reactive: non-blocking but ASYNC code style (callbacks, flatMap chains) — hard to read/debug
 *      - Virtual threads: non-blocking but SYNCHRONOUS code style — easy to read, stack traces are normal
 *      Virtual threads are the "structured" solution; reactive is the "composition" solution.
 *
 *   Q: "Are virtual threads good for CPU-bound tasks?"
 *   A: NO. Virtual threads shine for I/O-BOUND work (DB queries, HTTP calls, file I/O).
 *      For CPU-bound work, you still need platform threads (one per CPU core).
 *      A CPU-bound virtual thread keeps the carrier thread occupied — same as before.
 *
 *   Q: "What is thread pinning?"
 *   A: When a virtual thread runs inside a `synchronized` block, it gets PINNED to
 *      its carrier thread — the carrier thread cannot pick up other virtual threads.
 *      This defeats the purpose of virtual threads for that block.
 *      FIX: Use `ReentrantLock` instead of `synchronized` — ReentrantLock does NOT pin.
 *      Java 24 is working on removing this restriction.
 *
 *   Q: "How do you enable virtual threads in Spring Boot?"
 *   A: In Spring Boot 3.2+: `spring.threads.virtual.enabled=true` in application.properties.
 *      This makes Tomcat, @Async, and @Scheduled use virtual threads automatically.
 *
 *   Q: "What is Structured Concurrency?"
 *   A: A preview API in Java 21. `StructuredTaskScope` lets you fork subtasks and join them
 *      with built-in error handling and cancellation. Like CompletableFuture.allOf() but
 *      with cleaner resource management and scoped lifetimes.
 *
 * =====================================================================
 */
public class VirtualThreadsDemo {

    // =====================================================================
    // 1. CREATING A VIRTUAL THREAD — 3 ways
    // =====================================================================
    static void demo1_CreatingVirtualThreads() throws InterruptedException {
        System.out.println("=== 1. Creating Virtual Threads ===");

        // WAY 1: Thread.ofVirtual() — builder API (similar to Thread.ofPlatform())
        Thread vt1 = Thread.ofVirtual()
                .name("vt-1")  // optional name for debugging
                .start(() -> System.out.println("  Virtual thread 1 running on: " + Thread.currentThread()));
        vt1.join();  // wait for it to finish

        // WAY 2: Thread.startVirtualThread() — shorthand, no name
        Thread vt2 = Thread.startVirtualThread(() ->
                System.out.println("  Virtual thread 2 isVirtual: " + Thread.currentThread().isVirtual()));
        vt2.join();

        // WAY 3: Thread.Builder (for multiple threads with similar config)
        Thread.Builder builder = Thread.ofVirtual().name("worker-", 0);  // names: worker-0, worker-1, ...
        Thread vt3 = builder.start(() -> System.out.println("  Worker thread via builder"));
        vt3.join();

        // isVirtual() — check if a thread is virtual
        System.out.println("  Main thread isVirtual: " + Thread.currentThread().isVirtual()); // false
    }

    // =====================================================================
    // 2. EXECUTOR SERVICE WITH VIRTUAL THREADS
    // =====================================================================
    // This is the RECOMMENDED way for production — use an ExecutorService,
    // not raw thread creation. Just swap the executor implementation.
    static void demo2_ExecutorService() throws InterruptedException {
        System.out.println("\n=== 2. Virtual Thread ExecutorService ===");

        // Executors.newVirtualThreadPerTaskExecutor():
        //   - Creates ONE new virtual thread PER SUBMITTED TASK
        //   - This is safe because virtual threads are cheap (unlike platform threads)
        //   - For platform threads you'd use a fixed pool (Executors.newFixedThreadPool(N))
        //     because creating too many platform threads would exhaust memory
        //   - For virtual threads: create as many as you need — the JVM handles scheduling

        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // try-with-resources ensures executor.shutdown() + awaitTermination() is called automatically
            // This is the recommended pattern in Java 21 (AutoCloseable ExecutorService)
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        // Simulating I/O wait (e.g., DB query, HTTP call)
                        // When this thread sleeps, the carrier thread is released to run other virtual threads
                        Thread.sleep(10);
                        completedCount.incrementAndGet();
                        System.out.println("  Task " + taskId + " completed on " + Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(5, TimeUnit.SECONDS);
        } // executor.close() called here — waits for all tasks to complete

        System.out.println("  All " + completedCount.get() + " tasks completed");
    }

    // =====================================================================
    // 3. SCALE TEST — Spawn 10,000 virtual threads
    // =====================================================================
    // This demo shows WHY virtual threads exist.
    // With platform threads: 10,000 threads would need ~10GB RAM — likely OOM.
    // With virtual threads: trivial — they're tiny (~few KB each).
    static void demo3_ScaleTest() throws InterruptedException {
        System.out.println("\n=== 3. Scale Test — 10,000 Virtual Threads ===");

        int threadCount = 10_000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(100); // simulate 100ms I/O per thread
                        counter.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(30, TimeUnit.SECONDS);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("  Completed " + counter.get() + " tasks in " + elapsed + "ms");
        // Expected: ~100-200ms (all virtual threads run concurrently on few carrier threads)
        // If using platform threads (fixed pool of 8): ~10,000/8 * 100ms = ~125,000ms = 2 minutes!
    }

    // =====================================================================
    // 4. THREAD PINNING — What to AVOID
    // =====================================================================
    // When a virtual thread runs inside a `synchronized` block, it gets PINNED
    // to its carrier thread. The carrier thread CANNOT pick up other virtual threads
    // while the virtual thread is pinned, even if it blocks.
    //
    // FIX: Replace `synchronized` with `java.util.concurrent.locks.ReentrantLock`
    static void demo4_ThreadPinning() throws InterruptedException {
        System.out.println("\n=== 4. Thread Pinning (What to Avoid) ===");

        // BAD: synchronized + virtual thread = pinning
        Object lock = new Object();
        Thread pinned = Thread.ofVirtual().start(() -> {
            synchronized (lock) {
                // While in here, this virtual thread is PINNED to its carrier thread
                // If it blocks (e.g., Thread.sleep, I/O), the carrier thread is also blocked
                // This defeats the purpose of virtual threads
                System.out.println("  [BAD] In synchronized block — thread is pinned");
                try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        pinned.join();

        // GOOD: ReentrantLock + virtual thread = no pinning
        java.util.concurrent.locks.ReentrantLock reentrantLock = new java.util.concurrent.locks.ReentrantLock();
        Thread notPinned = Thread.ofVirtual().start(() -> {
            reentrantLock.lock();
            try {
                // ReentrantLock does NOT pin the virtual thread
                // If this thread blocks, the carrier thread can pick up other virtual threads
                System.out.println("  [GOOD] In ReentrantLock block — thread is NOT pinned");
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                reentrantLock.unlock(); // ALWAYS unlock in finally!
            }
        });
        notPinned.join();

        System.out.println("  Rule: Replace synchronized with ReentrantLock in virtual thread contexts");
    }

    // =====================================================================
    // 5. VIRTUAL THREADS vs PLATFORM THREADS — Comparison
    // =====================================================================
    static void demo5_Comparison() {
        System.out.println("\n=== 5. Summary: Virtual vs Platform Threads ===");
        System.out.println("  Feature               | Platform Thread      | Virtual Thread");
        System.out.println("  ----------------------|----------------------|----------------------");
        System.out.println("  OS mapping            | 1:1 (OS thread)      | M:N (carrier threads)");
        System.out.println("  Stack size            | ~1 MB                | ~few KB");
        System.out.println("  Max count (practical) | ~thousands           | millions");
        System.out.println("  I/O blocking          | Blocks OS thread     | Unmounts, carrier free");
        System.out.println("  Code style            | Sync/Async           | Always synchronous");
        System.out.println("  Good for              | CPU-bound            | I/O-bound");
        System.out.println("  synchronized          | No issue             | Causes pinning");
        System.out.println("  Thread local          | Works fine           | Use carefully (millions!)");

        System.out.println("\n  Spring Boot 3.2+ integration:");
        System.out.println("  spring.threads.virtual.enabled=true");
        System.out.println("  → Tomcat uses virtual threads per request");
        System.out.println("  → @Async uses virtual threads");
        System.out.println("  → @Scheduled uses virtual threads");
    }

    // =====================================================================
    // MAIN — Run all demos
    // =====================================================================
    public static void main(String[] args) throws InterruptedException {
        demo1_CreatingVirtualThreads();
        demo2_ExecutorService();
        demo3_ScaleTest();
        demo4_ThreadPinning();
        demo5_Comparison();
    }
}
