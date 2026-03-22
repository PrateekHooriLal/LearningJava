package com.java.ds.dp;

import java.util.*;

/**
 * CONCEPT: Dynamic Programming on Trees (Tree DP)
 * ─────────────────────────────────────────────────
 * Tree DP problems compute an optimal value for each subtree, and the answer
 * for a node depends on answers from its children. This is post-order DFS:
 * process children first, then use their results to compute the current node's value.
 *
 * COMMON PATTERN:
 *   1. Each DFS call returns some value(s) representing the "state" of the subtree.
 *   2. The parent combines children's return values to compute its own state.
 *   3. A global variable tracks the overall answer (often updated at each node).
 *
 * KEY DISTINCTION:
 *   - "Return value" = best we can offer the PARENT (must pass through current node)
 *   - "Global max" = best path/value that may PASS THROUGH or END at current node
 *   These two are often different, which is the core challenge.
 *
 * PROBLEMS COVERED:
 *   - LC 337 House Robber III      — rob or skip each node, no two adjacent
 *   - LC 543 Diameter of Binary Tree — longest path between any two nodes
 *   - LC 124 Binary Tree Max Path Sum — path can go up and then down
 *   - LC 687 Longest Univalue Path   — path where all nodes have same value
 *
 * WHEN TO USE:
 *   "Optimal value in a tree where choice at each node affects parent's options"
 *   Any problem asking for "maximum path" in a binary tree is almost always Tree DP.
 *
 * TIME/SPACE COMPLEXITY: All problems here are O(N) time, O(H) space (call stack)
 *   where N = number of nodes, H = height of tree (O(log N) balanced, O(N) skewed).
 *
 * INTERVIEW ANGLE:
 *   The hardest part is figuring out WHAT to return vs. WHAT to track globally.
 *   Always clarify: "Can the path go through the root? Can it go up and come back down?"
 *
 * REAL-WORLD USE:
 *   - File system optimization (max sum in directory tree)
 *   - Game trees (minimax)
 *   - Organization charts (optimal resource allocation per team)
 */
public class DPOnTrees {

    // ═══════════════════════════════════════════════════════════════════════
    // TreeNode definition
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Standard binary tree node. Used by all problems in this file.
     * INTERVIEW NOTE: LeetCode provides this — you don't define it.
     * In interviews, quickly define it at the top if coding from scratch.
     */
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val; this.left = left; this.right = right;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: LC 337 — HOUSE ROBBER III
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 337 — House Robber III
     *
     * Problem: Rob houses arranged in a binary tree. Cannot rob two directly
     * connected nodes. Maximize total amount robbed.
     *
     * KEY INSIGHT — Return a pair:
     *   int[]{robThis, skipThis} for each subtree:
     *   - robThis  = max money if we DO rob this node
     *   - skipThis = max money if we DON'T rob this node
     *
     * RECURRENCE:
     *   robThis  = node.val + skipLeft + skipRight
     *              (robbing this node means we MUST skip both children)
     *   skipThis = max(robLeft, skipLeft) + max(robRight, skipRight)
     *              (skipping this node, children can choose independently)
     *
     * INTERVIEW Q: Why return both rob and skip values instead of just the max?
     * A: If we only return the max, the parent doesn't know whether the child was
     *    robbed or skipped, so it can't enforce the "no adjacent" constraint.
     *    Returning both preserves enough information for the parent to decide.
     *
     * GOTCHA: Don't use a HashMap<TreeNode, Integer> memoization approach unless
     * you're using the naive recursive approach first. The pair-return approach
     * is cleaner and avoids the HashMap overhead.
     *
     * @param root root of binary tree
     * @return maximum amount that can be robbed
     */
    public static int rob(TreeNode root) {
        int[] result = robHelper(root);
        return Math.max(result[0], result[1]); // take the better of rob/skip root
    }

    /**
     * KEY INSIGHT: Returns int[]{robThis, skipThis}.
     * This is the "state" the parent node needs to make its decision.
     */
    private static int[] robHelper(TreeNode node) {
        if (node == null) return new int[]{0, 0}; // null node: rob=0, skip=0

        int[] left = robHelper(node.left);
        int[] right = robHelper(node.right);

        // Rob this node: cannot rob children, so use skipLeft and skipRight
        int robThis = node.val + left[1] + right[1];
        // Skip this node: children choose optimally (rob or skip)
        int skipThis = Math.max(left[0], left[1]) + Math.max(right[0], right[1]);

        return new int[]{robThis, skipThis};
    }
    /*
     * COMPLEXITY — House Robber III
     * ──────────────────────────────
     * TIME:  O(N) — each node visited exactly once (post-order DFS)
     * SPACE: O(H) — recursion stack, H = tree height (O(log N) to O(N))
     *
     * Compare: naive recursion without pair trick = O(2^N); with memoization = O(N) but O(N) extra space.
     * The pair trick achieves O(N) with O(H) space — optimal.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: LC 543 — DIAMETER OF BINARY TREE
    // ═══════════════════════════════════════════════════════════════════════

    private static int diameterMax; // global tracker for diameter

    /**
     * LC 543 — Diameter of Binary Tree
     *
     * DEFINITION: Diameter = longest path between any two nodes (number of edges).
     * The path does NOT need to pass through the root.
     *
     * KEY INSIGHT — Two different values:
     *   RETURN to parent: max depth (height) of this subtree (single-branch chain)
     *   UPDATE global:    left_depth + right_depth at this node (path THROUGH this node)
     *
     * At each node, the longest path THROUGH this node = leftDepth + rightDepth.
     * We return max(leftDepth, rightDepth) + 1 to the parent (one branch only).
     *
     * INTERVIEW Q: Why can't a path go left, up to parent, and then right in the returned value?
     * A: The returned value is the depth — the longest chain going in ONE direction from this node.
     *    The "both directions" combination is captured in the global update, not the return value.
     *
     * GOTCHA: If the tree is a single node, diameter = 0 (no edges). The depth of a
     * single node is 0 (or 1 if counting nodes — be careful about definition).
     * Here: depth = number of edges, so a leaf returns depth 0.
     *
     * @param root root of binary tree
     * @return diameter (number of edges in longest path)
     */
    public static int diameterOfBinaryTree(TreeNode root) {
        diameterMax = 0;
        depth(root);
        return diameterMax;
    }

    /**
     * KEY INSIGHT: Returns depth (max edges from this node downward).
     * As a side effect, updates diameterMax using both left and right depths.
     */
    private static int depth(TreeNode node) {
        if (node == null) return 0; // null has depth -1 if edges, but 0 works here (leaf returns 0+1-1)

        int leftDepth = depth(node.left);
        int rightDepth = depth(node.right);

        // Path through this node uses both sides: total edges = leftDepth + rightDepth
        diameterMax = Math.max(diameterMax, leftDepth + rightDepth);

        // Return the longer single branch to the parent (+ 1 for the edge to this node)
        return Math.max(leftDepth, rightDepth) + 1;
    }
    /*
     * COMPLEXITY — Diameter
     * ──────────────────────
     * TIME:  O(N) — one DFS pass
     * SPACE: O(H) — recursion stack
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: LC 124 — BINARY TREE MAXIMUM PATH SUM
    // ═══════════════════════════════════════════════════════════════════════

    private static int pathSumMax; // global tracker for max path sum

    /**
     * LC 124 — Binary Tree Maximum Path Sum
     *
     * DEFINITION: A path is any sequence of nodes from some starting node to some
     * ending node along the tree edges. The path must contain at least one node
     * and does NOT need to pass through the root. Values can be negative.
     *
     * KEY INSIGHT — Gain vs. Path Through Node:
     *   "Gain" returned to parent: max contribution this subtree can add to a longer path
     *   = node.val + max(0, leftGain, rightGain)  [take 0 if child's gain is negative]
     *
     *   "Path through this node" (for global update):
     *   = node.val + leftGain + rightGain  (combines both branches)
     *   Note: leftGain and rightGain are already clamped to 0 (negative branches discarded).
     *
     * RETURN TO PARENT: Only ONE branch can extend upward (a path can't fork and come back).
     *   Return: node.val + max(0, leftGain, rightGain)
     *
     * INTERVIEW Q: Why clamp gains at 0 (take max with 0)?
     * A: If a child subtree has negative gain, it's better NOT to include it.
     *    Including a negative-sum subtree would reduce the total path sum.
     *
     * GOTCHA: The path sum for a single node = just node.val (even if negative — the path
     * must include at least one node). Initialize globalMax = Integer.MIN_VALUE, not 0.
     *
     * @param root root of binary tree
     * @return maximum path sum
     */
    public static int maxPathSum(TreeNode root) {
        pathSumMax = Integer.MIN_VALUE; // GOTCHA: use MIN_VALUE, not 0 (all values could be negative)
        maxGain(root);
        return pathSumMax;
    }

    /**
     * KEY INSIGHT: Returns the maximum "gain" this subtree can contribute to a path
     * that continues upward. Negative contributions are replaced with 0 (pruned).
     */
    private static int maxGain(TreeNode node) {
        if (node == null) return 0; // null node contributes 0

        // Get maximum gains from each child; clamp at 0 (discard negative branches)
        int leftGain = Math.max(0, maxGain(node.left));
        int rightGain = Math.max(0, maxGain(node.right));

        // Path that passes THROUGH this node (may be the answer)
        // This path starts somewhere in left subtree, goes through node, ends in right subtree
        int pathThroughNode = node.val + leftGain + rightGain;
        pathSumMax = Math.max(pathSumMax, pathThroughNode);

        // Return to parent: can only extend ONE branch (can't fork and continue upward)
        return node.val + Math.max(leftGain, rightGain);
    }
    /*
     * COMPLEXITY — Binary Tree Max Path Sum
     * ───────────────────────────────────────
     * TIME:  O(N) — visit each node once
     * SPACE: O(H) — recursion stack
     *
     * The key trick is computing the global answer at each node while returning
     * a different (single-branch) value to the parent.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 687 — LONGEST UNIVALUE PATH
    // ═══════════════════════════════════════════════════════════════════════

    private static int univMax; // global tracker for longest univalue path

    /**
     * LC 687 — Longest Univalue Path
     *
     * DEFINITION: Find the length of the longest path where each node has the same value.
     * The path can go through the root OR not. Length = number of EDGES (not nodes).
     *
     * KEY INSIGHT — Only extend if child has the same value:
     *   At each node, check left and right children.
     *   leftLen = leftArm if node.val == left.val, else 0
     *   rightLen = rightArm if node.val == right.val, else 0
     *   (leftArm = result returned by left child's recursive call)
     *
     *   Global update: leftLen + rightLen (path through current node using both arms)
     *   Return to parent: max(leftLen, rightLen) + 1 (one arm only, + edge to parent)
     *
     * CRITICAL DISTINCTION from Diameter (LC 543):
     *   - In Diameter, we always use max depth from children.
     *   - Here, we only use a child's arm if the VALUE matches. If not, that arm = 0.
     *
     * INTERVIEW Q: Can the path have 0 length?
     * A: Yes — a single node has a path of 0 edges. Initialize univMax = 0.
     *
     * @param root root of binary tree
     * @return longest univalue path length (number of edges)
     */
    public static int longestUnivaluePath(TreeNode root) {
        univMax = 0;
        univArm(root);
        return univMax;
    }

    /**
     * KEY INSIGHT: Returns the length of the longest univalue "arm" going downward
     * from this node (single branch, not both). Returns 0 if no arm extends.
     */
    private static int univArm(TreeNode node) {
        if (node == null) return 0;

        int leftArm = univArm(node.left);
        int rightArm = univArm(node.right);

        // Only extend an arm if the child has the same value as current node
        int leftLen = (node.left != null && node.left.val == node.val) ? leftArm + 1 : 0;
        int rightLen = (node.right != null && node.right.val == node.val) ? rightArm + 1 : 0;

        // Path THROUGH this node (global update)
        univMax = Math.max(univMax, leftLen + rightLen);

        // Return to parent: the longer single arm
        return Math.max(leftLen, rightLen);
    }
    /*
     * COMPLEXITY — Longest Univalue Path
     * ────────────────────────────────────
     * TIME:  O(N) — one DFS pass through all N nodes
     * SPACE: O(H) — recursion stack depth
     */

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER: Build tree from level-order array (LeetCode format)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Builds a binary tree from a level-order Integer array where null means no node.
     * Used for testing only.
     */
    static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) return null;
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> q = new LinkedList<>();
        q.offer(root);
        int i = 1;
        while (!q.isEmpty() && i < arr.length) {
            TreeNode curr = q.poll();
            if (i < arr.length && arr[i] != null) {
                curr.left = new TreeNode(arr[i]);
                q.offer(curr.left);
            }
            i++;
            if (i < arr.length && arr[i] != null) {
                curr.right = new TreeNode(arr[i]);
                q.offer(curr.right);
            }
            i++;
        }
        return root;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== LC 337 House Robber III ===");
        //      3
        //     / \
        //    2   3
        //     \   \
        //      3   1
        TreeNode t1 = buildTree(new Integer[]{3, 2, 3, null, 3, null, 1});
        System.out.println("Rob (expected 7): " + rob(t1)); // 3+3+1=7

        //       3
        //      / \
        //     4   5
        //    / \   \
        //   1   3   1
        TreeNode t2 = buildTree(new Integer[]{3, 4, 5, 1, 3, null, 1});
        System.out.println("Rob (expected 9): " + rob(t2)); // 4+5=9

        System.out.println("\n=== LC 543 Diameter of Binary Tree ===");
        //      1
        //     / \
        //    2   3
        //   / \
        //  4   5
        TreeNode t3 = buildTree(new Integer[]{1, 2, 3, 4, 5});
        System.out.println("Diameter (expected 3): " + diameterOfBinaryTree(t3)); // 4-2-1-3 or 4-2-5: length 3

        TreeNode t4 = buildTree(new Integer[]{1, 2});
        System.out.println("Diameter (expected 1): " + diameterOfBinaryTree(t4));

        System.out.println("\n=== LC 124 Binary Tree Max Path Sum ===");
        //   1
        //  / \
        // 2   3
        TreeNode t5 = buildTree(new Integer[]{1, 2, 3});
        System.out.println("MaxPathSum (expected 6): " + maxPathSum(t5)); // 2+1+3=6

        // -3 (all negative — must pick the least negative)
        TreeNode t6 = new TreeNode(-3);
        System.out.println("MaxPathSum (expected -3): " + maxPathSum(t6));

        //   -10
        //   / \
        //  9   20
        //     / \
        //    15   7
        TreeNode t7 = buildTree(new Integer[]{-10, 9, 20, null, null, 15, 7});
        System.out.println("MaxPathSum (expected 42): " + maxPathSum(t7)); // 15+20+7=42

        System.out.println("\n=== LC 687 Longest Univalue Path ===");
        //      5
        //     / \
        //    4   5
        //   / \   \
        //  1   1   5
        TreeNode t8 = buildTree(new Integer[]{5, 4, 5, 1, 1, null, 5});
        System.out.println("LongestUnivaluePath (expected 2): " + longestUnivaluePath(t8)); // 5-5-5

        //      1
        //     / \
        //    4   5
        //   / \   \
        //  4   4   5
        TreeNode t9 = buildTree(new Integer[]{1, 4, 5, 4, 4, null, 5});
        System.out.println("LongestUnivaluePath (expected 2): " + longestUnivaluePath(t9)); // 4-4-4
    }
}
