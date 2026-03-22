package com.java.ds.sorting;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Binary Search — The Most Underestimated Topic in Interviews
 * ============================================================
 *
 * WHY THIS IS HIGH VALUE:
 *   Binary search appears in ~30% of medium/hard LeetCode problems — often disguised.
 *   The trick is recognizing WHEN binary search applies: any time you have a
 *   monotonic predicate (condition that is false for small values, true for large, or vice versa).
 *
 * THE UNIFIED TEMPLATE (most important thing to internalize):
 *   All binary search variants reduce to:
 *     "Find the FIRST position where condition is TRUE."
 *   - lo = smallest possible answer
 *   - hi = largest possible answer
 *   - while (lo < hi): mid = lo + (hi-lo)/2; if feasible(mid) → hi=mid; else → lo=mid+1
 *   - Return lo (= first true position)
 *   This template always terminates and never misses the answer.
 *
 * COMMON MISTAKES (gotchas):
 *   1. Integer overflow: (lo+hi)/2 overflows when lo+hi > Integer.MAX_VALUE.
 *      Fix: lo + (hi-lo)/2   (equivalent, no overflow)
 *   2. Infinite loop: when lo < hi and you set hi = mid-1 (possible miss) or lo = mid (possible inf loop)
 *   3. Off-by-one: carefully decide whether boundary is inclusive or exclusive in each direction
 *   4. Wrong invariant: sometimes you want lo<=hi (classic), sometimes lo<hi (template variant)
 *
 * TIME / SPACE:
 *   All binary search variants: O(log n) time, O(1) space
 *   Binary search on answer: O(log(answerRange) * feasibilityCheckCost)
 *     Koko: O(log(maxPile) * n),  Ship: O(log(sum) * n),  SplitArray: O(log(sum) * n)
 *
 * INTERVIEW ANGLE:
 *   "Given this problem, what's the search space?" — key question before coding
 *   "Is the predicate monotonic?" — if yes, binary search on answer
 *   Always state: "I'll binary search on the answer. The search space is [lo, hi].
 *    The feasibility check is: can we achieve mid? If yes, try smaller; if no, try larger."
 *
 * REAL-WORLD:
 *   - Database B-tree: repeated binary search through sorted disk pages
 *   - git bisect: binary search through commits to find regression
 *   - Load balancer capacity planning: binary search on server count
 *   - Feature flags / A/B test sizing: binary search on traffic percentage
 * ============================================================
 */
public class BinarySearchVariants {

    // =========================================================
    // SECTION 1: Classic Binary Search
    // =========================================================

    /**
     * KEY INSIGHT: Classic binary search requires array to be SORTED.
     * Three-way comparison: <, ==, > the target.
     * Loop invariant: if target exists, it is in [lo..hi] at all times.
     * Terminates when lo > hi (target absent) or arr[mid] == target.
     *
     * GOTCHA: lo + (hi-lo)/2 NOT (lo+hi)/2 — avoids integer overflow.
     * For hi = Integer.MAX_VALUE and lo = 0: (0 + MAX_VALUE)/2 is fine, but
     * if lo = 1_000_000_000 and hi = 2_000_000_000: lo+hi = 3_000_000_000 overflows int!
     */
    static int classicBinarySearch(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2; // safe midpoint — no overflow
            if      (arr[mid] == target) return mid;
            else if (arr[mid]  < target) lo = mid + 1; // target in right half
            else                         hi = mid - 1; // target in left half
        }
        return -1; // not found
    }

    // =========================================================
    // SECTION 2: First Occurrence
    // =========================================================

    /**
     * KEY INSIGHT: "First occurrence" = find LEFTMOST index where arr[idx] == target.
     * Template: when condition met, save result and go LEFT (hi = mid - 1).
     * When condition not met, go RIGHT (lo = mid + 1).
     * Result variable holds the last successful position.
     *
     * Equivalently using the unified template:
     *   Condition: arr[mid] >= target
     *   First true position where arr[mid] >= target.
     *   If arr[result] == target → found. Else → absent.
     */
    static int firstOccurrence(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1, result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) {
                result = mid;    // candidate found, but look LEFT for earlier occurrence
                hi = mid - 1;   // shrink right boundary
            } else if (arr[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    // =========================================================
    // SECTION 3: Last Occurrence
    // =========================================================

    /**
     * KEY INSIGHT: Mirror of first occurrence.
     * When condition met, save result and go RIGHT (lo = mid + 1).
     * "Last true" pattern.
     */
    static int lastOccurrence(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1, result = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) {
                result = mid;    // candidate found, but look RIGHT for later occurrence
                lo = mid + 1;   // shrink left boundary
            } else if (arr[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    // =========================================================
    // SECTION 4: Search Insert Position (LC 35)
    // =========================================================

    /**
     * KEY INSIGHT: After the loop, lo == hi+1, and lo points to the first element
     * that is >= target. This is exactly the insert position (where target WOULD go).
     *
     * This is the "lower_bound" from C++ STL:
     *   Returns the first index i such that arr[i] >= target.
     *   If all elements < target, returns arr.length.
     *
     * INTERVIEW: "Where would you insert x to keep the array sorted?"
     * → Binary search, return lo after the loop.
     */
    static int searchInsertPosition(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if      (arr[mid] <  target) lo = mid + 1;
            else if (arr[mid] >  target) hi = mid - 1;
            else                         return mid; // exact match
        }
        return lo; // lo = insert position (first element >= target)
    }

    // =========================================================
    // SECTION 5: Binary Search on ANSWER — the most important pattern
    // =========================================================

    /*
     * BINARY SEARCH ON ANSWER — THE MOST POWERFUL PATTERN:
     *
     * SIGNAL: "What is the minimum/maximum value such that some condition holds?"
     *   These problems have a MONOTONIC structure on the answer space:
     *   - If we CAN achieve X, we CAN achieve anything worse (X+1, X+2, ...)
     *   - If we CANNOT achieve X, we CANNOT achieve anything better (X-1, X-2, ...)
     *   This gives a yes/no boundary: [NO, NO, NO, YES, YES, YES]
     *   Binary search finds the FIRST YES.
     *
     * UNIVERSAL TEMPLATE:
     *   lo = minimum possible answer
     *   hi = maximum possible answer (safe upper bound)
     *   while (lo < hi):
     *       mid = lo + (hi - lo) / 2
     *       if feasible(mid):
     *           hi = mid       // mid might be the answer, try smaller
     *       else:
     *           lo = mid + 1   // mid is definitely not the answer
     *   return lo              // first feasible value
     *
     * NOTE: hi = mid (not mid-1) because mid might be the answer.
     *       lo = mid+1 because if mid is infeasible, mid is ruled out.
     *       Loop ends with lo == hi, which is the answer.
     */

    // --- LC 875: Koko Eating Bananas ---
    /**
     * PROBLEM: n piles of bananas. Koko can eat k bananas per hour.
     *   She has h hours. Find minimum k such that she can eat all piles in h hours.
     *   Each pile takes ceil(pile[i] / k) hours.
     *
     * INSIGHT: Search space for k is [1 .. max(piles)].
     *   At k = max(piles): she finishes each pile in 1 hour = n total hours (always feasible if h>=n)
     *   At k = 1: she takes sum(piles) hours (always feasible if h >= sum, but k=1 might be too slow)
     *   Feasibility is MONOTONE: if k works, k+1 also works (eats faster).
     *   Binary search for the MINIMUM k that is feasible.
     *
     * COMPLEXITY: O(n * log(max(piles))) — log range * O(n) feasibility check
     */
    static int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = Arrays.stream(piles).max().getAsInt();
        while (lo < hi) {
            int k = lo + (hi - lo) / 2; // candidate eating speed
            if (canFinish(piles, h, k)) {
                hi = k;       // k works, try smaller (we want minimum)
            } else {
                lo = k + 1;   // k doesn't work, need more speed
            }
        }
        return lo;
    }

    private static boolean canFinish(int[] piles, int h, int k) {
        int hoursNeeded = 0;
        for (int pile : piles) {
            hoursNeeded += (pile + k - 1) / k; // ceil(pile/k) without Math.ceil
            // Alternatively: (int) Math.ceil((double) pile / k) — but integer arithmetic is faster
        }
        return hoursNeeded <= h;
    }

    // --- LC 1011: Ship Packages Within D Days ---
    /**
     * PROBLEM: Packages with weights[i]. Ship them in order. Each day, load packages
     *   up to 'capacity' weight. Find minimum capacity to ship all packages in d days.
     *
     * INSIGHT: Search space = [max(weights), sum(weights)]
     *   lo = max(weights): minimum capacity to ship the heaviest package in one day
     *   hi = sum(weights): if capacity = sum, ship everything in 1 day
     *   Feasibility: given capacity, can we ship all in <= d days?
     *   Scan greedily: load packages until adding next would exceed capacity → new day.
     *
     * COMPLEXITY: O(n * log(sum - max)) per problem
     */
    static int shipWithinDays(int[] weights, int days) {
        int lo = Arrays.stream(weights).max().getAsInt(); // must be at least the heaviest
        int hi = Arrays.stream(weights).sum();             // can ship all in 1 day
        while (lo < hi) {
            int cap = lo + (hi - lo) / 2;
            if (canShip(weights, days, cap)) {
                hi = cap;       // cap works, try smaller
            } else {
                lo = cap + 1;   // cap doesn't work, need more capacity
            }
        }
        return lo;
    }

    private static boolean canShip(int[] weights, int days, int capacity) {
        int daysNeeded = 1, load = 0;
        for (int w : weights) {
            if (load + w > capacity) {
                daysNeeded++; // start a new day
                load = 0;
            }
            load += w;
        }
        return daysNeeded <= days;
    }

    // --- LC 410: Split Array Largest Sum ---
    /**
     * PROBLEM: Split nums into m non-empty subarrays. Minimize the largest subarray sum.
     *   (This is the "hardest" of the three — the key insight is identical to ship packages.)
     *
     * INSIGHT: Binary search on the answer (the maximum subarray sum).
     *   Search space: [max(nums), sum(nums)]
     *   Feasibility: with a maximum allowed sum of 'mid', can we split into <= m subarrays?
     *   Greedy: greedily add elements to current group until adding next would exceed mid.
     *
     * SAME feasibility function as shipWithinDays! (packages = nums, days = m, capacity = mid)
     *   This shows how recognizing the PATTERN is more valuable than memorizing solutions.
     *
     * COMPLEXITY: O(n * log(sum - max))
     */
    static int splitArray(int[] nums, int m) {
        int lo = Arrays.stream(nums).max().getAsInt();
        int hi = Arrays.stream(nums).sum();
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (canSplit(nums, m, mid)) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    private static boolean canSplit(int[] nums, int m, int maxSum) {
        int parts = 1, current = 0;
        for (int x : nums) {
            if (current + x > maxSum) {
                parts++;
                current = 0;
                if (parts > m) return false; // early exit — already exceeded allowed splits
            }
            current += x;
        }
        return parts <= m;
    }

    // =========================================================
    // SECTION 6: Matrix Binary Search (LC 74)
    // =========================================================

    /**
     * KEY INSIGHT: An m×n matrix where each row is sorted and the first element of each
     * row is greater than the last element of the previous row is effectively a 1D sorted array.
     * Treat index [0..m*n-1] as the "flat" array:
     *   row = mid / n,  col = mid % n
     *
     * COMPLEXITY: O(log(m*n)) = O(log m + log n) — single binary search over entire matrix.
     */
    static boolean searchMatrix(int[][] matrix, int target) {
        int m = matrix.length, n = matrix[0].length;
        int lo = 0, hi = m * n - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int val = matrix[mid / n][mid % n]; // convert flat index to 2D
            if      (val == target) return true;
            else if (val <  target) lo = mid + 1;
            else                    hi = mid - 1;
        }
        return false;
    }

    // =========================================================
    // SECTION 7: Binary Search on Floating Point
    // =========================================================

    /**
     * KEY INSIGHT: For continuous search spaces (e.g., square root), we can't use lo <= hi
     * (infinite loop on floats). Instead, run a fixed number of iterations (e.g., 100)
     * or loop while (hi - lo > epsilon). Each iteration halves the search space.
     *
     * After 100 iterations: error < (hi - lo) / 2^100 ≈ 10^-30 — more than precise enough.
     *
     * GOTCHA: Never use == for float comparison. Use epsilon (e.g., 1e-9).
     */
    static double squareRoot(double x) {
        if (x < 0) throw new IllegalArgumentException("Cannot sqrt negative");
        double lo = 0, hi = Math.max(x, 1.0); // hi = max(x,1) handles x<1 case
        for (int i = 0; i < 100; i++) {        // 100 iterations: precision ~ 10^-30
            double mid = (lo + hi) / 2.0;
            if (mid * mid < x) lo = mid;
            else               hi = mid;
        }
        return lo;
    }

    // =========================================================
    // SECTION 8: Peak Finding (LC 162)
    // =========================================================

    /**
     * KEY INSIGHT: A "peak" element is one that is greater than its neighbors.
     * We DON'T need to find the global maximum — any peak will do.
     *
     * Binary search insight: look at arr[mid] vs arr[mid+1].
     *   If arr[mid] < arr[mid+1]: the slope is going UP to the right.
     *     A peak MUST exist in [mid+1..hi] (either arr[mid+1] is a peak, or the slope
     *     eventually turns down before hi — and that turning point is a peak).
     *   If arr[mid] > arr[mid+1]: the slope is going DOWN to the right.
     *     A peak MUST exist in [lo..mid] (either arr[mid] is a peak, or there's one to its left).
     *
     * This works because: the array's boundaries are treated as -infinity.
     * So the array must "come back down" from any ascending slope before it ends.
     *
     * COMPLEXITY: O(log n) — each step halves the search space.
     */
    static int findPeakElement(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] < nums[mid + 1]) {
                lo = mid + 1; // peak is in right half (ascending slope)
            } else {
                hi = mid;     // peak is in left half or AT mid (descending slope)
            }
        }
        return lo; // lo == hi — this is a peak element
    }

    // =========================================================
    // TESTS & DEMOS
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Binary Search Variants                     ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        int[] arr = {1, 2, 2, 2, 3, 4, 5};
        System.out.println("\n=== Classic & Occurrence Search (arr=" + Arrays.toString(arr) + ") ===");
        System.out.println("classicBS(2): "   + classicBinarySearch(arr, 2));   // 2 (any valid index)
        System.out.println("firstOccurrence(2): " + firstOccurrence(arr, 2));   // 1
        System.out.println("lastOccurrence(2): "  + lastOccurrence(arr, 2));    // 3
        System.out.println("searchInsert(0): "    + searchInsertPosition(arr, 0)); // 0 (insert at front)
        System.out.println("searchInsert(6): "    + searchInsertPosition(arr, 6)); // 7 (insert at end)
        System.out.println("searchInsert(2): "    + searchInsertPosition(arr, 2)); // 1 (exact match)

        System.out.println("\n=== Binary Search on Answer ===");
        // Koko: piles=[3,6,7,11], h=8 → min k=4
        System.out.println("Koko minSpeed([3,6,7,11], h=8): " + minEatingSpeed(new int[]{3,6,7,11}, 8)); // 4
        // Ship: weights=[1,2,3,4,5,6,7,8,9,10], days=5 → cap=15
        System.out.println("Ship minCap([1..10], days=5): " + shipWithinDays(new int[]{1,2,3,4,5,6,7,8,9,10}, 5)); // 15
        // SplitArray: nums=[7,2,5,10,8], m=2 → 18
        System.out.println("SplitArray([7,2,5,10,8], m=2): " + splitArray(new int[]{7,2,5,10,8}, 2)); // 18

        System.out.println("\n=== Matrix Binary Search ===");
        int[][] matrix = {{1,3,5,7},{10,11,16,20},{23,30,34,60}};
        System.out.println("searchMatrix(matrix, 3): "  + searchMatrix(matrix, 3));  // true
        System.out.println("searchMatrix(matrix, 13): " + searchMatrix(matrix, 13)); // false

        System.out.println("\n=== Floating Point Binary Search ===");
        System.out.printf("sqrt(2) = %.10f (actual: %.10f)%n", squareRoot(2), Math.sqrt(2));
        System.out.printf("sqrt(9) = %.10f%n", squareRoot(9)); // ≈ 3.0

        System.out.println("\n=== Peak Finding ===");
        System.out.println("Peak in [1,2,3,1]: idx " + findPeakElement(new int[]{1,2,3,1})); // 2
        System.out.println("Peak in [1,2,1,3,5,6,4]: idx " + findPeakElement(new int[]{1,2,1,3,5,6,4})); // 5 or 1

        System.out.println("\n=== All tests complete ===");
    }
}
