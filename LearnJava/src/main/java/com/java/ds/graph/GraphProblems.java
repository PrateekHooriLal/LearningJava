package com.java.ds.graph;

import java.util.*;

/**
 * GRAPH PROBLEMS — LC Classic Set
 *
 * PROBLEMS COVERED:
 *   1. Number of Islands — LC 200 (DFS/BFS on grid)
 *   2. Clone Graph — LC 133 (DFS + HashMap)
 *   3. Flood Fill — LC 733 (BFS on grid)
 *   4. Rotten Oranges — LC 994 (Multi-source BFS)
 *
 * GRID AS GRAPH:
 *   A 2D grid is a graph where each cell is a vertex and
 *   adjacent cells (up/down/left/right) are edges.
 *   BFS/DFS on grid = BFS/DFS on graph — same algorithm.
 *
 * MULTI-SOURCE BFS (key pattern for LC 994, LC 286):
 *   Start BFS from MULTIPLE sources simultaneously.
 *   All sources are enqueued at the beginning.
 *   BFS expands outward from all sources at the same time.
 *   The level count = minimum distance to any source.
 */
public class GraphProblems {

    public static void main(String[] args) {

        System.out.println("=== LC 200: Number of Islands ===");
        char[][] grid1 = {
            {'1','1','1','1','0'},
            {'1','1','0','1','0'},
            {'1','1','0','0','0'},
            {'0','0','0','0','0'}
        };
        System.out.println(numIslands(grid1)); // Expected: 1

        char[][] grid2 = {
            {'1','1','0','0','0'},
            {'1','1','0','0','0'},
            {'0','0','1','0','0'},
            {'0','0','0','1','1'}
        };
        System.out.println(numIslands(grid2)); // Expected: 3

        System.out.println("\n=== LC 133: Clone Graph ===");
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.neighbors = Arrays.asList(n2, n4);
        n2.neighbors = Arrays.asList(n1, n3);
        n3.neighbors = Arrays.asList(n2, n4);
        n4.neighbors = Arrays.asList(n1, n3);
        Node cloned = cloneGraph(n1);
        System.out.println("Clone val: " + cloned.val + ", neighbors: " + cloned.neighbors.size()); // 1, 2
        System.out.println("Is different object: " + (cloned != n1)); // true

        System.out.println("\n=== LC 733: Flood Fill ===");
        int[][] image = {{1,1,1},{1,1,0},{1,0,1}};
        System.out.println(Arrays.deepToString(floodFill(image, 1, 1, 2)));
        // Expected: [[2,2,2],[2,2,0],[2,0,1]]

        System.out.println("\n=== LC 994: Rotten Oranges ===");
        int[][] oranges1 = {{2,1,1},{1,1,0},{0,1,1}};
        System.out.println(orangesRotting(oranges1)); // Expected: 4

        int[][] oranges2 = {{2,1,1},{0,1,1},{1,0,1}};
        System.out.println(orangesRotting(oranges2)); // Expected: -1 (unreachable fresh orange)

        int[][] oranges3 = {{0,2}};
        System.out.println(orangesRotting(oranges3)); // Expected: 0 (no fresh oranges)
    }

    // =========================================================================
    // LC 200 — Number of Islands
    // =========================================================================

    /**
     * Counts the number of distinct islands (groups of connected '1's).
     *
     * APPROACH: DFS — when we find an unvisited '1', start DFS to mark all
     *   connected '1's as visited. Each DFS start = one new island.
     *
     * TRICK: Modify grid in-place ('1' → '0') to mark visited, avoiding
     *   an extra boolean[][] visited array. Restore if needed (interviewer may ask).
     *
     * GRID-TO-GRAPH MAPPING:
     *   Vertex = each grid cell (i, j)
     *   Edge = between adjacent '1' cells (up/down/left/right)
     *
     * Time: O(m × n) — each cell visited at most once.  Space: O(m × n) stack.
     */
    public static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        int m = grid.length, n = grid[0].length;
        int islands = 0;

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == '1') {
                    // Found unvisited land — start DFS to mark the entire island
                    dfsIsland(grid, i, j);
                    islands++; // This DFS call covered one complete island
                }
            }
        }

        return islands;
    }

    private static void dfsIsland(char[][] grid, int r, int c) {
        int m = grid.length, n = grid[0].length;

        // Boundary check OR water OR already visited (marked '0')
        if (r < 0 || r >= m || c < 0 || c >= n || grid[r][c] != '1') return;

        grid[r][c] = '0'; // Mark as visited (sink the island cell)

        // Explore all 4 directions
        dfsIsland(grid, r + 1, c);
        dfsIsland(grid, r - 1, c);
        dfsIsland(grid, r, c + 1);
        dfsIsland(grid, r, c - 1);
    }

    // =========================================================================
    // LC 133 — Clone Graph
    // =========================================================================

    static class Node {
        int val;
        List<Node> neighbors;
        Node(int val) { this.val = val; this.neighbors = new ArrayList<>(); }
    }

    /**
     * Returns a deep clone (copy) of the graph.
     *
     * APPROACH: DFS + HashMap (original node → cloned node).
     *   The HashMap serves two purposes:
     *   1. Tracks which nodes have been cloned (visited set).
     *   2. Maps original node to its clone so we can wire up neighbor references.
     *
     * RECURSIVE STRUCTURE:
     *   cloneGraph(node):
     *     if already cloned → return existing clone
     *     create clone, store in map
     *     for each neighbor: recursively clone, add to clone's neighbors
     *     return clone
     *
     * WHY MAP AND NOT JUST VISITED SET?
     *   We need to set neighbor references in the clone.
     *   The map gives us the clone reference directly — O(1) lookup.
     */
    public static Node cloneGraph(Node node) {
        if (node == null) return null;

        Map<Node, Node> cloneMap = new HashMap<>(); // original → clone
        return cloneDFS(node, cloneMap);
    }

    private static Node cloneDFS(Node node, Map<Node, Node> cloneMap) {
        if (cloneMap.containsKey(node)) {
            return cloneMap.get(node); // Already cloned — return existing clone
        }

        Node clone = new Node(node.val);
        cloneMap.put(node, clone); // Register clone BEFORE recursing (handles cycles)

        for (Node neighbor : node.neighbors) {
            clone.neighbors.add(cloneDFS(neighbor, cloneMap)); // Recursively clone each neighbor
        }

        return clone;
    }

    // =========================================================================
    // LC 733 — Flood Fill
    // =========================================================================

    /**
     * Changes the color of all cells connected to (sr, sc) with the original color.
     * "Connected" = 4-directionally adjacent cells with the same color.
     *
     * APPROACH: DFS from (sr, sc). Change color as we go.
     *
     * EDGE CASE: If the starting cell already has the new color, return immediately.
     *   Without this check, DFS would run infinitely (the changed color == original → keep exploring).
     */
    public static int[][] floodFill(int[][] image, int sr, int sc, int color) {
        int originalColor = image[sr][sc];

        // Edge case: already the target color → nothing to do
        if (originalColor == color) return image;

        dfsFill(image, sr, sc, originalColor, color);
        return image;
    }

    private static void dfsFill(int[][] image, int r, int c, int originalColor, int newColor) {
        int m = image.length, n = image[0].length;

        if (r < 0 || r >= m || c < 0 || c >= n) return;       // Out of bounds
        if (image[r][c] != originalColor) return;               // Different color — don't fill

        image[r][c] = newColor; // Fill this cell

        dfsFill(image, r + 1, c, originalColor, newColor);
        dfsFill(image, r - 1, c, originalColor, newColor);
        dfsFill(image, r, c + 1, originalColor, newColor);
        dfsFill(image, r, c - 1, originalColor, newColor);
    }

    // =========================================================================
    // LC 994 — Rotten Oranges (Multi-Source BFS)
    // =========================================================================

    /**
     * Returns the minimum minutes until all oranges are rotten.
     * Returns -1 if some fresh orange is unreachable.
     *
     * MULTI-SOURCE BFS:
     *   All rotten oranges (initial sources) are enqueued at once.
     *   Each BFS level = 1 minute (all rotten oranges spread simultaneously).
     *   The number of BFS levels = minimum time.
     *
     * WHY BFS AND NOT DFS?
     *   BFS gives the MINIMUM spread time because it expands level-by-level.
     *   DFS might take a longer route — doesn't model simultaneous spreading.
     *
     * Grid values:
     *   0 = empty cell
     *   1 = fresh orange
     *   2 = rotten orange
     */
    public static int orangesRotting(int[][] grid) {
        int m = grid.length, n = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();
        int freshCount = 0;

        // Phase 1: Find all initial rotten oranges (multi-source BFS roots)
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 2) {
                    queue.offer(new int[]{i, j}); // All rotten oranges are starting points
                } else if (grid[i][j] == 1) {
                    freshCount++; // Count fresh oranges to verify all get rotten
                }
            }
        }

        if (freshCount == 0) return 0; // No fresh oranges — already done

        int minutes = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        // Phase 2: BFS — spread rottenness level by level
        while (!queue.isEmpty() && freshCount > 0) {
            minutes++; // Each BFS level = 1 minute
            int levelSize = queue.size(); // Process all current rotten oranges at once

            for (int i = 0; i < levelSize; i++) {
                int[] cell = queue.poll();

                for (int[] dir : dirs) {
                    int nr = cell[0] + dir[0];
                    int nc = cell[1] + dir[1];

                    if (nr >= 0 && nr < m && nc >= 0 && nc < n && grid[nr][nc] == 1) {
                        grid[nr][nc] = 2;          // Fresh → rotten
                        freshCount--;              // One fewer fresh orange
                        queue.offer(new int[]{nr, nc}); // This rotten orange will spread next minute
                    }
                }
            }
        }

        // If freshCount > 0, some fresh oranges were unreachable (isolated)
        return freshCount == 0 ? minutes : -1;
    }
}
