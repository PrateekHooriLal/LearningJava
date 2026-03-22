package com.java.multithreading.demo;

import java.util.concurrent.Semaphore;

/**
 * SEMAPHORE — Controlling Access to Limited Resources
 *
 * CONCEPT:
 *   A Semaphore maintains a set of PERMITS. Threads must acquire() a permit before
 *   proceeding and release() it when done. If no permits are available, acquire() blocks.
 *
 *   - Counting Semaphore: N permits → allows N threads to proceed concurrently.
 *   - Binary Semaphore: 1 permit → similar to mutex, but with important differences.
 *
 * KEY DIFFERENCE FROM synchronized/ReentrantLock:
 *   - A thread that acquires a lock MUST release it (same thread).
 *   - A semaphore permit can be released by a DIFFERENT thread.
 *     This enables use cases like signaling between producer and consumer.
 *
 * REAL-WORLD USES:
 *   - Connection pool: max N concurrent DB connections (counting semaphore, N permits).
 *   - Rate limiter: limit concurrent API requests.
 *   - Parking lot: N parking spots (each spot = 1 permit).
 *   - Thread signaling: producer signals consumer (1 permit, starts at 0).
 *
 * INTERVIEW FREQUENCY: Medium-high for Lead/Senior roles.
 *
 * COMMON QUESTIONS:
 *   1. "Semaphore vs synchronized?" → synchronized: one thread at a time, same-thread release.
 *      Semaphore: N threads at a time, different-thread release possible.
 *   2. "Binary Semaphore vs mutex?" → mutex has ownership (only acquirer can release).
 *      Binary semaphore has no ownership (any thread can release).
 *   3. "How is Semaphore used in Dining Philosophers?"
 *      Each fork = semaphore(1). Philosopher acquires 2 forks to eat, releases both when done.
 *   4. "What happens if release() is called more than acquire()?"
 *      Permits increase beyond initial value! This is a bug. Use carefully.
 */
public class SemaphoreDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Use Case 1: Database Connection Pool ===");
        System.out.println("(Max 3 concurrent connections, 8 threads compete)\n");
        connectionPool();

        System.out.println("\n=== Use Case 2: Binary Semaphore as Signaling ===");
        System.out.println("(Producer signals consumer via semaphore)\n");
        binarySignaling();
    }

    // =========================================================================
    // USE CASE 1: Connection Pool Simulation
    // =========================================================================

    /**
     * Simulates a DB connection pool with exactly MAX_CONNECTIONS permits.
     * 8 threads compete but only 3 can hold a connection at once.
     */
    static void connectionPool() throws InterruptedException {
        final int MAX_CONNECTIONS = 3;
        final int THREAD_COUNT = 8;

        // Counting semaphore: exactly 3 permits (3 simultaneous connections allowed)
        Semaphore connectionPool = new Semaphore(MAX_CONNECTIONS, true); // true = fair mode (FIFO)

        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                System.out.printf("[Thread-%d] Requesting DB connection...%n", id);

                try {
                    connectionPool.acquire(); // Block if no permits available
                    System.out.printf("[Thread-%d] Got connection! (Available: %d)%n",
                            id, connectionPool.availablePermits());

                    // Simulate DB work
                    Thread.sleep((long)(Math.random() * 500 + 200));

                    System.out.printf("[Thread-%d] Done. Releasing connection.%n", id);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    connectionPool.release(); // Always release in finally — even if exception occurs
                }
            }, "T" + id);
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.printf("%nFinal available permits: %d (should be %d)%n",
                connectionPool.availablePermits(), MAX_CONNECTIONS);
    }

    // =========================================================================
    // USE CASE 2: Binary Semaphore for Thread Signaling
    // =========================================================================

    /**
     * Binary semaphore (1 permit) used as a signal:
     * Consumer waits until Producer signals it.
     *
     * NOTE: Initial permits = 0 (consumer blocks immediately on acquire).
     * Producer calls release() after producing data — "wakes" the consumer.
     *
     * Unlike wait/notify, semaphore doesn't require synchronized blocks.
     * Unlike CountDownLatch, this can be reused (release and re-acquire).
     */
    static void binarySignaling() throws InterruptedException {
        // Start with 0 permits — consumer will block until producer releases 1
        Semaphore signal = new Semaphore(0);
        String[] sharedData = {null}; // Shared memory (in production, use AtomicReference)

        Thread producer = new Thread(() -> {
            try {
                System.out.println("[Producer] Preparing data...");
                Thread.sleep(800); // Simulate work
                sharedData[0] = "Important data payload";
                System.out.println("[Producer] Data ready. Signaling consumer.");
                signal.release(); // Give 1 permit → consumer can proceed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                System.out.println("[Consumer] Waiting for data...");
                signal.acquire(); // Block until producer releases a permit
                System.out.println("[Consumer] Received signal. Data: " + sharedData[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        consumer.start(); // Consumer starts waiting immediately
        producer.start(); // Producer starts preparing

        producer.join();
        consumer.join();
    }
}
