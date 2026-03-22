package com.java.concurrency.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import java.util.function.*;

/**
 * =============================================================================
 * CONCEPT: Parallel Streams Deep Dive — When They Help and When They Hurt
 * =============================================================================
 *
 * Stream.parallel() is one of the most misused Java features.
 * Adding .parallel() looks free but can HURT performance due to:
 *   - Thread creation and scheduling overhead (ForkJoinPool coordination)
 *   - Data splitting overhead (Spliterator.trySplit())
 *   - Result merging overhead (combining partial results)
 *   - Memory bandwidth saturation (threads fighting over cache lines)
 *
 * WHEN PARALLEL IS FASTER (golden rules):
 * ─────────────────────────────────────────────────────────────────────────────
 *   ✅ Large data: N > 10,000 elements (overhead < speedup)
 *   ✅ CPU-bound operations: heavy computation per element (parsing, hashing, encryption)
 *   ✅ Stateless/independent: no shared state between elements
 *   ✅ Good splittability: ArrayList, arrays (O(1) split); LinkedList is bad (O(N) split)
 *   ✅ No ordering required (or use unordered() to skip re-ordering overhead)
 *
 * WHEN PARALLEL IS SLOWER (common pitfalls):
 * ─────────────────────────────────────────────────────────────────────────────
 *   ❌ Small data: overhead > computation time (use sequential for N < 1000)
 *   ❌ I/O-bound: threads block on I/O, don't use CPU → use virtual threads instead
 *   ❌ Stateful lambdas: shared mutable state → data races or synchronization overhead
 *   ❌ ordered() operations: sorted(), forEachOrdered() → forces buffering
 *   ❌ Poor splittability: LinkedList, Stream.iterate() → hard to parallelize
 *   ❌ Small critical sections: synchronization overhead > parallel gain
 *
 * UNDER THE HOOD:
 *   Parallel streams use ForkJoinPool.commonPool() by default.
 *   The Spliterator divides data into chunks (fork), workers process each chunk,
 *   results are merged (join) — divide-and-conquer.
 *   Parallelism = Runtime.availableProcessors() - 1 (by default).
 *
 * FORKJOINPOOL CUSTOMIZATION:
 *   Default: commonPool (shared with parallelStream, CompletableFuture.supplyAsync, etc.)
 *   PROBLEM: if your parallel stream uses all commonPool threads, other tasks starve.
 *   SOLUTION: Use a custom ForkJoinPool:
 *     ForkJoinPool pool = new ForkJoinPool(4);
 *     pool.submit(() -> list.parallelStream().map(...).collect(...)).get()
 *   This isolates your parallel stream from the commonPool.
 *
 * THREAD-SAFETY PITFALL:
 *   Common bug: using non-thread-safe collection in a lambda.
 *   list.parallelStream().forEach(x -> results.add(x))  // BUG if results is ArrayList
 *   → Use collect(Collectors.toList()) or thread-safe alternatives
 *
 * AMDAHL'S LAW (theoretical speedup):
 *   Speedup = 1 / (S + (1-S)/N)
 *   where S = fraction of serial code, N = number of processors
 *   If 10% of code is serial: max speedup = 10x (even with infinite processors)
 *   → Minimize serial portions (data splitting, result merging) for best parallel gain
 *
 * TIME COMPLEXITY:
 *   O(N/P) parallel time + O(N) overhead = O(N/P) for large N, P processors
 *   Effective only when N/P >> overhead
 *
 * SPACE COMPLEXITY: O(N) for intermediate results + O(P) for thread stack
 *
 * INTERVIEW ANGLE:
 *   "When would you use parallel streams?"
 *   "What can go wrong with parallel streams?"
 *   "How do you control thread pool size for parallel streams?"
 *
 * REAL-WORLD USE:
 *   - Batch data processing (ETL pipelines)
 *   - Image processing (each pixel/frame is independent)
 *   - Large-scale aggregations (sum, average over millions of records)
 *   - NOT for web request handling (use virtual threads or async)
 * =============================================================================
 */
public class ParallelStreamsDeepDive {

    // =========================================================================
    // DEMO 1: When parallel is FASTER
    // =========================================================================

    /**
     * Demonstrates parallel speedup for CPU-bound operations on large data.
     *
     * CPU-bound: operations that compute heavily per element (no I/O).
     * Large data: N > 10k so parallelism overhead is amortized.
     * Stateless: each element processed independently, no shared state.
     *
     * EXPECTED: ~4x speedup on a 4-core machine (near-linear scaling).
     * REAL: typically 2-3x due to Amdahl's law overhead.
     */
    private static void demo1ParallelFaster() throws Exception {
        System.out.println("=== DEMO 1: When Parallel is FASTER ===");

        final int N = 2_000_000;
        List<Integer> largeList = IntStream.range(0, N).boxed().collect(Collectors.toList());

        // Simulate CPU-bound work: heavy computation per element
        Function<Integer, Double> heavyCompute = x -> {
            double result = x;
            for (int i = 0; i < 50; i++) {
                result = Math.sqrt(result + Math.sin(result));
            }
            return result;
        };

        // Sequential
        long seqStart = System.currentTimeMillis();
        double seqSum = largeList.stream()
                .mapToDouble(heavyCompute::apply)
                .sum();
        long seqTime = System.currentTimeMillis() - seqStart;

        // Parallel (using ForkJoinPool.commonPool)
        long parStart = System.currentTimeMillis();
        double parSum = largeList.parallelStream()
                .mapToDouble(heavyCompute::apply)
                .sum();
        long parTime = System.currentTimeMillis() - parStart;

        System.out.printf("Sequential: %dms | Parallel: %dms | Speedup: %.1fx%n",
                seqTime, parTime, (double) seqTime / parTime);
        System.out.printf("Results match: %b (sums: %.0f vs %.0f)%n",
                Math.abs(seqSum - parSum) < 0.01, seqSum, parSum);

        // Small data — parallel is SLOWER
        System.out.println("\n--- Small data: parallel SLOWER ---");
        List<Integer> smallList = IntStream.range(0, 100).boxed().collect(Collectors.toList());

        long smallSeqStart = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            smallList.stream().mapToInt(x -> x).sum();
        }
        long smallSeqTime = System.currentTimeMillis() - smallSeqStart;

        long smallParStart = System.currentTimeMillis();
        for (int i = 0; i < 10_000; i++) {
            smallList.parallelStream().mapToInt(x -> x).sum();
        }
        long smallParTime = System.currentTimeMillis() - smallParStart;

        System.out.printf("Small data (100 elements, 10k iterations): seq=%dms, par=%dms%n",
                smallSeqTime, smallParTime);
        System.out.println("Parallel is SLOWER for small data due to thread coordination overhead");
    }

    // =========================================================================
    // DEMO 2: Thread-safety pitfalls
    // =========================================================================

    /**
     * Shows the broken (race condition) and fixed versions of parallel stream usage.
     *
     * BUG: ArrayList is NOT thread-safe. Multiple threads adding concurrently
     * causes structural modifications: missed elements, ArrayIndexOutOfBoundsException,
     * or silent data corruption.
     *
     * FIX 1: Use collect() — designed for parallel streams, uses thread-local accumulators.
     * FIX 2: Use AtomicInteger for counters.
     * FIX 3: Use synchronized collection (but this serializes → no parallel benefit).
     */
    private static void demo2ThreadSafety() throws Exception {
        System.out.println("\n=== DEMO 2: Thread Safety Pitfalls ===");

        List<Integer> data = IntStream.range(0, 10_000).boxed().collect(Collectors.toList());

        // BUG: Non-thread-safe collection in parallel stream
        // GOTCHA: This sometimes "works" (no exception) but produces wrong count due to race conditions
        System.out.println("--- BUG: Shared mutable ArrayList ---");
        List<Integer> buggyResults = new ArrayList<>();
        try {
            data.parallelStream()
                    .filter(x -> x % 2 == 0)
                    .forEach(x -> buggyResults.add(x));  // RACE CONDITION!
            System.out.printf("  Buggy count: %d (expected 5000) — may be wrong!%n",
                    buggyResults.size());
            // Result may be < 5000 due to missed additions, or throw exception
        } catch (Exception e) {
            System.out.println("  Exception from race condition: " + e.getClass().getSimpleName());
        }

        // FIX 1: Use collect() — correct parallel collection
        System.out.println("\n--- FIX 1: Use collect() ---");
        List<Integer> correctResults = data.parallelStream()
                .filter(x -> x % 2 == 0)
                .collect(Collectors.toList());  // thread-safe: uses per-thread lists, then merges
        System.out.printf("  Correct count: %d (expected 5000)%n", correctResults.size());

        // FIX 2: AtomicInteger for counting
        System.out.println("\n--- FIX 2: AtomicInteger for counters ---");
        AtomicInteger atomicCount = new AtomicInteger(0);
        data.parallelStream()
                .filter(x -> x % 2 == 0)
                .forEach(x -> atomicCount.incrementAndGet());  // thread-safe CAS
        System.out.printf("  Atomic count: %d (expected 5000)%n", atomicCount.get());

        // FIX 3: Synchronized collection (poor performance — defeats parallelism)
        System.out.println("\n--- FIX 3: Synchronized collection (slow but correct) ---");
        List<Integer> syncResults = Collections.synchronizedList(new ArrayList<>());
        data.parallelStream()
                .filter(x -> x % 2 == 0)
                .forEach(syncResults::add);  // thread-safe but serialized adds
        System.out.printf("  Synchronized count: %d (expected 5000) — correct but slow!%n",
                syncResults.size());

        // KEY INSIGHT: Reduce/collect is the idiomatic parallel alternative to forEach
        System.out.println("\n--- BEST: Use reduce or collect (no shared state) ---");
        int sum = data.parallelStream()
                .filter(x -> x % 2 == 0)
                .reduce(0, Integer::sum);  // parallel reduction, no shared state
        System.out.printf("  Sum of evens: %d (expected 24990000)%n", sum);
    }

    // =========================================================================
    // DEMO 3: ForkJoinPool customization
    // =========================================================================

    /**
     * Use a CUSTOM ForkJoinPool instead of commonPool.
     *
     * WHY: The commonPool is shared across:
     *   - All parallelStream() calls in the JVM
     *   - CompletableFuture.supplyAsync() (no executor provided)
     *   - Arrays.parallelSort()
     *
     * If one heavy parallel stream uses all commonPool threads, other async
     * operations in the same JVM starve. Custom pool isolates the workload.
     *
     * USAGE PATTERN:
     *   ForkJoinPool pool = new ForkJoinPool(numThreads);
     *   ForkJoinTask<T> task = pool.submit(() -> stream.parallelStream()...collect());
     *   T result = task.get();
     *   pool.shutdown();
     *
     * NOTE: Java 19+ added ForkJoinPool.isCommonPool() for introspection.
     * The custom pool trick works because ForkJoinPool.submit() captures the
     * pool as the context for tasks forked within the submitted callable.
     *
     * GOTCHA: This is an undocumented behavior relying on the Fork/Join
     * implementation. Use it carefully; future JVM changes could break it.
     * Officially documented alternative: use Stream.gather() or custom collector.
     */
    private static void demo3CustomForkJoinPool() throws Exception {
        System.out.println("\n=== DEMO 3: Custom ForkJoinPool ===");

        List<Integer> data = IntStream.range(0, 1_000_000).boxed().collect(Collectors.toList());

        System.out.println("CommonPool parallelism: " + ForkJoinPool.commonPool().getParallelism());

        // Default: uses commonPool (shared)
        long sum1 = data.parallelStream().mapToLong(x -> x).sum();
        System.out.println("Default (commonPool) sum: " + sum1);

        // Custom pool with 2 threads (useful for limiting CPU usage)
        ForkJoinPool customPool = new ForkJoinPool(2);
        try {
            long sum2 = customPool.submit(() ->
                    data.parallelStream()
                            .peek(x -> {
                                // Verify: should see 2 threads, not commonPool parallelism
                                // Uncomment to inspect: System.out.println(Thread.currentThread().getName())
                            })
                            .mapToLong(x -> x)
                            .sum()
            ).get();
            System.out.printf("Custom pool (2 threads) sum: %d (same result, isolated threads)%n", sum2);
        } finally {
            customPool.shutdown();
        }

        // Why custom pool matters for FAANG-scale systems:
        // Scenario: Web server with parallel streams for analytics + CompletableFuture for DB.
        // Without custom pool: analytics parallel stream could starve DB async queries.
        // With custom pool: analytics uses its own threads, DB queries use commonPool.
        System.out.println("Custom pool use case: isolate batch work from reactive async work");
    }

    // =========================================================================
    // DEMO 4: Ordering — unordered() hint
    // =========================================================================

    /**
     * Demonstrates how ordering affects parallel performance.
     *
     * ORDERED STREAMS: some sources have encounter order (List, arrays).
     * Parallel + ordered requires collecting results IN INPUT ORDER, which
     * forces extra buffering and coordination between threads.
     *
     * unordered(): hint to stream that result order doesn't matter.
     * This allows parallel workers to emit results as soon as available,
     * without waiting for earlier elements.
     *
     * WHEN ORDER MATTERS: toList(), forEachOrdered(), findFirst()
     * WHEN ORDER DOESN'T MATTER: sum, count, max, min, collect(toSet())
     *
     * GOTCHA: sorted() on parallel stream:
     *   List result = list.parallelStream().sorted().collect(toList())
     *   This is SLOWER than sequential.sorted()!
     *   Why: parallel must process all elements (no early termination),
     *   sort each partition, then MERGE-SORT the partitions.
     *   Sequential sort has better cache locality and no thread coordination.
     */
    private static void demo4Ordering() throws InterruptedException {
        System.out.println("\n=== DEMO 4: Ordering Impact ===");

        List<Integer> data = IntStream.range(0, 500_000).boxed().collect(Collectors.toList());

        // Parallel with ordering (default for List source)
        long orderedStart = System.currentTimeMillis();
        List<Integer> ordered = data.parallelStream()
                .filter(x -> x % 2 == 0)
                .collect(Collectors.toList());  // maintains encounter order
        long orderedTime = System.currentTimeMillis() - orderedStart;

        // Parallel with unordered() — faster for collect to List when order doesn't matter
        long unorderedStart = System.currentTimeMillis();
        List<Integer> unordered = data.parallelStream()
                .unordered()                         // ORDER DOESN'T MATTER
                .filter(x -> x % 2 == 0)
                .collect(Collectors.toList());       // no ordering guarantee
        long unorderedTime = System.currentTimeMillis() - unorderedStart;

        System.out.printf("Ordered parallel: %dms | Unordered parallel: %dms%n",
                orderedTime, unorderedTime);
        System.out.printf("Both have %d elements: %b%n",
                ordered.size(), ordered.size() == unordered.size());

        // GOTCHA: parallel + sorted = often slower than sequential
        System.out.println("\n--- parallel + sorted vs sequential ---");
        List<Integer> shuffled = new ArrayList<>(data.subList(0, 100_000));
        Collections.shuffle(shuffled);

        long parSortStart = System.currentTimeMillis();
        List<Integer> parSorted = shuffled.parallelStream().sorted().collect(Collectors.toList());
        long parSortTime = System.currentTimeMillis() - parSortStart;

        long seqSortStart = System.currentTimeMillis();
        List<Integer> seqSorted = shuffled.stream().sorted().collect(Collectors.toList());
        long seqSortTime = System.currentTimeMillis() - seqSortStart;

        System.out.printf("Parallel sort: %dms | Sequential sort: %dms%n", parSortTime, seqSortTime);
        System.out.println("GOTCHA: parallel.sorted() often slower due to merge overhead!");
    }

    // =========================================================================
    // DEMO 5: Custom Spliterator
    // =========================================================================

    /**
     * A custom Spliterator to make a range of integers parallelizable.
     *
     * Spliterator is the "parallel-aware iterator". It can split itself into two
     * halves, allowing ForkJoinPool to distribute work across threads.
     *
     * CHARACTERISTICS FLAGS (bit flags passed to super()):
     *   SIZED:     knows exact size (estimateSize() is accurate)
     *   ORDERED:   has a meaningful encounter order
     *   IMMUTABLE: source won't change during traversal
     *   SUBSIZED:  split halves also have accurate sizes
     *
     * HOW FORKING WORKS:
     *   ForkJoinPool calls trySplit() repeatedly to divide the spliterator.
     *   Each split returns a new Spliterator for the first half.
     *   The split continues until sub-tasks are small enough to process linearly.
     *   Then tryAdvance() / forEachRemaining() processes elements.
     *
     * WHY CUSTOM SPLITERATOR?
     *   Built-in spliterators handle ArrayList, arrays, ranges well.
     *   Custom spliterators needed for: databases (paginated queries), graphs,
     *   lazy sequences, files (split by byte offset), tree structures.
     */
    private static class RangeSpliterator implements Spliterator<Long> {
        private long start;           // current start (inclusive)
        private final long end;       // end (exclusive)
        private static final long MIN_BATCH = 1000;  // don't split below this

        RangeSpliterator(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Long> action) {
            if (start >= end) return false;
            action.accept(start++);
            return true;
        }

        @Override
        public void forEachRemaining(Consumer<? super Long> action) {
            // More efficient batch processing than repeated tryAdvance
            for (long i = start; i < end; i++) {
                action.accept(i);
            }
            start = end;
        }

        /**
         * Split into two halves — this is what enables parallelism.
         * Called by ForkJoinPool to distribute work.
         *
         * GOOD SPLIT: always halve → balanced binary tree of tasks → O(log N) depth.
         * BAD SPLIT: split off one at a time (like LinkedList) → O(N) depth → no parallelism.
         *
         * Returns the LEFT half as a new spliterator; this becomes the RIGHT half.
         * Returns null if too small to split (MIN_BATCH threshold).
         */
        @Override
        public Spliterator<Long> trySplit() {
            long remaining = end - start;
            if (remaining < MIN_BATCH) return null;  // too small, process sequentially

            long mid = start + remaining / 2;
            RangeSpliterator left = new RangeSpliterator(start, mid);
            this.start = mid;  // this becomes the right half
            return left;
        }

        @Override
        public long estimateSize() { return end - start; }

        @Override
        public int characteristics() {
            // SIZED: we know exact size; ORDERED: sequential; IMMUTABLE: no modification; SUBSIZED: splits accurate
            return SIZED | ORDERED | IMMUTABLE | SUBSIZED;
        }
    }

    private static void demo5CustomSpliterator() {
        System.out.println("\n=== DEMO 5: Custom Spliterator ===");

        long N = 1_000_000L;
        Spliterator<Long> spliterator = new RangeSpliterator(0, N);

        // Make a stream from our custom spliterator
        // StreamSupport.stream(spliterator, parallel=true)
        long sum = StreamSupport.stream(spliterator, true)  // true = parallel
                .mapToLong(Long::longValue)
                .sum();

        long expected = N * (N - 1) / 2;
        System.out.printf("Custom spliterator sum: %d (expected: %d) ✓%n", sum, expected);
        System.out.println("The spliterator's trySplit() enables parallel processing of custom data sources");

        // Real-world example: database pagination spliterator
        // trySplit() → fetch next page range [0..500], [500..1000]...
        // Each "page" processed on a separate ForkJoinPool thread
        System.out.println("Use case: paginated DB query, each page processed in parallel");
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) throws Exception {
        System.out.println("=== Parallel Streams Deep Dive ===");
        System.out.printf("Available processors: %d%n",
                Runtime.getRuntime().availableProcessors());
        System.out.printf("ForkJoinPool.commonPool parallelism: %d%n%n",
                ForkJoinPool.commonPool().getParallelism());

        demo1ParallelFaster();
        demo2ThreadSafety();
        demo3CustomForkJoinPool();
        demo4Ordering();
        demo5CustomSpliterator();

        System.out.println("\n=== Decision Guide: parallel() or not? ===");
        System.out.println("  N > 10,000 AND CPU-bound AND stateless → YES");
        System.out.println("  N < 1,000 → NO (overhead dominates)");
        System.out.println("  I/O-bound → NO (use virtual threads instead)");
        System.out.println("  Shared mutable state → NO (race conditions)");
        System.out.println("  Need sorted/ordered output → PROBABLY NO");
        System.out.println("  LinkedList source → NO (O(N) split = no parallelism)");
        System.out.println("  ArrayList/Array source → YES (O(1) split)");
        System.out.println("  Need to limit CPU usage → YES with custom ForkJoinPool");

        /*
         * INTERVIEW Q: "Why is parallelStream() not always faster?"
         * A: The JVM must:
         *   1. Split the data (via Spliterator.trySplit()) — O(log N) overhead
         *   2. Dispatch sub-tasks to ForkJoinPool workers — thread communication overhead
         *   3. Merge results from all workers — O(P) overhead
         *   For small N, these overheads dominate computation time.
         *   For sequential I/O operations, threads block and CPU is wasted on context switching.
         *
         * INTERVIEW Q: "What's wrong with forEach in parallel streams?"
         * A: forEach with side effects on shared state causes race conditions.
         *    Even with a synchronized block inside forEach, you've effectively
         *    serialized the parallel work — negating the parallelism benefit.
         *    PREFER: collect(), reduce() — designed for parallel, no shared state.
         *
         * INTERVIEW Q: "How does ForkJoinPool.commonPool differ from a regular thread pool?"
         * A: Work-stealing: idle threads steal tasks from busy threads' deques.
         *    This maximizes CPU utilization when tasks have unequal sizes.
         *    Regular thread pools have a single shared queue (contention at scale).
         *    ForkJoin: each thread has its own deque; steal from tail, push/pop from head
         *    → minimal contention, better cache locality for small tasks.
         */
    }
}
