package com.java.ds.dp;

import java.util.*;

/**
 * CONCEPT: Interval DP (DP on Ranges)
 * ─────────────────────────────────────
 * Interval DP solves problems where the answer for a range [i, j] depends
 * on answers for sub-ranges [i, k] and [k, j] for some "split point" k.
 *
 * GENERAL TEMPLATE (fill by interval length):
 *   for (int len = 2; len <= n; len++) {           // interval length
 *       for (int i = 0; i + len - 1 < n; i++) {   // start of interval
 *           int j = i + len - 1;                    // end of interval
 *           for (int k = i; k < j; k++) {           // split point
 *               dp[i][j] = optimize(dp[i][k], dp[k+1][j], cost(i,k,j));
 *           }
 *       }
 *   }
 *
 * KEY INSIGHT — Fill by length, NOT by starting index:
 *   We must ensure shorter intervals are solved before longer ones that depend on them.
 *   Filling by increasing length guarantees this ordering.
 *
 * PROBLEMS COVERED:
 *   - LC 312 Burst Balloons           — "last balloon" interval DP
 *   - LC 132 Palindrome Partitioning II — min cuts using precomputed palindrome table
 *   - LC 877 Stone Game               — game theory interval DP
 *
 * WHEN TO USE:
 *   - Problem involves a sequence where you process segments in some order
 *   - "Remove elements one by one" or "merge elements one by one" problems
 *   - Game theory problems on arrays where two players alternate choices from ends
 *
 * TIME/SPACE COMPLEXITY: O(n^3) time, O(n^2) space for typical interval DP.
 *
 * INTERVIEW ANGLE:
 *   The trickiest part is identifying the "split point" and the "merge cost."
 *   For Burst Balloons, the key reframe is thinking of k as the LAST balloon popped,
 *   not the first — this makes subproblems independent.
 *
 * REAL-WORLD USE:
 *   - Optimal matrix chain multiplication (classic interval DP)
 *   - Optimal BST construction
 *   - RNA secondary structure prediction (bioinformatics)
 *   - Parser optimization
 */
public class DPOnIntervals {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: LC 312 — BURST BALLOONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 312 — Burst Balloons
     *
     * PROBLEM: Given n balloons with values nums[i]. Bursting balloon i earns
     * nums[i-1] * nums[i] * nums[i+1] coins. Burst all balloons to maximize coins.
     * (Treat out-of-bounds as value 1.)
     *
     * KEY INSIGHT — Think about the LAST balloon popped, not the first:
     *   If we think "which balloon do we pop FIRST in range [i,j]?", the subproblems
     *   overlap because the neighbors of the first popped balloon change over time.
     *   If we think "which balloon is popped LAST in range [i,j]?", the boundaries
     *   are fixed (i-1 and j+1 still exist when k is popped), making subproblems INDEPENDENT.
     *
     * SETUP:
     *   Add sentinel value 1 to both ends: newNums = [1, ...nums..., 1] (length n+2)
     *   dp[i][j] = max coins from bursting ALL balloons STRICTLY between indices i and j
     *   (i and j are "boundary" balloons that are NOT burst in range [i,j])
     *
     * RECURRENCE:
     *   For split point k (the LAST balloon popped between i and j):
     *   dp[i][j] = max over k in (i, j):
     *              newNums[i] * newNums[k] * newNums[j] + dp[i][k] + dp[k][j]
     *   (k is the last popped → its neighbors are i and j at the time of popping)
     *
     * BASE CASE: dp[i][j] = 0 when j == i+1 (no balloons between adjacent boundaries)
     *
     * INTERVIEW Q: Why does the "last popped" framing make subproblems independent?
     * A: After choosing k as the last to pop in [i,j], we have two independent ranges
     *    [i,k] and [k,j]. Each sub-range's answer doesn't affect the other because
     *    k is still present (as a boundary) while the sub-ranges are being solved.
     *
     * GOTCHA: The dp array is over the PADDED array (n+2 elements). Indices go from 0 to n+1.
     * dp[i][j] considers balloons STRICTLY between i and j (exclusive).
     *
     * @param nums original balloon values
     * @return maximum coins collectible
     */
    public static int maxCoins(int[] nums) {
        int n = nums.length;
        // Add boundary sentinels: newNums[0] = newNums[n+1] = 1
        int[] newNums = new int[n + 2];
        newNums[0] = 1;
        newNums[n + 1] = 1;
        for (int i = 0; i < n; i++) newNums[i + 1] = nums[i];
        int m = n + 2; // total length including sentinels

        // dp[i][j] = max coins from bursting all balloons strictly between i and j
        int[][] dp = new int[m][m];

        // Fill by interval length (len = number of balloons strictly inside [i, j])
        // len starts at 2 because the minimum range is [i, i+1] (0 balloons inside)
        // We want len to represent j - i, so j = i + len
        for (int len = 2; len < m; len++) {
            for (int i = 0; i + len < m; i++) {
                int j = i + len;
                // Try each balloon k as the LAST one popped in (i, j)
                for (int k = i + 1; k < j; k++) {
                    // When k is popped last, its neighbors are i and j (they still exist)
                    int coins = newNums[i] * newNums[k] * newNums[j];
                    // Plus coins from independently solving left [i,k] and right [k,j]
                    dp[i][j] = Math.max(dp[i][j], coins + dp[i][k] + dp[k][j]);
                }
            }
        }
        // Answer: burst all balloons strictly between 0 and n+1 (the full range)
        return dp[0][n + 1];
    }
    /*
     * COMPLEXITY — Burst Balloons
     * ────────────────────────────
     * TIME:  O(n^3) — three nested loops (len, i, k), each O(n)
     * SPACE: O(n^2) — dp table of size (n+2)^2
     *
     * The "last balloon" reframe turns an exponential brute force into O(n^3).
     * This is a classic example of how problem framing determines complexity.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: LC 132 — PALINDROME PARTITIONING II
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 132 — Palindrome Partitioning II
     *
     * PROBLEM: Find the minimum number of cuts needed to partition a string s
     * into substrings that are all palindromes.
     *
     * KEY INSIGHT — Two-phase DP:
     *   Phase 1: Precompute isPalin[i][j] = is s[i..j] a palindrome? (O(n^2))
     *   Phase 2: dp[i] = minimum cuts for s[0..i] (1D DP, O(n^2))
     *
     * PHASE 1 RECURRENCE (expand-around-center style in DP):
     *   isPalin[i][j] = true if s[i] == s[j] AND (j-i <= 2 OR isPalin[i+1][j-1])
     *   Fill by interval length (short before long).
     *
     * PHASE 2:
     *   dp[i] = 0 if s[0..i] is itself a palindrome (no cuts needed)
     *   dp[i] = min over all j from 1 to i where s[j..i] is palindrome:
     *           dp[j-1] + 1   (cut between position j-1 and j)
     *
     * INTERVIEW Q: Why check if the whole s[0..i] is a palindrome first?
     * A: If the whole substring is a palindrome, 0 cuts needed — don't loop.
     *    This is the base case optimization.
     *
     * GOTCHA: dp[i] = i (max cuts = i, one char per partition) is a valid upper bound
     * to initialize. Or use Integer.MAX_VALUE and handle carefully.
     *
     * @param s input string
     * @return minimum number of cuts to partition s into palindromes
     */
    public static int minCut(String s) {
        int n = s.length();

        // Phase 1: Precompute palindrome table
        boolean[][] isPalin = new boolean[n][n];
        // Single characters are always palindromes
        for (int i = 0; i < n; i++) isPalin[i][i] = true;
        // Length 2 pairs
        for (int i = 0; i < n - 1; i++) isPalin[i][i + 1] = (s.charAt(i) == s.charAt(i + 1));
        // Length 3 and above (fill by increasing length)
        for (int len = 3; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                isPalin[i][j] = (s.charAt(i) == s.charAt(j)) && isPalin[i + 1][j - 1];
            }
        }

        // Phase 2: DP for minimum cuts
        // dp[i] = min cuts for s[0..i]
        int[] dp = new int[n];
        for (int i = 0; i < n; i++) {
            if (isPalin[0][i]) {
                dp[i] = 0; // entire s[0..i] is a palindrome, no cut needed
            } else {
                dp[i] = i; // max cuts = i (cut every single char: "a|b|c|..." = i cuts for i+1 chars)
                for (int j = 1; j <= i; j++) {
                    if (isPalin[j][i]) {
                        // s[j..i] is a palindrome: cut between j-1 and j
                        dp[i] = Math.min(dp[i], dp[j - 1] + 1);
                    }
                }
            }
        }
        return dp[n - 1];
    }
    /*
     * COMPLEXITY — Palindrome Partitioning II
     * ─────────────────────────────────────────
     * TIME:  O(n^2) — palindrome precomputation (O(n^2)) + DP (O(n^2))
     * SPACE: O(n^2) — isPalin table + O(n) for dp array
     *
     * Brute force (generate all partitions) is exponential. This DP is O(n^2) — a huge win.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 877 — STONE GAME (Game Theory Interval DP)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 877 — Stone Game
     *
     * PROBLEM: Alex and Lee take turns picking piles from either end of an array.
     * Alex goes first. Both play optimally. Does Alex always win?
     *
     * MATH SHORTCUT: Alex always wins (return true always) when the number of piles
     * is even. Alex picks all odd-indexed or all even-indexed piles, whichever is larger.
     * But the DP version is the one interviewers want to see for follow-up questions.
     *
     * KEY INSIGHT — DP on score difference:
     *   dp[i][j] = max score difference (current player - opponent) for piles[i..j]
     *   The "current player" is whoever's turn it is when facing range [i, j].
     *
     * RECURRENCE:
     *   If current player picks piles[i]:
     *     score diff = piles[i] - dp[i+1][j]  (opponent now faces [i+1,j])
     *   If current player picks piles[j]:
     *     score diff = piles[j] - dp[i][j-1]  (opponent now faces [i,j-1])
     *   dp[i][j] = max of these two options
     *
     * WHY SUBTRACT? Because dp[i+1][j] already encodes the OPPONENT's net advantage
     * over the current player in range [i+1,j]. When we pick piles[i], we get
     * piles[i], and then the opponent gets their advantage (which subtracts from ours).
     *
     * ANSWER: Alex wins if dp[0][n-1] > 0 (Alex's score - Lee's score > 0)
     *
     * INTERVIEW Q: What is dp[i][j] when i == j?
     * A: Only one pile left — current player takes it. dp[i][i] = piles[i].
     *
     * @param piles array of pile sizes
     * @return true if Alex (first player) wins with optimal play
     */
    public static boolean stoneGame(int[] piles) {
        int n = piles.length;
        int[][] dp = new int[n][n];

        // Base case: single pile, current player takes it all
        for (int i = 0; i < n; i++) dp[i][i] = piles[i];

        // Fill by interval length (len 2 upward)
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i + len - 1 < n; i++) {
                int j = i + len - 1;
                // Current player picks left (piles[i]) or right (piles[j])
                // After picking, opponent faces a subrange and gains their advantage
                int pickLeft  = piles[i] - dp[i + 1][j]; // our gain minus opponent's future advantage
                int pickRight = piles[j] - dp[i][j - 1]; // our gain minus opponent's future advantage
                dp[i][j] = Math.max(pickLeft, pickRight);
            }
        }
        return dp[0][n - 1] > 0; // Alex wins if net advantage > 0
    }
    /*
     * COMPLEXITY — Stone Game
     * ────────────────────────
     * TIME:  O(n^2) — fill all pairs (i, j)
     * SPACE: O(n^2) — dp table (reducible to O(n) with diagonal-fill technique)
     *
     * NOTE: LC 877 has a trivial O(1) solution (Alex always wins), but the DP
     * generalizes to LC 1140 Stone Game II and LC 1406 Stone Game III.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: GENERAL INTERVAL DP TEMPLATE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * General Interval DP Template — Matrix Chain Multiplication
     *
     * KEY INSIGHT: Classic interval DP. To multiply matrices[i..j], choose
     * a split point k: multiply [i..k] and [k+1..j] separately, then combine.
     * Cost of combining = dims[i-1] * dims[k] * dims[j] (dimensions for k-split).
     *
     * TEMPLATE PATTERN (same structure as Burst Balloons):
     *   1. Fill by increasing interval length
     *   2. For each (i, j), try all split points k
     *   3. dp[i][j] = min/max of (dp[i][k] + dp[k+1][j] + cost(i,k,j))
     *
     * @param dims dimensions array: matrix i has dimensions dims[i-1] x dims[i]
     *             (n matrices → n+1 dimension values)
     * @return minimum number of scalar multiplications
     */
    public static int matrixChainMult(int[] dims) {
        int n = dims.length - 1; // number of matrices
        int[][] dp = new int[n + 1][n + 1]; // dp[i][j] = min cost to multiply matrices i..j (1-indexed)

        // len = 1 means single matrix = 0 cost (already computed, stays 0)
        for (int len = 2; len <= n; len++) {           // interval length
            for (int i = 1; i + len - 1 <= n; i++) {  // start
                int j = i + len - 1;                    // end
                dp[i][j] = Integer.MAX_VALUE;
                for (int k = i; k < j; k++) {           // split point
                    // Cost of this split: multiply [i..k] result by [k+1..j] result
                    int cost = dp[i][k] + dp[k + 1][j] + dims[i - 1] * dims[k] * dims[j];
                    dp[i][j] = Math.min(dp[i][j], cost);
                }
            }
        }
        return dp[1][n]; // cost to multiply all n matrices
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== LC 312 Burst Balloons ===");
        System.out.println(maxCoins(new int[]{3, 1, 5, 8})); // Expected: 167
        // 3*1*5=15, then 3*5*8=120, then 1*3*8=24, then 1*8*1=8 → 167
        System.out.println(maxCoins(new int[]{1, 5}));       // Expected: 10
        System.out.println(maxCoins(new int[]{4}));           // Expected: 4

        System.out.println("\n=== LC 132 Palindrome Partitioning II ===");
        System.out.println(minCut("aab"));   // Expected: 1 (aa | b)
        System.out.println(minCut("a"));     // Expected: 0
        System.out.println(minCut("ab"));    // Expected: 1 (a | b)
        System.out.println(minCut("ababbbabbababa")); // Expected: 3

        System.out.println("\n=== LC 877 Stone Game ===");
        System.out.println(stoneGame(new int[]{5, 3, 4, 5})); // Expected: true
        // Alex picks 5(left) → Lee picks 5(right) → Alex picks 4 → Lee picks 3
        // Alex: 5+4=9, Lee: 5+3=8 → Alex wins
        System.out.println(stoneGame(new int[]{3, 7, 2, 3})); // Expected: true

        System.out.println("\n=== Matrix Chain Multiplication ===");
        // 3 matrices: A(10x30), B(30x5), C(5x60) → dims = {10, 30, 5, 60}
        // Optimal: (A*B)*C = 10*30*5 + 10*5*60 = 1500+3000 = 4500
        System.out.println(matrixChainMult(new int[]{10, 30, 5, 60})); // Expected: 4500
        System.out.println(matrixChainMult(new int[]{40, 20, 30, 10, 30})); // Expected: 26000
    }
}
