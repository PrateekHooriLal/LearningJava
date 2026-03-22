package com.java.ds.backtracking;

import java.util.*;

/**
 * BACKTRACKING — The Universal Template for Combinatorial Problems
 *
 * CONCEPT:
 *   Backtracking is a systematic way to explore all possibilities by making choices,
 *   recursing to explore further, and UNDOING the choice (backtracking) if it doesn't work.
 *   It's essentially DFS on the decision tree of all possible solutions.
 *
 * THE TEMPLATE (memorize this):
 *   void backtrack(state, choices) {
 *     if (isComplete(state)) { result.add(copy of state); return; }
 *
 *     for (choice in choices) {
 *       if (isValid(choice)) {         // Pruning: skip invalid choices early
 *         make(choice, state);         // Add to current path
 *         backtrack(state, nextChoices); // Recurse deeper
 *         undo(choice, state);          // Remove from path (backtrack)
 *       }
 *     }
 *   }
 *
 * KEY INSIGHT: The undo step restores the state exactly — the recursive call
 *   explores the subtree as if that choice was the only option, then we undo and try the next.
 *
 * PROBLEMS COVERED:
 *   1. Permutations — LC 46
 *   2. Subsets — LC 78
 *   3. Combination Sum — LC 39 (reuse allowed)
 *   4. Combination Sum II — LC 40 (no reuse, no duplicate combos)
 *   5. N-Queens — LC 51
 *   6. Word Search — LC 79
 *
 * INTERVIEW FREQUENCY: Medium-high at FAANG. Tests recursion mastery.
 *
 * COMMON FOLLOW-UP:
 *   1. "How do you avoid duplicate results?" → Sort + skip same element at same level.
 *   2. "What is the time complexity of backtracking?"
 *      Depends on the problem. Permutations: O(n! × n). Subsets: O(2^n × n).
 *      Always include the copy cost (creating the result list) in the analysis.
 *   3. "How does backtracking differ from dynamic programming?"
 *      DP: overlapping subproblems → cache. Backtracking: exploration without caching.
 */
public class BacktrackingProblems {

    public static void main(String[] args) {

        System.out.println("=== LC 46: Permutations ===");
        System.out.println(permute(new int[]{1, 2, 3}));
        // Expected: [[1,2,3],[1,3,2],[2,1,3],[2,3,1],[3,1,2],[3,2,1]]

        System.out.println("\n=== LC 78: Subsets ===");
        System.out.println(subsets(new int[]{1, 2, 3}));
        // Expected: [[],[1],[2],[1,2],[3],[1,3],[2,3],[1,2,3]]

        System.out.println("\n=== LC 39: Combination Sum (reuse allowed) ===");
        System.out.println(combinationSum(new int[]{2, 3, 6, 7}, 7));
        // Expected: [[2,2,3],[7]]

        System.out.println("\n=== LC 40: Combination Sum II (no reuse, no duplicates) ===");
        System.out.println(combinationSum2(new int[]{10, 1, 2, 7, 6, 1, 5}, 8));
        // Expected: [[1,1,6],[1,2,5],[1,7],[2,6]]

        System.out.println("\n=== LC 51: N-Queens (n=4) ===");
        List<List<String>> queens = solveNQueens(4);
        queens.forEach(board -> { board.forEach(System.out::println); System.out.println(); });

        System.out.println("=== LC 79: Word Search ===");
        char[][] board = {
            {'A','B','C','E'},
            {'S','F','C','S'},
            {'A','D','E','E'}
        };
        System.out.println(exist(board, "ABCCED")); // Expected: true
        System.out.println(exist(board, "SEE"));    // Expected: true
        System.out.println(exist(board, "ABCB"));   // Expected: false
    }

    // =========================================================================
    // LC 46 — Permutations
    // =========================================================================

    /**
     * All permutations of distinct integers.
     *
     * DECISION TREE: At each level, choose which element goes in the next position.
     *   Skip elements already used (track with a boolean[] used array).
     *
     * WHY boolean[] used and not remove from list?
     *   Remove/add from list is O(n). Boolean array is O(1). Both work; array is faster.
     *
     * Total permutations: n! (3 elements → 6 permutations)
     * Time: O(n! × n) — n! permutations, each takes O(n) to copy.  Space: O(n) stack depth.
     */
    public static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackPermute(nums, new boolean[nums.length], new ArrayList<>(), result);
        return result;
    }

    private static void backtrackPermute(int[] nums, boolean[] used, List<Integer> current, List<List<Integer>> result) {
        if (current.size() == nums.length) {
            result.add(new ArrayList<>(current)); // Base case: permutation complete
            return;
        }

        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue; // Skip elements already in current permutation

            used[i] = true;           // Make choice: use nums[i]
            current.add(nums[i]);

            backtrackPermute(nums, used, current, result); // Explore further

            current.remove(current.size() - 1); // Undo: remove nums[i]
            used[i] = false;                    // Mark as available again
        }
    }

    // =========================================================================
    // LC 78 — Subsets
    // =========================================================================

    /**
     * All possible subsets (power set) of distinct integers.
     *
     * APPROACH: At each index, decide INCLUDE or SKIP.
     *   Record the current path at EVERY node (not just leaves) — each path is a valid subset.
     *
     * UNIQUENESS GUARANTEE: By always moving start index forward,
     *   we never revisit earlier elements → no duplicate subsets.
     *
     * Total subsets: 2^n (3 elements → 8 subsets including empty set).
     * Time: O(2^n × n)  Space: O(n) stack depth
     */
    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackSubsets(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
        result.add(new ArrayList<>(current)); // Add current subset BEFORE exploring (captures all intermediate states)

        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);                                      // Include nums[i]
            backtrackSubsets(nums, i + 1, current, result);            // Explore subsets starting from i+1
            current.remove(current.size() - 1);                        // Exclude nums[i] (backtrack)
        }
    }

    // =========================================================================
    // LC 39 — Combination Sum (elements can be reused)
    // =========================================================================

    /**
     * Finds all combinations that sum to target. Each number can be used unlimited times.
     *
     * KEY DIFFERENCE FROM SUBSETS: We pass i (not i+1) in recursion → allows reuse.
     *
     * PRUNING: If remaining < 0, we've exceeded target — prune this branch.
     *   Sorting candidates first enables early termination (break instead of continue).
     *
     * Time: O(N^(T/M + 1)) where N = candidates, T = target, M = min candidate value.
     */
    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates); // Optional: enables pruning optimization
        backtrackCombSum(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackCombSum(int[] candidates, int remaining, int start, List<Integer> current, List<List<Integer>> result) {
        if (remaining == 0) {
            result.add(new ArrayList<>(current)); // Found a valid combination
            return;
        }

        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] > remaining) break; // Pruning: sorted → all further are also too large

            current.add(candidates[i]);
            backtrackCombSum(candidates, remaining - candidates[i], i, current, result); // i, not i+1 → allows reuse
            current.remove(current.size() - 1);
        }
    }

    // =========================================================================
    // LC 40 — Combination Sum II (no reuse, no duplicate combinations)
    // =========================================================================

    /**
     * Each number can be used only once. Input may have duplicates.
     * Result must not contain duplicate combinations.
     *
     * DEDUPLICATION TRICK (crucial):
     *   Sort first. At the same recursion level (same start position), if we see the
     *   same value as the previous iteration, skip it → prevents duplicate combinations.
     *   i.e., if candidates[i] == candidates[i-1] and i > start → skip.
     *
     * WHY i > start (not i > 0)?
     *   If i == start, this is the FIRST time we use this value at this level → allowed.
     *   If i > start and candidates[i] == candidates[i-1], it means we already explored
     *   starting with candidates[i-1] at this level → duplicate → skip.
     */
    public static List<List<Integer>> combinationSum2(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates); // Required for deduplication
        backtrackCombSum2(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrackCombSum2(int[] candidates, int remaining, int start, List<Integer> current, List<List<Integer>> result) {
        if (remaining == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] > remaining) break; // Pruning

            // Deduplication: skip duplicate values at the same recursion level
            if (i > start && candidates[i] == candidates[i - 1]) continue;

            current.add(candidates[i]);
            backtrackCombSum2(candidates, remaining - candidates[i], i + 1, current, result); // i+1 → no reuse
            current.remove(current.size() - 1);
        }
    }

    // =========================================================================
    // LC 51 — N-Queens
    // =========================================================================

    /**
     * Places N queens on an N×N chessboard so no two queens attack each other.
     * Two queens attack if they're on the same row, column, or diagonal.
     *
     * APPROACH:
     *   Place one queen per row (one queen per row guaranteed → track columns and diagonals).
     *   For each row, try each column. If the placement is safe, recurse to next row.
     *   Three boolean arrays track conflicts:
     *     cols[j]        = true if column j is occupied
     *     diag1[row-col+n-1] = true if "/" diagonal is occupied (row-col is constant on "/" diagonals)
     *     diag2[row+col]     = true if "\" diagonal is occupied (row+col is constant on "\" diagonals)
     *
     * Time: O(n!) — n choices for row 0, n-2 for row 1 (after 2 columns eliminated), etc.
     */
    public static List<List<String>> solveNQueens(int n) {
        List<List<String>> result = new ArrayList<>();
        char[][] board = new char[n][n];
        for (char[] row : board) Arrays.fill(row, '.'); // Initialize empty board

        boolean[] cols  = new boolean[n];
        boolean[] diag1 = new boolean[2 * n - 1]; // "/" diagonals: index = row - col + (n-1)
        boolean[] diag2 = new boolean[2 * n - 1]; // "\" diagonals: index = row + col

        backtrackQueens(board, 0, n, cols, diag1, diag2, result);
        return result;
    }

    private static void backtrackQueens(char[][] board, int row, int n, boolean[] cols, boolean[] diag1, boolean[] diag2, List<List<String>> result) {
        if (row == n) {
            // All n queens placed successfully — convert board to string list
            List<String> solution = new ArrayList<>();
            for (char[] r : board) solution.add(new String(r));
            result.add(solution);
            return;
        }

        for (int col = 0; col < n; col++) {
            int d1 = row - col + n - 1;
            int d2 = row + col;

            // Check if this position is under attack
            if (cols[col] || diag1[d1] || diag2[d2]) continue;

            // Place queen
            board[row][col] = 'Q';
            cols[col] = diag1[d1] = diag2[d2] = true;

            backtrackQueens(board, row + 1, n, cols, diag1, diag2, result);

            // Remove queen (backtrack)
            board[row][col] = '.';
            cols[col] = diag1[d1] = diag2[d2] = false;
        }
    }

    // =========================================================================
    // LC 79 — Word Search
    // =========================================================================

    /**
     * Finds if a word exists in a 2D grid by adjacent (up/down/left/right) cells.
     * Each cell can only be used once per path.
     *
     * DFS + BACKTRACKING:
     *   Start from any cell matching word[0]. DFS in all 4 directions.
     *   Mark visited cells temporarily (cell = '#') to prevent revisiting.
     *   Restore after returning from recursion (backtrack: cell = original char).
     *
     * PRUNING: If current cell doesn't match expected character → return false immediately.
     *
     * Time: O(m × n × 4^L) where L = word length (4 choices at each step).
     * Space: O(L) recursion stack.
     */
    public static boolean exist(char[][] board, String word) {
        int m = board.length, n = board[0].length;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == word.charAt(0) && dfsWordSearch(board, i, j, word, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final int[][] DIRS = {{0,1},{0,-1},{1,0},{-1,0}}; // right, left, down, up

    private static boolean dfsWordSearch(char[][] board, int r, int c, String word, int idx) {
        if (idx == word.length()) return true; // All characters matched

        int m = board.length, n = board[0].length;
        if (r < 0 || r >= m || c < 0 || c >= n) return false; // Out of bounds
        if (board[r][c] != word.charAt(idx)) return false;      // Character mismatch

        char temp = board[r][c];
        board[r][c] = '#'; // Mark as visited (in-place — avoids extra boolean[][] array)

        for (int[] dir : DIRS) {
            if (dfsWordSearch(board, r + dir[0], c + dir[1], word, idx + 1)) {
                board[r][c] = temp; // Restore before returning true (good practice)
                return true;
            }
        }

        board[r][c] = temp; // Restore: unmark (backtrack)
        return false;
    }
}
