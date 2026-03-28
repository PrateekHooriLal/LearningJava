package com.interview.altimetrik;

/**
 * ============================================================
 * INTERVIEW QUESTION — Altemetrik
 * ============================================================
 * PROBLEM: Rotate an array to the RIGHT by k positions
 *          WITHOUT creating a new array (in-place, O(1) space)
 *
 * EXAMPLE:
 *   Input : [1, 2, 3, 4], k = 1
 *   Output: [4, 1, 2, 3]   ← last element wraps to front
 *
 *   Input : [1, 2, 3, 4], k = 2
 *   Output: [3, 4, 1, 2]   ← last 2 elements wrap to front
 *
 * WHAT "RIGHT ROTATION" MEANS:
 *   Each element shifts one position to the right.
 *   The last element wraps around to index 0.
 *   Think of it as: the TAIL of the array moves to the HEAD.
 *
 * KEY CONSTRAINT: No new array — must rotate in-place.
 *
 * ============================================================
 * THREE APPROACHES — complexity comparison
 * ============================================================
 *
 *  Approach 1 — Iterative (current):    O(n*k) time, O(1) space
 *  Approach 2 — Recursion (current):    O(n*k) time, O(k) stack space
 *  Approach 3 — REVERSAL (OPTIMAL):     O(n)   time, O(1) space  ← BEST
 *
 * INTERVIEW TIP: Always mention the optimal approach.
 *   Interviewers at product companies (Altemetrik, FAANG etc.)
 *   almost always push back: "Can you do it in O(n)?"
 *
 * ============================================================
 * EDGE CASES to handle (mention these in interviews!)
 * ============================================================
 *   1. k = 0       → no rotation needed
 *   2. k >= n      → k = k % n (rotating n times = original array)
 *   3. k < 0       → convert left-rotation to right: k = n + k
 *   4. empty array → guard against arr.length == 0
 *   5. single elem → nothing to rotate
 */
public class Altemetrik {

    public static void main(String[] args) {

        System.out.println("=== Approach 1: Iterative (O(n*k)) ===");
        int[] arr1 = {1, 2, 3, 4};
        System.out.print("Before: ");
        printArray(arr1);
        rotateArray(arr1, 1);
        System.out.print("After k=1: ");
        printArray(arr1);   // expected: [4, 1, 2, 3]

        int[] arr2 = {1, 2, 3, 4};
        rotateArray(arr2, 2);
        System.out.print("After k=2: ");
        printArray(arr2);   // expected: [3, 4, 1, 2]

        System.out.println("\n=== Approach 2: Recursion (O(n*k)) ===");
        int[] arr3 = {1, 2, 3, 4};
        rotateArrayRecursion(arr3, 1);
        System.out.print("After k=1: ");
        printArray(arr3);   // expected: [4, 1, 2, 3]

        System.out.println("\n=== Approach 3: REVERSAL — OPTIMAL O(n) ===");
        int[] arr4 = {1, 2, 3, 4};
        rotateOptimal(arr4, 1);
        System.out.print("After k=1: ");
        printArray(arr4);   // expected: [4, 1, 2, 3]

        int[] arr5 = {1, 2, 3, 4};
        rotateOptimal(arr5, 2);
        System.out.print("After k=2: ");
        printArray(arr5);   // expected: [3, 4, 1, 2]

        // Edge case: k >= array length
        int[] arr6 = {1, 2, 3, 4};
        rotateOptimal(arr6, 6);  // 6 % 4 = 2, same as k=2
        System.out.print("After k=6 (same as k=2): ");
        printArray(arr6);   // expected: [3, 4, 1, 2]
    }

    // ============================================================
    // APPROACH 1: ITERATIVE — O(n * k) time, O(1) space
    // ============================================================
    // HOW IT WORKS:
    //   Repeat k times: save last element, shift everything right by 1,
    //   place saved element at index 0.
    //
    // VISUAL for [1,2,3,4] k=1:
    //   temp = 4
    //   shift: [1,1,2,3]  (each element copies from left neighbour)
    //   place: [4,1,2,3]  ✓
    //
    // WHY IT'S NOT OPTIMAL:
    //   For k=3 and n=1000, we do 3000 shift operations.
    //   The reversal approach does only ~2n operations regardless of k.
    //
    // Time:  O(n * k) — outer loop k times, inner shift loop n times
    // Space: O(1)     — only the temp variable
    public static void rotateArray(int[] arr, int k) {
        int n = arr.length;

        // EDGE CASE: k % n handles k >= n (e.g., k=4 on size-4 array = no rotation)
        k = k % n;

        for (int i = 0; i < k; i++) {
            // Save the last element before it gets overwritten
            int temp = arr[n - 1];

            // Shift every element one position to the right
            // Go backwards: arr[j] = arr[j-1] (right to left to avoid overwriting)
            for (int j = n - 1; j > 0; j--) {
                arr[j] = arr[j - 1];  // each element takes value of its left neighbour
            }

            // Place the saved last element at the front
            arr[0] = temp;
        }
    }

    // ============================================================
    // APPROACH 2: RECURSION — O(n * k) time, O(k) stack space
    // ============================================================
    // HOW IT WORKS:
    //   Same as iterative but uses the call stack to count k rotations.
    //   Base case: k == 0, stop recursing.
    //   Recursive case: do one rotation, then recurse with k-1.
    //
    // WHY RECURSION IS WORSE THAN ITERATIVE HERE:
    //   Same O(n*k) time, BUT adds O(k) stack frames.
    //   Risk of StackOverflowError if k is very large (e.g., k = 100,000).
    //   Interviewers may ask: "What's the downside of the recursive version?"
    //   Answer: stack overflow risk + no benefit over iteration here.
    //
    // WHEN IS RECURSION JUSTIFIED? When the problem has natural sub-problems
    //   (e.g., trees, divide-and-conquer). Plain looping doesn't justify it.
    //
    // Time:  O(n * k) — k recursive calls, each doing O(n) shift work
    // Space: O(k)     — k frames on the call stack
    public static void rotateArrayRecursion(int[] arr, int k) {
        // BASE CASE: nothing left to rotate
        if (k <= 0) return;

        // Save last element before shifting
        int temp = arr[arr.length - 1];

        // Shift all elements right by 1 position
        for (int i = arr.length - 1; i > 0; i--) {
            arr[i] = arr[i - 1];
        }

        // Place last element at front
        arr[0] = temp;

        // Recurse for the remaining k-1 rotations
        rotateArrayRecursion(arr, k - 1);
    }

    // ============================================================
    // APPROACH 3: REVERSAL ALGORITHM — OPTIMAL O(n) time, O(1) space
    // ============================================================
    // THIS IS THE ANSWER INTERVIEWERS ARE LOOKING FOR.
    //
    // KEY INSIGHT (the "aha" moment):
    //   Reversing sub-arrays in-place achieves rotation without extra space.
    //   Three reverses = one rotation. No extra array needed.
    //
    // THE RECIPE for RIGHT rotation by k:
    //   Step 1: Reverse the ENTIRE array
    //   Step 2: Reverse the FIRST k elements
    //   Step 3: Reverse the REMAINING n-k elements
    //
    // VISUAL PROOF with [1, 2, 3, 4], k=1:
    //   Original:          [1, 2, 3, 4]
    //   Step 1 — reverse all:    [4, 3, 2, 1]
    //   Step 2 — reverse [0..0]: [4, 3, 2, 1]  (single element, no change)
    //   Step 3 — reverse [1..3]: [4, 1, 2, 3]  ✓
    //
    // VISUAL PROOF with [1, 2, 3, 4], k=2:
    //   Original:          [1, 2, 3, 4]
    //   Step 1 — reverse all:    [4, 3, 2, 1]
    //   Step 2 — reverse [0..1]: [3, 4, 2, 1]
    //   Step 3 — reverse [2..3]: [3, 4, 1, 2]  ✓
    //
    // WHY THIS WORKS (intuition):
    //   Reversing the whole array "mirrors" the positions.
    //   Then reversing each half "un-mirrors" each half independently,
    //   which effectively moves the last k elements to the front.
    //
    // Time:  O(n) — three reverse passes, each O(n/2) → total O(n)
    // Space: O(1) — only uses a temp variable inside reverse(), no new array
    //
    // INTERVIEW FOLLOW-UP: "What if k is larger than array length?"
    //   Answer: k = k % n. Rotating n times = same array. Take the remainder.
    public static void rotateOptimal(int[] arr, int k) {
        int n = arr.length;

        // EDGE CASE: guard against empty array or zero rotation
        if (n == 0 || k == 0) return;

        // EDGE CASE: k >= n means full rotations cancel out
        // e.g., k=6 on n=4 → effective rotation = 6 % 4 = 2
        k = k % n;
        if (k == 0) return;  // after mod, k=0 means no rotation needed

        // STEP 1: Reverse entire array
        // [1, 2, 3, 4] → [4, 3, 2, 1]
        reverse(arr, 0, n - 1);

        // STEP 2: Reverse first k elements (indices 0 to k-1)
        // These are the elements that should end up at the FRONT after rotation
        // [4, 3, 2, 1] → [3, 4, 2, 1]  (for k=2)
        reverse(arr, 0, k - 1);

        // STEP 3: Reverse remaining n-k elements (indices k to n-1)
        // These are the elements that stay at the BACK after rotation
        // [3, 4, 2, 1] → [3, 4, 1, 2]  (for k=2) ✓
        reverse(arr, k, n - 1);
    }

    // ============================================================
    // HELPER: reverse a sub-array in-place between index lo and hi
    // ============================================================
    // Uses two-pointer technique: swap outermost elements, move inward.
    // Stops when pointers meet or cross in the middle.
    //
    // Time:  O(hi - lo) → O(n) in the worst case
    // Space: O(1)        → single temp variable
    private static void reverse(int[] arr, int lo, int hi) {
        while (lo < hi) {
            // Swap arr[lo] and arr[hi]
            int temp = arr[lo];
            arr[lo] = arr[hi];
            arr[hi] = temp;

            lo++;   // move left pointer inward
            hi--;   // move right pointer inward
        }
    }

    // ============================================================
    // UTILITY: print array contents
    // ============================================================
    private static void printArray(int[] arr) {
        System.out.print("[");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i]);
            if (i < arr.length - 1) System.out.print(", ");
        }
        System.out.println("]");
    }

    /*
     * ============================================================
     * INTERVIEW Q&A — study these before the interview
     * ============================================================
     *
     * Q1: What is the most optimal solution for in-place array rotation?
     * A:  The Reversal Algorithm — O(n) time, O(1) space.
     *     Three calls to a reverse helper is all it takes.
     *
     * Q2: Why is the naive O(n*k) approach bad?
     * A:  For k=50 and n=10^6, that's 50 million operations.
     *     The reversal approach does ~2n = 2 million operations.
     *     At scale the difference is dramatic.
     *
     * Q3: What happens if k > array length?
     * A:  Apply k = k % n first. Rotating n times brings you back to the start,
     *     so only the remainder matters. Example: k=6, n=4 → same as k=2.
     *
     * Q4: Can you do LEFT rotation using the same technique?
     * A:  Yes. For left rotation by k, just use: rightRotate(arr, n - k).
     *     Or swap the order: reverse [0..k-1], reverse [k..n-1], reverse all.
     *
     * Q5: What's wrong with the recursive approach?
     * A:  Same time complexity as iterative O(n*k), but adds O(k) stack space.
     *     Risk of StackOverflowError for large k. No benefit over the loop.
     *
     * Q6: Is this the same as LeetCode 189 "Rotate Array"?
     * A:  Yes, exactly. The reversal approach is the accepted O(n)/O(1) solution.
     *
     * ============================================================
     * COMPLEXITY SUMMARY TABLE
     * ============================================================
     *
     *  Approach        | Time    | Space | Notes
     * -----------------|---------|-------|---------------------------
     *  Iterative       | O(n*k)  | O(1)  | Simple but slow for large k
     *  Recursive       | O(n*k)  | O(k)  | Stack overflow risk
     *  Reversal (BEST) | O(n)    | O(1)  | Always use this in interviews
     *
     * ============================================================
     */
}
