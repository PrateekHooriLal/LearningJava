package com.java.ds.stringalgo;

import java.util.*;

/**
 * CONCEPT: Z-Algorithm for String Matching
 * ─────────────────────────────────────────
 * The Z-array for a string s is defined as:
 *   z[i] = length of the longest substring starting at s[i] that is also a prefix of s
 *
 * EXAMPLES:
 *   s = "aabxaa"
 *   z = [6, 1, 0, 0, 2, 1]
 *   z[0] = 6 (the whole string is a prefix of itself — by convention, z[0] = len or undefined)
 *   z[4] = 2 (s[4..5] = "aa" matches prefix s[0..1] = "aa")
 *
 *   s = "aaaaaa"
 *   z = [6, 5, 4, 3, 2, 1]
 *
 *   s = "abcabc"
 *   z = [6, 0, 0, 3, 0, 0]
 *   z[3] = 3 because s[3..5] = "abc" = s[0..2]
 *
 * BUILDING THE Z-ARRAY (O(n)) — using a sliding window [l, r]:
 *   Maintain [l, r]: the rightmost Z-box (a substring matching a prefix).
 *   For each i:
 *     If i <= r (i is inside current Z-box):
 *       Use z[i - l] as a starting point (mirror position), but cap at r - i + 1
 *     Extend naively from the known starting point
 *     Update [l, r] if we extended beyond r
 *
 * PATTERN MATCHING via Z-ALGORITHM:
 *   Build combined = pattern + "$" + text
 *   Compute Z-array of combined
 *   Any position i (in the text portion) where z[i] == pattern.length() → match found
 *   The "$" separator ensures no match can span from pattern into text.
 *
 * WHEN TO USE vs KMP:
 *   Both solve the same problems in O(n+m). Z-algorithm is often considered easier to
 *   code (no tricky failure function logic). KMP is more widely known in interviews.
 *   Z-algorithm is preferred for problems where "prefix matches at position i" is the
 *   natural formulation.
 *
 * TIME/SPACE COMPLEXITY: O(n) build, O(n+m) for pattern matching.
 *
 * INTERVIEW ANGLE:
 *   Explain Z-algorithm as "every position gets a 'free ride' using a previously computed
 *   Z-box, only extending when we go beyond the known boundary."
 *   The amortized analysis: r only increases → each char extended at most once → O(n).
 *
 * REAL-WORLD USE:
 *   - Same applications as KMP (text search, pattern matching)
 *   - Minimum period detection
 *   - Competitive programming (often preferred over KMP for its clarity)
 */
public class ZAlgorithm {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: BUILD Z-ARRAY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build the Z-array for string s in O(n) time.
     *
     * KEY INSIGHT — Z-box [l, r] optimization:
     *   We maintain the rightmost Z-box: s[l..r] == s[0..r-l].
     *   For position i inside [l, r]:
     *     Mirror position: i - l (the corresponding position in the matched prefix)
     *     z[i] ≥ min(z[i-l], r-i+1):
     *       - If z[i-l] < r-i+1: z[i] = z[i-l] (fits entirely inside Z-box, no extension needed)
     *       - If z[i-l] >= r-i+1: z[i] starts at r-i+1, needs extension (we know s[i..r] matches)
     *
     * AMORTIZED O(n) ARGUMENT:
     *   r only increases (never decreases). Each character causes r to increase at most once
     *   in the extension step. Total extensions ≤ n → O(n) total work.
     *
     * CONVENTION: z[0] is often set to 0 (or the full string length).
     * We set z[0] = 0 here to avoid special cases (it's never used in pattern matching).
     *
     * @param s input string
     * @return z[] array where z[i] = longest prefix of s that matches s[i..]
     */
    public static int[] buildZArray(String s) {
        int n = s.length();
        int[] z = new int[n];
        z[0] = 0; // convention: z[0] = 0 (or n, but 0 avoids special casing)

        int l = 0, r = 0; // boundaries of the rightmost Z-box
        // [l, r] means s[l..r] == s[0..r-l] (a Z-box that extends to position r)

        for (int i = 1; i < n; i++) {
            if (i <= r) {
                // i is inside the current Z-box — use mirror to initialize z[i]
                int mirror = i - l; // mirror position in the prefix
                // Cap at r-i+1: we're certain up to r, need to extend beyond
                z[i] = Math.min(z[mirror], r - i + 1);
            }
            // Try to extend z[i] naively beyond what we know
            // Starting point: i + z[i] (already matched z[i] chars)
            while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) {
                z[i]++;
            }
            // Update Z-box if we extended beyond r
            if (i + z[i] - 1 > r) {
                l = i;
                r = i + z[i] - 1; // new rightmost boundary
            }
        }
        return z;
    }
    /*
     * COMPLEXITY — Build Z-Array
     * ───────────────────────────
     * TIME:  O(n) amortized
     *   - r increases monotonically from 0 to at most n-1 (total increase ≤ n)
     *   - Each character is "extended" at most once (when it causes r to increase)
     *   - Total extensions across all positions: O(n)
     * SPACE: O(n) — the z array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: PATTERN MATCHING USING Z-ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Pattern matching using Z-algorithm.
     *
     * KEY INSIGHT — Concatenation trick:
     *   combined = pattern + "$" + text  (separator "$" must NOT appear in pattern or text)
     *   z[i] == pattern.length() at some position i in the text portion → match!
     *   The match starts at text[i - pattern.length() - 1] (subtract pattern length and separator).
     *
     * SEPARATOR CHOICE:
     *   Use a character that doesn't appear in pattern or text.
     *   "$" works for lowercase letter problems. Adjust for other character sets.
     *
     * @param text    text to search in
     * @param pattern pattern to find
     * @return list of starting indices (in text) where pattern occurs
     */
    public static List<Integer> zSearch(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        int m = pattern.length(), n = text.length();
        if (m == 0 || m > n) return matches;

        // Build combined string with separator
        String combined = pattern + "$" + text;
        int[] z = buildZArray(combined);

        // Pattern found at positions where z[i] == m (in the text portion)
        // Text starts at index m + 1 in combined
        for (int i = m + 1; i < combined.length(); i++) {
            if (z[i] == m) {
                // i is in the combined string; text start is at i - (m + 1) in original text
                matches.add(i - m - 1);
            }
        }
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 796 — ROTATE STRING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 796 — Rotate String
     *
     * PROBLEM: Check if s can become goal by rotating it some number of times.
     *
     * KEY INSIGHT:
     *   s can be rotated to become goal iff goal is a substring of s + s
     *   AND s.length() == goal.length().
     *
     * Z-ALGORITHM APPROACH:
     *   Build combined = goal + "$" + s + s
     *   Find if z[i] == goal.length() anywhere in the s+s portion.
     *
     * SIMPLER EQUIVALENT: Use (s + s).contains(goal) — but Z-algorithm is O(n) without
     * String.contains()'s potential overhead. Same complexity but cleaner conceptually.
     *
     * INTERVIEW Q: What does rotating a string once mean?
     * A: Move the first character to the end. "abcde" → "bcdea".
     *    After k rotations: s[k..n-1] + s[0..k-1].
     *    All rotations appear as substrings of s+s.
     *
     * @param s    original string
     * @param goal target string after rotation
     * @return true if s can be rotated to become goal
     */
    public static boolean rotateString(String s, String goal) {
        if (s.length() != goal.length()) return false;
        if (s.equals(goal)) return true; // 0 rotations

        // Check if goal is a substring of s+s using Z-algorithm
        List<Integer> matches = zSearch(s + s, goal);
        return !matches.isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 459 — REPEATED SUBSTRING PATTERN (via Z-array)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 459 — Repeated Substring Pattern (Z-algorithm version)
     *
     * KEY INSIGHT — Minimum period via Z-array:
     *   A string s has a repeated substring pattern iff its minimum period p divides n.
     *   Minimum period p = smallest p such that s[i] == s[i % p] for all i.
     *
     * HOW TO FIND MINIMUM PERIOD:
     *   Find smallest i > 0 where z[i] + i == n (or z[i] >= n - i).
     *   This means s[i..n-1] matches prefix s[0..n-i-1] → period = i.
     *   If p = i divides n, then s is p repeated.
     *
     * ALTERNATIVE: Check if n % (n - lps[n-1]) == 0 (KMP LPS approach — same result).
     *
     * INTERVIEW Q: Prove the period-Z connection.
     * A: If z[i] + i == n, then s[i..n-1] == s[0..n-i-1].
     *    If i divides n, this "wraps around" perfectly: each block of length i is the same.
     *
     * @param s input string
     * @return true if s is formed by repeating a substring
     */
    public static boolean repeatedSubstringPattern(String s) {
        int n = s.length();
        int[] z = buildZArray(s);

        for (int i = 1; i < n; i++) {
            // z[i] + i == n means s[i..n-1] == s[0..n-i-1] (suffix equals a prefix)
            if (z[i] + i == n && n % i == 0) {
                return true; // minimum period i divides n → repeated pattern found
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: MINIMUM PERIOD OF A STRING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Find the minimum period of a string using the Z-array.
     *
     * DEFINITION: Minimum period p = smallest p such that s[i] == s[i % p] for all i.
     * If no period < n exists (s is its own shortest period), return n.
     *
     * KEY INSIGHT:
     *   For each candidate period i, check if z[i] >= n - i (the suffix matches the prefix).
     *   If yes, i is a valid period (not necessarily minimum yet — we take the first/smallest).
     *
     * APPLICATIONS:
     *   - Detect if a string is periodic
     *   - Find the basic repeating unit of a repeated string
     *   - Bioinformatics: find repeated motifs in DNA
     *
     * @param s input string
     * @return minimum period length (1 to n)
     */
    public static int minimumPeriod(String s) {
        int n = s.length();
        int[] z = buildZArray(s);

        for (int i = 1; i < n; i++) {
            // s[i..n-1] is a suffix matching the prefix of length n-i
            // If z[i] >= n - i, then period i is valid
            if (z[i] >= n - i && n % i == 0) {
                return i; // smallest valid period that divides n
            }
        }
        return n; // no smaller period — the string itself is the minimum period
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMPARISON: KMP vs Z-ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════
    /*
     * KMP vs Z-ALGORITHM COMPARISON
     * ───────────────────────────────
     * Feature               | KMP (LPS)                | Z-Algorithm
     * ──────────────────────|──────────────────────────|──────────────────────────────
     * Core concept          | prefix = suffix of prefixes| prefix = substring at position i
     * Build complexity      | O(m)                     | O(n)
     * Search complexity     | O(n + m)                 | O(n + m) (concatenate then build)
     * Intuition             | "Where can I restart?"   | "How long does prefix extend here?"
     * Implementation ease   | Moderate (3-case logic)  | Slightly easier (2-case logic)
     * Pattern in result     | lps[i] = length of match | z[i] = length of match from i
     * Overlapping matches   | Natural via lps           | Natural via z
     * Period detection      | n % (n - lps[n-1]) == 0  | z[i] + i == n && n % i == 0
     * Palindrome prefix     | Concat + LPS trick        | Concat + Z trick (same idea)
     * Popularity (interviews)| More common              | Less common (but valid!)
     *
     * BOTTOM LINE:
     *   If you're comfortable with KMP, stick with it — interviewers expect KMP.
     *   If you find Z-algorithm easier to implement under pressure, use it.
     *   Both are O(n + m) and produce equivalent results.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Z-Array Construction Tests ===");
        System.out.println(Arrays.toString(buildZArray("aabxaa")));   // [0,1,0,0,2,1]
        System.out.println(Arrays.toString(buildZArray("aaaaaa")));   // [0,5,4,3,2,1]
        System.out.println(Arrays.toString(buildZArray("abcabc")));   // [0,0,0,3,0,0]
        System.out.println(Arrays.toString(buildZArray("abcdef")));   // [0,0,0,0,0,0] (no prefix repeats)
        System.out.println(Arrays.toString(buildZArray("aabaab")));   // [0,1,0,3,1,0]

        System.out.println("\n=== Z-Algorithm Pattern Search ===");
        System.out.println(zSearch("aabxaabaabxaa", "aab")); // [0, 4, 7] (or similar)
        System.out.println(zSearch("hello world", "world")); // [6]
        System.out.println(zSearch("aaaa", "aa")); // [0, 1, 2]
        System.out.println(zSearch("abcdef", "xyz")); // []

        System.out.println("\n=== LC 796 Rotate String ===");
        System.out.println(rotateString("abcde", "cdeab")); // true (rotate by 2)
        System.out.println(rotateString("abcde", "abced")); // false
        System.out.println(rotateString("aa", "aa"));       // true (0 rotations)
        System.out.println(rotateString("a", "b"));          // false

        System.out.println("\n=== LC 459 Repeated Substring Pattern (Z-version) ===");
        System.out.println(repeatedSubstringPattern("abab"));         // true (period=2)
        System.out.println(repeatedSubstringPattern("aba"));          // false
        System.out.println(repeatedSubstringPattern("abcabcabcabc")); // true (period=3)
        System.out.println(repeatedSubstringPattern("a"));            // false

        System.out.println("\n=== Minimum Period Tests ===");
        System.out.println(minimumPeriod("abcabcabc")); // 3 ("abc" repeated 3 times)
        System.out.println(minimumPeriod("aaaa"));      // 1 ("a" repeated 4 times)
        System.out.println(minimumPeriod("abab"));      // 2 ("ab" repeated 2 times)
        System.out.println(minimumPeriod("abcdef"));    // 6 (no smaller period)
    }
}
