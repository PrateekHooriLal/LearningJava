package com.java.ds.stringalgo;

import java.util.*;

/**
 * CONCEPT: KMP (Knuth-Morris-Pratt) String Matching Algorithm
 * ─────────────────────────────────────────────────────────────
 * KMP avoids redundant comparisons by precomputing a "failure function" (LPS array)
 * that tells us how far to "fall back" in the pattern upon a mismatch — without
 * ever moving the text pointer backward.
 *
 * CORE IDEA:
 *   When a mismatch at pattern[j] occurs, we know text[i-j..i-1] == pattern[0..j-1].
 *   The LPS array tells us the longest proper prefix of pattern[0..j-1] that is also
 *   a suffix. We can restart matching from lps[j-1] instead of 0, skipping known matches.
 *
 * WHAT IS LPS (Longest Proper Prefix which is also Suffix)?
 *   For pattern "AABAAB":
 *   Index: 0  1  2  3  4  5
 *   Char:  A  A  B  A  A  B
 *   LPS:   0  1  0  1  2  3
 *   lps[5]=3 means pattern[0..2] == pattern[3..5] == "AAB"
 *
 * MISMATCH RULE:
 *   If j > 0: j = lps[j-1]  (don't restart from 0, jump back smartly)
 *   If j == 0: i++           (truly no prefix match, advance text)
 *
 * WHEN TO USE:
 *   - Single-pattern search in a text: O(n+m) vs O(n*m) brute force
 *   - Repeated substring detection
 *   - Palindrome prefix problems
 *
 * TIME/SPACE COMPLEXITY: O(n+m) time, O(m) space (LPS array)
 *
 * INTERVIEW ANGLE:
 *   Brute force is O(n×m). KMP is O(n+m). The interviewer cares about WHY.
 *   KMP never backtracks the text pointer. Each character is compared at most twice.
 *   Building LPS: O(m). Searching: O(n). Total: O(n+m).
 *
 * REAL-WORLD USE:
 *   - Text editors (Ctrl+F search)
 *   - Antivirus signature matching
 *   - DNA subsequence search
 *   - Network intrusion detection (packet payload scanning)
 */
public class KMPAlgorithm {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: BUILD LPS (FAILURE FUNCTION)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Build LPS (Longest Proper Prefix which is also Suffix) array.
     *
     * KEY INSIGHT — Two-pointer approach:
     *   `len` tracks the length of the current longest prefix-suffix match.
     *   `i` scans the pattern from left to right.
     *
     * THREE CASES at each step:
     *   1. pattern[i] == pattern[len]: match → lps[i] = ++len, advance i
     *   2. pattern[i] != pattern[len] AND len != 0: fall back → len = lps[len-1]
     *      (don't reset len to 0 — try the NEXT shorter prefix-suffix)
     *   3. pattern[i] != pattern[len] AND len == 0: no prefix-suffix → lps[i] = 0, advance i
     *
     * INTERVIEW Q: Why do we set len = lps[len-1] on mismatch (case 2)?
     * A: lps[len-1] gives the length of the next longest prefix-suffix of pattern[0..len-1].
     *    We know pattern[0..lps[len-1]-1] == pattern[len-lps[len-1]..len-1],
     *    so we try to extend that shorter prefix-suffix next.
     *
     * GOTCHA: lps[0] is always 0 (a single character has no proper prefix-suffix).
     *
     * @param pattern the pattern string to build LPS for
     * @return lps[] array where lps[i] = longest proper prefix-suffix length for pattern[0..i]
     */
    public static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        lps[0] = 0; // always 0 (single char has no proper prefix-suffix)

        int len = 0; // length of the current longest prefix-suffix
        int i = 1;   // start from index 1 (lps[0] is done)

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                // Case 1: match → extend the prefix-suffix
                lps[i] = ++len; // set lps[i] = len+1, then increment len
                i++;
            } else if (len != 0) {
                // Case 2: mismatch, but len > 0 → fall back to shorter prefix-suffix
                // DO NOT increment i here — we'll retry pattern[i] with the shorter prefix
                len = lps[len - 1]; // key: use lps to find next candidate
            } else {
                // Case 3: mismatch, len == 0 → no prefix-suffix possible, lps[i] = 0
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }
    /*
     * COMPLEXITY — Build LPS
     * ───────────────────────
     * TIME:  O(m) — i only increases, len only increases via i, falls back at most as many
     *         times as it increased → total len changes bounded by O(m)
     * SPACE: O(m) — the lps array
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: KMP SEARCH
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * KMP String Search — Find all occurrences of pattern in text.
     *
     * KEY INSIGHT:
     *   i = text pointer (NEVER goes backward)
     *   j = pattern pointer (falls back using lps on mismatch)
     *
     * On mismatch: j = lps[j-1] (try shorter prefix-suffix for pattern)
     *              DO NOT reset i — text progress is never lost
     * On full match (j == m): record position, then j = lps[j-1] to find overlapping matches
     *
     * COMPLEXITY ARGUMENT:
     *   i increases monotonically: O(n) text comparisons
     *   j increases at most n times total (bounded by i increases)
     *   j decreases at most n times total (bounded by increases)
     *   Total: O(n) for search phase
     *
     * @param text    the text to search in
     * @param pattern the pattern to search for
     * @return list of starting indices where pattern occurs in text (0-indexed)
     */
    public static List<Integer> kmpSearch(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0) return matches;

        int[] lps = buildLPS(pattern);

        int i = 0; // text index
        int j = 0; // pattern index

        while (i < n) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
            }

            if (j == m) {
                // Full match found: pattern starts at i - j = i - m
                matches.add(i - j);
                // DON'T reset j to 0 — use lps to look for overlapping matches
                j = lps[j - 1];
            } else if (i < n && text.charAt(i) != pattern.charAt(j)) {
                if (j != 0) {
                    j = lps[j - 1]; // fall back in pattern, don't move i
                } else {
                    i++; // no prefix-suffix to fall back to, advance text
                }
            }
        }
        return matches;
    }
    /*
     * COMPLEXITY — KMP Search
     * ────────────────────────
     * TIME:  O(n + m) — O(m) to build LPS, O(n) to search
     * SPACE: O(m) — LPS array (O(1) extra beyond input)
     *
     * vs Brute Force: O(n × m) worst case (e.g., text="aaaaab", pattern="aaab")
     * KMP amortizes mismatch costs via the LPS table.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 28 — FIND INDEX OF FIRST OCCURRENCE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 28 — Find the Index of the First Occurrence in a String
     *
     * KEY INSIGHT: Direct application of KMP search. Return the first match index, or -1.
     *
     * BRUTE FORCE: Two nested loops → O(n × m). KMP: O(n + m).
     *
     * INTERVIEW FOLLOW-UP: "What's the naive approach vs KMP?"
     * Naive: slide pattern one position at a time, restart on mismatch.
     * KMP: on mismatch, don't restart — use the LPS to skip already-matched portion.
     *
     * @param haystack the text
     * @param needle   the pattern
     * @return starting index of first occurrence of needle in haystack, or -1
     */
    public static int strStr(String haystack, String needle) {
        if (needle.isEmpty()) return 0; // GOTCHA: empty needle always matches at index 0
        List<Integer> matches = kmpSearch(haystack, needle);
        return matches.isEmpty() ? -1 : matches.get(0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 214 — SHORTEST PALINDROME
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 214 — Shortest Palindrome
     *
     * PROBLEM: Given string s, find the shortest palindrome by adding characters
     * ONLY to the FRONT of s.
     *
     * KEY INSIGHT:
     *   We need to find the longest palindromic PREFIX of s.
     *   If s[0..k] is a palindrome, we only need to add reverse(s[k+1..n-1]) to the front.
     *
     * HOW TO FIND LONGEST PALINDROMIC PREFIX USING KMP:
     *   Build: combined = s + "#" + reverse(s)
     *   The "#" separator prevents the LPS from spanning across the two halves.
     *   Compute LPS of combined. lps[last] = length of longest palindromic prefix of s.
     *
     * WHY IT WORKS:
     *   LPS of combined at the last position tells us: "how much of s (from the front)
     *   matches reverse(s) from the back" = longest palindromic prefix.
     *   Because: if s[0..k] is a palindrome, then s[0..k] == reverse(s)[end-k..end],
     *   which is exactly what LPS detects.
     *
     * GOTCHA: The "#" separator is crucial — without it, the LPS could be falsely extended
     * by matching s characters against themselves in the reversed part.
     *
     * @param s input string
     * @return shortest palindrome formed by adding chars to front of s
     */
    public static String shortestPalindrome(String s) {
        if (s.isEmpty()) return s;

        String rev = new StringBuilder(s).reverse().toString();
        // Combine with separator "#" to prevent cross-half matching
        String combined = s + "#" + rev;
        int[] lps = buildLPS(combined);

        // lps[last] = length of longest palindromic prefix of s
        int longestPalindromicPrefix = lps[combined.length() - 1];

        // Add the characters NOT in the palindromic prefix (reversed) to the front
        String toAdd = rev.substring(0, s.length() - longestPalindromicPrefix);
        return toAdd + s;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: LC 459 — REPEATED SUBSTRING PATTERN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 459 — Repeated Substring Pattern
     *
     * PROBLEM: Check if string s can be formed by repeating a substring multiple times.
     *
     * KEY INSIGHT — KMP approach:
     *   Build s + s, remove first and last characters (to avoid trivial match).
     *   If s appears in (s+s)[1..2n-2], then s has a repeated substring pattern.
     *
     * WHY IT WORKS:
     *   If s = p repeated k times, then s+s = p repeated 2k times.
     *   The string s appears starting at position |p| in s+s (before we trim the ends).
     *   After trimming first and last char: position |p|-1 to |p|+|s|-2 in trimmed string.
     *
     * ALTERNATIVE — LPS approach:
     *   Compute LPS of s. If lps[n-1] > 0 AND n % (n - lps[n-1]) == 0, then YES.
     *   Because: (n - lps[n-1]) is the minimum period of s.
     *
     * @param s input string
     * @return true if s is formed by repeating a substring
     */
    public static boolean repeatedSubstringPattern(String s) {
        // Method 1: s+s approach (trim first and last char to avoid trivial match)
        String doubled = s + s;
        String trimmed = doubled.substring(1, doubled.length() - 1);
        return kmpSearch(trimmed, s).size() > 0;
    }

    /**
     * LC 459 — Alternative using LPS directly (more elegant).
     * KEY INSIGHT: n % (n - lps[n-1]) == 0 checks if the minimum period divides n.
     */
    public static boolean repeatedSubstringPatternLPS(String s) {
        int n = s.length();
        int[] lps = buildLPS(s);
        int longestPrefixSuffix = lps[n - 1];
        // Minimum period = n - lps[n-1]
        // s has a repeated substring pattern iff period divides n AND period < n
        return longestPrefixSuffix > 0 && n % (n - longestPrefixSuffix) == 0;
    }
    /*
     * COMPLEXITY — Repeated Substring Pattern
     * ─────────────────────────────────────────
     * TIME:  O(n) — build LPS of s (O(n)), or search in s+s (O(n))
     * SPACE: O(n) — LPS array or doubled string
     */

    // ═══════════════════════════════════════════════════════════════════════
    // COMPARISON TABLE: KMP vs Brute Force
    // ═══════════════════════════════════════════════════════════════════════
    /*
     * ALGORITHM COMPARISON — String Search
     * ──────────────────────────────────────
     * Approach      | Preprocessing | Search   | Space | Notes
     * ──────────────|───────────────|──────────|───────|──────────────────────────
     * Brute Force   | O(1)          | O(n × m) | O(1)  | Simple but slow
     * KMP           | O(m)          | O(n)     | O(m)  | Never backtracks text pointer
     * Rabin-Karp    | O(m)          | O(n) avg | O(1)  | Rolling hash, good for multi-pattern
     * Z-Algorithm   | O(n+m)        | O(n+m)   | O(n)  | Conceptually similar to KMP
     * Boyer-Moore   | O(m + σ)      | O(n/m)   | O(σ)  | Fast in practice, complex to implement
     *
     * KEY DIFFERENCE (KMP vs Z-Algorithm):
     *   KMP's LPS = "prefix = suffix" for each prefix of pattern
     *   Z-array   = "prefix of whole string = substring starting at i"
     *   Both solve the same problems with the same complexity; KMP is more commonly expected.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== LPS Array Tests ===");
        System.out.println(Arrays.toString(buildLPS("AABAAB")));   // [0,1,0,1,2,3]
        System.out.println(Arrays.toString(buildLPS("ABCABD")));   // [0,0,0,1,2,0]
        System.out.println(Arrays.toString(buildLPS("AABAABAAB"))); // [0,1,0,1,2,3,4,5,6]
        System.out.println(Arrays.toString(buildLPS("ABCDE")));    // [0,0,0,0,0] (no repeats)

        System.out.println("\n=== KMP Search Tests ===");
        System.out.println(kmpSearch("ABABDABACDABABCABAB", "ABABCABAB")); // [10]
        System.out.println(kmpSearch("aaaaab", "aaa")); // [0,1,2] (overlapping)
        System.out.println(kmpSearch("hello", "ll")); // [2]

        System.out.println("\n=== LC 28 strStr Tests ===");
        System.out.println(strStr("hello", "ll"));   // 2
        System.out.println(strStr("aaaaa", "bba")); // -1
        System.out.println(strStr("", ""));          // 0

        System.out.println("\n=== LC 214 Shortest Palindrome ===");
        System.out.println(shortestPalindrome("aacecaaa")); // "aaacecaaa"
        System.out.println(shortestPalindrome("abcd"));      // "dcbabcd"
        System.out.println(shortestPalindrome(""));           // ""
        System.out.println(shortestPalindrome("a"));          // "a"

        System.out.println("\n=== LC 459 Repeated Substring Pattern ===");
        System.out.println(repeatedSubstringPattern("abab"));   // true (ab repeated)
        System.out.println(repeatedSubstringPattern("aba"));    // false
        System.out.println(repeatedSubstringPattern("abcabcabcabc")); // true (abc repeated 4x)
        System.out.println(repeatedSubstringPatternLPS("abab")); // true
        System.out.println(repeatedSubstringPatternLPS("aba"));  // false
    }
}
