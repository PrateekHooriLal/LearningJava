package com.java.functional;

/**
 * CONCEPT: Advanced Stream API
 *
 * CORE IDEA: Beyond basic filter/map/collect, the Stream API has powerful
 * operations for splitting, grouping, custom collection, and performance-tuning.
 * Knowing these separates junior developers from senior ones in interviews.
 *
 * WHEN TO USE:
 *   - Custom Collector: when built-in collectors don't express your aggregation
 *   - Collector.teeing: compute two aggregates in one stream pass (avoid two streams)
 *   - groupingBy + downstream: multi-level aggregations (stats per group)
 *   - mapMulti: variable-expansion more efficient than flatMap for many->many
 *   - takeWhile/dropWhile: early termination on ordered streams
 *   - IntStream/LongStream: avoid boxing overhead for numeric pipelines
 *
 * TIME/SPACE: See individual operations. Key point: streams are LAZY — intermediate
 *   operations (filter, map) don't run until a terminal operation is called.
 *
 * INTERVIEW ANGLE:
 *   Q: What are the four components of a Collector?
 *   A: 1. Supplier<A>: creates the mutable accumulation container
 *      2. BiConsumer<A,T>: adds one element to the container
 *      3. BinaryOperator<A>: merges two containers (for parallel streams)
 *      4. Function<A,R>: transforms final container into result type
 *      Plus optional Characteristics: CONCURRENT, UNORDERED, IDENTITY_FINISH
 *
 *   Q: What does Collector.teeing do (Java 12+)?
 *   A: Splits the stream into two sub-collectors and merges their results.
 *      Elements flow to BOTH collectors simultaneously. One stream pass.
 *      Great for computing two aggregates (min+max, sum+count) without two passes.
 *
 *   Q: What is the GOTCHA with toMap and duplicate keys?
 *   A: Without a merge function, duplicate keys throw IllegalStateException.
 *      Always provide a merge function for data that might have duplicates.
 *
 *   Q: When to use mapMulti vs flatMap?
 *   A: flatMap creates an intermediate Stream for each element (O(n) Streams).
 *      mapMulti uses a BiConsumer — you push values to a consumer, no Stream created.
 *      mapMulti is more efficient for variable-length expansions.
 *
 * REAL-WORLD USE:
 *   - Histogram Collector: analytics dashboards, frequency analysis
 *   - teeing: reporting (count + average in one pass over large dataset)
 *   - groupingBy: sales data per region per product
 *   - IntStream.range: matrix operations, indexed processing, batch sizing
 *
 * GOTCHA:
 *   - Stream is ONE-USE: calling terminal op on a used stream → IllegalStateException
 *   - Parallel streams: order not guaranteed; stateful lambdas → data races
 *   - toMap without merge: any duplicate key → IllegalStateException
 *   - Collectors.joining: only for Stream<String>; for other types, map first
 *   - takeWhile: only stops at first false for ORDERED streams; use with sorted or sequential
 */

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class AdvancedStreams {

    // =========================================================================
    // SECTION 1: Custom Collector — Histogram
    // =========================================================================

    /**
     * KEY INSIGHT: Implement Collector.of() when you need aggregation logic that
     * built-in collectors don't express. The four components mirror how you'd
     * write the aggregation imperatively.
     *
     * HISTOGRAM: count occurrences of each distinct element → Map<T, Long>
     *
     * COLLECTOR COMPONENTS for histogram:
     *   supplier():    () → new HashMap<T, Long>()   — create the accumulator
     *   accumulator(): (map, element) → increment count for element
     *   combiner():    (map1, map2) → merge maps (for parallel)
     *   finisher():    identity (map IS the result) — marked IDENTITY_FINISH
     *
     * COMPLEXITY:
     *   Time:  O(n) — one pass over stream
     *   Space: O(k) — k = number of distinct elements
     *
     * NOTE: This is equivalent to Collectors.groupingBy(identity(), counting())
     *   but implementing it manually teaches the Collector contract.
     */
    public static <T> Collector<T, ?, Map<T, Long>> histogramCollector() {
        return Collector.of(
            HashMap::new,                                   // supplier: create empty Map
            (map, element) -> map.merge(element, 1L, Long::sum), // accumulator: count++
            (map1, map2) -> {                               // combiner: merge for parallel
                map2.forEach((k, v) -> map1.merge(k, v, Long::sum));
                return map1;
            }
            // No finisher needed: Collector.of with 3 args uses IDENTITY_FINISH
            // which means the accumulation container (Map) IS the result.
            // Alternatively: pass finisher as 4th arg and add Collector.Characteristics.IDENTITY_FINISH
        );
        // Characteristics: could add UNORDERED (order doesn't matter for a histogram)
    }

    /**
     * Custom Collector: top-N elements (like a priority queue collector)
     * Demonstrates finisher transformation: accumulator type ≠ result type
     */
    public static <T extends Comparable<T>> Collector<T, ?, List<T>> topN(int n) {
        return Collector.of(
            // supplier: create a PriorityQueue (min-heap of size n)
            () -> new PriorityQueue<T>(n + 1),

            // accumulator: add element, keep only n largest
            (pq, element) -> {
                pq.offer(element);
                if (pq.size() > n) pq.poll(); // remove smallest (min-heap)
            },

            // combiner: merge two priority queues (for parallel)
            (pq1, pq2) -> {
                pq1.addAll(pq2);
                while (pq1.size() > n) pq1.poll();
                return pq1;
            },

            // finisher: convert PriorityQueue → sorted List (descending)
            pq -> {
                List<T> result = new ArrayList<>(pq);
                result.sort(Comparator.reverseOrder());
                return result;
            }
        );
    }

    // =========================================================================
    // SECTION 2: Collector.teeing — Two Collectors, One Pass
    // =========================================================================

    /**
     * KEY INSIGHT: Collector.teeing(c1, c2, merger) routes each element to
     * BOTH c1 and c2 simultaneously, then combines their results with merger.
     *
     * WHY TEEING:
     *   Without teeing: stream().min() + stream().max() = TWO stream passes
     *   With teeing: one pass computes both min and max
     *
     *   For large datasets (100M records from DB), this halves processing time.
     *
     * ANALOGY: Named after a T-pipe fitting — water flows into two branches simultaneously.
     *
     * JAVA 12+: Collectors.teeing(downstream1, downstream2, merger)
     *
     * COMPLEXITY: O(n) time, one pass, space = max of c1 and c2 space requirements
     */
    record MinMax<T>(T min, T max) {}

    public static <T extends Comparable<T>> MinMax<T> minMaxInOnePass(Stream<T> stream) {
        return stream.collect(
            Collectors.teeing(
                Collectors.minBy(Comparator.naturalOrder()),  // c1: find min
                Collectors.maxBy(Comparator.naturalOrder()),  // c2: find max
                (min, max) -> new MinMax<>(                   // merge results
                    min.orElseThrow(),
                    max.orElseThrow()
                )
            )
        );
    }

    // Another teeing example: compute sum AND count in one pass → average
    record SumCount(long sum, long count) {
        double average() { return count == 0 ? 0 : (double) sum / count; }
    }

    public static SumCount sumAndCount(Stream<Integer> stream) {
        return stream.collect(
            Collectors.teeing(
                Collectors.summingLong(Integer::longValue),  // c1: sum
                Collectors.counting(),                        // c2: count
                SumCount::new                                 // merge into record
            )
        );
    }

    // =========================================================================
    // SECTION 3: groupingBy with Downstream Collectors
    // =========================================================================

    /**
     * KEY INSIGHT: groupingBy alone gives Map<K, List<V>>.
     * With a downstream collector, you can compute ANY aggregation per group.
     *
     * PATTERN: groupingBy(classifier, downstreamCollector)
     *   groupingBy(person → dept, counting())
     *   → Map<String, Long>  (count of persons per department)
     *
     * MULTI-LEVEL GROUPING:
     *   groupingBy(dept, groupingBy(city, counting()))
     *   → Map<String, Map<String, Long>>  (count per dept per city)
     *
     * USEFUL DOWNSTREAM COLLECTORS:
     *   counting()                  → Long
     *   summingInt(fn)              → Integer
     *   averagingDouble(fn)         → Double
     *   mapping(fn, toList())       → List<R>
     *   joining(", ")               → String (must be String elements)
     *   maxBy(comparator)           → Optional<T>
     *   toUnmodifiableList()        → List<T>
     *   partitioningBy(predicate)   → Map<Boolean, List<T>>
     */
    record Employee(String name, String dept, String city, int salary) {}

    public static void demonstrateGroupingBy() {
        System.out.println("=== groupingBy with Downstream Collectors ===");

        List<Employee> employees = List.of(
            new Employee("Alice",   "Engineering", "NYC", 120000),
            new Employee("Bob",     "Engineering", "LA",  110000),
            new Employee("Carol",   "Marketing",   "NYC",  90000),
            new Employee("Dave",    "Marketing",   "LA",   85000),
            new Employee("Eve",     "Engineering", "NYC", 130000),
            new Employee("Frank",   "Marketing",   "NYC",  95000),
            new Employee("Grace",   "Engineering", "LA",  115000)
        );

        // 1. Count per department
        Map<String, Long> countByDept = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        System.out.println("  Count per dept: " + countByDept);

        // 2. Average salary per department
        Map<String, Double> avgSalaryByDept = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.averagingInt(Employee::salary)));
        System.out.println("  Avg salary per dept: " + avgSalaryByDept);

        // 3. Names per department (mapping + joining)
        Map<String, String> namesByDept = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.mapping(Employee::name, Collectors.joining(", "))));
        System.out.println("  Names per dept: " + namesByDept);

        // 4. Multi-level: per dept, per city → count
        Map<String, Map<String, Long>> byDeptAndCity = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.groupingBy(Employee::city, Collectors.counting())));
        System.out.println("  Count by dept→city: " + byDeptAndCity);

        // 5. Highest paid per department
        Map<String, Optional<Employee>> topEarnerByDept = employees.stream()
            .collect(Collectors.groupingBy(
                Employee::dept,
                Collectors.maxBy(Comparator.comparingInt(Employee::salary))));
        topEarnerByDept.forEach((dept, emp) ->
            System.out.println("  Top earner in " + dept + ": " + emp.map(Employee::name).orElse("none")));
    }

    // =========================================================================
    // SECTION 4: partitioningBy — Always Two Keys
    // =========================================================================

    /**
     * KEY INSIGHT: partitioningBy always returns Map<Boolean, List<T>> with
     * BOTH keys (true and false) present, even if one partition is empty.
     * This is unlike groupingBy which only has keys for existing values.
     *
     * GOTCHA: When using groupingBy with a boolean predicate, if no element
     * satisfies the condition, the true key is ABSENT from the map.
     * partitioningBy ALWAYS has both keys.
     *
     * USE CASE: When you need both halves of a split (passing/failing validation,
     *   adults/minors, active/inactive users).
     */
    public static void demonstratePartitioningBy() {
        System.out.println("\n=== partitioningBy (Both Partitions Always Present) ===");

        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Map<Boolean, List<Integer>> evenOdd = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0));
        System.out.println("  Even: " + evenOdd.get(true));   // [2, 4, 6, 8, 10]
        System.out.println("  Odd:  " + evenOdd.get(false));  // [1, 3, 5, 7, 9]

        // Edge case: no element satisfies predicate → empty list, not absent key
        Map<Boolean, List<Integer>> allFail = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n > 100));
        System.out.println("  partitioningBy(>100) true: " + allFail.get(true));   // []
        System.out.println("  partitioningBy(>100) false: " + allFail.get(false)); // all
        System.out.println("  Key 'true' present? " + allFail.containsKey(true));  // true (always!)

        // Partition with downstream
        Map<Boolean, Long> countEvenOdd = numbers.stream()
            .collect(Collectors.partitioningBy(n -> n % 2 == 0, Collectors.counting()));
        System.out.println("  Count even: " + countEvenOdd.get(true) + ", odd: " + countEvenOdd.get(false));
    }

    // =========================================================================
    // SECTION 5: toMap — Handle Duplicates
    // =========================================================================

    /**
     * KEY INSIGHT: Collectors.toMap(keyMapper, valueMapper) throws
     * IllegalStateException on duplicate keys. ALWAYS provide a merge function
     * when duplicates are possible.
     *
     * SIGNATURE:
     *   toMap(keyMapper, valueMapper)                     — exception on duplicate
     *   toMap(keyMapper, valueMapper, mergeFunction)      — handle duplicates
     *   toMap(keyMapper, valueMapper, mergeFunction, mapSupplier) — custom map type
     *
     * MERGE FUNCTION: (existingValue, newValue) → resolvedValue
     *   Keep first:  (existing, newVal) → existing
     *   Keep last:   (existing, newVal) → newVal
     *   Combine:     (existing, newVal) → existing + ", " + newVal
     *   Throw:       (existing, newVal) → { throw new IllegalStateException(); }
     */
    public static void demonstrateToMap() {
        System.out.println("\n=== toMap with Merge Function ===");

        List<String> words = List.of("apple", "banana", "avocado", "blueberry", "cherry");

        // Map: first char → word  (might have duplicates: 'a' → apple, avocado)
        // WITHOUT merge: IllegalStateException on duplicate key!
        try {
            Map<Character, String> bad = words.stream()
                .collect(Collectors.toMap(
                    w -> w.charAt(0),
                    w -> w));
        } catch (IllegalStateException e) {
            System.out.println("  Without merge → " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // WITH merge: keep longest word for each first char
        Map<Character, String> longest = words.stream()
            .collect(Collectors.toMap(
                w -> w.charAt(0),                               // key: first char
                w -> w,                                          // value: word itself
                (existing, newer) ->                             // merge: keep longer
                    existing.length() >= newer.length() ? existing : newer
            ));
        System.out.println("  Longest by first char: " + longest);

        // Collect all words per first char (merge by concatenation)
        Map<Character, String> allByChar = words.stream()
            .collect(Collectors.toMap(
                w -> w.charAt(0),
                w -> w,
                (a, b) -> a + ", " + b));  // merge: concatenate
        System.out.println("  All by first char: " + allByChar);

        // Custom map type (TreeMap for sorted keys)
        TreeMap<Character, String> sorted = words.stream()
            .collect(Collectors.toMap(
                w -> w.charAt(0),
                w -> w,
                (a, b) -> a + "/" + b,
                TreeMap::new));  // map supplier: TreeMap for sorted output
        System.out.println("  Sorted TreeMap: " + sorted);
    }

    // =========================================================================
    // SECTION 6: flatMap vs mapMulti
    // =========================================================================

    /**
     * KEY INSIGHT: mapMulti (Java 16+) is a push-based alternative to flatMap.
     *
     * flatMap: each element → Stream<R> (pull-based, creates intermediate Stream object)
     *   Pro: familiar, composable
     *   Con: creates a Stream object per element (GC pressure)
     *
     * mapMulti: each element → push values via Consumer<R>
     *   Pro: no intermediate Stream objects → less GC pressure
     *   Con: less readable (BiConsumer pattern)
     *
     * SIGNATURE: <R> Stream<R> mapMulti(BiConsumer<T, Consumer<R>> mapper)
     *   For each T, call consumer.accept(r) for each R you want to emit.
     *
     * WHEN TO PREFER mapMulti:
     *   - High-throughput numeric streams (avoid boxing)
     *   - When expansion ratio varies wildly (some expand to 0, some to 100)
     *   - Performance-critical pipelines where GC is a concern
     */
    public static void demonstrateFlatMapVsMapMulti() {
        System.out.println("\n=== flatMap vs mapMulti ===");

        List<List<Integer>> nested = List.of(
            List.of(1, 2, 3),
            List.of(4, 5),
            List.of(6, 7, 8, 9)
        );

        // flatMap: each sublist → Stream of elements
        List<Integer> flatMapped = nested.stream()
            .flatMap(Collection::stream)  // List<Integer> → Stream<Integer>
            .toList();
        System.out.println("  flatMap: " + flatMapped);

        // mapMulti: push each element individually via consumer
        List<Integer> multiMapped = nested.stream()
            .<Integer>mapMulti((subList, consumer) -> {
                for (Integer i : subList) {
                    consumer.accept(i);  // push each element
                }
            })
            .toList();
        System.out.println("  mapMulti: " + multiMapped);

        // mapMulti for filtering + expanding (conditional emit)
        // Only emit negative and positive (skip zeros), duplicated:
        List<Integer> numbers = List.of(-2, 0, 3, 0, -1, 4);
        List<Integer> expanded = numbers.stream()
            .<Integer>mapMulti((n, consumer) -> {
                if (n != 0) {          // filter: skip zeros
                    consumer.accept(n);     // emit original
                    consumer.accept(n * 2); // emit doubled
                }
            })
            .toList();
        System.out.println("  mapMulti (skip 0, emit original+doubled): " + expanded);
        // → [-2, -4, 3, 6, -1, -2, 4, 8]
    }

    // =========================================================================
    // SECTION 7: Stream.iterate vs Stream.generate
    // =========================================================================

    /**
     * KEY INSIGHT: Both create infinite streams. The difference is DEPENDENCY.
     *
     * Stream.iterate(seed, f): each element DEPENDS on the previous.
     *   iterate(0, n -> n + 2) → 0, 2, 4, 6, 8, ...  (sequential dependency)
     *   iterate(1, n -> n * 2) → 1, 2, 4, 8, 16, ... (powers of 2)
     *   Java 9+: iterate(seed, hasNext, f) — with a stopping predicate (like a for loop)
     *
     * Stream.generate(supplier): each element is INDEPENDENT.
     *   generate(Math::random) → 0.3, 0.8, 0.1, ...  (random, no dependency)
     *   generate(() -> "hello") → "hello", "hello", ...  (constant)
     *   For parallel streams, generate is better (no sequential dependency).
     *
     * BOTH are lazy — infinite until you limit(n), takeWhile, or findFirst.
     */
    public static void demonstrateIterateVsGenerate() {
        System.out.println("\n=== Stream.iterate vs Stream.generate ===");

        // iterate: sequential dependency (n+1 depends on n)
        List<Integer> first10Even = Stream.iterate(0, n -> n + 2)
            .limit(10)
            .toList();
        System.out.println("  iterate (evens): " + first10Even);

        // iterate with predicate (Java 9+): like a for loop
        List<Integer> under100Powers = Stream.iterate(1, n -> n < 100, n -> n * 2)
            .toList();
        System.out.println("  iterate (powers of 2 under 100): " + under100Powers);

        // Fibonacci using iterate:
        List<long[]> fibPairs = Stream.iterate(
            new long[]{0, 1},
            p -> new long[]{p[1], p[0] + p[1]}
        ).limit(10).toList();
        System.out.print("  Fibonacci (iterate): ");
        fibPairs.forEach(p -> System.out.print(p[0] + " "));
        System.out.println();

        // generate: independent elements
        List<Double> randoms = Stream.generate(Math::random)
            .limit(5)
            .toList();
        System.out.println("  generate (random doubles count): " + randoms.size());

        // generate with state (via AtomicInteger — use carefully, stateful)
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        List<Integer> counting = Stream.generate(counter::getAndIncrement)
            .limit(5)
            .toList();
        System.out.println("  generate with AtomicInt: " + counting);
    }

    // =========================================================================
    // SECTION 8: takeWhile / dropWhile (Java 9+)
    // =========================================================================

    /**
     * KEY INSIGHT: takeWhile and dropWhile are for ORDERED streams.
     * They stop/skip at the first element that fails the predicate.
     *
     * takeWhile(predicate): take elements WHILE predicate holds, stop at first false.
     *   Unlike filter: filter skips non-matching, continues to end.
     *   takeWhile: stops at first non-matching (lazy short-circuit).
     *   Use case: sorted data where you want everything below a threshold.
     *
     * dropWhile(predicate): skip elements WHILE predicate holds, take the rest.
     *   Use case: skip a header in a sorted dataset, take the actual data.
     *
     * GOTCHA: For UNORDERED or parallel streams, behavior is nondeterministic.
     *   Use only with sorted/sequential streams for predictable results.
     */
    public static void demonstrateTakeWhileDropWhile() {
        System.out.println("\n=== takeWhile / dropWhile ===");

        List<Integer> sorted = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // takeWhile: take while < 5 (stops at 5, doesn't check 6-10)
        List<Integer> taken = sorted.stream()
            .takeWhile(n -> n < 5)
            .toList();
        System.out.println("  takeWhile(< 5): " + taken);  // [1, 2, 3, 4]

        // dropWhile: skip while < 5 (skips 1-4, takes 5-10)
        List<Integer> dropped = sorted.stream()
            .dropWhile(n -> n < 5)
            .toList();
        System.out.println("  dropWhile(< 5): " + dropped);  // [5, 6, 7, 8, 9, 10]

        // CONTRAST with filter:
        List<Integer> filtered = sorted.stream()
            .filter(n -> n < 5)
            .toList();
        System.out.println("  filter(< 5):    " + filtered);  // [1, 2, 3, 4] — same for sorted
        // But with [1, 3, 5, 2, 4], filter still gets [1, 3, 2, 4]
        // while takeWhile would get [1, 3] (stops at 5)

        // Practical: sorted timestamps — take events before cutoff
        List<String> logs = List.of("2024-01-01 ERROR", "2024-01-02 WARN", "2024-02-01 INFO", "2024-03-01 DEBUG");
        List<String> q1Logs = logs.stream()
            .takeWhile(log -> log.startsWith("2024-01") || log.startsWith("2024-02"))
            .toList();
        System.out.println("  Logs before March: " + q1Logs);
    }

    // =========================================================================
    // SECTION 9: Primitive Streams — Avoid Boxing
    // =========================================================================

    /**
     * KEY INSIGHT: IntStream, LongStream, DoubleStream avoid autoboxing overhead.
     * For numeric pipelines, ALWAYS prefer primitive streams over Stream<Integer>.
     *
     * Stream<Integer>.sum():   each Integer is an object → boxing/unboxing overhead
     * IntStream.sum():         all primitives → no object creation, cache-line friendly
     *
     * PERFORMANCE: For 1M integers, primitive IntStream is ~5-10x faster than Stream<Integer>
     * due to no boxing, no GC pressure, better CPU cache behavior.
     *
     * CONVERSION:
     *   Stream<T>.mapToInt(fn)  → IntStream
     *   IntStream.boxed()       → Stream<Integer>
     *   IntStream.mapToObj(fn)  → Stream<T>
     *
     * USEFUL PRIMITIVE STREAM OPERATIONS:
     *   IntStream.range(start, end)      — [start, end), exclusive end like for loops
     *   IntStream.rangeClosed(start, end) — [start, end], inclusive end
     *   .sum(), .average(), .min(), .max() — built-in aggregates, no Collector needed
     *   .summaryStatistics()             — count, sum, min, max, average in one pass
     */
    public static void demonstratePrimitiveStreams() {
        System.out.println("\n=== Primitive Streams (No Boxing Overhead) ===");

        // IntStream.range: like for(int i = start; i < end; i++)
        int sumRange = IntStream.range(1, 11).sum(); // 1+2+...+10 = 55
        System.out.println("  IntStream.range(1,11).sum() = " + sumRange);

        // rangeClosed: inclusive both ends
        int sumClosed = IntStream.rangeClosed(1, 10).sum(); // same: 55
        System.out.println("  IntStream.rangeClosed(1,10).sum() = " + sumClosed);

        // summaryStatistics: count + sum + min + max + average in ONE pass
        IntSummaryStatistics stats = IntStream.rangeClosed(1, 100).summaryStatistics();
        System.out.printf("  Stats: count=%d, sum=%d, min=%d, max=%d, avg=%.1f%n",
            stats.getCount(), stats.getSum(), stats.getMin(), stats.getMax(), stats.getAverage());

        // mapToInt: convert Stream<String> to IntStream of lengths
        List<String> words = List.of("hello", "world", "java", "streams");
        IntSummaryStatistics wordStats = words.stream()
            .mapToInt(String::length)
            .summaryStatistics();
        System.out.println("  Word length stats: avg=" + wordStats.getAverage() + " max=" + wordStats.getMax());

        // Generate matrix indices using nested IntStream
        System.out.print("  3x3 matrix coords: ");
        IntStream.range(0, 3).forEach(row ->
            IntStream.range(0, 3).forEach(col ->
                System.out.print("(" + row + "," + col + ") ")
            )
        );
        System.out.println();

        // Performance comparison: boxing vs no boxing
        long boxedStart = System.nanoTime();
        long boxedSum = Stream.iterate(0, n -> n + 1).limit(1_000_000)
            .mapToLong(Integer::longValue).sum();
        long boxedTime = System.nanoTime() - boxedStart;

        long primitiveStart = System.nanoTime();
        long primitiveSum = LongStream.range(0, 1_000_000).sum();
        long primitiveTime = System.nanoTime() - primitiveStart;

        System.out.printf("  Boxed stream (1M): %d ms, sum=%d%n", boxedTime / 1_000_000, boxedSum);
        System.out.printf("  LongStream (1M):   %d ms, sum=%d%n", primitiveTime / 1_000_000, primitiveSum);
    }

    // =========================================================================
    // SECTION 10: Collectors.joining
    // =========================================================================

    /**
     * KEY INSIGHT: Collectors.joining is the most common terminal operation
     * for string streams. Three overloads:
     *
     *   joining()                              — no separator
     *   joining(delimiter)                     — with separator
     *   joining(delimiter, prefix, suffix)     — with separator, prefix, and suffix
     *
     * COMMON PATTERN: build comma-separated list, SQL IN clause, CSV row, etc.
     */
    public static void demonstrateJoining() {
        System.out.println("\n=== Collectors.joining ===");

        List<String> names = List.of("Alice", "Bob", "Carol", "Dave");

        String plain    = names.stream().collect(Collectors.joining());
        String csv      = names.stream().collect(Collectors.joining(", "));
        String sqlIn    = names.stream().collect(Collectors.joining("', '", "('", "')"));
        String markdown = names.stream().collect(Collectors.joining(" | ", "| ", " |"));

        System.out.println("  plain:    " + plain);
        System.out.println("  csv:      " + csv);
        System.out.println("  SQL IN:   " + sqlIn);      // ('Alice', 'Bob', 'Carol', 'Dave')
        System.out.println("  markdown: " + markdown);   // | Alice | Bob | Carol | Dave |
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Advanced Streams Demo ===\n");

        // Custom Collector
        System.out.println("--- Custom Collectors ---");
        List<String> words = List.of("apple", "banana", "apple", "cherry", "banana", "apple");
        Map<String, Long> histogram = words.stream().collect(histogramCollector());
        System.out.println("  Histogram: " + histogram);

        List<Integer> nums = List.of(5, 2, 9, 1, 7, 3, 8, 4, 6);
        List<Integer> top3 = nums.stream().collect(topN(3));
        System.out.println("  Top-3: " + top3);  // [9, 8, 7]

        // Teeing
        System.out.println("\n--- Collector.teeing ---");
        MinMax<Integer> minMax = minMaxInOnePass(Stream.of(3, 1, 4, 1, 5, 9, 2, 6));
        System.out.println("  MinMax: min=" + minMax.min() + " max=" + minMax.max());

        SumCount sc = sumAndCount(Stream.of(10, 20, 30, 40, 50));
        System.out.println("  SumCount: sum=" + sc.sum() + " count=" + sc.count() + " avg=" + sc.average());

        // groupingBy
        demonstrateGroupingBy();
        demonstratePartitioningBy();
        demonstrateToMap();
        demonstrateFlatMapVsMapMulti();
        demonstrateIterateVsGenerate();
        demonstrateTakeWhileDropWhile();
        demonstratePrimitiveStreams();
        demonstrateJoining();

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("Custom Collector: Collector.of(supplier, accumulator, combiner, finisher)");
        System.out.println("teeing: one pass → two aggregates, great for min+max, sum+count");
        System.out.println("groupingBy+downstream: multi-level aggregation, any downstream collector");
        System.out.println("partitioningBy: ALWAYS two keys (true/false), even if one is empty list");
        System.out.println("toMap: ALWAYS provide merge function if duplicates possible → else IllegalStateException");
        System.out.println("mapMulti: push-based, more efficient than flatMap (no intermediate Stream)");
        System.out.println("iterate: sequential dependency; generate: independent elements");
        System.out.println("takeWhile/dropWhile: early termination on ordered streams (Java 9+)");
        System.out.println("IntStream/LongStream: no boxing overhead, 5-10x faster for numeric pipelines");
    }
}
