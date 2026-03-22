package com.java.ds.heap;

import java.util.*;

/**
 * HEAP / PRIORITY QUEUE PROBLEMS — Top K, K-th Element, Median
 *
 * CONCEPT:
 *   A Heap is a complete binary tree satisfying the heap property:
 *   - MIN-HEAP: parent ≤ children → root is always the MINIMUM element.
 *   - MAX-HEAP: parent ≥ children → root is always the MAXIMUM element.
 *
 *   Java's PriorityQueue is a min-heap by default.
 *   For max-heap: new PriorityQueue<>(Collections.reverseOrder())
 *
 * KEY OPERATIONS:
 *   offer(x) — insert, O(log n)
 *   peek()   — read min (or max for max-heap), O(1)
 *   poll()   — remove and return min (or max), O(log n)
 *
 * WHEN TO USE A HEAP:
 *   Whenever you see "top K", "kth largest/smallest", "running median",
 *   "merge K sorted lists" → think heap.
 *
 * PROBLEMS COVERED:
 *   1. Kth Largest Element in Array — LC 215
 *   2. Top K Frequent Elements — LC 347
 *   3. K Closest Points to Origin — LC 973
 *   4. Merge K Sorted Lists — LC 23
 *   5. Find Median from Data Stream — LC 295
 *
 * INTERVIEW FREQUENCY: Very high at FAANG.
 *
 * COMMON INTERVIEW QUESTIONS:
 *   1. "What's the time complexity of building a heap from n elements?"
 *      O(n) — not O(n log n). Heapify-down from all internal nodes takes O(n) total.
 *   2. "Why use a min-heap of size k to find the kth LARGEST?"
 *      The min-heap maintains the k largest elements seen so far. Its minimum = kth largest.
 *      If new element > heap.peek() → replace the minimum → always keep top k.
 *   3. "What is the median-of-two-heaps technique?"
 *      Max-heap for lower half, min-heap for upper half. Balance sizes to get median in O(1).
 */
public class HeapProblems {

    public static void main(String[] args) {

        System.out.println("=== LC 215: Kth Largest Element ===");
        System.out.println(findKthLargest(new int[]{3,2,1,5,6,4}, 2));    // Expected: 5
        System.out.println(findKthLargest(new int[]{3,2,3,1,2,4,5,5,6}, 4)); // Expected: 4
        System.out.println(findKthLargest(new int[]{1}, 1));               // Expected: 1

        System.out.println("\n=== LC 347: Top K Frequent Elements ===");
        System.out.println(Arrays.toString(topKFrequent(new int[]{1,1,1,2,2,3}, 2))); // [1,2]
        System.out.println(Arrays.toString(topKFrequent(new int[]{1}, 1)));            // [1]

        System.out.println("\n=== LC 973: K Closest Points to Origin ===");
        int[][] result = kClosest(new int[][]{{1,3},{-2,2}}, 1);
        System.out.println("Closest: " + Arrays.deepToString(result)); // [[-2,2]]
        result = kClosest(new int[][]{{3,3},{5,-1},{-2,4}}, 2);
        System.out.println("Closest 2: " + Arrays.deepToString(result)); // [[3,3],[-2,4]]

        System.out.println("\n=== LC 23: Merge K Sorted Lists ===");
        ListNode l1 = list(1, 4, 5);
        ListNode l2 = list(1, 3, 4);
        ListNode l3 = list(2, 6);
        System.out.println(printList(mergeKLists(new ListNode[]{l1, l2, l3}))); // 1→1→2→3→4→4→5→6

        System.out.println("\n=== LC 295: Find Median from Data Stream ===");
        MedianFinder mf = new MedianFinder();
        mf.addNum(1); System.out.println("After 1: " + mf.findMedian());         // 1.0
        mf.addNum(2); System.out.println("After 1,2: " + mf.findMedian());       // 1.5
        mf.addNum(3); System.out.println("After 1,2,3: " + mf.findMedian());     // 2.0
        mf.addNum(4); System.out.println("After 1,2,3,4: " + mf.findMedian());   // 2.5
    }

    // =========================================================================
    // LC 215 — Kth Largest Element in an Array
    // =========================================================================

    /**
     * Returns the kth largest element in an unsorted array.
     *
     * APPROACH: Min-heap of size k.
     *   Maintain exactly k elements in the heap (the k largest seen so far).
     *   The heap's minimum (peek) = the kth largest.
     *
     * WHY MIN-HEAP for kth LARGEST?
     *   Think of it as: "I want to keep the top k candidates. If a new element beats
     *   the weakest candidate (heap.peek()), swap it in." The weakest of the top-k = kth largest.
     *
     * ALTERNATIVE: QuickSelect (O(n) average) — ask if time is critical.
     *   Heap approach: O(n log k), simpler to implement correctly in an interview.
     *
     * Time: O(n log k)  Space: O(k)
     */
    public static int findKthLargest(int[] nums, int k) {
        // Min-heap: smallest element at the top
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int num : nums) {
            minHeap.offer(num); // Add element

            if (minHeap.size() > k) {
                // Heap exceeded k elements → evict the smallest (not in top k)
                minHeap.poll();
            }
        }

        // After processing all elements, heap contains the k largest
        // The minimum of those k largest = kth largest overall
        return minHeap.peek();
    }

    // =========================================================================
    // LC 347 — Top K Frequent Elements
    // =========================================================================

    /**
     * Returns the k most frequent elements.
     *
     * APPROACH:
     *   1. Build frequency map: O(n)
     *   2. Min-heap of size k, ordered by frequency.
     *      Keep only k elements — the k most frequent.
     *      Poll (remove least frequent) when size > k.
     *
     * BUCKET SORT ALTERNATIVE: O(n) time — create buckets where bucket[i] = list of elements with freq i.
     *   Then scan buckets from high to low to collect top k. Ask interviewer which to use.
     *
     * Time: O(n log k)  Space: O(n + k)
     */
    public static int[] topKFrequent(int[] nums, int k) {
        // Step 1: Count frequencies
        Map<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) freq.put(n, freq.getOrDefault(n, 0) + 1);

        // Step 2: Min-heap ordered by frequency (least frequent at top)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>((a, b) -> freq.get(a) - freq.get(b));

        for (int key : freq.keySet()) {
            minHeap.offer(key);
            if (minHeap.size() > k) {
                minHeap.poll(); // Remove least frequent — not in top k
            }
        }

        // Step 3: Extract result
        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = minHeap.poll();
        }
        return result;
    }

    // =========================================================================
    // LC 973 — K Closest Points to Origin
    // =========================================================================

    /**
     * Returns the k points closest to the origin (0,0).
     * Distance = sqrt(x²+y²), but we compare x²+y² (avoid sqrt for performance).
     *
     * APPROACH: Max-heap of size k, ordered by distance descending.
     *   Keep k closest points. When a closer point arrives, evict the farthest.
     *   The heap's maximum (peek) = the farthest of the k closest = kth closest.
     *
     * WHY MAX-HEAP for kth CLOSEST?
     *   We want to remove the farthest when a closer point arrives.
     *   Max-heap gives us the farthest at the top for O(log k) removal.
     *
     * Time: O(n log k)  Space: O(k)
     */
    public static int[][] kClosest(int[][] points, int k) {
        // Max-heap: farthest point at top (so we can evict it when we find a closer one)
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> (b[0]*b[0] + b[1]*b[1]) - (a[0]*a[0] + a[1]*a[1]) // Descending distance
        );

        for (int[] point : points) {
            maxHeap.offer(point);
            if (maxHeap.size() > k) {
                maxHeap.poll(); // Remove the farthest point (it's not in the top k closest)
            }
        }

        return maxHeap.toArray(new int[0][]);
    }

    // =========================================================================
    // LC 23 — Merge K Sorted Lists
    // =========================================================================

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    /**
     * Merges k sorted linked lists into one sorted list.
     *
     * APPROACH: Min-heap containing one node from each list.
     *   1. Initialize heap with head of each list.
     *   2. Poll the minimum node, add it to result.
     *   3. If that node has a next, add next to heap.
     *   Heap always contains at most k elements.
     *
     * WHY HEAP OVER MERGE-TWO-LISTS REPEATEDLY?
     *   Merge-two-at-a-time: k rounds × O(n) each = O(kn). Poor when k is large.
     *   Heap: O(n log k) where n = total nodes. Each of n nodes is inserted/polled once.
     *
     * Time: O(n log k)  Space: O(k)
     */
    public static ListNode mergeKLists(ListNode[] lists) {
        // Min-heap ordered by node value
        PriorityQueue<ListNode> heap = new PriorityQueue<>((a, b) -> a.val - b.val);

        // Initialize with head of each non-empty list
        for (ListNode node : lists) {
            if (node != null) heap.offer(node);
        }

        ListNode dummy = new ListNode(0); // Sentinel head for result list
        ListNode tail = dummy;

        while (!heap.isEmpty()) {
            ListNode min = heap.poll();    // Extract smallest node
            tail.next = min;               // Append to result
            tail = tail.next;              // Advance result tail

            if (min.next != null) {
                heap.offer(min.next);      // Add next node from same list
            }
        }

        return dummy.next; // dummy.next is the actual head of merged list
    }

    // =========================================================================
    // LC 295 — Find Median from Data Stream
    // =========================================================================

    /**
     * Supports two operations:
     *   addNum(int num) — add a number
     *   findMedian() — return the current median
     *
     * TWO-HEAP TECHNIQUE:
     *   Split the stream into two halves:
     *   - maxHeap: lower half (max-heap → gives max of lower half in O(1))
     *   - minHeap: upper half (min-heap → gives min of upper half in O(1))
     *
     *   INVARIANT: |maxHeap.size() - minHeap.size()| <= 1
     *              All elements in maxHeap <= all elements in minHeap.
     *
     *   MEDIAN:
     *   - If sizes equal: median = (maxHeap.peek() + minHeap.peek()) / 2.0
     *   - If maxHeap is larger: median = maxHeap.peek()
     *   - (We never let minHeap be larger by convention.)
     *
     * Time: O(log n) per addNum, O(1) per findMedian  Space: O(n)
     */
    static class MedianFinder {
        private PriorityQueue<Integer> maxHeap; // Lower half — max at top
        private PriorityQueue<Integer> minHeap; // Upper half — min at top

        MedianFinder() {
            maxHeap = new PriorityQueue<>(Collections.reverseOrder()); // max-heap
            minHeap = new PriorityQueue<>();                            // min-heap
        }

        public void addNum(int num) {
            // Step 1: Add to maxHeap first
            maxHeap.offer(num);

            // Step 2: Ensure max of lower half <= min of upper half
            // If maxHeap's max > minHeap's min, rebalance
            if (!minHeap.isEmpty() && maxHeap.peek() > minHeap.peek()) {
                minHeap.offer(maxHeap.poll()); // Move maxHeap's max to minHeap
            }

            // Step 3: Balance sizes — maxHeap can be equal or 1 larger than minHeap
            if (maxHeap.size() > minHeap.size() + 1) {
                minHeap.offer(maxHeap.poll()); // Too many in lower half → move one up
            } else if (minHeap.size() > maxHeap.size()) {
                maxHeap.offer(minHeap.poll()); // Too many in upper half → move one down
            }
        }

        public double findMedian() {
            if (maxHeap.size() == minHeap.size()) {
                // Even total elements: median = average of the two middle elements
                return (maxHeap.peek() + minHeap.peek()) / 2.0;
            }
            // Odd total elements: median = top of the larger heap (maxHeap by convention)
            return maxHeap.peek();
        }
    }

    // Helpers for test
    static ListNode list(int... vals) {
        ListNode dummy = new ListNode(0), cur = dummy;
        for (int v : vals) { cur.next = new ListNode(v); cur = cur.next; }
        return dummy.next;
    }

    static String printList(ListNode head) {
        StringBuilder sb = new StringBuilder();
        while (head != null) { sb.append(head.val).append(head.next != null ? "→" : ""); head = head.next; }
        return sb.toString();
    }
}
