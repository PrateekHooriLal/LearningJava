package com.java.ds.search;

/**
 * BINARY SEARCH — 4 Essential Variants
 *
 * CONCEPT:
 *   Binary Search works by repeatedly halving the search space.
 *   Precondition: the array must be SORTED (or partially sorted for rotated variant).
 *   At each step we compare the middle element and discard the half that cannot
 *   contain the target. This gives O(log n) time vs O(n) for linear search.
 *
 * THE TEMPLATE (memorize this):
 *   int lo = 0, hi = n - 1;
 *   while (lo <= hi) {
 *     int mid = lo + (hi - lo) / 2;  // NEVER use (lo+hi)/2 — can overflow!
 *     if (arr[mid] == target) return mid;
 *     else if (arr[mid] < target) lo = mid + 1;
 *     else hi = mid - 1;
 *   }
 *   return -1;
 *
 * THE 4 VARIANTS HERE:
 *   1. Standard iterative (LC 704)
 *   2. Recursive
 *   3. Rotated sorted array (LC 33) — one half is always sorted, use that to decide
 *   4. First and last position (LC 34) — find leftmost / rightmost occurrence
 *
 * INTERVIEW FREQUENCY: Binary search is asked in nearly every FAANG screen.
 *   Mastering all 4 variants covers ~80% of binary search problems.
 *
 * COMMON FOLLOW-UP QUESTIONS:
 *   1. LC 33: "What if there are duplicates in the rotated array?" → LC 81 (harder)
 *   2. LC 34: "How would you extend this to find the count?" → hi - lo + 1
 *   3. "Why mid = lo + (hi-lo)/2 and not (lo+hi)/2?"
 *      (lo+hi) can overflow int. lo + (hi-lo)/2 is always safe.
 *   4. "What is the loop invariant?" → target is always in [lo..hi] if it exists.
 *
 * Time: O(log n) for all variants   Space: O(1) iterative, O(log n) recursive stack
 */
public class BinarySearch {

    public static void main(String[] args) {

        System.out.println("=== Variant 1: Standard Iterative Binary Search ===");
        int[] sorted = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        System.out.println(search(sorted, 7));   // Expected: 3
        System.out.println(search(sorted, 1));   // Expected: 0 (first element)
        System.out.println(search(sorted, 19));  // Expected: 9 (last element)
        System.out.println(search(sorted, 6));   // Expected: -1 (not found)
        System.out.println(search(new int[]{5}, 5));  // Expected: 0 (single element)
        System.out.println(search(new int[]{5}, 3));  // Expected: -1

        System.out.println("\n=== Variant 2: Recursive Binary Search ===");
        System.out.println(searchRecursive(sorted, 11, 0, sorted.length - 1)); // Expected: 5
        System.out.println(searchRecursive(sorted, 20, 0, sorted.length - 1)); // Expected: -1

        System.out.println("\n=== Variant 3: Search in Rotated Sorted Array (LC 33) ===");
        // [4,5,6,7,0,1,2] — rotated at index 4; original [0,1,2,4,5,6,7]
        int[] rotated = {4, 5, 6, 7, 0, 1, 2};
        System.out.println(searchRotated(rotated, 0));  // Expected: 4
        System.out.println(searchRotated(rotated, 3));  // Expected: -1
        System.out.println(searchRotated(rotated, 6));  // Expected: 2

        // Rotation at end: [6,7,0,1,2,3,4,5]
        int[] rotated2 = {6, 7, 0, 1, 2, 3, 4, 5};
        System.out.println(searchRotated(rotated2, 1)); // Expected: 3

        // Not rotated (edge case)
        System.out.println(searchRotated(new int[]{1, 3, 5}, 3)); // Expected: 1

        System.out.println("\n=== Variant 4: First and Last Position (LC 34) ===");
        int[] arr = {5, 7, 7, 8, 8, 10};
        int[] range = searchRange(arr, 8);
        System.out.println("[" + range[0] + ", " + range[1] + "]"); // Expected: [3, 4]

        range = searchRange(arr, 7);
        System.out.println("[" + range[0] + ", " + range[1] + "]"); // Expected: [1, 2]

        range = searchRange(arr, 6);
        System.out.println("[" + range[0] + ", " + range[1] + "]"); // Expected: [-1, -1]

        range = searchRange(new int[]{1}, 1);
        System.out.println("[" + range[0] + ", " + range[1] + "]"); // Expected: [0, 0]
    }

    // =========================================================================
    // VARIANT 1: Standard Iterative (LC 704)
    // =========================================================================

    /**
     * Returns the index of target in a sorted array, or -1 if not found.
     *
     * LOOP INVARIANT: If target exists, it is always within [lo, hi].
     * The loop shrinks [lo, hi] until it either finds target or the range is empty.
     *
     * WHY lo <= hi (not lo < hi)?
     *   We need to check when lo == hi (single element remains).
     *   If we used lo < hi, we'd miss that last element.
     */
    public static int search(int[] nums, int target) {
        int lo = 0;
        int hi = nums.length - 1;

        while (lo <= hi) {
            // CRITICAL: use lo + (hi-lo)/2 to prevent integer overflow
            // Example: lo=1_000_000_000, hi=2_000_000_000 → lo+hi overflows int
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] == target) {
                return mid; // Found

            } else if (nums[mid] < target) {
                // Target is in the right half — discard left half including mid
                lo = mid + 1;

            } else {
                // Target is in the left half — discard right half including mid
                hi = mid - 1;
            }
        }

        return -1; // Not found
    }

    // =========================================================================
    // VARIANT 2: Recursive
    // =========================================================================

    /**
     * Recursive binary search. Functionally identical to iterative.
     *
     * WHEN TO USE: When the interviewer specifically asks for recursion, or when
     *   the problem naturally builds on a recursive structure (e.g., BST search).
     *
     * TRADE-OFF: O(log n) stack frames vs O(1) stack for iterative.
     *   For n=10^9, log₂(10^9) ≈ 30 stack frames — negligible, but note it.
     */
    public static int searchRecursive(int[] nums, int target, int lo, int hi) {
        if (lo > hi) return -1; // Base case: search space exhausted

        int mid = lo + (hi - lo) / 2;

        if (nums[mid] == target) return mid;
        if (nums[mid] < target) return searchRecursive(nums, target, mid + 1, hi); // Right half
        return searchRecursive(nums, target, lo, mid - 1);                          // Left half
    }

    // =========================================================================
    // VARIANT 3: Search in Rotated Sorted Array (LC 33)
    // =========================================================================

    /**
     * Finds target in a sorted array that has been rotated at some unknown pivot.
     * No duplicates assumed (see LC 81 for duplicates variant).
     *
     * KEY INSIGHT: Even after rotation, ONE of the two halves [lo..mid] or [mid..hi]
     *   is always sorted (non-rotated). We identify the sorted half, check if
     *   target fits within it, and narrow accordingly.
     *
     * HOW TO IDENTIFY THE SORTED HALF:
     *   If nums[lo] <= nums[mid]: left half [lo..mid] is sorted (no rotation in it)
     *   Else:                     right half [mid..hi] is sorted
     *
     * ONCE WE KNOW WHICH HALF IS SORTED:
     *   Check if target is in the sorted half's range [nums[lo]..nums[mid]] or [nums[mid]..nums[hi]].
     *   If yes → search that half. If no → search the other half.
     *
     * EXAMPLE: [4,5,6,7,0,1,2], target=0
     *   lo=0, hi=6, mid=3: nums[3]=7
     *   nums[lo]=4 <= nums[mid]=7 → left half [4,5,6,7] is sorted
     *   Is 0 in [4,7]? No → search right half [mid+1..hi]
     *   lo=4, hi=6, mid=5: nums[5]=1
     *   nums[lo]=0 <= nums[mid]=1 → left half sorted
     *   Is 0 in [0,1]? Yes → search left half
     *   lo=4, hi=4, mid=4: nums[4]=0 == target → return 4 ✓
     */
    public static int searchRotated(int[] nums, int target) {
        int lo = 0;
        int hi = nums.length - 1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] == target) return mid;

            // Determine which half is sorted
            if (nums[lo] <= nums[mid]) {
                // LEFT half [lo..mid] is sorted

                if (target >= nums[lo] && target < nums[mid]) {
                    // Target is within the sorted left half
                    hi = mid - 1;
                } else {
                    // Target must be in the right half (possibly rotated)
                    lo = mid + 1;
                }
            } else {
                // RIGHT half [mid..hi] is sorted

                if (target > nums[mid] && target <= nums[hi]) {
                    // Target is within the sorted right half
                    lo = mid + 1;
                } else {
                    // Target must be in the left half
                    hi = mid - 1;
                }
            }
        }

        return -1;
    }

    // =========================================================================
    // VARIANT 4: First and Last Position (LC 34)
    // =========================================================================

    /**
     * Returns [firstIndex, lastIndex] of target in a sorted array with duplicates.
     * Returns [-1, -1] if target is not found.
     *
     * APPROACH: Two separate binary searches:
     *   1. findFirst: find the LEFTMOST occurrence — when nums[mid]==target, don't stop;
     *      instead record it and continue searching LEFT (hi = mid - 1).
     *   2. findLast: find the RIGHTMOST occurrence — when nums[mid]==target, record it
     *      and continue searching RIGHT (lo = mid + 1).
     *
     * WHY NOT just find any occurrence and scan left/right?
     *   That degrades to O(n) for arrays like [8,8,8,8,8]. Binary search keeps O(log n).
     */
    public static int[] searchRange(int[] nums, int target) {
        return new int[]{findFirst(nums, target), findLast(nums, target)};
    }

    /**
     * Finds the leftmost (first) index of target.
     * KEY: When we find target at mid, we don't return immediately — we continue
     *      searching left to see if there's an even earlier occurrence.
     */
    private static int findFirst(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int result = -1; // Will be updated each time we find the target

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] == target) {
                result = mid;       // Record this position as a candidate
                hi = mid - 1;       // Keep searching LEFT for an earlier occurrence
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return result;
    }

    /**
     * Finds the rightmost (last) index of target.
     * KEY: When we find target at mid, continue searching right.
     */
    private static int findLast(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int result = -1;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;

            if (nums[mid] == target) {
                result = mid;       // Record this position as a candidate
                lo = mid + 1;       // Keep searching RIGHT for a later occurrence
            } else if (nums[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return result;
    }
}
