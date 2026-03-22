package com.java.ds.dp;

import java.util.*;

/**
 * CONCEPT: Dynamic Programming on Strings
 * ─────────────────────────────────────────
 * This file covers classic "two-string DP" problems where we fill a 2D table
 * dp[i][j] representing a relationship between prefixes s[0..i-1] and t[0..j-1].
 *
 * COMMON PATTERN:
 *   1. Define dp[i][j] precisely in terms of prefixes.
 *   2. Handle base cases (empty strings): usually dp[0][j] and dp[i][0].
 *   3. Fill row by row (i from 1 to m, j from 1 to n).
 *   4. Answer is typically dp[m][n].
 *
 * PROBLEMS COVERED:
 *   - LC 44  Wildcard Matching          — * matches any sequence
 *   - LC 10  Regular Expression Matching — .* with character class
 *   - LC 115 Distinct Subsequences      — count ways to form t from s
 *   - LC 97  Interleaving String         — s3 formed by interleaving s1 and s2
 *   - LC 1092 Shortest Common Supersequence — shortest string containing both
 *
 * WHEN TO USE:
 *   When the problem involves two strings and asks for matching, counting,
 *   or combining — and subproblems have "how far into each string" as state.
 *
 * TIME/SPACE COMPLEXITY: O(m × n) time and space for most problems here.
 *   Space can often be reduced to O(n) by using only one row at a time.
 *
 * INTERVIEW ANGLE:
 *   Start by defining dp[i][j] out loud. Examiners care deeply about the definition.
 *   Then derive the recurrence from the definition — don't just memorize the formula.
 *
 * REAL-WORLD USE:
 *   - Wildcard / regex matching in search engines, file systems (glob patterns)
 *   - DNA sequence alignment (Distinct Subsequences, LCS variants)
 *   - Text diff tools (LCS / SCS)
 *   - Compiler lexical analysis (regex matching)
 */
public class DPOnStrings {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: LC 44 — WILDCARD MATCHING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 44 — Wildcard Matching
     * Pattern characters: '?' matches exactly one char, '*' matches any sequence (including empty).
     *
     * KEY INSIGHT — dp[i][j] definition:
     *   dp[i][j] = true if s[0..i-1] is fully matched by p[0..j-1]
     *
     * BASE CASES:
     *   dp[0][0] = true   — empty string matches empty pattern
     *   dp[i][0] = false  — non-empty string cannot match empty pattern
     *   dp[0][j] = true   — empty string matches pattern only if all of p[0..j-1] are '*'
     *
     * RECURRENCE:
     *   If p[j-1] == '?': dp[i][j] = dp[i-1][j-1]  (match exactly one char)
     *   If p[j-1] == '*': dp[i][j] = dp[i][j-1]    (use '*' for zero chars: ignore '*')
     *                               OR dp[i-1][j]   (use '*' for one+ chars: '*' absorbs s[i-1])
     *   Otherwise:        dp[i][j] = dp[i-1][j-1] && s[i-1] == p[j-1]
     *
     * INTERVIEW Q: Why dp[i-1][j] for "one+ chars" case of '*'?
     * A: dp[i-1][j] = "s[0..i-2] is matched by p[0..j-1]" — i.e., the same '*' is still
     *    "active" and just consumed one more character of s. This is the key.
     *
     * GOTCHA: dp[i][j-1] means '*' matches zero characters (the '*' is effectively deleted).
     *         dp[i-1][j] means '*' has already consumed at least one character (it stays).
     *
     * @param s input string
     * @param p wildcard pattern ('?' and '*')
     * @return true if s matches p
     */
    public static boolean isMatchWildcard(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true; // empty matches empty

        // dp[0][j]: empty string matches p[0..j-1] only if all chars are '*'
        for (int j = 1; j <= n; j++) {
            dp[0][j] = dp[0][j - 1] && p.charAt(j - 1) == '*';
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    // '*' matches zero chars (dp[i][j-1]) OR one+ chars (dp[i-1][j])
                    dp[i][j] = dp[i][j - 1] || dp[i - 1][j];
                } else if (pc == '?' || pc == s.charAt(i - 1)) {
                    // '?' matches any single char; literal must match exactly
                    dp[i][j] = dp[i - 1][j - 1];
                }
                // else dp[i][j] remains false (mismatch)
            }
        }
        return dp[m][n];
    }
    /*
     * COMPLEXITY — Wildcard Matching
     * ────────────────────────────────
     * TIME:  O(m × n) — fill (m+1)×(n+1) table
     * SPACE: O(m × n) — reducible to O(n) with rolling array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: LC 10 — REGULAR EXPRESSION MATCHING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 10 — Regular Expression Matching
     * Pattern characters: '.' matches any single char, '*' matches zero or more of the preceding char.
     *
     * KEY INSIGHT — CRITICAL DIFFERENCE FROM WILDCARD:
     *   In regex, '*' is NOT standalone — it pairs with the PRECEDING character.
     *   'a*' means "zero or more 'a'" — you can't have '*' at position 0.
     *   This makes the '*' case more complex than in wildcard matching.
     *
     * dp[i][j] = true if s[0..i-1] matches p[0..j-1]
     *
     * BASE CASES:
     *   dp[0][0] = true
     *   dp[0][j]: empty string can match patterns like "a*", "a*b*", ".*" (zero repetitions)
     *             dp[0][j] = dp[0][j-2] if p[j-1] == '*'  (remove the "x*" pair)
     *
     * RECURRENCE:
     *   If p[j-1] == '*':
     *     ZERO occurrences: dp[i][j] = dp[i][j-2]   (skip the "x*" pair entirely)
     *     ONE+ occurrences: dp[i][j] |= dp[i-1][j]  IF p[j-2] matches s[i-1]
     *                       (p[j-2] matches s[i-1] means p[j-2]=='.' OR p[j-2]==s[i-1])
     *   Else:
     *     dp[i][j] = dp[i-1][j-1] && (p[j-1]=='.' || p[j-1]==s[i-1])
     *
     * INTERVIEW Q: What does dp[i-1][j] represent for the '*' case?
     * A: s[0..i-2] matched p[0..j-1] (the "x*" pair still active), now we consume
     *    s[i-1] as another occurrence of x. The pattern position stays at j.
     *
     * GOTCHA: The "one+" occurrence check: we check p[j-2] (the char BEFORE '*'),
     * not p[j-1] (which is '*'). j must be >= 2 for a valid '*' pattern.
     *
     * @param s input string (only lowercase letters)
     * @param p regex pattern (lowercase letters, '.', '*')
     * @return true if s matches p
     */
    public static boolean isMatchRegex(String s, String p) {
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        // Handle patterns like "a*", "a*b*", ".*" matching empty string
        for (int j = 2; j <= n; j++) {
            // "x*" pattern can match empty — propagate dp[0][j-2] forward
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char pc = p.charAt(j - 1);
                if (pc == '*') {
                    // j >= 2 always holds for valid regex (no leading '*')
                    // Zero occurrences of preceding char: remove "x*" pair
                    dp[i][j] = dp[i][j - 2];
                    // One+ occurrences: preceding char matches s[i-1]
                    boolean prevCharMatches = (p.charAt(j - 2) == '.' || p.charAt(j - 2) == s.charAt(i - 1));
                    if (prevCharMatches) {
                        dp[i][j] = dp[i][j] || dp[i - 1][j]; // consume one more char of s
                    }
                } else {
                    // Direct match: '.' matches anything, else literal equality
                    boolean charMatches = (pc == '.' || pc == s.charAt(i - 1));
                    dp[i][j] = charMatches && dp[i - 1][j - 1];
                }
            }
        }
        return dp[m][n];
    }
    /*
     * COMPLEXITY — Regex Matching
     * ────────────────────────────
     * TIME:  O(m × n)
     * SPACE: O(m × n) — reducible to O(n) with two rows
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 115 — DISTINCT SUBSEQUENCES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 115 — Distinct Subsequences
     * Count the number of ways t appears as a subsequence of s.
     *
     * KEY INSIGHT:
     *   dp[i][j] = number of ways to form t[0..j-1] from s[0..i-1] as a subsequence.
     *
     * BASE CASES:
     *   dp[i][0] = 1  — empty t can be formed from any prefix of s in exactly 1 way (take nothing)
     *   dp[0][j] = 0  — non-empty t cannot be formed from empty s
     *
     * RECURRENCE:
     *   Always: dp[i][j] += dp[i-1][j]  — skip s[i-1] (don't use it for t[j-1])
     *   If s[i-1] == t[j-1]: dp[i][j] += dp[i-1][j-1]  — use s[i-1] to match t[j-1]
     *
     * INTERPRETATION:
     *   "Skip s[i-1]": the number of ways to form t[0..j-1] from s[0..i-2] (same t, shorter s)
     *   "Use s[i-1]": the number of ways to form t[0..j-2] from s[0..i-2] (matched both chars)
     *
     * INTERVIEW Q: Why is dp[i][j] potentially very large?
     * A: The number of distinct subsequences can be exponential. LeetCode says return
     *    it as-is, but for large inputs you'd need BigInteger or modular arithmetic.
     *
     * @param s source string
     * @param t target subsequence string
     * @return number of distinct subsequences of s equal to t
     */
    public static int numDistinct(String s, String t) {
        int m = s.length(), n = t.length();
        // dp[i][j] = ways to form t[0..j-1] from s[0..i-1]
        long[][] dp = new long[m + 1][n + 1]; // use long to avoid overflow

        // Base case: empty t matches any prefix in exactly 1 way
        for (int i = 0; i <= m; i++) dp[i][0] = 1;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                dp[i][j] = dp[i - 1][j]; // always: skip s[i-1]
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    dp[i][j] += dp[i - 1][j - 1]; // use s[i-1] to match t[j-1]
                }
            }
        }
        return (int) dp[m][n];
    }
    /*
     * COMPLEXITY — Distinct Subsequences
     * ─────────────────────────────────────
     * TIME:  O(m × n)
     * SPACE: O(m × n) — reducible to O(n) (only use previous row)
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 97 — INTERLEAVING STRING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 97 — Interleaving String
     * Check if s3 can be formed by interleaving s1 and s2 (preserving relative order in each).
     *
     * KEY INSIGHT:
     *   dp[i][j] = true if s3[0..i+j-1] can be formed by interleaving s1[0..i-1] and s2[0..j-1]
     *   At each position in s3, the last character came from either s1 or s2.
     *
     * BASE CASE: dp[0][0] = true (empty strings form empty s3)
     *
     * RECURRENCE:
     *   s3[i+j-1] came from s1: dp[i][j] = dp[i-1][j] && s1[i-1] == s3[i+j-1]
     *   s3[i+j-1] came from s2: dp[i][j] |= dp[i][j-1] && s2[j-1] == s3[i+j-1]
     *
     * GOTCHA: First check s3.length() == s1.length() + s2.length() — if not, return false.
     *
     * INTERVIEW Q: Could we solve this with recursion + memoization?
     * A: Yes, dp[i][j] = memoize(i, j). Both approaches are equivalent in complexity.
     *    Bottom-up DP avoids recursion stack overhead.
     *
     * @param s1 first string
     * @param s2 second string
     * @param s3 target interleaved string
     * @return true if s3 is a valid interleaving of s1 and s2
     */
    public static boolean isInterleave(String s1, String s2, String s3) {
        int m = s1.length(), n = s2.length();
        // GOTCHA: early exit if lengths don't add up
        if (m + n != s3.length()) return false;

        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        // First row: s1 only (j=0), s3 characters must match s1
        for (int i = 1; i <= m; i++) {
            dp[i][0] = dp[i - 1][0] && s1.charAt(i - 1) == s3.charAt(i - 1);
        }
        // First column: s2 only (i=0), s3 characters must match s2
        for (int j = 1; j <= n; j++) {
            dp[0][j] = dp[0][j - 1] && s2.charAt(j - 1) == s3.charAt(j - 1);
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char s3char = s3.charAt(i + j - 1); // current character in s3
                // Last char came from s1[i-1] OR s2[j-1]
                dp[i][j] = (dp[i - 1][j] && s1.charAt(i - 1) == s3char)
                         || (dp[i][j - 1] && s2.charAt(j - 1) == s3char);
            }
        }
        return dp[m][n];
    }
    /*
     * COMPLEXITY — Interleaving String
     * ──────────────────────────────────
     * TIME:  O(m × n)
     * SPACE: O(m × n) — reducible to O(n) with one-row rolling array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: LC 1092 — SHORTEST COMMON SUPERSEQUENCE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 1092 — Shortest Common Supersequence
     * Find the shortest string that has both s1 and s2 as subsequences.
     *
     * KEY INSIGHT — Connection to LCS:
     *   SCS length = s1.length() + s2.length() - LCS(s1, s2)
     *   Explanation: in SCS, the LCS characters are shared (not duplicated).
     *   Everything not in LCS from s1 must appear, and same for s2.
     *
     * RECONSTRUCTION (tracing the dp table):
     *   Start at dp[m][n], trace backwards:
     *   - If s1[i-1] == s2[j-1]: this char is in LCS → include once, go to dp[i-1][j-1]
     *   - Else if dp[i-1][j] >= dp[i][j-1]: came from s1, include s1[i-1], go to dp[i-1][j]
     *   - Else: came from s2, include s2[j-1], go to dp[i][j-1]
     *   Append remaining chars of whichever string has elements left.
     *
     * STEP 1: Compute LCS dp table (same as classic LCS DP).
     * STEP 2: Reconstruct SCS by tracing.
     *
     * INTERVIEW Q: Why subtract LCS length for SCS length?
     * A: In the optimal supersequence, LCS characters appear once (shared by both).
     *    Non-LCS characters from s1 and s2 each appear separately.
     *    Total = (s1 chars not in LCS) + LCS chars + (s2 chars not in LCS)
     *          = (m - LCS) + LCS + (n - LCS) = m + n - LCS.
     *
     * @param s1 first string
     * @param s2 second string
     * @return shortest common supersequence
     */
    public static String shortestCommonSupersequence(String s1, String s2) {
        int m = s1.length(), n = s2.length();

        // Step 1: Build LCS DP table
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1; // extend LCS
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // take best from either
                }
            }
        }

        // Step 2: Reconstruct SCS by tracing dp table backwards
        StringBuilder sb = new StringBuilder();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                // Characters match (part of LCS) — include once in SCS
                sb.append(s1.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                // LCS came from going up (s1's character is not in LCS here)
                sb.append(s1.charAt(i - 1));
                i--;
            } else {
                // LCS came from going left (s2's character is not in LCS here)
                sb.append(s2.charAt(j - 1));
                j--;
            }
        }
        // Append remaining characters from whichever string still has chars
        while (i > 0) { sb.append(s1.charAt(i - 1)); i--; }
        while (j > 0) { sb.append(s2.charAt(j - 1)); j--; }

        return sb.reverse().toString(); // we built it backwards
    }

    /** Helper: compute just the LCS length (not reconstruction). */
    public static int lcsLength(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = (s1.charAt(i - 1) == s2.charAt(j - 1))
                    ? dp[i - 1][j - 1] + 1
                    : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[m][n];
    }
    /*
     * COMPLEXITY — Shortest Common Supersequence
     * ───────────────────────────────────────────
     * TIME:  O(m × n) — LCS table + reconstruction (O(m+n) after table)
     * SPACE: O(m × n) — LCS table (needed for reconstruction; cannot reduce to O(n) easily)
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== LC 44 Wildcard Matching ===");
        System.out.println(isMatchWildcard("aa", "a"));       // false
        System.out.println(isMatchWildcard("aa", "*"));       // true
        System.out.println(isMatchWildcard("cb", "?a"));      // false
        System.out.println(isMatchWildcard("adceb", "*a*b")); // true
        System.out.println(isMatchWildcard("acdcb", "a*c?b")); // false

        System.out.println("\n=== LC 10 Regex Matching ===");
        System.out.println(isMatchRegex("aa", "a"));    // false
        System.out.println(isMatchRegex("aa", "a*"));   // true
        System.out.println(isMatchRegex("ab", ".*"));   // true
        System.out.println(isMatchRegex("aab", "c*a*b")); // true
        System.out.println(isMatchRegex("mississippi", "mis*is*p*.")); // false

        System.out.println("\n=== LC 115 Distinct Subsequences ===");
        System.out.println(numDistinct("rabbbit", "rabbit")); // 3
        System.out.println(numDistinct("babgbag", "bag"));    // 5

        System.out.println("\n=== LC 97 Interleaving String ===");
        System.out.println(isInterleave("aabcc", "dbbca", "aadbbcbcac")); // true
        System.out.println(isInterleave("aabcc", "dbbca", "aadbbbaccc")); // false
        System.out.println(isInterleave("", "", ""));                     // true

        System.out.println("\n=== LC 1092 Shortest Common Supersequence ===");
        System.out.println(shortestCommonSupersequence("abac", "cab")); // "cabac" (length 5)
        System.out.println(shortestCommonSupersequence("str", "pqr"));  // "pqstr" or similar

        System.out.println("\nLCS lengths:");
        System.out.println(lcsLength("abcde", "ace")); // 3
        System.out.println(lcsLength("abc", "abc"));   // 3
        System.out.println(lcsLength("abc", "def"));   // 0
    }
}
