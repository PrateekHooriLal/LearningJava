package com.java.ds.graph;

import java.util.*;

/**
 * CONCEPT: Advanced Graph Algorithms — MST, Modified Dijkstra, SCC
 * ─────────────────────────────────────────────────────────────────
 * This file covers:
 *   1. Minimum Spanning Tree (MST): Prim's and Kruskal's
 *   2. Modified Dijkstra: minimize max weight along path (minimax problems)
 *   3. Strongly Connected Components (Kosaraju's algorithm)
 *
 * MST Definition: A spanning tree of a graph with V nodes uses exactly V-1 edges,
 * connects all nodes, and has minimum total edge weight.
 *
 *   Algorithm     | Graph Type | Complexity       | Best For
 *   ──────────────|────────────|──────────────────|──────────────────────────
 *   Prim's        | Dense      | O((V+E) log V)   | Dense graphs (E ≈ V^2)
 *   Kruskal's     | Sparse     | O(E log E)       | Sparse graphs (E << V^2)
 *   Kosaraju's    | Directed   | O(V + E)         | Finding SCCs
 *
 * WHEN TO USE:
 *   - "Connect all nodes at minimum cost" → MST (Prim's or Kruskal's)
 *   - "Minimize the maximum edge weight along a path" → Modified Dijkstra
 *   - "Find groups where every node can reach every other" → Kosaraju's SCC
 *
 * TIME/SPACE COMPLEXITY: See individual algorithm sections.
 *
 * INTERVIEW ANGLE:
 *   Prim's = "grows from a seed" (like BFS), Kruskal's = "picks globally cheapest safe edge".
 *   Both produce valid MSTs but through different strategies.
 *   Kosaraju's SCC is a beautiful two-pass DFS — memorize the intuition, not just the code.
 *
 * REAL-WORLD USE:
 *   MST: Network design (cable laying), clustering algorithms, image segmentation
 *   Kosaraju's SCC: Compiler optimization (strongly connected subgraph detection),
 *                   web crawling (finding clusters of mutually linking pages)
 */
public class AdvancedGraphProblems {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: PRIM'S MST ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Prim's MST — Grows the MST one node at a time from a starting node.
     *
     * KEY INSIGHT: Similar to Dijkstra, but instead of minimizing cumulative distance,
     * we minimize the edge weight to ADD a new node to the MST.
     * At each step, pick the cheapest edge that crosses the MST boundary.
     *
     * ALGORITHM:
     *   inMST[] = boolean, cost[] = min edge weight to include node i
     *   min-heap: {edgeWeight, node}
     *   Start from node 0, cost[0] = 0
     *   While heap non-empty: extract min, mark inMST, add weight to total,
     *   update costs for neighbors not yet in MST
     *
     * DIFFERENCE FROM DIJKSTRA:
     *   Dijkstra: dist[v] = dist[u] + w  (cumulative path cost)
     *   Prim's:   cost[v] = w            (just the edge weight, not cumulative)
     *
     * @param n     number of nodes (0-indexed)
     * @param edges {from, to, weight} — undirected edges (add both directions internally)
     * @return total MST weight, or -1 if graph is not connected
     */
    public static int primsMST(int n, int[][] edges) {
        // Build undirected adjacency list
        List<int[]>[] adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        for (int[] e : edges) {
            adj[e[0]].add(new int[]{e[1], e[2]});
            adj[e[1]].add(new int[]{e[0], e[2]}); // undirected
        }

        boolean[] inMST = new boolean[n];
        // Min-heap: {edge_weight, node}
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.offer(new int[]{0, 0}); // start from node 0 with cost 0

        int totalWeight = 0;
        int edgesAdded = 0; // MST needs exactly n-1 edges

        while (!pq.isEmpty() && edgesAdded < n) {
            int[] curr = pq.poll();
            int w = curr[0], u = curr[1];

            if (inMST[u]) continue; // already in MST, skip (lazy deletion)
            inMST[u] = true;
            totalWeight += w;
            edgesAdded++;

            for (int[] next : adj[u]) {
                int v = next[0], weight = next[1];
                if (!inMST[v]) {
                    pq.offer(new int[]{weight, v});
                    // Unlike Dijkstra, we push ALL edges to non-MST neighbors,
                    // not just improved ones (though improvement check is optional optimization)
                }
            }
        }
        // If edgesAdded < n, graph is disconnected → no spanning tree
        return edgesAdded == n ? totalWeight : -1;
    }
    /*
     * COMPLEXITY — Prim's
     * ────────────────────
     * TIME:  O((V + E) log V)
     *   - Each vertex added once: V extractions × O(log heap) = O(V log E) ≈ O(V log V)
     *   - Each edge considered twice: 2E pushes × O(log E) = O(E log E) ≈ O(E log V)
     *   - Total: O((V + E) log V)
     * SPACE: O(V + E) — adjacency list + inMST array + heap
     *
     * With Fibonacci heap (rare in interviews): O(E + V log V)
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: UNION-FIND (DISJOINT SET UNION — DSU)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Union-Find with Path Compression and Union by Rank
     *
     * KEY INSIGHT: Two key optimizations:
     *   1. Path Compression (find): Make all nodes in the path point directly to root.
     *      This flattens the tree, making future finds faster.
     *   2. Union by Rank: Always attach the smaller tree under the root of the larger tree.
     *      Keeps trees shallow.
     *
     * With both optimizations, operations are amortized O(α(n)) ≈ O(1)
     * where α is the inverse Ackermann function (grows slower than log).
     *
     * INTERVIEW Q: What is the difference between union by rank and union by size?
     * A: Rank is an upper bound on tree height; size is the actual number of nodes.
     *    Both give O(log n) worst case without path compression, O(α(n)) with it.
     */
    static int[] parent, rank;

    static void initUF(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i; // each node is its own root
    }

    static int find(int x) {
        // Path compression: point x directly to root during find
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    /**
     * KEY INSIGHT: Returns true if the union was performed (they were in different components).
     * Returns false if they were already in the same component (would create a cycle in MST).
     */
    static boolean union(int x, int y) {
        int rx = find(x), ry = find(y);
        if (rx == ry) return false; // same component — adding this edge would create a cycle

        // Union by rank: attach smaller rank tree under larger rank root
        if (rank[rx] < rank[ry]) { int tmp = rx; rx = ry; ry = tmp; }
        parent[ry] = rx;
        if (rank[rx] == rank[ry]) rank[rx]++; // only increase rank when merging equal-rank trees
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: KRUSKAL'S MST ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Kruskal's MST — Sort all edges by weight, greedily add non-cycle-forming edges.
     *
     * KEY INSIGHT: Process edges in ascending weight order. An edge is "safe" to add
     * to the MST if and only if it doesn't create a cycle (i.e., its two endpoints
     * are in different components). Union-Find efficiently detects cycles.
     *
     * COMPARISON — Prim's vs Kruskal's:
     *   Kruskal's is better for sparse graphs (E << V^2) since it sorts edges O(E log E)
     *   Prim's with adjacency matrix is O(V^2) — better for dense graphs
     *   In practice with a heap, both are O(E log V) and choice is personal preference
     *
     * @param n     number of nodes
     * @param edges {from, to, weight}
     * @return total MST weight, or -1 if disconnected
     */
    public static int kruskalsMST(int n, int[][] edges) {
        // Sort edges by weight (ascending)
        Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));

        initUF(n);

        int totalWeight = 0;
        int edgesAdded = 0;

        for (int[] e : edges) {
            if (edgesAdded == n - 1) break; // MST complete — no need to process more edges

            // union() returns true only if the edge connects two different components
            if (union(e[0], e[1])) {
                totalWeight += e[2];
                edgesAdded++;
            }
            // If union() returns false, this edge would form a cycle — skip it
        }
        return edgesAdded == n - 1 ? totalWeight : -1; // n-1 edges needed for spanning tree
    }
    /*
     * COMPLEXITY — Kruskal's
     * ───────────────────────
     * TIME:  O(E log E) dominated by sorting (E log E ≈ E log V since E ≤ V^2)
     *        Union-Find operations: O(E × α(V)) ≈ O(E)
     * SPACE: O(V) for Union-Find parent/rank arrays (not counting input edges)
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 4: LC 1584 — Min Cost to Connect All Points (Kruskal's on Points)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 1584 — Minimum Cost to Connect All Points
     *
     * KEY INSIGHT: The "cost" between two 2D points is Manhattan distance.
     * We need MST of a complete graph (all pairs connected).
     * With N ≤ 1000, we have N*(N-1)/2 ≈ 500K edges — Kruskal's works fine.
     *
     * GOTCHA: For larger N, Prim's with adjacency matrix is O(V^2) = O(N^2) which
     * avoids explicitly storing all O(N^2) edges. Kruskal's needs to store them all.
     *
     * @param points array of [x, y] coordinates
     * @return minimum cost to connect all points
     */
    public static int minCostConnectPoints(int[][] points) {
        int n = points.length;

        // Generate all pairs of edges with Manhattan distance as weight
        // Total edges: n*(n-1)/2
        int[][] edges = new int[n * (n - 1) / 2][3];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int dist = Math.abs(points[i][0] - points[j][0])
                         + Math.abs(points[i][1] - points[j][1]);
                edges[idx++] = new int[]{i, j, dist};
            }
        }
        return kruskalsMST(n, edges);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 5: MODIFIED DIJKSTRA — MINIMAX PATH PROBLEMS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * LC 778 — Swim in Rising Water (Minimize Maximum along Path)
     *
     * KEY INSIGHT: Instead of minimizing total path cost (classic Dijkstra),
     * we minimize the MAXIMUM edge weight encountered along any path.
     * This is "minimax path" — minimize the maximum bottleneck.
     *
     * MODIFICATION: The "cost" to reach a node is max(cost_to_reach_u, edge_weight_u_to_v)
     * instead of sum. We still use a min-heap on this modified cost.
     *
     * Alternative approaches:
     *   - Binary search on answer + BFS/DFS check: O(N^2 log N^2) = O(N^2 log N)
     *   - Union-Find: process edges in order until src and dst are connected
     *
     * INTERVIEW Q: Why does greedy extraction still work with max instead of sum?
     * A: The minimax path satisfies the same monotonicity property — a node once
     *    extracted with the minimum possible bottleneck cannot be improved later,
     *    because the max() function is also monotonically non-decreasing.
     *
     * @param grid NxN grid where grid[i][j] = elevation (edge weight)
     * @return minimum time to swim from (0,0) to (n-1,n-1)
     */
    public static int swimInWater(int[][] grid) {
        int n = grid.length;
        int[][] dist = new int[n][n]; // dist[i][j] = min bottleneck to reach (i,j)
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
        dist[0][0] = grid[0][0];

        // Min-heap: {bottleneck, row, col}
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.offer(new int[]{grid[0][0], 0, 0});

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int d = curr[0], r = curr[1], c = curr[2];

            if (d > dist[r][c]) continue; // stale entry (lazy deletion)
            if (r == n - 1 && c == n - 1) return d; // reached destination

            for (int[] dir : dirs) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue;

                // KEY: new bottleneck = max of current bottleneck and next cell's elevation
                int newDist = Math.max(d, grid[nr][nc]);
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist;
                    pq.offer(new int[]{newDist, nr, nc});
                }
            }
        }
        return dist[n - 1][n - 1];
    }

    /**
     * LC 1631 — Path With Minimum Effort (Minimize Max Absolute Difference)
     *
     * KEY INSIGHT: Same minimax pattern as LC 778. "Effort" of a path = max |diff|
     * between adjacent cells. Find the path minimizing this maximum.
     *
     * The only difference from LC 778: edge weight = |grid[r][c] - grid[nr][nc]|
     * instead of the cell value itself.
     *
     * @param heights 2D height grid
     * @return minimum effort to travel from top-left to bottom-right
     */
    public static int minimumEffortPath(int[][] heights) {
        int m = heights.length, n = heights[0].length;
        int[][] effort = new int[m][n];
        for (int[] row : effort) Arrays.fill(row, Integer.MAX_VALUE);
        effort[0][0] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.offer(new int[]{0, 0, 0});

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int e = curr[0], r = curr[1], c = curr[2];

            if (e > effort[r][c]) continue;
            if (r == m - 1 && c == n - 1) return e;

            for (int[] dir : dirs) {
                int nr = r + dir[0], nc = c + dir[1];
                if (nr < 0 || nr >= m || nc < 0 || nc >= n) continue;

                // Edge weight = absolute height difference
                int edgeWeight = Math.abs(heights[r][c] - heights[nr][nc]);
                int newEffort = Math.max(e, edgeWeight); // bottleneck = max along path
                if (newEffort < effort[nr][nc]) {
                    effort[nr][nc] = newEffort;
                    pq.offer(new int[]{newEffort, nr, nc});
                }
            }
        }
        return effort[m - 1][n - 1];
    }
    /*
     * COMPLEXITY — Modified Dijkstra (LC 778 / LC 1631)
     * ───────────────────────────────────────────────────
     * TIME:  O(N^2 log N^2) = O(N^2 log N) for an N×N grid
     *   - N^2 cells, each pushed/popped from heap at most once with lazy deletion
     *   - Heap size ≤ N^2, so each operation costs O(log N^2) = O(2 log N) = O(log N)
     * SPACE: O(N^2) — effort/dist matrix + heap
     */

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 6: KOSARAJU'S STRONGLY CONNECTED COMPONENTS (SCC)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Kosaraju's Algorithm — Find Strongly Connected Components in a Directed Graph
     *
     * DEFINITION: SCC = maximal set of nodes where every node can reach every other node.
     *
     * KEY INSIGHT (Two-Pass DFS):
     *   PASS 1 (Original graph): Run DFS, push nodes to stack in FINISH ORDER.
     *          The last node to finish has the "highest" finish time.
     *   PASS 2 (Transposed graph): Pop from stack, run DFS on reversed edges.
     *          Each DFS tree in this pass is exactly one SCC.
     *
     * WHY IT WORKS:
     *   In the SCC DAG (condensation), if SCC A can reach SCC B, then A finishes
     *   AFTER B in the first DFS (A's nodes have higher finish times).
     *   Processing in reverse finish order means we start from "source SCCs" in the
     *   transposed graph. In the transposed graph, a source SCC becomes a sink,
     *   so DFS from it can only reach that SCC's own nodes.
     *
     * INTERVIEW Q: What does "transposing" a graph mean?
     * A: Reversing the direction of every edge. If original had u→v, transpose has v→u.
     *
     * GOTCHA: Tarjan's SCC also runs in O(V+E) in a single DFS pass and is more
     * commonly seen in competitive programming. Kosaraju's is simpler to explain
     * conceptually for interviews.
     *
     * @param n     number of nodes (0-indexed)
     * @param edges directed edges {from, to}
     * @return list of SCCs, each SCC is a list of node indices
     */
    public static List<List<Integer>> kosarajuSCC(int n, int[][] edges) {
        // Build original adjacency list
        List<Integer>[] adj = new List[n];
        // Build transposed adjacency list
        List<Integer>[] radj = new List[n];
        for (int i = 0; i < n; i++) {
            adj[i] = new ArrayList<>();
            radj[i] = new ArrayList<>();
        }
        for (int[] e : edges) {
            adj[e[0]].add(e[1]);
            radj[e[1]].add(e[0]); // reverse edge for transposed graph
        }

        // PASS 1: DFS on original graph, record finish order
        boolean[] visited = new boolean[n];
        Deque<Integer> finishStack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (!visited[i]) dfs1(i, adj, visited, finishStack);
        }

        // PASS 2: DFS on transposed graph in reverse finish order
        Arrays.fill(visited, false);
        List<List<Integer>> sccs = new ArrayList<>();
        while (!finishStack.isEmpty()) {
            int node = finishStack.pop();
            if (!visited[node]) {
                List<Integer> scc = new ArrayList<>();
                dfs2(node, radj, visited, scc);
                sccs.add(scc);
            }
        }
        return sccs;
    }

    /** PASS 1: DFS on original graph, push to stack on finish (post-order). */
    private static void dfs1(int u, List<Integer>[] adj, boolean[] visited, Deque<Integer> stack) {
        visited[u] = true;
        for (int v : adj[u]) {
            if (!visited[v]) dfs1(v, adj, visited, stack);
        }
        stack.push(u); // push AFTER all neighbors are processed (post-order = finish time)
    }

    /** PASS 2: DFS on transposed graph, collect all reachable nodes as one SCC. */
    private static void dfs2(int u, List<Integer>[] radj, boolean[] visited, List<Integer> scc) {
        visited[u] = true;
        scc.add(u);
        for (int v : radj[u]) {
            if (!visited[v]) dfs2(v, radj, visited, scc);
        }
    }
    /*
     * COMPLEXITY — Kosaraju's SCC
     * ────────────────────────────
     * TIME:  O(V + E) — two DFS passes, each O(V + E)
     * SPACE: O(V + E) — two adjacency lists + visited arrays + stack
     *
     * The two adjacency lists (original + transposed) use O(V + E) total space.
     */

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Prim's MST ===");
        int[][] mstEdges = {{0, 1, 2}, {0, 3, 6}, {1, 2, 3}, {1, 3, 8}, {1, 4, 5}, {2, 4, 7}};
        System.out.println("Prim's MST weight: " + primsMST(5, mstEdges)); // Expected: 2+3+5+6 = wait, let me trace...

        System.out.println("\n=== Kruskal's MST ===");
        int[][] mstEdges2 = {{0, 1, 2}, {0, 3, 6}, {1, 2, 3}, {1, 3, 8}, {1, 4, 5}, {2, 4, 7}};
        System.out.println("Kruskal's MST weight: " + kruskalsMST(5, mstEdges2));

        System.out.println("\n=== LC 1584 Min Cost Connect All Points ===");
        int[][] points1 = {{0, 0}, {2, 2}, {3, 10}, {5, 2}, {7, 0}};
        System.out.println("LC 1584: " + minCostConnectPoints(points1)); // Expected: 20

        int[][] points2 = {{3, 12}, {-2, 5}, {-4, 1}};
        System.out.println("LC 1584 (3 points): " + minCostConnectPoints(points2)); // Expected: 18

        System.out.println("\n=== LC 778 Swim in Rising Water ===");
        int[][] grid1 = {{0, 2}, {1, 3}};
        System.out.println("LC 778 (2x2): " + swimInWater(grid1)); // Expected: 3

        int[][] grid2 = {
            {0, 1, 2, 3, 4},
            {24, 23, 22, 21, 5},
            {12, 13, 14, 15, 16},
            {11, 17, 18, 19, 20},
            {10, 9, 8, 7, 6}
        };
        System.out.println("LC 778 (5x5): " + swimInWater(grid2)); // Expected: 16

        System.out.println("\n=== LC 1631 Minimum Effort Path ===");
        int[][] heights1 = {{1, 2, 2}, {3, 8, 2}, {5, 3, 5}};
        System.out.println("LC 1631: " + minimumEffortPath(heights1)); // Expected: 2

        int[][] heights2 = {{1, 2, 3}, {3, 8, 4}, {5, 3, 5}};
        System.out.println("LC 1631: " + minimumEffortPath(heights2)); // Expected: 1

        System.out.println("\n=== Kosaraju's SCC ===");
        // Graph: 0→1→2→0 (one SCC), 2→3 (3 is separate SCC)
        int[][] sccEdges = {{0, 1}, {1, 2}, {2, 0}, {2, 3}};
        List<List<Integer>> sccs = kosarajuSCC(4, sccEdges);
        System.out.println("Number of SCCs: " + sccs.size()); // Expected: 2
        for (List<Integer> scc : sccs) {
            Collections.sort(scc);
            System.out.println("SCC: " + scc);
        }
        // Expected: [0,1,2] and [3]
    }
}
