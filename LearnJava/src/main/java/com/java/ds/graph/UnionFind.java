package com.java.ds.graph;

/**
 * UNION-FIND (Disjoint Set Union — DSU)
 *
 * WHAT IS IT?
 *   A data structure that efficiently tracks which elements belong to the same group (component).
 *   Supports two operations:
 *   - find(x):  Returns the "root" representative of x's group. O(α(n)) ≈ O(1).
 *   - union(x,y): Merges the groups containing x and y.
 *
 * TWO OPTIMIZATIONS (both required in practice):
 *   1. PATH COMPRESSION (in find):
 *      While finding root, point all nodes directly to the root.
 *      Flattens the tree — future find() calls are O(1).
 *   2. UNION BY RANK (in union):
 *      Always attach the smaller tree under the larger tree's root.
 *      Keeps trees shallow — prevents the tree from degenerating into a linked list.
 *   With both: O(α(n)) per operation, where α is the inverse Ackermann function ≈ O(1) practically.
 *
 * REAL-WORLD USES:
 *   - Kruskal's Minimum Spanning Tree algorithm
 *   - Detecting cycles in undirected graphs
 *   - Network connectivity problems
 *   - Image processing (connected components)
 *
 * PROBLEMS COVERED:
 *   1. Number of Connected Components — LC 323
 *   2. Redundant Connection (detect cycle) — LC 684
 *
 * INTERVIEW FREQUENCY: Medium-high. Often paired with graph problems.
 *
 * COMMON QUESTIONS:
 *   1. "What is Union-Find used for?"
 *      Dynamic connectivity: efficiently answer "are A and B in the same group?"
 *   2. "What is path compression?"
 *      While finding root, attach all nodes directly to the root → flat tree.
 *   3. "What is union by rank?"
 *      Attach smaller tree under larger tree's root to keep height small.
 *   4. "What is α(n)?" → Inverse Ackermann function. Grows so slowly it's ≈ constant.
 */
public class UnionFind {

    private int[] parent; // parent[i] = parent of node i (parent[i] == i if i is root)
    private int[] rank;   // rank[i] = approximate height of subtree rooted at i
    private int components; // Number of connected components

    /**
     * Initializes n separate components (each node is its own root).
     * Initially: parent[i] = i, rank[i] = 0 for all i.
     */
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        components = n; // Start with n separate components

        for (int i = 0; i < n; i++) {
            parent[i] = i; // Each node is its own parent (self-loop)
            // rank is 0 by default
        }
    }

    /**
     * Finds the root (representative) of the component containing x.
     *
     * PATH COMPRESSION:
     *   While traversing to the root, set each visited node's parent directly to the root.
     *   This flattens the tree so future find() calls are nearly O(1).
     *
     *   EXAMPLE: Before compression: 1→2→3→4 (root=4, depth 3)
     *            After compression:  1→4, 2→4, 3→4 (all point to root directly)
     *
     * TWO-PASS vs ONE-PASS COMPRESSION:
     *   This uses one-pass (recursive) path compression. Iterative with explicit path save
     *   is also valid. Both achieve the same result.
     */
    public int find(int x) {
        if (parent[x] != x) {
            // Path compression: recursively find root AND update parent to root directly
            parent[x] = find(parent[x]);
        }
        return parent[x]; // Return the root
    }

    /**
     * Merges the components containing x and y.
     * Returns false if they're already in the same component.
     *
     * UNION BY RANK:
     *   The rank is an upper bound on the tree height.
     *   Always attach the lower-rank tree under the higher-rank tree's root.
     *   If ranks are equal, arbitrarily pick one root and increment its rank.
     *
     *   WHY? Without this, repeated unions can create a long chain (degenerate tree),
     *   making find() O(n). Union by rank keeps height O(log n) even without path compression.
     *   With path compression AND union by rank: O(α(n)) amortized.
     */
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX == rootY) return false; // Already in the same component

        // Attach lower-rank root under higher-rank root
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY; // rootX's tree is shorter → attach it under rootY
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX; // rootY's tree is shorter → attach under rootX
        } else {
            // Equal rank: arbitrary choice — attach rootY under rootX and increment rootX's rank
            parent[rootY] = rootX;
            rank[rootX]++;         // rootX's tree height may have increased by 1
        }

        components--; // Two separate components merged into one
        return true;
    }

    /** Returns true if x and y are in the same component */
    public boolean connected(int x, int y) {
        return find(x) == find(y); // Same root → same component
    }

    /** Returns current number of connected components */
    public int getComponents() {
        return components;
    }

    // =========================================================================
    // LC 323 — Number of Connected Components in Undirected Graph
    // =========================================================================

    /**
     * Given n nodes and a list of edges, return the number of connected components.
     *
     * UNION-FIND APPROACH:
     *   Initialize n components. For each edge (u, v): union(u, v).
     *   Each successful union reduces the component count by 1.
     *   Final component count = answer.
     *
     * Time: O(n + E × α(n)) ≈ O(n + E)  Space: O(n)
     */
    public static int countComponents(int n, int[][] edges) {
        UnionFind uf = new UnionFind(n);

        for (int[] edge : edges) {
            uf.union(edge[0], edge[1]); // Union reduces component count if they're in different groups
        }

        return uf.getComponents();
    }

    // =========================================================================
    // LC 684 — Redundant Connection (Detect Cycle, Find the Extra Edge)
    // =========================================================================

    /**
     * Given a tree with one extra edge (creating exactly one cycle), return that extra edge.
     * The "redundant" edge is the one that, when added, first creates a cycle.
     *
     * UNION-FIND APPROACH:
     *   Process edges in order. For each edge (u, v):
     *   - If find(u) == find(v): they're already connected → this edge creates a cycle → return it.
     *   - Else: union(u, v).
     *
     * WHY UNION-FIND OVER DFS FOR THIS PROBLEM?
     *   DFS can detect IF a cycle exists but not WHICH edge caused it without extra bookkeeping.
     *   Union-Find naturally identifies the first edge that connects two already-connected nodes.
     */
    public static int[] findRedundantConnection(int[][] edges) {
        int n = edges.length; // n+1 nodes (1-indexed), n+1 edges
        UnionFind uf = new UnionFind(n + 1); // Nodes are 1-indexed

        for (int[] edge : edges) {
            int u = edge[0], v = edge[1];

            if (!uf.union(u, v)) {
                // union returned false → u and v were already connected → this edge is redundant
                return edge;
            }
        }

        return new int[]{}; // Should not happen for valid input
    }

    // =========================================================================
    // main()
    // =========================================================================

    public static void main(String[] args) {

        System.out.println("=== Union-Find Operations ===");
        UnionFind uf = new UnionFind(6); // Nodes 0..5, 6 components initially

        System.out.println("Initial components: " + uf.getComponents()); // 6

        uf.union(0, 1); System.out.println("After union(0,1): " + uf.getComponents()); // 5
        uf.union(1, 2); System.out.println("After union(1,2): " + uf.getComponents()); // 4
        uf.union(3, 4); System.out.println("After union(3,4): " + uf.getComponents()); // 3

        System.out.println("connected(0,2): " + uf.connected(0, 2)); // true (0-1-2)
        System.out.println("connected(0,3): " + uf.connected(0, 3)); // false (separate groups)
        System.out.println("find(2) root: " + uf.find(2)); // root of {0,1,2}

        uf.union(2, 3); System.out.println("After union(2,3): " + uf.getComponents()); // 2
        System.out.println("connected(0,4): " + uf.connected(0, 4)); // true now

        System.out.println("\n=== LC 323: Number of Connected Components ===");
        System.out.println(countComponents(5, new int[][]{{0,1},{1,2},{3,4}}));         // Expected: 2
        System.out.println(countComponents(5, new int[][]{{0,1},{1,2},{2,3},{3,4}}));   // Expected: 1
        System.out.println(countComponents(4, new int[][]{}));                           // Expected: 4

        System.out.println("\n=== LC 684: Redundant Connection ===");
        System.out.println(java.util.Arrays.toString(
            findRedundantConnection(new int[][]{{1,2},{1,3},{2,3}}))); // Expected: [2,3]
        System.out.println(java.util.Arrays.toString(
            findRedundantConnection(new int[][]{{1,2},{2,3},{3,4},{1,4},{1,5}}))); // Expected: [1,4]
    }
}
