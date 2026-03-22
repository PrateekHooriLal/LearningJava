package com.java.ds.array;

import java.util.*;

/**
 * PIVOT TECHNIQUES — Three Distinct Contexts
 *
 * CONCEPT:
 *   "Pivot" appears in THREE unrelated algorithmic contexts. Knowing which
 *   context you're in determines the correct algorithm:
 *
 *   1. QuickSort / QuickSelect pivot:
 *      Element chosen to partition array into "< pivot" and "> pivot" halves.
 *      Goal: sort or find Kth element in O(n) average.
 *
 *   2. Rotated array pivot:
 *      The index where sorted order breaks in a rotated sorted array.
 *      Goal: binary search by determining which half is sorted.
 *
 *   3. Equilibrium / balance pivot:
 *      Index where left sum equals right sum (or meets some balance condition).
 *      Goal: find balance point in O(n) using prefix sums.
 *
 * WHEN TO RECOGNIZE:
 *   - "Sort / Kth largest" + O(n) average        → QuickSort / QuickSelect
 *   - "Sorted array rotated at unknown point"    → Rotated array pivot
 *   - "Left sum == right sum" / "Product except self" → Equilibrium pivot
 *
 * TIME COMPLEXITY:  QuickSort O(n log n) avg, O(n²) worst
 *                   QuickSelect O(n) avg, O(n²) worst
 *                   Rotated search O(log n)
 *                   Equilibrium O(n)
 * SPACE COMPLEXITY: O(log n) for QuickSort (call stack); O(1) for others
 *
 * INTERVIEW ANGLE:
 *   - QuickSort: always mention randomized pivot and O(n²) worst case
 *   - Rotated: explain WHY one half is always sorted — the key insight
 *   - Equilibrium: prefix sum reduces O(n²) brute force to O(n)
 *
 * REAL-WORLD USE:
 *   - Java's Arrays.sort() on primitives uses dual-pivot Quicksort (Yaroslavskiy)
 *   - Database query planners use partition-based O(n) median finding
 *   - Load balancers find equilibrium points for request distribution
 */
public class PivotTechniques {

    // =========================================================================
    // CONTEXT 1: QUICKSORT PIVOT (LOMUTO AND HOARE SCHEMES)
    // =========================================================================
    //
    // LOMUTO PARTITION:
    //   - Pivot = last element
    //   - Wall pointer p starts at lo-1
    //   - Scan i from lo to hi-1: if arr[i] <= pivot, swap(++p, i)
    //   - Final swap: swap(p+1, hi) → pivot lands at p+1 (its sorted position)
    //   - Simpler to implement, but ~3x more swaps than Hoare
    //
    // HOARE PARTITION:
    //   - Pivot = first element (or middle)
    //   - Two pointers: i starts left, j starts right
    //   - i moves right until arr[i] >= pivot; j moves left until arr[j] <= pivot
    //   - Swap arr[i] and arr[j]; repeat until i >= j
    //   - Return j (NOT the pivot's final position — Hoare doesn't place pivot)
    //   - Fewer swaps (~3x fewer than Lomuto); pivot is NOT at its final index
    //
    // KEY DIFFERENCE:
    //   Lomuto: pivot ends up at exact sorted position → QuickSelect uses this
    //   Hoare:  pivot is NOT necessarily at sorted position → only use for full sort
    //
    // RANDOMIZED PIVOT: swap arr[random] with arr[hi] (Lomuto) or arr[lo] (Hoare)
    //   before partitioning. Makes O(n²) worst case astronomically unlikely.
    //   Java's Arrays.sort does NOT randomize → vulnerable to adversarial input.

    /**
     * QuickSort — Lomuto Partition Scheme
     *
     * After each partition call, pivot is at its FINAL sorted position.
     * Recurse on both halves (not including the pivot).
     *
     * Time: O(n log n) avg, O(n²) worst  Space: O(log n) call stack avg
     */
    public void quickSortLomuto(int[] arr, int lo, int hi) {
        if (lo >= hi) return;
        int p = partitionLomuto(arr, lo, hi); // p = pivot's final sorted index
        quickSortLomuto(arr, lo, p - 1);      // sort left half (exclude pivot)
        quickSortLomuto(arr, p + 1, hi);      // sort right half (exclude pivot)
    }

    private int partitionLomuto(int[] arr, int lo, int hi) {
        // randomize to avoid O(n²) on sorted input
        int randIdx = lo + (int)(Math.random() * (hi - lo + 1));
        swap(arr, randIdx, hi);               // bring random pivot to end

        int pivot = arr[hi];
        int wall = lo - 1;                    // wall: everything left of wall is ≤ pivot

        for (int i = lo; i < hi; i++) {
            if (arr[i] <= pivot) {
                wall++;
                swap(arr, wall, i);           // move element ≤ pivot to left of wall
            }
        }
        swap(arr, wall + 1, hi);              // place pivot at its sorted position
        return wall + 1;                      // return pivot's final index
    }

    /**
     * QuickSort — Hoare Partition Scheme
     *
     * Hoare's original 1962 scheme. Fewer swaps (~3x fewer than Lomuto).
     * IMPORTANT: partition returns index j, NOT pivot's sorted position.
     * Recurse includes j in left half: quickSort(lo, p) and quickSort(p+1, hi).
     *
     * Time: O(n log n) avg  Space: O(log n) call stack avg
     */
    public void quickSortHoare(int[] arr, int lo, int hi) {
        if (lo >= hi) return;
        int p = partitionHoare(arr, lo, hi);
        quickSortHoare(arr, lo, p);     // include p in left half (Hoare's contract)
        quickSortHoare(arr, p + 1, hi);
    }

    private int partitionHoare(int[] arr, int lo, int hi) {
        int pivot = arr[lo + (hi - lo) / 2]; // middle element as pivot (reduces sorted-input cost)
        int i = lo - 1, j = hi + 1;

        while (true) {
            do { i++; } while (arr[i] < pivot);  // advance i until arr[i] >= pivot
            do { j--; } while (arr[j] > pivot);  // advance j until arr[j] <= pivot
            if (i >= j) return j;                 // pointers crossed → return partition point
            swap(arr, i, j);                      // arr[i] >= pivot and arr[j] <= pivot → swap
        }
    }

    private void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }

    // Q: Lomuto vs Hoare — which to use in interview?
    // A: Lomuto for QUICKSELECT (pivot lands at sorted position → easy to check).
    //    Hoare for QUICKSORT only (fewer swaps, but partition return value is confusing).
    //    In most interviews, Lomuto is preferred for clarity.

    // Q: QuickSort worst case O(n²) — when does it happen?
    // A: When pivot is always min or max element: already sorted/reverse sorted array
    //    with last-element pivot. Randomized pivot makes this O(n²) prob = 1/n!.
    //    MergeSort is O(n log n) GUARANTEED but needs O(n) extra space.

    // =========================================================================
    // CONTEXT 2: ROTATED SORTED ARRAY PIVOT
    // =========================================================================
    //
    // KEY INSIGHT: a rotated sorted array has ONE "break point" where arr[i] > arr[i+1].
    // The array consists of TWO sorted halves. Binary search works by identifying
    // WHICH HALF IS SORTED and then determining which side the target is in.
    //
    // INVARIANT at each step: one of the two halves [lo..mid] or [mid..hi] is SORTED.
    //   If arr[lo] <= arr[mid]: LEFT half is sorted → target in [lo, mid)?
    //   Else: RIGHT half is sorted → target in (mid, hi]?
    //
    // GOTCHA: with duplicates (LC 154), arr[lo] == arr[mid] is ambiguous —
    //   we can't tell which half is sorted. Must shrink lo++ (linear scan) in that case,
    //   degrading worst case to O(n).
    //
    // FINDING THE ROTATION POINT (minimum element):
    //   If arr[mid] > arr[hi]: rotation point is in RIGHT half → lo = mid + 1
    //   Else: rotation point is in LEFT half or mid → hi = mid
    //   Result: lo == hi == index of minimum element (rotation point)

    /**
     * LC 33 — Search in Rotated Sorted Array (no duplicates)
     *
     * Binary search with a twist: determine which half is sorted, then check
     * if target falls in the sorted half. If yes, search there; else other half.
     *
     * Time: O(log n)  Space: O(1)
     */
    public int search(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2; // avoid overflow vs (lo+hi)/2

            if (nums[mid] == target) return mid;

            if (nums[lo] <= nums[mid]) {
                // LEFT half [lo..mid] is sorted
                if (nums[lo] <= target && target < nums[mid]) {
                    hi = mid - 1; // target in sorted left half
                } else {
                    lo = mid + 1; // target in right half
                }
            } else {
                // RIGHT half [mid..hi] is sorted
                if (nums[mid] < target && target <= nums[hi]) {
                    lo = mid + 1; // target in sorted right half
                } else {
                    hi = mid - 1; // target in left half
                }
            }
        }
        return -1;
    }
    // INTERVIEW FOLLOW-UP: "Why nums[lo] <= nums[mid] and not nums[lo] < nums[mid]?"
    // A: When lo == mid (array of length 1 or 2), we need = to correctly classify.

    /**
     * LC 153 — Find Minimum in Rotated Sorted Array (no duplicates)
     *
     * KEY INSIGHT: minimum is at the rotation point where arr[i] < arr[i-1].
     * Binary search: if arr[mid] > arr[hi], min is in right half; else left.
     *
     * The unsorted half always contains the rotation point (minimum).
     *
     * Time: O(log n)  Space: O(1)
     */
    public int findMin(int[] nums) {
        int lo = 0, hi = nums.length - 1;

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] > nums[hi]) {
                // mid is in the LARGER left portion → rotation (min) is in right half
                lo = mid + 1;
            } else {
                // mid is in the smaller right portion → min is in [lo..mid]
                hi = mid; // include mid, don't do hi = mid-1
            }
        }
        return nums[lo]; // lo == hi == index of minimum
    }
    // GOTCHA: use hi = mid (not mid-1) because mid might BE the minimum.
    //         Always think: "is mid a candidate for the answer?"

    /**
     * LC 154 — Find Minimum in Rotated Sorted Array II (WITH duplicates)
     *
     * GOTCHA: when nums[mid] == nums[hi], we can't tell which half has the minimum.
     *   Example: [3,3,1,3] → mid=1 (val 3), hi=3 (val 3) — min could be either side.
     *   Workaround: shrink hi-- to eliminate one duplicate per iteration.
     *   This degrades worst case to O(n) (e.g., all equal elements [1,1,1,1]).
     *
     * Time: O(log n) avg, O(n) worst  Space: O(1)
     */
    public int findMinWithDuplicates(int[] nums) {
        int lo = 0, hi = nums.length - 1;

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] > nums[hi]) {
                lo = mid + 1;
            } else if (nums[mid] < nums[hi]) {
                hi = mid;
            } else {
                hi--; // nums[mid] == nums[hi]: can't determine side, shrink hi safely
            }
        }
        return nums[lo];
    }

    // =========================================================================
    // CONTEXT 3: EQUILIBRIUM / BALANCE PIVOT
    // =========================================================================
    //
    // KEY INSIGHT: "balance point" = index where left sum equals right sum.
    // Brute force: for each index i, sum left and right → O(n²).
    // Prefix sum reduces this to O(n): total - nums[i] - leftSum == leftSum
    //   → total - nums[i] == 2 * leftSum
    //   → leftSum == (total - nums[i]) / 2
    //
    // PATTERN: precompute total sum, then walk left-to-right accumulating leftSum.
    //   At each index: rightSum = total - leftSum - nums[i]
    //   If leftSum == rightSum → found pivot index.
    //
    // GOTCHA: don't require total to be even. Just check leftSum == rightSum
    //         (equivalently: 2*leftSum + nums[i] == total). No division needed.

    /**
     * LC 724 — Find Pivot Index
     *
     * Pivot index: sum of elements to left == sum of elements to right.
     *
     * Time: O(n)  Space: O(1)
     */
    public int pivotIndex(int[] nums) {
        int total = 0;
        for (int n : nums) total += n;

        int leftSum = 0;
        for (int i = 0; i < nums.length; i++) {
            // rightSum = total - nums[i] - leftSum
            if (leftSum == total - nums[i] - leftSum) return i; // leftSum == rightSum
            leftSum += nums[i]; // add current element to left sum for next iteration
        }
        return -1;
    }

    /**
     * LC 238 — Product of Array Except Self
     *
     * KEY INSIGHT: product at index i = (product of all left of i) * (product of all right of i).
     * Compute left products in a forward pass, then right products in a backward pass.
     * This is a "prefix product" pattern — the equilibrium pivot extended to products.
     *
     * Time: O(n)  Space: O(1) extra (output array not counted)
     */
    public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];

        // forward pass: result[i] = product of all elements LEFT of i
        result[0] = 1;
        for (int i = 1; i < n; i++) {
            result[i] = result[i - 1] * nums[i - 1]; // left product up to i
        }

        // backward pass: multiply result[i] by product of all elements RIGHT of i
        int rightProduct = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= rightProduct;  // result[i] = leftProduct[i] * rightProduct[i]
            rightProduct *= nums[i];    // update right product for next iteration
        }
        return result;
    }
    // Q: Why O(1) extra space if we use an output array?
    // A: The problem states the output array doesn't count toward space complexity.
    //    Original O(n) space solution uses separate left[] and right[] arrays.
    //    This optimization folds both into result[] + a running rightProduct variable.

    // Q: What if zeros are in the array?
    // A: The algorithm handles zeros naturally. If nums[i]=0, all elements in result
    //    get 0 except result[i] (which gets the product of all others — potentially non-zero).
    //    Edge case: two zeros → all products are 0 including the zero positions.

    // =========================================================================
    // COMBINED PROBLEM: QuickSelect + Pivot
    // =========================================================================

    /**
     * LC 215 — Kth Largest Element (QuickSelect using Lomuto partition)
     *
     * KEY INSIGHT: Lomuto partition places pivot at its EXACT sorted index.
     * If sorted index == target → done. Only recurse into one side → O(n) avg.
     *
     * This is different from full QuickSort which recurses on BOTH sides.
     *
     * Time: O(n) average  Space: O(1) ignoring call stack
     */
    public int findKthLargest(int[] nums, int k) {
        // Kth largest = element at index (n-k) in sorted ascending order
        return quickSelect(nums, 0, nums.length - 1, nums.length - k);
    }

    private int quickSelect(int[] nums, int lo, int hi, int target) {
        if (lo == hi) return nums[lo];
        int p = partitionLomuto(nums, lo, hi); // p = pivot's sorted position

        if (p == target) return nums[p];           // found it
        else if (p < target) return quickSelect(nums, p + 1, hi, target);
        else return quickSelect(nums, lo, p - 1, target);
    }

    // =========================================================================
    // PIVOT CONTEXT DISAMBIGUATION GUIDE
    // =========================================================================
    //
    // Problem signal                           → Context
    // ───────────────────────────────────────────────────────────────────────
    // "Sort array" / "Kth largest/smallest"   → QuickSort / QuickSelect pivot
    // "Search in rotated sorted array"        → Rotated array pivot
    // "Find rotation point / minimum"         → Rotated array pivot
    // "Left sum equals right sum"             → Equilibrium pivot
    // "Product of array except self"          → Prefix/suffix product (equilibrium)
    //
    // COMMON INTERVIEW TRAP: "Find the pivot in a rotated array" might sound like
    // QuickSort pivot. Clarify by asking: "Is the array rotated from a sorted state?"
    // If yes → rotated array binary search. If no → partition for sorting.

    public static void main(String[] args) {
        PivotTechniques pt = new PivotTechniques();

        // --- QuickSort ---
        System.out.println("=== QuickSort ===");
        int[] arr1 = {3, 6, 8, 10, 1, 2, 1};
        pt.quickSortLomuto(arr1, 0, arr1.length - 1);
        System.out.println(Arrays.toString(arr1)); // [1,1,2,3,6,8,10]

        int[] arr2 = {3, 6, 8, 10, 1, 2, 1};
        pt.quickSortHoare(arr2, 0, arr2.length - 1);
        System.out.println(Arrays.toString(arr2)); // [1,1,2,3,6,8,10]

        // --- Rotated Array ---
        System.out.println("\n=== Rotated Array Pivot ===");
        System.out.println(pt.search(new int[]{4,5,6,7,0,1,2}, 0));     // 4
        System.out.println(pt.search(new int[]{4,5,6,7,0,1,2}, 3));     // -1
        System.out.println(pt.findMin(new int[]{3,4,5,1,2}));            // 1
        System.out.println(pt.findMinWithDuplicates(new int[]{2,2,2,0,1})); // 0

        // --- Equilibrium ---
        System.out.println("\n=== Equilibrium Pivot ===");
        System.out.println(pt.pivotIndex(new int[]{1,7,3,6,5,6}));  // 3
        System.out.println(pt.pivotIndex(new int[]{1,2,3}));         // -1
        System.out.println(Arrays.toString(
            pt.productExceptSelf(new int[]{1,2,3,4}))); // [24,12,8,6]

        // --- QuickSelect ---
        System.out.println("\n=== QuickSelect (Kth Largest) ===");
        System.out.println(pt.findKthLargest(new int[]{3,2,1,5,6,4}, 2)); // 5
    }
}
