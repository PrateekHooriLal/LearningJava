package com.java.ds.search;

/**
 * BINARY SEARCH ON THE ANSWER — Search on a Value Range, Not an Array Index
 *
 * CONCEPT:
 *   Classic binary search searches for an element in a sorted array.
 *   "Binary Search on Answer" applies the same halving idea to a VALUE RANGE:
 *   - Define: what is the minimum and maximum possible answer?
 *   - Define: a check(mid) function that returns true/false (is mid feasible?)
 *   - Binary search to find the boundary between feasible and infeasible answers.
 *
 *   This technique converts "find the optimal X" into "is X feasible?" which is
 *   often much easier to check.
 *
 * THE TEMPLATE:
 *   int lo = minAnswer, hi = maxAnswer;
 *   while (lo < hi) {
 *     int mid = lo + (hi - lo) / 2;
 *     if (isFeasible(mid)) hi = mid;       // mid works → try smaller
 *     else lo = mid + 1;                   // mid doesn't work → need more
 *   }
 *   return lo; // lo == hi is the boundary answer
 *
 * PROBLEMS COVERED:
 *   1. Square Root (floor) — LC 69: binary search on answer range [0, n]
 *   2. Min Days to Make m Bouquets — LC 1482: binary search on days [1, max(bloomDay)]
 *
 * INTERVIEW FREQUENCY: Medium-high at FAANG/tier-1. Tests pattern recognition.
 *   This shows you can abstract "find the optimal value" → binary search.
 *
 * COMMON FOLLOW-UP:
 *   1. "What other problems use this pattern?"
 *      → Koko Eating Bananas (LC 875), Capacity to Ship (LC 1011), Split Array (LC 410)
 *   2. "Why is the check function always monotonic?"
 *      → Because the answer space is ordered: if X days works, X+1 also works.
 *      Monotonicity is the prerequisite for binary search.
 *
 * Time: O(log(range) × checkTime) for both problems
 */
public class BinarySearchOnAnswer {

    public static void main(String[] args) {

        System.out.println("=== Problem 1: Integer Square Root (LC 69) ===");
        System.out.println(mySqrt(4));    // Expected: 2
        System.out.println(mySqrt(8));    // Expected: 2 (floor of 2.828...)
        System.out.println(mySqrt(9));    // Expected: 3
        System.out.println(mySqrt(1));    // Expected: 1
        System.out.println(mySqrt(0));    // Expected: 0
        System.out.println(mySqrt(2147395600)); // Expected: 46340 (large input)

        System.out.println("\n=== Problem 2: Min Days to Make m Bouquets (LC 1482) ===");
        // bloomDay=[1,10,3,10,2], m=3, k=1
        // With 3 days: flowers 1,3,2 bloom → positions 0,2,4 → 3 bouquets of 1 ✓
        // With 2 days: only flowers 1,2 bloom → only 2 bouquets ✗
        System.out.println(minDays(new int[]{1, 10, 3, 10, 2}, 3, 1)); // Expected: 3

        // bloomDay=[1,10,3,10,2], m=3, k=2
        // Need 3 bouquets of 2 consecutive flowers = 6 flowers total, but only 5 → impossible
        System.out.println(minDays(new int[]{1, 10, 3, 10, 2}, 3, 2)); // Expected: -1

        // bloomDay=[7,7,7,7,12,7,7], m=2, k=3
        // Day 7: positions 0,1,2,3,5,6 bloom → [0,1,2] and [3,5?] no — need consecutive
        //         [0,1,2] forms bouquet 1, [3] → need 5,6 also → [5,6] need one more
        //         Actually [0,1,2] and [4,5,6]? pos4=12 not yet → day 12 for pos4
        //         At day 12: all bloom → [0,1,2] and [3,4,5] → 2 bouquets ✓
        System.out.println(minDays(new int[]{7, 7, 7, 7, 12, 7, 7}, 2, 3)); // Expected: 12
    }

    // =========================================================================
    // PROBLEM 1: Integer Square Root — LC 69
    // =========================================================================

    /**
     * Returns floor(sqrt(x)) without using Math.sqrt().
     *
     * ANSWER RANGE: The square root of x is in [0, x].
     *   But more precisely, for x >= 1, sqrt(x) <= x/2 + 1 (tighter bound).
     *   We binary search in [0, x] for simplicity.
     *
     * FEASIBILITY CHECK: Is mid*mid <= x?
     *   We want the LARGEST mid where mid*mid <= x.
     *   → Binary search for the rightmost "true" in a sequence of true/false.
     *
     * LOOP VARIANT — Finding the rightmost feasible answer:
     *   When mid*mid <= x, record mid and try higher (lo = mid + 1).
     *   When mid*mid > x, go lower (hi = mid - 1).
     *
     * OVERFLOW GUARD: Use long for mid*mid since x can be up to 2^31-1,
     *   and (46341)^2 > Integer.MAX_VALUE.
     */
    public static int mySqrt(int x) {
        if (x == 0) return 0;

        int lo = 1;
        int hi = x;
        int result = 0;

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            long sq = (long) mid * mid; // Use long to prevent overflow!

            if (sq == x) {
                return mid; // Perfect square
            } else if (sq < x) {
                result = mid;       // mid is a valid floor candidate — save it
                lo = mid + 1;       // Try to find a larger valid answer
            } else {
                hi = mid - 1;       // mid² > x → mid is too large
            }
        }

        return result; // Largest mid where mid² <= x
    }

    // =========================================================================
    // PROBLEM 2: Minimum Number of Days to Make m Bouquets — LC 1482
    // =========================================================================

    /**
     * Returns the minimum number of days needed to make m bouquets,
     * each requiring k consecutive bloomed flowers.
     *
     * bloomDay[i] = the day flower i blooms.
     * A bouquet needs k adjacent bloomed flowers.
     *
     * ANSWER RANGE: [1, max(bloomDay)] — we need at least 1 day, at most
     *   the day the last flower blooms.
     *
     * FEASIBILITY CHECK (canMakeBouquets):
     *   On a given day d, a flower at position i is bloomed if bloomDay[i] <= d.
     *   Scan left to right, count consecutive bloomed flowers.
     *   Each time we accumulate k consecutive flowers, count one bouquet.
     *   Return true if total bouquets >= m.
     *
     * MONOTONICITY: If we can make m bouquets in d days, we can also do it
     *   in d+1, d+2, etc. (more flowers bloom). So feasibility is monotonic → binary search applies.
     *
     * FINDING THE MINIMUM: We want the SMALLEST day where feasible = true.
     *   → Standard left-boundary binary search (hi = mid when feasible).
     */
    public static int minDays(int[] bloomDay, int m, int k) {
        // If total flowers < m*k, it's impossible even with infinite days
        // Use long to prevent overflow: m and k can each be up to 10^5
        if ((long) m * k > bloomDay.length) return -1;

        // Binary search on the answer: day range [1, max(bloomDay)]
        int lo = 1;
        int hi = 0;
        for (int d : bloomDay) hi = Math.max(hi, d); // hi = latest bloom day

        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;

            if (canMakeBouquets(bloomDay, m, k, mid)) {
                // mid days is feasible → try fewer days (maybe we can do it sooner)
                hi = mid;
            } else {
                // mid days is not enough → need more days
                lo = mid + 1;
            }
        }

        // lo == hi at this point: the minimum feasible day
        return lo;
    }

    /**
     * Returns true if we can make at least m bouquets of k consecutive flowers
     * by day 'day'.
     *
     * GREEDY SCAN: Walk left to right. Count consecutive bloomed flowers.
     * Whenever we accumulate k, claim a bouquet and reset the consecutive counter.
     */
    private static boolean canMakeBouquets(int[] bloomDay, int m, int k, int day) {
        int bouquets = 0;       // Total bouquets made so far
        int consecutive = 0;    // Current run of consecutive bloomed flowers

        for (int bd : bloomDay) {
            if (bd <= day) {
                // This flower has bloomed by 'day'
                consecutive++;

                if (consecutive == k) {
                    // We have k consecutive bloomed flowers → form a bouquet
                    bouquets++;
                    consecutive = 0; // Reset run for the next bouquet
                }
            } else {
                // This flower hasn't bloomed yet → breaks the consecutive run
                consecutive = 0;
            }
        }

        return bouquets >= m;
    }
}
