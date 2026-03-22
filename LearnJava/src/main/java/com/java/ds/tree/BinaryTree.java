package com.java.ds.tree;

import java.util.*;

/**
 * BINARY TREE — Core Data Structure + All Traversals
 *
 * CONCEPT:
 *   A Binary Tree is a tree where each node has at most 2 children (left and right).
 *   Unlike BST, a plain binary tree has no ordering constraint.
 *
 * NODE STRUCTURE:
 *   int val      — the data stored at this node
 *   TreeNode left  — reference to left child (or null)
 *   TreeNode right — reference to right child (or null)
 *
 * TRAVERSALS (crucial to memorize):
 *   In-order  (L → Root → R): DFS, gives sorted order for BST
 *   Pre-order (Root → L → R): DFS, useful for tree serialization/copy
 *   Post-order(L → R → Root): DFS, useful for tree deletion, evaluate expressions
 *   Level-order (BFS):        visits all nodes level by level, uses a Queue
 *
 * INTERVIEW FREQUENCY: Binary trees appear in ~50% of FAANG DS/A screens.
 *   Knowing all 4 traversals is table stakes for Lead/Senior roles.
 *
 * COMMON FOLLOW-UP:
 *   1. "Implement iteratively." → Pre/In/Post all solvable with explicit Stack.
 *   2. "What is the time complexity of each traversal?" → O(n) for all — every node visited once.
 *   3. "What is BFS space complexity?" → O(w) where w = max width (worst case O(n) for full tree).
 *   4. "What is DFS space complexity?" → O(h) where h = height (O(log n) balanced, O(n) skewed).
 *
 * Time: O(n) for all traversals   Space: O(h) DFS stack, O(w) BFS queue
 */
public class BinaryTree {

    // =========================================================================
    // TreeNode — reusable in BinaryTreeProblems.java and BinaryTreeAdvanced.java
    // =========================================================================

    /**
     * Standard binary tree node. Package-accessible so other classes in this
     * package can reuse without importing.
     */
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    public static void main(String[] args) {
        // Build test tree:
        //         1
        //        / \
        //       2   3
        //      / \   \
        //     4   5   6
        //    /
        //   7
        TreeNode root = insertLevelOrder(new int[]{1, 2, 3, 4, 5, 6, 7});

        System.out.println("=== Tree Traversals ===");
        System.out.print("In-order   (L-Root-R): "); inorder(root);   System.out.println();
        System.out.print("Pre-order  (Root-L-R): "); preorder(root);  System.out.println();
        System.out.print("Post-order (L-R-Root): "); postorder(root); System.out.println();
        System.out.print("Level-order (BFS):     "); System.out.println(levelOrder(root));

        System.out.println("\n=== Iterative Traversals ===");
        System.out.println("In-order   (iterative): " + inorderIterative(root));
        System.out.println("Pre-order  (iterative): " + preorderIterative(root));

        System.out.println("\n=== Tree Properties ===");
        System.out.println("Height: " + height(root));           // Expected: 4 (root→2→4→7)
        System.out.println("Is Balanced: " + isBalanced(root));  // Expected: false (7 makes it unbalanced)
        System.out.println("Node Count: " + countNodes(root));   // Expected: 7

        // Balanced tree for isBalanced = true
        TreeNode balanced = insertLevelOrder(new int[]{1, 2, 3, 4, 5, 6});
        System.out.println("Balanced tree isBalanced: " + isBalanced(balanced)); // Expected: true

        System.out.println("\n=== Edge Cases ===");
        System.out.println("Height of null: " + height(null));       // Expected: 0
        System.out.println("Height of single: " + height(new TreeNode(1))); // Expected: 1
        System.out.println("Level-order of null: " + levelOrder(null));
    }

    // =========================================================================
    // CONSTRUCTION
    // =========================================================================

    /**
     * Builds a binary tree from an array in level-order (BFS order).
     * arr[0] = root, arr[1] = root.left, arr[2] = root.right, etc.
     *
     * WHY LEVEL-ORDER INSERTION?
     *   It creates a COMPLETE binary tree (all levels filled left to right).
     *   This is a clean, predictable structure for testing — the same shape as
     *   a heap's internal array representation.
     *
     * ALGORITHM: Use a queue. For each dequeued parent node, assign arr[i] as
     *   left child and arr[i+1] as right child, then enqueue those children.
     */
    public static TreeNode insertLevelOrder(int[] arr) {
        if (arr == null || arr.length == 0) return null;

        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        int i = 1; // Next array index to assign
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();

            // Assign left child
            if (i < arr.length) {
                node.left = new TreeNode(arr[i++]);
                queue.add(node.left);
            }

            // Assign right child
            if (i < arr.length) {
                node.right = new TreeNode(arr[i++]);
                queue.add(node.right);
            }
        }

        return root;
    }

    // =========================================================================
    // TRAVERSALS — Recursive
    // =========================================================================

    /**
     * IN-ORDER: Left → Root → Right
     * KEY USE: Produces SORTED output for a BST — very commonly asked.
     * Recursion: go as far left as possible, print, then go right.
     */
    public static void inorder(TreeNode root) {
        if (root == null) return; // Base case: empty tree / leaf's child
        inorder(root.left);      // Explore entire left subtree first
        System.out.print(root.val + " "); // Visit root BETWEEN left and right
        inorder(root.right);     // Then explore right subtree
    }

    /**
     * PRE-ORDER: Root → Left → Right
     * KEY USE: Tree serialization — reconstruct a tree from its pre-order traversal.
     * The root is always the FIRST element in pre-order output.
     */
    public static void preorder(TreeNode root) {
        if (root == null) return;
        System.out.print(root.val + " "); // Visit root FIRST
        preorder(root.left);
        preorder(root.right);
    }

    /**
     * POST-ORDER: Left → Right → Root
     * KEY USE: Tree deletion (delete children before parent), expression tree evaluation.
     * The root is always the LAST element in post-order output.
     */
    public static void postorder(TreeNode root) {
        if (root == null) return;
        postorder(root.left);
        postorder(root.right);
        System.out.print(root.val + " "); // Visit root LAST
    }

    /**
     * LEVEL-ORDER (BFS): Visit nodes level by level, left to right.
     * KEY USE: Shortest path, finding width, right side view (LC 199).
     *
     * ALGORITHM:
     *   Use a Queue. Enqueue root. While queue is not empty:
     *     Dequeue node, add to result, enqueue its children.
     *   The queue naturally processes nodes level by level because children
     *   are enqueued after their parent and all siblings.
     *
     * Returns a list of lists (each inner list = one level) — needed for LC 102.
     */
    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size(); // Number of nodes at the current level
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);

                // Enqueue children for the NEXT level
                if (node.left != null) queue.add(node.left);
                if (node.right != null) queue.add(node.right);
            }

            result.add(level);
        }

        return result;
    }

    // =========================================================================
    // TRAVERSALS — Iterative (important for "avoid recursion" follow-up)
    // =========================================================================

    /**
     * In-order iterative using an explicit Stack.
     *
     * ALGORITHM: Simulate the call stack manually.
     *   While node != null OR stack not empty:
     *     Go as far left as possible, pushing nodes onto the stack.
     *     Pop a node (we've exhausted its left subtree), record it.
     *     Move to its right subtree.
     *
     * WHY THIS WORKS: The stack holds nodes whose left subtrees are fully processed.
     *   When we pop, we're ready to "visit" that node, then explore its right.
     */
    public static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;

        while (curr != null || !stack.isEmpty()) {
            // Push all left children — go as far left as possible
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }

            // Pop the node whose left subtree is fully processed
            curr = stack.pop();
            result.add(curr.val); // Visit it

            // Move to right subtree (next iteration will push its left children)
            curr = curr.right;
        }

        return result;
    }

    /**
     * Pre-order iterative using a Stack.
     *
     * ALGORITHM: Push root. While stack not empty:
     *   Pop, record it (visit = pre-order means visit BEFORE children).
     *   Push RIGHT child first, then LEFT child.
     *   (Stack is LIFO, so left will be processed before right — correct order.)
     */
    public static List<Integer> preorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val); // Visit BEFORE children (pre-order)

            // Push right first so left is processed first (LIFO)
            if (node.right != null) stack.push(node.right);
            if (node.left != null)  stack.push(node.left);
        }

        return result;
    }

    // =========================================================================
    // TREE PROPERTIES
    // =========================================================================

    /**
     * Height (max depth) of a binary tree.
     *
     * DEFINITION: The height of a node is 1 + max(height(left), height(right)).
     *   Height of null = 0 (empty tree has height 0).
     *   Height of a single node = 1.
     *
     * Note: Some definitions use 0-indexed height (edges not nodes). Clarify in interviews.
     *
     * Time: O(n) — must visit every node to find the deepest path.
     */
    public static int height(TreeNode root) {
        if (root == null) return 0; // Base case: empty subtree contributes 0

        int leftHeight  = height(root.left);   // Height of left subtree
        int rightHeight = height(root.right);  // Height of right subtree

        // This node adds 1 to the taller subtree's height
        return 1 + Math.max(leftHeight, rightHeight);
    }

    /**
     * Checks if the tree is height-balanced.
     *
     * DEFINITION: Balanced = for every node, |height(left) - height(right)| <= 1.
     *
     * EFFICIENT APPROACH (single pass):
     *   Return -1 from the recursive call to signal "unbalanced".
     *   If either subtree is unbalanced (-1), propagate -1 upward immediately.
     *   Otherwise return the actual height.
     *   This avoids recomputing heights — O(n) instead of O(n²) naive approach.
     *
     * NAIVE APPROACH (O(n²)): call height() at every node, check difference.
     *   Avoid this in interviews — shows you don't think about efficiency.
     */
    public static boolean isBalanced(TreeNode root) {
        return checkBalanced(root) != -1;
    }

    /**
     * Returns height of subtree if balanced, or -1 if unbalanced.
     * The -1 sentinel propagates upward without extra work.
     */
    private static int checkBalanced(TreeNode root) {
        if (root == null) return 0; // Base case: null subtree is balanced with height 0

        int leftH = checkBalanced(root.left);
        if (leftH == -1) return -1; // Left subtree is already unbalanced — short-circuit

        int rightH = checkBalanced(root.right);
        if (rightH == -1) return -1; // Right subtree is already unbalanced — short-circuit

        if (Math.abs(leftH - rightH) > 1) return -1; // This node is unbalanced

        return 1 + Math.max(leftH, rightH); // Balanced — return actual height
    }

    /**
     * Counts total number of nodes. O(n) — visits every node.
     */
    public static int countNodes(TreeNode root) {
        if (root == null) return 0;
        return 1 + countNodes(root.left) + countNodes(root.right);
    }
}
