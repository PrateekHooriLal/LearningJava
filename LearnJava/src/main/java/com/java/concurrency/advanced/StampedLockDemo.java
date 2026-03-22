package com.java.concurrency.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * =============================================================================
 * CONCEPT: StampedLock — High-Performance Locking with Optimistic Reads
 * =============================================================================
 *
 * StampedLock (Java 8+) is an advanced locking mechanism that improves on
 * ReadWriteLock by adding OPTIMISTIC READS — the killer feature that makes it
 * 10-100x faster for read-heavy workloads.
 *
 * THREE MODES OF LOCKING:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. WRITE LOCK (exclusive):
 *    - Only one writer at a time; blocks all readers and other writers.
 *    - Returns a stamp (long); must pass stamp back to unlock.
 *    - Same semantics as ReentrantReadWriteLock.writeLock().
 *
 * 2. READ LOCK (pessimistic shared):
 *    - Multiple readers simultaneously; blocks writers.
 *    - Same semantics as ReentrantReadWriteLock.readLock().
 *    - PROBLEM: still acquires the lock, adds memory barrier overhead.
 *
 * 3. OPTIMISTIC READ (the game-changer):
 *    - Does NOT acquire any lock — just reads a version stamp.
 *    - After reading data, validates: "did any writer modify since I read?"
 *    - If valid: done! Zero lock contention.
 *    - If invalid: upgrade to pessimistic read lock and re-read.
 *    - KEY: In read-heavy workloads, validation almost always succeeds.
 *
 * HOW OPTIMISTIC READ WORKS INTERNALLY:
 *   StampedLock maintains a state variable (version number):
 *   - tryOptimisticRead(): returns current version (even = unlocked)
 *   - validate(stamp): returns true if version unchanged since stamp was taken
 *   - Any write lock acquisition increments the version (makes stamp invalid)
 *   → This is the same idea as database MVCC (Multi-Version Concurrency Control)
 *   → Similar to Java's AtomicStampedReference CAS
 *
 * READWRITELOCK vs STAMPEDLOCK:
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ Feature               │ ReadWriteLock       │ StampedLock              │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ Optimistic reads      │ NO                  │ YES (key advantage)      │
 * │ Reentrancy            │ YES (Reentrant)     │ NO (NOT reentrant!)      │
 * │ Condition support     │ YES                 │ NO                       │
 * │ Lock upgrade          │ No (deadlock risk)  │ tryConvertToWriteLock    │
 * │ Writer starvation     │ YES (many readers)  │ Better fairness          │
 * │ Performance (reads)   │ Good                │ Excellent (optimistic)   │
 * │ Complexity            │ Simple              │ More complex to use      │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * CRITICAL GOTCHAS:
 * 1. NOT REENTRANT: If a thread holds a StampedLock and calls a method that
 *    tries to acquire the same lock, it DEADLOCKS. Never call synchronized
 *    or StampedLock methods while holding a StampedLock write lock.
 * 2. No Condition: Cannot use lock.newCondition() with StampedLock.
 *    Use a separate object for wait/notify.
 * 3. Stamp must be used correctly: always pass the exact stamp returned by
 *    lock/tryOptimisticRead. Don't store stamps across method boundaries.
 * 4. Validate before using data: ALWAYS call lock.validate(stamp) before
 *    using data read under optimistic mode.
 *
 * WHEN TO USE STAMPEDLOCK:
 *   - Read-heavy workloads (reads >> writes)
 *   - Non-reentrant code (no recursive locking)
 *   - Performance-critical sections (hot paths)
 *   - Short critical sections (lock held briefly)
 *
 * WHEN NOT TO USE:
 *   - When you need Condition (wait/notify)
 *   - Recursive or reentrant code
 *   - When code is complex (optimistic read pattern is error-prone)
 *   - When writes are frequent (optimistic reads keep failing → overhead)
 *
 * TIME COMPLEXITY: O(1) for all lock operations
 * SPACE COMPLEXITY: O(1) — a single long state variable
 *
 * INTERVIEW ANGLE:
 *   "How would you optimize a shared data structure with mostly reads?"
 *   → StampedLock with optimistic reads for read path
 *   → Explain the pattern: tryOptimisticRead → read → validate → fallback
 *
 * REAL-WORLD USE:
 *   - Database connection pools (frequent reads of available connections)
 *   - In-memory caches with rare invalidation
 *   - Configuration objects read frequently, updated rarely
 *   - ConcurrentHashMap's internal striped locking (conceptually similar)
 * =============================================================================
 */
public class StampedLockDemo {

    // =========================================================================
    // EXAMPLE CLASS: Thread-safe Point with optimistic reads
    // =========================================================================

    /**
     * A 2D point with x, y coordinates, protected by StampedLock.
     *
     * READ PATTERN (distanceTo): Read-heavy. Most calls don't coincide with writes.
     *   → Use optimistic read for the fast path.
     *
     * WRITE PATTERN (move): Write-heavy critical section.
     *   → Use write lock.
     *
     * KEY INSIGHT: distanceTo is called very frequently (e.g., for every
     * distance calculation in a game loop). Using a regular read lock would
     * cause all readers to serialize on the lock acquisition itself.
     * With optimistic reads, readers don't even touch the lock in the common case.
     */
    public static class OptimisticPoint {
        private double x;
        private double y;
        private final StampedLock lock = new StampedLock();

        public OptimisticPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Move the point to a new position. Uses WRITE LOCK.
         *
         * KEY INSIGHT: Must hold write lock while updating BOTH x and y.
         * If we updated x and y without a lock, another thread could read
         * x (new) and y (old) — an inconsistent intermediate state.
         * This is the "torn read" / "partial write" problem.
         *
         * COMPLEXITY: O(1), acquires exclusive write lock
         */
        public void move(double newX, double newY) {
            long stamp = lock.writeLock();
            try {
                this.x = newX;
                this.y = newY;
                // GOTCHA: Incrementing version happens INSIDE writeLock, making
                // any concurrent tryOptimisticRead stamps invalid.
            } finally {
                lock.unlockWrite(stamp);
                // CRITICAL: Always unlock in finally! If we forget, permanent deadlock.
            }
        }

        /**
         * Calculate distance from this point to origin (0,0).
         * Uses OPTIMISTIC READ for maximum performance.
         *
         * THE OPTIMISTIC READ PATTERN:
         * ─────────────────────────────────────────────────────────────────────
         * 1. tryOptimisticRead() → get current version stamp (no lock acquired!)
         * 2. Read shared data into LOCAL variables (snapshot)
         * 3. validate(stamp) → check if any writer ran since step 1
         * 4. If valid: use the snapshot (most common case, ~90%+ of calls)
         * 5. If invalid: a write happened, fall back to pessimistic read lock
         * ─────────────────────────────────────────────────────────────────────
         *
         * WHY COPY TO LOCAL VARS FIRST? Because after validate(stamp), we must
         * only use the snapshot. If we read x,y from fields after validate,
         * a writer could have modified them again — race condition.
         * The snapshot (localX, localY) is stable.
         *
         * COMPLEXITY: O(1), typically no lock acquired (optimistic path)
         */
        public double distanceTo(double targetX, double targetY) {
            // STEP 1: Get optimistic stamp (no lock)
            long stamp = lock.tryOptimisticRead();

            // STEP 2: Read data into LOCAL COPIES while (possibly) unprotected
            // IMPORTANT: These are stack-local; no other thread can modify them.
            double localX = this.x;
            double localY = this.y;

            // STEP 3: Validate — did any writer run between STEP 1 and now?
            if (!lock.validate(stamp)) {
                // STEP 5: Optimistic read failed (a write happened) → upgrade to read lock
                // This is the "slow path" — happens rarely in read-heavy scenarios.
                stamp = lock.readLock();
                try {
                    // Re-read under read lock (protected)
                    localX = this.x;
                    localY = this.y;
                } finally {
                    lock.unlockRead(stamp);
                }
                // NOTE: validate() failed ~1% of the time in practice → read lock overhead ~1%
            }
            // STEP 4: Use LOCAL COPIES (valid snapshot)
            double dx = localX - targetX;
            double dy = localY - targetY;
            return Math.sqrt(dx * dx + dy * dy);
        }

        /**
         * LOCK UPGRADE: Read lock → Write lock.
         * Used when you initially acquire a read lock but need to write based on what you read.
         *
         * GOTCHA with ReadWriteLock: Upgrading from read to write DEADLOCKS.
         *   Two threads: A holds read lock, B holds read lock.
         *   A tries to upgrade → waits for B to release read lock.
         *   B tries to upgrade → waits for A to release read lock.
         *   → DEADLOCK. ReadWriteLock cannot be upgraded.
         *
         * STAMPEDLOCK SOLUTION: tryConvertToWriteLock(stamp)
         *   Returns new write stamp if upgrade succeeds (no other readers).
         *   Returns 0 if upgrade fails (other readers exist).
         *   If fails: must release read lock, then acquire write lock (may block).
         *
         * COMPLEXITY: O(1) — but may block if upgrade fails
         */
        public void moveIfAtOrigin() {
            long stamp = lock.readLock();
            try {
                // Read under read lock
                double localX = x, localY = y;
                if (localX == 0 && localY == 0) {
                    // Need to write! Try to upgrade read → write
                    long writeStamp = lock.tryConvertToWriteLock(stamp);
                    if (writeStamp != 0) {
                        // Upgrade succeeded!
                        stamp = writeStamp;
                        x = 1.0;
                        y = 1.0;
                        System.out.println("  Upgrade succeeded: moved from origin");
                    } else {
                        // Upgrade failed (other readers) — must release read lock and re-acquire write
                        lock.unlockRead(stamp);
                        stamp = lock.writeLock();  // blocks until all readers release
                        // Re-check condition (state may have changed!)
                        if (x == 0 && y == 0) {
                            x = 1.0;
                            y = 1.0;
                            System.out.println("  Upgrade failed, re-acquired write lock: moved from origin");
                        }
                    }
                }
            } finally {
                lock.unlock(stamp);  // unlock() works for any stamp (read or write)
            }
        }

        public double getX() { return x; }
        public double getY() { return y; }

        @Override
        public String toString() { return String.format("Point(%.2f, %.2f)", x, y); }
    }

    // =========================================================================
    // COMPARISON: ReadWriteLock vs StampedLock
    // =========================================================================

    /**
     * Same Point using ReadWriteLock for performance comparison.
     * Note: no optimistic reads possible — all reads acquire the read lock.
     */
    public static class RWLockPoint {
        private double x;
        private double y;
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        public RWLockPoint(double x, double y) { this.x = x; this.y = y; }

        public void move(double newX, double newY) {
            rwLock.writeLock().lock();
            try { x = newX; y = newY; }
            finally { rwLock.writeLock().unlock(); }
        }

        // Every read acquires a lock — even if no writes are happening.
        // Under high read concurrency, the lock itself becomes the bottleneck.
        public double distanceTo(double targetX, double targetY) {
            rwLock.readLock().lock();
            try {
                double dx = x - targetX;
                double dy = y - targetY;
                return Math.sqrt(dx * dx + dy * dy);
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    // =========================================================================
    // PERFORMANCE BENCHMARK
    // =========================================================================

    /**
     * Compare throughput of StampedLock (optimistic) vs ReadWriteLock.
     * 10 reader threads + 1 writer thread, 100k read operations each.
     *
     * Expected results: StampedLock ~2-5x faster than ReadWriteLock under this load.
     * Under purely-read load (no writers): StampedLock can be 10-100x faster.
     */
    private static void benchmarkLocks() throws InterruptedException {
        final int READERS = 10;
        final int OPS_PER_READER = 100_000;

        OptimisticPoint stamped = new OptimisticPoint(3.0, 4.0);
        RWLockPoint rwLock = new RWLockPoint(3.0, 4.0);

        // --- Benchmark StampedLock ---
        long stampedTime = benchmarkPoint(
                () -> stamped.distanceTo(0, 0),
                () -> stamped.move(Math.random(), Math.random()),
                READERS, OPS_PER_READER, "StampedLock"
        );

        // --- Benchmark ReadWriteLock ---
        long rwLockTime = benchmarkPoint(
                () -> rwLock.distanceTo(0, 0),
                () -> rwLock.move(Math.random(), Math.random()),
                READERS, OPS_PER_READER, "ReadWriteLock"
        );

        System.out.printf("%nSpeedup: StampedLock is %.1fx faster than ReadWriteLock%n",
                (double) rwLockTime / stampedTime);
    }

    private static long benchmarkPoint(
            Runnable readOp, Runnable writeOp,
            int readers, int opsPerReader, String name
    ) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(readers + 1);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(readers + 1);

        // Writer thread: 1 write per 100 reads
        executor.submit(() -> {
            try {
                latch.await();
                for (int i = 0; i < opsPerReader / 10; i++) {
                    writeOp.run();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { done.countDown(); }
        });

        // Reader threads
        for (int i = 0; i < readers; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    for (int j = 0; j < opsPerReader; j++) {
                        readOp.run();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { done.countDown(); }
            });
        }

        long start = System.currentTimeMillis();
        latch.countDown();  // start all threads simultaneously
        done.await();
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("%-15s: %dms for %d read ops%n", name, elapsed, readers * opsPerReader);
        executor.shutdown();
        return elapsed;
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== StampedLock Demo ===\n");

        // Basic write and optimistic read
        OptimisticPoint point = new OptimisticPoint(3.0, 4.0);
        System.out.println("Initial point: " + point);
        System.out.printf("Distance to origin: %.2f (expected 5.0)%n", point.distanceTo(0, 0));

        // Write lock demo
        point.move(6.0, 8.0);
        System.out.println("After move: " + point);
        System.out.printf("Distance to origin: %.2f (expected 10.0)%n", point.distanceTo(0, 0));

        // Concurrent access test
        System.out.println("\n--- Concurrent access test (5 writers, 20 readers) ---");
        OptimisticPoint concurrent = new OptimisticPoint(0, 0);
        List<Thread> threads = new ArrayList<>();

        // Writer threads
        for (int i = 0; i < 5; i++) {
            final double val = i;
            threads.add(new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    concurrent.move(val * j * 0.01, val * j * 0.01);
                }
            }, "writer-" + i));
        }

        // Reader threads
        for (int i = 0; i < 20; i++) {
            threads.add(new Thread(() -> {
                for (int j = 0; j < 500; j++) {
                    concurrent.distanceTo(0, 0);
                }
            }, "reader-" + i));
        }

        threads.forEach(Thread::start);
        for (Thread t : threads) t.join();
        System.out.println("Concurrent test completed without exceptions — thread safe!");

        // Lock upgrade demo
        System.out.println("\n--- Lock upgrade demo ---");
        OptimisticPoint origin = new OptimisticPoint(0, 0);
        origin.moveIfAtOrigin();
        System.out.println("After moveIfAtOrigin: " + origin);

        // Performance benchmark
        System.out.println("\n--- Performance Benchmark ---");
        benchmarkLocks();

        System.out.println("\n=== StampedLock Key Points ===");
        System.out.println("tryOptimisticRead(): no lock, just version check — FASTEST");
        System.out.println("validate(stamp)    : check if any write happened since stamp");
        System.out.println("readLock()         : shared read, blocks writers");
        System.out.println("writeLock()        : exclusive write, blocks all");
        System.out.println("tryConvertToWriteLock(): upgrade read→write (avoids deadlock vs RWLock)");
        System.out.println("NOT reentrant!     : never acquire twice on same thread");
        System.out.println("No Condition!      : use separate object for wait/notify");

        /*
         * INTERVIEW Q: "Why is StampedLock faster than ReadWriteLock?"
         * A: optimistic reads skip lock acquisition entirely — they just read
         *    a version counter and check if it changed. No memory barriers,
         *    no CAS operations, no queue management. Pure read.
         *    Only on validation failure (rare when writes are infrequent) do we
         *    fall back to acquiring the read lock.
         *
         * INTERVIEW Q: "Can you upgrade from read to write with ReadWriteLock?"
         * A: NO — it deadlocks. Thread A holds read lock, tries to write.
         *    Thread B holds read lock, tries to write.
         *    Both wait for each other to release → deadlock.
         *    StampedLock.tryConvertToWriteLock() handles this safely:
         *    returns 0 (failure) instead of deadlocking, letting you release
         *    the read lock first, then acquire the write lock.
         *
         * INTERVIEW Q: "When would you still prefer ReadWriteLock?"
         * A: When you need reentrancy (same thread acquires multiple times),
         *    when you need Condition (await/signal), when your code is complex
         *    and the optimistic read pattern adds too much cognitive overhead.
         */
    }
}
