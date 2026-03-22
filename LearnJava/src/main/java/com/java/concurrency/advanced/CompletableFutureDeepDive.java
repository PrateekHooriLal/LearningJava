package com.java.concurrency.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * =============================================================================
 * CONCEPT: CompletableFuture Deep Dive — Asynchronous Pipelines in Java
 * =============================================================================
 *
 * CompletableFuture (CF) is Java's answer to non-blocking async programming.
 * Introduced in Java 8, enhanced in Java 9 (orTimeout, completeOnTimeout).
 *
 * WHY CF OVER FUTURE?
 * ┌────────────────────────────────────────────────────────────────┐
 * │ Future (Java 5)              │ CompletableFuture (Java 8+)    │
 * ├────────────────────────────────────────────────────────────────┤
 * │ .get() blocks calling thread │ .thenApply() non-blocking chain│
 * │ No composition               │ thenCombine, allOf, anyOf      │
 * │ No exception handling        │ exceptionally, handle          │
 * │ No timeout on completion     │ orTimeout, completeOnTimeout   │
 * │ Cannot complete manually     │ complete(value), completeExcep.│
 * └────────────────────────────────────────────────────────────────┘
 *
 * THREAD EXECUTION MODEL:
 *   - supplyAsync() → runs in ForkJoinPool.commonPool() (default)
 *   - thenApply() → runs in same thread as the completing stage (or commonPool)
 *   - thenApplyAsync() → always dispatches to a new thread (or provided executor)
 *   - For I/O-bound tasks: provide custom executor (or use virtual threads in Java 21)
 *   - ForkJoinPool.commonPool(): parallelism = Runtime.getRuntime().availableProcessors() - 1
 *     GOTCHA: If all commonPool threads are blocked on I/O, the pool starves.
 *     SOLUTION: Provide a dedicated IO executor for I/O-bound CF stages.
 *
 * KEY MENTAL MODEL:
 *   CF is like a pipeline of transformations.
 *   thenApply = map (sync transform, same thread)
 *   thenCompose = flatMap (async chain, avoids CF<CF<T>>)
 *   thenCombine = zip (two independent CFs, combine results)
 *   allOf = fan-in (wait for all)
 *   anyOf = race (first to finish wins)
 *
 * CF vs VIRTUAL THREADS (Java 21):
 * ┌────────────────────────────────────────────────────────────────┐
 * │ CompletableFuture              │ Virtual Threads               │
 * ├────────────────────────────────────────────────────────────────┤
 * │ Callback/pipeline style        │ Sequential blocking style     │
 * │ Complex fan-out/fan-in         │ Simple one-request-per-task   │
 * │ Non-blocking I/O               │ Blocking I/O (yields VT)      │
 * │ Good with Reactor/RxJava       │ Good for simpler code         │
 * │ Hard to debug stack traces     │ Normal stack traces           │
 * └────────────────────────────────────────────────────────────────┘
 * Rule of thumb: VT for simple sequential blocking; CF for complex async pipelines.
 *
 * TIME COMPLEXITY: O(1) per chaining operation; total = O(sum of all stages)
 * SPACE COMPLEXITY: O(depth of pipeline) — each stage holds the previous result
 *
 * INTERVIEW ANGLE:
 *   "How would you fetch data from 3 independent APIs in parallel and combine?"
 *   → CompletableFuture.allOf() + individual .join()
 *   "What happens when a stage throws an exception?"
 *   → CF completes exceptionally; propagates unless caught by exceptionally/handle
 *
 * REAL-WORLD USE:
 *   - Spring WebFlux (reactive HTTP clients)
 *   - Aggregator services (combine user profile + orders + recommendations)
 *   - Async DB queries (with async JDBC drivers)
 *   - Event-driven microservices
 * =============================================================================
 */
public class CompletableFutureDeepDive {

    // =========================================================================
    // SIMULATED EXTERNAL SERVICES (with artificial latency)
    // =========================================================================

    /** Simulates a slow user profile API call (100ms). */
    private static String fetchUserProfile(String userId) {
        sleep(100);
        if ("error-user".equals(userId)) throw new RuntimeException("User not found: " + userId);
        return "UserProfile{id=" + userId + ", name=Alice, tier=PREMIUM}";
    }

    /** Simulates a slow orders API call (150ms). */
    private static List<String> fetchOrders(String userId) {
        sleep(150);
        return List.of("Order#1001", "Order#1002", "Order#1003");
    }

    /** Simulates fetching recommendations based on a user profile (80ms). */
    private static List<String> fetchRecommendations(String profile) {
        sleep(80);
        return List.of("Product#A", "Product#B", "Product#C");
    }

    /** Simulates fetching detailed account info from profile (50ms). */
    private static String fetchAccountDetails(String profile) {
        sleep(50);
        return "Account{tier=PREMIUM, credits=500}";
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // =========================================================================
    // DEMO METHODS
    // =========================================================================

    /**
     * DEMO 1: Basic async, thenApply vs thenCompose.
     *
     * thenApply: synchronous transform — wraps the result in a new CF.
     *   supplyAsync(() → "hello").thenApply(s → s.toUpperCase()) → CF<"HELLO">
     *   Thread: transformation runs in same thread that completed the previous stage.
     *
     * thenCompose: asynchronous chain (flatMap equivalent).
     *   Use when the next step itself returns a CF (to avoid CF<CF<T>>).
     *   supplyAsync(() → fetchProfile(id)).thenCompose(profile → supplyAsync(() → fetchAccount(profile)))
     *   Without thenCompose (using thenApply): CF<CF<String>> — nested, unusable.
     *
     * GOTCHA: thenApply(x → supplyAsync(...)) gives CF<CF<String>>.
     *         thenCompose(x → supplyAsync(...)) gives CF<String>. Always prefer this.
     *
     * COMPLEXITY: O(sum of all stage latencies) sequential; parallel variants are faster.
     */
    private static void demo1BasicAsync() throws Exception {
        System.out.println("\n=== DEMO 1: thenApply vs thenCompose ===");

        long start = System.currentTimeMillis();

        // --- thenApply: sync transform ---
        CompletableFuture<String> profileFuture = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U1"))    // async: ~100ms
                .thenApply(profile -> profile.toUpperCase()); // sync transform: immediate

        // --- thenCompose: async chain (avoids CF<CF<String>>) ---
        CompletableFuture<String> accountFuture = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U1"))         // async: ~100ms
                .thenCompose(profile ->                            // profile is the input
                    CompletableFuture.supplyAsync(() -> fetchAccountDetails(profile))  // async: ~50ms
                );
        // Total: ~150ms (sequential async stages)
        // KEY: thenCompose flattens CF<CF<String>> → CF<String>

        // --- thenApplyAsync: transform on a different thread ---
        // Use this when the transform is expensive and you don't want to block
        // the thread that completed the previous stage.
        CompletableFuture<String> asyncTransform = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U1"))
                .thenApplyAsync(profile -> {
                    // This runs on a different ForkJoinPool thread, not the one that fetched profile
                    return "Processed: " + profile;
                });

        System.out.println("Profile (upper): " + profileFuture.get());
        System.out.println("Account: " + accountFuture.get());
        System.out.println("Async transform: " + asyncTransform.get());
        System.out.printf("Demo1 time: %dms%n", System.currentTimeMillis() - start);
    }

    /**
     * DEMO 2: Combining futures — thenCombine, allOf, anyOf.
     *
     * thenCombine: combines two INDEPENDENT CFs when BOTH complete.
     *   cf1.thenCombine(cf2, (r1, r2) -> combine(r1, r2))
     *   cf1 and cf2 run CONCURRENTLY; combine runs when both finish.
     *   → Total time = max(cf1, cf2) not cf1 + cf2
     *
     * allOf: wait for ALL CFs. Returns CF<Void> — must .join() each CF for values.
     *   KEY INSIGHT: allOf doesn't collect results automatically.
     *   Pattern: List<CF<T>> futures = ...; allOf(futures.toArray()); then futures.stream().map(CF::join)
     *
     * anyOf: complete when FIRST CF completes. Returns CF<Object>.
     *   Useful for: parallel retries (race N servers, use first response).
     *   GOTCHA: Other CFs keep running after anyOf completes! Not cancelled.
     *
     * COMPLEXITY: thenCombine: O(max(cf1, cf2)); allOf: O(max of all); anyOf: O(min of all)
     */
    private static void demo2Combining() throws Exception {
        System.out.println("\n=== DEMO 2: thenCombine, allOf, anyOf ===");
        long start = System.currentTimeMillis();

        // thenCombine: profile + orders run in parallel (~150ms total, not 250ms)
        CompletableFuture<String> profileCF = CompletableFuture.supplyAsync(() -> fetchUserProfile("U2"));
        CompletableFuture<List<String>> ordersCF = CompletableFuture.supplyAsync(() -> fetchOrders("U2"));

        CompletableFuture<String> combinedCF = profileCF.thenCombine(ordersCF,
                (profile, orders) -> profile + " | orders=" + orders.size());

        System.out.println("Combined: " + combinedCF.get());
        System.out.printf("thenCombine time: %dms (parallel, not sequential)%n",
                System.currentTimeMillis() - start);

        // allOf: 3 independent API calls in parallel
        start = System.currentTimeMillis();
        CompletableFuture<String> pCF = CompletableFuture.supplyAsync(() -> fetchUserProfile("U3"));
        CompletableFuture<List<String>> oCF = CompletableFuture.supplyAsync(() -> fetchOrders("U3"));
        CompletableFuture<List<String>> rCF = CompletableFuture.supplyAsync(
                () -> fetchRecommendations("U3_profile"));

        // allOf returns CF<Void> — wait for all, then get results individually
        CompletableFuture<Void> allDone = CompletableFuture.allOf(pCF, oCF, rCF);
        allDone.get();  // blocks until ALL 3 complete

        String profile = pCF.join();        // .join() is like .get() without checked exception
        List<String> orders = oCF.join();
        List<String> recs = rCF.join();
        System.out.printf("allOf result: profile=%s, orders=%d, recs=%d (time: %dms)%n",
                profile.substring(0, 20), orders.size(), recs.size(),
                System.currentTimeMillis() - start);
        // IMPORTANT: Time should be ~150ms (slowest), not 330ms (sum)

        // anyOf: race condition — first response wins
        start = System.currentTimeMillis();
        CompletableFuture<Object> raceCF = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> { sleep(200); return "Server A (200ms)"; }),
                CompletableFuture.supplyAsync(() -> { sleep(100); return "Server B (100ms)"; }),
                CompletableFuture.supplyAsync(() -> { sleep(300); return "Server C (300ms)"; })
        );
        System.out.printf("anyOf winner: %s (time: %dms)%n",
                raceCF.get(), System.currentTimeMillis() - start);
        // Always "Server B" — fastest wins
    }

    /**
     * DEMO 3: Error handling — exceptionally, handle, whenComplete.
     *
     * exceptionally: catch exception and provide a fallback value.
     *   Like catch block — only invoked on failure, passes through on success.
     *   cf.exceptionally(ex -> defaultValue)
     *
     * handle: handles BOTH success and failure (BiFunction<T, Throwable, U>).
     *   Like try-catch in a pipeline.
     *   if (ex != null) → failure path; else → success path
     *
     * whenComplete: side-effect on completion (don't transform, just observe).
     *   Like finally — always runs, for logging/metrics.
     *   IMPORTANT: doesn't change the pipeline result.
     *
     * EXCEPTION PROPAGATION:
     *   If stage N throws, all subsequent thenApply/thenCompose SKIP.
     *   The exception propagates until caught by exceptionally or handle.
     *   This is the CF equivalent of exception bubbling in try-catch chains.
     *
     * GOTCHA: exceptionally only triggers on failure. handle triggers always.
     *         Use handle for "always execute" cleanup; exceptionally for fallbacks.
     */
    private static void demo3ErrorHandling() throws Exception {
        System.out.println("\n=== DEMO 3: Error Handling ===");

        // exceptionally: fallback on failure
        String result1 = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("error-user"))  // throws!
                .thenApply(p -> p.toUpperCase())                     // SKIPPED — exception propagates
                .exceptionally(ex -> {
                    System.out.println("  exceptionally caught: " + ex.getCause().getMessage());
                    return "DEFAULT_PROFILE";  // fallback value
                })
                .get();
        System.out.println("  Result with fallback: " + result1);

        // handle: both success and failure
        String result2 = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U1"))  // succeeds
                .handle((profile, ex) -> {
                    if (ex != null) {
                        System.out.println("  handle: failure - " + ex.getMessage());
                        return "FALLBACK";
                    }
                    System.out.println("  handle: success - " + profile.substring(0, 20));
                    return profile + "_HANDLED";
                })
                .get();
        System.out.println("  Handle result: " + result2.substring(0, 30));

        // whenComplete: logging side-effect (doesn't transform result)
        String result3 = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U2"))
                .whenComplete((profile, ex) -> {
                    if (ex != null) System.out.println("  LOG: request failed - " + ex);
                    else System.out.println("  LOG: request succeeded, profile length=" + profile.length());
                    // GOTCHA: returning from here has NO EFFECT on the pipeline result
                })
                .get();  // result3 = original profile, unmodified by whenComplete
        System.out.println("  WhenComplete result unchanged: " + (result3 != null));
    }

    /**
     * DEMO 4: Timeouts (Java 9+).
     *
     * orTimeout: if CF doesn't complete within timeout, complete exceptionally
     *   with TimeoutException.
     *   cf.orTimeout(3, SECONDS)
     *
     * completeOnTimeout: if CF doesn't complete within timeout, complete with
     *   the given default value (no exception).
     *   cf.completeOnTimeout("default", 3, SECONDS)
     *
     * GOTCHA: These don't CANCEL the underlying computation!
     *   The original task keeps running even after timeout triggers.
     *   For true cancellation: pass a CancellationToken or use CF.cancel().
     *   cf.cancel(true) completes the CF with CancellationException and
     *   attempts to interrupt the underlying thread (may not succeed if task
     *   isn't checking interrupt status).
     *
     * USE CASE: Aggregate API calls with per-call timeouts.
     *   If recommendations service is slow, use a cached/default response.
     *   This prevents one slow dependency from degrading the entire response.
     */
    private static void demo4Timeouts() throws Exception {
        System.out.println("\n=== DEMO 4: Timeouts (Java 9+) ===");

        // orTimeout: fail with TimeoutException if not complete in 50ms
        // fetchUserProfile takes 100ms → will timeout
        try {
            String result = CompletableFuture
                    .supplyAsync(() -> fetchUserProfile("U1"))  // takes 100ms
                    .orTimeout(50, TimeUnit.MILLISECONDS)        // timeout at 50ms
                    .get();
            System.out.println("Should not reach here: " + result);
        } catch (ExecutionException e) {
            System.out.println("orTimeout: " + e.getCause().getClass().getSimpleName());
            // Expected: TimeoutException
        }

        // completeOnTimeout: use default value instead of exception
        String result = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U1"))         // takes 100ms
                .completeOnTimeout("CACHED_PROFILE", 50, TimeUnit.MILLISECONDS)  // default after 50ms
                .get();
        System.out.println("completeOnTimeout result: " + result);
        // Result: "CACHED_PROFILE" because 100ms > 50ms timeout
    }

    /**
     * DEMO 5: Real-world pattern — parallel API calls with individual timeouts and fallbacks.
     *
     * PATTERN: "Fan-out with fallback"
     *   1. Fire 3 API calls in parallel (profile, orders, recommendations)
     *   2. Each has its own timeout + fallback
     *   3. Wait for all 3 (allOf) — even slow ones get their fallback
     *   4. Combine results
     *
     * KEY INSIGHT: This pattern allows your aggregate API to respond in
     * max(all timeouts) time, with graceful degradation for slow dependencies.
     * A slow recommendations service doesn't ruin the user profile page.
     *
     * This is the core pattern behind Netflix's Hystrix and Resilience4j.
     * KEY INSIGHT: Circuit breaker = CF + fallback + health tracking.
     */
    private static void demo5RealWorldPattern() throws Exception {
        System.out.println("\n=== DEMO 5: Real-World Fan-Out with Timeouts ===");
        long start = System.currentTimeMillis();

        String userId = "U_REAL";

        // Fire 3 independent calls in parallel, each with timeout + fallback
        CompletableFuture<String> profileCF = CompletableFuture
                .supplyAsync(() -> fetchUserProfile(userId))
                .completeOnTimeout("DEFAULT_PROFILE", 200, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> "ERROR_PROFILE: " + ex.getMessage());

        CompletableFuture<List<String>> ordersCF = CompletableFuture
                .supplyAsync(() -> fetchOrders(userId))
                .completeOnTimeout(Collections.emptyList(), 300, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> { System.out.println("Orders error: " + ex); return List.of(); });

        CompletableFuture<List<String>> recsCF = CompletableFuture
                .supplyAsync(() -> fetchRecommendations("U_REAL_PROFILE"))
                .completeOnTimeout(List.of("POPULAR_ITEM_1"), 150, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> List.of("FALLBACK_REC"));

        // Wait for all 3 to complete (or timeout/fallback)
        CompletableFuture.allOf(profileCF, ordersCF, recsCF).get();

        // Collect results
        String profile = profileCF.join();
        List<String> orders = ordersCF.join();
        List<String> recs = recsCF.join();

        System.out.printf("Profile: %s%n", profile.substring(0, Math.min(40, profile.length())));
        System.out.printf("Orders: %d items%n", orders.size());
        System.out.printf("Recs: %s%n", recs);
        System.out.printf("Total time: %dms (max of all individual calls)%n",
                System.currentTimeMillis() - start);
        // Expected: ~150ms (slowest is 150ms with orders at 150ms)
        // Without parallelism: 100 + 150 + 80 = 330ms

        System.out.println("\nCF vs Virtual Threads:");
        System.out.println("  CF: best for complex fan-out/fan-in pipelines like this one");
        System.out.println("  VT: best for simple sequential: result = step1(); step2(result); step3(result2)");
        System.out.println("  Both compose well in Java 21 — can use VT executor with CF!");

        // Java 21: Use virtual thread executor for CF stages
        ExecutorService vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture<String> vtCF = CompletableFuture
                .supplyAsync(() -> fetchUserProfile("U_VT"), vtExecutor)  // runs on virtual thread
                .thenApplyAsync(p -> p.toUpperCase(), vtExecutor);         // also on virtual thread
        System.out.println("VT-backed CF result: " + vtCF.get().substring(0, 30));
        vtExecutor.shutdown();
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture Deep Dive ===");
        System.out.println("ForkJoinPool.commonPool parallelism: " +
                ForkJoinPool.commonPool().getParallelism());

        demo1BasicAsync();
        demo2Combining();
        demo3ErrorHandling();
        demo4Timeouts();
        demo5RealWorldPattern();

        System.out.println("\n=== Key Takeaways ===");
        System.out.println("thenApply     : sync transform (map)");
        System.out.println("thenCompose   : async chain (flatMap) — avoids CF<CF<T>>");
        System.out.println("thenCombine   : zip 2 independent CFs");
        System.out.println("allOf         : fan-in (wait all), then join() each");
        System.out.println("anyOf         : race (first wins), returns CF<Object>");
        System.out.println("exceptionally : fallback on failure (like catch)");
        System.out.println("handle        : both success+failure (like try-catch)");
        System.out.println("whenComplete  : side-effect only (like finally)");
        System.out.println("orTimeout     : fail with TimeoutException");
        System.out.println("completeOnTimeout: use default on timeout (no exception)");

        /*
         * INTERVIEW Q: "What's the difference between .get() and .join()?"
         * A: .get() throws checked ExecutionException + InterruptedException.
         *    .join() throws unchecked CompletionException.
         *    Prefer .join() in CF pipelines (cleaner with lambdas that can't throw checked).
         *    Use .get() when you need to handle InterruptedException explicitly.
         *
         * INTERVIEW Q: "What is thenApplyAsync vs thenApply?"
         * A: thenApply: runs the transform in the thread that completed the CF.
         *    thenApplyAsync: always dispatches to a new ForkJoinPool thread.
         *    Use thenApplyAsync when the transform is heavy or you want to free
         *    up the completing thread quickly (e.g., it's a worker thread).
         *
         * INTERVIEW Q: "How do you handle N parallel tasks and collect all results?"
         * A: List<CF<T>> cfs = tasks.stream().map(t -> supplyAsync(() -> process(t))).toList();
         *    CF.allOf(cfs.toArray(new CF[0])).join();
         *    List<T> results = cfs.stream().map(CF::join).collect(toList());
         */
    }
}
