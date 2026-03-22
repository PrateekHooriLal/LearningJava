package com.java.ds.heap;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Heap / PriorityQueue — Advanced Patterns
 * ============================================================
 *
 * HEAP BASICS REVIEW:
 *   A heap is a COMPLETE binary tree with the heap property:
 *   MIN-HEAP: every parent <= its children (root = minimum)
 *   MAX-HEAP: every parent >= its children (root = maximum)
 *   Java's PriorityQueue is a MIN-HEAP by default.
 *   For MAX-HEAP: new PriorityQueue<>(Comparator.reverseOrder())
 *
 * HEAP OPERATIONS:
 *   offer(e)  / add(e)   : O(log n) — add element, bubble up (sift up)
 *   poll()               : O(log n) — remove root (min/max), sift down last element
 *   peek()               : O(1)     — view root without removing
 *   contains(e)          : O(n)     — linear scan (heap is NOT sorted for contains)
 *   remove(e)            : O(n)     — find e (O(n)), then sift up/down (O(log n))
 *   size()               : O(1)
 *
 * GOTCHA: Java PriorityQueue is not thread-safe. Use PriorityBlockingQueue for concurrency.
 * GOTCHA: PriorityQueue iterator does NOT traverse in sorted order. Use while(!pq.isEmpty()) poll().
 * GOTCHA: When using custom objects, define Comparator carefully. Inconsistent comparator = heap violation.
 *
 * WHEN TO USE:
 *   "Find/maintain kth largest/smallest efficiently" → heap of size k
 *   "K-way merge of sorted lists" → min-heap of size k (one element per list)
 *   "Median from stream" → two heaps (max-heap left half, min-heap right half)
 *   "Task scheduling by priority/deadline" → PriorityQueue
 *   "Shortest path" → Dijkstra's algorithm uses a min-heap
 *
 * TIME / SPACE:
 *   Heapify n elements: O(n) [not O(n log n) — proven by geometric series analysis]
 *   K-way merge N total elements: O(N log K)
 *   Median stream add/get: O(log n) / O(1)
 *   Top-K from N elements: O(N log K) with min-heap of size K
 *                          O(N) with QuickSelect (average, no guarantee)
 *
 * INTERVIEW ANGLE:
 *   "Merge K sorted lists — what's the time complexity?"
 *   "How does the two-heap median trick work?"
 *   "Top-K: heap vs QuickSelect — when to prefer which?"
 *   "Why is heapify O(n) but inserting n elements one by one is O(n log n)?"
 *
 * REAL-WORLD:
 *   - OS process scheduling: priority queues for CPU scheduling
 *   - Dijkstra's algorithm: used in GPS routing, network routing protocols
 *   - Huffman encoding: build optimal prefix code using min-heap
 *   - Event simulation: process events in chronological order
 *   - Streaming top-K: media recommendation systems, trending topics
 * ============================================================
 */
public class HeapDeepDive {

    // =========================================================
    // PROBLEM 1: K-Way Merge (LC 23 — Merge K Sorted Lists)
    // =========================================================

    /**
     * KEY INSIGHT: Maintain a min-heap of size K, one element per list.
     * Always process the globally smallest element by:
     *   1. Poll the minimum from the heap.
     *   2. Add the next element from the same list.
     * This ensures we always know the global minimum with O(log K) poll.
     *
     * COMPLEXITY:
     *   Each of the N total elements is pushed into and popped from the heap once.
     *   Each heap operation = O(log K).
     *   Total: O(N log K). This is optimal — we must process all N elements.
     *
     * vs naive approach: concatenate all lists, sort → O(N log N). Worse when K << N.
     * vs pairwise merge: merge list 1+2, then +3, then +4... = O(NK). Much worse.
     *
     * HEAP ENTRY: int[] {value, listIndex, elementIndex}
     *   We store all three to efficiently retrieve the next element from the same list.
     *
     * INTERVIEW Q: "What if the lists are on disk and don't fit in memory?"
     * A: External sort! Same algorithm — each "list" is a sorted file on disk.
     *    Buffer reads from disk to avoid per-element I/O.
     */
    static int[] mergeKSortedArrays(int[][] arrays) {
        // Each entry: [value, arrayIndex, positionInArray]
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));

        int totalSize = 0;
        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i].length > 0) {
                heap.offer(new int[]{arrays[i][0], i, 0}); // add first element of each array
                totalSize += arrays[i].length;
            }
        }

        int[] result = new int[totalSize];
        int idx = 0;

        while (!heap.isEmpty()) {
            int[] entry = heap.poll();       // extract global minimum
            int val      = entry[0];
            int arrIdx   = entry[1];
            int elemIdx  = entry[2];

            result[idx++] = val;

            int nextElemIdx = elemIdx + 1;
            if (nextElemIdx < arrays[arrIdx].length) {
                // Add the next element from the same array
                heap.offer(new int[]{arrays[arrIdx][nextElemIdx], arrIdx, nextElemIdx});
            }
        }
        return result;
    }

    // =========================================================
    // PROBLEM 2: Median from Data Stream (LC 295) — TWO HEAP TRICK
    // =========================================================

    /**
     * KEY INSIGHT: Maintain two heaps that split the data into two halves:
     *   maxHeap (left half): contains the smaller half of all numbers
     *   minHeap (right half): contains the larger half of all numbers
     *
     * INVARIANTS:
     *   1. All elements in maxHeap <= all elements in minHeap.
     *      (maxHeap.peek() <= minHeap.peek())
     *   2. |maxHeap.size() - minHeap.size()| <= 1.
     *      (sizes differ by at most 1)
     *   3. If sizes differ, maxHeap has one more element (we "favor" the left).
     *
     * MEDIAN CALCULATION:
     *   If equal sizes: median = (maxHeap.peek() + minHeap.peek()) / 2.0
     *   If maxHeap is larger: median = maxHeap.peek() (the single middle element)
     *
     * ADD ELEMENT PROCEDURE (maintains invariants):
     *   Step 1: Always push to maxHeap first.
     *     (This handles the "all elements in maxHeap <= all in minHeap" invariant
     *      by routing through maxHeap.peek() which is the current max of left half.)
     *   Step 2: Balance sizes:
     *     If maxHeap.size() > minHeap.size() + 1: move maxHeap.peek() to minHeap.
     *     If minHeap.size() > maxHeap.size():      move minHeap.peek() to maxHeap.
     *
     * WAIT — why always push to maxHeap first even for large numbers?
     *   Assume new number = 100, and maxHeap has [5,3,1]. After pushing 100 to maxHeap,
     *   maxHeap.peek() = 100 (the max). Then we move 100 to minHeap. Invariant 1 restored.
     *   Alternatively: check if new > maxHeap.peek(), push to minHeap directly.
     *   Both work. "Always push to left first" is simpler to code correctly.
     *
     * COMPLEXITY: O(log n) per add, O(1) per findMedian.
     *   vs Sorted Array: O(n) insert, O(1) median. Heap wins for frequent inserts.
     *   vs BST: O(log n) insert, O(log n) median. Comparable, but heap is simpler.
     */
    static class MedianFinder {
        // maxHeap: left half (smaller numbers), peek = median candidate
        private final PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        // minHeap: right half (larger numbers), peek = median candidate
        private final PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        void addNum(int num) {
            maxHeap.offer(num);  // Step 1: always offer to maxHeap first

            // Step 2a: Ensure maxHeap.top <= minHeap.top (invariant 1)
            if (!minHeap.isEmpty() && maxHeap.peek() > minHeap.peek()) {
                minHeap.offer(maxHeap.poll()); // move max of left to right half
            }

            // Step 2b: Balance sizes (invariant 2 & 3)
            if (maxHeap.size() > minHeap.size() + 1) {
                minHeap.offer(maxHeap.poll()); // left too big → move to right
            } else if (minHeap.size() > maxHeap.size()) {
                maxHeap.offer(minHeap.poll()); // right too big → move to left
            }
        }

        double findMedian() {
            if (maxHeap.size() == minHeap.size()) {
                return (maxHeap.peek() + (double) minHeap.peek()) / 2.0;
            }
            return maxHeap.peek(); // maxHeap has one more (invariant 3)
        }
    }

    // =========================================================
    // PROBLEM 3: Task Scheduler (LC 621)
    // =========================================================

    /**
     * KEY INSIGHT: The minimum time is determined by the MOST FREQUENT task.
     * If the most frequent task appears maxFreq times, it needs (maxFreq - 1) "gaps"
     * between each execution. Each gap is of length n (cooldown period).
     * Other tasks fill these gaps; if gaps remain unfilled, we need idle time.
     *
     * FORMULA (O(1) approach):
     *   Let maxFreq = max frequency of any task.
     *   Let countMax = number of tasks with frequency == maxFreq.
     *   Minimum intervals = max(tasks.length, (maxFreq - 1) * (n + 1) + countMax)
     *
     * WHY THIS FORMULA:
     *   (maxFreq-1) gaps, each of size (n+1) [the task itself + n cooldown slots].
     *   Plus countMax tasks at the very end (they fill the last group without a gap after them).
     *   But if tasks.length > this formula, we don't need any idle time — tasks fill all slots.
     *
     * SIMULATION with max-heap (O(n log n)):
     *   1. Count frequency of each task → put in max-heap.
     *   2. Simulate time slots. For each "round" of n+1 slots:
     *      a. Pop up to n+1 tasks from heap (execute highest frequency first).
     *      b. Decrement their count. If count > 0, push back.
     *      c. If remaining tasks < n+1 slots, add idle time for unfilled slots.
     *   3. Continue until heap is empty.
     *
     * INTERVIEW: Know both the O(1) formula AND the simulation. Formula for efficiency,
     * simulation for "explain your reasoning step by step."
     */
    static int leastInterval(char[] tasks, int n) {
        // O(1) formula approach
        int[] freq = new int[26];
        for (char t : tasks) freq[t - 'A']++;
        Arrays.sort(freq);
        int maxFreq = freq[25];
        int countMax = 0;
        for (int f : freq) if (f == maxFreq) countMax++;

        // Either all tasks fill the slots (no idle), or we need the formula
        return Math.max(tasks.length, (maxFreq - 1) * (n + 1) + countMax);
    }

    // Simulation with max-heap (shows the reasoning)
    static int leastIntervalSimulation(char[] tasks, int n) {
        int[] freq = new int[26];
        for (char t : tasks) freq[t - 'A']++;

        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        for (int f : freq) if (f > 0) maxHeap.offer(f);

        int time = 0;
        while (!maxHeap.isEmpty()) {
            List<Integer> temp = new ArrayList<>();
            int slots = n + 1; // each "round" has n+1 slots

            while (slots > 0 && !maxHeap.isEmpty()) {
                int remaining = maxHeap.poll() - 1;
                if (remaining > 0) temp.add(remaining);
                slots--;
                time++;
            }

            maxHeap.addAll(temp); // push tasks with remaining work back
            if (!maxHeap.isEmpty()) {
                time += slots; // add idle time for unused slots in this round
            }
        }
        return time;
    }

    // =========================================================
    // PROBLEM 4: Smallest Range Covering K Lists (LC 632)
    // =========================================================

    /**
     * KEY INSIGHT: Like K-way merge, but instead of extracting and moving forward,
     * we also track the CURRENT MAXIMUM across all lists.
     *
     * The current "range" = [heap.peek() (min), currentMax].
     * We advance the list that contributes the MINIMUM (because advancing any other
     * list would potentially reduce the max, but would also remove the current minimum —
     * and we can only shrink the range by reducing the gap, which means moving the min UP).
     *
     * ALGORITHM:
     *   1. Initialize: add first element of each list to min-heap. currentMax = max of firsts.
     *   2. While heap is not empty:
     *      a. Poll minimum element (= heap.peek()). Range = [min, currentMax].
     *      b. Update best range if current is smaller.
     *      c. Advance the list that contributed the minimum.
     *      d. If next element exists: offer to heap, update currentMax if needed.
     *      e. If list exhausted: STOP (we can't maintain one element from every list).
     *
     * WHY STOP WHEN A LIST IS EXHAUSTED?
     *   The range MUST include at least one element from every list. If a list has no more
     *   elements, we can't include it in any range → stop.
     *
     * COMPLEXITY: O(N log K) where N = total elements across all K lists.
     */
    static int[] smallestRange(int[][] lists) {
        // Heap: [value, listIndex, elementIndex]
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));
        int currentMax = Integer.MIN_VALUE;

        // Initialize with first element of each list
        for (int i = 0; i < lists.length; i++) {
            if (lists[i].length == 0) return new int[]{0, 0}; // edge: empty list
            heap.offer(new int[]{lists[i][0], i, 0});
            currentMax = Math.max(currentMax, lists[i][0]);
        }

        int[] bestRange = {heap.peek()[0], currentMax}; // initial range

        while (!heap.isEmpty()) {
            int[] entry = heap.poll();
            int min = entry[0], listIdx = entry[1], elemIdx = entry[2];

            // Update best range: current range is [min, currentMax]
            if (currentMax - min < bestRange[1] - bestRange[0]) {
                bestRange = new int[]{min, currentMax};
            }

            // Advance the list that contributed the minimum
            int nextElemIdx = elemIdx + 1;
            if (nextElemIdx >= lists[listIdx].length) {
                break; // this list is exhausted → can't maintain one element from every list
            }
            int nextVal = lists[listIdx][nextElemIdx];
            heap.offer(new int[]{nextVal, listIdx, nextElemIdx});
            currentMax = Math.max(currentMax, nextVal); // max can only increase or stay same
        }
        return bestRange;
    }

    // =========================================================
    // PROBLEM 5: Top K Frequent Elements (LC 347)
    // =========================================================

    /**
     * KEY INSIGHT: Two approaches — O(n log k) heap, or O(n) bucket sort.
     *
     * APPROACH 1 — MIN-HEAP OF SIZE K:
     *   Count frequencies. Iterate over entries. Maintain a min-heap of size k.
     *   If heap size > k: poll (remove least frequent). This keeps top-k most frequent.
     *   Final heap contains the k most frequent elements.
     *   O(n log k) time. Better when k << n.
     *
     * APPROACH 2 — BUCKET SORT (O(n)):
     *   Count frequencies (max frequency = n for n elements).
     *   Create buckets array of size n+1: buckets[freq] = list of elements with that frequency.
     *   Collect elements from high-frequency bucket down to low until we have k elements.
     *   O(n) time, O(n) space.
     *   Better than heap approach asymptotically. Preferred for INTERVIEWS when asked for O(n).
     *
     * INTERVIEW Q: "Can you solve this in O(n)?"
     * A: Yes, bucket sort. Frequency range is [1..n], so we create n+1 buckets.
     */
    static int[] topKFrequent(int[] nums, int k) {
        // Count frequencies
        Map<Integer, Integer> freq = new HashMap<>();
        for (int x : nums) freq.merge(x, 1, Integer::sum);

        // Bucket sort: index = frequency, value = list of numbers with that frequency
        @SuppressWarnings("unchecked")
        List<Integer>[] buckets = new List[nums.length + 1];
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            int f = e.getValue();
            if (buckets[f] == null) buckets[f] = new ArrayList<>();
            buckets[f].add(e.getKey());
        }

        // Collect top-k from highest frequency bucket downward
        int[] result = new int[k];
        int idx = 0;
        for (int f = buckets.length - 1; f >= 1 && idx < k; f--) {
            if (buckets[f] != null) {
                for (int num : buckets[f]) {
                    if (idx < k) result[idx++] = num;
                }
            }
        }
        return result;
    }

    // Min-heap approach for comparison:
    static int[] topKFrequentHeap(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int x : nums) freq.merge(x, 1, Integer::sum);

        // Min-heap of size k by frequency: smallest frequency at top
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap =
            new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) minHeap.poll(); // remove least frequent
        }

        return minHeap.stream().mapToInt(Map.Entry::getKey).toArray();
    }

    // =========================================================
    // PROBLEM 6: Find K Pairs with Smallest Sums (LC 373) — Lazy Heap Expansion
    // =========================================================

    /**
     * KEY INSIGHT: "Lazy expansion" — don't load all pairs into the heap upfront.
     * Only load (nums1[0], nums2[0]) initially. When we pop a pair (i, j),
     * we add the next candidates: (i+1, j) and (i, j+1).
     *
     * Wait — this can add duplicates! Better approach: initially add ALL pairs
     * (nums1[0], nums2[j]) for j in [0..k-1]. When (i, j) is popped, add (i+1, j).
     *
     * WHY: nums1 and nums2 are sorted. The smallest sum involving nums1[i] and nums2[j]
     * has the next candidates as (i+1, j) — same position in nums2, next in nums1.
     * The (i, j+1) candidate is covered when j was the same and we initialized with j.
     *
     * This is the SAME pattern as K-way merge:
     *   nums2 provides k "lists". List j = {nums1[0]+nums2[j], nums1[1]+nums2[j], ...}
     *   K-way merge of these sorted lists using min-heap.
     *
     * COMPLEXITY: O(k log k) — at most k elements in heap at any time, poll k times.
     */
    static List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
        List<List<Integer>> result = new ArrayList<>();
        if (nums1.length == 0 || nums2.length == 0 || k == 0) return result;

        // Heap: [sum, i index in nums1, j index in nums2]
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));

        // Seed heap: all pairs (nums1[0], nums2[j]) for j in [0, min(k, nums2.length))
        for (int j = 0; j < Math.min(k, nums2.length); j++) {
            heap.offer(new int[]{nums1[0] + nums2[j], 0, j});
        }

        while (!heap.isEmpty() && result.size() < k) {
            int[] entry = heap.poll();
            int i = entry[1], j = entry[2];
            result.add(List.of(nums1[i], nums2[j]));

            // Only expand along nums1 dimension (nums2 dimension covered by initialization)
            if (i + 1 < nums1.length) {
                heap.offer(new int[]{nums1[i + 1] + nums2[j], i + 1, j});
            }
        }
        return result;
    }

    // =========================================================
    // MAIN: Tests & Demos
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Heap / PriorityQueue Deep Dive             ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // K-Way Merge
        System.out.println("\n=== K-Way Merge ===");
        int[][] arrays = {{1,4,7},{2,5,8},{3,6,9}};
        System.out.println("Merged: " + Arrays.toString(mergeKSortedArrays(arrays)));
        // Expected: [1,2,3,4,5,6,7,8,9]

        // Median from Stream
        System.out.println("\n=== Median from Data Stream ===");
        MedianFinder mf = new MedianFinder();
        mf.addNum(1); System.out.println("After 1: " + mf.findMedian());       // 1.0
        mf.addNum(2); System.out.println("After 1,2: " + mf.findMedian());     // 1.5
        mf.addNum(3); System.out.println("After 1,2,3: " + mf.findMedian());   // 2.0
        mf.addNum(4); System.out.println("After 1,2,3,4: " + mf.findMedian()); // 2.5
        mf.addNum(5); System.out.println("After 1,2,3,4,5: " + mf.findMedian()); // 3.0

        // Task Scheduler
        System.out.println("\n=== Task Scheduler ===");
        System.out.println("leastInterval([A,A,A,B,B,B], n=2): "
            + leastInterval(new char[]{'A','A','A','B','B','B'}, 2)); // 8
        System.out.println("leastInterval([A,A,A,B,B,B], n=0): "
            + leastInterval(new char[]{'A','A','A','B','B','B'}, 0)); // 6
        System.out.println("Simulation matches: "
            + (leastIntervalSimulation(new char[]{'A','A','A','B','B','B'}, 2) == 8)); // true

        // Smallest Range
        System.out.println("\n=== Smallest Range Covering K Lists ===");
        int[][] lists = {{4,10,15,24,26},{0,9,12,20},{5,18,22,30}};
        System.out.println("Smallest range: " + Arrays.toString(smallestRange(lists)));
        // Expected: [20,24]

        // Top K Frequent
        System.out.println("\n=== Top K Frequent Elements ===");
        System.out.println("topKFrequent([1,1,1,2,2,3], k=2): "
            + Arrays.toString(topKFrequent(new int[]{1,1,1,2,2,3}, 2))); // [1,2]
        System.out.println("topKFrequent([1], k=1): "
            + Arrays.toString(topKFrequent(new int[]{1}, 1))); // [1]

        // K Pairs Smallest Sums
        System.out.println("\n=== K Pairs with Smallest Sums ===");
        System.out.println("kSmallestPairs([1,7,11], [2,4,6], k=3): "
            + kSmallestPairs(new int[]{1,7,11}, new int[]{2,4,6}, 3));
        // Expected: [[1,2],[1,4],[1,6]]

        System.out.println("\n=== All heap tests complete ===");
    }
}
