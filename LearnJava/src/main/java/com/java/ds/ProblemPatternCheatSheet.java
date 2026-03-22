package com.java.ds;

/**
 * ============================================================
 * MASTER CHEAT SHEET: Problem Pattern Recognition Guide
 * ============================================================
 *
 * HOW TO USE THIS FILE:
 *   1. In an interview, identify the SIGNAL from the problem statement.
 *   2. Map the signal to a TECHNIQUE from Section 1.
 *   3. Apply the standard template for that technique.
 *   4. Follow the 45-minute time-box structure (Section 4).
 *
 * This file is intentionally comment-heavy — it IS the study notes.
 * ============================================================
 */
public class ProblemPatternCheatSheet {

    // =========================================================
    // SECTION 1: SIGNAL → TECHNIQUE MAP (55+ entries)
    // =========================================================

    /*
     * ════════════════════════════════════════════════════════════════════════════
     * ARRAY / TWO-POINTER SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Two numbers sum to target in SORTED array / target pair"
     * Technique: TWO-POINTER (lo=0, hi=n-1, move based on sum comparison)
     * Why: Sorted array → if sum too small, move lo right; if too large, move hi left.
     * Template: while(lo<hi) { sum=a[lo]+a[hi]; if sum==target return; else if sum<target lo++; else hi--; }
     * LC examples: 167 (Two Sum II), 15 (3Sum), 18 (4Sum)
     *
     * Signal: "Find closest pair / triplet with given sum in sorted array"
     * Technique: TWO-POINTER with closest tracking
     *
     * Signal: "Sort 0s, 1s, 2s in-place / sort array with 3 distinct values"
     * Technique: DUTCH NATIONAL FLAG (3-way partition, two pointers lo/hi + current)
     * Template: lo=0, hi=n-1, i=0; while(i<=hi): if a[i]==0 swap(lo,i), lo++,i++
     *           if a[i]==2 swap(i,hi), hi-- [don't advance i!]; else i++
     * LC: 75 (Sort Colors)
     *
     * Signal: "In-place array rotation by k positions"
     * Technique: THREE-REVERSAL TRICK — O(n) time, O(1) space
     * Template: reverse(0,n-1) → reverse(0,k-1) → reverse(k,n-1)
     * LC: 189 (Rotate Array)
     *
     * Signal: "Values in range [1..n], find missing number or duplicate"
     * Technique: CYCLE SORT — each element placed at its natural index (value-1)
     * Template: i=0; while(i<n) { correct=a[i]-1; if(a[i]!=a[correct]) swap(i,correct); else i++; }
     * LC: 41 (First Missing Positive), 442 (Find All Duplicates), 448 (Find Disappeared)
     *
     * Signal: "Subarray sum equals k (unsorted, may have negatives)"
     * Technique: PREFIX SUM + HASHMAP
     * Key insight: prefixSum[j] - prefixSum[i] = k → prefixSum[i] = prefixSum[j] - k
     * Template: map<prefixSum, count>; for each element: map.get(sum-k) gives count of valid subarrays
     * LC: 560 (Subarray Sum Equals K), 974 (Subarray Sums Divisible by K)
     *
     * Signal: "Maximum subarray sum"
     * Technique: KADANE'S ALGORITHM — O(n), O(1) space
     * Template: maxSoFar = maxEndingHere = nums[0]; for i in 1..n: maxEndingHere = max(nums[i], maxEndingHere+nums[i])
     * LC: 53 (Maximum Subarray)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * SLIDING WINDOW SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Longest substring/subarray satisfying a constraint"
     * Technique: SLIDING WINDOW (two pointers, expand right, shrink left when violated)
     * Template: left=0; for right in 0..n: update window state; while(violated) shrink left; update answer
     * LC: 3 (Longest Substring No Repeats), 76 (Minimum Window Substring), 567 (Permutation in String)
     *
     * Signal: "Maximum sum/length of subarray of FIXED size k"
     * Technique: FIXED SLIDING WINDOW — maintain window of size k
     *
     * Signal: "Max/min in every sliding window of size k"
     * Technique: MONOTONIC DEQUE — O(n), O(k) space
     * LC: 239 (Sliding Window Maximum)
     *
     * Signal: "Shortest subarray with sum >= k (negative numbers allowed)"
     * Technique: PREFIX SUM + MONOTONIC DEQUE — O(n)
     * LC: 862 (Shortest Subarray with Sum >= K)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * BINARY SEARCH SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Search in sorted array (or rotated sorted array)"
     * Technique: BINARY SEARCH — O(log n)
     * Rotated: determine which half is sorted, narrow to that half
     * LC: 704 (Binary Search), 33 (Search in Rotated Sorted Array)
     *
     * Signal: "Find minimum/maximum value such that [condition] is possible"
     * Technique: BINARY SEARCH ON ANSWER — search the answer space, not the input
     * Key: condition must be MONOTONIC (if k works, k+1 also works or vice versa)
     * Template: lo=minAns, hi=maxAns; while(lo<hi): mid=(lo+hi)/2; if feasible(mid): hi=mid; else lo=mid+1
     * LC: 875 (Koko Eating), 1011 (Ship Packages), 410 (Split Array), 1231 (Divide Chocolate)
     *
     * Signal: "Find peak element"
     * Technique: BINARY SEARCH — compare mid with mid+1, narrow to ascending side
     * LC: 162 (Find Peak Element), 852 (Peak Index in Mountain Array)
     *
     * Signal: "Square root / nth root of a number"
     * Technique: BINARY SEARCH ON FLOATING POINT — 100 iterations, no termination condition
     *
     * ════════════════════════════════════════════════════════════════════════════
     * LINKED LIST SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Detect cycle in linked list / find cycle start"
     * Technique: FAST/SLOW POINTERS (Floyd's algorithm)
     * Meeting point → move one to head, advance both by 1 → meet at cycle start
     * LC: 141 (Has Cycle), 142 (Cycle Detection Start)
     *
     * Signal: "Find middle of linked list"
     * Technique: FAST/SLOW POINTERS — slow moves 1, fast moves 2. When fast reaches end, slow is middle.
     * LC: 876 (Middle of Linked List)
     *
     * Signal: "kth element from end of linked list"
     * Technique: TWO POINTERS — advance fast by k first, then move both until fast reaches end
     *
     * Signal: "Merge two sorted linked lists / merge k sorted lists"
     * Technique: Two lists → iterative merge O(n+m). K lists → MIN-HEAP O(N log K)
     * LC: 21 (Merge Two), 23 (Merge K Sorted Lists)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * TREE SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Level order traversal / process tree level by level"
     * Technique: BFS with queue — add children to queue, process level by level
     *
     * Signal: "Lowest Common Ancestor"
     * Technique: DFS — if left and right both return non-null, current node is LCA
     * LC: 236 (LCA of Binary Tree), 235 (LCA of BST)
     *
     * Signal: "Diameter of tree / longest path between two nodes"
     * Technique: POST-ORDER DFS — at each node, diameter through node = left_depth + right_depth
     * Update global max at each node. Return max(left, right) + 1 up.
     * LC: 543 (Diameter of Binary Tree)
     *
     * Signal: "Path sum from root to leaf"
     * Technique: DFS with running sum, check at leaves
     * LC: 112 (Path Sum), 113 (Path Sum II), 437 (Path Sum III)
     *
     * Signal: "Serialize/deserialize binary tree"
     * Technique: Pre-order DFS with null markers, or BFS
     * LC: 297 (Serialize and Deserialize Binary Tree)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * STACK SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Next greater element / next smaller element"
     * Technique: MONOTONIC STACK — decreasing for NGE, increasing for NSE
     * O(n) amortized: each element pushed/popped at most once
     * LC: 496, 503 (Next Greater Element), 739 (Daily Temperatures)
     *
     * Signal: "Largest rectangle in histogram"
     * Technique: MONOTONIC INCREASING STACK — pop when current < top, compute area
     * LC: 84 (Largest Rectangle), 85 (Maximal Rectangle)
     *
     * Signal: "Valid parentheses / balanced brackets"
     * Technique: STACK — push open, pop on close and check match
     * LC: 20 (Valid Parentheses)
     *
     * Signal: "Evaluate expression / calculator"
     * Technique: TWO STACKS (operands + operators) or reverse polish notation
     * LC: 224 (Basic Calculator), 150 (Evaluate RPN)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * HEAP / PRIORITY QUEUE SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Kth largest / smallest element"
     * Technique: MIN-HEAP of size k (for kth largest), or MAX-HEAP of size k (for kth smallest)
     *            OR QUICKSELECT O(n) average — use for single query, heap for online/stream
     * LC: 215 (Kth Largest), 703 (Kth Largest in Stream)
     *
     * Signal: "Merge K sorted lists/arrays"
     * Technique: K-WAY MERGE with MIN-HEAP — O(N log K)
     * LC: 23 (Merge K Sorted Lists), 632 (Smallest Range Covering K Lists)
     *
     * Signal: "Median from data stream"
     * Technique: TWO HEAPS — max-heap (left half) + min-heap (right half)
     * O(log n) add, O(1) median
     * LC: 295 (Find Median from Data Stream)
     *
     * Signal: "Minimize max / maximize min across K groups"
     * Technique: BINARY SEARCH ON ANSWER + greedy feasibility check
     * (Often confused with heap problems — check if monotonic)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * GRAPH SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Shortest path in UNWEIGHTED graph"
     * Technique: BFS — visits nodes in non-decreasing distance order
     * LC: 127 (Word Ladder), 1091 (Shortest Path in Binary Matrix)
     *
     * Signal: "Shortest path in WEIGHTED graph (non-negative weights)"
     * Technique: DIJKSTRA's — priority queue (min-heap) of (distance, node)
     * O((V + E) log V)
     * LC: 743 (Network Delay Time), 1514 (Path with Maximum Probability)
     *
     * Signal: "Shortest path with NEGATIVE edge weights"
     * Technique: BELLMAN-FORD — relax all edges V-1 times. O(VE).
     * Can detect negative cycles (if still relaxing after V-1 rounds → negative cycle)
     *
     * Signal: "All pairs shortest path"
     * Technique: FLOYD-WARSHALL — O(V^3). dp[i][j][k] = shortest using vertices 0..k.
     * dp[i][j] = min(dp[i][j], dp[i][k] + dp[k][j])
     *
     * Signal: "Minimum spanning tree"
     * Technique: KRUSKAL's (sort edges + Union-Find) or PRIM's (min-heap + visited set)
     * Both O(E log V). Kruskal better for sparse graphs, Prim better for dense.
     * LC: 1135 (Connecting Cities with Minimum Cost)
     *
     * Signal: "Topological sort / course prerequisites / task ordering"
     * Technique: KAHN's algorithm (BFS-based, in-degree array) or DFS with post-order
     * LC: 207 (Course Schedule), 210 (Course Schedule II)
     *
     * Signal: "Number of connected components / union islands"
     * Technique: UNION-FIND (DSU) — O(α(n)) ≈ O(1) per union/find with path compression
     * OR DFS/BFS counting components
     * LC: 200 (Number of Islands), 547 (Number of Provinces), 684 (Redundant Connection)
     *
     * Signal: "Detect cycle in directed graph"
     * Technique: DFS with 3 states: unvisited (0), in-progress (1), done (2)
     * If you visit a node in state 1, there's a cycle.
     *
     * ════════════════════════════════════════════════════════════════════════════
     * DYNAMIC PROGRAMMING SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Count/find number of ways to reach target / make change"
     * Technique: DP — define dp[i] = ways to reach state i. Transition from previous states.
     * LC: 322 (Coin Change), 518 (Coin Change 2), 70 (Climbing Stairs)
     *
     * Signal: "Longest increasing subsequence (LIS)"
     * Technique: DP O(n^2) or PATIENCE SORT with binary search O(n log n)
     * LC: 300 (LIS), 354 (Russian Doll Envelopes — sort, then LIS)
     *
     * Signal: "Edit distance / string transformation"
     * Technique: 2D DP — dp[i][j] = edit distance of first i chars of s1, first j of s2
     * LC: 72 (Edit Distance), 44 (Wildcard Matching), 10 (Regular Expression Matching)
     *
     * Signal: "Longest common subsequence / substring"
     * Technique: 2D DP — dp[i][j] depends on s1[i]==s2[j] and dp[i-1][j-1], dp[i-1][j], dp[i][j-1]
     * LC: 1143 (LCS), 718 (Maximum Length of Repeated Subarray — LCS variant)
     *
     * Signal: "n <= 20, which subset was used"
     * Technique: BITMASK DP — dp[mask] = result for subset represented by bitmask
     * O(2^n * n) time and space
     * LC: 1986 (Minimum Number of Work Sessions), 847 (Shortest Path Visiting All Nodes)
     *
     * Signal: "Optimal strategy for two players taking from ends"
     * Technique: INTERVAL DP — dp[i][j] = optimal result for subarray [i..j]
     * LC: 877 (Stone Game), 375 (Guess Number Higher or Lower II)
     *
     * Signal: "Break string/array into parts satisfying some condition, maximize/minimize"
     * Technique: DP with binary search optimization if dp[i] = max/min(dp[j]) + cost(j+1..i)
     * And cost function satisfies the concave/convex hull trick condition
     *
     * ════════════════════════════════════════════════════════════════════════════
     * TREE MAP / SORTED STRUCTURE SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Find largest/smallest value <= or >= a given key in a running set"
     * Technique: TREEMAP (NavigableMap) — floorKey/ceilingKey in O(log n)
     * LC: 729 (My Calendar I), 1146 (Snapshot Array)
     *
     * Signal: "Interval scheduling / overlap detection with efficient queries"
     * Technique: TREEMAP or sorted list of intervals with binary search
     * LC: 729 (My Calendar I), 731 (My Calendar II), 732 (My Calendar III)
     *
     * Signal: "Merge overlapping intervals"
     * Technique: SORT by start, then linear scan merging. O(n log n)
     * LC: 56 (Merge Intervals), 57 (Insert Interval)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * STRING / PATTERN MATCHING SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Pattern match in O(n+m) (needle in haystack)"
     * Technique: KMP (Knuth-Morris-Pratt) — prefix function / failure function
     * Build failure array O(m), then scan O(n). Total O(n+m).
     * LC: 28 (Find Needle in Haystack), 214 (Shortest Palindrome)
     *
     * Signal: "Multiple pattern matching / detect duplicate substrings"
     * Technique: RABIN-KARP (rolling hash) — O(n+m) expected, O(nm) worst case
     * Or SUFFIX ARRAY for multiple patterns. Or TRIE for fixed patterns.
     *
     * Signal: "Longest palindromic substring in O(n)"
     * Technique: MANACHER'S ALGORITHM — transform to odd-length palindromes, expand
     * O(n) time, O(n) space.
     * LC: 5 (Longest Palindromic Substring)
     * Note: O(n^2) expand-around-center is acceptable for interviews (simpler code).
     *
     * Signal: "Longest palindromic SUBSEQUENCE"
     * Technique: DP — reverse string, find LCS. Or dp[i][j] directly.
     * LC: 516 (Longest Palindromic Subsequence)
     *
     * Signal: "Prefix queries / autocomplete / word search"
     * Technique: TRIE (prefix tree) — O(L) insert, O(L) search where L = word length
     * LC: 208 (Implement Trie), 212 (Word Search II uses Trie + DFS)
     *
     * Signal: "Count distinct substrings / longest repeated substring"
     * Technique: SUFFIX ARRAY + LCP array. Or Suffix Automaton.
     * Advanced — usually only in competitive programming.
     *
     * ════════════════════════════════════════════════════════════════════════════
     * RANGE QUERY SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Range sum query (no updates)"
     * Technique: PREFIX SUM — O(n) precompute, O(1) query
     *
     * Signal: "Range sum query WITH point updates"
     * Technique: FENWICK TREE (Binary Indexed Tree) — O(log n) update, O(log n) query
     * LC: 307 (Range Sum Query - Mutable), 315 (Count of Smaller Numbers After Self)
     *
     * Signal: "Range minimum/maximum query WITH range updates"
     * Technique: SEGMENT TREE with lazy propagation — O(log n) both
     * Or SPARSE TABLE for range minimum query without updates: O(n log n) precompute, O(1) query
     *
     * Signal: "Count smaller elements to right of each element"
     * Technique: FENWICK TREE + COORDINATE COMPRESSION (map values to ranks)
     * LC: 315 (Count of Smaller Numbers After Self)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * CACHING / DATA STRUCTURE DESIGN SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Design LRU cache"
     * Technique: HASHMAP + DOUBLY-LINKED LIST — O(1) get/put/evict
     * Quick version: LinkedHashMap with accessOrder=true, override removeEldestEntry
     * LC: 146 (LRU Cache)
     *
     * Signal: "Design LFU cache"
     * Technique: TWO HASHMAPS + DOUBLY-LINKED LIST
     *   Map<key, {value, freq}> + Map<freq, LinkedHashSet<keys>> + minFreq tracker
     * LC: 460 (LFU Cache)
     *
     * Signal: "Range add / range set + point query"
     * Technique: DIFFERENCE ARRAY — O(n) precompute, O(1) range update, O(n) finalize
     * Map<pos, delta> for sparse updates (TreeMap difference array)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * BACKTRACKING SIGNALS
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Generate all valid arrangements / paths satisfying constraints"
     * Technique: BACKTRACKING — build incrementally, prune invalid partial solutions early
     * Template: choose → recurse → unchoose (undo)
     * LC: 51 (N-Queens), 37 (Sudoku Solver), 131 (Palindrome Partitioning)
     *
     * Signal: "Word search in 2D grid"
     * Technique: DFS + BACKTRACKING — mark cell visited, recurse 4 directions, unmark
     * LC: 79 (Word Search)
     *
     * ════════════════════════════════════════════════════════════════════════════
     * CONCURRENCY SIGNALS (System Design / SDE3)
     * ════════════════════════════════════════════════════════════════════════════
     *
     * Signal: "Thread-safe counter / accumulator"
     * Technique: AtomicInteger/AtomicLong (CAS) or LongAdder (striped counter, better throughput)
     *
     * Signal: "Print alternating by multiple threads in order"
     * Technique: SEMAPHORE pair, or ReentrantLock + Condition, or synchronized + wait/notify
     *
     * Signal: "Rate limiter"
     * Technique: TOKEN BUCKET (refill tokens per interval) or SLIDING WINDOW COUNTER
     *
     * Signal: "Task execution with dependency ordering across threads"
     * Technique: CompletableFuture chain, or Phaser, or CountDownLatch
     */

    // =========================================================
    // SECTION 2: TIME COMPLEXITY QUICK REFERENCE
    // =========================================================

    /*
     * ┌──────────────────────────────────────┬─────────────────────────────────────┐
     * │ Algorithm                            │ Time Complexity                     │
     * ├──────────────────────────────────────┼─────────────────────────────────────┤
     * │ Linear scan                          │ O(n)                                │
     * │ Binary search                        │ O(log n)                            │
     * │ Two-pointer                          │ O(n)                                │
     * │ Sliding window                       │ O(n)                                │
     * │ Monotonic stack/deque                │ O(n) amortized                      │
     * │ Sorting (comparison)                 │ O(n log n)                          │
     * │ Counting/radix sort                  │ O(n+k) / O(d*(n+k))                │
     * │ QuickSelect                          │ O(n) avg, O(n^2) worst              │
     * │ BFS/DFS on graph                     │ O(V + E)                            │
     * │ Dijkstra's                           │ O((V+E) log V)                      │
     * │ Bellman-Ford                         │ O(VE)                               │
     * │ Floyd-Warshall                       │ O(V^3)                              │
     * │ Kruskal's / Prim's                   │ O(E log V)                          │
     * │ Topological sort (Kahn's)            │ O(V + E)                            │
     * │ Union-Find (path compression)        │ O(α(n)) ≈ O(1) per op              │
     * │ Trie insert/search                   │ O(L) per word, L = word length      │
     * │ Fenwick Tree update/query            │ O(log n)                            │
     * │ Segment Tree update/query            │ O(log n)                            │
     * │ Sparse Table (range min, no update)  │ O(n log n) build, O(1) query        │
     * │ KMP pattern match                    │ O(n + m)                            │
     * │ Rabin-Karp                           │ O(n + m) avg, O(nm) worst           │
     * │ Manacher's                           │ O(n)                                │
     * │ Subsets (2^n)                        │ O(2^n)                              │
     * │ Permutations (n!)                    │ O(n!)                               │
     * │ Bitmask DP                           │ O(2^n * n)                          │
     * │ 1D DP                               │ O(n) typically                      │
     * │ 2D DP (LCS, edit distance)           │ O(n * m)                            │
     * │ Interval DP                          │ O(n^3)                              │
     * │ Matrix chain multiplication          │ O(n^3)                              │
     * │ LIS with patience sort               │ O(n log n)                          │
     * │ k-way merge (N total, K lists)       │ O(N log K)                          │
     * │ Heap push/pop                        │ O(log n)                            │
     * │ Heap build (heapify)                 │ O(n) [NOT O(n log n)]               │
     * └──────────────────────────────────────┴─────────────────────────────────────┘
     *
     * SPACE COMPLEXITY QUICK REFERENCE:
     * ┌──────────────────────────────────────┬─────────────────────────────────────┐
     * │ Algorithm                            │ Space                               │
     * ├──────────────────────────────────────┼─────────────────────────────────────┤
     * │ Recursion (depth d)                  │ O(d) stack space                    │
     * │ BFS (queue)                          │ O(max level width)                  │
     * │ DFS (stack/recursion)                │ O(depth)                            │
     * │ Prefix sum                           │ O(n)                                │
     * │ Trie (N words, avg length L)         │ O(N * L * alphabet)                 │
     * │ 2D DP                               │ O(n * m), can optimize to O(n)      │
     * │ Union-Find                           │ O(n)                                │
     * │ Segment Tree                         │ O(4n) ≈ O(n)                        │
     * └──────────────────────────────────────┴─────────────────────────────────────┘
     */

    // =========================================================
    // SECTION 3: SPACE OPTIMIZATION DECISION TREE
    // =========================================================

    /*
     * HOW TO OPTIMIZE SPACE:
     *
     * ┌─ 1D DP with dp[i] depends on last k values?
     * │   YES → Use k rolling variables instead of O(n) array.
     * │          Fibonacci: only need prev2, prev1. O(n) → O(1).
     * │          Tribonacci: need prev3, prev2, prev1. O(n) → O(1).
     * └─ 1D DP with dp[i] depends on ALL previous dp[0..i-1]?
     *     → Cannot easily optimize below O(n).
     *
     * ┌─ 2D DP with dp[i][j] depends on dp[i-1][...]?
     * │   YES → Keep only 1 row: dp[j] represents current row.
     * │          Process left-to-right or right-to-left depending on dependency.
     * │          LCS: O(nm) → O(n) (one row at a time).
     * │          Knapsack 0/1: O(nW) → O(W) (traverse weight RIGHT-TO-LEFT to avoid reuse).
     * │          Knapsack unbounded: traverse LEFT-TO-RIGHT (allow reuse).
     * └─ 2D DP with dp[i][j] depends on dp[i-1][j-1], dp[i][j-1], dp[i-1][j]?
     *     → Keep 2 rows (current and previous). Still O(m) space.
     *
     * ┌─ Recursion stack too deep (StackOverflowError)?
     * │   Option 1: Convert to iterative with explicit Stack<> or Deque<>.
     * │   Option 2: Increase stack size: java -Xss4m (if you control JVM startup).
     * │   Option 3: Convert to tail recursion (note: Java does NOT optimize tail calls).
     * │   Option 4: Tabulation (bottom-up DP) instead of top-down memoization.
     * └─ Tree DFS with deeply unbalanced tree?
     *     → Use Morris traversal (O(1) space, no recursion, modifies tree temporarily).
     *
     * ┌─ Need to store all subsets/permutations but memory is limited?
     * │   → Generate them one by one (lazy iteration) instead of storing all upfront.
     * │   → Use bitmask iteration (current mask, compute next) without a results list.
     * └─ Rolling hash for string comparison with constant space?
     *     → Rabin-Karp rolling hash: remove leftmost char, add rightmost, compare hashes.
     *
     * COORDINATE COMPRESSION (for large value ranges):
     *   Values are large (e.g., 10^9) but count is small (n <= 10^5).
     *   Map values to rank [0..n-1]. Then use Fenwick Tree on ranks.
     *   Build: sort unique values, binary search to find rank.
     *   O(n log n) preprocessing, then O(log n) per query.
     */

    // =========================================================
    // SECTION 4: 45-MINUTE INTERVIEW TIME-BOX
    // =========================================================

    /*
     * ████████████████████████████████████████████████████████████
     * ████  THE 45-MINUTE INTERVIEW FRAMEWORK  ████████████████████
     * ████████████████████████████████████████████████████████████
     *
     * [0:00 – 0:05] CLARIFY — Ask constraints, edge cases, examples
     *   □ What are the constraints on n, values, time/space?
     *   □ Can values be negative? Zero? Duplicates?
     *   □ Is the array sorted? Can I sort it?
     *   □ Can I modify the input?
     *   □ What should be returned if no answer exists?
     *   □ Confirm understanding by restating the problem.
     *   TIP: Never skip this. Wrong assumptions = wrong solution = wasted 40 minutes.
     *
     * [0:05 – 0:10] BRUTE FORCE — State it even if suboptimal
     *   □ Say: "The brute force is [X]. It's O([time]) time and O([space]) space."
     *   □ Briefly explain why it works but is too slow.
     *   □ You may write pseudocode (not full code) for brute force.
     *   WHY: Shows you understand the problem. Gives you a correct baseline to optimize.
     *   TIP: Even if you know the optimal immediately, mention brute force briefly.
     *
     * [0:10 – 0:20] OPTIMAL APPROACH — Talk through before coding
     *   □ Identify the signal (from Section 1 of this file).
     *   □ Name the data structure / algorithm: "I'll use [X] because [Y]."
     *   □ Walk through the algorithm on the given example.
     *   □ State time and space complexity BEFORE coding.
     *   □ Ask: "Does this approach make sense to you?" → get buy-in.
     *   TIP: If you get stuck, try: smaller examples, draw the data structure,
     *        ask yourself "what information do I need at each step?"
     *
     * [0:20 – 0:35] CODE — Write clean, complete code
     *   □ Use meaningful variable names (not a,b,c — use lo,hi,left,right,count).
     *   □ Handle edge cases FIRST (empty array, n=0, n=1, single element, all negatives).
     *   □ Structure: main logic first, helper methods afterward.
     *   □ Talk through what you're writing: "I'm using a min-heap here because..."
     *   □ Don't erase code — cross it out and move on if wrong. Shows thinking process.
     *   TIP: Write the loop structure and invariant as a comment before the loop body.
     *
     * [0:35 – 0:42] TEST — Walk through examples, then edge cases
     *   □ Run through the provided example manually with your code.
     *   □ Test edge cases: empty input, single element, all same, already sorted, reverse sorted.
     *   □ Check off-by-one errors: loop bounds, array indices, ±1 adjustments.
     *   □ Verify your complexity claims are correct for the code written.
     *   TIP: Find bugs here, not when the interviewer is running their test cases.
     *
     * [0:42 – 0:45] OPTIMIZE / FOLLOW-UP — Demonstrate SDE3 thinking
     *   □ "If the data doesn't fit in memory, how would you scale this?"
     *   □ "How would you parallelize this?"
     *   □ "If this runs millions of times per second, what would you cache?"
     *   □ "Can the space complexity be reduced? (rolling array, in-place)"
     *   □ "Is there a more cache-friendly data structure?"
     *   TIP: These questions distinguish Senior from Junior. Always raise them yourself.
     *
     * ════════════════════════════════════════════════════════════════
     * GOTCHA: If you can't figure out the optimal, code the brute force correctly and
     *         explain what you'd change to optimize it. A correct O(n^2) is better than
     *         a buggy O(n) attempt that doesn't compile.
     * ════════════════════════════════════════════════════════════════
     */

    // =========================================================
    // SECTION 5: COMMON MISTAKES / GOTCHAS PER TOPIC
    // =========================================================

    /*
     * BINARY SEARCH:
     *   ✗ (lo+hi)/2 → integer overflow when lo+hi > Integer.MAX_VALUE
     *   ✓ lo + (hi-lo)/2
     *   ✗ while(lo<hi) with hi=mid-1 → may miss the answer
     *   ✓ For "find exact target": while(lo<=hi) with hi=mid-1, lo=mid+1
     *   ✓ For "find first true": while(lo<hi) with hi=mid, lo=mid+1
     *   ✗ Not handling empty array (arr.length == 0) → IndexOutOfBoundsException
     *
     * TWO POINTERS:
     *   ✗ Forgetting to handle duplicates in 3Sum → duplicate triplets in result
     *   ✓ Skip duplicates: while(lo < hi && nums[lo] == nums[lo-1]) lo++
     *   ✗ Moving both pointers when sum == target (for finding ALL pairs)
     *   ✓ Move both when sum == target and collecting all pairs
     *
     * SLIDING WINDOW:
     *   ✗ Shrinking window before checking window condition
     *   ✓ Expand right first, then shrink left while invalid
     *   ✗ Forgetting to update answer after shrinking (answer may be at the last valid state)
     *   ✓ Update answer every time the window is valid (or after shrinking)
     *
     * MONOTONIC STACK:
     *   ✗ Storing values instead of indices → can't compute distances or ranges
     *   ✓ Always store INDICES
     *   ✗ Forgetting to handle remaining elements in stack at end of array
     *   ✓ Use sentinel (0) at end, or process stack after main loop
     *   ✗ Wrong pop condition (< vs <=) changes strictly monotonic vs weak monotonic
     *
     * DP:
     *   ✗ dp[i] = answer for input of size i (ambiguous) → be precise about what dp[i] means
     *   ✓ Write "dp[i] = minimum cost to reach index i" explicitly
     *   ✗ Forgetting base cases → ArrayIndexOutOfBoundsException or wrong answers
     *   ✓ Initialize dp[] thoroughly: dp[0] = 0, dp[1] = something, handle n=0 separately
     *   ✗ 0/1 Knapsack: iterating items inner loop right-to-left vs. unbounded left-to-right
     *   ✓ 0/1 knapsack: iterate weight from W DOWN to w[i] to prevent reusing same item
     *
     * BACKTRACKING:
     *   ✗ Not making a COPY of current state when adding to results
     *   ✓ result.add(new ArrayList<>(current)) — not result.add(current)!
     *   ✗ Not undoing changes (backtracking) → corrupted state
     *   ✓ After recursive call: remove last element / unmark visited
     *
     * GRAPHS:
     *   ✗ Not marking nodes visited in BFS/DFS → infinite loops on cycles
     *   ✓ Use boolean[] visited or Set<Integer> visited; mark before processing
     *   ✗ Building adjacency list as 0-indexed when problem uses 1-indexed nodes
     *   ✓ Either adjust to 0-indexed or allocate n+1 array
     *
     * HEAP / PRIORITY QUEUE:
     *   ✗ Java PriorityQueue iterates in RANDOM order (not sorted)
     *   ✓ Use while(!pq.isEmpty()) pq.poll() to iterate in sorted order
     *   ✗ Mutable objects in heap → modifying them doesn't update heap order
     *   ✓ Remove, modify, re-insert (O(n) for remove by value — be careful)
     *   ✗ Not handling equal elements in custom Comparator → violates contract
     *
     * CONCURRENTHASHMAP:
     *   ✗ Assuming size() is exact → it's approximate
     *   ✓ Use mappingCount() for large maps
     *   ✗ Using null keys or null values → NullPointerException
     *   ✗ Compound operations (check-then-act) are NOT atomic unless using compute*/
     /*
     * STRING:
     *   ✗ String concatenation in a loop → O(n^2) due to immutable String
     *   ✓ Use StringBuilder, append(), then toString() → O(n)
     *   ✗ Comparing strings with == → compares references, not content
     *   ✓ Use .equals() always. Use == only for interned strings (usually avoid)
     *   ✗ charAt(i) - 'a' for non-lowercase letters → wrong index
     *   ✓ Verify character set assumption before index arithmetic
     *
     * INTEGER ARITHMETIC:
     *   ✗ int overflow in intermediate computation: n * (n+1) / 2 for n=100000 overflows int
     *   ✓ Cast to long: (long)n * (n+1) / 2
     *   ✗ Division before multiplication loses precision: 5/2*3 = 2*3 = 6 (not 7)
     *   ✓ Multiply first or use floating point carefully
     *   ✗ ceil(a/b) as a/b → floors! Use (a + b - 1) / b for integer ceil
     */

    // =========================================================
    // MINIMAL DEMO CODE (compilation verification)
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   Problem Pattern Cheat Sheet                ║");
        System.out.println("║   (This is a reference file — study notes)   ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        System.out.println("\nKey patterns to internalize:");
        System.out.println("1. Two-pointer: sorted array, pair sums");
        System.out.println("2. Sliding window: longest/shortest subarray with constraint");
        System.out.println("3. Binary search on answer: min/max with monotonic feasibility");
        System.out.println("4. Monotonic stack: next greater/smaller, histogram area");
        System.out.println("5. Monotonic deque: sliding window max/min");
        System.out.println("6. Two heaps: median from stream");
        System.out.println("7. K-way merge: merge sorted lists/files");
        System.out.println("8. Union-Find: connected components, cycle detection");
        System.out.println("9. Prefix sum + hashmap: subarray sum equals k");
        System.out.println("10. DP: optimal substructure + overlapping subproblems");

        System.out.println("\n45-minute framework:");
        System.out.println("0:00-0:05 Clarify | 0:05-0:10 Brute force");
        System.out.println("0:10-0:20 Optimal approach | 0:20-0:35 Code");
        System.out.println("0:35-0:42 Test | 0:42-0:45 Optimize/follow-up");

        // Integer ceiling without Math.ceil:
        int a = 7, b = 2;
        System.out.println("\nCeiling demo: ceil(7/2) = " + ((a + b - 1) / b)); // 4

        // Overflow-safe midpoint:
        int lo = 1_500_000_000, hi = 2_000_000_000;
        int safeMid = lo + (hi - lo) / 2;
        System.out.println("Safe midpoint: " + safeMid); // 1750000000

        System.out.println("\n=== Cheat sheet loaded ===");
    }
}
