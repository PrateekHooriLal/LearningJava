package com.java.collections.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ============================================================
 * CONCEPT: Concurrent Collections — Thread-Safe Without Locks (or With Fine-Grained Locks)
 * ============================================================
 *
 * WHY THIS EXISTS:
 *   Plain ArrayList/HashMap are NOT thread-safe. If two threads mutate them concurrently,
 *   you get corrupted state, infinite loops, or ConcurrentModificationException.
 *
 *   Three approaches to thread-safety, from worst to best:
 *   1. Hashtable / Vector          → coarse lock on EVERY method (full table lock)
 *   2. Collections.synchronizedMap → wrapper with coarse lock, still blocks ALL threads on every op
 *   3. ConcurrentHashMap           → fine-grained locking + CAS; concurrent READS are lock-free
 *
 * WHEN TO USE concurrent collections:
 *   - Multiple threads read/write the same collection without external synchronization
 *   - You want high throughput (many readers, occasional writers)
 *   - You need atomic compound operations (computeIfAbsent, merge)
 *
 * TIME / SPACE:
 *   ConcurrentHashMap: get O(1) avg, put O(1) avg, size() O(n) [approximate]
 *   CopyOnWriteArrayList: read O(1), write O(n) [copies entire array]
 *   BlockingQueue ops: O(1) for add/poll
 *   ConcurrentSkipListMap: O(log n) for all ops (no rehashing needed)
 *
 * INTERVIEW ANGLE:
 *   "Explain the difference between Hashtable, synchronizedMap, and ConcurrentHashMap"
 *   "How does ConcurrentHashMap achieve thread-safety without locking the entire map?"
 *   "When would you use CopyOnWriteArrayList?"
 *   "What is a CAS operation and why is it important for concurrency?"
 *
 * REAL-WORLD USE:
 *   - ConcurrentHashMap: caching in web servers, shared session stores, counters
 *   - CopyOnWriteArrayList: event listener registries (Spring ApplicationListeners)
 *   - BlockingQueue: producer-consumer pipelines, thread pool work queues
 *   - ConcurrentSkipListMap: leaderboards, time-ordered event queues
 * ============================================================
 */
public class ConcurrentCollectionsDeepDive {

    // =========================================================
    // SECTION 1: ConcurrentHashMap Internals
    // =========================================================

    /**
     * KEY INSIGHT: ConcurrentHashMap (Java 8+) uses a different locking model than Java 7.
     *
     * JAVA 7 approach: 16 "segments" (each a mini-HashMap with its own ReentrantLock).
     *   Concurrency level = 16 (at most 16 threads can write simultaneously).
     *
     * JAVA 8+ approach: Lock-free reads + per-bin synchronized writes.
     *   - The table is an array of Node<K,V> (each "bin" is a linked list or tree)
     *   - READ: volatile read of the Node array → no lock needed
     *   - WRITE head-of-chain: CAS (Compare-And-Swap) — atomic, no lock
     *   - WRITE within existing bin: synchronized(bin) — only locks THAT bucket
     *   - This means 2 threads writing to different buckets never block each other
     *
     * TREEIFICATION:
     *   When a bucket grows to >= TREEIFY_THRESHOLD (8 nodes), it converts from
     *   a linked list to a RED-BLACK TREE → O(log n) per-bucket instead of O(n).
     *   When it shrinks back to <= UNTREEIFY_THRESHOLD (6), it reverts to a list.
     *   This prevents hash-collision attacks (deliberate worst-case inputs).
     *
     * GOTCHA — size() is APPROXIMATE:
     *   ConcurrentHashMap uses a distributed counter (LongAdder-style) for size.
     *   size() sums up partial counts — it's not synchronized, so it may be stale.
     *   Use mappingCount() for large maps (returns long, same accuracy).
     *   If you need exact size under contention, you need external synchronization.
     *
     * GOTCHA — null keys/values NOT allowed:
     *   HashMap allows null key + null values. ConcurrentHashMap does NOT.
     *   Reason: null would be ambiguous — does get(key)==null mean "absent" or "value is null"?
     *   In a concurrent context, you can't check containsKey + get atomically without a lock.
     */
    static void demoConcurrentHashMap() {
        System.out.println("\n=== ConcurrentHashMap Demo ===");

        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // --- Atomic compound operations ---
        // computeIfAbsent: KEY INSIGHT — if key absent, compute and store; returns value.
        // The computation is done ATOMICALLY for that key (bin-level lock held during compute).
        // Use for: lazy initialization, building multi-maps
        map.computeIfAbsent("apple", k -> 0);
        map.computeIfAbsent("apple", k -> 999);  // 999 is NOT computed since "apple" already exists
        System.out.println("computeIfAbsent (apple): " + map.get("apple")); // 0

        // merge: KEY INSIGHT — if absent, use value; if present, apply biFunction(existing, newVal)
        // Perfect for frequency counting without putIfAbsent boilerplate
        map.merge("banana", 1, Integer::sum); // absent → value = 1
        map.merge("banana", 1, Integer::sum); // present → 1 + 1 = 2
        map.merge("banana", 1, Integer::sum); // present → 2 + 1 = 3
        System.out.println("merge (banana): " + map.get("banana")); // 3

        // getOrDefault: read-only, does NOT insert. Different from computeIfAbsent.
        int count = map.getOrDefault("cherry", 0);
        System.out.println("getOrDefault (cherry): " + count); // 0, nothing stored

        // putIfAbsent vs computeIfAbsent:
        // putIfAbsent(k, v) — v is EAGERLY evaluated before the call.
        //   If v is expensive to create (e.g., new ArrayList()), you waste time if key exists.
        // computeIfAbsent(k, fn) — fn is LAZILY called only if key is absent. Better.
        map.putIfAbsent("apple", new Random().nextInt()); // Random() called regardless!
        map.computeIfAbsent("date", k -> expensiveCompute(k)); // Only called if absent

        // INTERVIEW Q: "How is computeIfAbsent in CHM atomic?"
        // A: It acquires the bin lock before running the lambda, so no two threads
        //    can compute the same key's initial value simultaneously.

        // forEach with parallelism threshold:
        // When threshold = 1, uses ForkJoinPool for true parallel execution
        map.forEach(1, (k, v) -> {
            // runs in parallel if threshold reached
        });

        System.out.println("Final map: " + map);
    }

    static int expensiveCompute(String key) { return key.length() * 42; }

    // =========================================================
    // SECTION 2: CopyOnWriteArrayList Internals
    // =========================================================

    /**
     * KEY INSIGHT: CopyOnWriteArrayList (COWAL) achieves thread-safety by making writes
     * immutable — every mutating operation (add, set, remove) copies the ENTIRE backing array,
     * applies the change, then atomically swaps the reference.
     *
     * WHY reads are always safe (and fast):
     *   The internal array reference is volatile. When a reader calls get(), it reads the
     *   current snapshot of the array. Even if a writer creates a new array, the reader's
     *   snapshot reference remains valid until GC reclaims it.
     *
     * Iterators are SNAPSHOT iterators:
     *   When you call iterator(), it captures the current array reference.
     *   Subsequent modifications create a NEW array — the iterator still sees the OLD one.
     *   → ConcurrentModificationException is IMPOSSIBLE (no modCount tracking)
     *   → Iterator may NOT reflect recent additions/removals (stale view)
     *
     * GOTCHA: CopyOnWriteArrayList.iterator().remove() throws UnsupportedOperationException.
     *   You can't remove via iterator. Use removeIf() on the list directly.
     *
     * WHEN TO USE:
     *   Good: reads >> writes (e.g., event listener lists, configuration lists)
     *   Bad: high-frequency writes (copying O(n) array each time is expensive)
     *
     * vs ArrayList:
     *   ArrayList iterator uses modCount — throws ConcurrentModificationException if modified
     *   during iteration (even from same thread if not using iterator.remove()).
     */
    static void demoCopyOnWrite() throws InterruptedException {
        System.out.println("\n=== CopyOnWriteArrayList vs ArrayList Demo ===");

        // Demonstrate ConcurrentModificationException with ArrayList
        List<Integer> arrayList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        try {
            for (Integer i : arrayList) {
                if (i == 3) arrayList.add(99); // MODIFIES during iteration
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("ArrayList threw ConcurrentModificationException (expected)");
        }

        // COWAL: no exception — iterator sees old snapshot
        List<Integer> cowal = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        int iteratedCount = 0;
        for (Integer i : cowal) {
            if (i == 3) cowal.add(99); // modifies, but iterator has snapshot
            iteratedCount++;
        }
        System.out.println("COWAL iterated " + iteratedCount + " elements (snapshot, didn't see 99)");
        System.out.println("COWAL actual size after: " + cowal.size()); // 6

        // Multi-threaded scenario: event listener pattern
        CopyOnWriteArrayList<String> listeners = new CopyOnWriteArrayList<>();
        listeners.add("ListenerA");
        listeners.add("ListenerB");

        // Safe to iterate even if another thread adds a listener concurrently
        CountDownLatch latch = new CountDownLatch(1);
        Thread writer = Thread.ofVirtual().start(() -> {
            try {
                latch.await();
                listeners.add("ListenerC"); // safe concurrent add
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        latch.countDown();
        for (String listener : listeners) {
            System.out.println("Notifying: " + listener);
        }
        writer.join();
        System.out.println("Final listeners: " + listeners);
    }

    // =========================================================
    // SECTION 3: BlockingQueue Implementations
    // =========================================================

    /*
     * BLOCKING QUEUE COMPARISON TABLE:
     * ┌──────────────────────────┬──────────┬──────────────┬──────────────┬──────────────────┐
     * │ Implementation           │ Bounded? │ Backing      │ Locks        │ Best Use         │
     * ├──────────────────────────┼──────────┼──────────────┼──────────────┼──────────────────┤
     * │ ArrayBlockingQueue       │ Yes      │ Array        │ 1 lock       │ Simple pipelines │
     * │ LinkedBlockingQueue      │ Optional │ LinkedList   │ 2 locks      │ High-throughput  │
     * │ PriorityBlockingQueue    │ No       │ Heap array   │ 1 lock       │ Priority tasks   │
     * │ SynchronousQueue         │ 0 cap    │ None (none)  │ CAS          │ Direct handoff   │
     * │ LinkedTransferQueue      │ No       │ LinkedList   │ CAS          │ Best throughput  │
     * │ DelayQueue               │ No       │ Heap         │ 1 lock       │ Scheduled tasks  │
     * └──────────────────────────┴──────────┴──────────────┴──────────────┴──────────────────┘
     *
     * ArrayBlockingQueue:
     *   - Fixed capacity array, single ReentrantLock (fair or unfair)
     *   - Fair mode: FIFO ordering of blocked threads (lower throughput, no starvation)
     *   - put() blocks if full; take() blocks if empty
     *   - offer(e, timeout, unit): non-blocking with timeout
     *
     * LinkedBlockingQueue:
     *   - Two separate locks: takeLock (head) and putLock (tail)
     *   - Producers and consumers can proceed concurrently (different ends)
     *   - Default capacity = Integer.MAX_VALUE (effectively unbounded — GOTCHA: OOM risk!)
     *   - Used internally by ThreadPoolExecutor's default work queue
     *
     * SynchronousQueue:
     *   - Zero capacity — no buffering at all
     *   - put() blocks until another thread calls take() (and vice versa)
     *   - "Handoff" semantics: producer waits for a consumer to be ready
     *   - Used by Executors.newCachedThreadPool() — each task directly handed to a thread
     *
     * PriorityBlockingQueue:
     *   - Unbounded (grows as needed)
     *   - Elements must implement Comparable or provide Comparator
     *   - No fairness among equal-priority elements
     *   - GOTCHA: Iterator does NOT traverse in priority order — only poll() does
     *
     * DelayQueue:
     *   - Elements implement Delayed interface (getDelay() + compareTo())
     *   - take() blocks until an element's delay has expired
     *   - Use for: scheduled task execution, connection pool expiration
     */

    /**
     * KEY INSIGHT: BlockingQueue is the backbone of the Producer-Consumer pattern.
     * It removes the need for explicit wait/notify synchronization.
     * The blocking behavior is built into put() and take() operations.
     */
    static void demoBlockingQueue() throws InterruptedException {
        System.out.println("\n=== BlockingQueue Producer-Consumer Demo ===");

        // Bounded queue — backpressure: producer slows down when queue is full
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        // Producer: puts items, will block if queue is full (backpressure!)
        Thread producer = Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    queue.put(i);           // BLOCKS if queue full
                    produced.incrementAndGet();
                }
                queue.put(-1);             // sentinel / poison pill to signal done
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        // Consumer: takes items, will block if queue is empty
        Thread consumer = Thread.ofVirtual().start(() -> {
            try {
                while (true) {
                    int val = queue.take();  // BLOCKS if queue empty
                    if (val == -1) break;    // poison pill pattern
                    consumed.incrementAndGet();
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.join();
        consumer.join();
        System.out.println("Produced: " + produced.get() + ", Consumed: " + consumed.get());

        // SynchronousQueue demo: direct handoff
        SynchronousQueue<String> handoff = new SynchronousQueue<>();
        Thread sender = Thread.ofVirtual().start(() -> {
            try {
                System.out.println("Sender waiting for receiver...");
                handoff.put("DIRECT_MESSAGE"); // blocks until receiver calls take()
                System.out.println("Sender: message delivered");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread receiver = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(100); // simulate delay before receiving
                String msg = handoff.take();
                System.out.println("Receiver got: " + msg);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        sender.join(); receiver.join();
    }

    // =========================================================
    // SECTION 4: ConcurrentSkipListMap
    // =========================================================

    /**
     * KEY INSIGHT: ConcurrentSkipListMap is the concurrent alternative to TreeMap.
     *
     * Skip List structure:
     *   Multiple layers of linked lists. Bottom layer has all elements.
     *   Each higher layer skips over ~half the elements (like binary search on a list).
     *   Expected height = O(log n). Each search/insert = O(log n) expected.
     *
     * Why NOT use synchronized(treeMap)?
     *   TreeMap's balancing (rotations) requires locking the entire tree for restructuring.
     *   ConcurrentSkipListMap uses CAS for lock-free insertions at each level.
     *   Better scalability: multiple threads can traverse/insert at different levels concurrently.
     *
     * INTERVIEW Q: "When would you choose ConcurrentSkipListMap over ConcurrentHashMap?"
     * A: When you need SORTED iteration order or range queries (headMap, tailMap, subMap).
     *    ConcurrentHashMap has O(1) ops but no ordering. ConcurrentSkipListMap is O(log n)
     *    but provides NavigableMap operations needed for sorted data.
     *
     * ConcurrentSkipListSet: backed by ConcurrentSkipListMap. Thread-safe sorted set.
     */
    static void demoSkipList() {
        System.out.println("\n=== ConcurrentSkipListMap Demo ===");

        ConcurrentSkipListMap<Integer, String> skipMap = new ConcurrentSkipListMap<>();
        skipMap.put(5, "five");
        skipMap.put(2, "two");
        skipMap.put(8, "eight");
        skipMap.put(1, "one");
        skipMap.put(9, "nine");

        // NavigableMap ops — all O(log n), all thread-safe
        System.out.println("First: " + skipMap.firstKey());         // 1
        System.out.println("Last: " + skipMap.lastKey());           // 9
        System.out.println("FloorKey(6): " + skipMap.floorKey(6));  // 5 (<=6)
        System.out.println("CeilingKey(6): " + skipMap.ceilingKey(6)); // 8 (>=6)
        System.out.println("HeadMap(<5): " + skipMap.headMap(5));   // {1=one, 2=two}
        System.out.println("TailMap(>=5): " + skipMap.tailMap(5));  // {5=five, 8=eight, 9=nine}

        // pollFirstEntry is atomic — safe for concurrent access
        Map.Entry<Integer, String> smallest = skipMap.pollFirstEntry();
        System.out.println("Polled: " + smallest);
    }

    // =========================================================
    // SECTION 5: ConcurrentModificationException Demo
    // =========================================================

    /**
     * KEY INSIGHT: ConcurrentModificationException is a FAIL-FAST mechanism.
     * Collections track structural modifications via a modCount field.
     * Iterator captures modCount at creation; checks it on each next() call.
     * If modCount changed (any structural modification), next() throws CME.
     *
     * This is NOT a thread-safety guarantee — it's a best-effort detection tool.
     * It CAN be triggered from a single thread (wrong usage pattern).
     * It is NOT guaranteed in multi-threaded scenarios (may silently corrupt instead).
     *
     * GOTCHA: Removing via iterator.remove() is safe (updates modCount).
     *         Removing via list.remove() inside a for-each is NOT safe (CME).
     *         Use removeIf() for safe in-loop removal on regular collections.
     */
    static void demoConcurrentModification() throws InterruptedException {
        System.out.println("\n=== ConcurrentModificationException Demo ===");

        // --- Single-threaded CME ---
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        try {
            for (Integer x : list) {  // for-each uses an iterator internally
                list.remove(x);       // structural modification while iterating → CME
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Single-threaded CME from ArrayList (expected)");
        }

        // Safe removal: use removeIf (uses iterator.remove() internally)
        List<Integer> safeList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        safeList.removeIf(x -> x % 2 == 0); // removes 2, 4 safely
        System.out.println("After removeIf: " + safeList); // [1, 3, 5]

        // --- Multi-threaded: ConcurrentHashMap has NO CME during concurrent iteration ---
        ConcurrentHashMap<Integer, String> chm = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) chm.put(i, "v" + i);

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger iteratedCount = new AtomicInteger(0);

        // Thread 1: iterates the map
        Thread reader = Thread.ofVirtual().start(() -> {
            try {
                startLatch.await();
                // CHM iterators are WEAKLY consistent — they may or may not reflect
                // concurrent modifications. No CME thrown.
                for (Map.Entry<Integer, String> e : chm.entrySet()) {
                    iteratedCount.incrementAndGet();
                    Thread.yield(); // give writer a chance
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        // Thread 2: modifies the map while thread 1 iterates
        Thread writer = Thread.ofVirtual().start(() -> {
            try {
                startLatch.await();
                for (int i = 100; i < 200; i++) chm.put(i, "new" + i);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        startLatch.countDown();
        reader.join(); writer.join();
        System.out.println("CHM concurrent iteration completed without CME. Iterated ~" + iteratedCount.get() + " entries.");
        System.out.println("CHM final size: " + chm.size()); // 200
    }

    // =========================================================
    // SECTION 6: Comparison Summary (as interview prep)
    // =========================================================

    /*
     * INTERVIEW QUICK COMPARISON:
     *
     * Hashtable vs Collections.synchronizedMap vs ConcurrentHashMap:
     *
     *   Hashtable:
     *     - Legacy (Java 1.0). Every method is synchronized on the table.
     *     - Null keys/values: NOT allowed.
     *     - Performance: single lock = full serialization of all threads.
     *     - Don't use in new code.
     *
     *   Collections.synchronizedMap(new HashMap<>()):
     *     - Wrapper that adds synchronized on "this" (the wrapper object) to every method.
     *     - Null keys/values: allowed (HashMap semantics).
     *     - GOTCHA: compound operations (check-then-act) are NOT atomic unless you
     *       synchronized(syncMap) externally around them.
     *     - GOTCHA: iteration is NOT thread-safe — you must synchronize externally:
     *         synchronized(syncMap) { for (Map.Entry e : syncMap.entrySet()) ... }
     *
     *   ConcurrentHashMap:
     *     - Null keys/values: NOT allowed.
     *     - Reads: fully concurrent (no lock), Writes: bin-level lock only.
     *     - Compound ops (computeIfAbsent, merge) are atomic at bin level.
     *     - Iteration: weakly consistent (no CME, may/may not see concurrent changes).
     *     - size(): approximate. Use mappingCount() for large maps.
     *     - Best choice for shared mutable maps in modern Java.
     *
     * COMPLEXITY BLOCK:
     *   ┌──────────────────────────────┬─────────┬──────────┬───────────────┐
     *   │ Collection                   │ get     │ put      │ iterate       │
     *   ├──────────────────────────────┼─────────┼──────────┼───────────────┤
     *   │ HashMap                      │ O(1)    │ O(1)     │ O(n)          │
     *   │ Hashtable                    │ O(1)+L  │ O(1)+L   │ O(n)+L        │
     *   │ synchronizedMap(HashMap)     │ O(1)+L  │ O(1)+L   │ O(n) ext sync │
     *   │ ConcurrentHashMap            │ O(1)    │ O(1)+bL  │ O(n) weak     │
     *   │ CopyOnWriteArrayList (read)  │ O(1)    │ O(n)     │ O(n) snapshot │
     *   │ ConcurrentSkipListMap        │ O(logn) │ O(logn)  │ O(n) sorted   │
     *   └──────────────────────────────┴─────────┴──────────┴───────────────┘
     *   L = full table lock, bL = bin-level lock
     */

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Concurrent Collections Deep Dive           ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        demoConcurrentHashMap();
        demoCopyOnWrite();
        demoBlockingQueue();
        demoSkipList();
        demoConcurrentModification();

        System.out.println("\n=== All demos complete ===");
    }
}
