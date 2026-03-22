package com.interview.prep;

import java.util.HashMap;
import java.util.Map;

/**
 * MAX CONSECUTIVE SEQUENCE — Find the character with the longest run
 *
 * PROBLEM:
 *   Given a string like "aaabbaaaacccccbbbbcc", find which character has the
 *   longest consecutive run. E.g., 'c' runs 5 times ("ccccc") → answer is 'c' with length 5.
 *
 * BUGS IN THE ORIGINAL CODE:
 *   1. OFF-BY-ONE: str.charAt(i + 1) crashes with StringIndexOutOfBoundsException
 *      when i == str.length() - 1 (last character). Fix: loop to length-1, then
 *      handle the last group outside the loop.
 *   2. EMPTY findMaxSeq(): The method body was never implemented.
 *      Fix: inside findMaxSeq, record the substring length in a map and track the max.
 *
 * APPROACH:
 *   Two-pointer / grouping scan:
 *   - Walk the string. When the current char differs from the next, we've found
 *     the end of a consecutive run.
 *   - Call findMaxSeq() on that substring (e.g., "aaa", "bb", "aaaa").
 *   - findMaxSeq stores the (char → max length) in a map and updates the global max.
 *
 * INTERVIEW TIP:
 *   A cleaner O(n) single-pass approach uses just two variables: currentChar and
 *   currentCount. Avoids substring allocation. Show both — demonstrates awareness
 *   of memory optimization.
 *
 * Time: O(n)   Space: O(n) for substring approach, O(1) for clean approach
 */
public class Test {

    // Track the maximum run length seen so far
    static int maxLen = 0;

    // The character that achieved the maximum run
    static char maxChar = ' ';

    // Map of character → its maximum consecutive run length seen so far
    public static Map<Character, Integer> map = new HashMap<>();

    public static void main(String[] args) {

        System.out.println("=== Max Consecutive Character Sequence ===");

        // --- Normal case ---
        // "aaabbaaaacccccbbbbcc" breakdown:
        //   aaa(3) bb(2) aaaa(4) ccccc(5) bbbb(4) cc(2) → max is 'c' with 5
        String str = "aaabbaaaacccccbbbbcc";
        runAnalysis(str);
        System.out.println("Input: " + str);
        System.out.printf("Max run: '%c' appears %d times consecutively%n%n", maxChar, maxLen);

        // Reset state before next test
        resetState();

        // --- Edge case: single character ---
        String str2 = "aaaaa";
        runAnalysis(str2);
        System.out.println("Input: " + str2);
        System.out.printf("Max run: '%c' appears %d times consecutively%n%n", maxChar, maxLen);

        resetState();

        // --- Edge case: all unique ---
        String str3 = "abcdef";
        runAnalysis(str3);
        System.out.println("Input: " + str3);
        System.out.printf("Max run: '%c' appears %d times consecutively%n%n", maxChar, maxLen);

        resetState();

        // --- Cleaner approach (no substrings, O(1) space) ---
        System.out.println("=== Clean O(1) Space Approach ===");
        findMaxConsecutive("aaabbaaaacccccbbbbcc");
        findMaxConsecutive("zzzzz");
        findMaxConsecutive("abcde");
    }

    /**
     * Runs the grouping analysis on the input string.
     *
     * BUG FIX: Loop runs to str.length() - 1 (not str.length()) to avoid
     * accessing charAt(i+1) out of bounds. After the loop, we process the
     * final group manually (it was never terminated by a character change).
     */
    static void runAnalysis(String str) {
        if (str == null || str.isEmpty()) return;

        int groupStart = 0; // Index where the current consecutive run started

        // Loop to length-1 so that charAt(i+1) is always valid
        for (int i = 0; i < str.length() - 1; i++) {
            char current = str.charAt(i);
            char next = str.charAt(i + 1); // Safe now because i < length-1

            if (current != next) {
                // The run of 'current' just ended at index i
                // Extract the consecutive substring from groupStart to i (inclusive)
                findMaxSeq(str.substring(groupStart, i + 1));
                groupStart = i + 1; // Next group starts at i+1
            }
        }

        // FIX: Handle the LAST group — the loop above misses it because there's
        // no character after the last group to trigger the if-condition.
        findMaxSeq(str.substring(groupStart));
    }

    /**
     * Updates the global maximum if the given consecutive-character string
     * is longer than any we've seen before.
     *
     * @param s  a string of identical consecutive characters, e.g., "aaaa"
     */
    public static void findMaxSeq(String s) {
        if (s == null || s.isEmpty()) return;

        char c = s.charAt(0);    // All chars in s are the same — grab the first
        int len = s.length();    // Length of this consecutive run

        // Update the map: store the MAXIMUM run seen for this character
        // (The same character might have multiple runs, e.g., "aaa...aaaa")
        map.put(c, Math.max(map.getOrDefault(c, 0), len));

        // Update global max if this run beats the current record
        if (len > maxLen) {
            maxLen = len;
            maxChar = c;
        }
    }

    static void resetState() {
        maxLen = 0;
        maxChar = ' ';
        map.clear();
    }

    // -------------------------------------------------------------------------
    // CLEANER APPROACH — O(1) space, single pass, no substring allocations
    // -------------------------------------------------------------------------

    /**
     * Finds the character with the longest consecutive run using just counters.
     * No substrings, no HashMap — pure O(1) extra space.
     *
     * WHY THIS IS BETTER:
     *   The substring approach creates String objects in the heap for each group.
     *   This approach avoids all allocations — better for large strings or
     *   when the interviewer asks about memory efficiency.
     */
    public static void findMaxConsecutive(String str) {
        if (str == null || str.isEmpty()) {
            System.out.println("  Empty input");
            return;
        }

        char bestChar = str.charAt(0); // Character with the longest run
        int bestCount = 1;             // Length of the longest run

        char currentChar = str.charAt(0); // Character in the current run
        int currentCount = 1;             // Length of the current run

        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) == currentChar) {
                currentCount++; // Extend the current run
            } else {
                // New character detected — close the current run
                if (currentCount > bestCount) {
                    bestCount = currentCount;
                    bestChar = currentChar;
                }
                // Start a new run
                currentChar = str.charAt(i);
                currentCount = 1;
            }
        }

        // Check the final run (same reason as the original bug — last group not checked)
        if (currentCount > bestCount) {
            bestCount = currentCount;
            bestChar = currentChar;
        }

        System.out.printf("  Input: %-25s → Max run: '%c' × %d%n", str, bestChar, bestCount);
    }
}
