package com.java.generics;

/**
 * CONCEPT: Generic Algorithms and Data Structures
 *
 * CORE IDEA: Apply generics to build reusable, type-safe algorithms that work
 * for any type satisfying the bounds. These are the patterns used in the JDK
 * Collections framework and many enterprise libraries.
 *
 * WHEN TO USE:
 *   - Building utilities that should work for String, Integer, custom domain objects
 *   - Abstracting patterns like "either success or failure" generically (Result<T>)
 *   - Caching pure functions (memoize)
 *   - Event-driven systems (EventBus)
 *
 * TIME/SPACE COMPLEXITIES: See individual algorithm javadoc.
 *
 * INTERVIEW ANGLE:
 *   Q: How would you implement a generic binary search?
 *   A: <T extends Comparable<T>> with recursive bound lets you call compareTo.
 *      Return -1 for not found (like Arrays.binarySearch) or Optional<Integer> for clarity.
 *
 *   Q: What is the Either/Result pattern and why use it?
 *   A: Instead of throwing exceptions for expected failures (validation, parsing),
 *      return Result<T> = Success<T> | Failure<T>. The caller is FORCED to handle
 *      both cases at compile time. No accidental swallowing of exceptions.
 *      Used in: Vavr (Either), Rust (Result), Haskell (Either), Kotlin (Result).
 *
 *   Q: How does memoize work with generics?
 *   A: Function<T,R> wraps the original function. We store a HashMap<T,R>.
 *      On each call: check cache first, call original only on miss.
 *      CRITICAL: only memoize PURE functions (same input → same output, no side effects).
 *
 * REAL-WORLD USE:
 *   - Generic Pair<A,B>: HTTP client (response code + body), database row processing
 *   - Result<T>: service layer return type, replacing checked exceptions
 *   - Memoize: expensive computations (Fibonacci, factorial, DB queries)
 *   - LRUCache: CDN edge cache, DNS resolver cache, compiled template cache
 *   - EventBus: UI frameworks, microservice internal events, plugin systems
 *
 * GOTCHA:
 *   - Binary search requires the list to be SORTED. Calling on unsorted list = wrong answer, no error.
 *   - Memoize only works for pure functions. Memoizing System.currentTimeMillis() → always returns first call's value.
 *   - LRUCache with HashMap: iteration order of LinkedHashMap is insertion order by default.
 *     Pass accessOrder=true to the constructor to make it LRU.
 *   - EventBus: subscribers are stored by Class<T> key. If event extends multiple types,
 *     only exact class matches — not polymorphic by default (design choice).
 */

import java.util.*;
import java.util.function.*;

public class GenericAlgorithms {

    // =========================================================================
    // SECTION 1: Generic Binary Search
    // =========================================================================

    /**
     * CONCEPT: Binary search on a sorted list using generic bounds.
     *
     * ALGORITHM:
     *   Divide the search space in half each iteration.
     *   If target < mid → search left half.
     *   If target > mid → search right half.
     *   If target == mid → found!
     *
     * COMPLEXITY:
     *   Time:  O(log n) — halving each step: log2(1,000,000) ≈ 20 comparisons
     *   Space: O(1) iterative (O(log n) recursive due to call stack)
     *
     * GOTCHA: List must be SORTED by natural order (compareTo). If not sorted,
     *         results are UNDEFINED (may return wrong index or -1).
     *
     * INTERVIEW: "Why can't binary search work on LinkedList?"
     *   A: get(mid) on LinkedList is O(n) — must traverse from head.
     *      Total complexity becomes O(n log n). Use only with RandomAccess lists (ArrayList).
     *
     * BOUND: <T extends Comparable<T>>
     *   Requires T to implement Comparable<T> — has compareTo(T other).
     *   Works for: Integer, String, Double, LocalDate, any custom Comparable.
     *
     * @param sorted a list sorted in ascending natural order
     * @param target the element to search for
     * @return index if found, -1 if not found
     */
    public static <T extends Comparable<T>> int binarySearch(List<T> sorted, T target) {
        int lo = 0, hi = sorted.size() - 1;
        while (lo <= hi) {
            // KEY INSIGHT: mid = lo + (hi - lo) / 2 prevents integer overflow.
            // Bad: (lo + hi) / 2 can overflow if lo + hi > Integer.MAX_VALUE.
            int mid = lo + (hi - lo) / 2;
            int cmp = sorted.get(mid).compareTo(target);
            if (cmp == 0)      return mid;   // found at mid
            else if (cmp < 0)  lo = mid + 1; // mid is less than target → search right
            else               hi = mid - 1; // mid is greater than target → search left
        }
        return -1; // not found
    }

    // =========================================================================
    // SECTION 2: Generic Merge Sort
    // =========================================================================

    /**
     * CONCEPT: Merge sort using generics — the classic divide-and-conquer sort.
     *
     * WHY MERGE SORT FOR GENERICS:
     *   - Stable sort (equal elements maintain original order) — important for objects
     *   - O(n log n) worst case (quicksort is O(n²) worst case)
     *   - Java's Arrays.sort() uses a hybrid (Timsort) based on mergesort for objects
     *
     * ALGORITHM:
     *   1. Base case: list of 0 or 1 element is already sorted
     *   2. Split list in half
     *   3. Recursively sort each half
     *   4. Merge the two sorted halves
     *
     * COMPLEXITY:
     *   Time:  O(n log n) — log n levels, each O(n) merge = O(n log n)
     *   Space: O(n) auxiliary — merge creates temporary lists
     *
     * GOTCHA: This implementation creates new lists at each merge step.
     *         In-place merge sort exists but is complex (used in Timsort for memory efficiency).
     */
    public static <T extends Comparable<T>> List<T> mergeSort(List<T> list) {
        if (list.size() <= 1) return new ArrayList<>(list); // base case

        int mid = list.size() / 2;
        List<T> left  = mergeSort(list.subList(0, mid));
        List<T> right = mergeSort(list.subList(mid, list.size()));
        return merge(left, right);
    }

    // Merge two sorted lists into one sorted list
    // Time: O(n+m) where n,m are sizes of left,right
    private static <T extends Comparable<T>> List<T> merge(List<T> left, List<T> right) {
        List<T> result = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            // ≤ 0 for STABILITY: equal elements from left come first (maintains original order)
            if (left.get(i).compareTo(right.get(j)) <= 0) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        // Append remaining elements (one of these loops runs 0 times)
        while (i < left.size())  result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        return result;
    }

    // =========================================================================
    // SECTION 3: Generic Pair<A, B>
    // =========================================================================

    /**
     * CONCEPT: A tuple of two values of potentially different types.
     * Immutable by design — creation is the only mutation point.
     *
     * USE CASES:
     *   - Return two values from a method (without creating a one-off class)
     *   - Represent a key-value pair
     *   - Coordinate: Pair<Integer, Integer>
     *   - HTTP response: Pair<Integer, String> (status code, body)
     *
     * DESIGN NOTE: Java 16+ Records are better for named pairs:
     *   record Point(int x, int y) {}
     *   But generic Pair<A,B> is useful when field names don't matter.
     *
     * INTERVIEW: "Why not use Map.Entry<K,V>?"
     *   A: Map.Entry is tightly coupled to Map semantics. Pair is standalone.
     *      But Map.Entry.of(k, v) is available in Java 9+ for simple cases.
     *
     * @param <A> type of first element
     * @param <B> type of second element
     */
    public static final class Pair<A, B> {
        private final A first;
        private final B second;

        public Pair(A first, B second) {
            this.first  = Objects.requireNonNull(first,  "first must not be null");
            this.second = Objects.requireNonNull(second, "second must not be null");
        }

        public static <A, B> Pair<A, B> of(A first, B second) {
            return new Pair<>(first, second);
        }

        public A getFirst()  { return first;  }
        public B getSecond() { return second; }

        // map: transform both elements, potentially changing types
        // Returns a new Pair<C,D> where C and D may differ from A and B
        public <C, D> Pair<C, D> map(Function<A, C> mapFirst, Function<B, D> mapSecond) {
            return new Pair<>(mapFirst.apply(first), mapSecond.apply(second));
        }

        // swap: returns Pair<B, A> — note the type flip
        public Pair<B, A> swap() {
            return new Pair<>(second, first);
        }

        // toEntry: convert to java.util.Map.Entry for use with Maps
        public Map.Entry<A, B> toEntry() {
            return Map.entry(first, second);
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair<?, ?> p)) return false;
            return Objects.equals(first, p.first) && Objects.equals(second, p.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }

    // =========================================================================
    // SECTION 4: Generic Result<T> — Either Pattern
    // =========================================================================

    /**
     * CONCEPT: Result<T> = Success<T>(value) | Failure<T>(exception)
     *
     * MOTIVATION: Checked exceptions force callers to handle them but are verbose.
     *   Unchecked exceptions can be swallowed accidentally.
     *   Result<T> makes failure an explicit part of the return type:
     *     - Caller CANNOT ignore failure (unless they explicitly call .get() without checking)
     *     - Failures can be chained with map/flatMap (like Optional)
     *     - No stack unwinding for expected failures (performance win)
     *
     * USED IN:
     *   - Vavr: Either<L, R> and Try<T>
     *   - Rust: Result<T, E>
     *   - Haskell: Either a b
     *   - Kotlin: runCatching { } → Result<T>
     *
     * SEALED INTERFACE + RECORDS (Java 17+):
     *   The sealed + permits pattern ensures exhaustive matching in switch expressions.
     *
     * COMPLEXITY:
     *   Time/Space: O(1) for construction and all operations
     *
     * INTERVIEW: "How is Result<T> different from Optional<T>?"
     *   A: Optional represents presence/absence of a value (no context on absence).
     *      Result represents success/failure WITH the failure reason (exception/message).
     *      Optional.empty() gives no info. Failure<T>(e) tells you WHY it failed.
     */
    public sealed interface Result<T> permits GenericAlgorithms.Success, GenericAlgorithms.Failure {
        boolean isSuccess();
        T getValue();           // throws if Failure
        Exception getError();   // throws if Success
        <U> Result<U> map(Function<T, U> mapper);
        <U> Result<U> flatMap(Function<T, Result<U>> mapper);

        // Factory methods
        static <T> Result<T> success(T value)        { return new Success<>(value); }
        static <T> Result<T> failure(Exception error) { return new Failure<>(error); }

        // Try: wrap a computation that might throw
        static <T> Result<T> tryGet(java.util.concurrent.Callable<T> callable) {
            try {
                return success(callable.call());
            } catch (Exception e) {
                return failure(e);
            }
        }
    }

    // SUCCESS: holds the value
    public record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess()        { return true; }
        @Override public T getValue()               { return value; }
        @Override public Exception getError()       { throw new NoSuchElementException("No error in Success"); }
        @Override public <U> Result<U> map(Function<T, U> f)          { return Result.tryGet(() -> f.apply(value)); }
        @Override public <U> Result<U> flatMap(Function<T, Result<U>> f) { return f.apply(value); }
        @Override public String toString()          { return "Success(" + value + ")"; }
    }

    // FAILURE: holds the exception
    public record Failure<T>(Exception error) implements Result<T> {
        @Override public boolean isSuccess()        { return false; }
        @Override public T getValue()               { throw new NoSuchElementException("No value in Failure: " + error); }
        @Override public Exception getError()       { return error; }
        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> map(Function<T, U> f) { return (Result<U>) this; } // propagate failure
        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U> flatMap(Function<T, Result<U>> f) { return (Result<U>) this; }
        @Override public String toString()          { return "Failure(" + error.getMessage() + ")"; }
    }

    // =========================================================================
    // SECTION 5: Generic Memoize
    // =========================================================================

    /**
     * CONCEPT: Cache the results of a pure function so repeated calls with
     * the same input don't recompute — just return the cached result.
     *
     * PURE FUNCTION REQUIREMENT:
     *   f(x) must ALWAYS return the same value for the same x.
     *   No side effects. No dependency on external state.
     *   WRONG to memoize: random(), currentTimeMillis(), database queries (mutable state)
     *   CORRECT to memoize: factorial(n), fibonacci(n), hashOf(string), parseDate(str)
     *
     * COMPLEXITY:
     *   Time:  O(1) on cache hit, O(f(x)) on cache miss
     *   Space: O(k) where k = number of distinct inputs seen
     *
     * GOTCHA: The returned Function is NOT thread-safe (HashMap not synchronized).
     *         For concurrent use: wrap in Collections.synchronizedMap or use ConcurrentHashMap.
     *
     * INTERVIEW: "What's the difference between memoize and cache?"
     *   A: Memoize is function-level caching (specific to one function, usually in-process).
     *      Cache is more general (can be distributed, time-based expiry, size-limited).
     *      Memoize is a specialization of caching for pure functions.
     */
    public static <T, R> Function<T, R> memoize(Function<T, R> fn) {
        Map<T, R> cache = new HashMap<>();  // KEY INSIGHT: captured by the lambda closure
        return input -> {
            // computeIfAbsent: atomic check-then-compute (but not thread-safe on HashMap)
            return cache.computeIfAbsent(input, fn);
            // Equivalent to:
            // if (!cache.containsKey(input)) { cache.put(input, fn.apply(input)); }
            // return cache.get(input);
        };
    }

    // Thread-safe version using ConcurrentHashMap
    public static <T, R> Function<T, R> memoizeConcurrent(Function<T, R> fn) {
        Map<T, R> cache = new java.util.concurrent.ConcurrentHashMap<>();
        return input -> cache.computeIfAbsent(input, fn);
        // GOTCHA: ConcurrentHashMap.computeIfAbsent is atomic per key,
        // but fn might be called multiple times under high concurrency.
        // For exact-once semantics, use a lock or Guava's Cache.
    }

    // =========================================================================
    // SECTION 6: Generic LRU Cache
    // =========================================================================

    /**
     * CONCEPT: Least Recently Used cache — evicts the least recently accessed entry
     * when the cache is full.
     *
     * IMPLEMENTATION: LinkedHashMap with accessOrder=true.
     *   LinkedHashMap maintains insertion order by default.
     *   With accessOrder=true: every get() moves the accessed entry to the END.
     *   Entries at the START are least recently used → evict them when full.
     *
     *   Override removeEldestEntry() to define eviction policy.
     *
     * COMPLEXITY:
     *   get(): O(1) — HashMap lookup + LinkedList move
     *   put(): O(1) — HashMap insert + possibly O(1) eviction
     *   Space: O(capacity)
     *
     * ALTERNATIVES:
     *   - Guava Cache: richer API (expiry, async loading, stats)
     *   - Caffeine: high-performance, Window TinyLFU eviction
     *
     * INTERVIEW: "How would you implement LRU cache from scratch without LinkedHashMap?"
     *   A: Combine HashMap<K, Node<K,V>> + doubly linked list.
     *      HashMap for O(1) lookup. DLL for O(1) move-to-front and remove-tail.
     *      get(k): HashMap lookup → move node to front of DLL → return value
     *      put(k,v): HashMap lookup → if exists: update + move to front
     *                                → if new: add to front + if full: remove tail from DLL AND HashMap
     *
     * @param <K> key type
     * @param <V> value type
     */
    public static class LRUCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> cache;

        public LRUCache(int capacity) {
            this.capacity = capacity;
            // KEY INSIGHT: accessOrder=true makes get() move entry to end (most recently used).
            // The FIRST entry is then the least recently used.
            this.cache = new LinkedHashMap<>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    // Evict when over capacity. Java calls this AFTER each put().
                    return size() > LRUCache.this.capacity;
                }
            };
        }

        public V get(K key) {
            // LinkedHashMap(accessOrder=true): get() moves 'key' to end of list
            return cache.getOrDefault(key, null);
        }

        public void put(K key, V value) {
            cache.put(key, value);  // removeEldestEntry() called after this if over capacity
        }

        public int size()            { return cache.size(); }
        public boolean containsKey(K key) { return cache.containsKey(key); }

        @Override
        public String toString() { return cache.toString(); }
    }

    // =========================================================================
    // SECTION 7: Generic EventBus
    // =========================================================================

    /**
     * CONCEPT: Publish-subscribe event system where subscribers register for
     * specific event types and publishers emit events without knowing who listens.
     *
     * DESIGN:
     *   Map<Class<T>, List<Consumer<T>>> — key is event type, value is list of handlers.
     *   publish(event): find all handlers for event.getClass(), call each.
     *   subscribe(Class<T>, Consumer<T>): add handler to the list for that type.
     *
     * GENERICS INSIGHT:
     *   The Class<T> key is a TYPE TOKEN that maps to Consumer<T> handlers.
     *   The unchecked cast is safe because subscribe(Class<T>, Consumer<T>) guarantees
     *   that only Consumer<T> is stored for Class<T> key.
     *
     * COMPLEXITY:
     *   subscribe: O(1) amortized
     *   publish:   O(h) where h = number of handlers for that event type
     *
     * REAL-WORLD: Guava EventBus, Spring ApplicationEventPublisher, Vert.x EventBus
     *
     * GOTCHA: This implementation does NOT support polymorphic dispatch.
     *   If Dog extends Animal and you subscribe to Animal.class,
     *   publishing a Dog event will NOT trigger Animal.class handlers.
     *   For polymorphic dispatch, traverse the class hierarchy.
     */
    public static class EventBus {
        // Raw type Map needed because handlers have different T types per key
        // The unchecked cast is safe by construction (see subscribe)
        private final Map<Class<?>, List<Consumer<?>>> handlers = new HashMap<>();

        // Subscribe: register a handler for events of type eventType
        public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        }

        // Publish: dispatch event to all handlers registered for its type
        @SuppressWarnings("unchecked")
        public <T> void publish(T event) {
            List<Consumer<?>> eventHandlers = handlers.get(event.getClass());
            if (eventHandlers != null) {
                for (Consumer<?> handler : eventHandlers) {
                    // SAFE UNCHECKED: subscribe(Class<T>, Consumer<T>) guarantees
                    // only Consumer<T> is stored for event.getClass() == Class<T>
                    ((Consumer<T>) handler).accept(event);
                }
            }
        }

        public int handlerCount(Class<?> eventType) {
            return handlers.getOrDefault(eventType, Collections.emptyList()).size();
        }
    }

    // =========================================================================
    // MAIN
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Generic Algorithms Demo ===\n");

        // Binary Search
        System.out.println("--- Binary Search ---");
        List<Integer> sorted = List.of(1, 3, 5, 7, 9, 11, 13, 15, 17, 19);
        System.out.println("  Sorted: " + sorted);
        System.out.println("  Search 7:  index=" + binarySearch(sorted, 7));   // 3
        System.out.println("  Search 1:  index=" + binarySearch(sorted, 1));   // 0
        System.out.println("  Search 19: index=" + binarySearch(sorted, 19));  // 9
        System.out.println("  Search 6:  index=" + binarySearch(sorted, 6));   // -1 (not found)

        List<String> strings = List.of("apple", "banana", "cherry", "date", "elderberry");
        System.out.println("  Search 'cherry': index=" + binarySearch(strings, "cherry")); // 2

        // Merge Sort
        System.out.println("\n--- Merge Sort ---");
        List<Integer> unsorted = List.of(64, 34, 25, 12, 22, 11, 90);
        System.out.println("  Before: " + unsorted);
        System.out.println("  After:  " + mergeSort(unsorted));
        List<String> unsortedStr = List.of("banana", "apple", "cherry", "date");
        System.out.println("  Strings: " + mergeSort(unsortedStr));

        // Pair
        System.out.println("\n--- Pair<A,B> ---");
        Pair<String, Integer> pair = Pair.of("hello", 42);
        System.out.println("  Pair: " + pair);
        System.out.println("  Swapped: " + pair.swap());
        Pair<Integer, String> mapped = pair.map(String::length, Object::toString);
        System.out.println("  Mapped (length, toString): " + mapped);
        System.out.println("  As Map.Entry: " + pair.toEntry());

        // Result<T>
        System.out.println("\n--- Result<T> ---");
        Result<Integer> ok     = Result.tryGet(() -> Integer.parseInt("42"));
        Result<Integer> failed = Result.tryGet(() -> Integer.parseInt("not-a-number"));
        System.out.println("  Parse '42':            " + ok);
        System.out.println("  Parse 'not-a-number':  " + failed);
        System.out.println("  ok.isSuccess: " + ok.isSuccess());
        System.out.println("  failed.isSuccess: " + failed.isSuccess());

        // Chain: parse → double
        Result<Integer> doubled = ok.map(n -> n * 2);
        System.out.println("  42 doubled: " + doubled);

        // flatMap chain: parse → divide (might fail)
        Result<Integer> divResult = ok.flatMap(n ->
            Result.tryGet(() -> n / 0));  // division by zero!
        System.out.println("  42 / 0: " + divResult);

        // Failure propagates through map/flatMap
        Result<Integer> stillFailed = failed.map(n -> n * 2);
        System.out.println("  Failure.map: " + stillFailed);  // still Failure

        // Memoize
        System.out.println("\n--- Memoize ---");
        int[] callCount = {0};
        Function<Integer, Long> expensive = n -> {
            callCount[0]++;
            // Simulate expensive computation (e.g., Fibonacci)
            long result = 1;
            for (int i = 1; i <= n; i++) result *= i; // factorial
            return result;
        };
        Function<Integer, Long> memoized = memoize(expensive);
        System.out.println("  10! = " + memoized.apply(10) + " (calls: " + callCount[0] + ")");
        System.out.println("  10! = " + memoized.apply(10) + " (calls: " + callCount[0] + ") [cached]");
        System.out.println("  5!  = " + memoized.apply(5)  + " (calls: " + callCount[0] + ")");
        System.out.println("  10! = " + memoized.apply(10) + " (calls: " + callCount[0] + ") [still cached]");

        // LRU Cache
        System.out.println("\n--- LRUCache<K,V> ---");
        LRUCache<String, Integer> lru = new LRUCache<>(3);
        lru.put("a", 1); lru.put("b", 2); lru.put("c", 3);
        System.out.println("  After a,b,c: " + lru);
        lru.get("a");           // access "a" → moves "a" to recently used
        lru.put("d", 4);        // evicts LRU = "b" (a was accessed, c is older)
        System.out.println("  After access 'a' then put 'd': " + lru);
        System.out.println("  Contains 'b': " + lru.containsKey("b")); // evicted → false
        System.out.println("  Contains 'a': " + lru.containsKey("a")); // still in → true

        // EventBus
        System.out.println("\n--- EventBus ---");
        record UserCreated(String username) {}
        record OrderPlaced(int orderId, double amount) {}

        EventBus bus = new EventBus();

        // Subscribe
        bus.subscribe(UserCreated.class, e -> System.out.println("  [Handler1] User created: " + e.username()));
        bus.subscribe(UserCreated.class, e -> System.out.println("  [Handler2] Send welcome email to: " + e.username()));
        bus.subscribe(OrderPlaced.class, e -> System.out.printf("  [Handler3] Order #%d placed for $%.2f%n", e.orderId(), e.amount()));

        System.out.println("  Handlers for UserCreated: " + bus.handlerCount(UserCreated.class));

        // Publish
        bus.publish(new UserCreated("alice"));
        bus.publish(new OrderPlaced(1001, 59.99));
        bus.publish(new UserCreated("bob"));

        System.out.println("\n=== INTERVIEW SUMMARY ===");
        System.out.println("Binary search: <T extends Comparable<T>>, O(log n), list must be sorted");
        System.out.println("Merge sort: stable, O(n log n), uses generics + Comparable");
        System.out.println("Pair<A,B>: immutable generic tuple with map/swap/toEntry");
        System.out.println("Result<T>: sealed Success|Failure, map/flatMap chain, no exception spaghetti");
        System.out.println("Memoize: closure over HashMap<T,R>, pure functions only");
        System.out.println("LRUCache: LinkedHashMap(accessOrder=true) + removeEldestEntry override");
        System.out.println("EventBus: Map<Class<?>, List<Consumer<?>>> with type-safe subscribe/publish");
    }
}
