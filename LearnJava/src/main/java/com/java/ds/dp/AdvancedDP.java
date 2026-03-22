package com.java.ds.dp;

/**
 * ADVANCED DYNAMIC PROGRAMMING — 5 Essential Problems
 *
 * DP FRAMEWORK (applies to all problems below):
 *   1. DEFINE the subproblem: what does dp[i] or dp[i][j] represent?
 *   2. RECURRENCE: how does dp[i] depend on smaller subproblems?
 *   3. BASE CASES: the smallest valid subproblems with known answers.
 *   4. ORDER: ensure subproblems are solved before they're needed.
 *   5. ANSWER: which dp entry holds the final answer?
 *
 * PROBLEMS COVERED:
 *   1. Coin Change — LC 322                  (unbounded knapsack variant)
 *   2. Longest Increasing Subsequence — LC 300  (patience sort / DP)
 *   3. Word Break — LC 139                   (string segmentation DP)
 *   4. Unique Paths — LC 62                  (grid DP)
 *   5. Edit Distance — LC 72                 (2D DP, classic)
 *
 * INTERVIEW FREQUENCY: All 5 are in the "must-know" list for FAANG.
 *
 * SEE ALSO: LargestCommonSubsequence.java, LongestPalindomicSubsequence.java
 *   for the DP template pattern (memoization + backtracking).
 */
public class AdvancedDP {

    public static void main(String[] args) {

        System.out.println("=== LC 322: Coin Change ===");
        System.out.println(coinChange(new int[]{1, 5, 11}, 15)); // Expected: 3 (5+5+5 or 11+1+1+1+1 → 5+5+5 better? 15=11+4→11+1+1+1+1=5 coins. Wait: 5+5+5=3 coins)
        System.out.println(coinChange(new int[]{1, 2, 5}, 11));  // Expected: 3 (5+5+1)
        System.out.println(coinChange(new int[]{2}, 3));          // Expected: -1 (impossible)
        System.out.println(coinChange(new int[]{1}, 0));          // Expected: 0

        System.out.println("\n=== LC 300: Longest Increasing Subsequence (LIS) ===");
        System.out.println(lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18})); // Expected: 4 [2,3,7,101]
        System.out.println(lengthOfLIS(new int[]{0, 1, 0, 3, 2, 3}));           // Expected: 4 [0,1,2,3]
        System.out.println(lengthOfLIS(new int[]{7, 7, 7, 7}));                  // Expected: 1

        System.out.println("\n=== LC 139: Word Break ===");
        System.out.println(wordBreak("leetcode", new String[]{"leet", "code"}));            // Expected: true
        System.out.println(wordBreak("applepenapple", new String[]{"apple", "pen"}));       // Expected: true
        System.out.println(wordBreak("catsandog", new String[]{"cats", "dog", "sand", "and", "cat"})); // Expected: false

        System.out.println("\n=== LC 62: Unique Paths ===");
        System.out.println(uniquePaths(3, 7)); // Expected: 28
        System.out.println(uniquePaths(3, 2)); // Expected: 3
        System.out.println(uniquePaths(1, 1)); // Expected: 1

        System.out.println("\n=== LC 72: Edit Distance ===");
        System.out.println(minDistance("horse", "ros"));   // Expected: 3
        System.out.println(minDistance("intention", "execution")); // Expected: 5
        System.out.println(minDistance("", "abc"));         // Expected: 3 (3 insertions)
        System.out.println(minDistance("abc", ""));         // Expected: 3 (3 deletions)
        System.out.println(minDistance("abc", "abc"));      // Expected: 0
    }

    // =========================================================================
    // LC 322 — Coin Change
    // =========================================================================

    /**
     * Minimum number of coins to make amount. Infinite supply of each coin.
     *
     * SUBPROBLEM: dp[i] = minimum coins needed to make amount i.
     * BASE CASE: dp[0] = 0 (zero coins to make amount 0).
     * RECURRENCE: dp[i] = min over all coins c: 1 + dp[i - c]  (if i >= c)
     *   "Use coin c, then solve the remaining amount i-c optimally."
     * ANSWER: dp[amount], or -1 if dp[amount] == INF (impossible).
     *
     * WHY BOTTOM-UP?
     *   dp[i] depends on dp[i - coin] for all coins. Since coin > 0, all dependencies
     *   are smaller than i → fill table from 0 to amount.
     *
     * Time: O(amount × numCoins)  Space: O(amount)
     */
    public static int coinChange(int[] coins, int amount) {
        int INF = amount + 1; // Use amount+1 as "infinity" (more than any valid answer)
        int[] dp = new int[amount + 1];
        java.util.Arrays.fill(dp, INF); // Initialize all as impossible
        dp[0] = 0; // Base case: 0 coins for amount 0

        for (int i = 1; i <= amount; i++) {
            for (int coin : coins) {
                if (coin <= i) {
                    // Option: use this coin and solve remaining (i - coin)
                    dp[i] = Math.min(dp[i], 1 + dp[i - coin]);
                }
            }
        }

        return dp[amount] == INF ? -1 : dp[amount];
    }

    // =========================================================================
    // LC 300 — Longest Increasing Subsequence (LIS)
    // =========================================================================

    /**
     * Returns length of the longest strictly increasing subsequence.
     *
     * SUBPROBLEM: dp[i] = length of LIS ending at index i.
     * BASE CASE: dp[i] = 1 for all i (each element alone is a subsequence of length 1).
     * RECURRENCE: dp[i] = max over all j < i where nums[j] < nums[i]: 1 + dp[j]
     *   "Extend the best LIS ending at j by adding nums[i]."
     * ANSWER: max of all dp[i].
     *
     * PATIENCE SORT / BINARY SEARCH APPROACH: O(n log n) — if asked for optimal.
     *   This O(n²) DP is easier to explain in an interview setting.
     *
     * Time: O(n²)  Space: O(n)
     */
    public static int lengthOfLIS(int[] nums) {
        int n = nums.length;
        int[] dp = new int[n];
        java.util.Arrays.fill(dp, 1); // Each element is an LIS of length 1

        int maxLen = 1;

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[j] < nums[i]) {
                    // nums[i] can extend the LIS ending at j
                    dp[i] = Math.max(dp[i], 1 + dp[j]);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }

        return maxLen;
    }

    // =========================================================================
    // LC 139 — Word Break
    // =========================================================================

    /**
     * Returns true if s can be segmented into words from the dictionary.
     *
     * SUBPROBLEM: dp[i] = true if the prefix s[0..i-1] can be segmented.
     * BASE CASE: dp[0] = true (empty string is always valid).
     * RECURRENCE:
     *   dp[i] = true if there exists j < i such that:
     *     dp[j] == true AND s.substring(j, i) is in the dictionary.
     *   "The prefix s[0..j-1] is valid AND s[j..i-1] is a dictionary word."
     * ANSWER: dp[s.length()].
     *
     * Time: O(n³) — n² pairs (i,j), each substring check is O(n) in a HashSet.
     *   (HashSet.contains is O(length of string) due to hashing.)
     *   Using a Trie reduces to O(n²).
     */
    public static boolean wordBreak(String s, String[] wordDict) {
        java.util.Set<String> dict = new java.util.HashSet<>(java.util.Arrays.asList(wordDict));
        int n = s.length();
        boolean[] dp = new boolean[n + 1];
        dp[0] = true; // Base case: empty prefix is always "segmentable"

        for (int i = 1; i <= n; i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && dict.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break; // One valid segmentation is enough — no need to check more j's
                }
            }
        }

        return dp[n];
    }

    // =========================================================================
    // LC 62 — Unique Paths
    // =========================================================================

    /**
     * Count unique paths from top-left to bottom-right in an m×n grid.
     * Robot can only move RIGHT or DOWN.
     *
     * SUBPROBLEM: dp[i][j] = number of unique paths to reach cell (i, j).
     * BASE CASE: dp[0][j] = 1 for all j (top row: only one way — go straight right).
     *            dp[i][0] = 1 for all i (left column: only one way — go straight down).
     * RECURRENCE: dp[i][j] = dp[i-1][j] + dp[i][j-1]
     *   "Paths to (i,j) = paths from above (came down) + paths from left (came right)."
     * ANSWER: dp[m-1][n-1].
     *
     * MATHEMATICAL ALTERNATIVE: C(m+n-2, m-1) — combinatorics.
     *   Total moves = m+n-2. Choose m-1 of them to be downward moves.
     *   Mention this to interviewers to show breadth.
     *
     * SPACE OPTIMIZATION: Only need the current and previous row → O(n) space.
     *
     * Time: O(m × n)  Space: O(m × n) — optimizable to O(n).
     */
    public static int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];

        // Base cases: first row and first column = 1 each
        for (int i = 0; i < m; i++) dp[i][0] = 1; // Left column
        for (int j = 0; j < n; j++) dp[0][j] = 1; // Top row

        // Fill remaining cells
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i - 1][j] + dp[i][j - 1]; // From above + from left
            }
        }

        return dp[m - 1][n - 1];
    }

    // =========================================================================
    // LC 72 — Edit Distance (Levenshtein Distance)
    // =========================================================================

    /**
     * Minimum number of operations (insert, delete, replace) to convert word1 to word2.
     *
     * SUBPROBLEM: dp[i][j] = edit distance between word1[0..i-1] and word2[0..j-1].
     * BASE CASES:
     *   dp[i][0] = i  (delete all i chars from word1 to match empty word2)
     *   dp[0][j] = j  (insert j chars to match word2 from empty word1)
     * RECURRENCE:
     *   if word1[i-1] == word2[j-1]: dp[i][j] = dp[i-1][j-1]   (no operation needed)
     *   else: dp[i][j] = 1 + min(
     *     dp[i-1][j],    // DELETE word1[i-1] (solve word1[0..i-2] vs word2[0..j-1])
     *     dp[i][j-1],    // INSERT word2[j-1] into word1 (solve word1[0..i-1] vs word2[0..j-2])
     *     dp[i-1][j-1]   // REPLACE word1[i-1] with word2[j-1] (solve word1[0..i-2] vs word2[0..j-2])
     *   )
     *
     * INTERVIEW TIP: This is the hardest DP in the list. Take time to explain the 3-way min.
     *
     * Time: O(m × n)  Space: O(m × n) — optimizable to O(min(m,n)).
     */
    public static int minDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        // Base cases
        for (int i = 0; i <= m; i++) dp[i][0] = i; // Delete all chars from word1
        for (int j = 0; j <= n; j++) dp[0][j] = j; // Insert all chars for word2

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    // Characters match — no operation needed at this position
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    // Choose the cheapest of the 3 operations
                    dp[i][j] = 1 + Math.min(
                        dp[i - 1][j],     // Delete word1[i-1]
                        Math.min(
                            dp[i][j - 1], // Insert word2[j-1]
                            dp[i - 1][j - 1] // Replace word1[i-1] with word2[j-1]
                        )
                    );
                }
            }
        }

        return dp[m][n];
    }
}
