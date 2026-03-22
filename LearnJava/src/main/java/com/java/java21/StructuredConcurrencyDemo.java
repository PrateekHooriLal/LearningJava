// Run with: --enable-preview --source 21
package com.java.java21;

/**
 * CONCEPT: Structured Concurrency (Java 21 Preview — JEP 453)
 *
 * CORE IDEA: Treat a group of related tasks as a single unit of work.
 * If the parent scope closes, ALL child tasks are cancelled. A task can never
 * outlive the scope that created it — this is the "Nursery Pattern" from
 * structured programming applied to concurrency.
 *
 * WHEN TO USE:
 *   - Fan-out pattern: call multiple services in parallel, need ALL results
 *   - Race pattern: call multiple sources, take the fastest response
 *   - Any parallel work where failure of one task should cancel the rest
 *
 * TIME/SPACE: N/A (concurrency primitive, not algorithm)
 *
 * INTERVIEW ANGLE:
 *   Q: What problem does structured concurrency solve?
 *   A: "Fire-and-forget" resource leaks. With CompletableFuture, if one
 *      async task fails, other tasks keep running and consuming resources.
 *      Structured concurrency guarantees cleanup by tying task lifetimes
 *      to a lexical scope — like try-with-resources for threads.
 *
 *   Q: How is it different from CompletableFuture.allOf()?
 *   A: CF.allOf() does NOT cancel other futures if one fails. You get a
 *      failed CompletableFuture but all other tasks keep running silently.
 *      StructuredTaskScope.ShutdownOnFailure cancels ALL on first failure.
 *
 *   Q: Why do virtual threads make structured concurrency practical?
 *   A: With platform threads, forking 1000 tasks is expensive. Virtual
 *      threads are cheap (<<1MB each), so you can fork one virtual thread
 *      per task without a thread pool — just like goroutines in Go.
 *
 * REAL-WORLD USE:
 *   - Microservice fan-out: fetch user profile + order history + inventory
 *     simultaneously; fail fast if any service is down
 *   - A/B testing: send same request to two models, use the first response
 *   - Hedged requests (latency tail mitigation): send same request to two
 *     servers, cancel the slower one when the first responds
 *
 * GOTCHA:
 *   - This is a PREVIEW feature. Requires --enable-preview at compile AND runtime.
 *   - StructuredTaskScope is AutoCloseable — ALWAYS use try-with-resources.
 *   - scope.join() must be called before scope.throwIfFailed() or accessing results.
 *   - Fork BEFORE join. You cannot fork after join() returns.
 */

import java.util.concurrent.*;
import java.util.*;

public class StructuredConcurrencyDemo {

    // -------------------------------------------------------------------------
    // SECTION 1: ShutdownOnFailure — "all must succeed" pattern
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: ShutdownOnFailure is the fan-out pattern.
     * Use when you need ALL results and any single failure should abort everything.
     *
     * REAL-WORLD: fetchUserData + fetchOrders + fetchInventory for a dashboard.
     * If the orders service is down, there's no point waiting for inventory.
     *
     * FLOW:
     *   1. Open scope (try-with-resources)
     *   2. fork() each task → returns a Subtask (Future-like handle)
     *   3. scope.join() → blocks until ALL tasks finish (or scope shuts down)
     *   4. scope.throwIfFailed() → rethrows if any subtask threw
     *   5. subtask.get() → safe to call only AFTER join() + throwIfFailed()
     *
     * @param userId the user ID to fetch data for
     * @return Map of results from all three services
     */
    public static Map<String, String> fetchAllUserData(int userId) throws Exception {
        // NOTE: StructuredTaskScope requires --enable-preview in Java 21.
        // The code below is written as if it compiles; wrap in reflection or
        // conditional compilation for production use without preview flags.

        /*
         * STRUCTURED CONCURRENCY PATTERN (requires preview):
         *
         * try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *
         *     // fork() submits each task to a virtual thread.
         *     // It returns a StructuredTaskScope.Subtask<T> (similar to Future<T>)
         *     // KEY: fork() returns IMMEDIATELY — tasks run concurrently
         *     StructuredTaskScope.Subtask<String> userTask =
         *         scope.fork(() -> fetchUserProfile(userId));
         *
         *     StructuredTaskScope.Subtask<String> ordersTask =
         *         scope.fork(() -> fetchOrders(userId));
         *
         *     StructuredTaskScope.Subtask<String> inventoryTask =
         *         scope.fork(() -> fetchInventory(userId));
         *
         *     // join() blocks until ALL subtasks complete OR scope shuts down.
         *     // If any subtask throws, ShutdownOnFailure cancels remaining tasks.
         *     // CRITICAL: You MUST call join() before accessing subtask results.
         *     scope.join();
         *
         *     // throwIfFailed() rethrows the first exception from any subtask,
         *     // wrapped in ExecutionException. Call AFTER join().
         *     scope.throwIfFailed();
         *
         *     // At this point ALL subtasks succeeded.
         *     // subtask.get() is safe ONLY after join() + throwIfFailed().
         *     return Map.of(
         *         "user",      userTask.get(),
         *         "orders",    ordersTask.get(),
         *         "inventory", inventoryTask.get()
         *     );
         * }
         * // RESOURCE GUARANTEE: The try-with-resources block calls scope.close(),
         * // which interrupts any still-running tasks. This is the "nursery" guarantee:
         * // no child task can outlive this scope.
         */

        // Simulation without preview — demonstrates the SAME logic using CF:
        // This illustrates WHY structured concurrency is better:
        var cf1 = CompletableFuture.supplyAsync(() -> fetchUserProfile(userId));
        var cf2 = CompletableFuture.supplyAsync(() -> fetchOrders(userId));
        var cf3 = CompletableFuture.supplyAsync(() -> fetchInventory(userId));

        // PROBLEM with CF.allOf: if cf2 fails, cf1 and cf3 keep running!
        // No automatic cancellation. This wastes resources.
        CompletableFuture.allOf(cf1, cf2, cf3).join();

        return Map.of(
            "user",      cf1.get(),
            "orders",    cf2.get(),
            "inventory", cf3.get()
        );
    }

    // -------------------------------------------------------------------------
    // SECTION 2: ShutdownOnSuccess — "fastest wins" / race pattern
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: ShutdownOnSuccess is the hedged request / race pattern.
     * Use when you want the result from whichever service responds first.
     *
     * REAL-WORLD:
     *   - Send same DB query to primary and replica; use first response
     *   - Try two CDN endpoints; use whichever responds first (latency hedging)
     *   - Ask multiple ML models; use first inference result
     *
     * FLOW (preview code — same structure, different scope type):
     *   try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
     *       scope.fork(() -> queryPrimaryDB(query));
     *       scope.fork(() -> queryReplicaDB(query));
     *       scope.join(); // waits until FIRST success OR all fail
     *       return scope.result(); // returns first successful result
     *       // RESOURCE: scope.close() cancels the still-running loser task!
     *   }
     *
     * GOTCHA: scope.result() throws NoSuchElementException if ALL tasks failed.
     * GOTCHA: scope.result() throws ExecutionException if you call it and all failed.
     */
    public static String raceRequests(String query) throws Exception {
        /*
         * try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
         *     scope.fork(() -> queryPrimaryDB(query));
         *     scope.fork(() -> queryReplicaDB(query));
         *     scope.join();
         *     // scope.result() blocks until first success.
         *     // The OTHER task is cancelled automatically when scope closes.
         *     return scope.result();
         * }
         */
        // Simulation:
        return CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> queryPrimaryDB(query)),
            CompletableFuture.supplyAsync(() -> queryReplicaDB(query))
        ).thenApply(Object::toString).join();
        // NOTE: anyOf does NOT cancel the loser — another reason SC is better!
    }

    // -------------------------------------------------------------------------
    // SECTION 3: Nursery Pattern — tasks cannot outlive their scope
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: The "Nursery Pattern" is a metaphor from Nathaniel J. Smith's
     * blog post "Notes on structured concurrency". A nursery holds all child tasks;
     * you can't leave the nursery until all children are done (or cleaned up).
     *
     * This solves the "fire-and-forget" anti-pattern where background tasks
     * continue running after the caller method returns, potentially causing:
     *   - Resource leaks (DB connections held)
     *   - State corruption (writes after transaction committed)
     *   - Silent failures (exceptions swallowed in background threads)
     *
     * COMPARISON TABLE:
     * ┌──────────────────────────┬──────────────────────┬────────────────────────┐
     * │ Feature                  │ CompletableFuture    │ StructuredTaskScope    │
     * ├──────────────────────────┼──────────────────────┼────────────────────────┤
     * │ Cancel others on failure │ No (manual)          │ Yes (automatic)        │
     * │ Task lifetime bounded    │ No (fire-and-forget) │ Yes (scope = lifetime) │
     * │ Exception propagation    │ Wrapped in ExecExc.  │ throwIfFailed()        │
     * │ Virtual thread friendly  │ Partial              │ Designed for it        │
     * │ Deadlock detection       │ No                   │ Thread dump shows tree │
     * └──────────────────────────┴──────────────────────┴────────────────────────┘
     */
    public static void demonstrateNurseryPattern() {
        System.out.println("=== Nursery Pattern Demo ===");
        System.out.println("Structured Concurrency ensures:");
        System.out.println("1. All forked tasks complete before scope exits");
        System.out.println("2. Any task failure triggers cancellation of siblings");
        System.out.println("3. Exception from child propagates to parent scope");
        System.out.println("4. No task can outlive its enclosing scope");

        /*
         * STRUCTURED CODE (preview):
         *
         * void processRequest(Request req) throws Exception {
         *     try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
         *         var authTask     = scope.fork(() -> authenticate(req));
         *         var rateTask     = scope.fork(() -> checkRateLimit(req));
         *         var validTask    = scope.fork(() -> validatePayload(req));
         *
         *         scope.join().throwIfFailed(); // wait all, fail fast
         *
         *         // All three checks passed. Safe to proceed.
         *         processValidRequest(authTask.get(), rateTask.get(), validTask.get());
         *     }
         *     // GUARANTEE: by this closing brace, ALL three tasks have finished
         *     // (either successfully or cancelled). No dangling threads.
         * }
         */
    }

    // -------------------------------------------------------------------------
    // SECTION 4: Virtual Threads + Structured Concurrency = Go-style concurrency
    // -------------------------------------------------------------------------

    /**
     * KEY INSIGHT: Virtual threads (JEP 444, stable in Java 21) are the ENGINE;
     * structured concurrency is the STEERING WHEEL.
     *
     * Virtual threads:
     *   - Are NOT OS threads. They're scheduled by the JVM on a small pool of
     *     carrier (platform) threads.
     *   - When a virtual thread blocks (I/O, sleep, lock), the carrier thread
     *     is released to run another virtual thread.
     *   - Cost: ~few hundred bytes per virtual thread (vs ~1MB for platform thread)
     *   - You can have MILLIONS of virtual threads simultaneously.
     *
     * Together: fork one virtual thread per task, no thread pool needed.
     * This is semantically similar to Go's goroutines + WaitGroup.
     *
     * GO vs JAVA COMPARISON:
     *   Go:                              Java 21:
     *   var wg sync.WaitGroup            try (var scope = new SC.ShutdownOnFailure()) {
     *   wg.Add(3)                            scope.fork(() -> task1());
     *   go func() { task1(); wg.Done() }()   scope.fork(() -> task2());
     *   go func() { task2(); wg.Done() }()   scope.fork(() -> task3());
     *   go func() { task3(); wg.Done() }()   scope.join().throwIfFailed();
     *   wg.Wait()                        }
     *
     * INTERVIEW TALKING POINT:
     *   Java 21 is the convergence of two decades of concurrency evolution:
     *   Thread → ExecutorService → CompletableFuture → Virtual Threads + SC
     *   Each step addressed the previous generation's pain points.
     */
    public static void demonstrateVirtualThreads() throws Exception {
        System.out.println("\n=== Virtual Threads Demo ===");

        // Create 10,000 virtual threads — trivial with virtual threads
        // With platform threads, this would exhaust OS thread limit
        var tasks = new ArrayList<Thread>();
        for (int i = 0; i < 10_000; i++) {
            final int id = i;
            Thread vt = Thread.ofVirtual().start(() -> {
                // Simulate I/O — virtual thread yields carrier thread during sleep
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Carrier thread reused while this virtual thread "slept"
            });
            tasks.add(vt);
        }

        // Join all virtual threads
        for (Thread t : tasks) {
            t.join();
        }
        System.out.println("Completed 10,000 virtual threads successfully");

        // KEY METRIC: Platform threads max out around 10,000-20,000 on typical machines.
        // Virtual threads can scale to millions. Same blocking I/O code, massively scalable.
    }

    // -------------------------------------------------------------------------
    // HELPER METHODS (simulate network calls)
    // -------------------------------------------------------------------------

    private static String fetchUserProfile(int userId) {
        simulateIO(50); // simulate 50ms network call
        return "User{id=" + userId + ", name='Alice'}";
    }

    private static String fetchOrders(int userId) {
        simulateIO(80); // simulate 80ms network call
        return "Orders{userId=" + userId + ", count=5}";
    }

    private static String fetchInventory(int userId) {
        simulateIO(60); // simulate 60ms network call
        return "Inventory{items=100, reserved=5}";
    }

    private static String queryPrimaryDB(String query) {
        simulateIO(30);
        return "PrimaryResult{query=" + query + "}";
    }

    private static String queryReplicaDB(String query) {
        simulateIO(20); // replica faster in this scenario
        return "ReplicaResult{query=" + query + "}";
    }

    private static void simulateIO(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // MAIN — Demo all patterns
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("=== Structured Concurrency Demo ===");
        System.out.println("(Note: StructuredTaskScope API requires --enable-preview)");
        System.out.println();

        // Demo 1: Fan-out with ShutdownOnFailure
        System.out.println("--- Demo 1: Fan-out (ShutdownOnFailure) ---");
        long start = System.currentTimeMillis();
        Map<String, String> results = fetchAllUserData(42);
        long elapsed = System.currentTimeMillis() - start;
        results.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        // INTERVIEW POINT: Time should be ~80ms (max of 50,80,60), not 190ms (sum)
        // because tasks run concurrently. This is the fan-out performance benefit.
        System.out.println("  Elapsed: " + elapsed + "ms (should be ~80ms, not 190ms sum)");

        // Demo 2: Race / hedged request
        System.out.println("\n--- Demo 2: Race (ShutdownOnSuccess) ---");
        start = System.currentTimeMillis();
        String winner = raceRequests("SELECT * FROM users");
        elapsed = System.currentTimeMillis() - start;
        System.out.println("  Winner: " + winner);
        System.out.println("  Elapsed: " + elapsed + "ms (should be ~20ms, the faster replica)");

        // Demo 3: Nursery pattern explanation
        demonstrateNurseryPattern();

        // Demo 4: Virtual threads at scale
        demonstrateVirtualThreads();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("ShutdownOnFailure: use when ALL tasks must succeed (fan-out)");
        System.out.println("ShutdownOnSuccess: use when FIRST success wins (hedging/racing)");
        System.out.println("Nursery guarantee: no child task outlives its scope");
        System.out.println("Virtual threads: cheap enough to fork one per task, no pool needed");
        System.out.println("Key win over CF: automatic cancellation + guaranteed cleanup");
    }
}
