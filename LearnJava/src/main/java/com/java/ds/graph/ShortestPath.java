package com.java.ds.graph;

import java.util.*;

/**
 * CONCEPT: Shortest Path Algorithms
 * ─────────────────────────────────
 * Three fundamental algorithms for finding shortest paths in weighted graphs.
 * Each solves a different variant of the problem under different constraints.
 *
 *   Algorithm        | Edge Weights | Negative Cycles | Complexity      | Use Case
 *   ─────────────────|──────────────|─────────────────|─────────────────|─────────────────────────
 *   Dijkstra         | Non-negative | Cannot detect   | O((V+E) log V)  | GPS, network routing
 *   Bellman-Ford     | Any          | Detects them    | O(V * E)        | Currency arbitrage det.
 *   Floyd-Warshall   | Any          | Detects them    | O(V^3), O(V^2)  | All-pairs, dense graphs
 *   BFS              | Unweighted   | N/A             | O(V + E)        | Fewest hops
 *
 * WHEN TO USE:
 *   - Single source, no negative weights → Dijkstra (fastest in practice)
 *   - Single source, negative weights possible → Bellman-Ford
 *   - All-pairs shortest path → Floyd-Warshall
 *   - Unweighted graph → BFS
 *
 * TIME/SPACE COMPLEXITY: See each algorithm section below.
 *
 * INTERVIEW ANGLE:
 *   Always clarify: (1) directed or undirected? (2) negative weights? (3) single-source or all-pairs?
 *   These three questions determine which algorithm to pick.
 *   Dijkstra with a binary heap is the most commonly expected answer for FAANG interviews.
 *
 * REAL-WORLD USE:
 *   - Google Maps / GPS navigation (Dijkstra / A*)
 *   - Internet routing protocols (OSPF uses Dijkstra, BGP uses Bellman-Ford variant)
 *   - Currency exchange rate arbitrage detection (Bellman-Ford negative cycle)
 *   - Social network degrees of separation (BFS)
 */
public class ShortestPath {

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 1: DIJKSTRA'S ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dijkstra's Algorithm — Single Source Shortest Path (non-negative weights)
     *
     * KEY INSIGHT: Greedy algorithm. At each step, the node with the smallest
     * known distance is finalized. This works because with non-negative edge
     * weights, once a node is extracted from the min-heap, no shorter path to
     * it can be discovered (the triangle inequality holds forward in time).
     *
     * CORE IDEA:
     *   dist[] = {INF, INF, ...}, dist[src] = 0
     *   min-heap: poll (dist, node), skip if dist > known dist (stale entry)
     *   for each neighbor: relax edge → if dist[u] + w < dist[v]: update and push
     *
     * INTERVIEW Q: Why can't Dijkstra handle negative weights?
     * A: A greedy extraction assumes the current shortest path won't be improved
     *    later. A negative edge can create a shorter path discovered after extraction,
     *    violating the greedy invariant.
     *
     * GOTCHA: Always use a lazy deletion approach (skip stale heap entries) rather
     * than decrease-key, because Java's PriorityQueue doesn't support decrease-key.
     *
     * @param n      number of nodes (0-indexed: 0 to n-1)
     * @param edges  edge list: each int[] is {from, to, weight}
     * @param src    source node
     * @return dist[] array where dist[i] = shortest distance from src to i,
     *         or Integer.MAX_VALUE if unreachable
     */
    public static int[] dijkstra(int n, int[][] edges, int src) {
        // Build adjacency list: adj[u] = list of {neighbor, weight}
        List<int[]>[] adj = new List[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();
        for (int[] e : edges) {
            adj[e[0]].add(new int[]{e[1], e[2]});
            // GOTCHA: For undirected graphs, also add: adj[e[1]].add(new int[]{e[0], e[2]});
        }

        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // Min-heap ordered by distance: {dist, node}
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.offer(new int[]{0, src});

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int d = curr[0], u = curr[1];

            // CRITICAL: Skip stale entries (lazy deletion)
            // A node can be pushed multiple times with decreasing distances.
            // When we pop it with a larger distance than already recorded, skip it.
            if (d > dist[u]) continue;

            for (int[] next : adj[u]) {
                int v = next[0], w = next[1];
                // Edge relaxation: can we reach v cheaper through u?
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    pq.offer(new int[]{dist[v], v});
                    // We don't remove old entry — it becomes stale, will be skipped above
                }
            }
        }
        return dist;
    }
    /*
     * COMPLEXITY — Dijkstra
     * ─────────────────────
     * TIME:  O((V + E) log V)
     *   - Each vertex extracted once from heap: V extractions × O(log V) = O(V log V)
     *   - Each edge can cause at most one push: E pushes × O(log V) = O(E log V)
     *   - Total: O((V + E) log V)
     * SPACE: O(V + E) — adjacency list + dist array + heap (at most E entries)
     *
     * With Fibonacci heap, time becomes O(E + V log V) — rarely used in interviews.
     */

    /**
     * LC 743 — Network Delay Time
     *
     * KEY INSIGHT: Direct application of Dijkstra. Find max of all dist[] values
     * (the "last" node to receive the signal). If any node is unreachable, return -1.
     *
     * INTERVIEW Q: What if k is 1-indexed?
     * A: Use k-1 as the source, or adjust the node indices.
     *
     * @param times  edges as {u, v, w} (1-indexed nodes)
     * @param n      number of nodes
     * @param k      source node (1-indexed)
     * @return minimum time for all nodes to receive signal, -1 if impossible
     */
    public static int networkDelayTime(int[][] times, int n, int k) {
        // Convert 1-indexed to 0-indexed by subtracting 1
        int[][] zeroIndexed = new int[times.length][3];
        for (int i = 0; i < times.length; i++) {
            zeroIndexed[i] = new int[]{times[i][0] - 1, times[i][1] - 1, times[i][2]};
        }

        int[] dist = dijkstra(n, zeroIndexed, k - 1);

        int maxDist = 0;
        for (int d : dist) {
            if (d == Integer.MAX_VALUE) return -1; // some node unreachable
            maxDist = Math.max(maxDist, d);
        }
        return maxDist;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 2: BELLMAN-FORD ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Bellman-Ford — Single Source Shortest Path with Negative Weights
     *
     * KEY INSIGHT: Relax ALL edges V-1 times. After k iterations, dist[v] holds
     * the shortest path using at most k edges. With V nodes, any simple path
     * has at most V-1 edges. If a V-th relaxation still improves a distance,
     * a negative cycle exists.
     *
     * INTERVIEW Q: Why V-1 iterations?
     * A: Any simple shortest path visits at most V nodes → at most V-1 edges.
     *    After i iterations, we've found the optimal path using ≤ i edges.
     *
     * GOTCHA for LC 787 (K stops constraint):
     *   Copy dist array before each pass to prevent "chaining" — using a newly
     *   relaxed edge in the same iteration would effectively allow more than K stops
     *   in a single pass. Without copying, one iteration might traverse 2+ edges.
     *
     * @param n      number of nodes (0-indexed)
     * @param edges  {from, to, weight} array
     * @param src    source node
     * @return dist[] or null if negative cycle detected
     */
    public static int[] bellmanFord(int n, int[][] edges, int src) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // Relax all edges V-1 times
        for (int i = 0; i < n - 1; i++) {
            for (int[] e : edges) {
                int u = e[0], v = e[1], w = e[2];
                // Guard against overflow when dist[u] is MAX_VALUE
                if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                }
            }
        }

        // V-th relaxation: if improvement still possible, negative cycle exists
        for (int[] e : edges) {
            int u = e[0], v = e[1], w = e[2];
            if (dist[u] != Integer.MAX_VALUE && dist[u] + w < dist[v]) {
                return null; // negative cycle detected
            }
        }
        return dist;
    }
    /*
     * COMPLEXITY — Bellman-Ford
     * ──────────────────────────
     * TIME:  O(V × E) — V-1 passes, each scanning all E edges
     * SPACE: O(V) — just the dist array (edges given as input)
     *
     * Slower than Dijkstra, but handles negative weights and detects negative cycles.
     */

    /**
     * LC 787 — Cheapest Flights Within K Stops
     *
     * KEY INSIGHT: This is Bellman-Ford limited to K+1 iterations (K stops = K+1 edges).
     * CRITICAL: Copy the dist array before each iteration to prevent using a newly
     * relaxed node in the same pass (which would count 2 edges as 1 iteration / 1 stop).
     *
     * INTERVIEW TRAP: Students often skip the copy and wonder why their answer is wrong.
     * The copy ensures each iteration only relaxes paths of exactly one more edge.
     *
     * Alternative: Modified Dijkstra with state (node, stops_remaining) — O((V+E)log(VK))
     *
     * @param n      number of cities (0-indexed)
     * @param flights {from, to, price}
     * @param src    source city
     * @param dst    destination city
     * @param k      maximum number of stops (not edges — k stops = k+1 edges)
     * @return minimum price or -1 if unreachable within k stops
     */
    public static int findCheapestPrice(int n, int[][] flights, int src, int dst, int k) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;

        // K stops = K+1 edges → run K+1 iterations
        for (int i = 0; i <= k; i++) {
            // CRITICAL: snapshot dist before this pass to prevent chaining
            int[] temp = Arrays.copyOf(dist, n);

            for (int[] f : flights) {
                int u = f[0], v = f[1], w = f[2];
                // Use temp[u] (before this pass) not dist[u] (which may have been updated this pass)
                if (temp[u] != Integer.MAX_VALUE && temp[u] + w < dist[v]) {
                    dist[v] = temp[u] + w;
                }
            }
        }
        return dist[dst] == Integer.MAX_VALUE ? -1 : dist[dst];
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION 3: FLOYD-WARSHALL ALGORITHM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Floyd-Warshall — All-Pairs Shortest Path
     *
     * KEY INSIGHT: dp[k][i][j] = shortest path from i to j using only nodes 0..k as
     * intermediates. Can be space-optimized to 2D since each layer only depends on
     * the previous layer (and actually can be done in-place).
     *
     * RECURRENCE:
     *   dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])
     *   "Can we go i→k then k→j cheaper than the current i→j path?"
     *
     * ORDER OF LOOPS MATTERS: k (intermediate node) must be the OUTER loop.
     * Switching i and k would compute incorrect results.
     *
     * NEGATIVE CYCLE DETECTION: After running, if dist[i][i] < 0 for any i, a
     * negative cycle exists.
     *
     * INTERVIEW Q: Why O(V^3) and not O(V^2 * E)?
     * A: We iterate over all pairs (i,j) and for each intermediate k — that's V^3 total.
     *    For dense graphs (E ≈ V^2), this is actually comparable to running Dijkstra
     *    from each vertex: V × (V+E)logV = V × V^2 logV = V^3 logV (worse!).
     *
     * @param n     number of nodes (0-indexed)
     * @param edges {from, to, weight}
     * @return dist[i][j] = shortest path from i to j, or INF if unreachable
     */
    public static int[][] floydWarshall(int n, int[][] edges) {
        final int INF = (int) 1e9; // Use large finite value, NOT Integer.MAX_VALUE (overflow risk)
        int[][] dist = new int[n][n];

        // Initialize: same node = 0, others = INF
        for (int[] row : dist) Arrays.fill(row, INF);
        for (int i = 0; i < n; i++) dist[i][i] = 0;

        // Fill in direct edges
        for (int[] e : edges) {
            // GOTCHA: If multiple edges exist between same pair, take the minimum
            dist[e[0]][e[1]] = Math.min(dist[e[0]][e[1]], e[2]);
        }

        // Triple loop: k = intermediate node (MUST be outermost)
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    // Can we route i→j through k more cheaply?
                    if (dist[i][k] != INF && dist[k][j] != INF) { // guard against overflow
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
        }
        return dist;
    }
    /*
     * COMPLEXITY — Floyd-Warshall
     * ────────────────────────────
     * TIME:  O(V^3) — three nested loops each of size V
     * SPACE: O(V^2) — the dist[][] matrix
     *
     * Best for V ≤ 400 (400^3 ≈ 64M ops). For V = 1000, use V × Dijkstra instead.
     */

    /**
     * LC 1334 — Find the City With the Smallest Number of Neighbors at a Threshold Distance
     *
     * KEY INSIGHT: Floyd-Warshall gives all-pairs distances. Then for each city,
     * count reachable cities (dist ≤ threshold). Return the city with fewest reachable
     * neighbors, preferring higher-indexed city on tie.
     *
     * WHY FLOYD-WARSHALL? V ≤ 100, so O(V^3) = 10^6 — perfectly fine.
     * Could also run Dijkstra from each vertex but Floyd-Warshall is simpler.
     *
     * @param n            number of cities
     * @param edges        {from, to, weight}
     * @param distanceThreshold maximum allowed distance
     * @return the city index that best satisfies the conditions
     */
    public static int findTheCity(int n, int[][] edges, int distanceThreshold) {
        int[][] dist = floydWarshall(n, edges);
        // Also add reverse edges (undirected graph in this problem)
        // Floyd-Warshall above handles directed; for undirected, add both directions in edges

        int resultCity = -1;
        int minReachable = Integer.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            int reachable = 0;
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] <= distanceThreshold) reachable++;
            }
            // Use >= to prefer higher-indexed city on tie
            if (reachable <= minReachable) {
                minReachable = reachable;
                resultCity = i;
            }
        }
        return resultCity;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAIN — Test Cases
    // ═══════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Dijkstra Tests ===");

        // LC 743: Network Delay Time
        // Graph: 1→2 weight 1, 2→3 weight 1, 1→3 weight 4  (1-indexed)
        // From node 2: shortest to reach all nodes
        int[][] times1 = {{2, 1, 1}, {2, 3, 1}, {3, 4, 1}};
        System.out.println("LC 743 (n=4,k=2): " + networkDelayTime(times1, 4, 2)); // Expected: 2

        int[][] times2 = {{1, 2, 1}};
        System.out.println("LC 743 (n=2,k=1): " + networkDelayTime(times2, 2, 1)); // Expected: 1

        int[][] times3 = {{1, 2, 1}};
        System.out.println("LC 743 (n=2,k=2): " + networkDelayTime(times3, 2, 2)); // Expected: -1

        System.out.println("\n=== Bellman-Ford (raw) Tests ===");
        // Simple graph: 0→1 w=4, 0→2 w=1, 2→1 w=2, 1→3 w=1
        int[][] bfEdges = {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}};
        int[] bfDist = bellmanFord(4, bfEdges, 0);
        System.out.println("BF from 0: " + Arrays.toString(bfDist)); // [0, 3, 1, 4]

        System.out.println("\n=== LC 787 Cheapest Flights Tests ===");
        int[][] flights1 = {{0, 1, 100}, {1, 2, 100}, {0, 2, 500}};
        System.out.println("LC 787 (n=3,src=0,dst=2,k=1): " + findCheapestPrice(3, flights1, 0, 2, 1)); // 200

        int[][] flights2 = {{0, 1, 100}, {1, 2, 100}, {0, 2, 500}};
        System.out.println("LC 787 (n=3,src=0,dst=2,k=0): " + findCheapestPrice(3, flights2, 0, 2, 0)); // 500

        System.out.println("\n=== Floyd-Warshall Tests ===");
        // Undirected: 0-1 w=2, 1-2 w=3, 0-2 w=10
        // Add both directions for undirected
        int[][] fwEdges = {{0, 1, 2}, {1, 0, 2}, {1, 2, 3}, {2, 1, 3}, {0, 2, 10}, {2, 0, 10}};
        int[][] fwDist = floydWarshall(3, fwEdges);
        System.out.println("FW dist[0][2]: " + fwDist[0][2]); // 5 (via node 1)
        System.out.println("FW dist[0][1]: " + fwDist[0][1]); // 2

        System.out.println("\n=== LC 1334 Find the City Tests ===");
        int[][] cityEdges = {{0, 1, 3}, {1, 2, 1}, {1, 3, 4}, {2, 3, 1}};
        System.out.println("LC 1334 (n=4,threshold=4): " + findTheCity(4, cityEdges, 4)); // Expected: 3
        // Note: problem uses undirected edges, so we'd add both directions
    }
}
