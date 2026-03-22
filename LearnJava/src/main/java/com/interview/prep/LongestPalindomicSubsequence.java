package com.interview.prep;

import java.util.Arrays;

/**
 * LONGEST PALINDROMIC SUBSEQUENCE (LPS) — LC 516
 *
 * PROBLEM:
 *   Given string s, return the length of the longest subsequence that is a palindrome.
 *   A subsequence is a sequence derived by deleting some (or no) characters without
 *   changing the order of the remaining characters.
 *
 *   Example: s = "bbbba"
 *     LPS = "bbbb" → length 4
 *
 * THE KEY INSIGHT — Reduction to LCS:
 *   A palindromic subsequence reads the same forwards and backwards.
 *   → LPS(s) == LCS(s, reverse(s))
 *   WHY? Any character that appears in the LCS of s and its reverse must be part
 *   of a palindrome, because matching s[i] with reverse(s)[j] means matching
 *   characters that are symmetric in the original string.
 *
 *   This reduces LPS to a problem we already know how to solve.
 *   TEMPLATE: See LargestCommonSubsequence.java for the LCS memoization pattern.
 *
 * ALTERNATIVE (direct DP without reduction):
 *   dp[i][j] = LPS length of s[i..j]
 *   Base: dp[i][i] = 1 (single char is palindrome)
 *   If s[i] == s[j]: dp[i][j] = 2 + dp[i+1][j-1]
 *   Else: dp[i][j] = max(dp[i+1][j], dp[i][j-1])
 *   Fill diagonally (substring length 2, 3, ... n)
 *
 * INTERVIEW FREQUENCY: Medium-high at FAANG. Tests whether you recognize
 *   the LCS reduction — a key "problem transformation" skill.
 *
 * FOLLOW-UP QUESTIONS:
 *   1. "Reconstruct the actual palindromic subsequence (not just the length)."
 *   2. "What is the minimum number of deletions to make s a palindrome?"
 *      Answer: n - LPS(s)  (delete all characters NOT in the LPS)
 *   3. "What is the minimum insertions to make s a palindrome?"
 *      Answer: also n - LPS(s)  (same formula — insert mirror of deleted chars)
 *
 * Time: O(n²)   Space: O(n²) for memoization table
 */
public class LongestPalindomicSubsequence {

    // Memoization table shared across calls (same pattern as LargestCommonSubsequence.java)
    private static int[][] memo;

    public static void main(String[] args) {
        System.out.println("=== Longest Palindromic Subsequence ===");

        // --- Normal cases ---
        test("bbbba");    // Expected: 4 ("bbbb")
        test("cbbd");     // Expected: 2 ("bb")
        test("agbdba");   // Expected: 5 ("abdba")

        // --- Edge cases ---
        test("a");        // Expected: 1 (single char is a palindrome)
        test("aa");       // Expected: 2
        test("ab");       // Expected: 1 ("a" or "b")
        test("abcd");     // Expected: 1 (no repeating chars)

        // --- Already a palindrome ---
        test("racecar");  // Expected: 7 (the whole string)

        // --- Reconstruction ---
        System.out.println("\n=== Reconstructed Palindromic Subsequence ===");
        System.out.println("bbbba   → " + reconstructLPS("bbbba"));   // "bbbb"
        System.out.println("agbdba  → " + reconstructLPS("agbdba"));  // "abdba"
        System.out.println("cbbd    → " + reconstructLPS("cbbd"));    // "bb"

        // --- Minimum deletions follow-up ---
        System.out.println("\n=== Min Deletions to Make Palindrome (n - LPS) ===");
        String s = "abcba";
        int lps = longestPalindromeSubseq(s);
        System.out.println("\"" + s + "\" → min deletions: " + (s.length() - lps)); // 0
        s = "abcd";
        lps = longestPalindromeSubseq(s);
        System.out.println("\"" + s + "\" → min deletions: " + (s.length() - lps)); // 3
    }

    static void test(String s) {
        int result = longestPalindromeSubseq(s);
        System.out.printf("  %-10s → LPS length: %d%n", "\"" + s + "\"", result);
    }

    // -------------------------------------------------------------------------
    // APPROACH 1: LCS Reduction — LPS(s) = LCS(s, reverse(s))
    // -------------------------------------------------------------------------

    /**
     * Computes LPS by reducing to LCS(s, reverse(s)).
     *
     * WHY THIS WORKS:
     *   - Let r = reverse of s.
     *   - Any common subsequence of s and r is also a palindrome because
     *     characters matched between s[i] and r[j] correspond to positions
     *     that mirror each other in the original string.
     *   - The LONGEST such common subsequence is the LPS.
     *
     * We reuse the exact same memoized LCS logic from LargestCommonSubsequence.java.
     */
    public static int longestPalindromeSubseq(String s) {
        String rev = new StringBuilder(s).reverse().toString(); // reverse of s
        int n = s.length();

        // Initialize memoization table with -1 (same pattern as LCS template)
        memo = new int[n + 1][n + 1];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }

        return lcs(s, rev, n, n);
    }

    /**
     * Standard memoized LCS — same implementation as LargestCommonSubsequence.java.
     * Placed here so this file is self-contained for interview review.
     *
     * RECURRENCE:
     *   lcs(s1, s2, m, n):
     *     if m == 0 || n == 0 → 0  (base: empty string has no common subsequence)
     *     if s1[m-1] == s2[n-1] → 1 + lcs(s1, s2, m-1, n-1)  (chars match → include)
     *     else → max(lcs(s1, s2, m, n-1), lcs(s1, s2, m-1, n)) (skip one char from either)
     */
    private static int lcs(String s1, String s2, int m, int n) {
        if (m == 0 || n == 0) return 0; // Base case

        if (memo[m][n] != -1) return memo[m][n]; // Already computed

        if (s1.charAt(m - 1) == s2.charAt(n - 1)) {
            // Last characters match → they're part of the LCS
            memo[m][n] = 1 + lcs(s1, s2, m - 1, n - 1);
        } else {
            // Try skipping either the last char of s1 or s2, take the better result
            memo[m][n] = Math.max(lcs(s1, s2, m, n - 1), lcs(s1, s2, m - 1, n));
        }

        return memo[m][n];
    }

    // -------------------------------------------------------------------------
    // APPROACH 2: Direct interval DP — dp[i][j] = LPS of s[i..j]
    // -------------------------------------------------------------------------

    /**
     * Bottom-up interval DP. No recursion, fills table diagonally.
     *
     * TABLE MEANING:
     *   dp[i][j] = length of LPS in the substring s[i..j] (inclusive)
     *
     * FILL ORDER:
     *   We need dp[i+1][j-1] when computing dp[i][j], so we must fill
     *   shorter substrings before longer ones.
     *   → Fill by increasing "length" (outer loop) from len=1 to len=n.
     *
     * BASE CASES:
     *   len=1: dp[i][i] = 1  (single char is always a palindrome of length 1)
     *   len=2: if s[i]==s[i+1] → dp[i][i+1] = 2, else dp[i][i+1] = 1
     *
     * TRANSITION:
     *   if s[i] == s[j]: dp[i][j] = 2 + dp[i+1][j-1]
     *   else:            dp[i][j] = max(dp[i+1][j], dp[i][j-1])
     */
    public static int longestPalindromeSubseqDP(String s) {
        int n = s.length();
        int[][] dp = new int[n][n];

        // Base case: every single character is a palindrome of length 1
        for (int i = 0; i < n; i++) {
            dp[i][i] = 1;
        }

        // Fill by increasing substring length (len = 2, 3, ..., n)
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1; // Right boundary of substring s[i..j]

                if (s.charAt(i) == s.charAt(j)) {
                    // Outer characters match → extend the inner LPS by 2
                    // For len==2, dp[i+1][j-1] = dp[j][i] which is 0 (valid because i+1 > j-1)
                    dp[i][j] = 2 + (len == 2 ? 0 : dp[i + 1][j - 1]);
                } else {
                    // Outer characters don't match → try shrinking from either end
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[0][n - 1]; // LPS of the full string
    }

    // -------------------------------------------------------------------------
    // BONUS: Reconstruct the actual palindromic subsequence
    // -------------------------------------------------------------------------

    /**
     * Returns the actual LPS string (not just its length) via backtracking
     * through the dp table filled by the direct interval DP approach.
     *
     * BACKTRACKING LOGIC (mirrors LargestCommonSubsequence.getlcs):
     *   Start at dp[0][n-1], walk towards the base using the same decisions:
     *   - If s[i] == s[j]: include both chars, move diagonally inward
     *   - Else: move in the direction that gave the larger dp value
     *   Build the left half; mirror it for the right half.
     */
    public static String reconstructLPS(String s) {
        int n = s.length();
        int[][] dp = new int[n][n];

        for (int i = 0; i < n; i++) dp[i][i] = 1;

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j)) {
                    dp[i][j] = 2 + (len == 2 ? 0 : dp[i + 1][j - 1]);
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to reconstruct the palindrome
        StringBuilder left = new StringBuilder();
        int i = 0, j = n - 1;

        while (i < j) {
            if (s.charAt(i) == s.charAt(j)) {
                left.append(s.charAt(i)); // Collect left-half character
                i++;
                j--;
            } else if (dp[i + 1][j] >= dp[i][j - 1]) {
                i++; // Move left pointer right
            } else {
                j--; // Move right pointer left
            }
        }

        // Build the full palindrome: left + middle (if odd length) + reverse(left)
        String leftStr = left.toString();
        String mid = (i == j) ? String.valueOf(s.charAt(i)) : ""; // Middle char for odd-length LPS
        return leftStr + mid + left.reverse().toString();
    }
}
