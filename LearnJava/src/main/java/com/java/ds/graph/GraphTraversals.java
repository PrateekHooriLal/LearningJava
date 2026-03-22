package com.java.ds.graph;

import java.util.*;

/**
 * GRAPH TRAVERSALS — BFS and DFS (Iterative + Recursive)
 *
 * THE TWO FUNDAMENTAL GRAPH TRAVERSALS:
 *
 * BFS (Breadth-First Search):
 *   - Uses a QUEUE (FIFO).
 *   - Visits all nodes at distance 1, then distance 2, then distance 3, etc.
 *   - Guarantees SHORTEST PATH in unweighted graphs.
 *   - Layer-by-layer exploration.
 *
 * DFS (Depth-First Search):
 *   - Uses a STACK (LIFO) or recursion (implicit call stack).
 *   - Goes as deep as possible before backtracking.
 *   - Used for cycle detection, topological sort, connected components.
 *
 * CRITICAL DIFFERENCE: BFS = SHORTEST PATH. DFS = NO SHORTEST PATH GUARANTEE.
 *   If someone asks "shortest path in unweighted graph" → BFS.
 *   If they ask "is there any path?" → either BFS or DFS works.
 *
 * VISITED SET: Always required to prevent revisiting nodes in cyclic graphs.
 *   Without visited tracking, BFS/DFS can loop infinitely.
 *
 * Time: O(V + E) for both BFS and DFS — visit each vertex and edge once.
 * Space: O(V) — visited set + queue/stack hold at most V elements.
 *
 * INTERVIEW FREQUENCY: Very high. All graph problems build on BFS/DFS.
 *
 * COMMON QUESTIONS:
 *   1. "BFS vs DFS — when to use which?"
 *      BFS: shortest path, level-order, closest neighbor.
 *      DFS: cycle detection, topological sort, tree height, backtracking.
 *   2. "How do you handle disconnected graphs?"
 *      Loop through all vertices, start BFS/DFS from each unvisited vertex.
 *   3. "Can DFS find shortest path?" → No (in general). Use BFS.
 */
public class GraphTraversals {

    public static void main(String[] args) {
        // Build graph:
        // 0 - 1 - 3 - 5
        // |   |
        // 2 - 4
        Map<Integer, List<Integer>> graph = new HashMap<>();
        addEdge(graph, 0, 1);
        addEdge(graph, 0, 2);
        addEdge(graph, 1, 3);
        addEdge(graph, 1, 4);
        addEdge(graph, 2, 4);
        addEdge(graph, 3, 5);

        System.out.println("=== BFS from vertex 0 ===");
        System.out.println(bfs(graph, 0)); // Expected: [0, 1, 2, 3, 4, 5]

        System.out.println("\n=== DFS Recursive from vertex 0 ===");
        System.out.println(dfsRecursive(graph, 0)); // [0, 1, 3, 5, 4, 2] (order depends on list order)

        System.out.println("\n=== DFS Iterative from vertex 0 ===");
        System.out.println(dfsIterative(graph, 0));

        System.out.println("\n=== Shortest Path BFS: 0 → 5 ===");
        System.out.println(shortestPathBFS(graph, 0, 5)); // Expected: [0, 1, 3, 5]

        System.out.println("\n=== Has Path DFS: 0 → 5 ===");
        System.out.println(hasPathDFS(graph, 0, 5, new HashSet<>())); // Expected: true

        System.out.println("=== Has Path DFS: 2 → 5 ===");
        System.out.println(hasPathDFS(graph, 2, 5, new HashSet<>())); // Expected: true

        System.out.println("=== Has Path DFS: 5 → 2 (undirected) ===");
        System.out.println(hasPathDFS(graph, 5, 2, new HashSet<>())); // Expected: true

        System.out.println("\n=== Disconnected Graph: Count Components ===");
        Map<Integer, List<Integer>> disconnected = new HashMap<>();
        addEdge(disconnected, 0, 1);
        addEdge(disconnected, 1, 2);
        // 3 and 4 are isolated
        disconnected.put(3, new ArrayList<>());
        disconnected.put(4, new ArrayList<>());
        System.out.println("Components: " + countComponents(disconnected)); // Expected: 3
    }

    // =========================================================================
    // BFS — Breadth-First Search (Queue)
    // =========================================================================

    /**
     * Visits all reachable nodes from 'start' in BFS order (level by level).
     *
     * ALGORITHM:
     *   1. Add start to queue and mark visited.
     *   2. While queue is not empty: poll a node, add to result.
     *   3. For each unvisited neighbor: mark visited, add to queue.
     *
     * WHY MARK VISITED BEFORE ADDING TO QUEUE (not after dequeuing)?
     *   If we wait until dequeue to mark, multiple nodes can add the same neighbor
     *   to the queue before it's dequeued. This causes duplicate processing.
     *   Marking when enqueuing ensures each node is added to the queue exactly once.
     */
    public static List<Integer> bfs(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(start);
        visited.add(start); // Mark visited BEFORE adding to queue

        while (!queue.isEmpty()) {
            int node = queue.poll();
            result.add(node);

            for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);    // Mark before enqueuing
                    queue.offer(neighbor);
                }
            }
        }

        return result;
    }

    // =========================================================================
    // DFS — Recursive
    // =========================================================================

    /**
     * Recursive DFS using the call stack (implicit stack).
     *
     * ALGORITHM: Visit node, mark visited, recurse on each unvisited neighbor.
     *
     * PROS: Clean, readable.
     * CONS: Risk of StackOverflowError for very deep graphs (thousands of nodes).
     *   In interviews, always mention this limitation and offer iterative alternative.
     */
    public static List<Integer> dfsRecursive(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        dfsHelper(graph, start, visited, result);
        return result;
    }

    private static void dfsHelper(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited, List<Integer> result) {
        visited.add(node);
        result.add(node);

        for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                dfsHelper(graph, neighbor, visited, result); // Recurse deeper
            }
        }
        // When the for-loop ends with no unvisited neighbors → backtrack (return)
    }

    // =========================================================================
    // DFS — Iterative (explicit Stack)
    // =========================================================================

    /**
     * Iterative DFS using an explicit stack instead of recursion.
     *
     * IMPORTANT DIFFERENCE FROM BFS: Stack is LIFO → we process the most recently
     * added neighbor first. This gives DFS (last-in, first-out) order.
     *
     * NOTE: The order may differ from recursive DFS because we push all neighbors
     *       and process them LIFO (reverse order of recursive DFS).
     */
    public static List<Integer> dfsIterative(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            int node = stack.pop();

            if (visited.contains(node)) continue; // Already processed — skip

            visited.add(node);
            result.add(node);

            // Push neighbors — they'll be processed in LIFO order
            for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }

        return result;
    }

    // =========================================================================
    // Shortest Path using BFS (unweighted graph)
    // =========================================================================

    /**
     * Returns the shortest path from 'start' to 'target' in an unweighted graph.
     * BFS guarantees shortest path because it explores nodes in order of distance.
     *
     * APPROACH:
     *   Track the parent of each node. When we reach target, backtrack through
     *   parents to reconstruct the path.
     *
     * WHY BFS GIVES SHORTEST PATH:
     *   BFS visits nodes in non-decreasing order of distance from source.
     *   The first time we reach a node, we've reached it via the shortest route.
     *   (In weighted graphs, Dijkstra's algorithm is needed instead.)
     */
    public static List<Integer> shortestPathBFS(Map<Integer, List<Integer>> graph, int start, int target) {
        if (start == target) return List.of(start);

        Map<Integer, Integer> parent = new HashMap<>(); // parent[v] = which node led to v
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(start);
        parent.put(start, -1); // Start has no parent

        while (!queue.isEmpty()) {
            int node = queue.poll();

            for (int neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                if (!parent.containsKey(neighbor)) { // Not yet visited
                    parent.put(neighbor, node); // Record how we reached neighbor

                    if (neighbor == target) {
                        return reconstructPath(parent, start, target); // Found target
                    }

                    queue.offer(neighbor);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }

    /** Backtracks through parent map to reconstruct path from start to target */
    private static List<Integer> reconstructPath(Map<Integer, Integer> parent, int start, int target) {
        LinkedList<Integer> path = new LinkedList<>();
        int curr = target;

        while (curr != -1) { // -1 marks the start node (no parent)
            path.addFirst(curr); // Build path in reverse
            curr = parent.get(curr);
        }

        return path;
    }

    // =========================================================================
    // Has Path — DFS
    // =========================================================================

    /**
     * Returns true if there's any path from 'src' to 'dst'.
     * DFS with visited set to handle cycles.
     *
     * WHEN TO USE DFS over BFS for path existence:
     *   Both work. DFS is simpler to implement recursively.
     *   BFS is better when you also need the SHORTEST path.
     */
    public static boolean hasPathDFS(Map<Integer, List<Integer>> graph, int src, int dst, Set<Integer> visited) {
        if (src == dst) return true;
        if (visited.contains(src)) return false; // Already explored — avoid cycles

        visited.add(src);

        for (int neighbor : graph.getOrDefault(src, Collections.emptyList())) {
            if (hasPathDFS(graph, neighbor, dst, visited)) {
                return true; // Found a path through this neighbor
            }
        }

        return false; // No path through any neighbor of src
    }

    // =========================================================================
    // Count Connected Components
    // =========================================================================

    /**
     * Counts the number of connected components in an undirected graph.
     * Handles disconnected graphs by starting DFS from every unvisited vertex.
     *
     * PATTERN: "For each unvisited vertex, do DFS/BFS and count starts."
     *   This pattern applies to: number of islands, number of provinces (LC 547), etc.
     */
    public static int countComponents(Map<Integer, List<Integer>> graph) {
        Set<Integer> visited = new HashSet<>();
        int components = 0;

        for (int vertex : graph.keySet()) {
            if (!visited.contains(vertex)) {
                // New component found — DFS to mark all vertices in this component
                dfsHelper(graph, vertex, visited, new ArrayList<>());
                components++;
            }
        }

        return components;
    }

    // Helper to add undirected edge
    static void addEdge(Map<Integer, List<Integer>> graph, int u, int v) {
        graph.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        graph.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
    }
}
