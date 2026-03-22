package com.java.lld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * =============================================================================
 * CONCEPT: Custom Thread Pool — From Scratch
 * =============================================================================
 *
 * A thread pool amortizes the cost of thread creation by reusing a fixed number
 * of long-lived worker threads across many tasks.
 *
 * WHY NOT new Thread() FOR EVERY TASK?
 *   - Thread creation: ~1ms, stack allocation: ~512KB per thread (JVM default)
 *   - 1000 concurrent requests = 500MB stack memory + 1s thread creation overhead
 *   - Thread pools: threads are pre-created and reused, reducing both costs to ~0
 *
 * CORE COMPONENTS:
 *   1. Task Queue (BlockingQueue<Runnable>) — pending work
 *   2. Worker Threads — loop taking tasks from queue and executing them
 *   3. Lifecycle Management — shutdown gracefully, drain remaining tasks
 *   4. Rejection Policy — what to do when queue is full
 *
 * BLOCKING QUEUE CHOICE:
 *   - LinkedBlockingQueue: unbounded (risk: OOM under heavy load), O(1) ops
 *   - ArrayBlockingQueue: bounded (back-pressure, safer), O(1) ops
 *   - SynchronousQueue: no buffering, each submit blocks until a thread picks it up
 *     (used by CachedThreadPool — direct handoff)
 *
 * THREAD POOL TYPES (Java built-in Executors):
 * ─────────────────────────────────────────────────────────────────────────────
 * FixedThreadPool(n):
 *   - Fixed n workers, UNBOUNDED LinkedBlockingQueue
 *   - DANGER: queue grows without bound under sustained high load → OOM
 *   - USE: known, bounded workloads (e.g., 4 CPU-bound workers = num cores)
 *
 * CachedThreadPool:
 *   - 0 min threads, Integer.MAX_VALUE max threads
 *   - SynchronousQueue (zero capacity) — immediate handoff
 *   - Threads kept alive 60s then killed
 *   - DANGER: thread explosion under high load (max = Integer.MAX_VALUE)
 *   - USE: many short-lived tasks; NOT for sustained high load
 *
 * ScheduledThreadPool(n):
 *   - n core threads, DelayedWorkQueue (priority queue by execution time)
 *   - schedule(task, delay, unit): run once after delay
 *   - scheduleAtFixedRate: repeat every period (timer replacement)
 *   - scheduleWithFixedDelay: wait `delay` AFTER last execution finishes
 *   - USE: cron jobs, heartbeats, timeouts
 *
 * ForkJoinPool (Java 7+):
 *   - Work-stealing: idle threads steal tasks from busy threads' deques
 *   - Designed for recursive divide-and-conquer (fork = split, join = combine)
 *   - Powers: Arrays.parallelSort(), Stream.parallel(), CompletableFuture.supplyAsync()
 *   - commonPool() has (num_CPUs - 1) threads
 *   - USE: parallel recursive algorithms, parallel streams
 *
 * Virtual Thread Pool (Java 21+):
 *   - Executors.newVirtualThreadPerTaskExecutor()
 *   - Each task gets a new virtual thread (cheap: ~1KB heap, not ~512KB stack)
 *   - JVM scheduler maps many virtual threads → few OS threads (M:N threading)
 *   - When virtual thread blocks on I/O, it yields OS thread (non-blocking!)
 *   - USE: high-concurrency I/O tasks (web servers, DB queries, HTTP clients)
 *   - NOT for CPU-bound (too many VTs still run on limited OS threads)
 *
 * REJECTION POLICIES (when queue is full):
 *   - AbortPolicy (default): throw RejectedExecutionException
 *   - CallerRunsPolicy: calling thread executes the task (back-pressure mechanism)
 *   - DiscardPolicy: silently drop the task
 *   - DiscardOldestPolicy: remove oldest queued task, retry submission
 *
 * TIME COMPLEXITY:
 *   - submit: O(1) — just queue append
 *   - Worker execution: O(1) overhead — task.run() is O(task_complexity)
 *
 * SPACE COMPLEXITY:
 *   - O(numThreads + queueCapacity) for thread stacks + queued runnables
 *
 * INTERVIEW ANGLE:
 *   "Design a thread pool" is a common system design question testing:
 *   - Producer-consumer pattern (submit vs workers)
 *   - Thread lifecycle management (start, shutdown, drain)
 *   - Back-pressure and rejection policies
 *   - Monitoring (active/completed counts)
 *
 * REAL-WORLD USE:
 *   - Tomcat/Jetty: HTTP request handling thread pools
 *   - Spring @Async: TaskExecutor backed by thread pool
 *   - Kafka consumer thread pools
 * =============================================================================
 */
public class DesignThreadPool {

    // =========================================================================
    // CUSTOM THREAD POOL IMPLEMENTATION
    // =========================================================================

    /**
     * SimpleThreadPool — educational implementation of a fixed-size thread pool.
     *
     * KEY INSIGHT: The producer-consumer pattern is the foundation:
     *   - Producers: threads calling submit()
     *   - Queue: bounded BlockingQueue provides back-pressure
     *   - Consumers: Worker threads that loop taking from the queue
     *
     * LIFECYCLE: RUNNING → SHUTDOWN → TERMINATED
     *   RUNNING: accept new tasks, workers running
     *   SHUTDOWN: no new tasks, workers drain remaining queue, then exit
     *   TERMINATED: all workers exited
     *
     * THREAD SAFETY:
     *   - BlockingQueue handles concurrent producers and consumers safely
     *   - volatile shutdown flag ensures workers see the shutdown signal promptly
     *   - AtomicInteger/Long for counters — no synchronized block needed
     */
    public static class SimpleThreadPool {
        private final BlockingQueue<Runnable> taskQueue;
        private final List<WorkerThread> workers;
        private volatile boolean shutdown = false;  // volatile: visibility across threads

        // Metrics — use Atomic for lock-free, thread-safe increments
        private final AtomicLong submittedCount = new AtomicLong(0);
        private final AtomicLong completedCount = new AtomicLong(0);
        private final AtomicInteger activeWorkers = new AtomicInteger(0);

        /**
         * @param numThreads    Number of worker threads (fixed)
         * @param queueCapacity Max pending tasks before rejection
         *
         * SIZING GUIDELINES:
         *   CPU-bound tasks: numThreads = num_CPUs (or num_CPUs + 1)
         *   I/O-bound tasks: numThreads = num_CPUs × (1 + wait_time/service_time)
         *   Mixed: profile first, then tune
         *
         * GOTCHA: Don't set numThreads too high for CPU-bound work.
         * More threads = more context switches = slower, not faster.
         */
        public SimpleThreadPool(int numThreads, int queueCapacity) {
            // ArrayBlockingQueue = bounded = prevents OOM under high load
            // INTERVIEW: Why not LinkedBlockingQueue? → Unbounded → OOM risk
            this.taskQueue = new ArrayBlockingQueue<>(queueCapacity);
            this.workers = new ArrayList<>(numThreads);

            // Pre-create and start all worker threads
            for (int i = 0; i < numThreads; i++) {
                WorkerThread worker = new WorkerThread("pool-worker-" + i);
                workers.add(worker);
                worker.start();  // workers immediately start waiting for tasks
            }
            System.out.printf("ThreadPool started: %d workers, capacity=%d%n",
                    numThreads, queueCapacity);
        }

        /**
         * Submit a task for execution.
         *
         * KEY INSIGHT: offer() is non-blocking (unlike put() which would block
         * when queue is full). We want to fail fast with a rejection rather than
         * block the calling thread.
         *
         * COMPLEXITY: O(1)
         *
         * @throws RejectedExecutionException if pool is shut down or queue is full
         *
         * INTERVIEW Q: "What is CallerRunsPolicy and when is it useful?"
         * A: When queue is full, the CALLING thread runs the task directly.
         *    This slows down the producer, providing natural back-pressure.
         *    Prevents OOM and keeps work progressing. Risk: blocks HTTP handler threads.
         */
        public void submit(Runnable task) {
            if (shutdown) {
                throw new RejectedExecutionException("Pool is shut down, rejecting task");
            }
            // offer() = non-blocking; returns false if queue is full
            boolean added = taskQueue.offer(task);
            if (!added) {
                throw new RejectedExecutionException(
                    "Task queue full (capacity=" + taskQueue.remainingCapacity() + "), rejecting task");
            }
            submittedCount.incrementAndGet();
        }

        /**
         * Initiate graceful shutdown: stop accepting new tasks, let workers
         * drain the remaining queue, then exit.
         *
         * KEY INSIGHT: Setting shutdown=true prevents new submissions.
         * Workers check both !shutdown and queue.isEmpty() before exiting.
         * Workers are interrupted to wake them if they're blocked in queue.take().
         *
         * GOTCHA: There's a race — between shutdown=true and worker interrupt,
         * a task might be submitted. That's why submit() also checks shutdown.
         */
        public void shutdown() {
            System.out.println("Pool shutting down...");
            shutdown = true;
            // Interrupt all workers to wake them from queue.take() blocking
            for (WorkerThread worker : workers) {
                worker.interrupt();
            }
        }

        /**
         * Wait for all workers to finish execution after shutdown().
         *
         * @param timeout Max time to wait
         * @param unit    Time unit
         * @return true if all workers finished, false if timeout elapsed
         *
         * COMPLEXITY: O(numThreads) — joins each worker
         */
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long deadlineMs = System.currentTimeMillis() + unit.toMillis(timeout);
            for (WorkerThread worker : workers) {
                long remaining = deadlineMs - System.currentTimeMillis();
                if (remaining <= 0) return false;
                worker.join(remaining);  // wait at most `remaining` ms for this worker
                if (worker.isAlive()) return false;
            }
            return true;
        }

        public long getSubmittedCount()  { return submittedCount.get(); }
        public long getCompletedCount()  { return completedCount.get(); }
        public int  getActiveWorkers()   { return activeWorkers.get(); }
        public int  getQueueSize()       { return taskQueue.size(); }

        // =====================================================================
        // WORKER THREAD — The core of the pool
        // =====================================================================

        /**
         * A worker thread: continuously takes tasks from the queue and runs them.
         *
         * KEY INSIGHT: The loop uses queue.poll(timeout) rather than queue.take()
         * to periodically check the shutdown flag. Otherwise, a thread blocked in
         * take() would never notice the shutdown signal (unless interrupted).
         *
         * ALTERNATIVE: Use take() + Thread.interrupt() in shutdown().
         * Both are valid; the poll(timeout) approach is slightly more explicit.
         */
        private class WorkerThread extends Thread {
            WorkerThread(String name) {
                super(name);
                setDaemon(false);  // Non-daemon: JVM won't exit while workers run
            }

            @Override
            public void run() {
                System.out.println(getName() + " started");
                while (!shutdown || !taskQueue.isEmpty()) {
                    try {
                        // Poll with timeout — don't block forever so we can check shutdown
                        // GOTCHA: If we used take() here, the thread blocks until interrupted.
                        // poll(100ms) means we re-check shutdown flag every 100ms at worst.
                        Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            activeWorkers.incrementAndGet();
                            try {
                                task.run();
                                completedCount.incrementAndGet();
                            } catch (Exception e) {
                                // IMPORTANT: Catch task exceptions to keep worker alive!
                                // If we let exceptions propagate, the thread dies and the
                                // pool shrinks. This is a common bug in naive implementations.
                                System.err.println(getName() + " task threw exception: " + e.getMessage());
                            } finally {
                                activeWorkers.decrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        // Shutdown interrupted our poll() — re-check while condition
                        // GOTCHA: Don't swallow InterruptedException without re-interrupting
                        Thread.currentThread().interrupt();  // restore interrupt status
                        break;
                    }
                }
                System.out.println(getName() + " terminated");
            }
        }
    }

    // =========================================================================
    // COMPARISON: BUILT-IN THREAD POOLS DEMO
    // =========================================================================

    /**
     * Demonstrates Java's built-in thread pool variants side by side.
     * Each serves a different use case — choosing wrong causes performance issues.
     */
    private static void demonstrateBuiltinPools() throws Exception {
        System.out.println("\n=== Built-in Thread Pool Comparison ===\n");

        // 1. FixedThreadPool: good for known workloads, UNBOUNDED queue (watch out!)
        // GOTCHA: If producers are faster than consumers, queue grows without limit → OOM
        System.out.println("--- FixedThreadPool(2) ---");
        ExecutorService fixed = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 4; i++) {
            final int id = i;
            fixed.submit(() -> {
                System.out.printf("  FixedPool task %d on %s%n", id, Thread.currentThread().getName());
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        fixed.shutdown();
        fixed.awaitTermination(5, TimeUnit.SECONDS);

        // 2. CachedThreadPool: creates threads on demand, reuses idle threads, max = Integer.MAX_VALUE
        // DANGER: Under high load, creates too many threads → OutOfMemoryError
        // GOOD for: many short-lived, bursty tasks (e.g., user events, fire-and-forget)
        System.out.println("\n--- CachedThreadPool ---");
        ExecutorService cached = Executors.newCachedThreadPool();
        for (int i = 0; i < 4; i++) {
            final int id = i;
            cached.submit(() -> System.out.printf("  CachedPool task %d on %s%n",
                    id, Thread.currentThread().getName()));
        }
        cached.shutdown();
        cached.awaitTermination(5, TimeUnit.SECONDS);

        // 3. ScheduledThreadPool: delayed and periodic execution
        System.out.println("\n--- ScheduledThreadPool(2) ---");
        ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
        // Run once after 100ms delay
        scheduled.schedule(
            () -> System.out.println("  Scheduled: ran after delay"),
            100, TimeUnit.MILLISECONDS
        );
        // Run every 200ms (fixed rate — doesn't account for execution time)
        // GOTCHA: scheduleAtFixedRate starts next run AT period boundary even if task runs long
        // scheduleWithFixedDelay: waits `delay` AFTER task completes (safer for long tasks)
        ScheduledFuture<?> periodic = scheduled.scheduleAtFixedRate(
            () -> System.out.println("  Periodic task on " + Thread.currentThread().getName()),
            0, 200, TimeUnit.MILLISECONDS
        );
        Thread.sleep(500);
        periodic.cancel(false);
        scheduled.shutdown();
        scheduled.awaitTermination(1, TimeUnit.SECONDS);

        // 4. Virtual Thread Executor (Java 21) — for I/O-bound high-concurrency
        // Each task gets a new virtual thread (lightweight, ~1KB vs ~512KB for OS thread)
        // Blocking I/O on virtual thread: yields the OS thread (non-blocking under the hood!)
        // PERFECT for: 10k+ concurrent HTTP requests, DB queries, file I/O
        // NOT for: CPU-bound (VTs don't help — still limited by CPU cores)
        System.out.println("\n--- Virtual Thread Executor (Java 21) ---");
        try (ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                final int id = i;
                futures.add(vtExecutor.submit(() -> {
                    // Simulating blocking I/O — on virtual thread, this yields the OS thread
                    try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    System.out.printf("  VirtualThread task %d on thread: %s (virtual=%b)%n",
                            id,
                            Thread.currentThread().getName(),
                            Thread.currentThread().isVirtual());
                }));
            }
            for (Future<?> f : futures) f.get();
        }
        // try-with-resources: calls shutdown() + awaitTermination() automatically (Java 21)
    }

    // =========================================================================
    // MAIN — Demo of custom pool + built-in comparison
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== Custom Thread Pool Demo ===\n");

        // --- Test SimpleThreadPool ---
        SimpleThreadPool pool = new SimpleThreadPool(3, 10);

        // Submit tasks
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            pool.submit(() -> {
                try {
                    Thread.sleep(50);  // simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
                System.out.printf("  Task %d completed on %s%n",
                        taskId, Thread.currentThread().getName());
            });
        }

        // Check metrics while running
        Thread.sleep(30);
        System.out.printf("Metrics: submitted=%d, completed=%d, active=%d, queued=%d%n",
                pool.getSubmittedCount(), pool.getCompletedCount(),
                pool.getActiveWorkers(), pool.getQueueSize());

        // Test rejection
        System.out.println("\n--- Test rejection (fill queue) ---");
        SimpleThreadPool tinyPool = new SimpleThreadPool(1, 2);
        for (int i = 0; i < 5; i++) {
            try {
                final int id = i;
                tinyPool.submit(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                System.out.println("Submitted task " + i);
            } catch (RejectedExecutionException e) {
                System.out.println("REJECTED task " + i + ": " + e.getMessage());
            }
        }

        // Graceful shutdown
        System.out.println("\n--- Graceful shutdown ---");
        pool.shutdown();
        boolean terminated = pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.printf("Pool terminated: %s, completed: %d/8%n",
                terminated, pool.getCompletedCount());

        tinyPool.shutdown();
        tinyPool.awaitTermination(2, TimeUnit.SECONDS);

        // Compare built-in pools
        demonstrateBuiltinPools();

        System.out.println("\n=== Thread Pool Summary ===");
        System.out.println("FixedThreadPool:    n workers, unbounded queue — safe workers, OOM risk on queue");
        System.out.println("CachedThreadPool:   elastic workers, SynchronousQueue — thread explosion risk");
        System.out.println("ScheduledPool:      delayed/periodic — cron jobs, heartbeats");
        System.out.println("ForkJoinPool:       work-stealing — parallel streams, recursive tasks");
        System.out.println("VirtualThreadPool:  Java 21, lightweight — I/O-bound high-concurrency");

        /*
         * INTERVIEW Q: "How does ThreadPoolExecutor decide to create new threads?"
         * A: 1. If activeThreads < corePoolSize → create new thread
         *    2. If activeThreads >= corePoolSize → add to queue
         *    3. If queue is full AND activeThreads < maxPoolSize → create new thread
         *    4. If queue full AND at max → apply rejection policy
         *    This is the exact logic in ThreadPoolExecutor.execute()
         *
         * INTERVIEW Q: "Why does FixedThreadPool risk OOM but CachedThreadPool risks thread explosion?"
         * A: Fixed: queue grows unboundedly when all threads are busy.
         *    Cached: new thread per task when all are busy, up to Integer.MAX_VALUE.
         *    ThreadPoolExecutor with bounded queue + bounded max threads is safer.
         */
    }
}
