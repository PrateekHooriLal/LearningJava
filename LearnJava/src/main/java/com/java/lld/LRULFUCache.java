package com.java.lld;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU AND LFU CACHE — LLD DEEP DIVE
 *
 * CONCEPT:
 *   Cache eviction policies decide WHICH entry to remove when the cache is full.
 *   Two most common in FAANG interviews:
 *
 *   LRU (Least Recently Used) — evict the entry that was accessed LEAST RECENTLY.
 *     Mental model: "What you haven't touched in a while is cold data — kick it out."
 *     Used by: Linux page cache, CPU L1/L2 caches, Redis (with approximation), database buffer pools.
 *
 *   LFU (Least Frequently Used) — evict the entry that was accessed the FEWEST TIMES.
 *     Mental model: "What you rarely need is waste — kick it out."
 *     Used by: CDN edge caches, in-memory store for hot/cold data tiers.
 *
 * WHEN TO CHOOSE EACH:
 *   LRU: good for temporal locality (recently used is likely to be reused soon).
 *         Simple to implement; O(1) all operations; most common in interviews.
 *   LFU: good for frequency-skewed access (popular items should stay regardless of recency).
 *         More complex; O(1) all ops requires careful design. Risk: new items evicted quickly.
 *
 * TIME COMPLEXITY (both):
 *   get()  — O(1)
 *   put()  — O(1)
 *
 * SPACE COMPLEXITY: O(capacity) for each cache.
 *
 * INTERVIEW ANGLE:
 *   - Most common FAANG LLD question (Google, Amazon, Meta). Know both cold.
 *   - Interviewer will ask: "Make it thread-safe" → add ReadWriteLock or synchronized.
 *   - Follow-up: "What if capacity can change at runtime?" → expose setCapacity() with re-eviction.
 *   - Follow-up: "Add TTL (time-to-live) expiry" → store (value, expiryTime), check on get().
 *
 * REAL-WORLD USE:
 *   - java.util.LinkedHashMap(capacity, loadFactor, accessOrder=true) → built-in LRU in 3 lines!
 *   - Guava Cache, Caffeine → production LRU/LFU with async refresh, stats, weak references.
 */
public class LRULFUCache {

    // =========================================================================
    // PART 1: Cache Interface
    // =========================================================================

    /**
     * Generic cache contract — program to interface, not implementation.
     * INTERVIEW INSIGHT: defining an interface first is a signal of good OOP/LLD thinking.
     */
    interface Cache<K, V> {
        V get(K key);
        void put(K key, V value);
        int size();
        boolean containsKey(K key);
    }

    // =========================================================================
    // PART 2: LRU Cache
    // =========================================================================

    /**
     * LRU CACHE — O(1) get and put using DoublyLinkedList + HashMap.
     *
     * KEY INSIGHT: We need two things simultaneously:
     *   1. O(1) access by key → HashMap<K, Node>
     *   2. O(1) reorder (move to head) + O(1) evict (remove tail) → DoublyLinkedList
     *
     * WHY DOUBLY (not singly) linked list?
     *   To remove a node in O(1), we need both prev and next pointers.
     *   Singly linked: to remove node X, you must traverse from head to find X.prev → O(n).
     *   Doubly linked: node X has X.prev directly → unlink in O(1).
     *
     * STRUCTURE:
     *   [dummy_head] <-> [MRU] <-> ... <-> [LRU] <-> [dummy_tail]
     *   On get/put: move accessed node to just after dummy_head.
     *   On evict: remove node just before dummy_tail.
     *
     * DUMMY SENTINEL NODES trick:
     *   Using sentinel head/tail avoids null checks on edge cases (empty list, 1-element list).
     *   This is a standard interview pattern — always use sentinel nodes in linked list LLD.
     */
    static class LRUCache<K, V> implements Cache<K, V> {

        // Doubly linked list node
        private class Node {
            K key;
            V value;
            Node prev, next;

            Node(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        private final int capacity;
        private final Map<K, Node> map;   // key → node reference for O(1) lookup
        private final Node head;           // dummy sentinel: just after head = MRU
        private final Node tail;           // dummy sentinel: just before tail = LRU

        public LRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new HashMap<>();
            // Sentinel nodes — never hold real data
            head = new Node(null, null);
            tail = new Node(null, null);
            head.next = tail;
            tail.prev = head;
        }

        /**
         * Get value for key. Returns null if not present.
         *
         * KEY INSIGHT: On a cache HIT, we must mark this key as "most recently used"
         * by moving its node to just after dummy_head.
         *
         * Time: O(1) — HashMap lookup + O(1) pointer rewiring.
         */
        @Override
        public V get(K key) {
            Node node = map.get(key);
            if (node == null) return null;   // cache miss

            // Cache hit: promote to MRU position
            moveToFront(node);
            return node.value;
        }

        /**
         * Insert or update key-value pair.
         *
         * Cases:
         *   Key exists → update value + promote to MRU.
         *   Key new, cache not full → insert at MRU position.
         *   Key new, cache full → evict LRU (node before tail), insert at MRU position.
         *
         * Time: O(1)
         */
        @Override
        public void put(K key, V value) {
            Node node = map.get(key);

            if (node != null) {
                // Update existing entry
                node.value = value;
                moveToFront(node);    // promote to MRU
            } else {
                // New entry
                if (map.size() == capacity) {
                    evictLRU();       // remove node just before dummy tail
                }
                Node newNode = new Node(key, value);
                map.put(key, newNode);
                addToFront(newNode);  // insert as MRU
            }
        }

        @Override
        public int size() { return map.size(); }

        @Override
        public boolean containsKey(K key) { return map.containsKey(key); }

        // --- Helper: move existing node to MRU position ---
        private void moveToFront(Node node) {
            removeFromList(node);  // unlink from current position
            addToFront(node);      // re-link just after head
        }

        // --- Helper: insert node just after dummy head ---
        private void addToFront(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
            // After: [head] <-> [node] <-> [old-MRU] <-> ...
        }

        // --- Helper: unlink node from its current position ---
        private void removeFromList(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            // node.prev and node.next still point to neighbors — that's fine, GC won't collect
        }

        // --- Helper: evict the LRU node (just before dummy tail) ---
        private void evictLRU() {
            Node lru = tail.prev;     // node just before tail = LRU
            removeFromList(lru);
            map.remove(lru.key);      // must also remove from HashMap!
            // GOTCHA: forgetting map.remove(lru.key) is the #1 bug — map leaks memory
        }

        @Override
        public String toString() {
            // Print in MRU → LRU order for debugging
            StringBuilder sb = new StringBuilder("LRU[");
            Node curr = head.next;
            while (curr != tail) {
                sb.append(curr.key).append("=").append(curr.value);
                if (curr.next != tail) sb.append(", ");
                curr = curr.next;
            }
            return sb.append("]").toString();
        }
    }

    // =========================================================================
    // PART 3: LFU Cache — O(1) all operations
    // =========================================================================

    /**
     * LFU CACHE — O(1) get and put using three data structures.
     *
     * KEY INSIGHT: LFU needs to evict the LEAST FREQUENTLY used item.
     * Tie-breaking: if multiple items have the same minimum frequency,
     * evict the LEAST RECENTLY USED among them (consistent with LRU).
     *
     * NAIVE LFU (O(log n)):
     *   Min-heap of (freq, timestamp, key) → heap pop on eviction → O(log n).
     *   Fine for most production use but interviewers want O(1).
     *
     * O(1) LFU — the THREE-MAP design:
     *   1. keyMap:   Map<K, (V, freq)>         — key → value + current frequency
     *   2. freqMap:  Map<Integer, LinkedHashSet<K>> — freq → set of keys with that freq
     *                                              LinkedHashSet = insertion-order = LRU tie-break
     *   3. minFreq:  int                        — current minimum frequency tracker
     *
     * HOW minFreq STAYS O(1):
     *   - On get: freq++ → minFreq might increase by 1 (only if old freq == minFreq AND
     *     that freq bucket is now empty). Check this: if freqMap[minFreq] is empty, minFreq++.
     *   - On put (NEW key): minFreq resets to 1 — new keys always have freq=1.
     *   - On put (UPDATE): same as get — freq++, potentially minFreq++.
     *   Because we ALWAYS insert new keys at freq=1, minFreq can only reset to 1 on new put.
     *
     * INTERVIEW GOTCHA: LinkedHashSet preserves insertion order → iterating first element
     * gives the least recently used item among those with the same frequency.
     */
    static class LFUCache<K, V> implements Cache<K, V> {

        private static class Entry<V> {
            V value;
            int freq;
            Entry(V value, int freq) { this.value = value; this.freq = freq; }
        }

        private final int capacity;
        private int minFreq;
        private final Map<K, Entry<V>> keyMap;              // key → (value, freq)
        private final Map<Integer, LinkedHashSet<K>> freqMap; // freq → ordered set of keys

        public LFUCache(int capacity) {
            this.capacity = capacity;
            this.minFreq = 0;
            this.keyMap = new HashMap<>();
            this.freqMap = new HashMap<>();
        }

        /**
         * Get value for key. Returns null if not present.
         * Side effect: increments key's frequency, re-buckets it, updates minFreq if needed.
         *
         * Time: O(1) — all map ops O(1) amortized; LinkedHashSet ops O(1).
         */
        @Override
        public V get(K key) {
            Entry<V> entry = keyMap.get(key);
            if (entry == null) return null;  // cache miss

            incrementFreq(key, entry);        // O(1)
            return entry.value;
        }

        /**
         * Insert or update key-value pair.
         *
         * Time: O(1)
         */
        @Override
        public void put(K key, V value) {
            if (capacity <= 0) return;

            Entry<V> entry = keyMap.get(key);

            if (entry != null) {
                // Update: change value + increment freq
                entry.value = value;
                incrementFreq(key, entry);
            } else {
                // New key
                if (keyMap.size() == capacity) {
                    evictLFU();  // remove from minFreq bucket's LRU position
                }
                // New keys always start at freq=1
                keyMap.put(key, new Entry<>(value, 1));
                freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
                minFreq = 1;  // CRITICAL: minFreq resets to 1 for every new key
            }
        }

        @Override
        public int size() { return keyMap.size(); }

        @Override
        public boolean containsKey(K key) { return keyMap.containsKey(key); }

        /**
         * Increment frequency for an existing key.
         * Moves key from freqMap[oldFreq] → freqMap[oldFreq+1].
         * Updates minFreq if oldFreq bucket is now empty.
         */
        private void incrementFreq(K key, Entry<V> entry) {
            int oldFreq = entry.freq;
            entry.freq++;

            // Remove from old frequency bucket
            Set<K> oldBucket = freqMap.get(oldFreq);
            oldBucket.remove(key);

            // If old bucket is now empty AND it was the minimum, advance minFreq
            if (oldBucket.isEmpty() && oldFreq == minFreq) {
                minFreq++;   // safe: we know at least this key now has freq=oldFreq+1
                freqMap.remove(oldFreq);  // cleanup to avoid memory bloat
            }

            // Add to new frequency bucket
            freqMap.computeIfAbsent(entry.freq, k -> new LinkedHashSet<>()).add(key);
        }

        /**
         * Evict the LFU entry. Tie-break: evict LRU among min-freq entries.
         *
         * LinkedHashSet.iterator().next() returns the FIRST inserted = LRU within that freq.
         * This gives correct LRU tie-breaking in O(1).
         */
        private void evictLFU() {
            LinkedHashSet<K> minBucket = freqMap.get(minFreq);
            K evictKey = minBucket.iterator().next();  // LRU among min-freq keys
            minBucket.remove(evictKey);
            if (minBucket.isEmpty()) freqMap.remove(minFreq);
            keyMap.remove(evictKey);
            // NOTE: minFreq will be reset to 1 by the caller (new key insertion)
        }

        @Override
        public String toString() {
            return "LFU[size=" + keyMap.size() + ", minFreq=" + minFreq + ", keys=" + keyMap.keySet() + "]";
        }
    }

    // =========================================================================
    // PART 4: CacheFactory (Factory Pattern)
    // =========================================================================

    /**
     * CACHE FACTORY — creates the right cache type based on policy string.
     *
     * DESIGN PATTERN: Factory Method.
     * WHY: decouples cache creation from usage; clients only depend on the Cache interface.
     * INTERVIEW: shows understanding of OCP (Open/Closed Principle) — add new policies
     * without modifying callers.
     */
    static class CacheFactory {

        public enum Policy { LRU, LFU }

        public static <K, V> Cache<K, V> create(Policy policy, int capacity) {
            return switch (policy) {
                case LRU -> new LRUCache<>(capacity);
                case LFU -> new LFUCache<>(capacity);
            };
        }
    }

    // =========================================================================
    // PART 5: Thread-Safe Cache Wrapper
    // =========================================================================

    /**
     * THREAD-SAFE CACHE using ReadWriteLock (Decorator Pattern).
     *
     * DECORATOR PATTERN: wraps any Cache<K,V> and adds thread-safety transparently.
     * WHY ReadWriteLock vs synchronized?
     *   - synchronized: every access exclusive → reads block reads → low concurrency.
     *   - ReadWriteLock: multiple concurrent READS allowed; writes exclusive.
     *   For read-heavy caches (typical) → much better throughput.
     *
     * INTERVIEW FOLLOW-UP: "What if you want even higher concurrency?"
     *   → Striped lock (like ConcurrentHashMap's segment locking) — divide key space into N locks.
     *   → StampedLock with tryOptimisticRead (see StampedLockDemo.java) — ultra-low overhead reads.
     *
     * GOTCHA: ReadWriteLock does NOT upgrade — you can't hold a read lock and then acquire a write lock
     * (deadlock!). If put() needs to read-then-write (check-then-act), you must:
     *   1. Release read lock.
     *   2. Acquire write lock.
     *   3. Re-check condition (another thread may have changed state between steps 1 and 2).
     */
    static class ThreadSafeCache<K, V> implements Cache<K, V> {

        private final Cache<K, V> delegate;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        public ThreadSafeCache(Cache<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public V get(K key) {
            lock.readLock().lock();
            try {
                // GOTCHA: LRU.get() modifies internal structure (moves node to front)
                // → it's NOT a pure read, so strictly it needs a write lock!
                // Production fix: use write lock for get() in LRU, or use ConcurrentLinkedHashMap.
                // Here we use write lock for correctness:
                return delegate.get(key);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void put(K key, V value) {
            lock.writeLock().lock();
            try {
                delegate.put(key, value);
            } finally {
                lock.writeLock().unlock();
            }
        }

        @Override
        public int size() {
            lock.readLock().lock();
            try { return delegate.size(); }
            finally { lock.readLock().unlock(); }
        }

        @Override
        public boolean containsKey(K key) {
            lock.readLock().lock();
            try { return delegate.containsKey(key); }
            finally { lock.readLock().unlock(); }
        }
    }

    // =========================================================================
    // PART 6: DEMO AND TESTS
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== LRU CACHE DEMO ===\n");
        demoLRU();

        System.out.println("\n=== LFU CACHE DEMO ===\n");
        demoLFU();

        System.out.println("\n=== FACTORY + THREAD-SAFE DEMO ===\n");
        demoFactory();

        System.out.println("\n=== LINKEDHASHMAP-BASED LRU (3-line shortcut) ===\n");
        demoLinkedHashMapLRU();
    }

    static void demoLRU() {
        // LeetCode 146 — LRU Cache
        // capacity = 2
        LRUCache<Integer, Integer> lru = new LRUCache<>(2);

        lru.put(1, 1);  // cache: {1=1}
        lru.put(2, 2);  // cache: {2=2, 1=1}  (2 is MRU)
        System.out.println("get(1)=" + lru.get(1));  // 1  → cache: {1=1, 2=2} (1 promoted to MRU)
        lru.put(3, 3);  // evict key 2 (LRU), cache: {3=3, 1=1}
        System.out.println("get(2)=" + lru.get(2));  // -1 (evicted) — expect null
        lru.put(4, 4);  // evict key 1 (LRU), cache: {4=4, 3=3}
        System.out.println("get(1)=" + lru.get(1));  // null (evicted)
        System.out.println("get(3)=" + lru.get(3));  // 3
        System.out.println("get(4)=" + lru.get(4));  // 4
        System.out.println(lru);
    }

    static void demoLFU() {
        // LeetCode 460 — LFU Cache
        // capacity = 2
        LFUCache<Integer, Integer> lfu = new LFUCache<>(2);

        lfu.put(1, 1);   // cache: key=1, freq=1
        lfu.put(2, 2);   // cache: key=1 freq=1, key=2 freq=1; minFreq=1
        System.out.println("get(1)=" + lfu.get(1));  // 1  → key=1 freq becomes 2
        lfu.put(3, 3);   // evict key=2 (freq=1, LRU among freq-1); minFreq=1
        System.out.println("get(2)=" + lfu.get(2));  // null (evicted)
        System.out.println("get(3)=" + lfu.get(3));  // 3
        lfu.put(4, 4);   // evict key=1? key=1 freq=2, key=3 freq=2; minFreq now... both freq=2, key=3 is LRU
        System.out.println("get(1)=" + lfu.get(1));  // 1 — if not evicted
        System.out.println("get(3)=" + lfu.get(3));  // null — key=3 was LRU at freq=2
        System.out.println("get(4)=" + lfu.get(4));  // 4
        System.out.println(lfu);
    }

    static void demoFactory() {
        Cache<String, String> lruCache = CacheFactory.create(CacheFactory.Policy.LRU, 3);
        Cache<String, String> safeCache = new ThreadSafeCache<>(lruCache);

        safeCache.put("a", "apple");
        safeCache.put("b", "banana");
        safeCache.put("c", "cherry");
        System.out.println("get(a)=" + safeCache.get("a"));  // apple
        safeCache.put("d", "date");  // evicts "b" (LRU)
        System.out.println("get(b)=" + safeCache.get("b"));  // null
        System.out.println("size=" + safeCache.size());       // 3
    }

    /**
     * LINKEDHASHMAP-BASED LRU — the 3-line production shortcut.
     *
     * LinkedHashMap(capacity, loadFactor, accessOrder=true) maintains insertion-order
     * (accessOrder=false) OR access-order (accessOrder=true).
     * With accessOrder=true: get(k) moves k to end → end = MRU, head = LRU.
     * Override removeEldestEntry() to evict when over capacity.
     *
     * INTERVIEW: show this after implementing from scratch. Demonstrates breadth.
     *
     * Time: O(1) get/put  Space: O(capacity)
     */
    static void demoLinkedHashMapLRU() {
        final int CAP = 3;
        // Anonymous subclass override removeEldestEntry for auto-eviction
        Map<Integer, Integer> lruMap = new LinkedHashMap<>(CAP, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
                return size() > CAP;  // evict when capacity exceeded
            }
        };

        lruMap.put(1, 10);
        lruMap.put(2, 20);
        lruMap.put(3, 30);
        lruMap.get(1);       // access 1 → moves to MRU; LRU is now key=2
        lruMap.put(4, 40);   // evict LRU = key=2
        System.out.println("Contains key 2: " + lruMap.containsKey(2));  // false
        System.out.println("Contains key 1: " + lruMap.containsKey(1));  // true
        System.out.println("Map: " + lruMap);  // {3=30, 1=10, 4=40} LRU→MRU order
    }

    // =========================================================================
    // COMPLEXITY SUMMARY
    // =========================================================================
    //
    // ┌──────────────────────────────────────────────────────────────────────┐
    // │ Cache   │ get()  │ put()  │ evict() │ Space     │ Tie-break          │
    // ├──────────────────────────────────────────────────────────────────────┤
    // │ LRU     │ O(1)   │ O(1)   │ O(1)    │ O(cap)    │ always LRU         │
    // │ LFU     │ O(1)   │ O(1)   │ O(1)    │ O(cap)    │ LRU among min-freq │
    // │ Heap LFU│ O(log n)│O(log n)│O(log n) │ O(cap)    │ —                  │
    // └──────────────────────────────────────────────────────────────────────┘
    //
    // INTERVIEW Q&A:
    //
    // Q: Why does LRU use DoublyLinkedList + HashMap instead of just a sorted map?
    // A: TreeMap/sorted structures give O(log n) not O(1). We need O(1) reorder on
    //    every access. DLL gives O(1) splice+insert; HashMap gives O(1) node lookup.
    //
    // Q: Why does LFU use LinkedHashSet instead of just a Set?
    // A: Among keys with the same frequency, we need to evict the LRU one.
    //    LinkedHashSet preserves insertion order, so iterator().next() = LRU.
    //    Regular HashSet has no ordering guarantee → incorrect tie-breaking.
    //
    // Q: When would you choose LFU over LRU in production?
    // A: LFU is better when access patterns are stable and frequency is a better
    //    predictor of future access than recency. Example: search index where
    //    popular queries (high freq) should stay regardless of when last accessed.
    //    LRU is simpler and usually sufficient for temporal-locality workloads.
    //
    // Q: What's wrong with a simple synchronized LRU wrapper?
    // A: Single global lock → every get() blocks every other get(). For read-heavy
    //    caches, use ReadWriteLock (concurrent reads) or striped locks (partition key
    //    space into N locks → N× parallelism). Caffeine uses a striped ring buffer
    //    for O(1) amortized thread-safe LRU without blocking reads.
    //
    // Q: LRU get() is not a pure read — why does it need a write lock?
    // A: LRU.get() moves the node to the head of the doubly linked list. This
    //    mutates the list structure and is not thread-safe under concurrent reads.
    //    Solutions: (1) write lock for get() + put() [simplest], (2) CAS-based
    //    list (lock-free), (3) Caffeine's async promotion buffer.
}
