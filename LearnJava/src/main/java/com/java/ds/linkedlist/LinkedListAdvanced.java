package com.java.ds.linkedlist;

import java.util.*;

/**
 * LINKED LIST ADVANCED — LRU Cache, LFU Cache, Complex Pointer Problems
 *
 * CONCEPT:
 *   These problems test whether you can combine data structures to achieve
 *   O(1) for operations that seem to require O(n) scanning.
 *
 *   KEY INSIGHT: HashMap gives O(1) LOOKUP; DoublyLinkedList gives O(1) INSERT/DELETE.
 *   Combining them: O(1) lookup + O(1) move-to-position = O(1) total.
 *
 * INTERVIEW ANGLE:
 *   LRU Cache is one of the most common system design interview questions asked
 *   at the coding level. Interviewers test: can you design a real data structure,
 *   handle edge cases (capacity 1, update existing key), and achieve O(1) ops.
 *   LFU Cache is the harder follow-up — requires tracking frequency buckets.
 *
 * REAL-WORLD USE:
 *   - OS page replacement algorithms (LRU is the most common)
 *   - Browser cache (LRU or LFU variant)
 *   - Redis cache eviction policies
 *   - CPU cache replacement
 *   - Java's LinkedHashMap with accessOrder=true is an LRU implementation
 */
public class LinkedListAdvanced {

    // =========================================================================
    // LC 146 — LRU CACHE
    // =========================================================================

    /**
     * LRU Cache: evict the Least Recently Used item when capacity is exceeded.
     *
     * DESIGN:
     *   HashMap<K, Node>  — O(1) get/put by key
     *   Doubly Linked List — O(1) move-to-front (most recent) + remove-from-back (evict)
     *
     * HEAD ↔ [MRU] ↔ ... ↔ [LRU] ↔ TAIL
     * Head and tail are sentinel nodes — they don't hold data.
     * Sentinel nodes simplify edge cases (no null checks for empty list).
     *
     * ON GET(key):
     *   Find in map → move node to front (most recently used) → return value
     *
     * ON PUT(key, val):
     *   If key exists: update value, move to front
     *   If new: create node, add to front, add to map
     *             If over capacity: remove tail node (LRU), remove from map
     *
     * WHY DOUBLY LINKED (not singly):
     *   To REMOVE a node in O(1), we need access to its PREVIOUS node.
     *   With singly linked list, finding the previous node requires O(n) scan.
     *   With doubly linked list + a direct pointer (from HashMap), O(1) removal.
     *
     * Time: O(1) for get and put  Space: O(capacity)
     */
    static class LRUCache {
        private static class Node {
            int key, val;
            Node prev, next;
            Node(int k, int v) { key = k; val = v; }
        }

        private final int capacity;
        private final Map<Integer, Node> map;  // key → node (for O(1) lookup)
        private final Node head, tail;         // sentinels: head=MRU side, tail=LRU side

        public LRUCache(int capacity) {
            this.capacity = capacity;
            map = new HashMap<>();
            head = new Node(0, 0); // dummy head (most recent side)
            tail = new Node(0, 0); // dummy tail (least recent side)
            head.next = tail;      // empty cache: head ↔ tail
            tail.prev = head;
        }

        public int get(int key) {
            if (!map.containsKey(key)) return -1;
            Node node = map.get(key);
            moveToFront(node); // mark as recently used
            return node.val;
        }

        public void put(int key, int val) {
            if (map.containsKey(key)) {
                Node node = map.get(key);
                node.val = val;         // update value
                moveToFront(node);      // mark as recently used
            } else {
                if (map.size() == capacity) {
                    // evict LRU (the node just before tail)
                    Node lru = tail.prev;
                    removeNode(lru);
                    map.remove(lru.key);
                }
                Node newNode = new Node(key, val);
                addToFront(newNode);    // new = most recently used
                map.put(key, newNode);
            }
        }

        // Remove a node from the doubly linked list (O(1) because we have prev/next)
        private void removeNode(Node node) {
            node.prev.next = node.next; // bypass node
            node.next.prev = node.prev;
        }

        // Add a node right after head (most recently used position)
        private void addToFront(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        // Move existing node to front = remove + add to front
        private void moveToFront(Node node) {
            removeNode(node);
            addToFront(node);
        }
    }

    // ALTERNATIVE: Java's LinkedHashMap with accessOrder=true
    // LinkedHashMap(capacity, 0.75f, true) maintains access order automatically.
    // Override removeEldestEntry to implement eviction.
    // INTERVIEW NOTE: mention this exists but implement from scratch to show understanding.

    // =========================================================================
    // LC 460 — LFU CACHE
    // =========================================================================

    /**
     * LFU Cache: evict the Least Frequently Used item. Ties broken by LRU.
     *
     * LFU is HARDER than LRU because we must track per-key frequency
     * AND order within the same frequency.
     *
     * DESIGN (O(1) put/get):
     *   keyMap: Map<K, Node>              — O(1) access to any node
     *   freqMap: Map<freq, LinkedHashSet<K>> — keys grouped by frequency
     *             LinkedHashSet preserves insertion order → LRU within same freq
     *   minFreq: int                      — current minimum frequency (for eviction)
     *
     * ON GET(key):
     *   Find node, increment its frequency:
     *     remove from freqMap[freq], add to freqMap[freq+1]
     *   If removed set becomes empty AND freq == minFreq, increment minFreq.
     *   Return value.
     *
     * ON PUT(key, val):
     *   If key exists: update val, call get logic (increment freq)
     *   If new: create node with freq=1, add to freqMap[1], add to keyMap
     *     If over capacity: evict from freqMap[minFreq] (the FIRST/oldest entry)
     *     minFreq = 1 (new node always has freq=1)
     *
     * WHY LinkedHashSet: preserves insertion order → when same freq, evict oldest = LRU.
     *
     * Time: O(1) for get and put  Space: O(capacity)
     */
    static class LFUCache {
        private static class Node {
            int key, val, freq;
            Node(int k, int v) { key = k; val = v; freq = 1; }
        }

        private final int capacity;
        private int minFreq;
        private final Map<Integer, Node> keyMap;             // key → node
        private final Map<Integer, LinkedHashSet<Integer>> freqMap; // freq → set of keys (LRU order)

        public LFUCache(int capacity) {
            this.capacity = capacity;
            this.minFreq = 0;
            keyMap = new HashMap<>();
            freqMap = new HashMap<>();
        }

        public int get(int key) {
            if (!keyMap.containsKey(key)) return -1;
            Node node = keyMap.get(key);
            incrementFreq(node); // update frequency
            return node.val;
        }

        public void put(int key, int val) {
            if (capacity <= 0) return;

            if (keyMap.containsKey(key)) {
                Node node = keyMap.get(key);
                node.val = val;
                incrementFreq(node); // treat as access
            } else {
                if (keyMap.size() == capacity) {
                    // evict: remove the LRU key from freqMap[minFreq]
                    LinkedHashSet<Integer> minFreqSet = freqMap.get(minFreq);
                    int evictKey = minFreqSet.iterator().next(); // oldest = first inserted
                    minFreqSet.remove(evictKey);
                    keyMap.remove(evictKey);
                }
                Node newNode = new Node(key, val);
                keyMap.put(key, newNode);
                freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
                minFreq = 1; // new node always starts at freq=1
            }
        }

        private void incrementFreq(Node node) {
            int oldFreq = node.freq;
            node.freq++;

            // move from old freq bucket to new freq bucket
            freqMap.get(oldFreq).remove(node.key);
            if (freqMap.get(oldFreq).isEmpty()) {
                freqMap.remove(oldFreq);
                if (minFreq == oldFreq) minFreq++; // min freq no longer valid, increment
            }
            freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node.key);
        }
    }

    // LRU vs LFU comparison:
    // LRU: evict least RECENTLY used (recency only)
    //   Pro: simple, works well for temporal locality
    //   Con: a one-time bulk access pollutes cache with rarely-used items
    //
    // LFU: evict least FREQUENTLY used (frequency, then recency for ties)
    //   Pro: keeps popular items regardless of when last accessed
    //   Con: "cache pollution" for items that were frequent long ago but not recently
    //         → this is why some systems use "aging" (decay old frequencies)

    // =========================================================================
    // LC 138 — COPY LIST WITH RANDOM POINTER
    // =========================================================================

    /**
     * Copy a linked list where each node has both .next and .random pointers.
     * .random can point to any node (or null). Return a deep copy.
     *
     * CHALLENGE: when creating a copy of node A, its .random might point to
     * a node we haven't created yet. We can't set the random pointer until
     * all nodes are created.
     *
     * APPROACH 1 (HashMap, O(n) space):
     *   Pass 1: create all copy nodes, build map: original → copy
     *   Pass 2: set .next and .random using the map
     *
     * APPROACH 2 (O(1) space — interweave):
     *   Pass 1: insert copy of each node right after it: A → A' → B → B' → ...
     *   Pass 2: set copy.random = original.random.next (the interleaved copy)
     *   Pass 3: restore original list, extract copy list
     *
     * Time: O(n)  Space: O(n) HashMap or O(1) interweave
     */
    static class Node {
        int val;
        Node next, random;
        Node(int val) { this.val = val; }
    }

    // HashMap approach (simpler, more readable)
    public static Node copyRandomListHashMap(Node head) {
        if (head == null) return null;
        Map<Node, Node> map = new HashMap<>();

        // Pass 1: create all copy nodes
        Node cur = head;
        while (cur != null) {
            map.put(cur, new Node(cur.val));
            cur = cur.next;
        }

        // Pass 2: set pointers using map (all copies exist now)
        cur = head;
        while (cur != null) {
            map.get(cur).next = map.get(cur.next);     // null-safe: map.get(null) = null
            map.get(cur).random = map.get(cur.random);
            cur = cur.next;
        }
        return map.get(head);
    }

    // O(1) space: interweave approach
    public static Node copyRandomListO1(Node head) {
        if (head == null) return null;

        // Pass 1: A → A' → B → B' → C → C'
        Node cur = head;
        while (cur != null) {
            Node copy = new Node(cur.val);
            copy.next = cur.next; // A'.next = B
            cur.next = copy;      // A.next = A'
            cur = copy.next;      // move to B
        }

        // Pass 2: set random pointers for copies
        cur = head;
        while (cur != null) {
            if (cur.random != null) {
                cur.next.random = cur.random.next; // A'.random = A.random.next = B' (the copy of A.random)
            }
            cur = cur.next.next; // skip to next original node
        }

        // Pass 3: restore original, extract copy list
        cur = head;
        Node copyHead = head.next, copyCur = copyHead;
        while (cur != null) {
            cur.next = cur.next.next;                             // restore original: A.next = B
            copyCur.next = copyCur.next != null ? copyCur.next.next : null; // copy: A'.next = B'
            cur = cur.next;
            copyCur = copyCur.next;
        }
        return copyHead;
    }
    // Q: Why interweave instead of two-pass HashMap?
    // A: HashMap uses O(n) extra space. Interweave embeds the mapping
    //    IN the list structure itself (copy node is always original.next),
    //    achieving O(1) extra space at the cost of code complexity.

    public static void main(String[] args) {
        // LRU Cache
        System.out.println("=== LRU Cache ===");
        LRUCache lru = new LRUCache(2);
        lru.put(1, 1); lru.put(2, 2);
        System.out.println(lru.get(1));  // 1 — 1 is now MRU
        lru.put(3, 3);                   // evicts key 2 (LRU)
        System.out.println(lru.get(2));  // -1 (evicted)
        lru.put(4, 4);                   // evicts key 1 (LRU)
        System.out.println(lru.get(1));  // -1 (evicted)
        System.out.println(lru.get(3));  // 3
        System.out.println(lru.get(4));  // 4

        // LFU Cache
        System.out.println("\n=== LFU Cache ===");
        LFUCache lfu = new LFUCache(2);
        lfu.put(1, 1); lfu.put(2, 2);
        System.out.println(lfu.get(1));  // 1 — key 1 freq=2, key 2 freq=1
        lfu.put(3, 3);                   // evicts key 2 (freq=1, LRU among freq-1)
        System.out.println(lfu.get(2));  // -1 (evicted)
        System.out.println(lfu.get(3));  // 3 — key 3 freq=2, key 1 freq=2
        lfu.put(4, 4);                   // evicts key 1 (freq=2, but older than key 3)
        System.out.println(lfu.get(1));  // -1 (evicted)
        System.out.println(lfu.get(3));  // 3
        System.out.println(lfu.get(4));  // 4
    }
}
