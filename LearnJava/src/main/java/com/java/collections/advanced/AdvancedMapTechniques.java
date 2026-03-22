package com.java.collections.advanced;

import java.util.*;
import java.util.stream.*;

/**
 * ============================================================
 * CONCEPT: Advanced Map Techniques — Beyond HashMap
 * ============================================================
 *
 * WHY NOT JUST USE HashMap FOR EVERYTHING?
 *   HashMap: O(1) ops, no ordering, no null-key-safety in concurrent use, high memory overhead.
 *   Different Map implementations solve different problems:
 *     - Need SORTED iteration or range queries?  → TreeMap
 *     - Need INSERTION ORDER or LRU CACHE?       → LinkedHashMap
 *     - Keys are ENUM values?                    → EnumMap (array-backed, fastest)
 *     - Need OBJECT IDENTITY (==) comparison?   → IdentityHashMap
 *     - Cache that auto-shrinks under GC?        → WeakHashMap
 *
 * WHEN TO USE:
 *   - Scheduling / "find nearest event time" problems: TreeMap (NavigableMap ops)
 *   - LRU / MRU cache implementation: LinkedHashMap with accessOrder=true
 *   - State machine transitions (enum key): EnumMap
 *   - Tracking visited objects (not by .equals): IdentityHashMap
 *   - Soft/weak reference caches: WeakHashMap
 *
 * TIME / SPACE:
 *   EnumMap: O(1) all ops (array index by ordinal), O(|enum|) space
 *   TreeMap: O(log n) all ops, O(n) space (red-black tree)
 *   LinkedHashMap: O(1) all ops + O(n) for doubly-linked list overhead
 *   WeakHashMap: O(1) avg, GC can reclaim entries at any time
 *   IdentityHashMap: O(1) avg (open-addressing, uses System.identityHashCode)
 *
 * INTERVIEW ANGLE:
 *   "Implement an LRU cache" → LinkedHashMap override removeEldestEntry
 *   "Why not use HashMap with enum keys?" → EnumMap is faster and more memory-efficient
 *   "How does WeakHashMap avoid memory leaks?" → weak references, GC can reclaim
 *   "Difference between computeIfAbsent and putIfAbsent?" → lazy vs eager value creation
 *
 * REAL-WORLD USE:
 *   - LinkedHashMap LRU: Guava Cache, Caffeine cache, database query result caches
 *   - EnumMap: HTTP method dispatch tables, state machine transitions
 *   - IdentityHashMap: Java serialization (ObjectOutputStream tracks visited objects)
 *   - WeakHashMap: Class metadata caches in frameworks (Spring), interned resource maps
 * ============================================================
 */
public class AdvancedMapTechniques {

    // =========================================================
    // SECTION 1: TreeMap as NavigableMap — Range Queries
    // =========================================================

    /**
     * KEY INSIGHT: TreeMap implements NavigableMap which adds powerful "nearest key" queries.
     * Internally a red-black tree — all ops O(log n), keys always sorted.
     *
     * NavigableMap API:
     *   floorKey(k)   : largest key <= k  (floor = ≤)
     *   ceilingKey(k) : smallest key >= k (ceiling = ≥)
     *   lowerKey(k)   : largest key < k   (strictly less)
     *   higherKey(k)  : smallest key > k  (strictly greater)
     *
     *   headMap(k)           : all keys STRICTLY less than k (exclusive)
     *   headMap(k, inclusive): all keys <= k if inclusive=true
     *   tailMap(k)           : all keys >= k (inclusive by default)
     *   subMap(from, to)     : keys in [from, to) (from inclusive, to exclusive by default)
     *
     *   pollFirstEntry(): remove and return the entry with the smallest key (atomic)
     *   pollLastEntry() : remove and return the entry with the largest key (atomic)
     *
     * INTERVIEW use-case: "Find the event scheduled closest to but not after time T"
     *   → floorEntry(T) gives you exactly that.
     *
     * LeetCode problems using TreeMap NavigableMap ops:
     *   LC 729  My Calendar I (floorEntry to check overlap)
     *   LC 1348 Tweet Counts Per Frequency (range queries on timestamps)
     *   LC 715  Range Module (interval merging with floorEntry/ceilingEntry)
     */
    static void demoTreeMapNavigable() {
        System.out.println("\n=== TreeMap NavigableMap Operations ===");

        // Simulate a scheduling system: map from start-time → event-name
        TreeMap<Integer, String> schedule = new TreeMap<>();
        schedule.put(900,  "Meeting");
        schedule.put(1030, "Code Review");
        schedule.put(1400, "Lunch");
        schedule.put(1600, "Design Session");

        // Find event that starts at or before 1000
        Map.Entry<Integer, String> current = schedule.floorEntry(1000);
        System.out.println("At time 1000, current event: " + current); // 900=Meeting

        // Find next event after 1000
        Map.Entry<Integer, String> next = schedule.ceilingEntry(1001);
        System.out.println("Next event after 1000: " + next); // 1030=Code Review

        // Range: all events between 10:00 and 15:00
        NavigableMap<Integer, String> afternoon = schedule.subMap(1000, true, 1500, true);
        System.out.println("Events 1000-1500: " + afternoon);

        // Reverse order iteration (latest first)
        System.out.print("Descending schedule: ");
        schedule.descendingKeySet().forEach(t -> System.out.print(t + " "));
        System.out.println();

        // pollFirstEntry: remove the earliest event (atomic, useful for priority task processing)
        Map.Entry<Integer, String> first = schedule.pollFirstEntry();
        System.out.println("Processing (removed): " + first);
        System.out.println("Remaining: " + schedule);
    }

    // =========================================================
    // SECTION 2: LinkedHashMap as LRU Cache
    // =========================================================

    /**
     * KEY INSIGHT: LinkedHashMap maintains a doubly-linked list through all its entries,
     * in EITHER insertion order (default) OR access order (accessOrder=true).
     *
     * LRU CACHE in 5 lines:
     *   LinkedHashMap(capacity, 0.75f, true) with removeEldestEntry override.
     *   accessOrder=true: every get() and put() moves the entry to the TAIL of the list.
     *   The HEAD of the list = the LEAST RECENTLY USED entry.
     *   removeEldestEntry: called after each put(). Return true to auto-evict the head.
     *
     * INTERNAL STRUCTURE:
     *   Each Entry has: before (prev) and after (next) pointers in the linked list.
     *   HashMap provides O(1) lookup. LinkedList provides O(1) move-to-tail.
     *   Combined: O(1) get, O(1) put, O(1) eviction. This is the optimal LRU structure.
     *
     * vs Manual DoublyLinkedList + HashMap:
     *   LinkedHashMap LRU is 5 lines, works in interviews.
     *   Manual implementation needed for: LeetCode 146 (interviewer wants to see you implement it),
     *   thread-safe LRU, or when you need to extend with complex eviction logic.
     *
     * GOTCHA: LinkedHashMap is NOT thread-safe. For concurrent LRU, use ConcurrentLinkedHashMap
     *   (Guava's) or wrap with Collections.synchronizedMap (but then iteration needs external sync).
     *
     * INTERVIEW Q: "What's the time complexity of your LRU get/put?"
     * A: O(1) for both. HashMap lookup is O(1). Moving to tail in doubly-linked list is O(1).
     */
    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        // accessOrder=true is the KEY — makes get() update the access order
        LRUCache(int capacity) {
            super(capacity, 0.75f, true); // (initialCapacity, loadFactor, accessOrder)
            this.capacity = capacity;
        }

        // Called after every put(). Return true to remove the eldest (LRU) entry.
        // Eldest = front of the linked list = least recently accessed.
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    static void demoLRUCache() {
        System.out.println("\n=== LinkedHashMap LRU Cache Demo ===");

        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        System.out.println("Cache: " + cache); // {1=One, 2=Two, 3=Three}

        cache.get(1);       // 1 is now most recently used → moves to tail
        cache.put(4, "Four"); // capacity exceeded → evicts LRU (head = 2)
        System.out.println("After get(1) and put(4): " + cache); // {3=Three, 1=One, 4=Four}
        System.out.println("2 evicted? " + !cache.containsKey(2)); // true

        cache.put(5, "Five"); // evicts LRU (head = 3)
        System.out.println("After put(5): " + cache); // {1=One, 4=Four, 5=Five}
    }

    // =========================================================
    // SECTION 3: EnumMap — The Fastest Map for Enum Keys
    // =========================================================

    /**
     * KEY INSIGHT: EnumMap uses an array internally, indexed by enum.ordinal().
     * Every enum value has a unique ordinal (0, 1, 2, ...) assigned at compile time.
     * Array access by index = O(1) AND cache-friendly (contiguous memory).
     *
     * Why faster than HashMap<Enum, V>?
     *   HashMap: computes hashCode() → uses it to find bucket → may traverse list/tree
     *   EnumMap: uses enum.ordinal() directly as array index — no hashing, no collision
     *
     * Memory: EnumMap allocates an array of size |EnumType|, regardless of entries stored.
     *   For small enums this is fine. Never use for enums with thousands of values.
     *
     * GOTCHA: Keys CANNOT be null in EnumMap.
     * GOTCHA: EnumMap is NOT thread-safe. Use Collections.synchronizedMap() to wrap it.
     *
     * INTERVIEW Q: "You have a dispatch table mapping HTTP methods to handlers. What Map type?"
     * A: EnumMap<HttpMethod, Handler> — O(1) lookup, no boxing, cache-friendly.
     */
    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    static void demoEnumMap() {
        System.out.println("\n=== EnumMap Demo ===");

        EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
        schedule.put(Day.MON, "Sprint Planning");
        schedule.put(Day.WED, "Standup");
        schedule.put(Day.FRI, "Retrospective");

        // Iteration is in enum declaration order (not insertion order)
        schedule.forEach((day, event) -> System.out.println(day + ": " + event));

        // EnumSet for a set of enum values — even faster than EnumMap for set operations
        EnumSet<Day> weekdays = EnumSet.range(Day.MON, Day.FRI);
        EnumSet<Day> weekend  = EnumSet.of(Day.SAT, Day.SUN);
        System.out.println("Weekdays: " + weekdays);
        System.out.println("Weekend: " + weekend);
    }

    // =========================================================
    // SECTION 4: IdentityHashMap — Reference Equality
    // =========================================================

    /**
     * KEY INSIGHT: IdentityHashMap uses System.identityHashCode() and == for key comparison.
     * Normal HashMap uses .hashCode() and .equals(). This is a critical difference.
     *
     * WHEN TO USE:
     *   - Object identity tracking (have I seen this exact object, not an equal one?)
     *   - Preventing cycles in graph traversal when nodes might be .equals() but different objects
     *   - Serialization frameworks: tracking which objects have been serialized (ObjectOutputStream)
     *   - Proxy/AOP frameworks: mapping original objects to their proxies
     *
     * INTERNAL: Uses open addressing (linear probing) with identity hash.
     *   Unlike HashMap which uses chained buckets.
     *
     * GOTCHA: String literals are interned — "hello" == "hello" is true (same object).
     *   Use new String("hello") to create distinct objects with equal content.
     *
     * INTERVIEW Q: "When does a.equals(b) but you'd want IdentityHashMap to treat them as different keys?"
     * A: When tracking two different Integer objects that happen to have the same value,
     *    or when you're managing object lifecycle separately per instance.
     */
    static void demoIdentityHashMap() {
        System.out.println("\n=== IdentityHashMap Demo ===");

        String s1 = new String("hello");
        String s2 = new String("hello"); // different object, same content

        // Regular HashMap: s1.equals(s2) → same key
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put(s1, 1);
        hashMap.put(s2, 2); // overwrites s1's entry
        System.out.println("HashMap size: " + hashMap.size()); // 1 (same logical key)

        // IdentityHashMap: s1 != s2 → different keys
        Map<String, Integer> identityMap = new IdentityHashMap<>();
        identityMap.put(s1, 1);
        identityMap.put(s2, 2); // different entry
        System.out.println("IdentityHashMap size: " + identityMap.size()); // 2 (different objects)
        System.out.println("get(s1): " + identityMap.get(s1)); // 1
        System.out.println("get(s2): " + identityMap.get(s2)); // 2
    }

    // =========================================================
    // SECTION 5: WeakHashMap — GC-friendly Cache
    // =========================================================

    /**
     * KEY INSIGHT: WeakHashMap holds keys via WeakReference. When the key object has NO
     * other strong references, the GC may reclaim it. The map entry is then removed.
     *
     * STRONG reference: normal variable reference (Object o = new Object())
     * WEAK reference: GC can collect if no strong reference (WeakReference<Object> wr = ...)
     * SOFT reference: GC collects only under memory pressure (SoftReference<Object> sr = ...)
     *
     * WeakHashMap is NOT a full cache (use SoftReference for that). It's for:
     *   - Metadata associated with objects whose lifecycle you don't control
     *   - Caches where entries should disappear when the subject object is GC'd
     *
     * GOTCHA: String literals are NOT GC'd (they're in the string pool, always strongly reachable).
     *   To test WeakHashMap behavior, use new String("key") — heap-allocated, GC-able.
     *
     * GOTCHA: Entries can disappear at any time (between containsKey and get calls).
     *   Never assume an entry you just checked still exists.
     *
     * GOTCHA: Values are strongly referenced. If a value holds a reference back to its key,
     *   the key is never GC'd → memory leak! Use WeakReference for values too in that case.
     *
     * REAL-WORLD: ThreadLocal internally uses a WeakHashMap-like structure (ThreadLocalMap
     *   with WeakReference keys) to allow GC of ThreadLocal instances.
     */
    static void demoWeakHashMap() {
        System.out.println("\n=== WeakHashMap Demo ===");

        WeakHashMap<String, String> weakMap = new WeakHashMap<>();

        String key1 = new String("key1"); // heap-allocated, can be GC'd
        String key2 = new String("key2");
        weakMap.put(key1, "value1");
        weakMap.put(key2, "value2");

        System.out.println("Before nulling references, size: " + weakMap.size()); // 2

        key1 = null; // remove strong reference to key1
        key2 = null; // remove strong reference to key2

        // Request GC (no guarantee, but usually works in demos)
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        System.out.println("After GC (keys unreferenced), size: " + weakMap.size()); // likely 0
        System.out.println("(Size may vary — GC is not deterministic)");

        // Compare with string literal (NOT GC'd):
        WeakHashMap<String, String> weakMap2 = new WeakHashMap<>();
        weakMap2.put("literal", "value"); // "literal" is interned, never GC'd
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("Literal key entry still present: " + weakMap2.containsKey("literal")); // true
    }

    // =========================================================
    // SECTION 6: computeIfAbsent vs putIfAbsent vs merge vs getOrDefault
    // =========================================================

    /**
     * KEY INSIGHT: These four operations cover all "upsert" patterns. Choosing the right one
     * makes code cleaner AND more efficient.
     *
     * getOrDefault(k, default): READ-ONLY fallback. Does NOT modify the map.
     *   Use when: you want a value for display/computation but don't need to store a default.
     *
     * putIfAbsent(k, v): EAGERLY evaluates v, only inserts if key absent.
     *   v is created/computed BEFORE the method call, even if the key is present (wasteful).
     *   Use when: v is a constant or very cheap to create.
     *   GOTCHA: new ArrayList() inside putIfAbsent is always allocated regardless of key presence.
     *
     * computeIfAbsent(k, fn): LAZILY evaluates fn(k) only if key absent.
     *   fn is only called when the key is missing. No waste.
     *   Use when: value is expensive to create (new ArrayList, DB call, etc.)
     *   Perfect for: building multi-maps (Map<K, List<V>>)
     *
     * merge(k, v, fn): if absent → stores v; if present → stores fn(existing, v).
     *   Perfect for: aggregation (counting, summing, concatenating)
     *   fn receives (currentValue, newValue) and returns the merged result.
     *
     * compute(k, fn): fn receives (k, currentValue) — currentValue may be null if absent.
     *   Most general form. Use when value depends on both key and current value.
     *   If fn returns null, the entry is removed.
     */
    static void demoMapOps() {
        System.out.println("\n=== Map Atomic Operations Demo ===");

        Map<String, List<String>> multiMap = new HashMap<>();

        // WRONG: putIfAbsent with new ArrayList() — always allocates!
        // multiMap.putIfAbsent("fruits", new ArrayList<>()).add("apple"); // allocates even if key exists

        // RIGHT: computeIfAbsent — lazy, only creates list if key absent
        multiMap.computeIfAbsent("fruits", k -> new ArrayList<>()).add("apple");
        multiMap.computeIfAbsent("fruits", k -> new ArrayList<>()).add("banana"); // reuses existing list
        multiMap.computeIfAbsent("vegs",   k -> new ArrayList<>()).add("carrot");
        System.out.println("MultiMap: " + multiMap);

        // merge for frequency counting
        Map<String, Integer> freq = new HashMap<>();
        String[] words = {"a", "b", "a", "c", "b", "a"};
        for (String w : words) {
            freq.merge(w, 1, Integer::sum); // if absent → 1; if present → existing + 1
        }
        System.out.println("Frequencies: " + freq); // {a=3, b=2, c=1}

        // getOrDefault: does NOT insert
        int count = freq.getOrDefault("z", 0);
        System.out.println("getOrDefault('z'): " + count); // 0
        System.out.println("'z' in map? " + freq.containsKey("z")); // false

        // compute: remove entry if result is null
        freq.compute("c", (k, v) -> v == null ? null : v - 1); // decrement c
        System.out.println("After decrement c (0): " + freq.get("c")); // null (removed if 0)
    }

    // =========================================================
    // SECTION 7: Multimap Simulation with groupingBy
    // =========================================================

    /**
     * KEY INSIGHT: Java has no built-in Multimap (unlike Guava). Two idiomatic approaches:
     *
     * 1. Manual Map<K, List<V>> with computeIfAbsent:
     *    Good for: mutable multi-maps built incrementally.
     *
     * 2. Stream.collect(Collectors.groupingBy(...)):
     *    Good for: one-shot transformation of a collection into a grouped map.
     *    Returns Map<K, List<V>> by default.
     *    Can compose: groupingBy + counting(), summingInt(), mapping(), etc.
     *
     * INTERVIEW Q: "Group employees by department, counting each department's headcount."
     * A: employees.stream().collect(Collectors.groupingBy(Employee::dept, Collectors.counting()))
     */
    record Employee(String name, String dept, int salary) {}

    static void demoMultimap() {
        System.out.println("\n=== Multimap and groupingBy Demo ===");

        List<Employee> employees = List.of(
            new Employee("Alice",   "Eng", 120000),
            new Employee("Bob",     "Eng", 110000),
            new Employee("Charlie", "HR",  80000),
            new Employee("Diana",   "HR",  85000),
            new Employee("Eve",     "Eng", 130000)
        );

        // Manual multimap
        Map<String, List<Employee>> byDeptManual = new HashMap<>();
        for (Employee e : employees) {
            byDeptManual.computeIfAbsent(e.dept(), k -> new ArrayList<>()).add(e);
        }
        System.out.println("Manual multimap Eng count: " + byDeptManual.get("Eng").size()); // 3

        // Stream groupingBy — same result, more concise
        Map<String, List<Employee>> byDept = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept));
        System.out.println("groupingBy Eng count: " + byDept.get("Eng").size()); // 3

        // groupingBy + downstream: average salary per department
        Map<String, Double> avgSalary = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.averagingInt(Employee::salary)));
        System.out.println("Average salary by dept: " + avgSalary);

        // groupingBy + counting
        Map<String, Long> headcount = employees.stream()
            .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        System.out.println("Headcount by dept: " + headcount);

        // Partition into two groups: high earners vs rest
        Map<Boolean, List<Employee>> partition = employees.stream()
            .collect(Collectors.partitioningBy(e -> e.salary() >= 110000));
        System.out.println("High earners: " + partition.get(true).stream().map(Employee::name).toList());
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Advanced Map Techniques                    ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        demoTreeMapNavigable();
        demoLRUCache();
        demoEnumMap();
        demoIdentityHashMap();
        demoWeakHashMap();
        demoMapOps();
        demoMultimap();

        System.out.println("\n=== All demos complete ===");
    }
}
