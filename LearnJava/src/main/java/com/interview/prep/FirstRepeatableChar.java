package com.interview.prep;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * FIRST REPEATABLE CHARACTER — Classic HashMap Frequency Problem
 *
 * CONCEPT:
 *   Scan the string once. For each character, check if it already exists in
 *   a HashMap. If yes → it's the first repeating character, return it.
 *   If no → insert it. HashMap gives O(1) average lookup → entire pass is O(n).
 *
 *   Brute force alternative: nested loops — O(n²). Always mention this to
 *   the interviewer and explain why HashMap is better.
 *
 * INTERVIEW FREQUENCY: Very high at service companies (TCS, Infosys, Wipro)
 *   and medium at product companies as a warm-up problem.
 *
 * COMMON FOLLOW-UP QUESTIONS:
 *   1. "Do it in O(1) space" → use boolean[26] if only lowercase a–z.
 *   2. "Find the first NON-repeating character." (LC 387 — flip the condition)
 *   3. "What if the string has Unicode?" → char still works for BMP; use codePoints() for supplementary.
 *   4. "Find ALL repeating chars in order of first repeat."
 *
 * Time: O(n)   Space: O(k) where k = alphabet size (≤26 for a–z, so effectively O(1))
 */
public class FirstRepeatableChar {

    public static void main(String[] args) {

        // --- Normal cases ---
        System.out.println("=== First Repeating Character (HashMap) ===");
        System.out.println(getFRC("abcdeeeeefghiiiijkkkk")); // Expected: e
        System.out.println(getFRC("abcda"));                 // Expected: a  ('a' at index 0 repeats at index 4)
        System.out.println(getFRC("aabb"));                  // Expected: a

        // --- No repeat ---
        System.out.println(getFRC("abcdef"));                // Expected: -1 (none)

        // --- Edge cases ---
        System.out.println(getFRC(""));                      // Expected: -1 (empty string)
        System.out.println(getFRC("z"));                     // Expected: -1 (single char, can't repeat)

        // --- O(1) space variant ---
        System.out.println("\n=== O(1) Space Variant (boolean[26]) ===");
        System.out.println(getFRCO1Space("abcdeeeeefghiiiijkkkk")); // Expected: e
        System.out.println(getFRCO1Space("abcdef"));                // Expected: -1

        // --- Follow-up: First NON-repeating character (LC 387) ---
        System.out.println("\n=== First NON-Repeating Character (follow-up LC 387) ===");
        System.out.println(getFirstNonRepeating("leetcode")); // Expected: l
        System.out.println(getFirstNonRepeating("aabb"));     // Expected: -1
    }

    // -------------------------------------------------------------------------
    // APPROACH 1: Single-pass HashMap  — O(n) time, O(k) space
    // -------------------------------------------------------------------------

    /**
     * Returns the first character that appears more than once.
     *
     * KEY TRICK: Return immediately on the first duplicate hit — no need to
     *            finish scanning. This is correct because we're building the
     *            seen-set left to right, so the earliest duplicate is found first.
     *
     * WHY HashMap<Character, Boolean> and not Integer?
     *   We only need to know seen/unseen (a boolean). Using containsKey()
     *   avoids a get() + null-check. Alternatively, use a HashSet<Character>.
     */
    public static char getFRC(String s) {
        if (s == null || s.isEmpty()) {
            System.out.println("  → No repeating char (empty input)");
            return 0;
        }

        // HashSet is even cleaner here — we only care about presence, not count
        HashMap<Character, Boolean> seen = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (seen.containsKey(c)) {
                // This character appeared before → it is the first repeat
                System.out.print("  → First repeat: ");
                return c;
            }

            seen.put(c, true); // Mark character as seen for the first time
        }

        System.out.println("  → No repeating char found");
        return 0;
    }

    // -------------------------------------------------------------------------
    // APPROACH 2: O(1) space — boolean[26] (lowercase a–z only)
    // -------------------------------------------------------------------------

    /**
     * Same logic but uses a fixed 26-element boolean array instead of HashMap.
     * Works ONLY for lowercase English letters (a–z).
     *
     * WHY THIS IS PREFERRED for simple inputs:
     *   - No boxing overhead (HashMap stores Character objects, not primitives)
     *   - Better CPU cache performance (26 booleans = 26 bytes, fits in cache line)
     *   - Truly O(1) space — exactly 26 booleans regardless of input size
     *
     * INDEX TRICK:
     *   'a' - 'a' = 0, 'b' - 'a' = 1, ..., 'z' - 'a' = 25
     *   This maps each lowercase letter to a unique 0–25 index.
     */
    public static char getFRCO1Space(String s) {
        if (s == null || s.isEmpty()) return 0;

        boolean[] seen = new boolean[26]; // indices 0–25 map to 'a'–'z'

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int idx = c - 'a'; // Map char to array index

            if (seen[idx]) {
                System.out.print("  → First repeat: ");
                return c;
            }
            seen[idx] = true; // Mark as seen
        }

        System.out.println("  → No repeating char found");
        return 0;
    }

    // -------------------------------------------------------------------------
    // FOLLOW-UP: First NON-repeating character (LC 387)
    // -------------------------------------------------------------------------

    /**
     * Returns the first character that appears exactly ONCE.
     *
     * WHY TWO PASSES?
     *   A single left-to-right pass can't determine "non-repeating" because a
     *   later occurrence of a character invalidates the earlier "unique" label.
     *   Example: "loveleetcode" — 'l' seems unique at index 0 but repeats at 3.
     *   We must complete the full frequency count FIRST, then find the first unique.
     *
     * WHY LinkedHashMap?
     *   LinkedHashMap preserves insertion order. When we iterate keySet(),
     *   characters appear in the order they were first seen — exactly what
     *   "first non-repeating" requires.
     *
     * Time: O(n)  Space: O(k)
     */
    public static char getFirstNonRepeating(String s) {
        // LinkedHashMap preserves insertion order — critical for "first" requirement
        LinkedHashMap<Character, Integer> freq = new LinkedHashMap<>();

        // Pass 1: build frequency map
        for (char c : s.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }

        // Pass 2: find first entry with frequency == 1 (in insertion order)
        for (char c : freq.keySet()) {
            if (freq.get(c) == 1) {
                System.out.print("  → First non-repeating: ");
                return c;
            }
        }

        System.out.println("  → No non-repeating char found");
        return 0;
    }
}
