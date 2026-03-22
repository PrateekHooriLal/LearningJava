package com.java.ds.segmenttree;

import java.util.*;

/**
 * FENWICK TREE (Binary Indexed Tree / BIT)
 *
 * CONCEPT:
 *   A Fenwick Tree is a space-efficient data structure for prefix sum queries
 *   and point updates in O(log n). It uses the binary representation of indices
 *   to store partial sums in a clever way.
 *
 * THE LOWEST-SET-BIT TRICK (core of Fenwick Tree):
 *   For a number i, `i & (-i)` isolates the LOWEST (rightmost) set bit.
 *   Example: i=6 = 110 binary → -i = 010 (two's complement) → 6 & (-6) = 010 = 2
 *   Example: i=12 = 1100 → -i = 0100 → 12 & (-12) = 0100 = 4
 *
 * WHAT EACH CELL STORES:
 *   tree[i] stores sum of a range ending at index i.
 *   The range length = i & (-i) (the lowest set bit).
 *   tree[6] (110) = sum of elements at indices [5, 6] (length 2 = lowest bit of 6)
 *   tree[4] (100) = sum of elements at indices [1, 2, 3, 4] (length 4)
 *   tree[8] (1000) = sum of indices [1..8] (length 8)
 *
 * UPDATE (add delta to index i):
 *   Walk UP by adding i & (-i): i → i + (i & -i) → ...
 *   This visits all tree[] cells whose range INCLUDES index i.
 *
 * PREFIX QUERY (sum of [1..i]):
 *   Walk DOWN by removing i & (-i): i → i - (i & -i) → ...
 *   This visits all tree[] cells that compose the prefix [1..i].
 *
 * 1-INDEXED: Fenwick Trees are 1-indexed. Index 0 is a sentinel (unused).
 *   Map 0-indexed input: use index+1 internally.
 *
 * TIME: update O(log n), prefixQuery O(log n), build O(n log n)
 * SPACE: O(n)
 *
 * INTERVIEW ANGLE:
 *   Interviewers use Fenwick Tree as a proxy for "do you know advanced data structures?"
 *   Key questions: "Explain the i & (-i) trick", "Why is it 1-indexed?",
 *   "When would you choose this over Segment Tree?"
 *
 * REAL-WORLD USE:
 *   - Order statistics in databases
 *   - Counting inversions in arrays
 *   - Online rank tracking systems
 */
public class FenwickTree {

    private final int[] tree;
    private final int n;

    public FenwickTree(int n) {
        this.n = n;
        this.tree = new int[n + 1]; // 1-indexed: tree[1..n]
    }

    public FenwickTree(int[] nums) {
        this.n = nums.length;
        this.tree = new int[n + 1];
        for (int i = 0; i < n; i++) {
            update(i + 1, nums[i]); // build by updating each position (1-indexed)
        }
    }
    // NOTE: O(n log n) build. Fenwick Trees can be built in O(n) but the above
    // is simpler and sufficient for most interviews. O(n) build is a follow-up topic.

    // =========================================================================
    // CORE OPERATIONS
    // =========================================================================

    /**
     * UPDATE: add delta to position i (1-indexed).
     *
     * Walk UP the tree: from i, move to parent by adding i & (-i).
     * Each cell visited covers a range that INCLUDES position i.
     *
     * Time: O(log n) — at most log(n) cells updated
     */
    public void update(int i, int delta) {
        for (; i <= n; i += i & (-i)) { // move to next responsible cell
            tree[i] += delta;
        }
    }

    /**
     * PREFIX QUERY: sum of elements at positions 1..i (1-indexed).
     *
     * Walk DOWN the tree: from i, remove the lowest set bit to get the next cell.
     * Each cell visited stores the sum of a contiguous segment ending at its index.
     * Together, they compose the prefix sum [1..i].
     *
     * VISUAL: query(7) = tree[7] + tree[6] + tree[4]
     *   7 = 111 → range [7,7], then 7-1=6
     *   6 = 110 → range [5,6], then 6-2=4
     *   4 = 100 → range [1,4], then 4-4=0 (done)
     *   Total: [7,7] + [5,6] + [1,4] = [1,7] ✓
     *
     * Time: O(log n)
     */
    public int prefixQuery(int i) {
        int sum = 0;
        for (; i > 0; i -= i & (-i)) { // remove lowest set bit to move down
            sum += tree[i];
        }
        return sum;
    }

    /**
     * RANGE QUERY: sum of elements at positions l..r (1-indexed).
     *
     * Range sum = prefix[r] - prefix[l-1].
     * Requires subtraction → the operation must be INVERTIBLE.
     * This is why Fenwick Trees can't directly support range min/max.
     *
     * Time: O(log n)
     */
    public int rangeQuery(int l, int r) {
        return prefixQuery(r) - prefixQuery(l - 1);
    }

    // =========================================================================
    // BIT MANIPULATION VISUAL EXPLANATION
    // =========================================================================
    //
    // Index |  Binary | i & (-i) | Responsible Range
    // ──────────────────────────────────────────────
    //   1   |  0001   |    1     | [1,1]
    //   2   |  0010   |    2     | [1,2]
    //   3   |  0011   |    1     | [3,3]
    //   4   |  0100   |    4     | [1,4]
    //   5   |  0101   |    1     | [5,5]
    //   6   |  0110   |    2     | [5,6]
    //   7   |  0111   |    1     | [7,7]
    //   8   |  1000   |    8     | [1,8]
    //
    // PATTERN: tree[i] covers exactly (i & -i) elements ending at index i.
    //
    // UPDATE path for i=5:
    //   5 (0101) → 5+1=6 (0110) → 6+2=8 (1000) → done
    //   Updates tree[5], tree[6], tree[8] — all ranges that include index 5.
    //
    // QUERY path for i=7:
    //   7 (0111) → 7-1=6 (0110) → 6-2=4 (0100) → 4-4=0 (done)
    //   Sums tree[7], tree[6], tree[4] — three disjoint ranges composing [1,7].

    // =========================================================================
    // LC 315 — COUNT OF SMALLER NUMBERS AFTER SELF
    // =========================================================================

    /**
     * For each element, count how many elements to its RIGHT are smaller.
     *
     * APPROACH: process array from RIGHT to LEFT.
     *   Before inserting nums[i], query how many elements already inserted
     *   (i.e., to the right of i) have value < nums[i].
     *   Then insert nums[i] into the Fenwick Tree.
     *
     * COORDINATE COMPRESSION: values can be up to 10^4 → compress to [1..maxVal].
     *   Map each value to its rank in sorted order.
     *
     * QUERY: "how many elements < nums[i] are in BIT?" = prefix query at rank-1.
     *
     * Time: O(n log n)  Space: O(n) for BIT + O(n) for compression
     */
    public static List<Integer> countSmaller(int[] nums) {
        // coordinate compression
        int[] sorted = nums.clone();
        Arrays.sort(sorted);
        // rank[i] = 1-indexed rank of nums[i] in sorted order
        Map<Integer, Integer> rank = new HashMap<>();
        int r = 1;
        for (int val : sorted) {
            if (!rank.containsKey(val)) rank.put(val, r++);
        }
        int maxRank = r - 1;

        FenwickTree bit = new FenwickTree(maxRank);
        int[] result = new int[nums.length];

        // process right to left
        for (int i = nums.length - 1; i >= 0; i--) {
            int rankI = rank.get(nums[i]);
            // count elements already in BIT with rank < rankI (i.e., value < nums[i])
            result[i] = rankI > 1 ? bit.prefixQuery(rankI - 1) : 0;
            bit.update(rankI, 1); // add nums[i] to BIT
        }

        List<Integer> resList = new ArrayList<>();
        for (int v : result) resList.add(v);
        return resList;
    }
    // WHY RIGHT TO LEFT: we want to count smaller elements that appear AFTER position i.
    //   Processing right-to-left means the BIT always contains only elements we've
    //   already processed (i.e., elements to the RIGHT of current position i).

    // Q: Why coordinate compression?
    // A: Fenwick Tree size = range of values. Values up to 10^4 are manageable without
    //    compression, but values up to 10^9 require compression (can't allocate 10^9 array).
    //    Compression maps values to their sorted rank [1..n], reducing array size to n.

    // =========================================================================
    // COUNTING INVERSIONS
    // =========================================================================

    /**
     * Count the number of inversions in an array.
     * An inversion is a pair (i, j) where i < j but nums[i] > nums[j].
     *
     * Same pattern as LC 315: process left to right,
     * for each element count how many ALREADY inserted elements are GREATER.
     * "Count greater elements in BIT" = total_inserted - prefixQuery(rankI).
     *
     * Time: O(n log n)  Space: O(n)
     */
    public static long countInversions(int[] nums) {
        int[] sorted = nums.clone();
        Arrays.sort(sorted);
        Map<Integer, Integer> rank = new HashMap<>();
        int r = 1;
        for (int val : sorted) {
            if (!rank.containsKey(val)) rank.put(val, r++);
        }
        int maxRank = r - 1;

        FenwickTree bit = new FenwickTree(maxRank);
        long inversions = 0;

        for (int i = 0; i < nums.length; i++) {
            int rankI = rank.get(nums[i]);
            // count already-inserted elements with rank > rankI (i.e., value > nums[i])
            // = total inserted so far - count with rank <= rankI
            int insertedSoFar = i; // i elements inserted before this one
            int notGreater = bit.prefixQuery(rankI); // elements with value <= nums[i]
            inversions += insertedSoFar - notGreater;
            bit.update(rankI, 1);
        }
        return inversions;
    }

    public static void main(String[] args) {
        // Basic Fenwick Tree
        System.out.println("=== Basic Fenwick Tree ===");
        FenwickTree ft = new FenwickTree(new int[]{1, 3, 5, 7, 9, 11});
        System.out.println("Prefix[3]: " + ft.prefixQuery(3));    // 1+3+5=9
        System.out.println("Range[2,4]: " + ft.rangeQuery(2, 4)); // 3+5+7=15
        ft.update(2, 5); // add 5 to position 2 (1-indexed): arr[1] += 5 → 3+5=8
        System.out.println("Range[2,4] after update: " + ft.rangeQuery(2, 4)); // 8+5+7=20

        // LC 315
        System.out.println("\n=== Count Smaller Numbers After Self (LC 315) ===");
        System.out.println(countSmaller(new int[]{5,2,6,1})); // [2,1,1,0]
        System.out.println(countSmaller(new int[]{-1,-1}));   // [0,0]

        // Inversions
        System.out.println("\n=== Count Inversions ===");
        System.out.println(countInversions(new int[]{2,4,1,3,5})); // 3: (2,1),(4,1),(4,3)
        System.out.println(countInversions(new int[]{5,4,3,2,1})); // 10: reverse sorted
    }
}
