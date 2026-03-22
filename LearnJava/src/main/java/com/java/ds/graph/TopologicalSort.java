package com.java.ds.graph;

import java.util.*;

/**
 * TOPOLOGICAL SORT — Ordering Nodes in a DAG
 *
 * WHAT IS TOPOLOGICAL SORT?
 *   For a Directed Acyclic Graph (DAG), topological sort produces a linear ordering
 *   of vertices such that for every directed edge u→v, u appears BEFORE v.
 *
 *   In other words: if task B depends on task A, A must come before B in the order.
 *
 * REAL-WORLD APPLICATIONS:
 *   - Build systems (make, Maven): compile dependencies before the dependent module.
 *   - Course scheduling: prerequisite courses before advanced ones.
 *   - Package managers (npm, pip): install dependencies in correct order.
 *   - Task scheduling: jobs with dependencies.
 *
 * TWO ALGORITHMS:
 *   A) DFS + Stack (post-order): When DFS finishes a node, push to stack. Reverse = topo order.
 *   B) Kahn's Algorithm (BFS/in-degree): Start with nodes having in-degree 0. Remove them,
 *      reduce neighbors' in-degree, add newly 0-in-degree nodes to queue.
 *
 * CYCLE DETECTION BONUS:
 *   Kahn's: if processed count < n → cycle exists (some nodes have in-degree > 0 always).
 *   DFS: if a GRAY node is visited → cycle (back edge).
 *
 * INTERVIEW FREQUENCY: High. "Course Schedule" (LC 207, 210) is one of the most common graph questions.
 *
 * COMMON QUESTIONS:
 *   1. "DFS vs Kahn's — when to use which?"
 *      Both are O(V+E). Kahn's is easier to understand for most people.
 *      DFS approach is more elegant and space-efficient.
 *   2. "What if the graph has a cycle?"
 *      Topological sort is undefined. Return empty / detect cycle.
 *   3. "Is topological sort unique?" → Not always. Multiple valid orderings may exist.
 */
public class TopologicalSort {

    public static void main(String[] args) {

        System.out.println("=== DAG Topological Sort ===");
        // Graph: 5→2, 5→0, 4→0, 4→1, 2→3, 3→1
        //   5 → 2 → 3
        //   5 → 0     ↓
        //   4 → 0   → 1
        //   4 → 1
        int n = 6;
        List<List<Integer>> dag = buildGraph(n, new int[][]{{5,2},{5,0},{4,0},{4,1},{2,3},{3,1}});

        System.out.println("DFS approach:   " + topologicalSortDFS(dag, n));
        System.out.println("Kahn's approach:" + topologicalSortKahn(dag, n));

        System.out.println("\n=== LC 207: Course Schedule (Can Finish?) ===");
        // 2 courses: course 1 requires course 0 → no cycle → can finish
        System.out.println(canFinish(2, new int[][]{{1,0}}));       // Expected: true
        // Cycle: 0→1→0 → can't finish
        System.out.println(canFinish(2, new int[][]{{1,0},{0,1}})); // Expected: false
        // No prerequisites
        System.out.println(canFinish(3, new int[][]{}));             // Expected: true

        System.out.println("\n=== LC 210: Course Schedule II (Return Order) ===");
        System.out.println(Arrays.toString(findOrder(4, new int[][]{{1,0},{2,0},{3,1},{3,2}}))); // [0,1,2,3] or [0,2,1,3]
        System.out.println(Arrays.toString(findOrder(2, new int[][]{{1,0}})));    // [0, 1]
        System.out.println(Arrays.toString(findOrder(2, new int[][]{{0,1},{1,0}}))); // [] (cycle)
    }

    // =========================================================================
    // APPROACH A: DFS + Stack
    // =========================================================================

    /**
     * Topological sort using DFS post-order traversal.
     *
     * ALGORITHM:
     *   For each unvisited node, do DFS.
     *   When DFS FINISHES processing a node (all descendants explored), push it to stack.
     *   The last-finished node has no dependencies → should come FIRST.
     *   → Stack in reverse = topological order.
     *
     * WHY POST-ORDER?
     *   We push a node only AFTER all its successors are processed.
     *   So when we pop from the stack (LIFO), we get nodes with no unprocessed predecessors first.
     *
     * Note: This implementation doesn't detect cycles (use GraphCycleDetection.java for that).
     */
    public static List<Integer> topologicalSortDFS(List<List<Integer>> graph, int n) {
        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();

        for (int v = 0; v < n; v++) {
            if (!visited[v]) {
                dfsTopoSort(graph, v, visited, stack);
            }
        }

        // Pop from stack to get topological order
        List<Integer> order = new ArrayList<>();
        while (!stack.isEmpty()) order.add(stack.pop());
        return order;
    }

    private static void dfsTopoSort(List<List<Integer>> graph, int v, boolean[] visited, Deque<Integer> stack) {
        visited[v] = true;

        for (int neighbor : graph.get(v)) {
            if (!visited[neighbor]) {
                dfsTopoSort(graph, neighbor, visited, stack);
            }
        }

        stack.push(v); // Push AFTER all successors are processed (post-order)
    }

    // =========================================================================
    // APPROACH B: Kahn's Algorithm (BFS / In-Degree)
    // =========================================================================

    /**
     * Topological sort using Kahn's BFS algorithm.
     *
     * ALGORITHM:
     *   1. Compute in-degree (number of incoming edges) for each node.
     *   2. Enqueue all nodes with in-degree 0 (no prerequisites).
     *   3. While queue is not empty:
     *      a. Poll a node, add to result.
     *      b. For each neighbor: reduce in-degree by 1.
     *         If in-degree becomes 0 → add to queue (all its prerequisites are done).
     *   4. If result size < n → cycle detected (some nodes always had in-degree > 0).
     *
     * INTUITION: A node with in-degree 0 has no remaining prerequisites → can be "done" now.
     *   Removing it from the graph reduces dependencies on its successors.
     *
     * ADVANTAGE OVER DFS: Naturally detects cycles (via count check).
     *   Also conceptually maps well to task scheduling ("process what's ready").
     */
    public static List<Integer> topologicalSortKahn(List<List<Integer>> graph, int n) {
        int[] inDegree = new int[n];

        // Compute in-degree for each node
        for (int u = 0; u < n; u++) {
            for (int v : graph.get(u)) {
                inDegree[v]++; // Edge u→v increases v's in-degree
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int v = 0; v < n; v++) {
            if (inDegree[v] == 0) queue.offer(v); // Start with nodes that have no prerequisites
        }

        List<Integer> order = new ArrayList<>();

        while (!queue.isEmpty()) {
            int v = queue.poll();
            order.add(v); // This node's prerequisites are all done

            for (int neighbor : graph.get(v)) {
                inDegree[neighbor]--; // v is done → remove its contribution to neighbor's in-degree

                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor); // Neighbor's prerequisites are all done now
                }
            }
        }

        // If not all nodes are in order → cycle (some nodes' in-degree never reached 0)
        if (order.size() != n) {
            System.out.println("  Cycle detected — no topological order exists");
            return Collections.emptyList();
        }

        return order;
    }

    // =========================================================================
    // LC 207 — Course Schedule (Can you complete all courses?)
    // =========================================================================

    /**
     * Returns true if it's possible to finish all numCourses.
     * prerequisites[i] = [a, b] means course b must be taken before course a.
     *
     * REDUCTION: Build directed graph from prerequisites. If the graph has a cycle → impossible.
     * Use Kahn's algorithm — if topological sort processes all n nodes → no cycle → possible.
     */
    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> graph = buildFromPrereqs(numCourses, prerequisites);
        List<Integer> order = topologicalSortKahn(graph, numCourses);
        return order.size() == numCourses; // True only if no cycle
    }

    // =========================================================================
    // LC 210 — Course Schedule II (Return the order)
    // =========================================================================

    /**
     * Returns one valid course order, or empty array if impossible (cycle).
     */
    public static int[] findOrder(int numCourses, int[][] prerequisites) {
        List<List<Integer>> graph = buildFromPrereqs(numCourses, prerequisites);
        List<Integer> order = topologicalSortKahn(graph, numCourses);

        if (order.size() != numCourses) return new int[0]; // Cycle detected

        return order.stream().mapToInt(Integer::intValue).toArray();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    static List<List<Integer>> buildGraph(int n, int[][] edges) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        for (int[] e : edges) graph.get(e[0]).add(e[1]);
        return graph;
    }

    static List<List<Integer>> buildFromPrereqs(int n, int[][] prereqs) {
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());
        // prereqs[i] = [a, b] means b → a (b must come before a)
        for (int[] p : prereqs) graph.get(p[1]).add(p[0]);
        return graph;
    }
}
