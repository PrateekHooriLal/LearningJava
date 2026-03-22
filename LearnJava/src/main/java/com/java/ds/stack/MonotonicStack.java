package com.java.ds.stack;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Monotonic Stack — Efficient Range Queries via Ordering
 * ============================================================
 *
 * WHAT IS A MONOTONIC STACK?
 *   A stack where elements are ALWAYS in monotonic order (either strictly increasing
 *   or strictly decreasing from bottom to top). When pushing a new element that
 *   VIOLATES the monotonic property, we pop elements until the property is restored.
 *
 *   The KEY insight: the moment we pop element X because of new element Y, we know:
 *     → Y is the "answer" for X (the first element to the right that is greater/smaller)
 *     → This lets us answer "next greater/smaller element" for all elements in O(n) total.
 *
 * TWO TYPES:
 *   DECREASING stack (top < bottom): pop when current > stack.top
 *     → Answers "NEXT GREATER ELEMENT" for each popped element
 *   INCREASING stack (top > bottom): pop when current < stack.top
 *     → Answers "NEXT SMALLER ELEMENT" for each popped element
 *
 * ALWAYS STORE INDICES (not values):
 *   Storing indices allows: computing distances, accessing the value via arr[idx],
 *   checking window boundaries (for sliding window variants).
 *
 * AMORTIZED ANALYSIS — why O(n)?
 *   Each element is pushed onto the stack EXACTLY ONCE.
 *   Each element is popped from the stack AT MOST ONCE.
 *   Total push + pop operations = 2n → O(n) amortized, despite the inner while loop.
 *
 * WHEN TO USE — SIGNAL WORDS:
 *   "next greater element", "next smaller element"
 *   "how many days until temperature rises"
 *   "visible buildings" (each building blocks all shorter ones behind it)
 *   "largest rectangle in histogram"
 *   "trapping rain water"
 *   "stock span problem"
 *
 * TIME / SPACE:
 *   All monotonic stack problems: O(n) time, O(n) space (stack stores at most n elements)
 *
 * INTERVIEW ANGLE:
 *   "Why is this O(n) if there's a while loop inside the for loop?"
 *   → Amortized: total pushes = n, total pops ≤ n. Sum = O(n).
 *   "What if I need next greater element in a CIRCULAR array?"
 *   → Double the loop (run 0..2n-1) using index % n to access elements.
 *
 * REAL-WORLD:
 *   - Stock span (financial: how many consecutive days was price <= today?)
 *   - Skyline problem (LC 218)
 *   - Largest rectangle in skyline (building code, computer graphics)
 *   - Expression evaluation (operand stacks in compilers)
 * ============================================================
 */
public class MonotonicStack {

    // =========================================================
    // PROBLEM 1: Next Greater Element I & II (LC 496, LC 503)
    // =========================================================

    /**
     * KEY INSIGHT: Use a DECREASING monotonic stack.
     * For each element, when we push it, we check: is there any element below me that
     * should have ME as their "next greater"? → pop those elements, set their answer to me.
     *
     * LC 496 (no circular): run once through nums2, answer[i] = NGE of nums2[i]
     *
     * CIRCULAR VARIANT (LC 503):
     *   Treat the array as doubled: for index i in [0..2n-1], use nums[i%n].
     *   Only record answers for i < n (first pass only).
     *   The second pass (i in [n..2n-1]) allows elements near the end to "wrap around"
     *   and find their NGE in the beginning of the array.
     *
     * GOTCHA: In the circular version, initialize result[] to -1 (no NGE found yet).
     *         Don't record answers for i >= n (those are the "ghost" second pass elements).
     */
    static int[] nextGreaterElements(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1); // default: no NGE found

        // Stack stores INDICES of elements waiting for their NGE
        Deque<Integer> stack = new ArrayDeque<>();

        // Run 0..2n-1 for circular — use i%n to access the actual element
        for (int i = 0; i < 2 * n; i++) {
            // While stack not empty AND current element > stack top's value:
            // → current element IS the NGE for the stack top element
            while (!stack.isEmpty() && nums[i % n] > nums[stack.peek()]) {
                int idx = stack.pop();
                result[idx] = nums[i % n]; // NGE found for element at idx
            }
            // Only push indices from the first pass (0..n-1)
            // Second pass resolves existing questions but doesn't add new ones
            if (i < n) {
                stack.push(i);
            }
        }
        return result;
        // Elements still in the stack at the end have no NGE → remain -1
    }

    // =========================================================
    // PROBLEM 2: Largest Rectangle in Histogram (LC 84 — HARD)
    // =========================================================

    /**
     * KEY INSIGHT: For each bar, find the LARGEST rectangle that uses this bar as its HEIGHT.
     * That rectangle extends LEFT until it hits a bar shorter than this one,
     * and extends RIGHT until it hits a bar shorter than this one.
     * Width = (rightBoundary - leftBoundary - 1)
     *
     * Monotonic INCREASING stack: pop when current bar is SHORTER than stack top.
     * When we pop height h (at index top), the current bar i is the right boundary
     * (first bar to the right that is shorter than h).
     * The new stack top (after popping) is the left boundary (last bar shorter than h to the left).
     *
     * Width calculation (TRICKY, memorize this):
     *   If stack is empty after pop: width = i  (rectangle spans from index 0 to i-1)
     *   If stack not empty: width = i - stack.peek() - 1  (spans from peek+1 to i-1)
     *
     * SENTINEL TECHNIQUE:
     *   Append 0 at both ends of the heights array.
     *   Left 0: serves as the "stop" for extending left — no real bar is shorter than 0.
     *   Right 0: flushes all remaining elements from the stack at the end.
     *   With sentinels, the stack always has at least 1 element (the left sentinel index).
     *   This simplifies the width calculation: width = i - stack.peek() - 1 always.
     *
     * COMPLEXITY: O(n) time — each bar pushed and popped at most once.
     *             O(n) space — stack.
     *
     * INTERVIEW: This is one of the hardest "stack" problems. Walk through an example:
     *   heights = [2,1,5,6,2,3] → answer = 10 (bars at index 2,3 with height 5,6: 5*2=10)
     */
    static int largestRectangleInHistogram(int[] heights) {
        // Add sentinel bars: 0 at start and end
        int n = heights.length;
        int[] h = new int[n + 2];
        h[0] = 0; // left sentinel
        System.arraycopy(heights, 0, h, 1, n);
        h[n + 1] = 0; // right sentinel — flushes all remaining elements

        Deque<Integer> stack = new ArrayDeque<>(); // stores indices
        stack.push(0); // push left sentinel index
        int maxArea = 0;

        for (int i = 1; i < h.length; i++) {
            // Pop while current bar is shorter than stack top (monotonic increasing invariant broken)
            while (h[i] < h[stack.peek()]) {
                int height = h[stack.pop()]; // this is the height of the rectangle
                int width  = i - stack.peek() - 1; // peek is the new left boundary after pop
                maxArea = Math.max(maxArea, height * width);
            }
            stack.push(i); // maintain increasing order
        }
        return maxArea;
    }

    // =========================================================
    // PROBLEM 3: Daily Temperatures (LC 739)
    // =========================================================

    /**
     * KEY INSIGHT: "How many days until a warmer temperature?" = "next greater element" distance.
     * Store INDICES in the stack. When popping, the answer = current_day - popped_day.
     *
     * DECREASING stack of indices (stack values are indices, but their corresponding temps
     * are in decreasing order from bottom to top).
     *
     * Walk-through for [73,74,75,71,69,72,76,73]:
     *   i=0: push 0. Stack: [0]
     *   i=1: temp[1]=74 > temp[0]=73 → pop 0, result[0]=1-0=1. Push 1. Stack: [1]
     *   i=2: temp[2]=75 > temp[1]=74 → pop 1, result[1]=2-1=1. Push 2. Stack: [2]
     *   i=3: 71 < 75 → push 3. Stack: [2,3]
     *   i=4: 69 < 71 → push 4. Stack: [2,3,4]
     *   i=5: 72 > 69 → pop 4, result[4]=1. 72 > 71 → pop 3, result[3]=2. 72 < 75 → push 5.
     *   i=6: 76 > 72 → pop 5, result[5]=1. 76 > 75 → pop 2, result[2]=4. Push 6.
     *   i=7: 73 < 76 → push 7.
     *   End: stack [6,7] → result[6]=0, result[7]=0 (no warmer day found)
     *   Final: [1,1,4,2,1,1,0,0]
     */
    static int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] result = new int[n]; // initialized to 0 — means "no warmer day found"
        Deque<Integer> stack = new ArrayDeque<>(); // decreasing monotonic stack of indices

        for (int i = 0; i < n; i++) {
            // Pop all days that finally have their answer
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int prevDay = stack.pop();
                result[prevDay] = i - prevDay; // days waited
            }
            stack.push(i);
        }
        return result;
    }

    // =========================================================
    // PROBLEM 4: Asteroid Collision (LC 735)
    // =========================================================

    /**
     * KEY INSIGHT: Positive asteroids move RIGHT, negative asteroids move LEFT.
     * Collision only happens when a positive asteroid (moving right) is followed by
     * a negative asteroid (moving left) — they're heading toward each other.
     * No collision when: both same direction, or negative then positive (moving apart).
     *
     * Use a stack to simulate survivors:
     *   Push positive asteroids (they move right, safe until a negative comes).
     *   For negative asteroids: compare with top of stack:
     *     - Stack top is negative: no collision (both go left), just push.
     *     - Stack is empty: no asteroid to collide with, push (goes left freely).
     *     - Stack top is positive and abs(top) > abs(current): top wins, discard current.
     *     - Stack top is positive and abs(top) < abs(current): pop top (destroyed), continue.
     *     - Stack top is positive and abs(top) == abs(current): both destroyed, pop top, discard current.
     *
     * GOTCHA: The while loop processes multiple sequential collisions for one asteroid.
     *         A large leftward asteroid can destroy many smaller rightward ones.
     */
    static int[] asteroidCollision(int[] asteroids) {
        Deque<Integer> stack = new ArrayDeque<>();

        for (int ast : asteroids) {
            boolean alive = true; // does the current asteroid survive?

            while (alive && ast < 0 && !stack.isEmpty() && stack.peek() > 0) {
                // Current asteroid is moving LEFT, top is moving RIGHT — collision!
                int top = stack.peek();
                if (Math.abs(top) < Math.abs(ast)) {
                    stack.pop(); // top asteroid destroyed, current continues left
                } else if (Math.abs(top) == Math.abs(ast)) {
                    stack.pop(); // both destroyed
                    alive = false;
                } else {
                    alive = false; // current asteroid destroyed by larger top
                }
            }

            if (alive) stack.push(ast); // survivor gets pushed to stack
        }

        // Convert stack to array (stack is LIFO, so collect and reverse)
        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = stack.pop();
        }
        return result;
    }

    // =========================================================
    // PROBLEM 5: Trapping Rain Water — Stack Approach (LC 42)
    // =========================================================

    /**
     * KEY INSIGHT: Process bar by bar. Water can be trapped between a "bottom" (just popped)
     * and the current bar (right boundary), with the stack top (left boundary).
     *
     * Algorithm:
     *   Maintain a DECREASING stack (heights are non-increasing from bottom to top).
     *   When current bar > stack top: water can be trapped above the top.
     *     1. Pop the top (this is the "bottom" of the water container).
     *     2. Left boundary = new stack top (if exists).
     *     3. Right boundary = current index.
     *     4. Water height = min(height[left], height[right]) - height[bottom]
     *     5. Water width = right - left - 1
     *     6. Add height * width to total water.
     *
     * This computes water layer by layer (horizontal slices), not column by column.
     *
     * vs Two-Pointer approach: Two-pointer is simpler (O(1) space).
     *    Stack approach is more intuitive for visualizing horizontal slices.
     *    Both are O(n) time. Two-pointer is preferred in interviews for simplicity.
     *
     * Example: [0,1,0,2,1,0,1,3,2,1,2,1]
     *   Water trapped = 6 units
     */
    static int trapRainWater(int[] height) {
        Deque<Integer> stack = new ArrayDeque<>(); // stores indices, decreasing heights
        int water = 0;

        for (int i = 0; i < height.length; i++) {
            // While current bar is taller than stack top: water is trapped
            while (!stack.isEmpty() && height[i] > height[stack.peek()]) {
                int bottom = stack.pop(); // the "trough" bottom
                if (stack.isEmpty()) break; // no left boundary → no water

                int left  = stack.peek(); // left boundary index
                int right = i;            // right boundary index (current)

                int waterHeight = Math.min(height[left], height[right]) - height[bottom];
                int waterWidth  = right - left - 1;
                water += waterHeight * waterWidth;
            }
            stack.push(i);
        }
        return water;
    }

    // =========================================================
    // MAIN: Tests & Demos
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Monotonic Stack Problems                   ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Next Greater Element (Circular)
        System.out.println("\n=== Next Greater Element (circular) ===");
        int[] nge = nextGreaterElements(new int[]{1, 2, 1});
        System.out.println("NGE([1,2,1]): " + Arrays.toString(nge)); // [2,-1,2]
        int[] nge2 = nextGreaterElements(new int[]{1, 2, 3, 4, 3});
        System.out.println("NGE([1,2,3,4,3]): " + Arrays.toString(nge2)); // [2,3,4,-1,4]

        // Largest Rectangle in Histogram
        System.out.println("\n=== Largest Rectangle in Histogram ===");
        System.out.println("LRH([2,1,5,6,2,3]): " + largestRectangleInHistogram(new int[]{2,1,5,6,2,3})); // 10
        System.out.println("LRH([2,4]): "          + largestRectangleInHistogram(new int[]{2,4}));          // 4
        System.out.println("LRH([1]): "            + largestRectangleInHistogram(new int[]{1}));            // 1

        // Daily Temperatures
        System.out.println("\n=== Daily Temperatures ===");
        int[] temps = {73,74,75,71,69,72,76,73};
        System.out.println("DT(" + Arrays.toString(temps) + "): " + Arrays.toString(dailyTemperatures(temps)));
        // Expected: [1,1,4,2,1,1,0,0]

        // Asteroid Collision
        System.out.println("\n=== Asteroid Collision ===");
        System.out.println("AC([5,10,-5]): " + Arrays.toString(asteroidCollision(new int[]{5,10,-5}))); // [5,10]
        System.out.println("AC([8,-8]): "    + Arrays.toString(asteroidCollision(new int[]{8,-8})));    // []
        System.out.println("AC([10,2,-5]): " + Arrays.toString(asteroidCollision(new int[]{10,2,-5}))); // [10]
        System.out.println("AC([-2,-1,1,2]): " + Arrays.toString(asteroidCollision(new int[]{-2,-1,1,2}))); // [-2,-1,1,2]

        // Trapping Rain Water
        System.out.println("\n=== Trapping Rain Water ===");
        System.out.println("Trap([0,1,0,2,1,0,1,3,2,1,2,1]): " + trapRainWater(new int[]{0,1,0,2,1,0,1,3,2,1,2,1})); // 6
        System.out.println("Trap([4,2,0,3,2,5]): " + trapRainWater(new int[]{4,2,0,3,2,5})); // 9

        System.out.println("\n=== All monotonic stack tests complete ===");
    }
}
