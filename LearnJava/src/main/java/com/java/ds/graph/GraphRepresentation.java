package com.java.ds.graph;

import java.util.*;

/**
 * GRAPH REPRESENTATION — Adjacency List and Adjacency Matrix
 *
 * WHAT IS A GRAPH?
 *   A graph G = (V, E) where V = vertices (nodes) and E = edges (connections).
 *   Unlike trees, graphs can have CYCLES and no single root.
 *
 * TWO MAIN REPRESENTATIONS:
 *
 * A) ADJACENCY LIST: Map<Integer, List<Integer>> or List<List<Integer>>
 *    - Each vertex maps to a list of its neighbors.
 *    - Space: O(V + E) — only stores actual edges.
 *    - Edge lookup: O(degree(v)) — must scan neighbor list.
 *    - BEST FOR: Sparse graphs (few edges). Most real-world graphs (social networks, roads).
 *
 * B) ADJACENCY MATRIX: boolean[V][V] or int[V][V]
 *    - matrix[u][v] = true/1 if edge exists between u and v.
 *    - Space: O(V²) — stores all possible edges.
 *    - Edge lookup: O(1) — direct array access.
 *    - BEST FOR: Dense graphs (many edges). When frequent edge lookup is needed.
 *
 * GRAPH TYPES:
 *   - Directed: edges have direction (A→B doesn't mean B→A). Social media "follows".
 *   - Undirected: edges are bidirectional. Road connections.
 *   - Weighted: edges have weights. Road distances, network costs.
 *   - Unweighted: all edges equal. Adjacency/connection.
 *
 * INTERVIEW FREQUENCY: Graph problems appear in ~20% of FAANG rounds.
 *   Knowing how to build the graph from input is step 0 of every graph problem.
 *
 * COMMON FOLLOW-UP:
 *   1. "When would you use adjacency matrix vs list?"
 *      Matrix: dense graphs, O(1) edge check. List: sparse graphs, O(V+E) space.
 *   2. "How do you represent weighted edges?"
 *      List: Map<Integer, List<int[]>> where int[] = {neighbor, weight}.
 *      Matrix: int[V][V] where value = weight (Integer.MAX_VALUE for no edge).
 */
public class GraphRepresentation {

    // =========================================================================
    // Adjacency List Graph
    // =========================================================================

    /**
     * Graph represented as an adjacency list.
     * Supports both directed and undirected, weighted and unweighted edges.
     *
     * USING Map<Integer, List<Integer>> instead of List<List<Integer>>:
     *   Allows non-contiguous vertex numbers (e.g., vertices 1, 5, 100).
     *   List<List<Integer>> requires vertices to be 0..n-1.
     */
    static class AdjacencyListGraph {
        private final Map<Integer, List<Integer>> adjList = new HashMap<>();
        private final boolean directed;

        AdjacencyListGraph(boolean directed) {
            this.directed = directed;
        }

        /** Adds vertex v (with no edges yet) */
        public void addVertex(int v) {
            adjList.putIfAbsent(v, new ArrayList<>());
        }

        /**
         * Adds an edge between u and v.
         * For undirected: adds both u→v and v→u.
         * For directed: adds only u→v.
         */
        public void addEdge(int u, int v) {
            adjList.computeIfAbsent(u, k -> new ArrayList<>()).add(v);

            if (!directed) {
                // Undirected: add reverse edge too
                adjList.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
            } else {
                // Ensure destination vertex exists in map (for iteration)
                adjList.putIfAbsent(v, new ArrayList<>());
            }
        }

        /** Returns neighbors of vertex v */
        public List<Integer> getNeighbors(int v) {
            return adjList.getOrDefault(v, Collections.emptyList());
        }

        public Set<Integer> getVertices() {
            return adjList.keySet();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {
                sb.append(entry.getKey()).append(" → ").append(entry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // Adjacency Matrix Graph
    // =========================================================================

    /**
     * Graph represented as a 2D boolean matrix.
     * Vertex IDs must be 0..n-1 (contiguous).
     */
    static class AdjacencyMatrixGraph {
        private final boolean[][] matrix;
        private final int n;
        private final boolean directed;

        AdjacencyMatrixGraph(int n, boolean directed) {
            this.n = n;
            this.directed = directed;
            this.matrix = new boolean[n][n];
        }

        public void addEdge(int u, int v) {
            matrix[u][v] = true;
            if (!directed) matrix[v][u] = true; // Undirected: add reverse edge
        }

        public boolean hasEdge(int u, int v) {
            return matrix[u][v]; // O(1) lookup — key advantage over adjacency list
        }

        public List<Integer> getNeighbors(int v) {
            List<Integer> neighbors = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (matrix[v][i]) neighbors.add(i);
            }
            return neighbors;
        }

        public void print() {
            System.out.print("   ");
            for (int i = 0; i < n; i++) System.out.printf("%3d", i);
            System.out.println();
            for (int i = 0; i < n; i++) {
                System.out.printf("%2d:", i);
                for (int j = 0; j < n; j++) {
                    System.out.printf("%3s", matrix[i][j] ? "1" : ".");
                }
                System.out.println();
            }
        }
    }

    // =========================================================================
    // main() — demonstrate both representations
    // =========================================================================

    public static void main(String[] args) {
        System.out.println("=== Undirected Graph (Adjacency List) ===");
        //  0 --- 1 --- 3
        //  |     |
        //  2 --- 4
        AdjacencyListGraph undir = new AdjacencyListGraph(false);
        undir.addEdge(0, 1);
        undir.addEdge(0, 2);
        undir.addEdge(1, 3);
        undir.addEdge(1, 4);
        undir.addEdge(2, 4);
        System.out.println(undir);

        System.out.println("Neighbors of 1: " + undir.getNeighbors(1)); // [0, 3, 4]

        System.out.println("=== Directed Graph (Adjacency List) ===");
        // 0 → 1 → 3
        // ↓   ↓
        // 2 → 4
        AdjacencyListGraph dir = new AdjacencyListGraph(true);
        dir.addEdge(0, 1);
        dir.addEdge(0, 2);
        dir.addEdge(1, 3);
        dir.addEdge(1, 4);
        dir.addEdge(2, 4);
        System.out.println(dir);

        System.out.println("=== Undirected Graph (Adjacency Matrix, 5 vertices) ===");
        AdjacencyMatrixGraph matrix = new AdjacencyMatrixGraph(5, false);
        matrix.addEdge(0, 1);
        matrix.addEdge(0, 2);
        matrix.addEdge(1, 3);
        matrix.addEdge(1, 4);
        matrix.addEdge(2, 4);
        matrix.print();
        System.out.println("Has edge 1-4: " + matrix.hasEdge(1, 4)); // true
        System.out.println("Has edge 0-3: " + matrix.hasEdge(0, 3)); // false

        System.out.println("\n=== Space Comparison ===");
        System.out.println("Adjacency List — Space: O(V+E) = O(5+5) = O(10)");
        System.out.println("Adjacency Matrix — Space: O(V²) = O(25)");
        System.out.println("For sparse graphs: List wins. For dense graphs: Matrix is comparable.");

        System.out.println("\n=== Building Graph from LeetCode-style Edge List ===");
        // Input: [[0,1],[0,2],[1,3]], n=4
        int n = 4;
        int[][] edges = {{0,1},{0,2},{1,3}};
        AdjacencyListGraph g = buildFromEdgeList(n, edges, false);
        System.out.println(g);
    }

    /** Utility: build adjacency list from edge list input (common in LC problems) */
    public static AdjacencyListGraph buildFromEdgeList(int n, int[][] edges, boolean directed) {
        AdjacencyListGraph g = new AdjacencyListGraph(directed);
        for (int i = 0; i < n; i++) g.addVertex(i);
        for (int[] edge : edges) g.addEdge(edge[0], edge[1]);
        return g;
    }
}
