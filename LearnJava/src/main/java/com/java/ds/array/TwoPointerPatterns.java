package com.java.ds.array;

import java.util.*;

/**
 * TWO-POINTER PATTERNS — Advanced Mental Model & All Variants
 *
 * CONCEPT:
 *   Two-pointer is a FAMILY of techniques, not a single trick.
 *   The unifying idea: use two indices to reduce an O(n²) or O(n³) search
 *   to O(n) by exploiting some structure (sorted order, monotonicity, or
 *   a "contracted" search space).
 *
 * THE 4 VARIANTS (each has different pointer movement rules):
 *   1. Opposite-ends → meet in middle  (sorted array, sum/container problems)
 *   2. Fast + slow (Floyd's cycle)      (linked list, cycle detection)
 *   3. Sliding window                   (substring/subarray with constraint)
 *   4. Read-write / partition pointer   (in-place array cleanup/partition)
 *
 * WHEN TO RECOGNIZE TWO-POINTER:
 *   - "Find pair / triplet with sum X" in sorted array  → opposite-ends
 *   - "Detect cycle / find middle" in linked list       → fast/slow
 *   - "Longest/shortest subarray with property"         → sliding window
 *   - "Remove / partition in-place"                     → read-write
 *
 * TIME COMPLEXITY:  O(n) — each pointer moves at most n steps total
 * SPACE COMPLEXITY: O(1) — only pointer variables (no extra data structure)
 *
 * INTERVIEW ANGLE:
 *   Interviewers use two-pointer to test whether you can avoid O(n²) brute
 *   force. Follow-ups often ask: "Can you do it in O(1) space?" — two-pointer
 *   IS that answer. Know why HashMap/HashSet alternatives use O(n) space.
 *
 * REAL-WORLD USE:
 *   - Java's Arrays.binarySearch uses a form of pointer convergence
 *   - Stream merging in merge-sort uses read-pointer per sorted run
 *   - Cycle detection in Java's java.util.concurrent (thread scheduling)
 */
public class TwoPointerPatterns {

    // =========================================================================
    // VARIANT 1: OPPOSITE-ENDS — MEET IN MIDDLE
    // =========================================================================
    //
    // PATTERN:  lo = 0, hi = n-1; while (lo < hi) { check; move one pointer }
    // REQUIRES: array is SORTED (or problem has monotonic structure)
    // INVARIANT: every element outside [lo, hi] has been proven impossible
    // WHY IT WORKS: sorted order lets us steer — if sum too small → move lo right
    //               (only way to get larger), if sum too big → move hi left
    //
    // KEY GOTCHA: skip duplicates EXPLICITLY when problem asks for unique results
    //   after collecting a valid pair/triplet, do:
    //   while (lo < hi && nums[lo] == nums[lo+1]) lo++;
    //   while (lo < hi && nums[hi] == nums[hi-1]) hi--;
    //   THEN advance: lo++; hi--;
    //   Missing this causes duplicate triplets in Three Sum — a classic mistake.

    /**
     * LC 167 — Two Sum II (sorted array)
     *
     * KEY INSIGHT: sorted order guarantees that moving lo right always increases
     * the sum, moving hi left always decreases it. We steer toward target.
     *
     * DIFFERENCE FROM LC 1: LC 1 uses HashMap O(n) space; this is O(1) space
     * because the array is sorted — interviewers love asking "which is better?"
     * Answer: depends on constraints — sorted input → two-pointer wins on space.
     *
     * Time: O(n)  Space: O(1)
     */
    public int[] twoSumII(int[] numbers, int target) {
        int lo = 0, hi = numbers.length - 1;
        while (lo < hi) {
            int sum = numbers[lo] + numbers[hi];
            if (sum == target) return new int[]{lo + 1, hi + 1}; // 1-indexed
            else if (sum < target) lo++;   // sum too small → need larger left value
            else hi--;                      // sum too big  → need smaller right value
        }
        return new int[]{-1, -1}; // guaranteed to find by problem statement
    }

    /**
     * LC 15 — Three Sum (find all unique triplets summing to 0)
     *
     * KEY INSIGHT: fix one element (outer loop), then two-pointer on remainder.
     * Reduces O(n³) brute to O(n²). Sort first to enable two-pointer + dedup.
     *
     * GOTCHA: duplicate triplets if you don't skip equal elements after fixing
     * and after collecting. This is the #1 mistake in interviews.
     *
     * Time: O(n²)  Space: O(1) extra (output list not counted)
     */
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);                          // sort enables two-pointer + dedup
        List<List<Integer>> result = new ArrayList<>();

        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue; // skip duplicate fixed element

            int lo = i + 1, hi = nums.length - 1;
            while (lo < hi) {
                int sum = nums[i] + nums[lo] + nums[hi];
                if (sum == 0) {
                    result.add(Arrays.asList(nums[i], nums[lo], nums[hi]));
                    // skip duplicates on both sides before advancing
                    while (lo < hi && nums[lo] == nums[lo + 1]) lo++;
                    while (lo < hi && nums[hi] == nums[hi - 1]) hi--;
                    lo++; hi--;                     // move both since pair is "used"
                } else if (sum < 0) lo++;
                else hi--;
            }
        }
        return result;
    }
    // Time:  O(n²) — outer loop O(n), inner two-pointer O(n)
    // Space: O(1) extra

    // Q: Why sort first?
    // A: (1) Enables two-pointer monotonicity. (2) Makes dedup O(1) — just check
    //    adjacent equals. Without sorting, dedup needs a HashSet O(n) space.

    /**
     * LC 42 — Trapping Rain Water
     *
     * KEY INSIGHT: water at position i = min(maxLeft, maxRight) - height[i].
     * Two-pointer avoids precomputing the entire maxLeft/maxRight arrays.
     * We process from the side with the SMALLER max height — that side's water
     * is fully determined (bounded by the smaller side, not the taller unknown).
     *
     * WHY WE MOVE THE SMALLER SIDE:
     *   If maxLeft < maxRight, water at lo = maxLeft - height[lo] (guaranteed
     *   because even if right wall grows taller, the constraint is maxLeft).
     *   The right side has an even taller wall, so maxLeft is the bottleneck.
     *
     * Time: O(n)  Space: O(1)
     */
    public int trap(int[] height) {
        int lo = 0, hi = height.length - 1;
        int maxLeft = 0, maxRight = 0, water = 0;

        while (lo < hi) {
            if (height[lo] < height[hi]) {
                // left side is the bottleneck → process it
                if (height[lo] >= maxLeft) maxLeft = height[lo]; // new max, no water here
                else water += maxLeft - height[lo];              // trapped water
                lo++;
            } else {
                // right side is the bottleneck → process it
                if (height[hi] >= maxRight) maxRight = height[hi];
                else water += maxRight - height[hi];
                hi--;
            }
        }
        return water;
    }
    // Time:  O(n)
    // Space: O(1) — classic two-pointer instead of O(n) prefix max arrays

    // INTERVIEW FOLLOW-UP: "Can you explain the O(n) space solution first?"
    // → Precompute maxLeft[] and maxRight[], then ans[i] = min(L,R) - height[i]
    // → Then explain how two-pointer eliminates the arrays → better space

    /**
     * LC 11 — Container With Most Water
     *
     * KEY INSIGHT: area = min(height[lo], height[hi]) * (hi - lo).
     * We always move the SHORTER line inward — moving the taller line can only
     * decrease width while height is already limited by the shorter line.
     * So moving the shorter line is our only chance to find a larger container.
     *
     * Time: O(n)  Space: O(1)
     */
    public int maxArea(int[] height) {
        int lo = 0, hi = height.length - 1, maxWater = 0;
        while (lo < hi) {
            int area = Math.min(height[lo], height[hi]) * (hi - lo);
            maxWater = Math.max(maxWater, area);
            if (height[lo] < height[hi]) lo++;  // shorter left wall → move it right
            else hi--;                           // shorter (or equal) right wall → move left
        }
        return maxWater;
    }

    // =========================================================================
    // VARIANT 2: FAST + SLOW POINTERS (FLOYD'S CYCLE DETECTION)
    // =========================================================================
    //
    // PATTERN:  slow moves 1 step, fast moves 2 steps per iteration
    // USE CASE: detect cycles, find midpoints, detect duplicates in value-range arrays
    //
    // WHY IT WORKS (cycle detection):
    //   If there's a cycle of length L, fast catches up to slow at rate 1 step/iter.
    //   They MUST meet inside the cycle (slow can't escape once in the cycle,
    //   fast is always behind by fewer steps each iteration).
    //
    // FINDING CYCLE START:
    //   After meeting: reset one pointer to head, advance both 1 step at a time.
    //   They meet at the cycle start. (Mathematical proof: distance from head to
    //   cycle start = distance from meeting point to cycle start through the cycle)
    //
    // GOTCHA: always check fast != null && fast.next != null before moving fast
    //         to avoid NullPointerException on acyclic lists

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int v) { val = v; }
    }

    /**
     * LC 141 — Linked List Cycle Detection
     *
     * Time: O(n)  Space: O(1)
     * Alternative: HashSet of visited nodes — O(n) space, O(n) time
     * Two-pointer wins on space.
     */
    public boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {  // fast.next prevents NPE
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) return true;  // pointers meet → cycle exists
        }
        return false;  // fast hit null → list is acyclic
    }

    /**
     * LC 142 — Find Cycle Start Node
     *
     * MATH INSIGHT: Let F = dist(head → cycle start), C = cycle length,
     * k = steps into cycle where slow and fast meet.
     * slow traveled: F + k steps
     * fast traveled: F + k + n*C steps (n full loops ahead)
     * fast = 2 * slow → F + k + n*C = 2(F + k) → F = n*C - k
     * F = n*C - k means distance from head to cycle start =
     * distance from meeting point to cycle start (going forward in cycle).
     * So resetting one pointer to head and walking both at speed 1 finds the start.
     *
     * Time: O(n)  Space: O(1)
     */
    public ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                // cycle detected — find entry point
                ListNode ptr = head;
                while (ptr != slow) {   // both advance 1 step — meet at cycle start
                    ptr = ptr.next;
                    slow = slow.next;
                }
                return ptr;
            }
        }
        return null;
    }

    /**
     * LC 876 — Middle of Linked List
     *
     * KEY INSIGHT: when fast reaches end, slow is at middle.
     * For even length: slow ends at second middle (return first? adjust condition).
     *
     * Time: O(n)  Space: O(1)
     */
    public ListNode middleNode(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow; // for even length n: slow is at n/2+1 (second middle)
    }

    /**
     * LC 202 — Happy Number
     *
     * KEY INSIGHT: sequences either reach 1 (happy) or enter a CYCLE.
     * Treat the "next number" function as a linked list's .next pointer.
     * Fast/slow on this abstract linked list detects the cycle.
     *
     * Alternatively: know the cycle contains 4 (if not happy, always hits 4).
     * But Floyd's works without memorizing that fact.
     *
     * Time: O(log n)  Space: O(1)
     */
    public boolean isHappy(int n) {
        int slow = n, fast = sumOfSquares(n);
        while (fast != 1 && slow != fast) {
            slow = sumOfSquares(slow);
            fast = sumOfSquares(sumOfSquares(fast));
        }
        return fast == 1;
    }

    private int sumOfSquares(int n) {
        int sum = 0;
        while (n > 0) {
            int d = n % 10;
            sum += d * d;
            n /= 10;
        }
        return sum;
    }

    // =========================================================================
    // VARIANT 3: SLIDING WINDOW
    // =========================================================================
    //
    // PATTERN:  lo and hi both start at 0 and only move RIGHT
    //           hi expands the window; lo shrinks it when a constraint is violated
    //           → window [lo, hi] always satisfies the constraint
    //
    // DIFFERENCE FROM OPPOSITE-ENDS:
    //   - Opposite-ends: both pointers start at opposite sides, array must be sorted
    //   - Sliding window: both start left, array need NOT be sorted,
    //     but constraint must be "monotonic" (adding element makes it worse,
    //     removing makes it better — or vice versa)
    //
    // FIXED vs VARIABLE window:
    //   Fixed: hi - lo + 1 == k, slide both together
    //   Variable: expand hi until constraint violated, shrink lo to restore it
    //
    // COMPLEXITY: O(n) — lo and hi each move at most n steps (not O(n²))
    //
    // GOTCHA: sliding window does NOT work if adding one element can both satisfy
    //         AND violate the constraint (non-monotonic) — then you need other approaches

    /**
     * LC 3 — Longest Substring Without Repeating Characters
     *
     * KEY INSIGHT: maintain a window [lo, hi] with no duplicates.
     * When hi introduces a duplicate, advance lo past the previous occurrence.
     * HashMap stores char → most recent index for O(1) jump.
     *
     * Time: O(n)  Space: O(min(n, charset)) — at most 128 for ASCII
     */
    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int lo = 0, maxLen = 0;

        for (int hi = 0; hi < s.length(); hi++) {
            char c = s.charAt(hi);
            // if c was seen inside [lo, hi), jump lo past it to remove duplicate
            if (lastSeen.containsKey(c) && lastSeen.get(c) >= lo) {
                lo = lastSeen.get(c) + 1; // jump lo to one past last occurrence
            }
            lastSeen.put(c, hi);
            maxLen = Math.max(maxLen, hi - lo + 1);
        }
        return maxLen;
    }
    // GOTCHA: check lastSeen.get(c) >= lo — the char might be in the map but
    //         BEFORE lo (we already moved past it). Don't shrink lo backwards!

    /**
     * LC 76 — Minimum Window Substring
     *
     * KEY INSIGHT: expand hi to include all required chars, then shrink lo
     * as much as possible while still covering all requirements. Track a
     * "formed" count of chars meeting their required frequency.
     *
     * This is the HARDEST sliding window problem — master this pattern.
     *
     * Time: O(|s| + |t|)  Space: O(|t|) for frequency maps
     */
    public String minWindow(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) return "";

        // build required frequency map
        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

        int required = need.size(); // number of UNIQUE chars to satisfy
        int formed = 0;             // unique chars currently meeting frequency requirement
        Map<Character, Integer> window = new HashMap<>();

        int lo = 0, minLen = Integer.MAX_VALUE, minLo = 0;

        for (int hi = 0; hi < s.length(); hi++) {
            char c = s.charAt(hi);
            window.merge(c, 1, Integer::sum);

            // check if this char now meets its required count
            if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) {
                formed++;
            }

            // shrink window from left while all requirements met
            while (formed == required) {
                if (hi - lo + 1 < minLen) {
                    minLen = hi - lo + 1;
                    minLo = lo;
                }
                char leftChar = s.charAt(lo);
                window.merge(leftChar, -1, Integer::sum);
                if (need.containsKey(leftChar) && window.get(leftChar) < need.get(leftChar)) {
                    formed--; // removing leftChar broke a requirement
                }
                lo++;
            }
        }
        return minLen == Integer.MAX_VALUE ? "" : s.substring(minLo, minLo + minLen);
    }
    // INTERVIEW FOLLOW-UP: "Can you do O(|filtered_s|) instead of O(|s|)?"
    // → Yes: pre-filter s to only chars in t, store (char, original_index) pairs
    // → Apply window on filtered array — helps when |t| << |s|

    /**
     * LC 904 — Fruit Into Baskets (at most 2 distinct fruit types)
     *
     * GENERALIZATION: "longest subarray with at most K distinct elements"
     * (here K=2). Same sliding window, track count of each type.
     *
     * Time: O(n)  Space: O(K) = O(1) for K=2
     */
    public int totalFruit(int[] fruits) {
        Map<Integer, Integer> basket = new HashMap<>(); // type → count in window
        int lo = 0, maxLen = 0;

        for (int hi = 0; hi < fruits.length; hi++) {
            basket.merge(fruits[hi], 1, Integer::sum); // add fruit at hi

            // more than 2 types → shrink from left
            while (basket.size() > 2) {
                int leftFruit = fruits[lo];
                basket.merge(leftFruit, -1, Integer::sum);
                if (basket.get(leftFruit) == 0) basket.remove(leftFruit);
                lo++;
            }
            maxLen = Math.max(maxLen, hi - lo + 1);
        }
        return maxLen;
    }

    // =========================================================================
    // VARIANT 4: READ-WRITE / PARTITION POINTER
    // =========================================================================
    //
    // PATTERN:  read pointer (hi) scans all elements
    //           write pointer (lo) marks next valid write position
    //           → hi advances every iteration; lo only advances when writing
    //
    // USE CASE: in-place cleanup of arrays (remove elements, compress)
    //           or partition arrays (quick sort, Dutch National Flag)
    //
    // INVARIANT: arr[0..lo-1] is the "cleaned" or "partitioned" result so far
    //            arr[lo..hi-1] is not yet decided
    //
    // WHY IT WORKS: lo and hi separate "good" from "unprocessed" — no element
    //               is visited more than once, so O(n)

    /**
     * LC 26 — Remove Duplicates from Sorted Array
     *
     * KEY INSIGHT: write pointer lo tracks next position to write the next
     * unique element. Read pointer hi explores. Write only when new unique found.
     *
     * Time: O(n)  Space: O(1)
     */
    public int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;
        int lo = 1; // write position (index 0 is always kept)
        for (int hi = 1; hi < nums.length; hi++) {
            if (nums[hi] != nums[hi - 1]) { // new unique element found
                nums[lo++] = nums[hi];        // write it, advance write pointer
            }
        }
        return lo; // length of deduplicated prefix
    }

    /**
     * LC 283 — Move Zeroes
     *
     * KEY INSIGHT: read-write pointer moves all non-zeros to the front (lo),
     * then fills remainder with zeros. No need to track what's being overwritten
     * (zeros are discarded, non-zeros are moved forward).
     *
     * Time: O(n)  Space: O(1)
     */
    public void moveZeroes(int[] nums) {
        int lo = 0; // write position for next non-zero
        for (int hi = 0; hi < nums.length; hi++) {
            if (nums[hi] != 0) {
                nums[lo++] = nums[hi]; // write non-zero, advance lo
            }
        }
        // fill rest with zeros
        while (lo < nums.length) nums[lo++] = 0;
    }
    // VARIANT: swap instead of fill-after (preserves relative order of zeros too)
    // for (int hi = 0; hi < nums.length; hi++)
    //   if (nums[hi] != 0) { int tmp = nums[lo]; nums[lo++] = nums[hi]; nums[hi] = tmp; }

    /**
     * LC 27 — Remove Element
     *
     * Simple read-write: copy non-target elements to the front.
     *
     * Time: O(n)  Space: O(1)
     */
    public int removeElement(int[] nums, int val) {
        int lo = 0;
        for (int hi = 0; hi < nums.length; hi++) {
            if (nums[hi] != val) nums[lo++] = nums[hi];
        }
        return lo;
    }

    // =========================================================================
    // PATTERN RECOGNITION GUIDE
    // =========================================================================
    //
    // Signal                                → Use This Variant
    // ─────────────────────────────────────────────────────────────────────
    // "Sorted array" + "pair/triplet sum"   → Opposite-ends
    // "Container/water/area" in sorted      → Opposite-ends
    // "Cycle" or "middle" in linked list    → Fast + slow
    // "Longest/shortest substring with..."  → Sliding window (variable)
    // "Subarray of size K with..."          → Sliding window (fixed)
    // "Remove in-place" or "partition"      → Read-write pointer
    //
    // Q: How do I tell sliding window from two-sum two-pointer?
    // A: Sliding window: both pointers start at 0, move RIGHT only,
    //    array need not be sorted. Opposite-ends: start at opposite ends,
    //    converge, requires sorted order.
    //
    // Q: When does sliding window NOT work?
    // A: When adding an element can simultaneously satisfy AND violate the
    //    constraint (non-monotonic constraint). Example: "subarray with sum
    //    exactly K" — adding an element can't be "undone" by just shrinking lo
    //    if there are negative numbers. Use prefix sums + HashMap instead.

    public static void main(String[] args) {
        TwoPointerPatterns tp = new TwoPointerPatterns();

        // --- Opposite-ends ---
        System.out.println("=== Opposite-Ends ===");
        System.out.println(Arrays.toString(tp.twoSumII(new int[]{2,7,11,15}, 9)));  // [1,2]
        System.out.println(tp.threeSum(new int[]{-1,0,1,2,-1,-4}));               // [[-1,-1,2],[-1,0,1]]
        System.out.println(tp.trap(new int[]{0,1,0,2,1,0,1,3,2,1,2,1}));          // 6
        System.out.println(tp.maxArea(new int[]{1,8,6,2,5,4,8,3,7}));             // 49

        // --- Fast + Slow ---
        System.out.println("\n=== Fast + Slow ===");
        System.out.println(tp.isHappy(19));  // true
        System.out.println(tp.isHappy(2));   // false

        // --- Sliding Window ---
        System.out.println("\n=== Sliding Window ===");
        System.out.println(tp.lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println(tp.minWindow("ADOBECODEBANC", "ABC"));    // "BANC"
        System.out.println(tp.totalFruit(new int[]{1,2,1,2,3}));     // 4

        // --- Read-Write ---
        System.out.println("\n=== Read-Write Pointer ===");
        int[] arr = {0,1,0,3,12};
        tp.moveZeroes(arr);
        System.out.println(Arrays.toString(arr)); // [1,3,12,0,0]
    }
}
