package com.java.multithreading.demo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * COUNTDOWNLATCH — One-Time Barrier / Synchronization Point
 *
 * CONCEPT:
 *   CountDownLatch is initialized with a count N.
 *   - Worker threads call countDown() when they complete their work.
 *   - The waiting thread(s) call await() which BLOCKS until the count reaches 0.
 *   - Once the count reaches 0, await() unblocks — ALL waiting threads proceed.
 *
 * KEY CHARACTERISTICS:
 *   - ONE-TIME USE: once the count reaches 0, it stays 0 forever.
 *     Cannot be reset. Use CyclicBarrier if you need reuse (see CyclicBarrierDemo.java).
 *   - Any number of threads can call await() — they all unblock when count == 0.
 *   - The threads calling countDown() don't need to wait for each other.
 *
 * INTERNAL MECHANISM:
 *   Uses AbstractQueuedSynchronizer (AQS) — the backbone of Java's Lock/Condition framework.
 *   await() parks the thread (O(1) overhead). countDown() decrements the AQS state;
 *   when state reaches 0, it unparks all waiting threads.
 *
 * REAL-WORLD USE CASES:
 *   1. Start gate: All threads wait for a "start" signal (latch count=1, main thread counts down).
 *   2. End gate: One thread waits for all workers to finish (latch count=N workers).
 *   3. Service initialization: App waits until all dependencies (DB, cache, Kafka) are ready.
 *
 * COMPARISON:
 *   CountDownLatch: N → 0, one-time, multiple awaiters.
 *   CyclicBarrier: threads wait for each other, reusable (see CyclicBarrierDemo.java).
 *   Semaphore: controls access count (permits), not one-time.
 *
 * INTERVIEW FREQUENCY: High at Lead/Senior rounds.
 *
 * COMMON QUESTIONS:
 *   1. "CountDownLatch vs CyclicBarrier?" → CountDownLatch one-time, CyclicBarrier reusable.
 *   2. "Can you reset a CountDownLatch?" → No. That's CyclicBarrier.
 *   3. "What happens if countDown() is called more than N times?" → Nothing; count stays at 0.
 *   4. "What if a worker throws an exception before countDown()?" → Other threads wait forever!
 *      Always use try/finally: latch.countDown() in finally block.
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Use Case 1: Startup Barrier ===");
        System.out.println("(All services must start before the app opens to traffic)\n");
        startupBarrier();

        System.out.println("\n=== Use Case 2: Parallel Computation ===");
        System.out.println("(Split a large task across threads, gather results)\n");
        parallelComputation();
    }

    // =========================================================================
    // USE CASE 1: Service Startup Barrier
    // =========================================================================

    /**
     * Simulates waiting for multiple services to start before accepting traffic.
     * Main thread waits; each service thread calls countDown() when ready.
     */
    static void startupBarrier() throws InterruptedException {
        String[] services = {"DatabaseService", "CacheService", "KafkaService"};

        // Latch count = number of services to start
        CountDownLatch readyLatch = new CountDownLatch(services.length);

        System.out.println("[Main] Starting " + services.length + " services...");

        // Launch each service in its own thread
        for (String service : services) {
            new Thread(() -> {
                try {
                    System.out.println("[" + service + "] Initializing...");
                    Thread.sleep((long)(Math.random() * 1000 + 200)); // Simulate init time

                    System.out.println("[" + service + "] READY ✓");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // CRITICAL: always in finally — ensures countDown even if exception occurs
                    readyLatch.countDown();
                }
            }, service).start();
        }

        System.out.println("[Main] Waiting for all services to start...");
        readyLatch.await(); // BLOCKS here until count reaches 0

        // All services ready — now safe to open traffic
        System.out.println("[Main] All services ready! Opening traffic. ✓");
    }

    // =========================================================================
    // USE CASE 2: Parallel Computation
    // =========================================================================

    /**
     * Splits a sum computation across N threads, main thread waits for all to finish.
     * Demonstrates: worker threads do parallel work, one coordinator waits.
     */
    static void parallelComputation() throws InterruptedException {
        int threadCount = 4;
        int[] array = new int[100]; // Array to sum
        for (int i = 0; i < array.length; i++) array[i] = i + 1; // 1 to 100

        long[] partialSums = new long[threadCount]; // Each thread stores its partial sum
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        int chunkSize = array.length / threadCount; // Each thread handles one chunk

        System.out.printf("[Main] Computing sum of 1..%d using %d threads%n", array.length, threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadIdx = t;
            final int from = t * chunkSize;
            final int to = (t == threadCount - 1) ? array.length : from + chunkSize; // Last thread takes remainder

            pool.submit(() -> {
                try {
                    long sum = 0;
                    for (int i = from; i < to; i++) sum += array[i];
                    partialSums[threadIdx] = sum;
                    System.out.printf("[Thread-%d] Sum of [%d..%d] = %d%n", threadIdx, from+1, to, sum);
                } finally {
                    doneLatch.countDown(); // Signal this thread is done
                }
            });
        }

        doneLatch.await(); // Main thread waits for all workers

        // Combine partial results
        long total = 0;
        for (long partial : partialSums) total += partial;

        System.out.printf("[Main] Total sum = %d (expected: %d)%n", total, (long)array.length*(array.length+1)/2);

        pool.shutdown();
    }
}
