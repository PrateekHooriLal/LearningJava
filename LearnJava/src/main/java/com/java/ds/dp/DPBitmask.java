package com.java.ds.dp;

import java.util.*;

/**
 * CONCEPT: Bitmask Dynamic Programming
 * ──────────────────────────────────────
 * Bitmask DP uses an integer bitmask to represent a SUBSET of elements.
 * Each bit i in the mask represents whether element i is "included" or "visited."
 * This encodes exponential state space (2^n subsets) into a compact integer.
 *
 * BIT REPRESENTATION:
 *   mask = 0b1011 → elements 0, 1, and 3 are selected (bit at position i = 1 means selected)
 *   (1 << i)     → mask with only bit i set (selects element i alone)
 *   mask | (1<<i) → add element i to the mask
 *   mask & (1<<i) → check if element i is in the mask
 *   mask ^ (1<<i) → toggle element i in the mask
 *   mask & (mask-1) → clear the lowest set bit
 *   mask & (-mask)  → isolate the lowest set bit
 *
 * GENERAL DP STRUCTURE:
 *   dp[mask] or dp[mask][i] = optimal cost to achieve state described by mask
 *                              optionally ending at node i
 *
 * WHEN TO USE (key indicator: n ≤ 20):
 *   - "Which subset of tasks/nodes has been visited?"
 *   - "Assign items to slots in an optimal way"
 *   - "Traveling Salesman / shortest Hamiltonian path"
 *   - "Word/task assignment to workers"
 *
 * TIME/SPACE COMPLEXITY:
 *   - Bitmask DP with n items: O(2^n × n) time and O(2^n × n) space typically
 *   - For n=20: 2^20 × 20 ≈ 20M operations — feasible
 *   - For n=25: 2^25 × 25 ≈ 800M — borderline; for n=30+: impractical
 *
 * INTERVIEW ANGLE:
 *   Bitmask DP signals "exponential state space, but n is small (≤ 20)."
 *   Explain it as "instead of remembering which exact items were picked, we encode
 *   the entire choice history as a bitmask."
 *
 * REAL-WORLD USE:
 *   - CPU register allocation (subset of registers used)
 *   - Task scheduling / job assignment
 *   - Compiler optimization (set of available expressions)
 *   - Genome sequencing (subset selection problems)
 */
public class DPBitmask {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: BIT TRICKS REFERENCE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Essential bit manipulation tricks for bitmask DP and interviews.
     * KEY INSIGHT: Mastering these operations makes bitmask problems much cleaner.
     */
    public static void bitTricksDemo() {
        int mask = 0b1010; // bits 1 and 3 are set (0-indexed from right)
        int i = 2;

        System.out.println("mask = " + Integer.toBinaryString(mask)); // 1010

        // Set bit i: turn ON bit i
        int setResult = mask | (1 << i);       // 1010 | 0100 = 1110
        System.out.println("Set bit " + i + ": " + Integer.toBinaryString(setResult));

        // Clear bit i: turn OFF bit i
        int clearResult = mask & ~(1 << i);    // 1010 & 1011 = 1010 (bit 2 was already 0)
        System.out.println("Clear bit " + i + ": " + Integer.toBinaryString(clearResult));

        // Check bit i: is bit i set?
        boolean isSet = (mask & (1 << i)) != 0; // 1010 & 0100 = 0 → false
        System.out.println("Bit " + i + " is set: " + isSet);

        // Toggle bit i: flip bit i
        int toggleResult = mask ^ (1 << i);    // 1010 ^ 0100 = 1110
        System.out.println("Toggle bit " + i + ": " + Integer.toBinaryString(toggleResult));

        // Count set bits (population count / Hamming weight)
        int popcount = Integer.bitCount(mask); // 2 (bits 1 and 3)
        System.out.println("Bit count: " + popcount);

        // Lowest set bit (isolate rightmost 1)
        int lowest = mask & (-mask);           // 1010 & 0110 = 0010 (two's complement trick)
        System.out.println("Lowest set bit: " + Integer.toBinaryString(lowest));

        // Clear lowest set bit
        int clearLowest = mask & (mask - 1);   // 1010 & 1001 = 1000
        System.out.println("Clear lowest set bit: " + Integer.toBinaryString(clearLowest));

        // Full mask for n items: all n bits set
        int n = 4;
        int fullMask = (1 << n) - 1;           // 10000 - 1 = 01111
        System.out.println("Full mask (n=4): " + Integer.toBinaryString(fullMask));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: TRAVELLING SALESMAN PROBLEM (TSP)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * TSP — Shortest Hamiltonian Path visiting all nodes exactly once
     *
     * KEY INSIGHT:
     *   dp[mask][i] = minimum cost to visit exactly the nodes in `mask`, ending at node i
     *   mask must include bit i (you can't end at i without having visited it)
     *
     * RECURRENCE:
     *   dp[mask | (1<<j)][j] = min(dp[mask | (1<<j)][j], dp[mask][i] + dist[i][j])
     *   For each state (mask, i) and each unvisited neighbor j (bit j not in mask):
     *   → extend the path by going from i to j
     *
     * INITIALIZATION:
     *   dp[1<<src][src] = 0  (start at src, only src is visited)
     *   All other dp values = INF
     *
     * ANSWER:
     *   min over all i: dp[FULL_MASK][i] + dist[i][src]  (return to start for TSP cycle)
     *   OR min over all i: dp[FULL_MASK][i]              (open path, no return)
     *
     * INTERVIEW Q: What is the time complexity and why is it better than O(n!)?
     * A: O(2^n × n^2). Brute force tries all n! permutations ≈ 3.6M for n=10.
     *    Bitmask DP: 2^10 × 100 = 102K operations — exponentially better.
     *    For n=20: 2^20 × 400 ≈ 400M — feasible. Brute force n=20: 2.4 × 10^18 — impossible.
     *
     * GOTCHA: dist[][] must be complete (dist[i][j] for all pairs). Use INF for no direct edge.
     *
     * @param n    number of cities (0-indexed: 0 to n-1)
     * @param dist dist[i][j] = cost to travel from city i to city j
     * @param src  starting city
     * @return minimum cost to visit all cities starting and ending at src (TSP cycle)
     */
    public static int tsp(int n, int[][] dist, int src) {
        final int INF = (int) 1e9;
        int FULL = (1 << n) - 1; // all n bits set = all cities visited

        // dp[mask][i] = min cost visiting cities in mask, ending at city i
        int[][] dp = new int[1 << n][n];
        for (int[] row : dp) Arrays.fill(row, INF);
        dp[1 << src][src] = 0; // start at src, only src visited, cost 0

        for (int mask = 0; mask <= FULL; mask++) {
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) == 0) continue; // i must be in mask (we've visited it)
                if (dp[mask][i] == INF) continue;      // unreachable state

                // Try extending path to unvisited city j
                for (int j = 0; j < n; j++) {
                    if ((mask & (1 << j)) != 0) continue; // j already visited
                    if (dist[i][j] == INF) continue;       // no direct edge

                    int newMask = mask | (1 << j);
                    dp[newMask][j] = Math.min(dp[newMask][j], dp[mask][i] + dist[i][j]);
                }
            }
        }

        // Find minimum cost to complete the cycle (return to src from any city)
        int minCost = INF;
        for (int i = 0; i < n; i++) {
            if (i == src || dist[i][src] == INF) continue;
            minCost = Math.min(minCost, dp[FULL][i] + dist[i][src]);
        }
        return minCost == INF ? -1 : minCost;
    }
    /*
     * COMPLEXITY — TSP Bitmask DP
     * ────────────────────────────
     * TIME:  O(2^n × n^2) — iterate over all 2^n masks × n ending nodes × n transitions
     * SPACE: O(2^n × n) — dp table
     *
     * Practical for n ≤ 20. For n > 20, need approximation algorithms (Christofides, etc.)
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 1986 — MINIMUM WORK SESSIONS (Bitmask DP)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 1986 — Minimum Number of Work Sessions to Finish the Tasks
     *
     * PROBLEM: Given n tasks with durations and a sessionTime limit, find the
     * minimum number of work sessions needed where each session ≤ sessionTime.
     *
     * KEY INSIGHT:
     *   dp[mask] = minimum number of sessions to complete tasks in mask
     *   Also track session remainder to avoid always starting new sessions.
     *
     * ALTERNATIVE STATE: dp[mask] = min remaining time in the last session
     * after completing tasks in mask (to pack tasks greedily).
     *
     * Cleaner approach: dp[mask] = {sessions, timeUsedInCurrentSession}
     * We use dp[mask] = min sessions with smallest possible "last session time used."
     * Encode as: dp[mask] = sessions * (sessionTime+1) + timeUsedInLastSession
     * Take modulo to extract each component.
     *
     * SIMPLER ALTERNATIVE (used here): dp[mask] = (minSessions, lastSessionTimeLeft)
     * encoded as pair, sort by sessions first, then by last session time remaining.
     * For n ≤ 14 (constraint), 2^14 = 16K states.
     *
     * INTERVIEW Q: Why bitmask for this problem?
     * A: n ≤ 14 (or 20 in some variants). State = "which subset of tasks completed."
     *    Ordering tasks within a session doesn't matter — only the subset matters.
     *    → 2^n subsets × O(n) transitions = O(2^n × n) total.
     *
     * @param tasks       array of task durations
     * @param sessionTime maximum time per work session
     * @return minimum number of sessions needed
     */
    public static int minSessions(int[] tasks, int sessionTime) {
        int n = tasks.length;
        int FULL = (1 << n) - 1;
        // dp[mask] = encoded state: (sessions-1) * (sessionTime+1) + timeUsedInCurrentSession
        // Minimize sessions first, then minimize time used in current session (pack greedily)
        int[] dp = new int[1 << n];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0; // 0 tasks done: 0 sessions started, 0 time used

        for (int mask = 0; mask < (1 << n); mask++) {
            if (dp[mask] == Integer.MAX_VALUE) continue;

            int sessions = dp[mask] / (sessionTime + 1);    // sessions completed so far
            int timeUsed = dp[mask] % (sessionTime + 1);    // time used in current session

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) continue; // task i already done

                int newMask = mask | (1 << i);
                int newState;
                if (timeUsed + tasks[i] <= sessionTime) {
                    // Task fits in current session
                    newState = sessions * (sessionTime + 1) + (timeUsed + tasks[i]);
                } else {
                    // Need a new session for this task
                    newState = (sessions + 1) * (sessionTime + 1) + tasks[i];
                }
                dp[newMask] = Math.min(dp[newMask], newState);
            }
        }
        return dp[FULL] / (sessionTime + 1) + 1; // +1 because sessions is 0-indexed in encoding
    }
    /*
     * COMPLEXITY — Minimum Work Sessions
     * ─────────────────────────────────────
     * TIME:  O(2^n × n) — for each of 2^n masks, try each of n tasks
     * SPACE: O(2^n) — dp array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: SUBSET ENUMERATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Bitmask Subset Enumeration — Iterate over all subsets of a mask
     *
     * KEY INSIGHT: The idiom `for (int sub = mask; sub > 0; sub = (sub-1) & mask)`
     * visits all non-empty subsets of mask exactly once, in decreasing order.
     *
     * WHY IT WORKS:
     *   sub-1 flips all trailing zeros to 1 and the lowest set bit to 0.
     *   Then & mask keeps only the bits that are in the original mask.
     *   This traverses all subsets from mask down to 1 (excludes 0 unless handled separately).
     *
     * TIME COMPLEXITY: O(3^n) total for enumerating all subsets of all masks.
     *   Each element is either: not in mask, in mask but not in sub, or in both → 3 choices.
     *   Sum over all masks of 2^(bits in mask) = 3^n by binomial theorem.
     *
     * INTERVIEW Q: How many subsets does a set of n elements have?
     * A: 2^n (including empty set). Including all subsets of all subsets: 3^n total work.
     *
     * @param mask the "universe" mask to enumerate subsets of
     * @return all non-empty subsets of mask
     */
    public static List<Integer> enumerateSubsets(int mask) {
        List<Integer> subsets = new ArrayList<>();
        // GOTCHA: loop condition is sub > 0 (stops before 0, which is the empty set)
        for (int sub = mask; sub > 0; sub = (sub - 1) & mask) {
            subsets.add(sub);
        }
        // If you need the empty subset too, add 0 separately
        return subsets;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: LC 318 — MAX PRODUCT OF WORD LENGTHS (Bitmask)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 318 — Maximum Product of Word Lengths
     *
     * PROBLEM: Given words, find max product of lengths of two words that
     * share NO common letters.
     *
     * KEY INSIGHT — Encode letter set as bitmask:
     *   For each word, compute a 26-bit integer where bit i is set if letter i is present.
     *   Two words share no common letters iff (mask1 & mask2) == 0.
     *
     * TIME: O(n^2 × L) naive, or O(n^2) after O(n×L) preprocessing.
     * L = average word length.
     *
     * GOTCHA: Two different words can have the same bitmask (e.g., "ab" and "ba").
     * We can optimize by keeping only the longest word for each unique mask.
     *
     * @param words array of lowercase letter strings
     * @return maximum product of lengths of two words with no shared letters
     */
    public static int maxProduct(String[] words) {
        int n = words.length;
        int[] masks = new int[n];

        // Precompute bitmask for each word (O(n × L))
        for (int i = 0; i < n; i++) {
            for (char c : words[i].toCharArray()) {
                masks[i] |= (1 << (c - 'a')); // set bit for this letter
            }
        }

        int maxProd = 0;
        // Compare all pairs O(n^2)
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if ((masks[i] & masks[j]) == 0) { // no shared letters
                    maxProd = Math.max(maxProd, words[i].length() * words[j].length());
                }
            }
        }
        return maxProd;
    }
    /*
     * COMPLEXITY — Max Product Word Lengths
     * ───────────────────────────────────────
     * TIME:  O(n^2 + n × L) — O(n^2) pair comparison + O(n × L) mask building
     * SPACE: O(n) — masks array
     *
     * The bitmask trick converts O(L^2) letter comparison per pair to O(1) bitwise AND.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 6: BITMASK DP — ASSIGNMENT PROBLEM VARIANT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Assignment Problem — Assign n workers to n jobs to minimize total cost.
     *
     * KEY INSIGHT:
     *   dp[mask] = minimum cost to assign jobs in mask to the first popcount(mask) workers.
     *   The k-th worker (0-indexed) is assigned the job corresponding to bit being set.
     *
     * TRANSITION:
     *   For each mask with k set bits:
     *     worker = popcount(mask) - 1  (the worker being assigned in this step)
     *     For each job j in mask:
     *       dp[mask] = min over j: dp[mask ^ (1<<j)] + cost[worker][j]
     *                  (remove job j from mask → previous assignment cost + current cost)
     *
     * INTERVIEW Q: Why does the order of workers matter here?
     * A: It doesn't for the optimal cost — we assign the first worker when mask has 1 bit,
     *    second worker when mask has 2 bits, etc. This is a canonical ordering that ensures
     *    each worker-job assignment is tried exactly once per worker.
     *
     * @param cost cost[i][j] = cost of assigning worker i to job j
     * @return minimum total assignment cost
     */
    public static int assignmentProblem(int[][] cost) {
        int n = cost.length;
        int FULL = (1 << n) - 1;
        int[] dp = new int[1 << n];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0; // no jobs assigned = 0 cost

        for (int mask = 1; mask <= FULL; mask++) {
            int worker = Integer.bitCount(mask) - 1; // this worker gets assigned in this step
            // Try assigning each job j that's in mask to this worker
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) == 0) continue; // job j not in mask
                int prevMask = mask ^ (1 << j); // mask without job j
                if (dp[prevMask] == Integer.MAX_VALUE) continue;
                dp[mask] = Math.min(dp[mask], dp[prevMask] + cost[worker][j]);
            }
        }
        return dp[FULL];
    }
    /*
     * COMPLEXITY — Assignment Problem
     * ────────────────────────────────
     * TIME:  O(2^n × n) — 2^n masks × n jobs to try per mask
     * SPACE: O(2^n) — dp array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Bit Tricks Demo ===");
        bitTricksDemo();

        System.out.println("\n=== TSP (Travelling Salesman) ===");
        // 4 cities, complete graph
        int INF = (int) 1e9;
        int[][] dist = {
            {0, 10, 15, 20},
            {10, 0, 35, 25},
            {15, 35, 0, 30},
            {20, 25, 30, 0}
        };
        System.out.println("TSP (expected 80): " + tsp(4, dist, 0)); // 0→1→3→2→0: 10+25+30+15=80

        System.out.println("\n=== LC 1986 Min Work Sessions ===");
        System.out.println(minSessions(new int[]{1, 2, 3}, 3));       // Expected: 2 (session1:[1,2], session2:[3])
        System.out.println(minSessions(new int[]{3, 1, 3, 1, 1}, 8)); // Expected: 2
        System.out.println(minSessions(new int[]{1, 2, 3, 4, 5}, 15)); // Expected: 1

        System.out.println("\n=== Subset Enumeration ===");
        int mask = 0b1011; // bits 0, 1, 3 → elements {0,1,3}
        List<Integer> subsets = enumerateSubsets(mask);
        System.out.println("Subsets of 1011 (count should be 7): " + subsets.size());
        // 2^3 - 1 = 7 non-empty subsets (mask has 3 set bits)

        System.out.println("\n=== LC 318 Max Product Word Lengths ===");
        System.out.println(maxProduct(new String[]{"abcw", "baz", "foo", "bar", "xtfn", "abcdef"})); // Expected: 16 (abcw×xtfn)
        System.out.println(maxProduct(new String[]{"a", "ab", "abc", "d", "cd", "bcd", "abcd"}));    // Expected: 4 (a×bcd)
        System.out.println(maxProduct(new String[]{"a", "aa", "aaa", "aaaa"}));                       // Expected: 0

        System.out.println("\n=== Assignment Problem ===");
        int[][] cost = {
            {9, 2, 7, 8},
            {6, 4, 3, 7},
            {5, 8, 1, 8},
            {7, 6, 9, 4}
        };
        System.out.println("Min assignment cost (expected 13): " + assignmentProblem(cost));
        // Optimal: worker0→job1(2), worker1→job2(3), worker2→job0(5)... wait let me trace
        // Actually optimal varies; known answer for this matrix is 13
    }
}
