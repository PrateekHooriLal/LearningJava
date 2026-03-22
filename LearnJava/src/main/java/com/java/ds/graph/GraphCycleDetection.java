package com.java.ds.graph;

import java.util.*;

/**
 * GRAPH CYCLE DETECTION — Directed and Undirected
 *
 * WHY CYCLE DETECTION MATTERS:
 *   - Topological sort is only valid on a DAG (Directed Acyclic Graph).
 *     Before sorting, verify no cycles exist.
 *   - Deadlock detection: threads waiting for each other = cycle in wait-for graph.
 *   - Package dependency resolution: circular imports = cycle.
 *
 * TWO ALGORITHMS:
 *
 * A) DIRECTED GRAPH — DFS with 3-color (WHITE/GRAY/BLACK) marking:
 *    WHITE (0) = not yet visited
 *    GRAY  (1) = currently in DFS stack (being explored)
 *    BLACK (2) = fully explored (all descendants processed)
 *    CYCLE DETECTED: if DFS reaches a GRAY node (we're revisiting a node on the current path).
 *
 * B) UNDIRECTED GRAPH — DFS with parent tracking:
 *    When we visit a node, track which node we came from (parent).
 *    CYCLE DETECTED: if we find a visited neighbor that is NOT the parent.
 *    (The parent check is needed because in an undirected graph, u-v means both u→v and v→u.
 *     Without it, we'd false-positive: "visited parent via back-edge = cycle" — wrong.)
 *
 * INTERVIEW FREQUENCY: High. Always paired with topological sort questions.
 *
 * COMMON QUESTIONS:
 *   1. "Why can't you use the same algorithm for directed and undirected?"
 *      Directed: back-edge = cycle only if it goes to a GRAY ancestor.
 *      Undirected: we'd always see the parent as "visited" — need parent exclusion.
 *   2. "What is a back edge?" → An edge pointing to an ancestor in the DFS tree.
 *      In directed graphs, back edges indicate cycles.
 *   3. "Can you detect cycles using BFS?" → Yes: Kahn's algorithm (topological sort BFS).
 *      If not all nodes are processed in topological order → cycle exists.
 */
public class GraphCycleDetection {

    // Color constants for 3-color DFS
    private static final int WHITE = 0; // Not visited
    private static final int GRAY  = 1; // In current DFS path (being explored)
    private static final int BLACK = 2; // Fully explored

    public static void main(String[] args) {

        System.out.println("=== Directed Graph Cycle Detection ===");

        // DAG (no cycle): 0→1→3, 0→2→3
        List<List<Integer>> dag = new ArrayList<>();
        for (int i = 0; i < 4; i++) dag.add(new ArrayList<>());
        dag.get(0).add(1); dag.get(0).add(2);
        dag.get(1).add(3); dag.get(2).add(3);
        System.out.println("DAG has cycle: " + hasCycleDirected(dag)); // Expected: false

        // Graph with cycle: 0→1→2→0
        List<List<Integer>> cyclic = new ArrayList<>();
        for (int i = 0; i < 3; i++) cyclic.add(new ArrayList<>());
        cyclic.get(0).add(1); cyclic.get(1).add(2); cyclic.get(2).add(0);
        System.out.println("Cyclic directed has cycle: " + hasCycleDirected(cyclic)); // Expected: true

        // Course Schedule style: prerequisites form a cycle
        // Course 0 requires 1, Course 1 requires 0 → cycle → can't complete
        List<List<Integer>> courses = new ArrayList<>();
        for (int i = 0; i < 2; i++) courses.add(new ArrayList<>());
        courses.get(0).add(1); courses.get(1).add(0);
        System.out.println("Circular prerequisite: " + hasCycleDirected(courses)); // Expected: true

        System.out.println("\n=== Undirected Graph Cycle Detection ===");

        // Graph with cycle: 0-1-2-0
        Map<Integer, List<Integer>> cyclic2 = new HashMap<>();
        addUndirectedEdge(cyclic2, 0, 1);
        addUndirectedEdge(cyclic2, 1, 2);
        addUndirectedEdge(cyclic2, 2, 0);
        System.out.println("Undirected cycle (triangle): " + hasCycleUndirected(cyclic2)); // Expected: true

        // Tree (no cycle): 0-1-2-3 (path graph)
        Map<Integer, List<Integer>> tree = new HashMap<>();
        addUndirectedEdge(tree, 0, 1);
        addUndirectedEdge(tree, 1, 2);
        addUndirectedEdge(tree, 2, 3);
        System.out.println("Path graph (tree): " + hasCycleUndirected(tree)); // Expected: false
    }

    // =========================================================================
    // Cycle Detection — Directed Graph (3-color DFS)
    // =========================================================================

    /**
     * Detects cycle in a directed graph using WHITE/GRAY/BLACK DFS coloring.
     *
     * COLOR SEMANTICS:
     *   WHITE: Node not yet discovered.
     *   GRAY:  Node is on the current DFS path (in the call stack).
     *   BLACK: Node fully processed — all reachable nodes from it are explored.
     *
     * CYCLE = reaching a GRAY node during DFS.
     *   A GRAY node is an ancestor in the current path → we have a back edge → cycle.
     *   (Reaching a BLACK node is fine — it's already fully explored, not a cycle.)
     *
     * WHY NOT JUST USE boolean[] visited?
     *   With only visited/unvisited, we can't distinguish between:
     *   a) Already fully explored node (safe to see again — no cycle)
     *   b) Ancestor in current path (cycle!)
     *   The GRAY color is the key distinction.
     */
    public static boolean hasCycleDirected(List<List<Integer>> graph) {
        int n = graph.size();
        int[] color = new int[n]; // All start WHITE (0)

        for (int v = 0; v < n; v++) {
            if (color[v] == WHITE) {
                if (dfsCycleDirected(graph, v, color)) return true;
            }
        }

        return false;
    }

    private static boolean dfsCycleDirected(List<List<Integer>> graph, int v, int[] color) {
        color[v] = GRAY; // Mark as "currently exploring"

        for (int neighbor : graph.get(v)) {
            if (color[neighbor] == GRAY) {
                // Back edge to an ancestor — CYCLE FOUND
                return true;
            }
            if (color[neighbor] == WHITE) {
                // Unvisited neighbor — explore it
                if (dfsCycleDirected(graph, neighbor, color)) return true;
            }
            // BLACK neighbor: already fully explored, not a cycle — skip
        }

        color[v] = BLACK; // Mark as fully explored
        return false;
    }

    // =========================================================================
    // Cycle Detection — Undirected Graph (DFS with parent tracking)
    // =========================================================================

    /**
     * Detects cycle in an undirected graph.
     *
     * KEY INSIGHT: In an undirected graph, every edge u-v appears as both
     *   u→v and v→u. When we're at v (came from u), we'll see u as a neighbor.
     *   This should NOT be counted as a cycle — it's just the edge we came from.
     *   Track the PARENT node and skip it when checking neighbors.
     *
     * CYCLE = reaching a visited node that is NOT the parent.
     *   This means we found an alternative path back to an ancestor = cycle.
     */
    public static boolean hasCycleUndirected(Map<Integer, List<Integer>> graph) {
        Set<Integer> visited = new HashSet<>();

        for (int v : graph.keySet()) {
            if (!visited.contains(v)) {
                if (dfsCycleUndirected(graph, v, -1, visited)) return true;
            }
        }

        return false;
    }

    private static boolean dfsCycleUndirected(Map<Integer, List<Integer>> graph, int v, int parent, Set<Integer> visited) {
        visited.add(v);

        for (int neighbor : graph.getOrDefault(v, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                // Unvisited neighbor — explore it (passing v as the new parent)
                if (dfsCycleUndirected(graph, neighbor, v, visited)) return true;

            } else if (neighbor != parent) {
                // Visited neighbor that is NOT our parent → back edge → CYCLE
                // (If it IS the parent, it's just the undirected edge we came from — not a cycle)
                return true;
            }
        }

        return false;
    }

    private static void addUndirectedEdge(Map<Integer, List<Integer>> graph, int u, int v) {
        graph.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        graph.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
    }
}
