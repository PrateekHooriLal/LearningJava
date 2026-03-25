package com.java.collections.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ============================================================
 * HASHMAP & CONCURRENTHASHMAP INTERNALS
 * ============================================================
 *
 * CONCEPT:
 *   HashMap is the most frequently used data structure in Java.
 *   Senior interviews don't ask "how do you use HashMap" — they ask
 *   "what happens INSIDE HashMap when you put a key?" and
 *   "why can't you use HashMap in a multithreaded environment?"
 *
 * HASHMAP INTERNAL STRUCTURE (Java 8+):
 *   - An array of Node<K,V>[] called `table` (the bucket array)
 *   - Default initial capacity: 16 buckets
 *   - Each bucket holds a LINKED LIST of entries (for collisions)
 *   - When a bucket's list grows to 8 entries AND table size >= 64:
 *     the list is converted to a RED-BLACK TREE (treeify)
 *   - When tree shrinks back to 6: converted back to linked list (untreeify)
 *   - Load factor: 0.75 — when 75% of buckets are occupied, RESIZE (double)
 *
 * PUT OPERATION — step by step:
 *   1. Compute hash: hash = key.hashCode() ^ (key.hashCode() >>> 16)
 *      (XOR with upper 16 bits — spreads entropy, reduces collisions)
 *   2. Find bucket: index = hash & (capacity - 1)
 *      (bitwise AND instead of modulo — works because capacity is always power of 2)
 *   3. If bucket empty: insert as first node
 *   4. If bucket has entries: walk the list/tree checking equals()
 *      a. If key found (equals() == true): UPDATE value
 *      b. If not found: APPEND to list / insert in tree
 *   5. If size > capacity * loadFactor: RESIZE (rehash all entries)
 *
 * THE hashCode() + equals() CONTRACT:
 *   If a.equals(b) → a.hashCode() MUST == b.hashCode()
 *   If a.hashCode() == b.hashCode() → a.equals(b) MAY be false (collision)
 *   BREAKING THIS CONTRACT = lost entries (the most common HashMap bug)
 *
 * INTERVIEW ANGLE:
 *   Q: "What happens when two keys have the same hashCode?"
 *   Q: "Why is the default load factor 0.75?"
 *   Q: "Why can't you use HashMap in multithreaded code?"
 *   Q: "How does ConcurrentHashMap work without locking the whole map?"
 *   Q: "What's the time complexity of HashMap.get()?"
 *
 * REAL-WORLD USE:
 *   HashMap: caches, frequency counts, memoization
 *   ConcurrentHashMap: shared caches, concurrent frequency counters
 *   LinkedHashMap: LRU cache (access-order mode)
 *   TreeMap: sorted iteration, range queries
 */
public class HashMapInternals {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. HOW HashMap.put() WORKS ===");
        demonstratePutMechanics();

        System.out.println("\n=== 2. HASH FUNCTION — why not just hashCode()? ===");
        demonstrateHashFunction();

        System.out.println("\n=== 3. COLLISION — linked list → red-black tree ===");
        demonstrateCollision();

        System.out.println("\n=== 4. RESIZE — load factor and rehashing ===");
        demonstrateResize();

        System.out.println("\n=== 5. hashCode() + equals() CONTRACT ===");
        demonstrateHashCodeEqualsContract();

        System.out.println("\n=== 6. WHY HashMap IS NOT THREAD-SAFE ===");
        demonstrateThreadUnsafety();

        System.out.println("\n=== 7. ConcurrentHashMap INTERNALS ===");
        demonstrateConcurrentHashMap();

        System.out.println("\n=== 8. LinkedHashMap — LRU Cache use case ===");
        demonstrateLinkedHashMap();

        System.out.println("\n=== 9. COMMON INTERVIEW TRICKS ===");
        demonstrateInterviewTricks();
    }

    // =========================================================
    // 1. HOW HashMap.put() WORKS — the full algorithm
    // =========================================================
    //
    // Simulates the exact logic inside HashMap.putVal():
    //
    //   final V putVal(int hash, K key, V value, ...) {
    //     Node<K,V>[] tab = table;
    //     int n = tab.length;
    //     int i = (n - 1) & hash;            // bucket index
    //     Node<K,V> p = tab[i];              // first node in bucket
    //     if (p == null)                     // bucket empty
    //         tab[i] = newNode(hash,key,value,null);
    //     else {
    //         // bucket has entries — walk list/tree
    //         if (p.hash == hash && (p.key == key || key.equals(p.key)))
    //             e = p;                     // key found at head
    //         else if (p instanceof TreeNode)
    //             e = ((TreeNode)p).putTreeVal(...);  // tree insert
    //         else {
    //             for (int binCount = 0; ; binCount++) {
    //                 if (binCount >= TREEIFY_THRESHOLD - 1)
    //                     treeifyBin(tab, hash);  // convert to tree
    //                 if (e.hash == hash && key.equals(e.key)) break;
    //                 p = e; e = e.next;
    //             }
    //         }
    //         if (e != null) { V old = e.value; e.value = value; return old; }
    //     }
    //     if (++size > threshold) resize();
    //   }
    //
    // TIME COMPLEXITY:
    //   Best case:  O(1) — bucket empty or key found at head
    //   Average:    O(1) — assuming good hash distribution
    //   Worst case: O(log n) — all keys in one bucket (tree) after Java 8
    //               O(n)     — before Java 8 (linked list only)
    //
    // INTERVIEW Q: "What's the time complexity of HashMap.get()?"
    // A: O(1) AVERAGE, O(log n) WORST CASE (Java 8+).
    //    Before Java 8 it was O(n) worst case (pure linked list).
    //    "O(1)" is the expected/amortized case with good hash distribution.

    static void demonstratePutMechanics() {
        Map<String, Integer> map = new HashMap<>(4); // small capacity to show behavior

        // Each put:
        // 1. hash("Alice") → spread hash → find bucket index
        // 2. bucket empty → insert as first node
        map.put("Alice", 1);
        map.put("Bob",   2);
        map.put("Carol", 3);

        // Update: same key → equals() match → value replaced
        map.put("Alice", 99); // hash("Alice") same bucket → equals() true → UPDATE
        System.out.println("  Updated Alice: " + map.get("Alice")); // 99

        // Null key: always goes to bucket 0 (hash(null) = 0)
        map.put(null, 0);
        System.out.println("  null key works: " + map.get(null)); // 0

        // Null value: perfectly valid
        map.put("Ghost", null);
        System.out.println("  null value: " + map.get("Ghost")); // null
        System.out.println("  containsKey vs get==null: "
            + map.containsKey("Ghost")   // true
            + " / " + map.containsKey("Missing")); // false

        // GOTCHA: map.get(key) == null does NOT mean key is absent!
        // It could mean the key maps to null. Always use containsKey() to check presence.
    }

    // =========================================================
    // 2. THE HASH FUNCTION — why not just key.hashCode()?
    // =========================================================
    //
    // Raw hashCode() has bad distribution for small table sizes.
    // Example: String.hashCode() for short strings often has entropy
    // concentrated in the LOWER bits. With a 16-bucket table, only
    // the last 4 bits of hashCode matter for the bucket index.
    // This causes many short strings to cluster in the same bucket.
    //
    // HashMap's spread function:
    //   static final int hash(Object key) {
    //     int h;
    //     return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    //   }
    //
    // XOR with upper 16 bits → mixes high-order bits into low-order bits
    // → better distribution across small tables
    //
    // INTERVIEW Q: "Why does HashMap XOR the hashCode with itself shifted right?"
    // A: To spread entropy from the upper 16 bits into the lower 16 bits,
    //    because for small tables only the low bits determine the bucket index.
    //    This reduces collisions without adding significant cost.

    static void demonstrateHashFunction() {
        String key = "test";
        int rawHash    = key.hashCode();
        int spreadHash = rawHash ^ (rawHash >>> 16);  // HashMap's actual spread()

        System.out.printf("  raw hashCode:    %d  (binary: %s)%n",
            rawHash, Integer.toBinaryString(rawHash));
        System.out.printf("  spread hash:     %d  (binary: %s)%n",
            spreadHash, Integer.toBinaryString(spreadHash));

        // Bucket index for capacity 16: hash & (16 - 1) = hash & 0b1111 (last 4 bits)
        int capacity = 16;
        int bucket   = spreadHash & (capacity - 1);
        System.out.println("  bucket index (cap=16): " + bucket);

        // WHY capacity must be a power of 2:
        // index = hash & (capacity - 1)  works ONLY when capacity is power of 2
        // e.g. capacity=16: 15 = 0b1111 → AND masks to low 4 bits = modulo 16
        // If capacity were 15: 14 = 0b1110 → uneven distribution, odd buckets never used
        System.out.println("  capacity is always power of 2 (16,32,64...) so & replaces %");
    }

    // =========================================================
    // 3. COLLISION — linked list → red-black tree (Java 8)
    // =========================================================
    //
    // BEFORE JAVA 8: collision = linked list. Worst case O(n) for get().
    //   All keys with same hashCode → one giant linked list → O(n) lookup.
    //   This was exploited in DoS attacks (hash flooding) — attacker crafts
    //   keys that all map to the same bucket, degrading HashMap to O(n).
    //
    // JAVA 8 FIX: Treeify threshold = 8
    //   When a bucket's linked list reaches 8 entries AND table.length >= 64:
    //   convert the bucket to a RED-BLACK TREE → O(log n) lookup in that bucket.
    //   When tree shrinks to 6 (UNTREEIFY_THRESHOLD): convert back to linked list.
    //
    // WHY 8 and not something else?
    //   Statistical analysis: with good hash distribution, probability of a bucket
    //   reaching 8 entries is ~6 millionths. Treeify is rare — it's a safety net.
    //   Cost of TreeNode is ~2x memory vs Node, so treeify only when necessary.
    //
    // INTERVIEW Q: "What's the difference between HashMap before and after Java 8?"
    // A: Java 8 added treeification of buckets at threshold 8 (TREEIFY_THRESHOLD).
    //    This changed worst-case lookup from O(n) to O(log n) and mitigated
    //    hash flooding DoS attacks.

    static void demonstrateCollision() {
        // Create keys that force collisions by overriding hashCode()
        // In real code, bad hashCode() implementations cause this
        System.out.println("  Keys with same hashCode go to same bucket:");
        System.out.println("  < 8 entries → linked list  O(n) walk");
        System.out.println("  ≥ 8 entries (table≥64) → red-black tree  O(log n)");
        System.out.println("  ≤ 6 entries after removal → back to linked list");

        // Demonstrate with a custom class with terrible hashCode (always 1)
        Map<BadHashKey, String> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(new BadHashKey(i), "value" + i);
        }
        System.out.println("  All 10 keys same hashCode, map still works: size=" + map.size());
        // Java 8+: bucket 1 now has a red-black tree with 10 nodes
        // All gets are O(log 10) ≈ O(3) instead of O(10)
    }

    // Simulates a class with degenerate hashCode — puts everything in one bucket
    static class BadHashKey {
        final int id;
        BadHashKey(int id) { this.id = id; }

        @Override public int hashCode() { return 1; }  // ALWAYS 1 — every key in bucket 1
        @Override public boolean equals(Object o) {
            return o instanceof BadHashKey bk && bk.id == this.id;
        }
    }

    // =========================================================
    // 4. RESIZE — load factor, threshold, rehashing
    // =========================================================
    //
    // LOAD FACTOR = 0.75 (default)
    //   threshold = capacity * loadFactor
    //   When size > threshold → resize (double capacity, rehash all)
    //
    //   capacity=16, loadFactor=0.75 → threshold=12
    //   When 13th entry is added → resize to capacity=32
    //
    // WHY 0.75?
    //   Trade-off between time and space:
    //   - High load factor (0.9): fewer resizes, more collisions, slower lookup
    //   - Low load factor (0.5): fewer collisions, more resizes, more memory waste
    //   0.75 is empirically optimal for most use cases.
    //
    // RESIZE IS EXPENSIVE: O(n) — must rehash every entry into new buckets.
    //   All existing entries are re-indexed: newIndex = hash & (newCapacity - 1)
    //
    // PRE-SIZING TIP:
    //   If you know you'll store N entries, initialize with:
    //   new HashMap<>(N / 0.75 + 1)   or   new HashMap<>((int)(N / 0.75) + 1)
    //   This avoids resize overhead.
    //   Guava's Maps.newHashMapWithExpectedSize(n) does this for you.
    //
    // INTERVIEW Q: "How do you avoid HashMap resizing?"
    // A: Pre-size the map: new HashMap<>(expectedSize * 4 / 3 + 1).
    //    Default capacity is 16 with load factor 0.75, so resize at 13 entries.
    //    For large maps this saves significant GC pressure and rehashing cost.

    static void demonstrateResize() {
        // Default: capacity=16, threshold=12
        // Adding 13th entry triggers resize to capacity=32
        System.out.println("  Default HashMap: capacity=16, resizes at 13 entries");
        System.out.println("  Pre-sized for 100 entries: new HashMap<>(134)");
        System.out.println("  Formula: initialCapacity = (int)(n / 0.75) + 1");

        int n = 100;
        int optimalInitialCapacity = (int)(n / 0.75) + 1;
        System.out.println("  For n=100: optimal initial capacity = " + optimalInitialCapacity);

        Map<Integer, Integer> presized = new HashMap<>(optimalInitialCapacity);
        for (int i = 0; i < n; i++) presized.put(i, i);
        System.out.println("  Added " + n + " entries with 0 resizes (pre-sized correctly)");
    }

    // =========================================================
    // 5. hashCode() + equals() CONTRACT
    // =========================================================
    //
    // THE CONTRACT (from Object.hashCode() Javadoc):
    //   1. Consistency:    a.hashCode() same result across multiple calls (same JVM run)
    //   2. Equality:       a.equals(b) == true → a.hashCode() == b.hashCode()  (MANDATORY)
    //   3. Inequality:     a.hashCode() != b.hashCode() → a.equals(b) MUST be false
    //      (converse of rule 2 — if hashes differ, objects can't be equal)
    //
    // NOTE: a.hashCode() == b.hashCode() does NOT imply a.equals(b) — this is a collision.
    //
    // BROKEN CONTRACT = lost data:
    //   If you override equals() but NOT hashCode():
    //   → Two "equal" objects may hash to different buckets
    //   → map.get(key) returns null even though key is "in" the map
    //   → map.containsKey(key) returns false for an "equal" key
    //   This is the #1 HashMap bug in Java codebases.
    //
    // INTERVIEW Q: "What happens if you override equals() but not hashCode()?"
    // A: HashMap becomes broken — logically equal objects hash to different buckets.
    //    You can put(key, value) and then get(equivalentKey) returns null
    //    because they land in different buckets and are never compared.

    record GoodKey(String name, int age) {
        // record auto-generates correct hashCode() and equals() based on all components
    }

    static class BrokenKey {
        String name; int age;
        BrokenKey(String name, int age) { this.name = name; this.age = age; }

        @Override
        public boolean equals(Object o) {
            // equals() correctly compares by content
            return o instanceof BrokenKey bk && bk.name.equals(name) && bk.age == age;
        }
        // hashCode() NOT overridden → uses Object.hashCode() (identity-based)
        // Two BrokenKey("Alice", 30) objects will have DIFFERENT hashCodes!
    }

    static void demonstrateHashCodeEqualsContract() {
        // GOOD: record — hashCode and equals both use name+age
        GoodKey k1 = new GoodKey("Alice", 30);
        GoodKey k2 = new GoodKey("Alice", 30);
        Map<GoodKey, String> goodMap = new HashMap<>();
        goodMap.put(k1, "found");
        System.out.println("  Good key (record):");
        System.out.println("    k1.equals(k2): " + k1.equals(k2));           // true
        System.out.println("    same hashCode: " + (k1.hashCode() == k2.hashCode())); // true
        System.out.println("    get(k2): "       + goodMap.get(k2));          // "found"

        // BROKEN: equals() overridden but hashCode() uses identity
        BrokenKey b1 = new BrokenKey("Alice", 30);
        BrokenKey b2 = new BrokenKey("Alice", 30);
        Map<BrokenKey, String> brokenMap = new HashMap<>();
        brokenMap.put(b1, "found");
        System.out.println("  Broken key (equals without hashCode):");
        System.out.println("    b1.equals(b2): " + b1.equals(b2));           // true
        System.out.println("    same hashCode: " + (b1.hashCode() == b2.hashCode())); // FALSE!
        System.out.println("    get(b2): "       + brokenMap.get(b2));        // null — LOST!
        // brokenMap has the entry under b1's identity hash, b2 has a different hash
        // → they land in different buckets, equals() never even gets called
    }

    // =========================================================
    // 6. WHY HashMap IS NOT THREAD-SAFE
    // =========================================================
    //
    // PROBLEM 1: Lost updates (race on put)
    //   Thread A and Thread B both call put() simultaneously.
    //   Both see bucket as empty, both create a new node.
    //   One overwrites the other → silent data loss.
    //
    // PROBLEM 2: Infinite loop during resize (Java 7 — historical but famous)
    //   Two threads resize simultaneously → circular reference in linked list
    //   → get() enters infinite loop.
    //   Java 8 fixed this by using head/tail insert instead of head insert,
    //   but concurrent modification is still unsafe.
    //
    // PROBLEM 3: Visible state without happens-before
    //   Thread A puts an entry; Thread B may never see it without synchronization.
    //
    // SOLUTIONS:
    //   Collections.synchronizedMap(new HashMap<>())  → wraps every method in synchronized
    //     ⚠ Entire map locked per operation — low concurrency
    //   ConcurrentHashMap                             → fine-grained locking (Java 8: per-bucket)
    //     ✅ Multiple threads can write to different buckets simultaneously
    //   Hashtable                                     → legacy, synchronized, don't use

    static void demonstrateThreadUnsafety() throws Exception {
        // Demonstrate lost updates with plain HashMap
        Map<Integer, Integer> unsafeMap = new HashMap<>();
        AtomicInteger lostUpdates = new AtomicInteger(0);

        Runnable incrementer = () -> {
            for (int i = 0; i < 1000; i++) {
                // NOT atomic: get → compute → put (3 separate steps, races possible)
                unsafeMap.merge(0, 1, Integer::sum);
            }
        };

        Thread t1 = new Thread(incrementer);
        Thread t2 = new Thread(incrementer);
        t1.start(); t2.start();
        t1.join(); t2.join();

        int expected = 2000;
        int actual = unsafeMap.getOrDefault(0, 0);
        System.out.println("  HashMap concurrent increment:");
        System.out.println("    expected: " + expected + ", actual: " + actual);
        System.out.println("    lost updates: " + (expected - actual) + " (varies per run)");

        // Safe version with ConcurrentHashMap
        ConcurrentHashMap<Integer, Integer> safeMap = new ConcurrentHashMap<>();
        Runnable safeIncrementer = () -> {
            for (int i = 0; i < 1000; i++) {
                safeMap.merge(0, 1, Integer::sum); // atomic in ConcurrentHashMap
            }
        };

        Thread t3 = new Thread(safeIncrementer);
        Thread t4 = new Thread(safeIncrementer);
        t3.start(); t4.start();
        t3.join(); t4.join();

        System.out.println("  ConcurrentHashMap concurrent increment:");
        System.out.println("    expected: 2000, actual: " + safeMap.get(0)); // always 2000
    }

    // =========================================================
    // 7. ConcurrentHashMap INTERNALS (Java 8+)
    // =========================================================
    //
    // Java 7: Segments (16 by default) — each segment is a mini HashMap with its own lock.
    //   → 16 threads could write concurrently (one per segment)
    //
    // Java 8: Redesigned — no more segments. Uses:
    //   - CAS (Compare-And-Swap) for inserting the FIRST node in a bucket (lock-free)
    //   - synchronized on the FIRST NODE of a bucket for chained operations
    //   - Multiple threads writing to DIFFERENT buckets proceed in parallel
    //   - Only threads writing to the SAME bucket are serialized
    //
    // Java 8 ConcurrentHashMap operations:
    //   putVal: CAS for empty bucket, synchronized(firstNode) for non-empty
    //   get:    FULLY lock-free (volatile reads)
    //   size:   approximate (uses LongAdder internally, not exact)
    //           Use mappingCount() for more accurate large counts
    //
    // KEY INSIGHT: In Java 8+, ConcurrentHashMap locks at BUCKET LEVEL, not map level.
    //   Practically: with n buckets and random keys, contention probability is ~1/n.
    //   With default 16 buckets and random keys: 15/16 = 93.75% of writes don't contend.
    //
    // IMPORTANT: ConcurrentHashMap does NOT allow null keys or null values.
    //   Reason: null would be ambiguous — does get(key)==null mean absent or null-valued?
    //   HashMap allows null; ConcurrentHashMap throws NullPointerException.
    //
    // INTERVIEW Q: "How is ConcurrentHashMap thread-safe without locking the whole map?"
    // A: Java 8+ uses CAS for inserting to empty buckets (fully lock-free) and
    //    synchronized on the bucket's head node for collision chains. Reads are
    //    lock-free via volatile. Only threads hitting the same bucket contend.
    //
    // INTERVIEW Q: "Does ConcurrentHashMap.size() return the exact count?"
    // A: Not guaranteed. It uses LongAdder cells and sums them — reads may see a
    //    momentarily stale count. Use mappingCount() for large maps, or accept
    //    that the count is a snapshot estimate.

    static void demonstrateConcurrentHashMap() throws Exception {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // compute, merge, computeIfAbsent are ATOMIC in ConcurrentHashMap
        map.put("counter", 0);
        map.merge("counter", 1, Integer::sum);         // atomic increment
        map.computeIfAbsent("lazy", k -> expensiveCompute(k));  // compute only if absent
        map.compute("counter", (k, v) -> v == null ? 1 : v + 1); // atomic read-modify-write

        System.out.println("  ConcurrentHashMap counter: " + map.get("counter"));

        // NULL not allowed
        try {
            map.put(null, 1);  // throws NullPointerException
        } catch (NullPointerException e) {
            System.out.println("  null key throws NPE in ConcurrentHashMap (by design)");
        }

        // size() is approximate for large maps
        System.out.println("  size(): " + map.size() + " (approximate for large maps)");
        System.out.println("  mappingCount(): " + map.mappingCount() + " (better for large)");
    }

    static int expensiveCompute(String key) { return key.length() * 42; }

    // =========================================================
    // 8. LinkedHashMap — LRU Cache
    // =========================================================
    //
    // LinkedHashMap extends HashMap with a doubly-linked list through all entries.
    // Two modes:
    //   INSERT ORDER (default): iteration order = insertion order
    //   ACCESS ORDER:           iteration order = least-recently-used → most-recently-used
    //                           (access = get OR put)
    //
    // LRU Cache using LinkedHashMap(capacity, loadFactor, accessOrder=true):
    //   Override removeEldestEntry() to evict when size > maxCapacity.
    //   The "eldest" in access-order mode = least recently used.
    //
    // INTERVIEW: "Implement an LRU cache" — LinkedHashMap is the clean Java answer.
    //   For thread-safe LRU: wrap in Collections.synchronizedMap() or use
    //   the DoublyLinkedList + ConcurrentHashMap pattern (see LinkedListAdvanced.java).
    //
    // REAL-WORLD: Spring's LRU eviction, Hibernate 2nd level cache,
    //             DNS cache, CPU instruction cache simulation

    static void demonstrateLinkedHashMap() {
        int MAX_SIZE = 3;

        // LRU Cache: accessOrder=true, removeEldestEntry evicts when over capacity
        LinkedHashMap<String, Integer> lruCache = new LinkedHashMap<>(MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return size() > MAX_SIZE; // evict LRU when over capacity
            }
        };

        lruCache.put("A", 1); // [A]
        lruCache.put("B", 2); // [A, B]
        lruCache.put("C", 3); // [A, B, C]
        System.out.println("  After A,B,C: " + lruCache.keySet()); // [A, B, C]

        lruCache.get("A");    // access A → moves to tail (most recently used)
        System.out.println("  After get(A): " + lruCache.keySet()); // [B, C, A]

        lruCache.put("D", 4); // capacity exceeded → evict head = B (least recently used)
        System.out.println("  After put(D): " + lruCache.keySet()); // [C, A, D]
    }

    // =========================================================
    // 9. COMMON INTERVIEW TRICKS
    // =========================================================

    static void demonstrateInterviewTricks() {
        // TRICK 1: getOrDefault vs get + null check
        Map<String, Integer> freq = new HashMap<>();
        String[] words = {"a", "b", "a", "c", "b", "a"};
        for (String w : words) {
            freq.merge(w, 1, Integer::sum); // cleaner than getOrDefault + put
        }
        System.out.println("  Word frequencies: " + freq);

        // TRICK 2: computeIfAbsent for building value lists
        Map<Integer, List<String>> grouped = new HashMap<>();
        String[] names = {"Alice", "Bob", "Anna", "Brian", "Carol"};
        for (String name : names) {
            grouped.computeIfAbsent(name.length(), k -> new ArrayList<>()).add(name);
        }
        System.out.println("  Grouped by length: " + grouped);

        // TRICK 3: entrySet iteration is faster than keySet + get
        // keySet + get:  two lookups per entry (hash + bucket walk twice)
        // entrySet:      one lookup, directly access key + value
        System.out.println("  entrySet iteration (preferred):");
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            // access e.getKey() and e.getValue() — no extra lookup
        }

        // TRICK 4: Map.of() is immutable and throws on duplicate keys
        try {
            Map<String, Integer> m = Map.of("a", 1, "a", 2); // duplicate key!
        } catch (IllegalArgumentException e) {
            System.out.println("  Map.of() duplicate key → " + e.getMessage());
        }

        // TRICK 5: HashMap vs Hashtable vs ConcurrentHashMap
        System.out.println("\n  Quick comparison:");
        System.out.println("    HashMap:           fast, null keys/values OK,  NOT thread-safe");
        System.out.println("    Hashtable:         legacy, synchronized whole, NO null keys");
        System.out.println("    synchronizedMap:   wraps HashMap, coarse lock, NO null key safety added");
        System.out.println("    ConcurrentHashMap: fine-grained lock, NO null keys/values, preferred");
        System.out.println("    LinkedHashMap:     insertion/access order, LRU pattern");
        System.out.println("    TreeMap:           sorted by key (natural/comparator), O(log n) ops");
    }
}

/*
 * ============================================================
 * INTERVIEW CHEAT SHEET — HashMap Internals
 * ============================================================
 *
 * STRUCTURE:
 *   Node<K,V>[] table   (bucket array, always power-of-2 size)
 *   Default capacity:   16
 *   Load factor:        0.75  → resize at size > capacity * 0.75
 *   Treeify threshold:  8     → linked list → red-black tree
 *   Untreeify threshold: 6   → tree → linked list
 *
 * PUT ALGORITHM:
 *   hash = key.hashCode() ^ (hashCode >>> 16)   ← spread entropy
 *   bucket = hash & (capacity - 1)              ← power-of-2 trick
 *   empty bucket → insert; collision → walk list/tree checking equals()
 *   size > threshold → resize (double, rehash all)
 *
 * hashCode + equals CONTRACT:
 *   equals() true → MUST have same hashCode
 *   Override both or neither (or use records/IDE generation)
 *   Breaking contract → lost entries (silent bug)
 *
 * THREAD SAFETY:
 *   HashMap            → NOT safe (race on put, resize)
 *   Hashtable          → safe but legacy, coarse lock
 *   synchronizedMap    → safe but coarse lock, low concurrency
 *   ConcurrentHashMap  → safe, bucket-level lock (Java 8 CAS + synchronized)
 *
 * ConcurrentHashMap KEY FACTS:
 *   - Empty bucket insert: CAS (lock-free)
 *   - Non-empty bucket:    synchronized on head node (per-bucket lock)
 *   - Read (get):          fully lock-free (volatile reads)
 *   - size():              approximate (LongAdder)
 *   - null keys/values:    NOT allowed (NPE)
 *
 * TIME COMPLEXITY:
 *   get/put:   O(1) average, O(log n) worst (Java 8+), O(n) worst (Java 7)
 *   resize:    O(n) — rehash all entries
 *   TreeMap:   O(log n) for all operations (sorted)
 *
 * PRE-SIZING:
 *   new HashMap<>((int)(expectedSize / 0.75) + 1)
 */
