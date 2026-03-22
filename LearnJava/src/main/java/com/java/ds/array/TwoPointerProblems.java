package com.java.ds.array;

import java.util.*;

/**
 * TWO-POINTER TECHNIQUE — Classic Problems
 *
 * CONCEPT:
 *   Use two indices (lo and hi) that start at opposite ends and move toward
 *   each other (or same direction for different problems). Reduces O(n²) brute
 *   force to O(n) by eliminating impossible candidates based on sorted order.
 *
 * PREREQUISITE: Usually requires a SORTED array (or sorting as a first step).
 *
 * THE TEMPLATE:
 *   int lo = 0, hi = n - 1;
 *   while (lo < hi) {
 *     int sum = arr[lo] + arr[hi];
 *     if (sum == target) { collect result; adjust pointers; }
 *     else if (sum < target) lo++;   // Need larger value → move left pointer right
 *     else hi--;                     // Need smaller value → move right pointer left
 *   }
 *
 * PROBLEMS COVERED:
 *   1. Two Sum (sorted input) — LC 167
 *   2. Three Sum — LC 15
 *   3. Trapping Rain Water — LC 42
 *   4. Container with Most Water — LC 11
 *   5. Remove Duplicates from Sorted Array — LC 26
 *
 * INTERVIEW FREQUENCY: Very high — two-pointer is in ~30% of array problems at FAANG.
 *
 * COMMON FOLLOW-UP:
 *   1. "What if the array isn't sorted?" → Sort it first: O(n log n) overall.
 *   2. "3Sum with duplicates — how do you avoid duplicate triplets?" → Skip equal elements.
 *   3. "Trapping rain water — can you do it O(1) space without two arrays?" → Two-pointer approach!
 */
public class TwoPointerProblems {

    public static void main(String[] args) {

        System.out.println("=== LC 167: Two Sum (Sorted) ===");
        // Array is 1-indexed in LC 167
        System.out.println(Arrays.toString(twoSum(new int[]{2, 7, 11, 15}, 9)));  // [1,2]
        System.out.println(Arrays.toString(twoSum(new int[]{2, 3, 4}, 6)));       // [1,3]
        System.out.println(Arrays.toString(twoSum(new int[]{-1, 0}, -1)));        // [1,2]

        System.out.println("\n=== LC 15: Three Sum (Find all triplets summing to 0) ===");
        System.out.println(threeSum(new int[]{-1, 0, 1, 2, -1, -4}));  // [[-1,-1,2],[-1,0,1]]
        System.out.println(threeSum(new int[]{0, 1, 1}));               // []
        System.out.println(threeSum(new int[]{0, 0, 0}));               // [[0,0,0]]
        System.out.println(threeSum(new int[]{-4, -1, -1, 0, 1, 2}));  // [[-1,-1,2],[-1,0,1]]

        System.out.println("\n=== LC 42: Trapping Rain Water ===");
        System.out.println(trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1})); // Expected: 6
        System.out.println(trap(new int[]{4, 2, 0, 3, 2, 5}));                    // Expected: 9
        System.out.println(trap(new int[]{1, 0, 1}));                              // Expected: 1
        System.out.println(trap(new int[]{}));                                     // Expected: 0

        System.out.println("\n=== LC 11: Container with Most Water ===");
        System.out.println(maxWater(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7})); // Expected: 49
        System.out.println(maxWater(new int[]{1, 1}));                       // Expected: 1

        System.out.println("\n=== LC 26: Remove Duplicates from Sorted Array ===");
        int[] arr = {1, 1, 2};
        System.out.println(removeDuplicates(arr) + " → " + Arrays.toString(arr)); // 2 → [1,2,2]
        arr = new int[]{0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        System.out.println(removeDuplicates(arr) + " → " + Arrays.toString(arr)); // 5 → [0,1,2,3,4,...]
    }

    // =========================================================================
    // 1. Two Sum — Sorted Array (LC 167)
    // =========================================================================

    /**
     * Finds two numbers in a sorted array that add up to target.
     * Returns 1-indexed positions [lo+1, hi+1].
     *
     * WHY TWO-POINTER WORKS:
     *   - If arr[lo] + arr[hi] == target → found.
     *   - If sum < target → we need a larger sum → move lo right (next larger value).
     *   - If sum > target → we need a smaller sum → move hi left (next smaller value).
     *   Each step eliminates at least one candidate, so the algorithm terminates in O(n).
     *
     * Time: O(n)  Space: O(1)
     */
    public static int[] twoSum(int[] numbers, int target) {
        int lo = 0, hi = numbers.length - 1;

        while (lo < hi) {
            int sum = numbers[lo] + numbers[hi];
            if (sum == target) return new int[]{lo + 1, hi + 1}; // 1-indexed result
            else if (sum < target) lo++;  // Need more → move left pointer right
            else hi--;                    // Need less → move right pointer left
        }

        return new int[]{-1, -1}; // Should never happen if input guarantees a solution
    }

    // =========================================================================
    // 2. Three Sum (LC 15)
    // =========================================================================

    /**
     * Finds all unique triplets [a, b, c] such that a + b + c = 0.
     *
     * APPROACH:
     *   Sort first. Fix one element (arr[i]), then use two-pointer on the rest.
     *   This reduces O(n³) brute force → O(n²).
     *
     * AVOIDING DUPLICATES:
     *   - Skip duplicate values of arr[i] (outerloop): if arr[i] == arr[i-1], skip.
     *   - After finding a triplet, skip duplicates of arr[lo] and arr[hi].
     *   This ensures each unique triplet appears exactly once.
     *
     * OPTIMIZATION: If arr[i] > 0, no triplet can sum to 0 (sorted array → all remaining are positive).
     *
     * Time: O(n²)  Space: O(1) ignoring output
     */
    public static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums); // Sort is required for the two-pointer step

        for (int i = 0; i < nums.length - 2; i++) {
            // Optimization: sorted array — if nums[i] > 0, all elements to right are also > 0
            // No way to get a zero sum with three positives
            if (nums[i] > 0) break;

            // Skip duplicates for the fixed element (avoid duplicate triplets in output)
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int lo = i + 1;
            int hi = nums.length - 1;

            while (lo < hi) {
                int sum = nums[i] + nums[lo] + nums[hi];

                if (sum == 0) {
                    result.add(Arrays.asList(nums[i], nums[lo], nums[hi]));

                    // Skip duplicates of lo and hi to avoid duplicate triplets
                    while (lo < hi && nums[lo] == nums[lo + 1]) lo++;
                    while (lo < hi && nums[hi] == nums[hi - 1]) hi--;

                    lo++; // Move both pointers since we've recorded this triplet
                    hi--;

                } else if (sum < 0) {
                    lo++; // Sum too small → increase lo
                } else {
                    hi--; // Sum too large → decrease hi
                }
            }
        }

        return result;
    }

    // =========================================================================
    // 3. Trapping Rain Water (LC 42)
    // =========================================================================

    /**
     * Computes total units of rain water trapped between the bars.
     *
     * KEY INSIGHT:
     *   Water at position i = min(maxLeft[i], maxRight[i]) - height[i]
     *   (water level is limited by the shorter of the two surrounding walls)
     *
     * TWO-POINTER APPROACH (O(1) space):
     *   We don't need to precompute both maxLeft[] and maxRight[] arrays.
     *   Instead, use two pointers lo and hi, tracking maxLeft and maxRight as we go:
     *   - If maxLeft <= maxRight: water at lo is determined by maxLeft (right side is taller).
     *     Process lo → water = maxLeft - height[lo], move lo right.
     *   - Else: water at hi is determined by maxRight. Process hi → move hi left.
     *
     * WHY IS THIS CORRECT?
     *   When maxLeft <= maxRight, we know the right side has at least maxRight height,
     *   so the bottleneck for position lo is definitely maxLeft (not some unknown right wall).
     *   We can safely compute water[lo] = maxLeft - height[lo].
     *
     * Time: O(n)  Space: O(1)  [naive approach is O(n) space with two extra arrays]
     */
    public static int trap(int[] height) {
        if (height == null || height.length < 3) return 0;

        int lo = 0, hi = height.length - 1;
        int maxLeft = 0, maxRight = 0;
        int water = 0;

        while (lo < hi) {
            // Update the max wall heights seen so far from each side
            maxLeft  = Math.max(maxLeft, height[lo]);
            maxRight = Math.max(maxRight, height[hi]);

            if (maxLeft <= maxRight) {
                // Left wall is the bottleneck for position lo
                water += maxLeft - height[lo]; // maxLeft - height[lo] >= 0 (guaranteed by maxLeft calc)
                lo++;
            } else {
                // Right wall is the bottleneck for position hi
                water += maxRight - height[hi];
                hi--;
            }
        }

        return water;
    }

    // =========================================================================
    // 4. Container with Most Water (LC 11)
    // =========================================================================

    /**
     * Finds two lines that together with the x-axis forms a container that holds the most water.
     * Width = hi - lo, Height = min(height[lo], height[hi]).
     * Volume = width × height.
     *
     * GREEDY INSIGHT:
     *   Starting with the widest container (lo=0, hi=n-1).
     *   To increase volume, we need to increase height (width can only shrink).
     *   The current height is limited by the SHORTER wall (min(lo, hi)).
     *   Moving the shorter wall pointer might find a taller wall → might increase volume.
     *   Moving the taller wall pointer can only maintain or decrease volume (shorter wall stays).
     *   → Always move the pointer at the SHORTER wall.
     *
     * Time: O(n)  Space: O(1)
     */
    public static int maxWater(int[] height) {
        int lo = 0, hi = height.length - 1;
        int maxVolume = 0;

        while (lo < hi) {
            int h = Math.min(height[lo], height[hi]); // Limited by shorter wall
            int w = hi - lo;                           // Width between the two walls
            maxVolume = Math.max(maxVolume, h * w);

            // Move the shorter wall pointer (hoping for a taller wall)
            if (height[lo] < height[hi]) {
                lo++;
            } else {
                hi--;
            }
        }

        return maxVolume;
    }

    // =========================================================================
    // 5. Remove Duplicates from Sorted Array (LC 26)
    // =========================================================================

    /**
     * Removes duplicates in-place from a sorted array. Returns the count of unique elements.
     * The first k elements of the array contain the unique values (in order).
     *
     * SLOW + FAST POINTER VARIANT:
     *   slow = index where next unique element should be written (write pointer)
     *   fast = index scanning for the next unique element (read pointer)
     *
     * When nums[fast] != nums[slow]: we found a new unique value → write it.
     *
     * Time: O(n)  Space: O(1)
     */
    public static int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;

        int slow = 0; // Points to the last written unique element

        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                // New unique element found → advance slow pointer and write
                slow++;
                nums[slow] = nums[fast];
            }
            // If nums[fast] == nums[slow]: duplicate → skip (do nothing)
        }

        return slow + 1; // slow is 0-indexed, so count = slow + 1
    }
}
