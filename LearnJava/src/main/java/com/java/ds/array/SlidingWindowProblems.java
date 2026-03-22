package com.java.ds.array;

import java.util.*;

/**
 * SLIDING WINDOW TECHNIQUE — Fixed and Variable Size Windows
 *
 * CONCEPT:
 *   Maintain a "window" [lo, hi] over the array and slide it right.
 *   Instead of recomputing the window from scratch on each step (O(n) per step = O(n²) total),
 *   we INCREMENTALLY update: add arr[hi] to the window, remove arr[lo] when needed.
 *   This achieves O(n) total.
 *
 * TWO FLAVORS:
 *   A) FIXED-SIZE window: move both lo and hi together (hi - lo = k always).
 *   B) VARIABLE-SIZE window: expand hi, shrink from lo when a constraint is violated.
 *      Classic pattern:
 *        for hi in 0..n-1:
 *          add arr[hi] to window
 *          while(window violates constraint): remove arr[lo], lo++
 *          update answer (window [lo..hi] is now valid)
 *
 * PROBLEMS COVERED:
 *   1. Max Sum Subarray of Size K (fixed window)
 *   2. Longest Substring Without Repeating Characters — LC 3 (variable window)
 *   3. Minimum Window Substring — LC 76 (variable window, hardest)
 *   4. Max Consecutive Ones III — LC 1004 (variable window with flip budget)
 *
 * INTERVIEW FREQUENCY: Very high — sliding window appears in ~25% of string/array problems.
 *
 * COMMON FOLLOW-UP:
 *   1. "Why not just check every substring?" → O(n²) or O(n³). Window is O(n).
 *   2. "What data structure do you use inside the window?" → HashMap for frequency, Deque for max.
 *   3. "How do you handle duplicates in the window?" → HashMap with count, remove when count reaches 0.
 */
public class SlidingWindowProblems {

    public static void main(String[] args) {

        System.out.println("=== Fixed Window: Max Sum Subarray of Size K ===");
        System.out.println(maxSumSubarrayK(new int[]{2, 1, 5, 1, 3, 2}, 3)); // Expected: 9 (5+1+3)
        System.out.println(maxSumSubarrayK(new int[]{2, 3, 4, 1, 5}, 2));    // Expected: 7 (3+4)
        System.out.println(maxSumSubarrayK(new int[]{1}, 1));                 // Expected: 1

        System.out.println("\n=== LC 3: Longest Substring Without Repeating Characters ===");
        System.out.println(lengthOfLongestSubstring("abcabcbb")); // Expected: 3 ("abc")
        System.out.println(lengthOfLongestSubstring("bbbbb"));    // Expected: 1 ("b")
        System.out.println(lengthOfLongestSubstring("pwwkew"));   // Expected: 3 ("wke")
        System.out.println(lengthOfLongestSubstring(""));         // Expected: 0
        System.out.println(lengthOfLongestSubstring("abcde"));    // Expected: 5

        System.out.println("\n=== LC 76: Minimum Window Substring ===");
        System.out.println(minWindow("ADOBECODEBANC", "ABC")); // Expected: "BANC"
        System.out.println(minWindow("a", "a"));               // Expected: "a"
        System.out.println(minWindow("a", "aa"));              // Expected: "" (impossible)
        System.out.println(minWindow("ADOBECODEBANC", "ABC")); // Expected: "BANC"

        System.out.println("\n=== LC 1004: Max Consecutive Ones III (with k flips) ===");
        System.out.println(longestOnes(new int[]{1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0}, 2)); // Expected: 6
        System.out.println(longestOnes(new int[]{0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1}, 3)); // Expected: 10
        System.out.println(longestOnes(new int[]{1, 1, 1}, 0));  // Expected: 3 (no flips needed)
        System.out.println(longestOnes(new int[]{0, 0, 0}, 0));  // Expected: 0
    }

    // =========================================================================
    // 1. Max Sum Subarray of Size K (Fixed Window)
    // =========================================================================

    /**
     * Finds the maximum sum of any contiguous subarray of exactly size k.
     *
     * SLIDING WINDOW (FIXED):
     *   Build initial window [0..k-1], then slide right:
     *   At each step, add the new right element and subtract the element
     *   falling off the left (index hi - k).
     *
     * BRUTE FORCE comparison: O(n×k) — recomputes each window sum from scratch.
     * SLIDING WINDOW: O(n) — each element is added once and removed once.
     *
     * Time: O(n)  Space: O(1)
     */
    public static int maxSumSubarrayK(int[] arr, int k) {
        if (arr.length < k) return -1;

        // Build the first window [0..k-1]
        int windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += arr[i];
        }

        int maxSum = windowSum;

        // Slide the window: add arr[hi], remove arr[hi-k]
        for (int hi = k; hi < arr.length; hi++) {
            windowSum += arr[hi];       // New element enters the window on the right
            windowSum -= arr[hi - k];   // Oldest element leaves the window on the left
            maxSum = Math.max(maxSum, windowSum);
        }

        return maxSum;
    }

    // =========================================================================
    // 2. Longest Substring Without Repeating Characters (LC 3)
    // =========================================================================

    /**
     * Returns the length of the longest substring with all unique characters.
     *
     * VARIABLE WINDOW with HashMap:
     *   Expand hi to the right, adding characters to the window.
     *   When a duplicate is detected (char already in the window),
     *   shrink from the left (advance lo past the previous occurrence of that char).
     *
     * FASTER APPROACH: Instead of lo++, jump lo to max(lo, prevIndex+1) directly.
     *   This skips multiple steps in one jump.
     *
     * Time: O(n)  Space: O(min(n, 128)) for the HashMap (bounded by charset size)
     */
    public static int lengthOfLongestSubstring(String s) {
        if (s == null || s.isEmpty()) return 0;

        // Map: character → its most recent index in the string
        Map<Character, Integer> lastSeen = new HashMap<>();
        int maxLen = 0;
        int lo = 0; // Left boundary of the current window

        for (int hi = 0; hi < s.length(); hi++) {
            char c = s.charAt(hi);

            if (lastSeen.containsKey(c) && lastSeen.get(c) >= lo) {
                // Duplicate detected WITHIN current window [lo..hi]
                // Jump lo past the previous occurrence — shrinks window to remove duplicate
                // We use max(lo, prevIdx+1) to avoid moving lo backwards
                lo = lastSeen.get(c) + 1;
            }

            lastSeen.put(c, hi); // Update the most recent index of this character
            maxLen = Math.max(maxLen, hi - lo + 1); // Window size = hi - lo + 1
        }

        return maxLen;
    }

    // =========================================================================
    // 3. Minimum Window Substring (LC 76) — Hardest Sliding Window
    // =========================================================================

    /**
     * Returns the shortest substring of s that contains all characters of t.
     * Characters in t may appear multiple times (frequency matters).
     *
     * ALGORITHM:
     *   1. Build freq map for t (how many of each char we need).
     *   2. Track "formed" = how many distinct chars from t are satisfied.
     *   3. Expand hi: add s[hi] to window. If its count matches t's requirement → formed++.
     *   4. While window contains all of t (formed == required):
     *      - Update answer if window is smaller.
     *      - Shrink from lo: if removing s[lo] breaks a requirement → formed--.
     *   5. Repeat until hi reaches end of s.
     *
     * TIME COMPLEXITY: O(|s| + |t|) — each character is added and removed at most once.
     * SPACE: O(|t|) for the frequency map.
     */
    public static String minWindow(String s, String t) {
        if (s.isEmpty() || t.isEmpty() || s.length() < t.length()) return "";

        // Build frequency map for t
        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) {
            need.put(c, need.getOrDefault(c, 0) + 1);
        }

        int required = need.size(); // Number of distinct chars in t that must be satisfied
        int formed = 0;             // How many distinct chars are currently satisfied in window

        // Window frequency map
        Map<Character, Integer> windowFreq = new HashMap<>();

        int lo = 0;
        int minLen = Integer.MAX_VALUE;
        int minStart = 0;

        for (int hi = 0; hi < s.length(); hi++) {
            char c = s.charAt(hi);
            windowFreq.put(c, windowFreq.getOrDefault(c, 0) + 1);

            // Check if this character's requirement is now satisfied
            if (need.containsKey(c) && windowFreq.get(c).equals(need.get(c))) {
                formed++; // We've met the frequency requirement for char c
            }

            // Try to shrink the window from the left while it's valid
            while (formed == required && lo <= hi) {
                // Record answer if this window is the smallest so far
                if (hi - lo + 1 < minLen) {
                    minLen = hi - lo + 1;
                    minStart = lo;
                }

                // Remove s[lo] from the window
                char leftChar = s.charAt(lo);
                windowFreq.put(leftChar, windowFreq.get(leftChar) - 1);

                if (need.containsKey(leftChar) && windowFreq.get(leftChar) < need.get(leftChar)) {
                    // Removing this char broke a requirement → window is no longer valid
                    formed--;
                }

                lo++; // Shrink window from the left
            }
        }

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }

    // =========================================================================
    // 4. Max Consecutive Ones III (LC 1004)
    // =========================================================================

    /**
     * Returns the max length of a subarray containing only 1s, after flipping at most k 0s.
     *
     * REFRAME: Find the longest subarray with at most k zeros.
     *   (Flipping k zeros to 1s is equivalent to allowing k zeros in the window.)
     *
     * VARIABLE WINDOW:
     *   Expand hi. Count zeros in window.
     *   When zeros exceed k → shrink from lo until zeros <= k again.
     *
     * Time: O(n)  Space: O(1)
     */
    public static int longestOnes(int[] nums, int k) {
        int lo = 0;
        int zeros = 0; // Count of zeros in the current window
        int maxLen = 0;

        for (int hi = 0; hi < nums.length; hi++) {
            if (nums[hi] == 0) zeros++; // New zero entered the window

            // Window has too many zeros → shrink from left
            while (zeros > k) {
                if (nums[lo] == 0) zeros--; // Left element was a zero → shrinking helps
                lo++;
            }

            // Window [lo..hi] has at most k zeros (all valid)
            maxLen = Math.max(maxLen, hi - lo + 1);
        }

        return maxLen;
    }
}
