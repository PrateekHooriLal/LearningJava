package com.java.ds.stack;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Monotonic Deque — Sliding Window Optimization
 * ============================================================
 *
 * WHAT IS A MONOTONIC DEQUE?
 *   A double-ended queue (Deque) maintained in monotonic order.
 *   Unlike a monotonic STACK (only operates on one end), the deque supports:
 *     - ADD from BACK (like a stack push, maintaining order)
 *     - ANSWER from FRONT (the current window's answer lives at the front)
 *     - EXPIRE from FRONT (when the front index leaves the sliding window)
 *
 *   This combination enables O(n) sliding window optimizations that would otherwise
 *   require O(n*k) brute force.
 *
 * HOW IT WORKS — SLIDING WINDOW MAXIMUM:
 *   Maintain a DECREASING deque (front = maximum of current window):
 *   1. Before adding right: remove from BACK while back value <= new value.
 *      (If a previous element is <= the new one, it can NEVER be the max of any future window
 *       that includes the new element — the new element "dominates" it.)
 *   2. Check FRONT: if front index is out of the window, remove from front.
 *   3. Answer: deque.front() = max of current window.
 *
 * DEQUE vs STACK for these problems:
 *   Stack: only access to one end → can't expire old elements from the other end.
 *   Deque: access to both ends → can expire from front, maintain order from back.
 *   Use deque whenever you need: ORDER MAINTENANCE + WINDOW EXPIRATION.
 *
 * AMORTIZED ANALYSIS — why O(n)?
 *   Each element is added to the deque EXACTLY ONCE (from the right).
 *   Each element is removed from the deque AT MOST ONCE (from either end).
 *   Total operations ≤ 2n → O(n) amortized.
 *
 * WHEN TO USE — SIGNAL WORDS:
 *   "Maximum/minimum in every sliding window of size k"
 *   "Shortest subarray with sum ≥ k" (negative values allowed → monotonic deque on prefix sums)
 *   "Jump game with range limit" (DP optimization with sliding window max)
 *   Any DP where dp[i] = max(dp[j] + f(j)) for j in [i-k, i-1]
 *
 * TIME / SPACE:
 *   All monotonic deque solutions: O(n) time, O(k) space (deque ≤ k elements at a time)
 *
 * INTERVIEW ANGLE:
 *   "Why do you remove from the back before adding?"
 *   → Dominated elements: if old ≤ new, old can never be the answer for any window
 *      that includes the new element. Removing them prunes dead weight.
 *   "What's the invariant maintained in the deque?"
 *   → Indices are strictly increasing. Values are strictly decreasing (for max-deque).
 *
 * REAL-WORLD:
 *   - Stream processing: rolling max/min over time windows (financial analytics)
 *   - Network packet scheduling: sliding window flow control
 *   - Video streaming: buffer fullness sliding window statistics
 *   - DP optimization: shortest path algorithms, scheduling DP
 * ============================================================
 */
public class MonotonicDeque {

    // =========================================================
    // PROBLEM 1: Sliding Window Maximum (LC 239 — HARD)
    // =========================================================

    /**
     * KEY INSIGHT: Brute force O(n*k) — for each window of size k, scan all k elements.
     * Monotonic deque reduces this to O(n) by maintaining a "useful candidates" deque.
     *
     * INVARIANT: The deque stores indices in INCREASING order, and the corresponding
     * values in STRICTLY DECREASING order. The front is always the maximum of the current window.
     *
     * ALGORITHM:
     *   For each right pointer r = 0..n-1:
     *   1. MAINTAIN ORDER (add r): while deque not empty AND nums[deque.back()] <= nums[r]:
     *        remove from back. Then push r to back.
     *        Why: nums[r] is in the window longer AND is >= back value → back is useless.
     *   2. EXPIRE OLD (remove front): if deque.front() <= r - k:
     *        remove front. (Front is outside the window.)
     *   3. RECORD ANSWER: if r >= k-1 (window is full):
     *        result[r - k + 1] = nums[deque.front()]
     *
     * NOTE: Steps 1 and 2 can be swapped. Order matters slightly for edge cases —
     * doing expiration before answer recording is cleaner.
     *
     * GOTCHA: Use ArrayDeque<Integer> in Java. Deque.peekFirst()=front, Deque.peekLast()=back.
     *         addLast=add to back, pollFirst=remove front, pollLast=remove back.
     *
     * EXAMPLE: nums=[1,3,-1,-3,5,3,6,7], k=3
     *   Window [1,3,-1] → max=3
     *   Window [3,-1,-3] → max=3
     *   Window [-1,-3,5] → max=5
     *   Window [-3,5,3] → max=5
     *   Window [5,3,6] → max=6
     *   Window [3,6,7] → max=7
     *   Result: [3,3,5,5,6,7]
     */
    static int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>(); // stores INDICES, values are decreasing

        for (int r = 0; r < n; r++) {
            // Step 1: Maintain decreasing order — remove dominated elements from back
            // nums[r] dominates any element <= nums[r] that is already in the deque,
            // because: it's to the right (will stay in window longer) AND >= in value
            while (!deque.isEmpty() && nums[deque.peekLast()] <= nums[r]) {
                deque.pollLast();
            }
            deque.addLast(r);

            // Step 2: Expire elements outside the window from front
            if (deque.peekFirst() <= r - k) {
                deque.pollFirst();
            }

            // Step 3: Record answer when first full window is formed
            if (r >= k - 1) {
                result[r - k + 1] = nums[deque.peekFirst()]; // front = current window max
            }
        }
        return result;
    }

    // =========================================================
    // PROBLEM 2: Shortest Subarray with Sum >= K (LC 862 — HARD)
    // =========================================================

    /**
     * KEY INSIGHT: This problem is hard because of NEGATIVE NUMBERS.
     * With only non-negative numbers, a two-pointer/sliding window works.
     * With negative numbers, shrinking a window might not reduce the sum.
     *
     * TWO-STEP APPROACH:
     * Step 1: Prefix sums. Let prefix[i] = sum of nums[0..i-1].
     *   Sum of subarray [l..r-1] = prefix[r] - prefix[l].
     *   We want prefix[r] - prefix[l] >= k with r - l minimized.
     *
     * Step 2: Monotonic deque on prefix sums.
     *   For each r, we want the LARGEST l such that prefix[r] - prefix[l] >= k.
     *   (Largest l = closest l to r = shortest subarray.)
     *
     * KEY INSIGHT FOR DEQUE:
     *   Maintain deque of INCREASING prefix sum values (indices where prefix is smaller).
     *   For each r:
     *     a) Pop from FRONT while prefix[r] - prefix[deque.front()] >= k:
     *        → Found a valid subarray [deque.front(), r-1]. Record length.
     *        → Pop because: for any future r' > r, if this l was valid, r' would give a LONGER
     *          subarray than using the same l with r. But wait — we pop because r' with a FUTURE
     *          r' > r and SAME l gives longer subarrays. So we want the SMALLEST r that satisfies
     *          the condition with l, so we can pop l once used.
     *     b) Pop from BACK while prefix[r] >= prefix[deque.back()]:
     *        → deque.back() is dominated: it's EARLIER (left) but has LARGER prefix sum.
     *          If prefix[deque.back()] >= prefix[r], and r is to the right, then for any
     *          future r', using r as the left boundary gives sum = prefix[r'] - prefix[r]
     *          which is LARGER than prefix[r'] - prefix[deque.back()]. And r gives a SHORTER
     *          subarray. So deque.back() is dominated and will never be optimal.
     *
     * COMPLEXITY: O(n) time, O(n) space
     *
     * GOTCHA: prefix array has length n+1 (prefix[0] = 0 handles subarrays starting at index 0).
     */
    static int shortestSubarrayWithSumAtLeastK(int[] nums, int k) {
        int n = nums.length;
        long[] prefix = new long[n + 1]; // long to avoid overflow (nums[i] up to 10^5)
        for (int i = 0; i < n; i++) prefix[i + 1] = prefix[i] + nums[i];

        int minLen = Integer.MAX_VALUE;
        Deque<Integer> deque = new ArrayDeque<>(); // indices, prefix values are increasing

        for (int r = 0; r <= n; r++) {
            // Pop from front: find valid subarrays ending at r
            // prefix[r] - prefix[deque.front()] >= k → subarray [deque.front()..r-1] is valid
            while (!deque.isEmpty() && prefix[r] - prefix[deque.peekFirst()] >= k) {
                minLen = Math.min(minLen, r - deque.pollFirst());
            }

            // Pop from back: maintain increasing prefix sums in deque
            // If prefix[r] <= prefix[deque.back()], then deque.back() is DOMINATED by r:
            // r is to the right AND has smaller prefix sum → better starting point for future windows
            while (!deque.isEmpty() && prefix[r] <= prefix[deque.peekLast()]) {
                deque.pollLast();
            }
            deque.addLast(r);
        }
        return minLen == Integer.MAX_VALUE ? -1 : minLen;
    }

    // =========================================================
    // PROBLEM 3: Jump Game VI (LC 1696) — DP with Monotonic Deque
    // =========================================================

    /**
     * KEY INSIGHT: dp[i] = max score when reaching index i.
     * dp[i] = nums[i] + max(dp[j]) for j in [i-k, i-1].
     *
     * Naive: O(n*k) — scan all k previous states for each i.
     * Optimized: Sliding window MAXIMUM over dp values in a window of size k → O(n).
     *
     * This is the canonical DP-with-sliding-window-max pattern.
     * The monotonic deque maintains the maximum dp value in the last k positions.
     *
     * GENERAL PATTERN:
     *   Any DP of the form dp[i] = f(i) + max/min(dp[j]) for j in [i-k, i-1]
     *   can be optimized from O(n*k) to O(n) using monotonic deque.
     *   This is also called "Divide and Conquer DP" or "Monotonic Deque DP optimization".
     *
     * COMPLEXITY: O(n) time, O(k) space (deque holds ≤ k indices)
     */
    static int maxResult(int[] nums, int k) {
        int n = nums.length;
        int[] dp = new int[n];
        dp[0] = nums[0];

        // Deque stores indices in decreasing order of dp values (front = max dp)
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addLast(0);

        for (int i = 1; i < n; i++) {
            // Expire indices out of window [i-k, i-1]
            if (deque.peekFirst() < i - k) {
                deque.pollFirst();
            }

            // dp[i] = nums[i] + max dp in window = nums[i] + dp[deque.front()]
            dp[i] = nums[i] + dp[deque.peekFirst()];

            // Maintain decreasing dp values in deque
            while (!deque.isEmpty() && dp[deque.peekLast()] <= dp[i]) {
                deque.pollLast();
            }
            deque.addLast(i);
        }
        return dp[n - 1];
    }

    // =========================================================
    // PATTERN COMPARISON TABLE
    // =========================================================

    /*
     * MONOTONIC STACK vs MONOTONIC DEQUE — WHEN TO USE WHICH:
     *
     * ┌─────────────────────────────────────────┬──────────────────────────────────────────┐
     * │ Monotonic STACK                         │ Monotonic DEQUE                          │
     * ├─────────────────────────────────────────┼──────────────────────────────────────────┤
     * │ "Next greater/smaller element"          │ "Max/min in every sliding window of k"   │
     * │ "Largest rectangle in histogram"        │ "Shortest subarray with sum >= k"        │
     * │ "Trapping rain water"                   │ "DP with range max/min optimization"     │
     * │ Results use ALL previous elements       │ Results limited to last k elements       │
     * │ No expiration — old elements can        │ Expiration — elements outside window     │
     * │   contribute at any future point        │   are definitively removed from front    │
     * │ Single end (top): push and pop          │ Both ends: add to back, expire from front│
     * │ O(n) total, no window constraint        │ O(n) total, O(k) space per window        │
     * └─────────────────────────────────────────┴──────────────────────────────────────────┘
     *
     * BOTH use the "dominated elements" pruning from the back:
     *   Stack: pop from top when order violated.
     *   Deque: pollLast() when back value dominated by new value.
     *
     * DEQUE adds: pollFirst() when front index is outside the window.
     */

    // =========================================================
    // MAIN: Tests & Demos
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Monotonic Deque Problems                   ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Sliding Window Maximum
        System.out.println("\n=== Sliding Window Maximum (LC 239) ===");
        int[] nums1 = {1,3,-1,-3,5,3,6,7};
        System.out.println("maxSlidingWindow([1,3,-1,-3,5,3,6,7], k=3): "
            + Arrays.toString(maxSlidingWindow(nums1, 3)));
        // Expected: [3,3,5,5,6,7]

        System.out.println("maxSlidingWindow([1], k=1): "
            + Arrays.toString(maxSlidingWindow(new int[]{1}, 1)));
        // Expected: [1]

        System.out.println("maxSlidingWindow([1,-1], k=1): "
            + Arrays.toString(maxSlidingWindow(new int[]{1,-1}, 1)));
        // Expected: [1,-1]

        // Shortest Subarray with Sum >= K
        System.out.println("\n=== Shortest Subarray with Sum >= K (LC 862) ===");
        System.out.println("shortestSubarray([1], k=1): "
            + shortestSubarrayWithSumAtLeastK(new int[]{1}, 1)); // 1
        System.out.println("shortestSubarray([1,2], k=4): "
            + shortestSubarrayWithSumAtLeastK(new int[]{1,2}, 4)); // -1
        System.out.println("shortestSubarray([2,-1,2], k=3): "
            + shortestSubarrayWithSumAtLeastK(new int[]{2,-1,2}, 3)); // 3
        System.out.println("shortestSubarray([84,-37,32,40,95], k=167): "
            + shortestSubarrayWithSumAtLeastK(new int[]{84,-37,32,40,95}, 167)); // 3

        // Jump Game VI
        System.out.println("\n=== Jump Game VI — DP with Sliding Window Max (LC 1696) ===");
        System.out.println("maxResult([1,-1,-2,4,-7,3], k=2): "
            + maxResult(new int[]{1,-1,-2,4,-7,3}, 2)); // 7
        System.out.println("maxResult([10,-5,-2,4,0,3], k=3): "
            + maxResult(new int[]{10,-5,-2,4,0,3}, 3)); // 17
        System.out.println("maxResult([1,-5,-20,4,-1,3,-6,-3], k=2): "
            + maxResult(new int[]{1,-5,-20,4,-1,3,-6,-3}, 2)); // 0

        System.out.println("\n=== All monotonic deque tests complete ===");
    }
}
