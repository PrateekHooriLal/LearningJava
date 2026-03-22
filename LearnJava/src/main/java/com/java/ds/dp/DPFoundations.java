package com.java.ds.dp;

import java.util.*;

/**
 * DP FOUNDATIONS — The Mental Model Behind Dynamic Programming
 *
 * CONCEPT:
 *   Dynamic Programming (DP) solves problems with OVERLAPPING SUBPROBLEMS
 *   by storing results to avoid recomputation. It applies when:
 *   1. Optimal substructure: optimal solution = optimal solution of subproblems
 *   2. Overlapping subproblems: same subproblems solved multiple times
 *
 * THE 3-STEP DP FRAMEWORK (use this in every interview):
 *   STEP 1 — DEFINE: What does dp[i] (or dp[i][j]) represent?
 *            Write it as a SENTENCE. e.g., "dp[i] = max profit using first i days"
 *            Getting this wrong makes the rest impossible. ALWAYS define first.
 *   STEP 2 — RECURRENCE: How does dp[i] depend on smaller subproblems?
 *            This IS the algorithm. e.g., dp[i] = max(dp[i-1], dp[i-2] + nums[i])
 *   STEP 3 — BASE CASE: What are dp[0] or dp[0][0] values?
 *            Prevents index-out-of-bounds and seeds the recurrence.
 *
 * THE 5 PATTERNS IN THIS FILE:
 *   1. 1D DP — linear state          (Fibonacci, House Robber, Climbing Stairs)
 *   2. 1D DP — prefix/suffix state   (Stock problems, Kadane's, Min Cost Stairs)
 *   3. 2D DP — grid                  (Unique Paths, Min Path Sum, Dungeon Game)
 *   4. 2D DP — two sequences         (LCS, Edit Distance, Coin Change)
 *   5. Memoization vs Tabulation     (Coin Change implemented both ways)
 *
 * TIME COMPLEXITY:  O(n) for 1D, O(n*m) for 2D — base
 * SPACE COMPLEXITY: O(n) for 1D, reducible to O(1) for some; O(n*m) for 2D,
 *                   reducible to O(n) or O(m) with rolling array
 *
 * INTERVIEW ANGLE:
 *   DP is the most feared interview topic. The secret: you don't need to
 *   "see the answer" — you need the 3-step framework. Interviewers value
 *   clear problem decomposition over magical insight.
 *   Common follow-up: "Can you optimize space?" → rolling array trick.
 *
 * REAL-WORLD USE:
 *   - LCS underlies diff tools (git diff, vimdiff)
 *   - Edit Distance in spell checkers and DNA sequencing
 *   - Coin Change in currency exchange systems
 *   - Shortest path DP in routing protocols (Bellman-Ford)
 */
public class DPFoundations {

    // =========================================================================
    // PATTERN 1: 1D DP — LINEAR STATE
    // =========================================================================
    //
    // STATE: dp[i] depends on a small number of previous states (dp[i-1], dp[i-2])
    // DIRECTION: fill left to right
    // SPACE OPT: if only dp[i-1] needed: use one variable; dp[i-1]+dp[i-2]: two variables
    //
    // TEMPLATE:
    //   dp[0] = base0; dp[1] = base1;
    //   for i in 2..n: dp[i] = f(dp[i-1], dp[i-2])
    //   return dp[n]

    /**
     * Fibonacci — 3 Implementations (teaching the DP spectrum)
     *
     * fib(n) = fib(n-1) + fib(n-2), base: fib(0)=0, fib(1)=1
     *
     * DEFINE: dp[i] = ith Fibonacci number
     * RECURRENCE: dp[i] = dp[i-1] + dp[i-2]
     * BASE CASE: dp[0]=0, dp[1]=1
     */

    // Version 1: Naive recursion — O(2^n) time (exponential — NEVER USE in production)
    // Shows WHY we need DP: fib(5) calls fib(3) twice, fib(2) three times, etc.
    public int fibNaive(int n) {
        if (n <= 1) return n;
        return fibNaive(n - 1) + fibNaive(n - 2); // O(2^n) — recomputes same values!
    }

    // Version 2: Memoization (top-down DP) — O(n) time, O(n) space
    // Add cache to naive recursion. Each fib(i) computed once.
    public int fibMemo(int n) {
        int[] memo = new int[n + 1];
        Arrays.fill(memo, -1);
        return fibHelper(n, memo);
    }
    private int fibHelper(int n, int[] memo) {
        if (n <= 1) return n;
        if (memo[n] != -1) return memo[n]; // cache hit
        memo[n] = fibHelper(n - 1, memo) + fibHelper(n - 2, memo);
        return memo[n];
    }

    // Version 3: Tabulation (bottom-up DP) — O(n) time, O(1) space
    // Iterative, no call stack. Space optimized to 2 variables since only last 2 states needed.
    public int fibTab(int n) {
        if (n <= 1) return n;
        int prev2 = 0, prev1 = 1; // dp[i-2], dp[i-1]
        for (int i = 2; i <= n; i++) {
            int cur = prev1 + prev2; // dp[i] = dp[i-1] + dp[i-2]
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }
    // SPACE OPTIMIZATION: dp[i] only depends on dp[i-1] and dp[i-2]
    //   → don't need the full array, just 2 variables
    //   This reduces O(n) space to O(1). Apply whenever "only last k states needed."

    /**
     * LC 70 — Climbing Stairs (k=1 or k=2 steps at a time)
     *
     * DEFINE: dp[i] = number of distinct ways to reach step i
     * RECURRENCE: dp[i] = dp[i-1] + dp[i-2]
     *             (from step i-1 take 1 step, or from step i-2 take 2 steps)
     * BASE CASE: dp[0]=1 (one way to stand at bottom), dp[1]=1
     *
     * KEY INSIGHT: this IS Fibonacci! dp[n] = fib(n+1)
     *
     * Time: O(n)  Space: O(1)
     */
    public int climbStairs(int n) {
        if (n <= 2) return n;
        int prev2 = 1, prev1 = 2; // dp[1]=1, dp[2]=2
        for (int i = 3; i <= n; i++) {
            int cur = prev1 + prev2;
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }

    /**
     * LC 198 — House Robber
     *
     * DEFINE: dp[i] = max money robbing from houses 0..i
     * RECURRENCE: dp[i] = max(dp[i-1], dp[i-2] + nums[i])
     *             Either skip house i (take dp[i-1]),
     *             or rob house i + best from 2 houses back (dp[i-2] + nums[i])
     * BASE CASE: dp[0]=nums[0], dp[1]=max(nums[0],nums[1])
     *
     * Time: O(n)  Space: O(1) with space optimization
     */
    public int rob(int[] nums) {
        if (nums.length == 1) return nums[0];
        int prev2 = nums[0], prev1 = Math.max(nums[0], nums[1]);
        for (int i = 2; i < nums.length; i++) {
            int cur = Math.max(prev1, prev2 + nums[i]); // skip or rob
            prev2 = prev1;
            prev1 = cur;
        }
        return prev1;
    }
    // INTERVIEW FOLLOW-UP: "What if houses are in a circle?" (LC 213 — House Robber II)
    // → Run house robber twice: once on [0..n-2], once on [1..n-1], take max.
    //   (Can't rob first AND last since they're adjacent in a circle.)

    /**
     * LC 55 — Jump Game (can you reach the last index?)
     *
     * DP APPROACH: dp[i] = true if reachable from index 0.
     * Greedy is simpler (track maxReach), but DP illustrates the pattern.
     *
     * DEFINE: dp[i] = can we reach index i from index 0?
     * RECURRENCE: dp[i] = any j < i where dp[j] == true AND j + nums[j] >= i
     * BASE CASE: dp[0] = true
     *
     * Time: O(n²) DP version  Space: O(n)
     * Greedy version: O(n) time, O(1) space — see below
     */
    public boolean canJumpDP(int[] nums) {
        boolean[] dp = new boolean[nums.length];
        dp[0] = true;
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && j + nums[j] >= i) {
                    dp[i] = true;
                    break;
                }
            }
        }
        return dp[nums.length - 1];
    }

    // Greedy version — O(n), O(1): track farthest reachable index
    public boolean canJumpGreedy(int[] nums) {
        int maxReach = 0;
        for (int i = 0; i < nums.length; i++) {
            if (i > maxReach) return false;           // can't reach index i
            maxReach = Math.max(maxReach, i + nums[i]); // update farthest reachable
        }
        return true;
    }
    // Q: When to prefer Greedy over DP?
    // A: When local optimal choice always leads to global optimal. Here: always
    //    maximize reach → greedy works. But LC 45 (min jumps) still uses DP reasoning.

    /**
     * LC 45 — Jump Game II (minimum jumps to reach last index)
     *
     * BFS / Greedy with levels (each "level" = one jump).
     * At each jump, explore all positions reachable from current level
     * and track the farthest reachable for the next level.
     *
     * Time: O(n)  Space: O(1)
     */
    public int jump(int[] nums) {
        int jumps = 0, currentEnd = 0, farthest = 0;
        for (int i = 0; i < nums.length - 1; i++) { // don't jump past last index
            farthest = Math.max(farthest, i + nums[i]); // update farthest this jump can reach
            if (i == currentEnd) {    // reached end of current jump's range
                jumps++;              // must make another jump
                currentEnd = farthest; // next jump's range starts here
            }
        }
        return jumps;
    }

    // =========================================================================
    // PATTERN 2: 1D DP — PREFIX / SUFFIX STATE
    // =========================================================================
    //
    // STATE: dp[i] = best result ENDING at index i (prefix) or STARTING at index i (suffix)
    // DIRECTION: depends — prefix fills left-to-right, suffix fills right-to-left
    // KEY: Kadane's algorithm is the canonical example of prefix DP

    /**
     * LC 53 — Maximum Subarray (Kadane's Algorithm)
     *
     * DEFINE: dp[i] = max sum subarray ENDING AT index i
     * RECURRENCE: dp[i] = max(nums[i], dp[i-1] + nums[i])
     *             Either start fresh at i, or extend the previous subarray
     * BASE CASE: dp[0] = nums[0]
     *
     * KADANE'S INSIGHT: if dp[i-1] < 0, extending it only hurts → start fresh.
     * This is the "reset when negative" rule.
     *
     * Time: O(n)  Space: O(1)
     */
    public int maxSubArray(int[] nums) {
        int maxEndingHere = nums[0]; // dp[i] — max subarray sum ending at i
        int globalMax = nums[0];

        for (int i = 1; i < nums.length; i++) {
            // extend previous subarray OR start fresh — whichever is better
            maxEndingHere = Math.max(nums[i], maxEndingHere + nums[i]);
            globalMax = Math.max(globalMax, maxEndingHere);
        }
        return globalMax;
    }
    // INTERVIEW FOLLOW-UP: "Return the actual subarray too?"
    // → Track start, end, and tempStart indices.
    //   When maxEndingHere resets (nums[i] > maxEndingHere + nums[i]): tempStart = i
    //   When globalMax updates: start = tempStart, end = i

    /**
     * LC 121 — Best Time to Buy and Sell Stock (one transaction)
     *
     * DEFINE: minSoFar = minimum price seen so far (best buy day)
     *         profit = max(profit, price - minSoFar)
     * ALTERNATIVE DP: dp[i] = max profit if sold on day i = prices[i] - minPrice[0..i]
     *
     * Time: O(n)  Space: O(1)
     */
    public int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE, maxProfit = 0;
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);          // best buy day so far
            maxProfit = Math.max(maxProfit, price - minPrice); // profit if sold today
        }
        return maxProfit;
    }

    /**
     * LC 122 — Best Time to Buy and Sell Stock II (unlimited transactions)
     *
     * KEY INSIGHT: sum up all positive differences (greedy = collect every upswing).
     * With unlimited transactions, "buy low sell high" every day = optimal.
     *
     * Time: O(n)  Space: O(1)
     */
    public int maxProfitII(int[] prices) {
        int profit = 0;
        for (int i = 1; i < prices.length; i++) {
            if (prices[i] > prices[i - 1]) {
                profit += prices[i] - prices[i - 1]; // collect every upswing
            }
        }
        return profit;
    }

    /**
     * LC 746 — Min Cost Climbing Stairs
     *
     * DEFINE: dp[i] = min cost to reach step i (0-indexed)
     * RECURRENCE: dp[i] = min(dp[i-1], dp[i-2]) + cost[i]
     * BASE CASE: dp[0]=cost[0], dp[1]=cost[1]
     * ANSWER: min(dp[n-1], dp[n-2]) — can step from last two positions to top
     *
     * Time: O(n)  Space: O(1)
     */
    public int minCostClimbingStairs(int[] cost) {
        int n = cost.length;
        int prev2 = cost[0], prev1 = cost[1];
        for (int i = 2; i < n; i++) {
            int cur = Math.min(prev1, prev2) + cost[i];
            prev2 = prev1;
            prev1 = cur;
        }
        return Math.min(prev1, prev2);
    }

    // =========================================================================
    // PATTERN 3: 2D DP — GRID
    // =========================================================================
    //
    // STATE: dp[r][c] = best result reaching cell (r, c)
    // TRANSITION: from top dp[r-1][c] or from left dp[r][c-1]
    //             (some problems: also diagonal, or from any direction)
    // BASE CASE: first row (only reachable from left) + first column (only from top)
    //
    // SPACE OPT: if transition only uses previous row, use 1D rolling array O(cols)
    //
    // DIRECTION TRICK: some problems fill bottom-right to top-left
    //   (e.g., Dungeon Game — where "best" means something about future cells)

    /**
     * LC 62 — Unique Paths
     *
     * DEFINE: dp[r][c] = number of unique paths from (0,0) to (r,c)
     * RECURRENCE: dp[r][c] = dp[r-1][c] + dp[r][c-1]  (from top or from left)
     * BASE CASE: dp[0][c] = 1 (top row), dp[r][0] = 1 (left column)
     *
     * Time: O(m*n)  Space: O(m*n), optimizable to O(n) with rolling row
     */
    public int uniquePaths(int m, int n) {
        int[] dp = new int[n]; // rolling array — only need previous row
        Arrays.fill(dp, 1);    // base: first row is all 1s

        for (int r = 1; r < m; r++) {
            for (int c = 1; c < n; c++) {
                dp[c] += dp[c - 1]; // dp[c] = old dp[c] (from above) + dp[c-1] (from left)
            }
        }
        return dp[n - 1];
    }
    // MATHEMATICAL ALTERNATIVE: C(m+n-2, m-1) — choose which m-1 of m+n-2 steps go down
    // O(m+n) time, O(1) space. But DP generalizes to obstacles; math doesn't.

    /**
     * LC 63 — Unique Paths II (with obstacles)
     *
     * Obstacle cells get dp=0; everything else: dp[r][c] = dp[r-1][c] + dp[r][c-1].
     * If obstacle is at (0,0) → 0 paths. Once a base case is blocked, all paths through it = 0.
     *
     * Time: O(m*n)  Space: O(n) with rolling row
     */
    public int uniquePathsWithObstacles(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        int[] dp = new int[n];
        dp[0] = grid[0][0] == 1 ? 0 : 1; // base: start cell

        for (int c = 1; c < n; c++) {
            dp[c] = grid[0][c] == 1 ? 0 : dp[c - 1]; // first row: blocked → 0, else carry left
        }

        for (int r = 1; r < m; r++) {
            dp[0] = grid[r][0] == 1 ? 0 : dp[0]; // first column: blocked → 0
            for (int c = 1; c < n; c++) {
                dp[c] = grid[r][c] == 1 ? 0 : dp[c] + dp[c - 1];
            }
        }
        return dp[n - 1];
    }

    /**
     * LC 64 — Minimum Path Sum
     *
     * DEFINE: dp[r][c] = minimum sum path from (0,0) to (r,c)
     * RECURRENCE: dp[r][c] = grid[r][c] + min(dp[r-1][c], dp[r][c-1])
     * BASE CASE: dp[0][0]=grid[0][0]; first row/col = cumulative sum
     *
     * Time: O(m*n)  Space: O(1) by modifying grid in place
     */
    public int minPathSum(int[][] grid) {
        int m = grid.length, n = grid[0].length;

        // base case: first row (can only come from left)
        for (int c = 1; c < n; c++) grid[0][c] += grid[0][c - 1];
        // base case: first column (can only come from above)
        for (int r = 1; r < m; r++) grid[r][0] += grid[r - 1][0];

        for (int r = 1; r < m; r++) {
            for (int c = 1; c < n; c++) {
                grid[r][c] += Math.min(grid[r-1][c], grid[r][c-1]); // from top or left
            }
        }
        return grid[m - 1][n - 1];
    }

    /**
     * LC 174 — Dungeon Game (find minimum initial health)
     *
     * TWIST: fill FROM BOTTOM-RIGHT TO TOP-LEFT because health needed depends on
     * future cells, not past. "What minimum health do I need AT this cell to survive?"
     *
     * DEFINE: dp[r][c] = minimum health needed ENTERING cell (r,c) to reach exit
     * RECURRENCE: dp[r][c] = max(1, min(dp[r+1][c], dp[r][c+1]) - dungeon[r][c])
     *   min(right, down) = minimum health needed after leaving (r,c)
     *   subtract dungeon[r][c]: if dungeon[r][c] restores health, need less entering
     *   max(1, ...): health must be at least 1 (can't enter dead)
     * BASE CASE: dp[m][n] (fictitious cell beyond exit) = 1
     *            border cells: only one direction available
     *
     * Time: O(m*n)  Space: O(m*n), reducible to O(n)
     */
    public int calculateMinimumHP(int[][] dungeon) {
        int m = dungeon.length, n = dungeon[0].length;
        int[][] dp = new int[m + 1][n + 1];
        // initialize with MAX so border "off-grid" cells don't affect min()
        for (int[] row : dp) Arrays.fill(row, Integer.MAX_VALUE);
        dp[m][n - 1] = 1; dp[m - 1][n] = 1; // virtual cell requiring 1 health

        for (int r = m - 1; r >= 0; r--) {
            for (int c = n - 1; c >= 0; c--) {
                int minHealthAfter = Math.min(dp[r + 1][c], dp[r][c + 1]);
                dp[r][c] = Math.max(1, minHealthAfter - dungeon[r][c]);
            }
        }
        return dp[0][0];
    }
    // INTERVIEW INSIGHT: this is the "aha" DP problem. Forward DP fails because
    // maximizing current health doesn't guarantee reaching the exit alive.
    // Backward DP asks "what do I NEED here?" — reframe the state definition.

    // =========================================================================
    // PATTERN 4: 2D DP — TWO SEQUENCES
    // =========================================================================
    //
    // STATE: dp[i][j] = answer considering first i elements of seq1, first j of seq2
    // TRANSITION: depends on whether seq1[i-1] == seq2[j-1]
    //   Match:   dp[i][j] = dp[i-1][j-1] + something
    //   No match: dp[i][j] = min/max of (dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    // BASE CASE: dp[0][*] and dp[*][0] (empty string matches)
    //
    // VISUAL TRICK: draw the 2D grid on paper, fill cell by cell.
    //   The recurrence "falls out" when you realize what each cell means.

    /**
     * LC 1143 — Longest Common Subsequence (LCS)
     *
     * DEFINE: dp[i][j] = LCS length of text1[0..i-1] and text2[0..j-1]
     * RECURRENCE:
     *   text1[i-1] == text2[j-1]: dp[i][j] = dp[i-1][j-1] + 1  (chars match → extend LCS)
     *   else: dp[i][j] = max(dp[i-1][j], dp[i][j-1])            (skip one char from either)
     * BASE CASE: dp[0][*] = 0, dp[*][0] = 0 (empty string → LCS = 0)
     *
     * WHY DOES THIS WORK: every subsequence of text1[0..i] is either
     *   - also a subsequence of text1[0..i-1] (doesn't use char i), or
     *   - uses char i, so the last char matches some char in text2
     *
     * REAL-WORLD: git diff uses LCS to compute minimal edit sets between file versions
     *
     * Time: O(m*n)  Space: O(m*n), optimizable to O(min(m,n))
     */
    public int longestCommonSubsequence(String text1, String text2) {
        int m = text1.length(), n = text2.length();
        int[][] dp = new int[m + 1][n + 1]; // +1 for empty string base cases

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                    dp[i][j] = dp[i-1][j-1] + 1; // chars match → extend LCS by 1
                } else {
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]); // skip one char
                }
            }
        }
        return dp[m][n];
    }

    /**
     * LC 72 — Edit Distance (Levenshtein Distance)
     *
     * DEFINE: dp[i][j] = min edits to convert word1[0..i-1] to word2[0..j-1]
     * RECURRENCE:
     *   word1[i-1] == word2[j-1]: dp[i][j] = dp[i-1][j-1]      (no edit needed)
     *   else: dp[i][j] = 1 + min(
     *     dp[i-1][j],   // delete char from word1 (use word1[0..i-2] → word2[0..j-1])
     *     dp[i][j-1],   // insert char into word1 (use word1[0..i-1] → word2[0..j-2])
     *     dp[i-1][j-1]  // replace char in word1  (use word1[0..i-2] → word2[0..j-2])
     *   )
     * BASE CASE: dp[i][0] = i (delete all chars), dp[0][j] = j (insert all chars)
     *
     * REAL-WORLD: spell checkers, DNA sequence alignment, keyboard autocorrect
     *
     * Time: O(m*n)  Space: O(m*n)
     */
    public int minDistance(String word1, String word2) {
        int m = word1.length(), n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        // base cases: converting to/from empty string
        for (int i = 0; i <= m; i++) dp[i][0] = i; // delete i chars
        for (int j = 0; j <= n; j++) dp[0][j] = j; // insert j chars

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i-1][j-1]; // chars match → no edit needed
                } else {
                    dp[i][j] = 1 + Math.min(dp[i-1][j-1],           // replace
                                    Math.min(dp[i-1][j], dp[i][j-1])); // delete, insert
                }
            }
        }
        return dp[m][n];
    }

    // =========================================================================
    // PATTERN 5: MEMOIZATION VS TABULATION SIDE-BY-SIDE
    // =========================================================================
    //
    // MEMOIZATION (top-down):
    //   + Computes only needed states (lazy)
    //   + Easier to derive — just add cache to recursive solution
    //   - Call stack overhead; can stack overflow for large inputs
    //   - Not cache-friendly (jumps around memory)
    //
    // TABULATION (bottom-up):
    //   + No recursion → no stack overflow
    //   + Cache-friendly iteration
    //   + Easier to apply space optimization (rolling array)
    //   - Must compute ALL states (even unneeded ones)
    //   - Requires thinking about fill ORDER (dependency direction)
    //
    // RECOMMENDATION: In interviews, start with memoization (easier to code),
    //   then mention tabulation as the space-optimizable follow-up.

    /**
     * LC 322 — Coin Change (minimum coins to make amount)
     *
     * DEFINE: dp[i] = min coins needed to make amount i
     * RECURRENCE: dp[i] = min over all coins c: 1 + dp[i - c]  (if i >= c)
     * BASE CASE: dp[0] = 0 (0 coins for amount 0), dp[i] = INF initially
     *
     * KEY INSIGHT: the OUTER loop can be over amount (fill) or over coins (both work).
     *   Amount outer, coins inner: standard BFS-like fill.
     *
     * Time: O(amount * |coins|)  Space: O(amount)
     */

    // Memoization version (top-down)
    public int coinChangeMemo(int[] coins, int amount) {
        int[] memo = new int[amount + 1];
        Arrays.fill(memo, -1);
        memo[0] = 0;
        int result = coinChangeHelper(coins, amount, memo);
        return result == Integer.MAX_VALUE ? -1 : result;
    }
    private int coinChangeHelper(int[] coins, int rem, int[] memo) {
        if (rem < 0) return Integer.MAX_VALUE; // can't make this amount
        if (memo[rem] != -1) return memo[rem];

        int minCoins = Integer.MAX_VALUE;
        for (int coin : coins) {
            int sub = coinChangeHelper(coins, rem - coin, memo);
            if (sub != Integer.MAX_VALUE) {
                minCoins = Math.min(minCoins, sub + 1);
            }
        }
        return memo[rem] = minCoins;
    }

    // Tabulation version (bottom-up)
    public int coinChangeTab(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1); // use amount+1 as "infinity" (impossible amount)
        dp[0] = 0;                   // base: 0 coins to make amount 0

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    dp[i] = Math.min(dp[i], dp[i - coin] + 1); // use this coin
                }
            }
        }
        return dp[amount] > amount ? -1 : dp[amount]; // > amount means impossible
    }

    /**
     * LC 518 — Coin Change II (number of combinations to make amount)
     *
     * vs Coin Change: instead of minimum coins, count the number of WAYS.
     *
     * CRITICAL DIFFERENCE — LOOP ORDER:
     *   Coin Change (min): either loop order works (order doesn't matter)
     *   Coin Change II (count combos): coins OUTER, amount INNER
     *     → ensures each coin considered only as part of a non-repeating COMBINATION
     *     → amount outer, coins inner would count PERMUTATIONS (different orderings)
     *
     * DEFINE: dp[i] = number of combinations to make amount i
     * RECURRENCE: dp[i] += dp[i - coin]  for each coin
     * BASE CASE: dp[0] = 1 (one way to make 0: use no coins)
     *
     * Time: O(amount * |coins|)  Space: O(amount)
     */
    public int change(int amount, int[] coins) {
        int[] dp = new int[amount + 1];
        dp[0] = 1; // base: one way to make 0 (empty selection)

        // OUTER = coins: each coin is added to all previously-built combinations
        // This ensures [1,2] and [2,1] are counted as ONE combination
        for (int coin : coins) {
            for (int i = coin; i <= amount; i++) {
                dp[i] += dp[i - coin]; // add ways using this coin
            }
        }
        return dp[amount];
    }
    // INTERVIEW Q: "Why coins outer, amount inner for combinations?"
    // A: By fixing the outer loop as coins, when we process coin C,
    //    all previous dp[i] values already represent combinations using ONLY
    //    coins before C. So adding C extends those combinations without reordering.
    //    If amount were outer, dp[5] could use coin [2,3] or [3,2] separately →
    //    permutation count, not combination count.

    // =========================================================================
    // THE 3-STEP FRAMEWORK APPLIED (INTERVIEW TEMPLATE)
    // =========================================================================
    //
    // When faced with a new DP problem in an interview:
    //
    // STEP 1 — DEFINE (say this out loud):
    //   "Let dp[i] represent ___. Specifically, dp[i] = ___."
    //
    // STEP 2 — RECURRENCE (derive it from the definition):
    //   "To compute dp[i], I look at the last choice made:
    //    If [choice A] → dp[i] = [expression using smaller states]
    //    If [choice B] → dp[i] = [expression using smaller states]
    //    Take min/max of all choices."
    //
    // STEP 3 — BASE CASE (fill in the edges):
    //   "dp[0] = ___ because [reason from definition]"
    //
    // THEN: code it as memoization first (easiest), then offer to convert to tabulation.
    //
    // COMMON MISTAKES:
    //   - Defining dp[i] ambiguously → recurrence won't follow naturally
    //   - Forgetting base cases → IndexOutOfBounds
    //   - Wrong loop order for counting combinations
    //   - Not initializing dp to INF/0 appropriately

    public static void main(String[] args) {
        DPFoundations dp = new DPFoundations();

        // Pattern 1: 1D Linear
        System.out.println("=== 1D Linear DP ===");
        System.out.println("Fib(10): " + dp.fibTab(10));        // 55
        System.out.println("Climb stairs(5): " + dp.climbStairs(5)); // 8
        System.out.println("Rob [2,7,9,3,1]: " + dp.rob(new int[]{2,7,9,3,1})); // 12
        System.out.println("Jump Game [2,3,1,1,4]: " + dp.canJumpGreedy(new int[]{2,3,1,1,4})); // true
        System.out.println("Min jumps [2,3,1,1,4]: " + dp.jump(new int[]{2,3,1,1,4})); // 2

        // Pattern 2: Prefix/Suffix
        System.out.println("\n=== Prefix/Suffix DP ===");
        System.out.println("Max subarray [-2,1,-3,4,-1,2,1,-5,4]: " +
            dp.maxSubArray(new int[]{-2,1,-3,4,-1,2,1,-5,4})); // 6
        System.out.println("Max profit [7,1,5,3,6,4]: " +
            dp.maxProfit(new int[]{7,1,5,3,6,4})); // 5

        // Pattern 3: 2D Grid
        System.out.println("\n=== 2D Grid DP ===");
        System.out.println("Unique paths 3x7: " + dp.uniquePaths(3, 7)); // 28
        System.out.println("Min path sum: " +
            dp.minPathSum(new int[][]{{1,3,1},{1,5,1},{4,2,1}})); // 7

        // Pattern 4: Two Sequences
        System.out.println("\n=== Two-Sequence DP ===");
        System.out.println("LCS abcde,ace: " + dp.longestCommonSubsequence("abcde","ace")); // 3
        System.out.println("Edit distance horse,ros: " + dp.minDistance("horse","ros")); // 3

        // Pattern 5: Memo vs Tab
        System.out.println("\n=== Coin Change ===");
        System.out.println("Memo [1,2,5] → 11: " + dp.coinChangeMemo(new int[]{1,2,5}, 11)); // 3
        System.out.println("Tab  [1,2,5] → 11: " + dp.coinChangeTab(new int[]{1,2,5}, 11));  // 3
        System.out.println("Combinations [1,2,5] → 5: " + dp.change(5, new int[]{1,2,5}));  // 4
    }
}
