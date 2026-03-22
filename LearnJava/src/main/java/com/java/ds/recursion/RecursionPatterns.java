package com.java.ds.recursion;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: Recursion — The Foundation of DP and Backtracking
 * ============================================================
 *
 * WHAT IS RECURSION?
 *   A function that calls itself with a SMALLER / SIMPLER input.
 *   Every recursive solution has:
 *   1. BASE CASE: condition where we stop recursing (prevents infinite recursion)
 *   2. RECURSIVE CASE: call self with smaller input (progress toward base case)
 *   3. COMBINATION: merge results of recursive calls into the answer for current input
 *
 * THE RECURSION → DP → ITERATION LADDER:
 *   Recursion (exponential) → + Memoization (top-down DP) → Tabulation (bottom-up DP)
 *   Each step eliminates redundant computation. Final step (tabulation) uses O(1) stack.
 *
 * WHEN TO USE RECURSION:
 *   - Problem has OPTIMAL SUBSTRUCTURE (solution uses solutions to subproblems)
 *   - Problem can be naturally divided: trees, graphs, divide-and-conquer
 *   - Problem requires exhaustive enumeration: subsets, permutations, combinations
 *   - The recursive formulation is clear even if you'll implement it differently
 *
 * RECURSION PITFALLS:
 *   - Missing base case → StackOverflowError
 *   - Not making progress (recursive call with same input) → infinite recursion
 *   - Overlapping subproblems without memoization → exponential time (recomputing same things)
 *   - Stack overflow for large n → convert to iterative + explicit stack, or use tail recursion
 *
 * TIME / SPACE:
 *   Subsets: O(2^n) time, O(n) recursion depth
 *   Permutations: O(n!) time, O(n) depth
 *   Combinations: O(C(n,k)) time, O(k) depth
 *   Fibonacci naive: O(2^n). With memo: O(n).
 *   MergeSort: T(n) = 2T(n/2) + O(n) → O(n log n) by Master Theorem
 *
 * INTERVIEW ANGLE:
 *   "Walk me through the recursion tree for fib(5)."
 *   "How would you memoize this recursive solution?"
 *   "What's the time complexity of generating all subsets?"
 *   "How do you prevent stack overflow in deep recursion?"
 *
 * REAL-WORLD:
 *   - File system traversal: recursive descent through directories
 *   - XML/JSON parsing: recursive descent parsers
 *   - GUI rendering: compositing UI trees recursively
 *   - Compiler: recursive descent parsing, AST evaluation
 * ============================================================
 */
public class RecursionPatterns {

    // =========================================================
    // SECTION 1: Subsets (LC 78) — Include/Exclude Pattern
    // =========================================================

    /**
     * KEY INSIGHT: At each index, we make a binary choice: INCLUDE or EXCLUDE this element.
     * The recursion tree has 2^n leaves — one for each subset.
     *
     * APPROACH 1: RECURSIVE (include/exclude):
     *   Base: index == nums.length → add current subset to results.
     *   Recursive: two branches — include nums[index], recurse; then exclude, recurse.
     *
     * APPROACH 2: ITERATIVE BITMASK:
     *   For n elements, there are 2^n subsets. Enumerate all bitmasks 0..2^n-1.
     *   If bit j of mask is set, include nums[j].
     *   Each mask represents one unique subset.
     *   Time: O(n * 2^n) — for each of 2^n masks, scan n bits.
     *
     * BACKTRACKING vs include/exclude:
     *   Include/exclude: naturally generates all subsets. No "undo" needed.
     *   Backtracking: add element, recurse, then REMOVE (undo). Used for subsets too.
     *   For pure subsets, both work. Backtracking becomes essential when you need to
     *   prune the search space (combinations with sum, N-queens, etc.).
     *
     * INTERVIEW: "Subsets II" (LC 90) has duplicates → sort first, skip duplicates at same recursion level.
     */
    static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        generateSubsets(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private static void generateSubsets(int[] nums, int index, List<Integer> current, List<List<Integer>> result) {
        // Base case: processed all elements → add the current subset
        if (index == nums.length) {
            result.add(new ArrayList<>(current)); // IMPORTANT: new copy, not a reference
            return;
        }
        // Include nums[index]
        current.add(nums[index]);
        generateSubsets(nums, index + 1, current, result);

        // Exclude nums[index] (backtrack)
        current.remove(current.size() - 1); // remove last element (undo the include)
        generateSubsets(nums, index + 1, current, result);
    }

    // Iterative bitmask approach
    static List<List<Integer>> subsetsBitmask(int[] nums) {
        int n = nums.length;
        List<List<Integer>> result = new ArrayList<>();

        for (int mask = 0; mask < (1 << n); mask++) { // 0 to 2^n - 1
            List<Integer> subset = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) { // bit j is set in mask
                    subset.add(nums[j]);
                }
            }
            result.add(subset);
        }
        return result;
    }

    // =========================================================
    // SECTION 2: Permutations (LC 46) — Swap-Based Backtracking
    // =========================================================

    /**
     * KEY INSIGHT: At position 'start', the element can be ANY of nums[start..end].
     * Swap nums[start] with nums[i] for each i in [start..end].
     * Recurse for the rest (start+1..end).
     * UNDO the swap after recursing (backtrack) to restore the array state.
     *
     * RECURSION TREE for [1,2,3]:
     *   Level 0: 3 choices for position 0 → [1,2,3], [2,1,3], [3,2,1]
     *   Level 1: 2 choices for position 1 → ...
     *   Level 2: 1 choice for position 2 → leaf = one permutation
     *   Total leaves = 3! = 6
     *
     * COMPLEXITY: O(n!) time (n! permutations, each O(n) to copy to result)
     *             O(n) space (recursion depth n, no extra array needed)
     *
     * ALTERNATIVE: boolean[] visited array — pick unused elements one by one.
     *   Same O(n!) but uses O(n) extra space for visited array.
     *
     * INTERVIEW: "Permutations II" (LC 47) with duplicates:
     *   Sort first, use visited[], skip duplicate choices at same level.
     */
    static List<List<Integer>> permutations(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        permuteHelper(nums, 0, result);
        return result;
    }

    private static void permuteHelper(int[] nums, int start, List<List<Integer>> result) {
        if (start == nums.length) {
            // All positions filled → one permutation complete
            List<Integer> perm = new ArrayList<>();
            for (int x : nums) perm.add(x);
            result.add(perm);
            return;
        }
        for (int i = start; i < nums.length; i++) {
            swap(nums, start, i);              // choose: put nums[i] at position 'start'
            permuteHelper(nums, start + 1, result); // recurse: fill positions start+1..end
            swap(nums, start, i);              // unchoose (backtrack): restore original
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
    }

    // =========================================================
    // SECTION 3: Combinations (LC 77)
    // =========================================================

    /**
     * KEY INSIGHT: Choose k elements from [1..n]. Order does NOT matter.
     * Use a 'start' index to prevent reusing or reordering elements.
     * Each recursive call only considers elements from 'start' onward.
     *
     * PRUNING: if remaining elements to consider < elements still needed:
     *   remaining = n - start + 1, needed = k - current.size()
     *   If remaining < needed: impossible to form a valid combination → prune.
     *   This reduces the recursion tree significantly for small k.
     *
     * COMPLEXITY: O(C(n,k) * k) — C(n,k) combinations, each of length k.
     *             O(k) recursion depth.
     *
     * INTERVIEW: "Combination Sum" (LC 39): elements can be reused → start = i (not i+1).
     *            "Combination Sum II" (LC 40): no reuse + duplicates → sort, skip duplicates.
     */
    static List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        combineHelper(n, k, 1, new ArrayList<>(), result);
        return result;
    }

    private static void combineHelper(int n, int k, int start, List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        // PRUNING: need (k - current.size()) more elements, only (n - start + 1) left
        int needed = k - current.size();
        int available = n - start + 1;
        if (available < needed) return; // not enough elements remaining

        for (int i = start; i <= n; i++) {
            current.add(i);
            combineHelper(n, k, i + 1, current, result); // i+1: no reuse, preserves order
            current.remove(current.size() - 1); // backtrack
        }
    }

    // =========================================================
    // SECTION 4: Tree Recursion & Master Theorem
    // =========================================================

    /**
     * FIBONACCI — Exponential recursion tree:
     *   fib(n) calls fib(n-1) and fib(n-2) — binary branching.
     *   Recursion tree has ~2^n nodes. Many are IDENTICAL (overlapping subproblems).
     *   fib(5) → computes fib(3) THREE TIMES, fib(2) FIVE TIMES.
     *   Without memoization: O(2^n) time, O(n) space (max depth n).
     *
     * TOWER OF HANOI:
     *   T(n) = 2T(n-1) + O(1) → T(n) = O(2^n)
     *   Minimum moves to solve Hanoi with n disks = 2^n - 1.
     *   Unavoidable — you MUST move 2^n - 1 times. Recursion is optimal.
     *
     * MASTER THEOREM — T(n) = a*T(n/b) + f(n):
     *   a = number of subproblems, b = factor by which problem shrinks, f(n) = combine cost.
     *   Case 1: f(n) = O(n^(log_b(a) - ε)) → T(n) = O(n^log_b(a))   [subproblems dominate]
     *   Case 2: f(n) = Θ(n^log_b(a))        → T(n) = O(n^log_b(a) * log n) [equal work each level]
     *   Case 3: f(n) = Ω(n^(log_b(a) + ε)) → T(n) = O(f(n))         [combine dominates]
     *
     *   MergeSort: T(n) = 2T(n/2) + O(n) → a=2, b=2, log_2(2)=1, f(n)=n^1 → Case 2 → O(n log n)
     *   BinarySearch: T(n) = T(n/2) + O(1) → a=1, b=2, log_2(1)=0, f(n)=n^0=1 → Case 2 → O(log n)
     *   QuickSort avg: T(n) = 2T(n/2) + O(n) → O(n log n) (same as merge sort)
     *   QuickSort worst: T(n) = T(n-1) + O(n) → O(n^2) (pivot always picks min/max)
     */
    static long fibNaive(int n) {
        if (n <= 1) return n; // base cases: fib(0)=0, fib(1)=1
        return fibNaive(n - 1) + fibNaive(n - 2); // two recursive calls: O(2^n)
    }

    static void hanoi(int n, char from, char to, char aux) {
        if (n == 0) return; // base: no disk to move
        hanoi(n - 1, from, aux, to); // move n-1 disks from 'from' to 'aux'
        System.out.println("Move disk " + n + " from " + from + " to " + to);
        hanoi(n - 1, aux, to, from); // move n-1 disks from 'aux' to 'to'
    }

    // =========================================================
    // SECTION 5: Memoization — Top-Down DP
    // =========================================================

    /**
     * KEY INSIGHT: Memoization = recursion + cache.
     * Store the result of each subproblem in a map (or array).
     * Before computing, check if result is already cached → return it.
     * This eliminates OVERLAPPING SUBPROBLEMS, reducing O(2^n) to O(n).
     *
     * MEMOIZATION vs TABULATION:
     *   Memoization (top-down): only computes subproblems that are actually needed.
     *     + Natural to write (extends recursive solution)
     *     - Function call overhead, stack depth proportional to recursion depth
     *     - StackOverflowError for large n
     *
     *   Tabulation (bottom-up): iteratively fills a table from small to large subproblems.
     *     + No recursion overhead, no stack overflow
     *     + Often faster in practice (array access vs map lookup)
     *     - Must determine the order to fill the table (dependency order)
     *
     * For Fibonacci: n=50 requires depth 50 → both fine.
     *               n=100,000 → tabulation only (stack overflow with recursion).
     *
     * CONVERSION RULE:
     *   Recursive: think about the problem.
     *   Memoize: add Map<args, result> + check before computing.
     *   Tabulate: reverse the computation order, use array instead of map.
     */
    static Map<Integer, Long> memo = new HashMap<>();

    static long fibMemo(int n) {
        if (n <= 1) return n;
        if (memo.containsKey(n)) return memo.get(n); // cache hit: O(1) lookup

        long result = fibMemo(n - 1) + fibMemo(n - 2); // cache miss: compute
        memo.put(n, result);                            // store in cache
        return result;
    }

    // Tabulation (iterative bottom-up DP)
    static long fibTabulation(int n) {
        if (n <= 1) return n;
        long prev2 = 0, prev1 = 1;
        for (int i = 2; i <= n; i++) {
            long curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        return prev1; // O(1) space — only keep last two values
    }

    // =========================================================
    // SECTION 6: Divide and Conquer — MergeSort
    // =========================================================

    /**
     * KEY INSIGHT: Divide-and-conquer template:
     *   1. DIVIDE: split problem into subproblems (usually halves)
     *   2. CONQUER: recursively solve each subproblem
     *   3. COMBINE: merge/aggregate results
     *
     * MergeSort is the canonical D&C sorting algorithm:
     *   DIVIDE: split array in half (O(1) for array indices)
     *   CONQUER: sort each half recursively
     *   COMBINE: merge two sorted halves (O(n))
     *   T(n) = 2T(n/2) + O(n) → O(n log n) — guaranteed (unlike QuickSort)
     *
     * MergeSort is STABLE: equal elements from the left half come before the right.
     *   Merge step: when left[i] == right[j], take from LEFT first → stability preserved.
     *
     * MergeSort needs O(n) EXTRA SPACE for the merge step.
     *   In-place merge is possible but O(n log n) complexity per merge → O(n log^2 n) total.
     *   Not used in practice. TimSort handles this with run detection.
     *
     * APPLICATIONS of merge logic:
     *   - Count inversions (LC 315, LC 493): during merge, count how many right elements
     *     are placed before left elements. Each such placement = one inversion.
     *   - External sort: merge sorted files.
     *   - Merge K sorted arrays (using a heap or recursive merge).
     */
    static void mergeSort(int[] arr, int lo, int hi) {
        if (lo >= hi) return; // base: single element is already sorted

        int mid = lo + (hi - lo) / 2;
        mergeSort(arr, lo, mid);      // CONQUER: sort left half
        mergeSort(arr, mid + 1, hi);  // CONQUER: sort right half
        merge(arr, lo, mid, hi);      // COMBINE: merge sorted halves
    }

    private static void merge(int[] arr, int lo, int mid, int hi) {
        // Copy to temporary array (avoid overwriting elements we haven't processed yet)
        int[] temp = Arrays.copyOfRange(arr, lo, hi + 1);
        int offset = lo; // temp[i - offset] corresponds to arr[i]

        int i = lo, j = mid + 1, k = lo;

        while (i <= mid && j <= hi) {
            // STABILITY: take from left when EQUAL (<=, not <)
            if (temp[i - offset] <= temp[j - offset]) {
                arr[k++] = temp[i++ - offset];
            } else {
                arr[k++] = temp[j++ - offset];
            }
        }
        while (i <= mid) arr[k++] = temp[i++ - offset]; // remaining left elements
        while (j <= hi)  arr[k++] = temp[j++ - offset]; // remaining right elements (already in place)
    }

    // =========================================================
    // SECTION 7: Recursion vs Iteration Decision Guide
    // =========================================================

    /*
     * USE RECURSION WHEN:
     *   - Problem is naturally recursive: trees, graphs, divide-and-conquer
     *   - Recursion tree is small (depth <= 10,000 — default Java stack limit ~500 frames deep
     *     for complex methods, ~10,000 for simple ones)
     *   - Code clarity matters (recursive version is more readable/maintainable)
     *   - Exploring all paths (backtracking, exhaustive search)
     *
     * USE ITERATION WHEN:
     *   - Deep recursion (n > 10,000 with simple recursion) → StackOverflow risk
     *   - Linear DP: tabulation is always safer than recursive memoization for large n
     *   - Performance-critical: iteration avoids function call overhead
     *   - Tail recursion: Java does NOT optimize tail calls (unlike Scala/Haskell)
     *
     * CONVERT RECURSION TO ITERATION:
     *   Use an explicit Stack<> to simulate the call stack.
     *   For DFS on graphs/trees: stack.push(root), while(!stack.empty()) process and push children.
     *   For DP: flip the computation order, fill table[0..n] bottom-up.
     *
     * GOTCHA — Java Stack Limit:
     *   Java's default thread stack size is 256KB to 512KB.
     *   Each stack frame for a method uses memory proportional to local variables.
     *   A simple recursive fibonacci can handle ~5,000-10,000 depth.
     *   A method with many local variables may stackoverflow at 1,000 depth.
     *   Increase with: java -Xss2m MyClass (2MB stack) — valid but not always an option.
     */

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Recursion Patterns                         ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // Subsets
        System.out.println("\n=== Subsets [1,2,3] ===");
        List<List<Integer>> subs = subsets(new int[]{1, 2, 3});
        System.out.println("Recursive: " + subs.size() + " subsets"); // 8
        List<List<Integer>> subsBit = subsetsBitmask(new int[]{1, 2, 3});
        System.out.println("Bitmask:   " + subsBit.size() + " subsets"); // 8

        // Permutations
        System.out.println("\n=== Permutations [1,2,3] ===");
        List<List<Integer>> perms = permutations(new int[]{1, 2, 3});
        System.out.println("Count: " + perms.size()); // 6
        System.out.println("First few: " + perms.subList(0, Math.min(4, perms.size())));

        // Combinations
        System.out.println("\n=== Combinations C(4,2) ===");
        List<List<Integer>> combs = combine(4, 2);
        System.out.println("Count: " + combs.size()); // 6
        System.out.println(combs);

        // Fibonacci comparison
        System.out.println("\n=== Fibonacci n=10 ===");
        System.out.println("Naive:      fib(10)=" + fibNaive(10));           // 55
        System.out.println("Memoized:   fib(10)=" + fibMemo(10));            // 55
        System.out.println("Tabulated:  fib(10)=" + fibTabulation(10));      // 55
        System.out.println("Tabulated:  fib(50)=" + fibTabulation(50));      // 12586269025

        // Tower of Hanoi n=3
        System.out.println("\n=== Tower of Hanoi n=3 ===");
        hanoi(3, 'A', 'C', 'B'); // moves: 2^3 - 1 = 7

        // MergeSort
        System.out.println("\n=== MergeSort ===");
        int[] arr = {5, 2, 8, 1, 9, 3, 7, 4, 6};
        mergeSort(arr, 0, arr.length - 1);
        System.out.println("Sorted: " + Arrays.toString(arr)); // [1,2,3,4,5,6,7,8,9]

        System.out.println("\n=== All recursion tests complete ===");
    }
}
