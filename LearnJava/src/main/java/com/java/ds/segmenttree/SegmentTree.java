package com.java.ds.segmenttree;

import java.util.*;

/**
 * SEGMENT TREE — Range Query with Point/Range Updates
 *
 * CONCEPT:
 *   A Segment Tree is a full binary tree where each node stores an aggregate
 *   (sum, min, max) over a contiguous subarray (segment) of the input.
 *   - Leaf nodes: individual elements
 *   - Internal node: aggregate of its children's ranges
 *   - Root: aggregate of the entire array
 *
 * WHEN TO USE:
 *   - Multiple range queries AND updates on the same array
 *   - If only queries, prefix sums are simpler. If only updates, direct access.
 *   - Both operations: Segment Tree O(log n) each vs O(n) naive for query or update.
 *
 * INDEX ARITHMETIC (1-indexed array representation):
 *   node i → left child: 2*i, right child: 2*i+1, parent: i/2
 *   Tree array size: 4*n (safe upper bound for any input array of size n)
 *   Why 4*n? For n elements, the tree has at most 4n nodes due to padding at leaves.
 *
 * TIME COMPLEXITY:
 *   build:       O(n)
 *   point update: O(log n)
 *   range query: O(log n)
 *   range update with lazy: O(log n)
 *
 * SPACE COMPLEXITY: O(n) — tree array of size 4n
 *
 * INTERVIEW ANGLE:
 *   Segment Trees test your ability to design recursive tree structures with
 *   careful index management. Key questions:
 *   - "What if you need range updates instead of point updates?" → Lazy propagation
 *   - "Fenwick Tree vs Segment Tree — when would you pick each?" (see comparison)
 *   - "Can you do range min/max?" → Yes, same structure, change aggregate function
 *
 * REAL-WORLD USE:
 *   - Database indexing for range queries
 *   - Computational geometry (line segment intersection)
 *   - Competitive programming (very common)
 */
public class SegmentTree {

    // =========================================================================
    // BASIC SEGMENT TREE (POINT UPDATE + RANGE SUM QUERY)
    // =========================================================================

    private int[] tree;
    private int n;

    public SegmentTree(int[] nums) {
        n = nums.length;
        tree = new int[4 * n]; // 4*n is a safe upper bound
        if (n > 0) build(nums, 1, 0, n - 1);
    }

    /**
     * Build the segment tree from the input array.
     *
     * node: current tree node index (1-indexed)
     * lo, hi: range of the input array this node covers
     *
     * Base case: leaf node (lo == hi) → tree[node] = nums[lo]
     * Recursive: build left half, build right half, merge
     *
     * Time: O(n) — visits each element once
     */
    private void build(int[] nums, int node, int lo, int hi) {
        if (lo == hi) {
            tree[node] = nums[lo]; // leaf: store the element
            return;
        }
        int mid = lo + (hi - lo) / 2;        // avoid overflow
        build(nums, 2 * node, lo, mid);       // build left subtree
        build(nums, 2 * node + 1, mid + 1, hi); // build right subtree
        tree[node] = tree[2 * node] + tree[2 * node + 1]; // merge: sum of children
    }

    /**
     * POINT UPDATE: change nums[idx] to val.
     *
     * Walk down to the leaf, update it, then update all ancestors on the way back up.
     * "Update ancestors on the way back up" = the tree[node] = children sum line.
     *
     * Time: O(log n) — path from root to leaf
     */
    public void update(int idx, int val, int node, int lo, int hi) {
        if (lo == hi) {
            tree[node] = val; // found the leaf
            return;
        }
        int mid = lo + (hi - lo) / 2;
        if (idx <= mid) update(idx, val, 2 * node, lo, mid);         // update left child
        else            update(idx, val, 2 * node + 1, mid + 1, hi); // update right child
        tree[node] = tree[2 * node] + tree[2 * node + 1]; // re-merge after child update
    }

    public void update(int idx, int val) { update(idx, val, 1, 0, n - 1); }

    /**
     * RANGE SUM QUERY: sum of nums[l..r].
     *
     * Three cases for each recursive call:
     * 1. Current range [lo, hi] is COMPLETELY OUTSIDE [l, r] → return 0 (identity for sum)
     * 2. Current range [lo, hi] is COMPLETELY INSIDE [l, r]  → return tree[node] (fully covered)
     * 3. PARTIAL overlap → recurse into both children and sum results
     *
     * Time: O(log n) — at most 4 nodes per level are "active" during traversal
     * WHY: at any level, only the boundary nodes overlap partially; interior nodes are fully covered.
     */
    public int query(int l, int r, int node, int lo, int hi) {
        if (r < lo || hi < l) return 0;              // case 1: completely outside
        if (l <= lo && hi <= r) return tree[node];   // case 2: completely inside
        int mid = lo + (hi - lo) / 2;                // case 3: partial overlap
        return query(l, r, 2 * node, lo, mid)
             + query(l, r, 2 * node + 1, mid + 1, hi);
    }

    public int query(int l, int r) { return query(l, r, 1, 0, n - 1); }

    // =========================================================================
    // LAZY PROPAGATION (RANGE UPDATE + RANGE SUM QUERY)
    // =========================================================================

    /**
     * PROBLEM WITH NAIVE RANGE UPDATE:
     *   If we update all points in [l, r] one by one → O(n log n).
     *   Lazy propagation defers updates, applying them only when needed → O(log n).
     *
     * KEY IDEA:
     *   Store "pending updates" in a lazy[] array. When a node is "fully covered"
     *   by an update, DON'T update its children immediately. Instead, store the
     *   pending update in lazy[node]. When later querying or updating, "push down"
     *   (propagate) the lazy update to children before proceeding.
     *
     * LAZY INVARIANT: lazy[node] = pending "add delta" that must be applied to
     *   all elements in this node's range, but hasn't been pushed to children yet.
     *
     * tree[node] IS kept current (reflects the lazy update), but children are NOT
     * updated until we need to recurse into them (pushDown).
     */
    static class LazySegTree {
        private final int[] tree, lazy;
        private final int n;

        public LazySegTree(int[] nums) {
            n = nums.length;
            tree = new int[4 * n];
            lazy = new int[4 * n]; // all zeros initially — no pending updates
            build(nums, 1, 0, n - 1);
        }

        private void build(int[] nums, int node, int lo, int hi) {
            if (lo == hi) { tree[node] = nums[lo]; return; }
            int mid = lo + (hi - lo) / 2;
            build(nums, 2 * node, lo, mid);
            build(nums, 2 * node + 1, mid + 1, hi);
            tree[node] = tree[2 * node] + tree[2 * node + 1];
        }

        /**
         * PUSH DOWN: propagate pending lazy update to children.
         *
         * Called before recursing into children (to ensure children are up-to-date).
         * Updates children's tree values AND their lazy arrays.
         * Clears the current node's lazy (it's been passed down).
         *
         * This is the HEART of lazy propagation.
         */
        private void pushDown(int node, int lo, int hi) {
            if (lazy[node] == 0) return; // nothing pending

            int mid = lo + (hi - lo) / 2;
            // apply pending delta to left child
            tree[2 * node] += lazy[node] * (mid - lo + 1); // each element in range gets +delta
            lazy[2 * node] += lazy[node];                   // pass pending update down

            // apply pending delta to right child
            tree[2 * node + 1] += lazy[node] * (hi - mid);
            lazy[2 * node + 1] += lazy[node];

            lazy[node] = 0; // clear: it's been pushed down
        }

        /**
         * RANGE UPDATE: add delta to all elements in [l, r].
         *
         * If fully covered → update tree[node] directly, store delta in lazy[node].
         * Else → pushDown, recurse into children, then re-merge.
         *
         * Time: O(log n)
         */
        public void rangeUpdate(int l, int r, int delta, int node, int lo, int hi) {
            if (r < lo || hi < l) return; // completely outside
            if (l <= lo && hi <= r) {
                // fully covered — apply and lazify
                tree[node] += delta * (hi - lo + 1); // update this node's sum
                lazy[node] += delta;                  // mark pending for children
                return;
            }
            pushDown(node, lo, hi);  // partial overlap: push before recursing
            int mid = lo + (hi - lo) / 2;
            rangeUpdate(l, r, delta, 2 * node, lo, mid);
            rangeUpdate(l, r, delta, 2 * node + 1, mid + 1, hi);
            tree[node] = tree[2 * node] + tree[2 * node + 1]; // re-merge
        }

        public void rangeUpdate(int l, int r, int delta) { rangeUpdate(l, r, delta, 1, 0, n - 1); }

        /**
         * RANGE QUERY with lazy propagation.
         *
         * Push down before recursing to ensure children have up-to-date values.
         * Fully covered case: return tree[node] directly (already includes lazy updates).
         *
         * Time: O(log n)
         */
        public int rangeQuery(int l, int r, int node, int lo, int hi) {
            if (r < lo || hi < l) return 0;
            if (l <= lo && hi <= r) return tree[node]; // already current
            pushDown(node, lo, hi); // ensure children are up-to-date
            int mid = lo + (hi - lo) / 2;
            return rangeQuery(l, r, 2 * node, lo, mid)
                 + rangeQuery(l, r, 2 * node + 1, mid + 1, hi);
        }

        public int rangeQuery(int l, int r) { return rangeQuery(l, r, 1, 0, n - 1); }
    }

    // =========================================================================
    // LC 307 — RANGE SUM QUERY (MUTABLE)
    // =========================================================================

    /**
     * NumArray supports:
     *   update(i, val): change nums[i] to val
     *   sumRange(l, r): return sum of nums[l..r]
     *
     * ALTERNATIVES COMPARED:
     *   Brute force: O(1) update, O(n) query
     *   Prefix sum: O(n) update (must rebuild prefix), O(1) query
     *   Segment Tree: O(log n) update, O(log n) query — BEST when both operations frequent
     *   Fenwick Tree: O(log n) update, O(log n) query — simpler code than Segment Tree
     *
     * WHEN TO CHOOSE SEGMENT TREE OVER FENWICK:
     *   Segment Tree wins when: range min/max (Fenwick only does prefix sums),
     *   lazy range updates, or more complex merge operations.
     *   Fenwick wins when: only prefix sums needed, want simpler code.
     */
    static class NumArray {
        private final SegmentTree st;
        private final int n;

        public NumArray(int[] nums) {
            n = nums.length;
            st = new SegmentTree(nums);
        }

        public void update(int idx, int val) {
            st.update(idx, val);
        }

        public int sumRange(int left, int right) {
            return st.query(left, right);
        }
    }

    // =========================================================================
    // SEGMENT TREE vs FENWICK TREE COMPARISON
    // =========================================================================
    //
    // Feature                  | Segment Tree          | Fenwick Tree (BIT)
    // ─────────────────────────────────────────────────────────────────────
    // Build                    | O(n)                  | O(n log n)
    // Point update             | O(log n)              | O(log n)
    // Range query              | O(log n)              | O(log n)
    // Range update             | O(log n) with lazy    | O(log n) with diff array
    // Range min/max query      | YES                   | No (not directly)
    // Code complexity          | Medium (50-80 lines)  | Simple (10-15 lines)
    // Space                    | O(n) — 4n array       | O(n) — n+1 array
    // Non-invertible ops       | YES (any associative) | Requires invertible ops
    //
    // INTERVIEW RULE OF THUMB:
    //   "Need range sum + point update only" → Fenwick (simpler)
    //   "Need range min/max" OR "lazy range updates" → Segment Tree
    //
    // Q: What does "non-invertible" mean for Fenwick?
    // A: Fenwick prefix query gives prefix[r] - prefix[l-1]. This requires
    //    the merge operation to be "invertible" (like subtraction for sums).
    //    Min/max can't be computed this way (min(l..r) != min(0..r) - min(0..l-1)).
    //    Segment Tree stores the merge result at each node → no inversion needed.

    public static void main(String[] args) {
        // Basic Segment Tree
        System.out.println("=== Basic Segment Tree (Point Update + Range Sum) ===");
        int[] nums = {1, 3, 5, 7, 9, 11};
        SegmentTree st = new SegmentTree(nums);
        System.out.println("Sum [1,3]: " + st.query(1, 3));  // 3+5+7=15
        st.update(1, 2);                                       // nums[1] = 2
        System.out.println("Sum [1,3] after update: " + st.query(1, 3)); // 2+5+7=14

        // Lazy Segment Tree
        System.out.println("\n=== Lazy Segment Tree (Range Update + Range Sum) ===");
        int[] nums2 = {1, 2, 3, 4, 5};
        LazySegTree lst = new LazySegTree(nums2);
        System.out.println("Sum [1,3]: " + lst.rangeQuery(1, 3));    // 2+3+4=9
        lst.rangeUpdate(1, 3, 10);                                    // add 10 to [1..3]
        System.out.println("Sum [1,3] after +10: " + lst.rangeQuery(1, 3)); // 12+13+14=39
        System.out.println("Sum [0,4]: " + lst.rangeQuery(0, 4));    // 1+12+13+14+5=45

        // NumArray (LC 307)
        System.out.println("\n=== NumArray (LC 307) ===");
        NumArray na = new NumArray(new int[]{1,3,5});
        System.out.println(na.sumRange(0, 2)); // 9
        na.update(1, 2);
        System.out.println(na.sumRange(0, 2)); // 8
    }
}
