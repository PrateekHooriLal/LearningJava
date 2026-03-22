package com.java.ds.array;

import java.util.*;

/**
 * SWAPPING TECHNIQUES — In-Place Array Manipulation Patterns
 *
 * CONCEPT:
 *   Swapping is a family of in-place techniques that rearrange array elements
 *   without extra memory. The simple "swap two elements" operation unlocks
 *   5 distinct algorithmic patterns used across sorting, searching, and cleanup.
 *
 * THE 5 SWAP PATTERNS:
 *   1. Cycle Sort         — place each value at its "home" index in one pass
 *   2. Dutch National Flag— 3-way partition via swap (Lo/Mid/Hi pointers)
 *   3. XOR Swap           — bitwise trick to swap without temp variable
 *   4. Reversal via Swap  — in-place reversal = foundation for array rotation
 *   5. Selection-style    — find min/max, swap to its final position (QuickSelect)
 *
 * WHEN TO RECOGNIZE:
 *   - Values in range [1..n] or [0..n-1] + "find missing/duplicate"  → Cycle Sort
 *   - "Sort 0s, 1s, 2s" or "partition by condition"                   → DNF
 *   - "In-place reverse" or "rotate by k"                             → Reversal
 *   - "Kth largest/smallest in O(n) avg"                              → QuickSelect
 *
 * TIME COMPLEXITY:  O(n) for Cycle Sort and DNF; O(n) avg for QuickSelect
 * SPACE COMPLEXITY: O(1) for all patterns — the defining property
 *
 * INTERVIEW ANGLE:
 *   These patterns appear when interviewers demand O(1) extra space.
 *   Cycle sort is famous for minimizing WRITES (important for flash storage).
 *   QuickSelect is the O(n) alternative to sorting for "Kth" problems.
 *
 * REAL-WORLD USE:
 *   - Java's Arrays.sort uses dual-pivot Quicksort (pivot + swap)
 *   - JVM GC compaction: cycle-sort-like element movement
 *   - In-place string reversal in StringBuilder
 */
public class SwappingTechniques {

    // helper: standard swap
    private void swap(int[] a, int i, int j) {
        int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
    }

    // =========================================================================
    // PATTERN 1: CYCLE SORT
    // =========================================================================
    //
    // KEY INSIGHT: if values are in range [1..n], element with value v belongs
    // at index v-1. Cycle sort places each element at its correct index in
    // exactly one pass using swaps.
    //
    // ALGORITHM:
    //   for i in 0..n-1:
    //     while nums[i] != nums[nums[i]-1]:   // element not at its home
    //       swap(nums, i, nums[i]-1)           // send it home (or bring home to i)
    //   after this pass: nums[i] == i+1 for correct positions; else nums[i] == i+1 missed
    //
    // PRECONDITION: values must be in range [1..n] (or [0..n-1] with index offset)
    //   If this precondition is missing, cycle sort CANNOT be applied.
    //   Always check: "Are values bounded by array length?"
    //
    // WHY FEWEST WRITES: each element is swapped AT MOST once to its final position.
    //   Total swaps ≤ n. Compare: bubble sort does O(n²) swaps.
    //
    // GOTCHA: the condition is nums[i] != nums[nums[i]-1] NOT nums[i] != i+1.
    //   With duplicates: nums[i] == nums[nums[i]-1] means home is already occupied
    //   by a duplicate — advance i to avoid infinite loop.

    /**
     * LC 268 — Missing Number (range [0..n], one missing)
     *
     * CYCLE SORT APPROACH: place values in range [0..n-1] at index i=val.
     * After sorting, first index where nums[i] != i is the missing value.
     * If all indices match, missing value is n.
     *
     * ALTERNATIVE (simpler): expected sum = n*(n+1)/2, subtract actual sum.
     * Cycle sort approach generalizes to "find ALL missing numbers" (LC 448).
     *
     * Time: O(n)  Space: O(1)
     */
    public int missingNumber(int[] nums) {
        int i = 0;
        while (i < nums.length) {
            int correctIdx = nums[i]; // value nums[i] belongs at index nums[i]
            // only place if within bounds [0..n-1] and not already there
            if (nums[i] < nums.length && nums[i] != nums[correctIdx]) {
                swap(nums, i, correctIdx);
            } else {
                i++; // element at home or out of range → advance
            }
        }
        // find the first index where value doesn't match
        for (int j = 0; j < nums.length; j++) {
            if (nums[j] != j) return j;
        }
        return nums.length; // missing number is n
    }

    /**
     * LC 442 — Find All Duplicates in Array (values in [1..n], each appears 1 or 2 times)
     *
     * After cycle sort, any index where nums[i] != i+1 has a DUPLICATE.
     * The value nums[i] appeared twice; i+1 appeared zero times.
     *
     * Time: O(n)  Space: O(1) (output list not counted)
     */
    public List<Integer> findAllDuplicates(int[] nums) {
        int i = 0;
        while (i < nums.length) {
            int correctIdx = nums[i] - 1; // value v belongs at index v-1
            if (nums[i] != nums[correctIdx]) { // home not already holding this value
                swap(nums, i, correctIdx);
            } else {
                i++;
            }
        }
        List<Integer> duplicates = new ArrayList<>();
        for (int j = 0; j < nums.length; j++) {
            if (nums[j] != j + 1) duplicates.add(nums[j]); // nums[j] is the duplicate
        }
        return duplicates;
    }

    /**
     * LC 287 — Find the Duplicate Number
     *
     * Constraint: n+1 elements in [1..n] → exactly one value is duplicated.
     * After cycle sort, the index where nums[i] != i+1 holds the duplicate value.
     *
     * NOTE: Floyd's cycle detection is the O(1) space O(n) alternative that
     * does NOT modify the array. Use that if input modification is disallowed.
     *
     * Time: O(n)  Space: O(1) — modifies input array
     */
    public int findDuplicate(int[] nums) {
        int i = 0;
        while (i < nums.length) {
            if (nums[i] == i + 1) { i++; continue; } // already home
            int correctIdx = nums[i] - 1;
            if (nums[i] == nums[correctIdx]) return nums[i]; // duplicate found
            swap(nums, i, correctIdx);
        }
        return -1; // guaranteed to find by problem constraints
    }
    // INTERVIEW GOTCHA: "What if you can't modify the array?"
    // → Use Floyd's cycle detection (treat array as implicit linked list where
    //   node i points to nums[i]). The duplicate creates the cycle entry point.

    /**
     * LC 41 — First Missing Positive
     *
     * Range [1..n] in array of length n. Ignore values outside [1..n].
     * After cycle sort, first index with nums[i] != i+1 → missing positive is i+1.
     *
     * Time: O(n)  Space: O(1)
     */
    public int firstMissingPositive(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correctIdx = nums[i] - 1;
            // only place if value in [1..n] and not already at home
            if (nums[i] > 0 && nums[i] <= n && nums[i] != nums[correctIdx]) {
                swap(nums, i, correctIdx);
            } else {
                i++;
            }
        }
        for (int j = 0; j < n; j++) {
            if (nums[j] != j + 1) return j + 1;
        }
        return n + 1; // 1..n all present → answer is n+1
    }

    // =========================================================================
    // PATTERN 2: DUTCH NATIONAL FLAG (3-WAY PARTITION)
    // =========================================================================
    //
    // KEY INSIGHT: maintain 3 regions using 3 pointers:
    //   [0..lo-1]   = all 0s (region A)
    //   [lo..mid-1] = all 1s (region B)
    //   [mid..hi]   = unknown (region C — to be processed)
    //   [hi+1..n-1] = all 2s (region D)
    //
    // ALGORITHM:
    //   mid starts at 0, hi starts at n-1.
    //   while mid <= hi:
    //     nums[mid]==0: swap(lo, mid), lo++, mid++  // 0 goes to region A
    //     nums[mid]==1: mid++                        // 1 already in region B
    //     nums[mid]==2: swap(mid, hi), hi--          // 2 goes to region D
    //                                                // DON'T increment mid — new element at mid unexamined
    //
    // GOTCHA: when swapping mid with hi, DO NOT advance mid (the swapped element
    //   from hi is unknown and must be re-examined). When swapping mid with lo,
    //   DO advance both (element from lo was already processed — it's a 1).

    /**
     * LC 75 — Sort Colors (sort array of 0s, 1s, 2s in one pass)
     *
     * Named "Dutch National Flag" after the Dutch flag's 3-color bands.
     * Invented by Dijkstra. This is the canonical O(n) single-pass solution.
     *
     * Time: O(n)  Space: O(1)
     */
    public void sortColors(int[] nums) {
        int lo = 0, mid = 0, hi = nums.length - 1;

        while (mid <= hi) {
            if (nums[mid] == 0) {
                swap(nums, lo, mid);
                lo++; mid++;          // both advance: lo received a 0; mid received a 1
            } else if (nums[mid] == 1) {
                mid++;                // 1 is already in the right region
            } else {                 // nums[mid] == 2
                swap(nums, mid, hi);
                hi--;                 // advance hi; DON'T advance mid (re-examine)
            }
        }
    }
    // Q: Why not counting sort (count 0s, 1s, 2s, then fill)?
    // A: Counting sort requires 2 passes. DNF is 1 pass. Also, DNF works when
    //    you can't enumerate all values (generalized to any 3-way partition condition).

    /**
     * LC 905 — Sort Array By Parity (even elements first, then odd)
     *
     * 2-way partition (simpler than DNF — only 2 groups).
     * Same read-write pointer can work but two-pointer is cleaner here.
     *
     * Time: O(n)  Space: O(1)
     */
    public int[] sortArrayByParity(int[] nums) {
        int lo = 0, hi = nums.length - 1;
        while (lo < hi) {
            if (nums[lo] % 2 == 0) {
                lo++; // even at lo → already in right place
            } else if (nums[hi] % 2 == 1) {
                hi--; // odd at hi → already in right place
            } else {
                swap(nums, lo++, hi--); // lo is odd, hi is even → swap them
            }
        }
        return nums;
    }

    // =========================================================================
    // PATTERN 3: XOR SWAP
    // =========================================================================
    //
    // KEY INSIGHT: for integers, XOR has self-inverse property: a ^ b ^ b = a.
    // XOR swap eliminates the need for a temp variable:
    //   a ^= b;   // a = a XOR b
    //   b ^= a;   // b = b XOR (a XOR b) = original a
    //   a ^= b;   // a = (a XOR b) XOR original_a = original b
    //
    // WHEN USEFUL: embedded systems / microcontrollers with no spare registers.
    //              Hot inner loops where temp variable causes register spill.
    //
    // CRITICAL GOTCHA: XOR swap FAILS if a and b reference the SAME memory location.
    //   xorSwap(arr, i, i) → arr[i] becomes 0 because a ^= a = 0, then rest zeros.
    //   Always guard: if (i == j) return;
    //
    // IN PRACTICE: modern compilers optimize standard temp-swap to the same speed.
    //   XOR swap is a INTERVIEW TRIVIA / SYSTEMS knowledge question, not a daily tool.

    public void xorSwap(int[] a, int i, int j) {
        if (i == j) return; // CRITICAL: same location → XOR self = 0 = data loss
        a[i] ^= a[j]; // a[i] = a[i] XOR a[j]
        a[j] ^= a[i]; // a[j] = a[j] XOR (a[i] XOR a[j]) = original a[i]
        a[i] ^= a[j]; // a[i] = (a[i] XOR a[j]) XOR original_a[i] = original a[j]
    }
    // Q: When would an interviewer ask about XOR swap?
    // A: Systems design interviews (C/C++ embedded), or when asking about
    //    bit manipulation. In Java interviews, mention it as trivia and note
    //    that JIT compilation makes it unnecessary for performance.

    // =========================================================================
    // PATTERN 4: REVERSAL VIA SWAP (IN-PLACE REVERSAL = ARRAY ROTATION)
    // =========================================================================
    //
    // KEY INSIGHT: any rotation can be decomposed into 3 reversals.
    //   Rotate right by k: reverse ALL, reverse [0..k-1], reverse [k..n-1]
    //   This is the "three-reversal trick" — memorize it for LC 189.
    //
    // WHY IT WORKS:
    //   Original:  [1,2,3,4,5,6,7], k=3
    //   Reverse all: [7,6,5,4,3,2,1]
    //   Reverse [0..2]: [5,6,7,4,3,2,1]
    //   Reverse [3..6]: [5,6,7,1,2,3,4] ✓ rotated right by 3
    //
    // GOTCHA: k = k % n first! If k == n, rotation is a no-op.
    //   Interviewers sometimes include k > n as a test case.

    private void reverse(int[] nums, int lo, int hi) {
        while (lo < hi) swap(nums, lo++, hi--); // standard in-place reverse
    }

    /**
     * LC 189 — Rotate Array (rotate right by k steps)
     *
     * Three-reversal trick: O(n) time, O(1) space.
     * Common alternative: use extra array O(n) space — interviewers want O(1).
     *
     * Time: O(n)  Space: O(1)
     */
    public void rotate(int[] nums, int k) {
        int n = nums.length;
        k = k % n;               // normalize: k=7 on n=7 is k=0 (no-op)
        if (k == 0) return;
        reverse(nums, 0, n - 1); // step 1: reverse entire array
        reverse(nums, 0, k - 1); // step 2: reverse first k elements
        reverse(nums, k, n - 1); // step 3: reverse remaining n-k elements
    }

    /**
     * LC 344 — Reverse String (in-place character reversal)
     *
     * Simplest application of reversal via swap.
     * Time: O(n)  Space: O(1)
     */
    public void reverseString(char[] s) {
        int lo = 0, hi = s.length - 1;
        while (lo < hi) {
            char tmp = s[lo]; s[lo] = s[hi]; s[hi] = tmp;
            lo++; hi--;
        }
    }

    /**
     * LC 151 — Reverse Words in a String
     *
     * TRICK: reverse entire string, then reverse each word individually.
     * Works on char array; Java strings are immutable so use StringBuilder.
     *
     * Time: O(n)  Space: O(n) for StringBuilder (unavoidable with immutable strings)
     */
    public String reverseWords(String s) {
        String[] words = s.trim().split("\\s+"); // split on any whitespace
        int lo = 0, hi = words.length - 1;
        while (lo < hi) {
            String tmp = words[lo]; words[lo] = words[hi]; words[hi] = tmp;
            lo++; hi--;
        }
        return String.join(" ", words);
    }
    // FOLLOW-UP: "Do it on a char array in O(1) space?"
    // → Reverse entire array char by char, then find each word and reverse it

    // =========================================================================
    // PATTERN 5: SELECTION-STYLE SWAP (QUICKSELECT)
    // =========================================================================
    //
    // KEY INSIGHT: after one partition step of QuickSort, the PIVOT element
    // lands at its FINAL sorted position. If pivot position == k, we found
    // the Kth smallest. Only recurse into ONE side → O(n) average.
    //
    // QUICKSELECT vs SORTING:
    //   Sort first then index → O(n log n)
    //   QuickSelect → O(n) average, O(n²) worst case (bad pivot)
    //   For guaranteed O(n): Median-of-Medians algorithm (complex, rarely asked)
    //
    // RANDOMIZATION: shuffle or pick random pivot to avoid O(n²) worst case.
    //   Arrays.sort uses dual-pivot Quicksort for primitives (not randomized —
    //   DoS attack vector on competitive programming with adversarial input).
    //
    // LOMUTO PARTITION SCHEME:
    //   pivot = arr[hi]; lo = start - 1
    //   for i in start..hi-1: if arr[i] <= pivot: swap(++lo, i)
    //   swap(lo+1, hi); return lo+1
    //   → pivot ends up at index lo+1 in its final sorted position

    /**
     * LC 215 — Kth Largest Element in Array
     *
     * QuickSelect finds Kth LARGEST = find element at index (n-k) in sorted order.
     * Partition around a pivot; recurse only into the relevant half.
     *
     * Time: O(n) average, O(n²) worst  Space: O(1) (in-place, ignoring call stack)
     */
    public int findKthLargest(int[] nums, int k) {
        int target = nums.length - k; // Kth largest = (n-k)th smallest (0-indexed)
        return quickSelect(nums, 0, nums.length - 1, target);
    }

    private int quickSelect(int[] nums, int lo, int hi, int target) {
        if (lo == hi) return nums[lo];

        // randomize pivot to avoid O(n²) worst case on sorted inputs
        int randIdx = lo + (int)(Math.random() * (hi - lo + 1));
        swap(nums, randIdx, hi); // move pivot to end (Lomuto scheme)

        int pivot = nums[hi];
        int p = lo - 1; // p = "wall" — everything ≤ pivot is left of wall

        for (int i = lo; i < hi; i++) {
            if (nums[i] <= pivot) {
                p++;
                swap(nums, p, i); // move element ≤ pivot to left of wall
            }
        }
        p++;               // p is now pivot's final sorted index
        swap(nums, p, hi); // place pivot at its final position

        if (p == target) return nums[p];           // pivot IS the answer
        else if (p < target) return quickSelect(nums, p + 1, hi, target);  // answer is right
        else return quickSelect(nums, lo, p - 1, target);                   // answer is left
    }
    // Q: Why O(n) average?
    // A: Each partition step reduces search space by ~half on average.
    //    Work per level: n + n/2 + n/4 + ... = 2n = O(n).
    //    Worst case (always bad pivot): n + (n-1) + ... = O(n²).
    //    Random pivot makes worst case astronomically unlikely.

    // =========================================================================
    // PATTERN SUMMARY
    // =========================================================================
    //
    // Pattern             | Precondition             | Use For
    // ──────────────────────────────────────────────────────────────────────
    // Cycle Sort          | values in [1..n]         | find missing/duplicate
    // Dutch National Flag | 3 distinct categories    | sort 0/1/2, partition
    // XOR Swap            | distinct memory locs      | swap without temp (trivia)
    // Reversal            | none                     | rotate, reverse words
    // QuickSelect         | none (random pivot)      | Kth largest/smallest O(n)

    public static void main(String[] args) {
        SwappingTechniques st = new SwappingTechniques();

        // --- Cycle Sort ---
        System.out.println("=== Cycle Sort ===");
        System.out.println(st.missingNumber(new int[]{3,0,1}));             // 2
        System.out.println(st.findAllDuplicates(new int[]{4,3,2,7,8,2,3,1})); // [3,2]
        System.out.println(st.firstMissingPositive(new int[]{3,4,-1,1}));   // 2

        // --- Dutch National Flag ---
        System.out.println("\n=== Dutch National Flag ===");
        int[] colors = {2,0,2,1,1,0};
        st.sortColors(colors);
        System.out.println(Arrays.toString(colors)); // [0,0,1,1,2,2]

        // --- Reversal ---
        System.out.println("\n=== Reversal / Rotation ===");
        int[] arr = {1,2,3,4,5,6,7};
        st.rotate(arr, 3);
        System.out.println(Arrays.toString(arr)); // [5,6,7,1,2,3,4]
        System.out.println(st.reverseWords("  hello world  ")); // "world hello"

        // --- QuickSelect ---
        System.out.println("\n=== QuickSelect ===");
        System.out.println(st.findKthLargest(new int[]{3,2,1,5,6,4}, 2));  // 5
        System.out.println(st.findKthLargest(new int[]{3,2,3,1,2,4,5,5,6}, 4)); // 4
    }
}
