package com.java.ds.stringalgo;

import java.util.*;

/**
 * CONCEPT: Rabin-Karp Rolling Hash Algorithm
 * ────────────────────────────────────────────
 * Rabin-Karp hashes a window of the text using a polynomial rolling hash function,
 * then slides the window one character at a time by updating the hash in O(1).
 * When a hash matches the pattern's hash, verify with actual string comparison.
 *
 * ROLLING HASH FORMULA:
 *   hash(s[i..i+m-1]) = (s[i] * BASE^(m-1) + s[i+1] * BASE^(m-2) + ... + s[i+m-1]) % MOD
 *
 * SLIDE WINDOW (from position i to i+1):
 *   new_hash = (old_hash - s[i] * BASE^(m-1)) * BASE + s[i+m]
 *   (remove leftmost char contribution, shift left, add new rightmost char)
 *   Apply % MOD throughout to keep numbers manageable.
 *
 * KEY INSIGHT — Two-Step Verification:
 *   Hash match is NECESSARY but not SUFFICIENT (hash collisions can occur).
 *   Always verify with actual string comparison when hashes match.
 *   Average: O(n+m). Worst case with many false positives: O(n*m) (rare with good hash).
 *
 * WHEN TO USE RABIN-KARP vs KMP:
 *   - Multi-pattern search (hash multiple patterns, check all at once): Rabin-Karp shines
 *   - Single pattern: KMP is preferred (no collision risk, same complexity)
 *   - Substring problems requiring binary search + hash: Rabin-Karp is ideal (LC 1044)
 *
 * TIME/SPACE COMPLEXITY:
 *   Average: O(n + m), Worst case: O(n * m) (due to collisions)
 *   Space: O(1) extra (just hash values, no LPS array needed)
 *
 * INTERVIEW ANGLE:
 *   Rabin-Karp is most powerful when combined with binary search (binary search on length,
 *   then use rolling hash to check for any match). This pattern appears in LC 1044.
 *   Always mention double hashing to reduce collision probability when asked about robustness.
 *
 * REAL-WORLD USE:
 *   - Plagiarism detection (search for copied code fragments)
 *   - Substring search in databases
 *   - File sync (rsync uses rolling checksums for block comparison)
 *   - DNA sequence search (search for multiple gene sequences simultaneously)
 */
public class RabinKarp {

    // Constants for rolling hash
    private static final long BASE = 31L;        // prime close to 26 (for lowercase letters)
    private static final long MOD  = 1_000_000_007L; // large prime to reduce collisions

    // For double hashing (secondary hash to further reduce collision probability)
    private static final long BASE2 = 37L;
    private static final long MOD2  = 998_244_353L; // another large prime

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: CORE RABIN-KARP SEARCH
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rabin-Karp Single Pattern Search
     *
     * KEY INSIGHT — Rolling hash sliding window:
     *   1. Compute hash of pattern (O(m))
     *   2. Compute hash of text[0..m-1] (first window, O(m))
     *   3. Slide window: remove leftmost char, add rightmost char (O(1) per step)
     *   4. On hash match: verify with actual string comparison (O(m) but rare)
     *
     * MODULAR ARITHMETIC GOTCHA:
     *   When subtracting, result can be negative in Java (unlike Python which handles
     *   negative mod correctly). Always add MOD before taking % MOD after subtraction:
     *   (a - b % MOD + MOD) % MOD
     *
     * @param text    text to search in
     * @param pattern pattern to find
     * @return list of starting indices where pattern matches (0-indexed)
     */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m > n) return matches;

        // Precompute BASE^(m-1) % MOD — needed to remove leftmost character
        long power = 1L;
        for (int i = 0; i < m - 1; i++) {
            power = (power * BASE) % MOD; // BASE^(m-1)
        }

        // Compute hash of pattern
        long patHash = 0L;
        for (int i = 0; i < m; i++) {
            patHash = (patHash * BASE + (pattern.charAt(i) - 'a' + 1)) % MOD;
        }

        // Compute hash of first window
        long winHash = 0L;
        for (int i = 0; i < m; i++) {
            winHash = (winHash * BASE + (text.charAt(i) - 'a' + 1)) % MOD;
        }

        for (int i = 0; i <= n - m; i++) {
            // Hash match: verify to rule out false positives
            if (winHash == patHash && text.substring(i, i + m).equals(pattern)) {
                matches.add(i);
            }

            // Slide the window: remove text[i], add text[i+m]
            if (i < n - m) {
                long leftChar = (text.charAt(i) - 'a' + 1);
                long rightChar = (text.charAt(i + m) - 'a' + 1);
                // Remove leftmost char: subtract its contribution (power * leftChar)
                // GOTCHA: add MOD before mod to prevent negative values
                winHash = (winHash - leftChar * power % MOD + MOD) % MOD;
                // Shift left (multiply by BASE) and add new rightmost char
                winHash = (winHash * BASE + rightChar) % MOD;
            }
        }
        return matches;
    }
    /*
     * COMPLEXITY — Rabin-Karp Search
     * ────────────────────────────────
     * TIME:  O(n + m) average (O(n*m) worst case with many collisions)
     * SPACE: O(1) extra — just hash values (output list aside)
     *
     * The probability of a false positive with MOD = 10^9+7:
     * ≈ m/MOD per comparison ≈ negligible for typical inputs.
     * With double hashing: probability ≈ m/(MOD1 × MOD2) ≈ 10^-18 per comparison.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: DOUBLE HASHING (Reduce Collision Probability)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Double Hashing — Compute two independent hashes to minimize false positives.
     *
     * KEY INSIGHT: Two independent hash functions. A false positive requires BOTH
     * to collide simultaneously. Probability: ≈ 1/(MOD1 × MOD2) ≈ 10^-18.
     * This is essentially impossible in practice.
     *
     * TRADE-OFF: Double the computation, near-zero collision probability.
     * In contest/interview: single hash is usually fine; double hash when MOD is small.
     *
     * Returns a pair [hash1, hash2] for a string.
     */
    public static long[] computeDoubleHash(String s, int start, int len) {
        long h1 = 0L, h2 = 0L;
        for (int i = start; i < start + len; i++) {
            int c = s.charAt(i) - 'a' + 1;
            h1 = (h1 * BASE + c) % MOD;
            h2 = (h2 * BASE2 + c) % MOD2;
        }
        return new long[]{h1, h2};
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 187 — REPEATED DNA SEQUENCES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 187 — Repeated DNA Sequences
     *
     * PROBLEM: Find all 10-letter DNA sequences (substrings) that appear more than once.
     *
     * KEY INSIGHT: Rolling hash on a fixed window of size 10.
     * DNA has only 4 characters (A, C, G, T), so we can use BASE=4 encoding.
     * Alternatively: use a HashSet of string substrings (simpler, O(n × 10) = O(n)).
     *
     * Rabin-Karp approach: O(n) time with rolling hash vs O(n × 10) with substring hashing.
     * For n=100K and window=10: the difference is negligible; HashSet<String> works fine.
     *
     * INTERVIEW Q: What if the window size were large (e.g., 1000)?
     * A: Rolling hash becomes critical — O(n) vs O(n × 1000) = very different.
     *
     * GOTCHA: The result should have unique sequences. Use a set to deduplicate.
     *
     * @param s DNA string (contains only 'A', 'C', 'G', 'T')
     * @return list of all 10-letter sequences that appear more than once
     */
    public static List<String> findRepeatedDnaSequences(String s) {
        int n = s.length();
        List<String> result = new ArrayList<>();
        if (n <= 10) return result;

        // Encode DNA: A=1, C=2, G=3, T=4
        Map<Character, Integer> dnaMap = Map.of('A', 1, 'C', 2, 'G', 3, 'T', 4);
        int window = 10;
        long base = 4L; // 4 possible characters
        long mod = MOD;

        // Precompute base^(window-1) % mod
        long power = 1L;
        for (int i = 0; i < window - 1; i++) power = (power * base) % mod;

        // Compute initial hash for first window
        long winHash = 0L;
        for (int i = 0; i < window; i++) {
            winHash = (winHash * base + dnaMap.get(s.charAt(i))) % mod;
        }

        Set<Long> seen = new HashSet<>();
        Set<String> resultSet = new HashSet<>(); // avoid duplicates in output
        seen.add(winHash);

        for (int i = 1; i <= n - window; i++) {
            // Roll: remove leftmost char, add rightmost char
            long leftChar = dnaMap.get(s.charAt(i - 1));
            long rightChar = dnaMap.get(s.charAt(i + window - 1));
            winHash = (winHash - leftChar * power % mod + mod) % mod;
            winHash = (winHash * base + rightChar) % mod;

            if (seen.contains(winHash)) {
                // Hash match → verify (guard against collision)
                String sub = s.substring(i, i + window);
                resultSet.add(sub); // actual duplicate found (substring IS the verification)
            }
            seen.add(winHash);
        }
        return new ArrayList<>(resultSet);
    }
    /*
     * COMPLEXITY — Repeated DNA Sequences (Rolling Hash)
     * ────────────────────────────────────────────────────
     * TIME:  O(n) — one pass with O(1) hash updates
     * SPACE: O(n) — HashSet of hashes (up to n-10 entries)
     *
     * Note: Java's HashSet<String> approach is also O(n) with O(n) space but
     * substring() creates O(window) objects per step → O(n × window) = O(n × 10) = O(n).
     * Rolling hash avoids object allocation, which is faster in practice.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 1044 — LONGEST DUPLICATE SUBSTRING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 1044 — Longest Duplicate Substring
     *
     * KEY INSIGHT — Binary Search + Rolling Hash:
     *   1. Binary search on the answer (length L of the duplicate substring)
     *   2. For each L, use rolling hash to check: "does any length-L substring appear twice?"
     *   3. If YES → try longer (go right). If NO → try shorter (go left).
     *
     * WHY BINARY SEARCH WORKS:
     *   Monotonicity: if a duplicate of length L exists, a duplicate of length L-1 also exists
     *   (just take a prefix of the duplicate). So we can binary search.
     *
     * ROLLING HASH for EXISTENCE CHECK (O(n)):
     *   Use a HashSet of hashes. Slide window of size L. If hash seen before → duplicate found.
     *   Use double hashing to minimize false positives.
     *
     * TOTAL COMPLEXITY: O(n log n) — log n binary search iterations × O(n) each
     *
     * INTERVIEW Q: Why not just use suffix arrays (O(n log n) construction)?
     * A: Suffix arrays give O(n log n) total and are theoretically elegant, but harder to
     *    code in an interview. Binary search + rolling hash is easier to explain and implement.
     *
     * @param s input string (lowercase letters)
     * @return longest substring that appears at least twice (empty string if none)
     */
    public static String longestDupSubstring(String s) {
        int n = s.length();
        // Precompute character values for rolling hash
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = s.charAt(i) - 'a' + 1;

        // Binary search on length
        int lo = 1, hi = n - 1;
        String result = "";

        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            String dup = checkForDuplicate(s, nums, mid);
            if (dup != null) {
                result = dup;
                lo = mid + 1; // found duplicate of length mid, try longer
            } else {
                hi = mid - 1; // no duplicate of length mid, try shorter
            }
        }
        return result;
    }

    /**
     * KEY INSIGHT: Check if any substring of length `len` appears at least twice.
     * Uses rolling hash with double hashing to minimize collisions.
     * Returns the duplicate substring, or null if none exists.
     */
    private static String checkForDuplicate(String s, int[] nums, int len) {
        int n = nums.length;

        // Compute power = BASE^(len-1) % MOD (for removing leftmost char)
        long power1 = 1L, power2 = 1L;
        for (int i = 0; i < len - 1; i++) {
            power1 = (power1 * BASE) % MOD;
            power2 = (power2 * BASE2) % MOD2;
        }

        // Compute hash of first window
        long h1 = 0, h2 = 0;
        for (int i = 0; i < len; i++) {
            h1 = (h1 * BASE + nums[i]) % MOD;
            h2 = (h2 * BASE2 + nums[i]) % MOD2;
        }

        // Use a map from hash pair to starting index (for verification)
        // Key: combined double hash (h1 * MOD2 + h2)
        Map<Long, Integer> seen = new HashMap<>();
        long key = h1 * MOD2 + h2;
        seen.put(key, 0);

        for (int i = 1; i <= n - len; i++) {
            // Roll the window
            h1 = (h1 - (long) nums[i - 1] * power1 % MOD + MOD) % MOD;
            h1 = (h1 * BASE + nums[i + len - 1]) % MOD;
            h2 = (h2 - (long) nums[i - 1] * power2 % MOD2 + MOD2) % MOD2;
            h2 = (h2 * BASE2 + nums[i + len - 1]) % MOD2;

            key = h1 * MOD2 + h2;
            if (seen.containsKey(key)) {
                // Hash match — verify to guard against collision
                int prev = seen.get(key);
                if (s.substring(i, i + len).equals(s.substring(prev, prev + len))) {
                    return s.substring(i, i + len); // confirmed duplicate
                }
            }
            seen.put(key, i);
        }
        return null; // no duplicate of this length
    }
    /*
     * COMPLEXITY — Longest Duplicate Substring
     * ──────────────────────────────────────────
     * TIME:  O(n log n) — log n binary search iterations, each O(n) with rolling hash
     * SPACE: O(n) — HashMap of hashes for each window position
     *
     * Suffix array approach: O(n log n) construction, O(n) query → same complexity but
     * complex to implement. Rolling hash + binary search is more interview-friendly.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // COMPARISON: KMP vs RABIN-KARP
    // ═══════════════════════════════════════════════════════════════════════
    /*
     * KMP vs RABIN-KARP COMPARISON
     * ─────────────────────────────
     * Feature             | KMP                    | Rabin-Karp
     * ─────────────────────|─────────────────────── |──────────────────────────────────
     * Preprocessing       | O(m) — build LPS       | O(m) — compute pattern hash
     * Search time (avg)   | O(n) — guaranteed      | O(n) — expected
     * Search time (worst) | O(n) — guaranteed      | O(n*m) — with many collisions
     * False positives     | Never                  | Possible (require verification)
     * Multi-pattern       | Needs Aho-Corasick      | Native (check all hashes at once)
     * Longest dup substr  | Hard to adapt          | Natural (binary search + hash)
     * Implementation      | Moderate (LPS logic)   | Simple (arithmetic)
     * Memory              | O(m) for LPS            | O(1) for single hash
     *
     * RULE OF THUMB:
     *   Single pattern, must be exact → KMP
     *   Multi-pattern search → Aho-Corasick (or Rabin-Karp with hash set)
     *   Binary search + existence check → Rabin-Karp
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Rabin-Karp Search Tests ===");
        System.out.println(search("abcabcabc", "abc")); // [0, 3, 6]
        System.out.println(search("hello world", "world")); // [6]
        System.out.println(search("aaaa", "aa")); // [0, 1, 2]
        System.out.println(search("abcdef", "xyz")); // []

        System.out.println("\n=== LC 187 Repeated DNA Sequences ===");
        System.out.println(findRepeatedDnaSequences("AAAAACCCCCAAAAACCCCCCAAAAAGGGTTT"));
        // Expected: ["AAAAACCCCC","CCCCCAAAAA"]
        System.out.println(findRepeatedDnaSequences("AAAAAAAAAAAAA"));
        // Expected: ["AAAAAAAAAA"]
        System.out.println(findRepeatedDnaSequences("AGTCAGTCAGTC"));
        // 10-char windows: AGTCAGTCAG, GTCAGTCAGT, TCAGTCAGTC → check for duplicates

        System.out.println("\n=== LC 1044 Longest Duplicate Substring ===");
        System.out.println(longestDupSubstring("banana")); // "ana" (appears at index 1 and 3)
        System.out.println(longestDupSubstring("abcd"));   // "" (no duplicate substring)

        System.out.println("\n=== Double Hash Demo ===");
        long[] hash1 = computeDoubleHash("banana", 1, 3); // hash of "ana"
        long[] hash2 = computeDoubleHash("banana", 3, 3); // hash of "ana" again
        System.out.println("Hash of 'ana' at pos 1: [" + hash1[0] + ", " + hash1[1] + "]");
        System.out.println("Hash of 'ana' at pos 3: [" + hash2[0] + ", " + hash2[1] + "]");
        System.out.println("Hashes match: " + (hash1[0] == hash2[0] && hash1[1] == hash2[1]));
    }
}
