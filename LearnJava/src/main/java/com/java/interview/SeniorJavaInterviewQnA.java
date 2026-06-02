package com.java.interview;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ============================================================
 * SENIOR JAVA INTERVIEW — Q&A + CODING (Subject-Wise)
 * ============================================================
 *
 * Topics covered (exact subjects from real interview):
 *   1. Spring Bean Creation & How Spring Starts
 *   2. Spring Dependency Identification Mechanics
 *   3. Parallel Streams
 *   4. Java 17 & 21 New Features
 *   5. Functional Interfaces (Function, Consumer, Supplier, BiConsumer)
 *   6. CompletableFuture Exception Handling
 *   7. CODING: 2nd Highest Repeating Character (Stream + Parallel Stream)
 *
 * FORMAT per section:
 *   - Likely interview questions
 *   - The exact answer to give (SDE 3 level — not just "Spring handles it")
 *   - Runnable code demonstration
 *   - Gotchas and follow-ups
 *
 * HOW TO USE:
 *   Read one section, close file, answer the questions aloud.
 *   Come back and compare. Repeat until fluent.
 */
public class SeniorJavaInterviewQnA {

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println(" SUBJECT 3: PARALLEL STREAMS");
        System.out.println("========================================");
        parallelStreamsQnA();

        System.out.println("\n========================================");
        System.out.println(" SUBJECT 7: CODING QUESTION");
        System.out.println("========================================");
        codingQuestion();
    }

    // =========================================================
    // SUBJECT 1: SPRING BEAN CREATION & HOW SPRING STARTS
    // =========================================================
    // → Full code: com.java.spring.SpringBeanLifecycle
    //
    // Q1: "Walk me through how Spring starts up."
    //
    // A (SDE 3 level — memorize this sequence):
    //   "When SpringApplication.run() is called, it creates an ApplicationContext
    //    and calls refresh(). Refresh has 10 key steps:
    //
    //    1. Parse configuration — reads @ComponentScan, @Configuration, finds all classes
    //    2. Register BeanDefinitions — each found class becomes a BeanDefinition
    //       (metadata only, no instances yet — just the recipe)
    //    3. BeanFactoryPostProcessors run — they can MODIFY BeanDefinitions before
    //       any bean is created. PropertySourcesPlaceholderConfigurer is one — it
    //       resolves @Value('${...}') placeholders here.
    //    4. Instantiate singletons — beans are created in dependency order
    //    5. Inject dependencies — @Autowired fields/constructors populated
    //    6. BeanPostProcessor.postProcessBeforeInitialization — runs on each bean
    //    7. @PostConstruct / afterPropertiesSet / init-method — init logic runs
    //    8. BeanPostProcessor.postProcessAfterInitialization — runs on each bean.
    //       THIS is where AOP proxies are created for @Transactional, @Async, etc.
    //    9. Bean is ready in the context
    //    10. On shutdown: @PreDestroy → destroy-method"
    //
    // Q2: "What is a BeanDefinition?"
    // A: "The recipe for a bean — not the bean itself. Spring separates registration
    //    (BeanDefinition) from instantiation. BeanDefinitions hold: the class,
    //    scope, lazy flag, init/destroy method names, and constructor/property values.
    //    This separation allows BeanFactoryPostProcessors to modify the recipe
    //    before any beans are actually created."
    //
    // Q3: "What is the difference between BeanFactoryPostProcessor and BeanPostProcessor?"
    // A: "BeanFactoryPostProcessor runs BEFORE any beans are created — it operates
    //    on BeanDefinitions (the metadata). BeanPostProcessor runs AFTER each bean
    //    is instantiated — it operates on the bean instance and can return a
    //    different object (e.g., a proxy). AbstractAutoProxyCreator is a
    //    BeanPostProcessor — that's how @Transactional creates CGLIB proxies."
    //
    // Q4: "When is a @Transactional proxy created?"
    // A: "Step 8 — postProcessAfterInitialization. AbstractAutoProxyCreator
    //    (a BeanPostProcessor) detects @Transactional on the bean and returns
    //    a CGLIB subclass proxy instead of the original bean. The proxy overrides
    //    your methods: open transaction → call real method → commit or rollback."
    //
    // GOTCHAS:
    //   - Self-invocation bypasses the proxy: this.method() goes directly to the
    //     real bean, not through the proxy → no transaction. Fix: inject self
    //     via @Autowired or move to a separate @Service.
    //   - @PostConstruct runs BEFORE the AOP proxy is created, so logging
    //     inside @PostConstruct won't show transaction/AOP behavior.

    // =========================================================
    // SUBJECT 2: SPRING DEPENDENCY IDENTIFICATION MECHANICS
    // =========================================================
    // → Full code: com.java.spring.SpringDependencyResolution
    //
    // Q1: "How does Spring know what to inject into a @Service?"
    //
    // A (the complete mechanical answer):
    //   "Spring dependency resolution happens in 3 phases:
    //
    //    PHASE 1 — Discovery:
    //    ClassPathBeanDefinitionScanner scans the classpath using ASM (bytecode
    //    analysis — it does NOT load classes through ClassLoader). It finds all
    //    classes annotated with @Component (and stereotypes: @Service, @Repository,
    //    @Controller) and registers them as BeanDefinitions.
    //
    //    PHASE 2 — Identification:
    //    AutowiredAnnotationBeanPostProcessor.postProcessMergedBeanDefinition() runs
    //    before each bean is instantiated. It uses reflection to find all @Autowired
    //    fields, setters, and constructors, and stores the result as InjectionMetadata
    //    in a cache — so reflection only happens ONCE per class, not once per instance.
    //
    //    PHASE 3 — Resolution:
    //    When a bean is being created, DefaultListableBeanFactory.resolveDependency()
    //    finds all beans ASSIGNABLE to the required type. The resolution algorithm:
    //      1. Exactly 1 match → inject it
    //      2. 0 matches → NoSuchBeanDefinitionException (if required=true)
    //      3. Multiple → disambiguate:
    //         a. @Qualifier — highest priority (explicit name)
    //         b. @Primary   — default candidate
    //         c. @Priority  — numeric priority
    //         d. Name fallback — field name matches a bean name (implicit, careful!)
    //         e. Still ambiguous → NoUniqueBeanDefinitionException"
    //
    // Q2: "What happens when two beans implement the same interface?"
    // A: "Spring finds multiple candidates via resolveDependency(). It then looks
    //    for @Primary on one of them, or @Qualifier at the injection point. If
    //    neither exists, it tries name-matching (field name = bean name). If all
    //    three fail, it throws NoUniqueBeanDefinitionException. The fix is to
    //    add @Primary to the default implementation or @Qualifier at the injection point."
    //
    // Q3: "Does @Autowired inject by type or by name?"
    // A: "By TYPE first. Spring finds all beans assignable to the required type.
    //    Name only comes in as a FALLBACK — if multiple candidates exist and no
    //    @Primary/@Qualifier is present, Spring checks if any candidate's bean
    //    name matches the field name. So: primarily by type, name as tiebreaker."
    //
    // Q4: "Do you need @Autowired on a constructor?"
    // A: "Not if there's only ONE constructor — Spring 4.3+ auto-detects it.
    //    This is why modern Spring Boot code often has no @Autowired annotation
    //    at all — constructor injection with a single constructor is implicit."

    // =========================================================
    // SUBJECT 3: PARALLEL STREAMS
    // =========================================================

    static void parallelStreamsQnA() {
        // Q1: "How does a parallel stream work internally?"
        //
        // A: "A parallel stream uses the Fork/Join framework under the hood.
        //    When you call .parallelStream() or .parallel(), the stream's
        //    Spliterator divides (splits) the data into chunks — this is FORKING.
        //    Each chunk is processed by a worker thread from ForkJoinPool.commonPool().
        //    Results are then JOINED back together (merge/reduce phase).
        //    The commonPool has (availableProcessors - 1) threads by default."
        //
        // Q2: "When should you use a parallel stream?"
        //
        // A: "Three conditions must ALL be true for parallel to help:
        //    1. LARGE dataset — overhead of splitting/merging must be worth it
        //       (rule of thumb: thousands of elements minimum)
        //    2. CPU-bound operations — computation per element must be non-trivial
        //       (parallel stream does NOT help with I/O — use virtual threads instead)
        //    3. Good splittability — ArrayList, arrays split in O(1);
        //       LinkedList splits in O(n) — terrible for parallel"
        //
        // Q3: "What are the dangers of parallel streams?"
        //
        // A:
        //   "1. Shared mutable state — non-thread-safe operations in lambdas
        //       cause data races. Use thread-safe collectors instead.
        //    2. Ordered streams have overhead — sorted(), forEachOrdered()
        //       force buffering to maintain order, killing parallel benefit.
        //    3. commonPool starvation — parallel streams share ForkJoinPool.commonPool()
        //       with CompletableFuture.supplyAsync(). If both run together, they
        //       compete for the same threads.
        //    4. Small datasets — overhead of splitting and thread coordination
        //       is MORE than the parallel speedup."
        //
        // Q4: "How do you use a custom thread pool for a parallel stream?"
        //
        // A: "ForkJoinPool.commonPool() is the default and it's shared globally.
        //    To isolate, wrap the stream operation in a ForkJoinPool.submit():
        //    ForkJoinPool pool = new ForkJoinPool(4);
        //    List<Result> results = pool.submit(() ->
        //        list.parallelStream().map(this::process).collect(toList())
        //    ).get();"
        //
        // GOTCHA: "Does parallel stream use virtual threads?"
        // A: "NO. Parallel streams use PLATFORM threads from ForkJoinPool.
        //    Virtual threads are for I/O-bound concurrency.
        //    Parallel streams are for CPU-bound data processing.
        //    They solve different problems and should not be mixed."

        System.out.println("--- Q: When does parallel stream HELP vs HURT? ---");

        List<Integer> largeList = IntStream.rangeClosed(1, 1_000_000)
            .boxed().collect(Collectors.toList());

        // GOOD USE: large list, CPU-bound, no ordering, no shared state
        long sum = largeList.parallelStream()
            .mapToLong(i -> (long) i * i)   // CPU-bound: square each number
            .sum();                           // reduce — thread-safe terminal op
        System.out.println("  parallel sum of squares: " + sum);

        // BAD USE: shared mutable ArrayList — race condition!
        List<Integer> racyResult = new ArrayList<>();
        // DON'T DO THIS:
        // largeList.parallelStream().forEach(racyResult::add); // DATA RACE

        // CORRECT: use a thread-safe collector
        List<Integer> safeResult = largeList.parallelStream()
            .filter(i -> i % 2 == 0)
            .collect(Collectors.toList());  // Collectors.toList() is thread-safe in parallel
        System.out.println("  safe parallel filter, even count: " + safeResult.size());

        // groupingByConcurrent vs groupingBy in parallel:
        // groupingBy:           uses a sequential merge step → limits parallelism
        // groupingByConcurrent: uses ConcurrentHashMap → fully parallel
        Map<Integer, List<Integer>> grouped = IntStream.rangeClosed(1, 100).boxed()
            .collect(Collectors.groupingByConcurrent(i -> i % 5)); // parallel-friendly
        System.out.println("  groupingByConcurrent groups: " + grouped.size());

        // Custom ForkJoinPool to avoid commonPool contention
        System.out.println("--- Custom ForkJoinPool (isolates from commonPool) ---");
        try {
            ForkJoinPool customPool = new ForkJoinPool(2); // 2 threads only
            long customSum = customPool.submit(() ->
                IntStream.rangeClosed(1, 100).parallel().asLongStream().sum()
            ).get();
            System.out.println("  Custom pool sum: " + customSum);
            customPool.shutdown();
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }

        // Amdahl's Law — why parallel isn't always faster
        System.out.println("--- Amdahl's Law ---");
        System.out.println("  If 10% of work is serial: max speedup = 10x (even infinite cores)");
        System.out.println("  If 50% of work is serial: max speedup = 2x");
        System.out.println("  Parallel stream overhead = splitting + merging (serial parts)");
        System.out.println("  Rule of thumb: use parallel only when N >> 10,000 and CPU-bound");
    }

    // =========================================================
    // SUBJECT 4: JAVA 17 & 21 NEW FEATURES
    // =========================================================
    // → Full code: com.java.java21.Java17To21Features
    //
    // Q1: "What are the major features in Java 17?"
    // A: "Java 17 is an LTS release. The headline feature is SEALED CLASSES (final).
    //    Sealed classes restrict which classes can extend them using the 'permits'
    //    clause. Combined with pattern matching switch (Java 21), the compiler can
    //    verify that a switch over a sealed hierarchy is exhaustive — no default needed.
    //    Other notable 17 features: strong encapsulation of JDK internals (no more
    //    --illegal-access), deprecation of Security Manager.
    //    But in practice, most Java 17 'features' were finalized in 14-16:
    //    Records (16), Pattern matching instanceof (16), Text blocks (15),
    //    Switch expressions (14)."
    //
    // Q2: "What is new in Java 21?"
    // A (the 5 you must know):
    //   "1. Virtual Threads — lightweight threads (millions possible vs thousands
    //       for platform threads). Ideal for I/O-bound server workloads.
    //       Thread.ofVirtual().start(() -> ...) or Executors.newVirtualThreadPerTaskExecutor()
    //    2. Sequenced Collections — new interface adding getFirst(), getLast(),
    //       addFirst(), addLast(), reversed() to List, Deque, LinkedHashMap etc.
    //       Fills the gap: previously no clean way to get last element of a List.
    //    3. Record Patterns — deconstruct records in pattern matching:
    //       if (obj instanceof Point(int x, int y)) { ... }
    //    4. Pattern Matching for switch — final (was preview). Match on type with guards:
    //       case String s when s.isEmpty() -> ...
    //    5. Scoped Values — VT-safe replacement for ThreadLocal."
    //
    // Q3: "What is the difference between Virtual Threads and Platform Threads?"
    // A: "Platform threads are OS threads — expensive (1MB+ stack), limited to
    //    ~thousands. Virtual threads are JVM-managed lightweight threads — cheap
    //    (~few KB), millions possible. Virtual threads are MOUNTED on carrier
    //    platform threads from a ForkJoinPool. When a VT blocks on I/O, it
    //    UNMOUNTS from its carrier (which can then run another VT) — no OS thread
    //    is blocked. Key gotcha: synchronized blocks PIN a VT to its carrier,
    //    blocking other VTs. Use ReentrantLock instead with virtual threads."
    //
    // Q4: "What is the difference between Records and regular classes?"
    // A: "A record is an immutable data carrier. It auto-generates a canonical
    //    constructor, getters (name(), age()), equals(), hashCode(), toString().
    //    Records cannot extend other classes (implicitly extend java.lang.Record),
    //    cannot be extended, and all components are final. Use records for DTOs,
    //    value objects, and map keys. They work perfectly with pattern matching."

    // =========================================================
    // SUBJECT 5: FUNCTIONAL INTERFACES
    // =========================================================
    // → Full code: com.java.functional.FunctionalInterfacesDeepDive
    //
    // Q1: "What is a functional interface?"
    // A: "An interface with exactly ONE abstract method (SAM = Single Abstract Method).
    //    @FunctionalInterface annotation is optional but recommended — the compiler
    //    will error if you accidentally add a second abstract method.
    //    A lambda expression is an anonymous implementation of a functional interface."
    //
    // Q2: "What is the difference between Function and Consumer?"
    // A: "Function<T,R> transforms a value — takes T, returns R. Used in stream.map().
    //    Consumer<T> takes a value but returns void — used for side effects like
    //    printing, saving, logging. Used in stream.forEach().
    //    Both compose with andThen(), but Consumer's andThen doesn't short-circuit
    //    — both sides always execute unless an exception is thrown."
    //
    // Q3: "What is Supplier used for?"
    // A: "Supplier<T> takes nothing and returns T — it enables LAZY EVALUATION.
    //    The value is computed only when get() is called. The most important use:
    //    Optional.orElseGet(Supplier) vs Optional.orElse(value).
    //    orElse(value): value is ALWAYS evaluated even if Optional is present.
    //    orElseGet(supplier): supplier is called ONLY if Optional is empty.
    //    So if the fallback is expensive (DB call, object creation), always use
    //    orElseGet to avoid unnecessary computation."
    //
    // Q4: "What is BiConsumer?"
    // A: "BiConsumer<T,U> takes two arguments and returns void — it's Consumer
    //    for two inputs. Classic use: Map.forEach(BiConsumer<K,V>).
    //    It composes with andThen() like Consumer."
    //
    // Q5: "What is the difference between andThen and compose on Function?"
    // A: "f.andThen(g) applies f FIRST, then passes result to g — left to right: g(f(x)).
    //    f.compose(g) applies g FIRST, then passes result to f — right to left: f(g(x)).
    //    Mnemonic: andThen = 'do f, THEN do g'. compose = mathematical f∘g notation."
    //
    // Q6: "What are the 4 types of method references?"
    // A: "1. Static:    ClassName::staticMethod   — e.g., Integer::parseInt
    //     2. Captured:  instance::method           — e.g., myStr::concat
    //     3. Arbitrary: ClassName::instanceMethod  — e.g., String::toUpperCase
    //        (tricky: first parameter becomes 'this')
    //     4. Constructor: ClassName::new           — e.g., ArrayList::new
    //    Type 3 is the one that trips people up: String::length maps to
    //    Function<String,Integer> — the String IS the first parameter."

    // =========================================================
    // SUBJECT 6: COMPLETABLEFUTURE EXCEPTION HANDLING
    // =========================================================
    // → Full code: com.java.concurrency.advanced.CompletableFutureDeepDive
    //
    // Q1: "What happens when a stage in a CompletableFuture pipeline throws?"
    // A: "The exception propagates through the pipeline — all subsequent thenApply/
    //    thenCompose stages are SKIPPED. The CompletableFuture completes exceptionally.
    //    The exception is wrapped in a CompletionException if it wasn't already.
    //    This is equivalent to exception bubbling in a try-catch chain."
    //
    // Q2: "What is the difference between exceptionally, handle, and whenComplete?"
    //
    // A (the answer that wins the interview):
    //   "Three ways to handle exceptions in CF pipelines:
    //
    //    exceptionally(Function<Throwable, T>):
    //      Like a CATCH block — only invoked on FAILURE, passes through on success.
    //      Returns a fallback value. Transforms the exception into a value.
    //      cf.exceptionally(ex -> defaultValue)
    //
    //    handle(BiFunction<T, Throwable, U>):
    //      Like a TRY-CATCH-FINALLY — invoked on BOTH success AND failure.
    //      Receives (result, exception) — one will always be null.
    //      Can transform both the success value and the exception.
    //      if (ex != null) → handle failure; else → transform success.
    //
    //    whenComplete(BiConsumer<T, Throwable>):
    //      Like a FINALLY block — invoked on both success and failure.
    //      For SIDE EFFECTS ONLY (logging, metrics) — does NOT transform result.
    //      The pipeline result passes through unchanged.
    //
    //    Rule: use exceptionally for fallbacks, handle for conditional logic,
    //          whenComplete for logging/cleanup."
    //
    // Q3: "How do you run 3 API calls in parallel and combine results?"
    // A: "CompletableFuture.allOf() fans out, then .join() on each future collects results:
    //    var cf1 = CompletableFuture.supplyAsync(() -> fetchUser());
    //    var cf2 = CompletableFuture.supplyAsync(() -> fetchOrders());
    //    CompletableFuture.allOf(cf1, cf2).join();
    //    String user = cf1.join(); List<Order> orders = cf2.join();"
    //
    // Q4: "What is the difference between thenApply and thenCompose?"
    // A: "thenApply is like map — takes the result and applies a sync transformation.
    //    thenCompose is like flatMap — used when the transformation itself returns
    //    a CompletableFuture. Without thenCompose you'd get CF<CF<T>>; thenCompose
    //    flattens it to CF<T>. Use thenCompose when chaining async calls."

    // =========================================================
    // SUBJECT 7: CODING — 2nd Highest Repeating Character
    // =========================================================
    //
    // QUESTION: "Given a string, find the character with the 2nd highest frequency.
    //            Use streams. The interviewer wants parallel stream."
    //
    // APPROACH:
    //   Step 1: Count frequency of each character → Map<Character, Long>
    //   Step 2: Find the 2nd highest distinct frequency value
    //   Step 3: Find a character with that frequency
    //
    // KEY INTERVIEW POINTS:
    //   - s.chars() returns IntStream → need .mapToObj(c -> (char) c) to get Stream<Character>
    //   - For PARALLEL collection use groupingByConcurrent (not groupingBy)
    //     groupingBy in parallel has a sequential merge step
    //     groupingByConcurrent uses ConcurrentHashMap — fully parallel
    //   - .distinct() on values — need 2nd DISTINCT frequency, not 2nd element
    //   - ConcurrentHashMap returned by groupingByConcurrent → values() is fine to stream

    static void codingQuestion() {
        System.out.println("--- Sequential Stream Solution ---");
        System.out.println("2nd highest: " + secondHighestSequential("aabbccc"));   // a or b (freq 2)
        System.out.println("2nd highest: " + secondHighestSequential("hello"));      // h,e,o (freq 1)
        System.out.println("2nd highest: " + secondHighestSequential("aaaaabbc"));   // b (freq 2)

        System.out.println("\n--- Parallel Stream Solution (what interviewer wanted) ---");
        System.out.println("2nd highest: " + secondHighestParallel("aabbccc"));
        System.out.println("2nd highest: " + secondHighestParallel("hello"));
        System.out.println("2nd highest: " + secondHighestParallel("aaaaabbc"));

        System.out.println("\n--- All chars with 2nd highest frequency (follow-up) ---");
        System.out.println("All 2nd highest: " + allCharsWithSecondHighest("aabbccc")); // [a, b]
    }

    // ── SOLUTION 1: Sequential Stream ──────────────────────────────────────
    //
    // Time:  O(n) for frequency count + O(k log k) for sorting distinct freqs
    //        where k = number of distinct characters (max 26 for lowercase)
    // Space: O(k) for the frequency map

    static char secondHighestSequential(String s) {
        // STEP 1: frequency map
        // s.chars() → IntStream; mapToObj(c -> (char) c) → Stream<Character>
        Map<Character, Long> freq = s.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.groupingBy(
                Function.identity(),   // group by the character itself
                Collectors.counting()  // count occurrences per character
            ));

        // STEP 2: find the 2nd highest DISTINCT frequency
        // Must use .distinct() — multiple chars may share the same frequency
        // e.g. "aabb" → {a:2, b:2} → distinct freqs = {2} → only 1 distinct → no 2nd!
        long secondHighestFreq = freq.values().stream()
            .distinct()
            .sorted(Comparator.reverseOrder()) // [3, 2, 1] descending
            .skip(1)                           // skip highest (3)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No 2nd highest frequency"));

        // STEP 3: return any character with that frequency
        return freq.entrySet().stream()
            .filter(e -> e.getValue() == secondHighestFreq)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
    }

    // ── SOLUTION 2: Parallel Stream ────────────────────────────────────────
    //
    // KEY CHANGE from sequential: groupingByConcurrent instead of groupingBy
    //
    // WHY groupingByConcurrent?
    //   groupingBy:           creates a HashMap and merges partials sequentially
    //                         In parallel: worker threads each build a partial map,
    //                         then ONE thread merges them → bottleneck at merge step
    //   groupingByConcurrent: uses ConcurrentHashMap, threads write directly to it
    //                         No merge step → truly parallel collection
    //
    // TRADEOFF: groupingByConcurrent does NOT guarantee encounter order within groups.
    //   For frequency counting this doesn't matter — we only care about counts.
    //   For ordered grouping (e.g., preserving insertion order) → use sequential.
    //
    // WHEN PARALLEL HELPS HERE:
    //   For short strings like "hello" → sequential is faster (parallel overhead > gain)
    //   For very long strings (millions of chars) → parallel gives real speedup
    //   Interviewer asking for parallel = they want you to know the API and gotchas

    static char secondHighestParallel(String s) {
        // STEP 1: Parallel frequency count
        // s.chars() is already an IntStream — chain .parallel() to parallelize it
        // groupingByConcurrent → ConcurrentHashMap (thread-safe, no merge bottleneck)
        Map<Character, Long> freq = s.chars()
            .parallel()                              // parallelize the IntStream
            .mapToObj(c -> (char) c)                 // IntStream → Stream<Character>
            .collect(Collectors.groupingByConcurrent(
                Function.identity(),
                Collectors.counting()
            ));
        // freq is a ConcurrentHashMap<Character, Long>

        // STEP 2: find 2nd highest distinct frequency (parallel sort over small set)
        // freq.values() → Collection<Long> → .parallelStream() for parallel sort
        // In practice with 26 max chars this is overkill, but shows the pattern
        long secondHighestFreq = freq.values().parallelStream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .skip(1)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No 2nd highest frequency"));

        // STEP 3: find a char with that frequency (parallel search)
        return freq.entrySet().parallelStream()
            .filter(e -> e.getValue() == secondHighestFreq)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow();
    }

    // ── FOLLOW-UP: Return ALL characters with 2nd highest frequency ─────────
    //
    // Interviewer follow-up: "What if multiple chars share the 2nd highest freq?"
    // Change findFirst() to collect all matching chars

    static List<Character> allCharsWithSecondHighest(String s) {
        Map<Character, Long> freq = s.chars()
            .parallel()
            .mapToObj(c -> (char) c)
            .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));

        long secondHighestFreq = freq.values().parallelStream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .skip(1)
            .findFirst()
            .orElseThrow();

        return freq.entrySet().parallelStream()
            .filter(e -> e.getValue() == secondHighestFreq)
            .map(Map.Entry::getKey)
            .sorted()   // sort result for deterministic output
            .collect(Collectors.toList());
    }

    // ── VARIATIONS the interviewer might ask ───────────────────────────────
    //
    // "Make it case-insensitive":
    //   Add .map(Character::toLowerCase) before groupingByConcurrent
    //
    // "Count only letters, ignore spaces/digits":
    //   Add .filter(Character::isLetter) before mapToObj
    //
    // "What if the string is empty?":
    //   freq.values() will be empty → .findFirst() returns empty Optional
    //   → orElseThrow() throws NoSuchElementException → handle at call site
    //
    // "What is the time complexity?":
    //   O(n) for frequency count (one pass through the string)
    //   O(k log k) for sorting distinct frequencies (k = distinct chars ≤ 26)
    //   Overall: O(n) since k is bounded by alphabet size
    //   Space: O(k) = O(26) = O(1) for lowercase English
    //
    // "Why parallelStream() vs .parallel()?":
    //   On a Collection: list.parallelStream() creates a parallel stream directly
    //   On an existing Stream: stream.parallel() switches the stream to parallel mode
    //   s.chars() returns IntStream → use .parallel() on it (no parallelStream() method)
    //   Collections have parallelStream(); IntStream/Stream use .parallel()
}

/*
 * ============================================================
 * QUICK REFERENCE — ALL 7 SUBJECTS ONE-LINER ANSWERS
 * ============================================================
 *
 * 1. HOW SPRING STARTS:
 *    refresh() → BeanDefinitions registered → BeanFactoryPostProcessors →
 *    instantiate → inject → @PostConstruct → BeanPostProcessors (AOP proxy here)
 *
 * 2. SPRING DEPENDENCY MECHANICS:
 *    ClassPathBeanDefinitionScanner (ASM, no classloading) → BeanDefinitions
 *    AutowiredAnnotationBeanPostProcessor (reflection, cached as InjectionMetadata)
 *    resolveDependency() → by type → @Qualifier → @Primary → name → exception
 *
 * 3. PARALLEL STREAMS:
 *    Fork/Join framework + Spliterator. Good: large data, CPU-bound, no shared state.
 *    Bad: small data, I/O, ordered ops. Use groupingByConcurrent not groupingBy.
 *    Custom pool: new ForkJoinPool(n).submit(() -> list.parallelStream()...).get()
 *    Does NOT use virtual threads.
 *
 * 4. JAVA 17 & 21:
 *    17 LTS: Sealed Classes (final). 14-16 backlog: Records, Pattern instanceof, Text blocks.
 *    21 LTS: Virtual Threads, Sequenced Collections, Record Patterns,
 *            Pattern Switch (final), Scoped Values.
 *
 * 5. FUNCTIONAL INTERFACES:
 *    Function(T→R), Consumer(T→void), Supplier(→T), Predicate(T→bool),
 *    BiFunction(T,U→R), BiConsumer(T,U→void), UnaryOperator(T→T), BinaryOperator(T,T→T)
 *    andThen = f then g; compose = g then f. orElseGet is LAZY; orElse is EAGER.
 *
 * 6. COMPLETABLEFUTURE EXCEPTIONS:
 *    exceptionally = catch (fallback on failure only)
 *    handle         = try-catch (both success and failure, can transform)
 *    whenComplete   = finally (side effects only, doesn't change result)
 *    Exception propagates through pipeline, skipping stages, until caught.
 *
 * 7. CODING — 2nd highest repeating char:
 *    Sequential: groupingBy + counting → distinct freqs → sort desc → skip(1) → findFirst
 *    Parallel:   .parallel() + groupingByConcurrent (ConcurrentHashMap, no merge step)
 *    Follow-up:  remove findFirst(), collect to List for all chars at that frequency
 */
