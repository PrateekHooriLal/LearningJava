package com.java.ds.stringalgo;

import java.util.*;

/**
 * CONCEPT: Manacher's Algorithm — O(n) Longest Palindromic Substring
 * ────────────────────────────────────────────────────────────────────
 * Manacher's algorithm finds the palindrome radius at every center in a string
 * in O(n) time by reusing previously computed palindrome information.
 *
 * CORE IDEA — Transformation:
 *   Insert '#' between every character (and at both ends):
 *   "abc" → "#a#b#c#"
 *   "abba" → "#a#b#b#a#"
 *   This converts ALL palindromes to ODD-length palindromes in the transformed string.
 *   Every original palindrome (odd or even length) becomes an odd-length palindrome in T.
 *
 * WHY TRANSFORM?
 *   Original: "abba" — even-length palindrome, no single center character.
 *   Transformed: "#a#b#b#a#" — center '#' at position 4, now has a well-defined center.
 *   This unifies odd and even palindrome handling into one case.
 *
 * P[] ARRAY:
 *   p[i] = radius of palindrome centered at T[i] in the TRANSFORMED string.
 *   The palindrome in T is T[i-p[i]..i+p[i]].
 *
 * MAPPING BACK TO ORIGINAL:
 *   Palindrome in original starts at: (i - p[i]) / 2
 *   Palindrome length in original: p[i]  (NOT p[i]*2+1 — because T includes '#' chars)
 *
 * BUILDING P[] — using center c and right boundary r:
 *   Maintain the rightmost palindrome: center c, right boundary r.
 *   For each i:
 *     Mirror position: 2*c - i (symmetric to i around c)
 *     If i < r: p[i] = min(r - i, p[2*c - i])  (use mirror, bounded by r)
 *     Expand: while T[i-p[i]-1] == T[i+p[i]+1]: p[i]++
 *     Update c,r if i+p[i] > r
 *
 * TIME/SPACE COMPLEXITY: O(n) time, O(n) space.
 *
 * INTERVIEW ANGLE:
 *   "Manacher's is DP on palindromes. We reuse palindrome info via the mirror property:
 *    a palindrome centered at c has a mirror palindrome at the symmetric position."
 *   Contrast with expand-from-center: O(n^2) time, O(1) space.
 *   Manacher's trades space for an optimal O(n) time.
 *
 * WHEN TO USE:
 *   - Any problem requiring palindromic substring properties for all centers
 *   - LC 5 (longest palindromic substring), LC 647 (count palindromes)
 *   - Computing palindrome radii at all centers efficiently
 *
 * REAL-WORLD USE:
 *   - DNA sequence analysis (palindromic restriction sites)
 *   - Text compression (finding repeated palindromic patterns)
 *   - Bioinformatics (inverted repeat detection)
 */
public class ManacherAlgorithm {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: STRING TRANSFORMATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Transform string by inserting '#' between every character.
     *
     * KEY INSIGHT:
     *   "abc"  → "#a#b#c#"   (length 2*n+1)
     *   "abba" → "#a#b#b#a#" (length 2*n+1)
     *   '#' characters act as "virtual centers" for even-length palindromes.
     *
     * EXAMPLE PALINDROME MAPPING:
     *   Original "aba" (odd, center at 'b', index 1):
     *   Transformed "#a#b#a#" → center at 'b' (index 3), p[3] = 3
     *   Original palindrome length = p[3] = 3 ✓
     *
     *   Original "abba" (even, center between 'b','b'):
     *   Transformed "#a#b#b#a#" → center at '#' (index 4), p[4] = 4
     *   Original palindrome length = p[4] = 4 ✓
     *
     * @param s original string
     * @return transformed string with '#' separators
     */
    private static String transform(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('#');
        for (char c : s.toCharArray()) {
            sb.append(c);
            sb.append('#');
        }
        return sb.toString();
        // Length = 2 * s.length() + 1
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: MANACHER'S ALGORITHM — BUILD P[] ARRAY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build the palindrome radius array p[] using Manacher's algorithm.
     *
     * KEY INSIGHT — Mirror property optimization:
     *   If i < r (inside current rightmost palindrome centered at c):
     *     p[i] can be initialized using p[mirror] where mirror = 2*c - i.
     *     BUT: cap at r - i (we only KNOW the match up to r).
     *     If p[mirror] < r - i: p[i] = p[mirror] (fully inside, no need to extend)
     *     If p[mirror] >= r - i: p[i] = r - i, then try to extend beyond r
     *
     * AMORTIZED O(n):
     *   r only increases (never decreases).
     *   Each character causes r to increase at most once during expansion.
     *   Total expansion work: O(n).
     *
     * GOTCHA: When i >= r, we start from p[i] = 0 (or initialize then expand).
     *         When i < r, we start from min(p[2*c-i], r-i) then attempt extension.
     *
     * @param s original string (will be transformed internally)
     * @return p[] palindrome radius array for the transformed string T
     */
    public static int[] manacher(String s) {
        String T = transform(s); // "#a#b#c#..." format
        int n = T.length();
        int[] p = new int[n]; // p[i] = palindrome radius at T[i]

        int c = 0; // center of rightmost palindrome seen so far
        int r = 0; // right boundary (inclusive) of that palindrome: T[c-p[c]..c+p[c]]

        for (int i = 0; i < n; i++) {
            // Mirror of i with respect to center c
            int mirror = 2 * c - i;

            if (i < r) {
                // i is inside the rightmost palindrome — use mirror to initialize p[i]
                p[i] = Math.min(r - i, p[mirror]);
                // r - i: how far i is from the right boundary (we can guarantee up to here)
                // p[mirror]: the radius at the mirror position
                // Take the minimum: can't extend beyond r without verification
            }
            // p[i] is either 0 (i >= r) or the mirror value capped at r-i (i < r)

            // Try to expand palindrome centered at i beyond the current known bounds
            // Start expanding from p[i]+1 to the left and right simultaneously
            while (i + p[i] + 1 < n && i - p[i] - 1 >= 0
                   && T.charAt(i + p[i] + 1) == T.charAt(i - p[i] - 1)) {
                p[i]++; // expand palindrome by one character on each side
            }

            // Update rightmost palindrome if current extends beyond r
            if (i + p[i] > r) {
                c = i;          // new rightmost center
                r = i + p[i];   // new right boundary
            }
        }
        return p;
    }
    /*
     * COMPLEXITY — Manacher's
     * ────────────────────────
     * TIME:  O(n) — r only increases monotonically; each extension causes r to grow by at least 1
     * SPACE: O(n) — transformed string T + p[] array
     *
     * vs Expand-from-Center: O(n^2) time, O(1) extra space
     * Manacher trades O(n) space for O(n) time improvement.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 5 — LONGEST PALINDROMIC SUBSTRING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 5 — Longest Palindromic Substring
     *
     * KEY INSIGHT:
     *   After running Manacher's, find the index i with maximum p[i].
     *   Map back to original string:
     *     - Center in original: (i - 1) / 2  (subtract 1 for the leading '#', divide by 2)
     *     - Length in original: p[i]
     *     - Start in original: (i - p[i]) / 2
     *
     * MAPPING FORMULA:
     *   T = "#a#b#c#..." has position i. In original string:
     *   Original index = (T_index - 1) / 2  (for non-'#' characters)
     *   Start of palindrome in original = (i - p[i]) / 2
     *   This formula works because every original char at index k appears at T[2k+1].
     *
     * EXPAND-FROM-CENTER (O(n^2)) vs Manacher's (O(n)):
     *   For n=10^5: O(n^2) = 10^10 operations (too slow), O(n) = 10^5 (fast).
     *   LeetCode's n is typically ≤ 1000, where O(n^2) is acceptable, but Manacher's is better.
     *
     * INTERVIEW Q: What's the expand-from-center approach?
     * A: For each center (n centers for odd, n-1 centers for even), expand outward
     *    while characters match. O(n) centers × O(n) expansion = O(n^2) total.
     *    Manacher's avoids redundant expansion using previously computed palindrome info.
     *
     * @param s input string
     * @return longest palindromic substring
     */
    public static String longestPalindrome(String s) {
        if (s.isEmpty()) return s;

        int[] p = manacher(s);
        String T = transform(s);

        // Find index with maximum palindrome radius
        int maxLen = 0, centerIdx = 0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] > maxLen) {
                maxLen = p[i];
                centerIdx = i;
            }
        }

        // Map back to original string indices
        // Start in original = (centerIdx - maxLen) / 2
        int start = (centerIdx - maxLen) / 2;
        return s.substring(start, start + maxLen);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 647 — PALINDROMIC SUBSTRINGS COUNT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 647 — Palindromic Substrings
     *
     * PROBLEM: Count all palindromic substrings in s (including single characters).
     *
     * KEY INSIGHT — Counting via p[] array:
     *   For each center i in T with radius p[i]:
     *   Number of palindromes centered at i in ORIGINAL string = (p[i] + 1) / 2
     *
     * WHY (p[i] + 1) / 2?
     *   p[i] = radius in transformed string T.
     *   The palindromes at center i in T of radii 1, 2, ..., p[i] correspond to:
     *   - Radius 1 (odd): single char palindrome in original = 1 palindrome
     *   - Radius 2 (even center '#'): even-length palindrome of length 2 in original = 1 palindrome
     *   ... Each step of 2 in radius corresponds to 1 more palindrome in original.
     *   So (p[i] + 1) / 2 palindromes per center (integer division handles odd/even).
     *
     * ALTERNATIVE FORMULA:
     *   For each center: count = (p[i] / 2) + 1 if p[i] is even; (p[i] + 1) / 2 if odd.
     *   Simplified: (p[i] + 1) / 2 works for all cases using integer division.
     *
     * INTERVIEW Q: Why does a single character count as a palindromic substring?
     * A: By definition, a single character is a palindrome. Every n-character string
     *    has at least n palindromic substrings (the n single characters).
     *
     * @param s input string
     * @return count of palindromic substrings
     */
    public static int countSubstrings(String s) {
        int[] p = manacher(s);
        int count = 0;
        for (int pi : p) {
            count += (pi + 1) / 2; // each center contributes (pi+1)/2 palindromes
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: LC 1312 — MIN INSERTIONS TO MAKE STRING A PALINDROME
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 1312 — Minimum Insertions to Make a String a Palindrome
     *
     * KEY INSIGHT — LPS Connection:
     *   Minimum insertions = n - LPS(s)
     *   Where LPS = Longest Palindromic Subsequence length.
     *
     * WHY?
     *   To make s a palindrome, we must "fill in" the characters not in the LPS.
     *   Each character NOT in the LPS needs to have its mirror added.
     *   Characters in LPS are already "paired" — no insertion needed.
     *   Characters NOT in LPS: (n - LPS) characters, each needs 1 insertion → (n - LPS) total.
     *
     * COMPUTING LPS:
     *   Method 1: LPS(s) = LCS(s, reverse(s)) — use standard DP, O(n^2)
     *   Method 2: Use Manacher's result, but Manacher finds subSTRING not subSEQUENCE.
     *   NOTE: Manacher's gives Longest Palindromic SUBSTRING, not SUBSEQUENCE.
     *         For LPS, we still need the DP approach.
     *
     * INTERVIEW GOTCHA: Distinguish between:
     *   - Longest Palindromic SUBSTRING (Manacher's, O(n)): contiguous characters
     *   - Longest Palindromic SUBSEQUENCE (DP with LCS, O(n^2)): can skip characters
     *
     * @param s input string
     * @return minimum insertions to make s a palindrome
     */
    public static int minInsertions(String s) {
        int n = s.length();
        // LPS = LCS(s, reverse(s))
        String rev = new StringBuilder(s).reverse().toString();
        // Standard LCS DP
        int[][] dp = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (s.charAt(i - 1) == rev.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        int lps = dp[n][n];
        return n - lps; // insertions needed = chars not in LPS
    }
    /*
     * COMPLEXITY — Min Insertions
     * ────────────────────────────
     * TIME:  O(n^2) — LCS DP
     * SPACE: O(n^2) — DP table (reducible to O(n) with rolling array)
     *
     * Note: Manacher's would give O(n) for the Longest Palindromic SUBSTRING.
     *       For SUBSEQUENCE, O(n^2) DP is the best known polynomial approach.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 6: EXPAND-FROM-CENTER (O(n^2)) — For Comparison
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Expand-from-Center — O(n^2) palindrome finding.
     *
     * KEY INSIGHT: There are 2n-1 possible centers (n for odd, n-1 for even length palindromes).
     * For each center, expand outward while characters match.
     * Simpler to code but O(n^2) — use when n is small or code simplicity matters.
     *
     * INTERVIEW Q: When would you use expand-from-center over Manacher's?
     * A: When n ≤ 1000 (O(n^2) is fast enough) OR in an interview where you want
     *    to show the intuition clearly before jumping to the optimal solution.
     *
     * @param s input string
     * @return longest palindromic substring (O(n^2) approach)
     */
    public static String longestPalindromeSimple(String s) {
        int n = s.length();
        int bestStart = 0, bestLen = 1;

        for (int center = 0; center < n; center++) {
            // Odd-length palindromes (single center character)
            int lo = center, hi = center;
            while (lo >= 0 && hi < n && s.charAt(lo) == s.charAt(hi)) {
                if (hi - lo + 1 > bestLen) {
                    bestLen = hi - lo + 1;
                    bestStart = lo;
                }
                lo--;
                hi++;
            }

            // Even-length palindromes (center between characters)
            lo = center;
            hi = center + 1;
            while (lo >= 0 && hi < n && s.charAt(lo) == s.charAt(hi)) {
                if (hi - lo + 1 > bestLen) {
                    bestLen = hi - lo + 1;
                    bestStart = lo;
                }
                lo--;
                hi++;
            }
        }
        return s.substring(bestStart, bestStart + bestLen);
    }
    /*
     * COMPLEXITY — Expand-from-Center
     * ─────────────────────────────────
     * TIME:  O(n^2) — 2n-1 centers × O(n) expansion each
     * SPACE: O(1) extra (just index variables)
     *
     * Manacher's vs Expand-from-Center:
     *   Manacher's: O(n) time, O(n) space
     *   Expand: O(n^2) time, O(1) space
     *   For n ≤ 1000: both acceptable. For n = 10^5+: Manacher's is needed.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Manacher P[] Array Tests ===");
        System.out.println(Arrays.toString(manacher("aabaa")));
        // T = "#a#a#b#a#a#" (length 11)
        // Expected p: [0,1,0,3,0,5,0,3,0,1,0] (center at 'b' has radius 5)
        System.out.println(Arrays.toString(manacher("abba")));
        // T = "#a#b#b#a#" (length 9)
        System.out.println(Arrays.toString(manacher("racecar")));

        System.out.println("\n=== LC 5 Longest Palindromic Substring ===");
        System.out.println(longestPalindrome("babad"));    // "bab" or "aba" (both valid)
        System.out.println(longestPalindrome("cbbd"));     // "bb"
        System.out.println(longestPalindrome("a"));        // "a"
        System.out.println(longestPalindrome("racecar"));  // "racecar"
        System.out.println(longestPalindrome("abacaba")); // "abacaba"

        System.out.println("\n=== LC 5 via Expand-from-Center (O(n^2)) ===");
        System.out.println(longestPalindromeSimple("babad"));   // "bab" or "aba"
        System.out.println(longestPalindromeSimple("cbbd"));    // "bb"
        System.out.println(longestPalindromeSimple("racecar")); // "racecar"

        System.out.println("\n=== LC 647 Count Palindromic Substrings ===");
        System.out.println(countSubstrings("abc"));   // 3 (a, b, c — all singles)
        System.out.println(countSubstrings("aaa"));   // 6 (a,a,a,aa,aa,aaa)
        System.out.println(countSubstrings("abba"));  // 6 (a,b,b,a,bb,abba)
        System.out.println(countSubstrings("racecar")); // count all palindromes

        System.out.println("\n=== LC 1312 Min Insertions for Palindrome ===");
        System.out.println(minInsertions("zzazz"));  // 0 (already a palindrome)
        System.out.println(minInsertions("mbadm"));  // 2 (mbadbm or mdbadm)
        System.out.println(minInsertions("leetcode")); // 5
        System.out.println(minInsertions("ab"));     // 1 (aba or bab)

        System.out.println("\n=== Transform Examples ===");
        System.out.println(transform("abc"));   // #a#b#c#
        System.out.println(transform("abba"));  // #a#b#b#a#
        System.out.println(transform(""));      // #
    }
}
