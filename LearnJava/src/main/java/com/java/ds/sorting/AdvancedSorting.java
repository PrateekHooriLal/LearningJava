package com.java.ds.sorting;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Advanced Sorting Algorithms — Beyond QuickSort and MergeSort
 * ============================================================
 *
 * WHY STUDY THESE?
 *   Most interviews ask about O(n log n) sorts. But knowing WHY Java uses TimSort,
 *   WHEN counting/radix sort beats comparison sorts, and HOW external sort works
 *   for system design distinguishes SDE3 candidates.
 *
 * COMPARISON SORT LOWER BOUND:
 *   Any algorithm that only uses comparisons to sort CANNOT do better than O(n log n).
 *   Proof: there are n! possible orderings of n elements. A binary decision tree
 *   needs at least log2(n!) ≈ n*log(n) comparisons to distinguish all cases.
 *   Non-comparison sorts (counting, radix) break this barrier by using element values
 *   directly (not just comparing them).
 *
 * WHEN TO USE WHICH:
 *   TimSort     : general purpose, real-world data, stable, Java's default for objects
 *   QuickSort   : fastest in practice for primitives (Java's Arrays.sort for int[])
 *   CountingSort: integer keys in small range [0..k], O(n+k), stable
 *   RadixSort   : fixed-width integers or strings, O(d*(n+k)), stable
 *   ExternalSort: data doesn't fit in RAM (databases, Hadoop MapReduce)
 *   3-way QSort : many duplicate keys (avoid O(n^2) on [1,1,1,1,1])
 *
 * TIME / SPACE:
 *   ┌──────────────────┬────────────┬────────────┬────────────┬────────┬─────────┐
 *   │ Algorithm        │ Best       │ Average    │ Worst      │ Space  │ Stable? │
 *   ├──────────────────┼────────────┼────────────┼────────────┼────────┼─────────┤
 *   │ TimSort          │ O(n)       │ O(n log n) │ O(n log n) │ O(n)   │ Yes     │
 *   │ MergeSort        │ O(n log n) │ O(n log n) │ O(n log n) │ O(n)   │ Yes     │
 *   │ QuickSort        │ O(n log n) │ O(n log n) │ O(n^2)     │ O(logn)│ No      │
 *   │ HeapSort         │ O(n log n) │ O(n log n) │ O(n log n) │ O(1)   │ No      │
 *   │ InsertionSort    │ O(n)       │ O(n^2)     │ O(n^2)     │ O(1)   │ Yes     │
 *   │ CountingSort     │ O(n+k)     │ O(n+k)     │ O(n+k)     │ O(k)   │ Yes     │
 *   │ RadixSort (LSD)  │ O(d*(n+k)) │ O(d*(n+k)) │ O(d*(n+k)) │ O(n+k) │ Yes     │
 *   └──────────────────┴────────────┴────────────┴────────────┴────────┴─────────┘
 *
 * INTERVIEW ANGLE:
 *   "Why does Java use TimSort for Collections.sort but dual-pivot QuickSort for int[]?"
 *   "When would counting sort outperform QuickSort?"
 *   "Describe external sort for a file with 1 billion integers and 1GB RAM."
 *   "How do you avoid O(n^2) QuickSort on nearly-sorted or all-duplicate arrays?"
 *
 * REAL-WORLD:
 *   TimSort: Python's sort, Android's sort, Java's sort (all use the same algorithm)
 *   External sort: database ORDER BY on large tables, Hadoop reducer input sorting
 *   Radix sort: network packet classification, string sorting in databases
 * ============================================================
 */
public class AdvancedSorting {

    // =========================================================
    // SECTION 1: TimSort
    // =========================================================

    /*
     * TIMSORT DEEP DIVE:
     *
     * TimSort = Merge Sort + Insertion Sort, optimized for real-world data.
     * Invented by Tim Peters for Python (2002). Adopted by Java (Java 7+) for Object sorting.
     *
     * Java uses TimSort for:
     *   - Collections.sort()
     *   - Arrays.sort(Object[])
     *   - List.sort()
     * Java uses DUAL-PIVOT QuickSort for:
     *   - Arrays.sort(int[]) / Arrays.sort(long[]) / etc. (primitives)
     *   Why dual-pivot for primitives? Stable sort doesn't matter for primitives
     *   (no "identity" to preserve), and QuickSort has better cache behavior.
     *
     * ALGORITHM:
     *   1. Find "runs": already-sorted (or reverse-sorted) subsequences in the input.
     *      A run is a maximal sequence of consecutive elements that are already ordered.
     *      If the run is descending, reverse it to make it ascending (O(n) with two-pointer).
     *   2. MinRun: if a run is shorter than minRun (32-64, computed from input size),
     *      extend it using InsertionSort to reach minRun length.
     *      InsertionSort is O(n^2) but extremely fast on nearly-sorted small arrays
     *      due to cache locality and low constant factor.
     *   3. Push runs onto a stack. Merge runs that satisfy the merge invariants:
     *      Invariant: for stack [..., X, Y, Z]:  |Z| > |Y| + |X|  AND  |Y| > |X|
     *      This ensures O(n log n) regardless of input distribution.
     *   4. Galloping mode: when merging two runs, if one is consistently "winning"
     *      (contributing many elements in a row), switch to binary search to skip ahead.
     *      This gives O(n) on already-sorted input (runs are maximal → single merge).
     *
     * WHY STABLE?
     *   Stability means equal elements maintain their original relative order.
     *   Important for: sorting records by multiple keys (sort by name then by age →
     *   records with same age retain their name-sorted order if age-sort is stable).
     *   Merge sort is inherently stable (we copy left before right for equal elements).
     *
     * GOTCHA — Comparator consistency with equals():
     *   The Comparator contract requires: if compare(a,b)==0 then a.equals(b) (ideally).
     *   Violating this can cause TreeMap/TreeSet to behave incorrectly (treats them as equal
     *   when they're not, or vice versa). TimSort won't throw but behavior is undefined.
     *   Example bug: Comparator.comparingInt(String::length) — "abc" and "xyz" compare as 0
     *   but "abc".equals("xyz") is false. TreeSet would only store one of them!
     */
    static void explainTimSort() {
        System.out.println("\n=== TimSort Simulation ===");

        // TimSort shines on "partially sorted" data
        int[] partlySorted = {1, 2, 3, 4, 5, 9, 8, 7, 6, 10, 11, 12, 13}; // two runs
        // TimSort detects: run [1..5,9], reverse-run [8,7,6] → run [6,7,8,9]
        // Then merges → O(n) in this case

        Integer[] boxed = Arrays.stream(partlySorted).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed); // Uses TimSort (Object array)
        System.out.println("Sorted (TimSort): " + Arrays.toString(boxed));

        // Demonstrating stability matters:
        String[] words = {"banana", "apple", "cherry", "avocado", "apricot"};
        // Sort by first character — stable sort preserves original order among equals
        Arrays.sort(words, Comparator.comparingInt(s -> s.charAt(0)));
        System.out.println("Stable sort by first char: " + Arrays.toString(words));
        // "apple" and "avocado" and "apricot" all start with 'a' — original relative order preserved
    }

    // =========================================================
    // SECTION 2: Counting Sort
    // =========================================================

    /**
     * KEY INSIGHT: Counting sort works by counting how many elements have each value,
     * then using those counts to determine the final position of each element.
     *
     * WHEN TO USE:
     *   - Elements are non-negative integers in range [0..k]
     *   - k (range) is not much larger than n (count) — otherwise too much space
     *   - Example: sort student grades 0-100, sort ages 0-150, sort characters 0-255
     *
     * ALGORITHM STEPS:
     *   1. Count[i] = number of elements equal to i
     *   2. Cumulative: Count[i] += Count[i-1]  (Count[i] = number of elements <= i)
     *   3. Reverse scan original array: output[--Count[arr[j]]] = arr[j]
     *      Reverse scan ensures STABILITY (equal elements in original order)
     *
     * WHY REVERSE SCAN FOR STABILITY?
     *   After cumulative step, Count[v] tells us "the rightmost position for value v".
     *   Scanning right-to-left places the last occurrence first at the rightmost slot,
     *   maintaining relative order of equal elements.
     *
     * COMPLEXITY: O(n + k) time, O(n + k) space.
     * When k = O(n): effectively O(n) — linear sort!
     *
     * GOTCHA: Doesn't work for negative integers without offset transformation.
     *         Range [min..max]: offset = -min, effective k = max - min + 1.
     */
    static int[] countingSort(int[] arr, int k) {
        // k = max value in arr (range is [0..k])
        int[] count = new int[k + 1];

        // Step 1: Count occurrences
        for (int x : arr) count[x]++;

        // Step 2: Cumulative counts (prefix sum)
        // count[i] = how many elements <= i
        for (int i = 1; i <= k; i++) count[i] += count[i - 1];

        // Step 3: Build output — REVERSE scan for stability
        int[] output = new int[arr.length];
        for (int j = arr.length - 1; j >= 0; j--) {
            output[--count[arr[j]]] = arr[j]; // place at correct position, decrement count
        }
        return output;
    }

    // =========================================================
    // SECTION 3: Radix Sort (LSD)
    // =========================================================

    /**
     * KEY INSIGHT: Radix sort avoids comparisons entirely. It sorts integers digit by digit,
     * from least significant (LSD) to most significant (MSD), using counting sort per digit.
     *
     * LSD (Least Significant Digit first):
     *   - Sort by ones place → sort by tens place → sort by hundreds place → ...
     *   - Each pass is a stable counting sort on that digit.
     *   - Stability is CRUCIAL: after sorting by tens, we must preserve the ones-order
     *     within each group. That's why we use stable counting sort per digit.
     *
     * COMPLEXITY: O(d * (n + k)) where:
     *   d = number of digits (log_radix(maxValue))
     *   k = radix (base 10 → k=10, base 256 → k=256)
     *   For 32-bit integers with radix 256: d=4 passes of O(n+256) = O(n) effectively.
     *
     * WHEN TO USE:
     *   - Sorting large arrays of integers or fixed-length strings
     *   - When d * (n + k) < n * log(n) [typically when n is large, d is small]
     *   - Network packet classification (sort by destination IP address)
     *
     * GOTCHA: Radix sort typically only works on non-negative integers directly.
     *         For signed integers, separate negatives from positives, sort each, recombine.
     *
     * LSD vs MSD:
     *   LSD: iterate from rightmost digit, build full sorted array. Simpler, always O(d*(n+k)).
     *   MSD: start from leftmost digit, recurse (like quicksort by digit). Can be faster for
     *        strings with common prefixes (stops early), but harder to implement correctly.
     */
    static void radixSort(int[] arr) {
        int max = Arrays.stream(arr).max().getAsInt();

        // Process each digit position: exp = 1, 10, 100, 1000, ...
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSortByDigit(arr, exp);
        }
    }

    private static void countingSortByDigit(int[] arr, int exp) {
        int n = arr.length;
        int[] output = new int[n];
        int[] count = new int[10]; // digits 0-9

        // Count occurrences of each digit at 'exp' position
        for (int x : arr) count[(x / exp) % 10]++;

        // Cumulative count
        for (int i = 1; i < 10; i++) count[i] += count[i - 1];

        // Build output (reverse scan for stability)
        for (int j = n - 1; j >= 0; j--) {
            int digit = (arr[j] / exp) % 10;
            output[--count[digit]] = arr[j];
        }

        // Copy back
        System.arraycopy(output, 0, arr, 0, n);
    }

    // =========================================================
    // SECTION 4: External Sort (Algorithm Explanation)
    // =========================================================

    /*
     * EXTERNAL SORT — For data that exceeds available RAM:
     *
     * PROBLEM: Sort 100GB of data with only 1GB of RAM.
     *
     * PHASE 1: Run Generation
     *   1. Read as much data as fits in RAM (e.g., 1GB chunk).
     *   2. Sort it in memory using any in-memory sort (QuickSort, TimSort, etc.).
     *   3. Write the sorted "run" to disk.
     *   4. Repeat for all chunks → you now have 100 sorted runs of ~1GB each on disk.
     *
     *   Optimization: "Replacement Selection" can create runs LARGER than RAM on average
     *   (1.8x RAM size), reducing total number of runs.
     *
     * PHASE 2: K-Way Merge
     *   1. Open all 100 sorted run files simultaneously.
     *   2. Use a PriorityQueue<RunReader> where RunReader holds the current minimum value
     *      from its run, the run index, and a buffer for the next value to read.
     *   3. Repeatedly: poll the minimum from the heap, write it to output, advance that run.
     *   4. When a run is exhausted, remove it from the heap.
     *   5. Continue until all runs are exhausted.
     *
     * COMPLEXITY:
     *   Phase 1: O(N/M * M*log(M)) = O(N*log(M)) where N=total data, M=RAM size
     *   Phase 2: O(N*log(K)) where K=number of runs
     *   Total: O(N*log(N)) just like comparison sort — but with I/O cost per element.
     *
     * OPTIMIZATION: Multi-level merging
     *   If K (number of runs) is very large, merge in levels:
     *   Level 0: 100 runs → Level 1: merge 10 runs at a time → 10 mega-runs
     *   Level 1: merge 10 mega-runs → 1 sorted output
     *   This is how databases handle ORDER BY on large tables.
     *
     * The PriorityQueue<RunReader> is O(log K) per element, which is why K-way merge
     * with a heap is so efficient — each element participates in just one heap operation.
     *
     * INTERVIEW QUESTION: "System design: sort 1TB of log files by timestamp with 4GB RAM"
     * ANSWER: External sort.
     *   1. Split into ~256 chunks of 4GB, sort each in-memory, write to disk → 256 runs.
     *   2. K=256 way merge with min-heap of (timestamp, runIndex) pairs.
     *   3. Output is a single sorted file.
     *   Throughput bottleneck: disk I/O speed. Can parallelize phase 1 across multiple machines.
     */

    /**
     * K-way merge simulation (in-memory, no file I/O for demonstration).
     * In production, each "chunk" would be a disk-backed sorted file.
     */
    static int[] kWayMerge(int[][] sortedChunks) {
        // Each entry in heap: [value, chunkIndex, elementIndex]
        PriorityQueue<int[]> heap = new PriorityQueue<>(Comparator.comparingInt(e -> e[0]));

        int totalSize = 0;
        for (int i = 0; i < sortedChunks.length; i++) {
            if (sortedChunks[i].length > 0) {
                heap.offer(new int[]{sortedChunks[i][0], i, 0}); // value, which chunk, position in chunk
                totalSize += sortedChunks[i].length;
            }
        }

        int[] result = new int[totalSize];
        int idx = 0;

        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            int val = top[0], chunkIdx = top[1], elemIdx = top[2];
            result[idx++] = val;

            // Advance to next element in the same chunk
            int nextElemIdx = elemIdx + 1;
            if (nextElemIdx < sortedChunks[chunkIdx].length) {
                heap.offer(new int[]{sortedChunks[chunkIdx][nextElemIdx], chunkIdx, nextElemIdx});
            }
            // If chunk exhausted, nothing to add — heap naturally shrinks
        }
        return result;
    }

    // =========================================================
    // SECTION 5: 3-Way QuickSort (Bentley-McIlroy)
    // =========================================================

    /**
     * KEY INSIGHT: Standard 2-way QuickSort degrades to O(n^2) on arrays with many duplicates.
     * Example: [1,1,1,1,1,1,1] — every partition puts all elements on one side.
     *
     * 3-WAY PARTITION (Dutch National Flag applied to QuickSort):
     *   Partition array into three regions: [< pivot | == pivot | > pivot]
     *   Elements equal to pivot are already in their final position — skip them in recursion!
     *   If all elements are equal: O(n) one pass, done. No recursion needed.
     *
     * This is exactly how Java's Arrays.sort(int[]) (Dual-Pivot QuickSort) handles duplicates.
     * Dual-pivot uses TWO pivots, creating four regions: [<p1 | p1<=x<=p2 | >p2]
     *
     * BENTLEY-MCILROY PARTITION:
     *   Moves equal elements to the array ends during partition, then swaps them to middle.
     *   Elegant and cache-efficient. Used in production QuickSort implementations.
     *
     * COMPLEXITY:
     *   Best/Average: O(n log n)
     *   When all elements are equal: O(n) — single pass, no recursive calls
     *   Worst case: O(n^2) only if elements are nearly distinct AND pivot choice is bad
     */
    static void threeWayQuickSort(int[] arr, int lo, int hi) {
        if (lo >= hi) return;

        // 3-way partition: [lo..lt-1] < pivot, [lt..gt] == pivot, [gt+1..hi] > pivot
        int pivot = arr[lo + (hi - lo) / 2]; // middle element as pivot (reduces sorted-array issue)
        int lt = lo;  // next position for element < pivot
        int gt = hi;  // next position for element > pivot
        int i  = lo;  // current scan position

        while (i <= gt) {
            if      (arr[i] < pivot) swap(arr, lt++, i++); // move to left region
            else if (arr[i] > pivot) swap(arr, i, gt--);   // move to right region (don't advance i)
            else                     i++;                   // equal to pivot, skip
        }
        // CRITICAL: only recurse on [lo..lt-1] and [gt+1..hi] — skip the equal region!
        threeWayQuickSort(arr, lo, lt - 1);
        threeWayQuickSort(arr, gt + 1, hi);
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }

    // =========================================================
    // DEMOS & TESTS
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Advanced Sorting Algorithms                ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        explainTimSort();

        // Counting Sort test
        System.out.println("\n=== Counting Sort ===");
        int[] grades = {85, 92, 78, 95, 88, 72, 95, 100, 85, 78};
        int[] sorted = countingSort(grades, 100);
        System.out.println("Grades sorted: " + Arrays.toString(sorted));

        // Radix Sort test
        System.out.println("\n=== Radix Sort ===");
        int[] nums = {170, 45, 75, 90, 802, 24, 2, 66};
        radixSort(nums);
        System.out.println("Radix sorted: " + Arrays.toString(nums));

        // K-Way Merge (external sort simulation)
        System.out.println("\n=== K-Way Merge (External Sort simulation) ===");
        int[][] chunks = {
            {1, 4, 7, 10},
            {2, 5, 8, 11},
            {3, 6, 9, 12}
        };
        int[] merged = kWayMerge(chunks);
        System.out.println("Merged: " + Arrays.toString(merged));

        // 3-Way QuickSort on all-duplicates
        System.out.println("\n=== 3-Way QuickSort on duplicates ===");
        int[] dups = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 5, 5, 5, 5};
        threeWayQuickSort(dups, 0, dups.length - 1);
        System.out.println("3-way QSort: " + Arrays.toString(dups));

        // Demonstrate 3-way handles all-same in O(n)
        int[] allSame = {7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
        threeWayQuickSort(allSame, 0, allSame.length - 1);
        System.out.println("All same sorted: " + Arrays.toString(allSame)); // [7,7,7...]

        System.out.println("\n=== All sorting tests complete ===");
    }
}
