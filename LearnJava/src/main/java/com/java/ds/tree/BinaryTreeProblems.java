package com.java.ds.tree;

import java.util.*;

/**
 * BINARY TREE PROBLEMS — LC Classic Set
 *
 * PROBLEMS COVERED:
 *   1. Max Depth / Height (LC 104)
 *   2. Invert Binary Tree (LC 226)
 *   3. Same Tree (LC 100)
 *   4. Has Path Sum (LC 112)
 *   5. Symmetric Tree (LC 101)
 *   6. Diameter of Binary Tree (LC 543)
 *
 * SHARED PATTERN — DFS Post-order:
 *   Most tree problems follow the same structural recursion:
 *   1. Base case: if root == null, return the "empty" answer
 *   2. Recurse on left subtree
 *   3. Recurse on right subtree
 *   4. Combine left and right answers at the current node
 *
 *   This pattern is called "post-order" because we process children BEFORE
 *   using their results at the parent.
 *
 * INTERVIEW FREQUENCY: All 6 problems are Leetcode Easy/Medium frequently
 *   asked in first rounds. Interviewers use them to test recursion comfort.
 *
 * Time: O(n) for all — must visit every node.
 * Space: O(h) — recursion stack, where h = height (O(log n) balanced, O(n) skewed).
 */
public class BinaryTreeProblems {

    // Reuse TreeNode from BinaryTree.java (same package)
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val; this.left = left; this.right = right;
        }
    }

    // Helper to build a tree from level-order array (null = missing node)
    static TreeNode build(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) return null;
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        int i = 1;
        while (!q.isEmpty() && i < arr.length) {
            TreeNode node = q.poll();
            if (i < arr.length && arr[i] != null) { node.left = new TreeNode(arr[i]); q.add(node.left); }
            i++;
            if (i < arr.length && arr[i] != null) { node.right = new TreeNode(arr[i]); q.add(node.right); }
            i++;
        }
        return root;
    }

    public static void main(String[] args) {
        //        3
        //       / \
        //      9  20
        //        /  \
        //       15   7
        TreeNode t1 = build(new Integer[]{3, 9, 20, null, null, 15, 7});

        System.out.println("=== LC 104: Max Depth ===");
        System.out.println(maxDepth(t1));         // Expected: 3
        System.out.println(maxDepth(null));        // Expected: 0

        System.out.println("\n=== LC 226: Invert Binary Tree ===");
        TreeNode t2 = build(new Integer[]{4, 2, 7, 1, 3, 6, 9});
        System.out.println("Before: " + levelOrderFlat(t2)); // [4,2,7,1,3,6,9]
        invertTree(t2);
        System.out.println("After:  " + levelOrderFlat(t2)); // [4,7,2,9,6,3,1]

        System.out.println("\n=== LC 100: Same Tree ===");
        TreeNode a = build(new Integer[]{1, 2, 3});
        TreeNode b = build(new Integer[]{1, 2, 3});
        TreeNode c = build(new Integer[]{1, 2, null});
        System.out.println(isSameTree(a, b)); // Expected: true
        System.out.println(isSameTree(a, c)); // Expected: false
        System.out.println(isSameTree(null, null)); // Expected: true

        System.out.println("\n=== LC 112: Has Path Sum ===");
        //       5
        //      / \
        //     4   8
        //    /   / \
        //   11  13   4
        //  /  \       \
        // 7    2       1
        TreeNode t3 = build(new Integer[]{5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1});
        System.out.println(hasPathSum(t3, 22)); // 5→4→11→2 = 22. Expected: true
        System.out.println(hasPathSum(t3, 27)); // 5→8→4→1 = 18, not 27. Expected: false
        System.out.println(hasPathSum(null, 0)); // Expected: false

        System.out.println("\n=== LC 101: Symmetric Tree ===");
        TreeNode sym1 = build(new Integer[]{1, 2, 2, 3, 4, 4, 3});
        TreeNode sym2 = build(new Integer[]{1, 2, 2, null, 3, null, 3});
        System.out.println(isSymmetric(sym1)); // Expected: true
        System.out.println(isSymmetric(sym2)); // Expected: false

        System.out.println("\n=== LC 543: Diameter of Binary Tree ===");
        TreeNode d1 = build(new Integer[]{1, 2, 3, 4, 5});
        System.out.println(diameterOfBinaryTree(d1)); // Expected: 3 (path 4→2→1→3 or 5→2→1→3)
        System.out.println(diameterOfBinaryTree(build(new Integer[]{1, 2}))); // Expected: 1
    }

    // =========================================================================
    // LC 104 — Maximum Depth of Binary Tree
    // =========================================================================

    /**
     * Returns the maximum depth (number of nodes along the longest root-to-leaf path).
     *
     * RECURSION PATTERN (post-order):
     *   The depth of a tree = 1 + max(depth of left subtree, depth of right subtree).
     *   Base: empty tree has depth 0.
     *
     * IDENTICAL TO height() in BinaryTree.java — max depth == height.
     */
    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;                              // Base case
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    // =========================================================================
    // LC 226 — Invert Binary Tree
    // =========================================================================

    /**
     * Inverts (mirrors) a binary tree in-place.
     *
     * APPROACH: At every node, swap the left and right children.
     *   Then recursively invert the subtrees (post-order or pre-order both work).
     *
     * INTERVIEW STORY: This problem is "famous" because Max Howell (creator of Homebrew)
     *   was rejected by Google for not solving it. Used as an example of interview absurdity.
     *   Interviewers now use it as an easy warm-up — don't overthink it.
     *
     * @return the root of the inverted tree (same root, structure changed in place)
     */
    public static TreeNode invertTree(TreeNode root) {
        if (root == null) return null; // Base case: empty subtree, nothing to invert

        // Swap left and right children (pre-order: process node before recursing)
        TreeNode temp = root.left;
        root.left = root.right;
        root.right = temp;

        // Recursively invert both subtrees
        invertTree(root.left);
        invertTree(root.right);

        return root; // Return root for chaining
    }

    // =========================================================================
    // LC 100 — Same Tree
    // =========================================================================

    /**
     * Returns true if two trees are structurally identical with the same node values.
     *
     * RECURSION:
     *   Two trees are the same if:
     *   1. Both are null (both empty → identical)
     *   2. Neither is null AND root values match AND left subtrees match AND right subtrees match
     *
     *   If one is null and the other isn't → false (structural mismatch).
     */
    public static boolean isSameTree(TreeNode p, TreeNode q) {
        // Both null → same empty trees
        if (p == null && q == null) return true;

        // One null, one not → structural mismatch
        if (p == null || q == null) return false;

        // Values must match, and recursively both subtrees must match
        return p.val == q.val
                && isSameTree(p.left, q.left)
                && isSameTree(p.right, q.right);
    }

    // =========================================================================
    // LC 112 — Path Sum
    // =========================================================================

    /**
     * Returns true if there is a root-to-leaf path where all node values sum to targetSum.
     *
     * KEY: Must reach a LEAF (node with no children) — not just any node.
     *
     * RECURSION:
     *   Subtract current node's value from targetSum at each step.
     *   At a leaf: check if remaining sum == 0.
     *
     * WHY NOT check at null node?
     *   If we checked at null and targetSum == 0, we'd incorrectly return true
     *   for single-child nodes (the null child would satisfy the check even though
     *   the path isn't root-to-leaf).
     */
    public static boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) return false; // Empty tree or went past a leaf — no path

        // We're at a leaf AND remaining sum == current node's value
        if (root.left == null && root.right == null && root.val == targetSum) {
            return true;
        }

        // Subtract current value and recurse on both children
        // (either child can complete the path)
        return hasPathSum(root.left, targetSum - root.val)
                || hasPathSum(root.right, targetSum - root.val);
    }

    // =========================================================================
    // LC 101 — Symmetric Tree
    // =========================================================================

    /**
     * Returns true if the tree is a mirror of itself (symmetric around its center).
     *
     * APPROACH: Check if left subtree and right subtree are mirrors of each other.
     *   Two trees are mirrors if:
     *   1. Their roots have the same value
     *   2. Left's right subtree mirrors Right's left subtree (and vice versa)
     */
    public static boolean isSymmetric(TreeNode root) {
        if (root == null) return true;
        return isMirror(root.left, root.right);
    }

    private static boolean isMirror(TreeNode left, TreeNode right) {
        if (left == null && right == null) return true;   // Both null → symmetric
        if (left == null || right == null) return false;  // One null → not symmetric

        return left.val == right.val                       // Values match
                && isMirror(left.left, right.right)        // Outer pair matches
                && isMirror(left.right, right.left);       // Inner pair matches
    }

    // =========================================================================
    // LC 543 — Diameter of Binary Tree
    // =========================================================================

    private static int maxDiameter; // Global max tracked during DFS

    /**
     * Returns the length of the longest path between any two nodes.
     * The path may or may not pass through the root.
     *
     * INSIGHT: The diameter through a node = height(left) + height(right).
     *   We want the maximum of this across all nodes.
     *
     * APPROACH: Post-order DFS. At each node, compute left and right heights.
     *   Update global max with left + right (diameter through this node).
     *   Return height of this subtree (for parent to use).
     *
     * WHY NOT just compute height at every node?
     *   That's O(n²) — height() is called O(n) times, each taking O(n).
     *   The DFS approach computes everything in one O(n) pass.
     */
    public static int diameterOfBinaryTree(TreeNode root) {
        maxDiameter = 0;
        dfsHeight(root);
        return maxDiameter;
    }

    private static int dfsHeight(TreeNode root) {
        if (root == null) return 0;

        int leftH = dfsHeight(root.left);    // Height of left subtree
        int rightH = dfsHeight(root.right);  // Height of right subtree

        // Diameter through this node = leftH + rightH (path goes through this node)
        maxDiameter = Math.max(maxDiameter, leftH + rightH);

        return 1 + Math.max(leftH, rightH); // Return height of this subtree
    }

    // Helper: flatten level-order to a list for printing
    static List<Integer> levelOrderFlat(TreeNode root) {
        List<Integer> res = new ArrayList<>();
        if (root == null) return res;
        Queue<TreeNode> q = new LinkedList<>();
        q.add(root);
        while (!q.isEmpty()) {
            TreeNode n = q.poll();
            res.add(n.val);
            if (n.left != null) q.add(n.left);
            if (n.right != null) q.add(n.right);
        }
        return res;
    }
}
