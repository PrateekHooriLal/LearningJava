# SDE 3 Java 21 — FAANG Learning Plan

> **Goal:** Fill gaps for a 10 YOE SDE 3 targeting FAANG/top-tier interviews.
> Each file = self-contained study notes + runnable code.

---

## Overall Progress

| Phase | Files | Done | Progress |
|-------|-------|------|----------|
| Module 0 — Core Technique Foundations | 4 | 4 | ✅ 100% |
| P0 Weeks 1-2 — DSA Gaps | 15 | 15 | ✅ 100% |
| P0 Weeks 3-4 — LLD | 6 | 6 | ✅ 100% |
| P0 Weeks 5-6 — Advanced Java | 8 | 8 | ✅ 100% |
| P1 Weeks 7-8 — Generics / Functional / Patterns / JVM | 15 | 15 | ✅ 100% |
| P2 — Collections / Sorting | 4 | 4 | ✅ 100% |
| Additions | 6 | 6 | ✅ 100% |
| **Total** | **62** | **62** | **🎉 100%** |

---

## Week-by-Week Daily Schedule (9 Weeks, Mon–Fri)

> **Daily routine:** Read the file's class-level javadoc (10 min) → study the implementations (20 min) → close the file, implement from scratch (30 min) → compare + fix (10 min) → solve the linked LeetCode problems (30 min).

| Week | Day | File | Key Topics | LeetCode |
|------|-----|------|------------|----------|
| **Week 0** | Mon | `ds/array/TwoPointerPatterns.java` | Opposite-ends, fast/slow, sliding window, read-write | LC 15, 42, 141, 3, 26 |
| | Tue | `ds/array/SwappingTechniques.java` | Cycle sort, Dutch flag, XOR swap, reversal | LC 268, 75, 189, 215 |
| | Wed | `ds/array/PivotTechniques.java` | QuickSort, rotated binary search, equilibrium | LC 33, 153, 724, 238 |
| | Thu | `ds/dp/DPFoundations.java` | 1D/2D DP, Kadane's, LCS, Coin Change | LC 70, 198, 53, 1143, 322 |
| | Fri | `ds/recursion/RecursionPatterns.java` | Subsets, permutations, divide & conquer | LC 78, 46, 77 |
| **Week 1** | Mon | `ds/trie/TrieDataStructure.java` | TrieNode, insert/search/prefix, WordDictionary | LC 208, 211, 212 |
| | Tue | `ds/trie/TrieProblems.java` | Autocomplete, replace words, longest prefix | LC 720, 648, 642 |
| | Wed | `ds/segmenttree/SegmentTree.java` | Build/query/update, lazy propagation | LC 307 |
| | Thu | `ds/segmenttree/FenwickTree.java` | BIT update/query, i&(-i) trick | LC 315 |
| | Fri | `ds/linkedlist/LinkedListAdvanced.java` | LRU/LFU from scratch, random pointer | LC 146, 460, 138 |
| **Week 2** | Mon | `ds/graph/ShortestPath.java` | Dijkstra, Bellman-Ford, Floyd-Warshall | LC 743, 787 |
| | Tue | `ds/graph/AdvancedGraphProblems.java` | Prim's, Kruskal's, Kosaraju's SCC | LC 1584, 778, 1631 |
| | Wed | `ds/dp/DPOnStrings.java` | Wildcard, Regex, Distinct Subseq, Interleave | LC 44, 10, 115, 97 |
| | Thu | `ds/dp/DPOnTrees.java` | House Robber III, Diameter, Max Path Sum | LC 337, 543, 124 |
| | Fri | `ds/dp/DPOnIntervals.java` | Burst Balloons, merge stones, interval DP | LC 312, 1000 |
| **Week 3** | Mon | `ds/dp/DPBitmask.java` | TSP, bitmask state, subset enumeration | LC 1986, 1349 |
| | Tue | `ds/stringalgo/KMPAlgorithm.java` | Failure function, O(n+m) search | LC 28, 459 |
| | Wed | `ds/stringalgo/RabinKarp.java` | Rolling hash, double-hashing | LC 1044 |
| | Thu | `ds/stringalgo/ZAlgorithm.java` | Z-array, pattern via concat | LC 796 |
| | Fri | `ds/stringalgo/ManacherAlgorithm.java` | Palindrome radius, O(n) | LC 5, 647 |
| **Week 4** | Mon | `lld/LRULFUCache.java` | Cache interface, Factory, ThreadSafeCache | Design question |
| | Tue | `lld/RateLimiter.java` | Token bucket, sliding window log | System design |
| | Wed | `lld/ParkingLot.java` | Sealed classes, Strategy, ReentrantLock | LLD round |
| | Thu | `lld/DesignThreadPool.java` | BlockingQueue workers, FutureTask, shutdown | LLD round |
| | Fri | `lld/BookMyShow.java` + `lld/RideSharingSystem.java` | State machine, Observer, Strategy | LLD round |
| **Week 5** | Mon | `concurrency/advanced/CompletableFutureDeepDive.java` | thenApply/thenCompose, allOf, error handling | Concurrency interview |
| | Tue | `concurrency/advanced/StampedLockDemo.java` | Optimistic read, Phaser | Concurrency interview |
| | Wed | `concurrency/advanced/ParallelStreamsDeepDive.java` | ForkJoin, RecursiveTask, when NOT to use parallel | Interview |
| | Thu | `concurrency/advanced/ReactiveStreamsIntro.java` | Flow API, backpressure | Interview |
| | Fri | `collections/advanced/ConcurrentCollectionsDeepDive.java` | CHM internals, COWAL, BlockingQueue | Interview |
| **Week 6** | Mon | `java21/StructuredConcurrencyDemo.java` | ShutdownOnFailure, task ownership | Java 21 interview |
| | Tue | `java21/ScopedValuesDemo.java` + `java21/StringTemplatesDemo.java` | ThreadLocal alternative, STR processor | Java 21 interview |
| | Wed | `java21/ForeignMemoryAPIDemo.java` | MemorySegment, Arena, off-heap | Java 21 interview |
| | Thu | `ds/stack/MonotonicStack.java` | NGE, histogram, daily temperatures | LC 84, 739, 735 |
| | Fri | `ds/stack/MonotonicDeque.java` + `ds/heap/HeapDeepDive.java` | Sliding max, median stream, K-way merge | LC 239, 295, 23 |
| **Week 7** | Mon | `generics/GenericsFoundations.java` | Type erasure, bounded params | Interview |
| | Tue | `generics/WildcardsAndVariance.java` | PECS, covariance, contravariance | Interview |
| | Wed | `generics/GenericAlgorithms.java` | Generic sort/search, recursive generics | Interview |
| | Thu | `functional/FunctionComposition.java` | andThen/compose, method refs, currying | Interview |
| | Fri | `functional/AdvancedStreams.java` + `functional/MonadPatterns.java` | Custom Collectors, teeing, Either monad | Interview |
| **Week 8** | Mon | `patterns/StructuralPatterns.java` | Adapter, Decorator, Proxy, Facade, Flyweight | Design interview |
| | Tue | `patterns/BehavioralPatterns.java` | Strategy, Command, Chain, State, Mediator | Design interview |
| | Wed | `patterns/CreationalPatterns.java` + `patterns/ModernArchitecturePatterns.java` | Abstract Factory, Prototype, CQRS, Circuit Breaker | Design interview |
| | Thu | `ds/bits/BitManipulation.java` + `ds/bits/AdvancedBitProblems.java` | Core tricks, XOR, bitmask subsets | LC 136, 137, 191, 421 |
| | Fri | `jvm/GarbageCollectionDemo.java` + `jvm/JVMMemoryModel.java` + `jvm/ClassLoadingDemo.java` | GC algorithms, JMM, happens-before, volatile | JVM interview |
| **Week 9** | Mon | `collections/advanced/AdvancedMapTechniques.java` | merge/compute, TreeMap navigation | Interview |
| | Tue | `ds/sorting/AdvancedSorting.java` | TimSort, Radix/Counting/Bucket sort | Interview |
| | Wed | `ds/sorting/BinarySearchVariants.java` | Left/right boundary, binary search on answer | LC 875, 410, 1011 |
| | Thu | `spring/SpringInternals.java` | @Transactional proxy, N+1, AOP, @Async | Spring interview |
| | Fri | `ds/ProblemPatternCheatSheet.java` | Full review: signal → technique map | Mock interview |

> **Tip:** On Friday of each week, do a 45-minute mock session using the cheat sheet — pick 2 random LC mediums/hards and time yourself.

---

## How to Use This Guide

1. **Check off files** as you complete them by replacing `- [ ]` with `- [x]`
2. **Study note comments** — every file has inline theory, complexity, gotchas, interview Q&A
3. **Guided Coding Helper** — before opening the file, read the template below, try to code it yourself, then compare
4. **Compile check** — after each file: `javac -encoding UTF-8 -cp src/main/java src/main/java/com/java/ds/...`
5. **Priority order** — Module 0 → P0 → P1 → P2 → Additions

---

## Module 0 — Core Technique Foundations ✅

> Do these FIRST. Every other DSA module depends on these mental models.

---

### ✅ Two-Pointer Patterns
**File:** `com/java/ds/array/TwoPointerPatterns.java`

- [x] Opposite-ends (Two Sum II, Three Sum, Trapping Rain Water, Container Water)
- [x] Fast + slow / Floyd's cycle (Linked List Cycle, Find Cycle Start, Happy Number)
- [x] Sliding window (Longest Substring, Min Window Substring, Fruit Baskets)
- [x] Read-write / partition pointer (Remove Duplicates, Move Zeroes, Remove Element)

<details>
<summary>📖 Guided Coding Helper</summary>

**When to use:** "Sorted array + pair/triplet sum" → opposite-ends. "Cycle/middle in linked list" → fast/slow. "Longest/shortest substring with property" → sliding window. "Remove in-place" → read-write.

**Opposite-Ends Template:**
```java
int lo = 0, hi = n - 1;
while (lo < hi) {
    int sum = arr[lo] + arr[hi];
    if (sum == target)      { /* collect */ lo++; hi--; }
    else if (sum < target)  lo++;
    else                    hi--;
}
```
**Gotcha:** skip duplicates after collecting: `while (lo<hi && arr[lo]==arr[lo+1]) lo++;`

**Sliding Window Template:**
```java
int lo = 0;
Map<Character, Integer> window = new HashMap<>();
for (int hi = 0; hi < s.length(); hi++) {
    window.merge(s.charAt(hi), 1, Integer::sum);  // expand
    while (/* constraint violated */) {
        window.merge(s.charAt(lo), -1, Integer::sum); // shrink
        lo++;
    }
    maxLen = Math.max(maxLen, hi - lo + 1);
}
```

**Interview Q:** "What's the difference between sliding window and opposite-ends?"
→ Sliding window: both pointers start at 0, move RIGHT only, array need not be sorted.
→ Opposite-ends: start at opposite ends, converge, REQUIRES sorted order.

</details>

---

### ✅ Swapping Techniques
**File:** `com/java/ds/array/SwappingTechniques.java`

- [x] Cycle Sort (Missing Number, All Duplicates, Find Duplicate, First Missing Positive)
- [x] Dutch National Flag (Sort Colors, Partition by Parity)
- [x] XOR Swap (theory + gotcha)
- [x] Reversal via Swap (Rotate Array, Reverse String, Reverse Words)
- [x] QuickSelect (Kth Largest Element)

<details>
<summary>📖 Guided Coding Helper</summary>

**Cycle Sort Template:**
```java
int i = 0;
while (i < nums.length) {
    int correctIdx = nums[i] - 1;               // value v belongs at index v-1
    if (nums[i] != nums[correctIdx]) {           // home not occupied by this value
        swap(nums, i, correctIdx);
    } else {
        i++;                                     // advance only when can't swap
    }
}
// after: indices where nums[i] != i+1 are missing/duplicate
```
**Precondition:** values must be in range [1..n]. Always check this before reaching for cycle sort.

**Dutch National Flag Template:**
```java
int lo = 0, mid = 0, hi = n - 1;
while (mid <= hi) {
    if      (nums[mid] == 0) { swap(lo, mid); lo++; mid++; }
    else if (nums[mid] == 1) { mid++; }
    else                     { swap(mid, hi); hi--; } // DON'T increment mid
}
```

**Three-Reversal Rotation:**
```java
k = k % n;
reverse(0, n-1);  // reverse all
reverse(0, k-1);  // reverse first k
reverse(k, n-1);  // reverse rest
```

</details>

---

### ✅ Pivot Techniques
**File:** `com/java/ds/array/PivotTechniques.java`

- [x] QuickSort Lomuto partition
- [x] QuickSort Hoare partition
- [x] Search in Rotated Sorted Array (LC 33)
- [x] Find Min in Rotated Array (LC 153, 154 with duplicates)
- [x] Find Pivot Index (LC 724)
- [x] Product of Array Except Self (LC 238)
- [x] Kth Largest via QuickSelect (LC 215)

<details>
<summary>📖 Guided Coding Helper</summary>

**Rotated Array Binary Search — Key Insight:**
One half is ALWAYS sorted. Identify which, then check if target is in the sorted half.
```java
if (nums[lo] <= nums[mid]) {
    // left half [lo..mid] is sorted
    if (nums[lo] <= target && target < nums[mid]) hi = mid - 1;
    else lo = mid + 1;
} else {
    // right half [mid..hi] is sorted
    if (nums[mid] < target && target <= nums[hi]) lo = mid + 1;
    else hi = mid - 1;
}
```

**Find Min in Rotated:**
```java
if (nums[mid] > nums[hi]) lo = mid + 1;  // min is in right half
else                       hi = mid;      // min is in left half (mid could BE min)
```

**Equilibrium Pivot:**
```java
int total = sum(nums), leftSum = 0;
for (int i = 0; i < n; i++) {
    if (leftSum == total - nums[i] - leftSum) return i;
    leftSum += nums[i];
}
```

</details>

---

### ✅ DP Foundations
**File:** `com/java/ds/dp/DPFoundations.java`

- [x] 1D linear: Fibonacci (3 ways), Climbing Stairs, House Robber, Jump Game I & II
- [x] Prefix/suffix: Kadane's (Max Subarray), Stock problems, Min Cost Stairs
- [x] 2D grid: Unique Paths, Min Path Sum, Dungeon Game (backwards fill)
- [x] Two sequences: LCS, Edit Distance
- [x] Memoization vs Tabulation: Coin Change both ways
- [x] Coin Change II (combination count vs permutation count — loop order trick)

<details>
<summary>📖 Guided Coding Helper</summary>

**The 3-Step DP Framework (say this in every interview):**
1. **DEFINE:** "dp[i] = ___" — write it as a sentence
2. **RECURRENCE:** how dp[i] depends on smaller states
3. **BASE CASE:** dp[0], dp[1] — seed the recurrence

**1D Rolling Array:**
When `dp[i]` only needs `dp[i-1]` and `dp[i-2]`, use two variables instead of array.
```java
int prev2 = base0, prev1 = base1;
for (int i = 2; i <= n; i++) {
    int cur = f(prev1, prev2);
    prev2 = prev1; prev1 = cur;
}
return prev1;
```

**Coin Change vs Coin Change II — Loop Order:**
- **Min coins** (Coin Change): either loop order works
- **Count combinations** (Coin Change II): coins OUTER, amount INNER
  → If amount is outer, you count permutations [1,2] and [2,1] separately

**2D Space Optimization:**
If `dp[r][c]` only uses `dp[r-1][c]` and `dp[r][c-1]`, use a 1D rolling array.

</details>

---

## Phase 1 — P0: DSA Gaps ✅ (Weeks 1-2)

---

### ✅ Trie Data Structure
**File:** `com/java/ds/trie/TrieDataStructure.java`

- [x] TrieNode (children[26] + isEnd boolean)
- [x] insert, search, startsWith, delete
- [x] WordDictionary (LC 211 — wildcard `.` via DFS)
- [x] WordSearch II (LC 212 — Trie + backtracking on grid)
- [x] Why Trie over HashMap for prefix queries

<details>
<summary>📖 Guided Coding Helper</summary>

**Trie Node:**
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEnd;
}
```

**Insert:**
```java
TrieNode cur = root;
for (char c : word.toCharArray()) {
    int idx = c - 'a';
    if (cur.children[idx] == null) cur.children[idx] = new TrieNode();
    cur = cur.children[idx];
}
cur.isEnd = true;
```

**Search (exact):** traverse + check `cur.isEnd`
**StartsWith (prefix):** traverse + check `cur != null` (no isEnd check)

**Wildcard `.` (LC 211):** when char is `.`, recurse into ALL non-null children.

**When Trie beats HashMap:** prefix search, autocomplete, shared-prefix compression.

</details>

---

### ✅ Trie Problems
**File:** `com/java/ds/trie/TrieProblems.java`

- [x] Longest Word With All Prefixes (LC 720)
- [x] Replace Words (LC 648)
- [x] Autocomplete System (LC 642 — top-3 by frequency)

---

### ✅ Segment Tree
**File:** `com/java/ds/segmenttree/SegmentTree.java`

- [x] Build / query / update for range sum
- [x] Lazy propagation for range update
- [x] 4-rule index arithmetic (left=2i, right=2i+1)
- [x] NumArray (LC 307)
- [x] Segment Tree vs Fenwick Tree comparison

<details>
<summary>📖 Guided Coding Helper</summary>

**Array-based Segment Tree (1-indexed, size 4*n):**
```java
void build(int node, int lo, int hi) {
    if (lo == hi) { tree[node] = nums[lo]; return; }
    int mid = (lo + hi) / 2;
    build(2*node, lo, mid);
    build(2*node+1, mid+1, hi);
    tree[node] = tree[2*node] + tree[2*node+1];
}

int query(int node, int lo, int hi, int l, int r) {
    if (r < lo || hi < l) return 0;               // out of range
    if (l <= lo && hi <= r) return tree[node];    // fully covered
    int mid = (lo + hi) / 2;
    return query(2*node, lo, mid, l, r)
         + query(2*node+1, mid+1, hi, l, r);
}
```

**Segment Tree vs Fenwick:**
| | Segment Tree | Fenwick Tree |
|--|--|--|
| Range update | ✅ (lazy prop) | limited |
| Flexibility | any associative op | prefix sums only |
| Code | medium | simple |

</details>

---

### ✅ Fenwick Tree (Binary Indexed Tree)
**File:** `com/java/ds/segmenttree/FenwickTree.java`

- [x] update(i, delta), prefixQuery(i), rangeQuery(l, r)
- [x] `i & (-i)` lowest-set-bit trick explained
- [x] Count of Smaller Numbers After Self (LC 315)

<details>
<summary>📖 Guided Coding Helper</summary>

**Core (the whole structure is these two methods):**
```java
void update(int i, int delta) {
    for (; i <= n; i += i & (-i))  // move UP: add lowest set bit
        tree[i] += delta;
}
int query(int i) {
    int sum = 0;
    for (; i > 0; i -= i & (-i))   // move DOWN: remove lowest set bit
        sum += tree[i];
    return sum;
}
```
**Range query:** `query(r) - query(l-1)`
**Always 1-indexed.** Map problem's 0-indexed values to 1-indexed.

</details>

---

### ✅ LinkedList Advanced
**File:** `com/java/ds/linkedlist/LinkedListAdvanced.java`

- [x] LRU Cache (LC 146) — DoublyLinkedList + HashMap, all O(1)
- [x] LFU Cache (LC 460) — minFreq + freqMap + keyMap
- [x] Copy List with Random Pointer (LC 138)

<details>
<summary>📖 Guided Coding Helper</summary>

**LRU Cache Design:**
HashMap<K, Node> for O(1) get + doubly linked list for O(1) reorder.
Head = MRU, tail = LRU. On `get`: promote to head. On `put` when full: remove tail + map entry.

**LFU Key Insight:**
`freqMap: Map<Integer, LinkedHashSet<K>>` — keys grouped by frequency.
`LinkedHashSet` preserves insertion order → iterator().next() = LRU within same freq.
`minFreq` resets to 1 on every new key insertion.

</details>

---

### ✅ Shortest Path Algorithms
**File:** `com/java/ds/graph/ShortestPath.java`

- [x] Dijkstra (min-heap, O((V+E)logV))
- [x] Bellman-Ford (O(VE), detects negative cycles)
- [x] Floyd-Warshall (O(V³), all-pairs)
- [x] Network Delay Time (LC 743), Cheapest Flights K Stops (LC 787)

<details>
<summary>📖 Guided Coding Helper</summary>

| Condition | Algorithm |
|-----------|-----------|
| Single source, non-negative weights | **Dijkstra** |
| Single source, negative weights | **Bellman-Ford** |
| All-pairs shortest paths | **Floyd-Warshall** |
| At most K edges constraint | **Bellman-Ford (K iterations)** |
| Unweighted graph | **BFS** |

**Dijkstra gotcha:** `if (d > dist[u]) continue;` — skip stale PQ entries.

**Bellman-Ford K stops:** Copy dist array before each pass to prevent same-pass chaining.

</details>

---

### ✅ Advanced Graph Problems
**File:** `com/java/ds/graph/AdvancedGraphProblems.java`

- [x] Prim's MST (min-heap)
- [x] Kruskal's MST (Union-Find)
- [x] Kosaraju's SCC (two-pass DFS)
- [x] Min Cost Connect All Points (LC 1584)
- [x] Swim in Rising Water (LC 778)
- [x] Path With Minimum Effort (LC 1631)

<details>
<summary>📖 Guided Coding Helper</summary>

**Kruskal's Template:**
```java
Arrays.sort(edges, (a, b) -> a[2] - b[2]);  // sort by weight
for (int[] edge : edges) {
    if (uf.union(edge[0], edge[1])) mstCost += edge[2];
}
```

**Kosaraju's SCC (2 passes):**
1. DFS original graph → push nodes to stack by finish time
2. Transpose graph (reverse all edges)
3. Pop from stack, DFS transposed → each DFS tree = one SCC

**Modified Dijkstra (LC 778, 1631):** minimize MAX weight along path instead of sum.

</details>

---

### ✅ DP on Strings
**File:** `com/java/ds/dp/DPOnStrings.java`

- [x] Wildcard Matching (LC 44)
- [x] Regex Matching (LC 10)
- [x] Distinct Subsequences (LC 115)
- [x] Interleaving String (LC 97)
- [x] Shortest Common Supersequence (LC 1092)

<details>
<summary>📖 Guided Coding Helper</summary>

**All string DP:** `dp[i][j]` = answer for `s1[0..i-1]` and `s2[0..j-1]`

**Wildcard `*`:** matches zero: `dp[i][j] = dp[i][j-1]` | matches one+: `dp[i][j] = dp[i-1][j]`

**SCS length:** `len(s1) + len(s2) - LCS_length`

</details>

---

### ✅ DP on Trees
**File:** `com/java/ds/dp/DPOnTrees.java`

- [x] House Robber III (LC 337) — rob/skip pair per node
- [x] Diameter of Binary Tree (LC 543)
- [x] Binary Tree Maximum Path Sum (LC 124)
- [x] Longest Univalue Path (LC 687)

<details>
<summary>📖 Guided Coding Helper</summary>

**Tree DP Pattern:** return two values (withNode, withoutNode) from each recursive call.
```java
int[] dfs(TreeNode node) {
    if (node == null) return new int[]{0, 0};
    int[] L = dfs(node.left), R = dfs(node.right);
    int rob   = node.val + L[1] + R[1];   // can't rob children
    int skip  = Math.max(L[0], L[1]) + Math.max(R[0], R[1]);
    return new int[]{rob, skip};
}
```

</details>

---

### ✅ DP on Intervals
**File:** `com/java/ds/dp/DPOnIntervals.java`

- [x] Burst Balloons (LC 312)
- [x] Strange Printer (LC 664)
- [x] Minimum Cost to Merge Stones (LC 1000)
- [x] Zuma Game (LC 488)

<details>
<summary>📖 Guided Coding Helper</summary>

**Interval DP Template (bottom-up by length):**
```java
for (int len = 2; len <= n; len++) {           // iterate by interval length
    for (int i = 0; i + len - 1 < n; i++) {   // left endpoint
        int j = i + len - 1;                   // right endpoint
        for (int k = i; k < j; k++) {          // split point
            dp[i][j] = Math.min(dp[i][j], dp[i][k] + dp[k+1][j] + cost(i,j,k));
        }
    }
}
```

**Burst Balloons insight:** think of k as the LAST balloon burst in [i..j], not the first.
This avoids broken references — at the time k is burst, i-1 and j+1 are still the boundary.

</details>

---

### ✅ DP Bitmask
**File:** `com/java/ds/dp/DPBitmask.java`

- [x] Traveling Salesman Problem (TSP)
- [x] Assign Work to Workers (LC 1986)
- [x] Maximum Students Taking Exam (LC 1349)
- [x] Minimum XOR Sum (LC 1879)

<details>
<summary>📖 Guided Coding Helper</summary>

**Bitmask DP Template (TSP-style):**
```java
// dp[mask][i] = min cost to visit all nodes in mask, ending at node i
dp[1 << src][src] = 0;
for (int mask = 0; mask < (1 << n); mask++) {
    for (int u = 0; u < n; u++) {
        if ((mask & (1 << u)) == 0) continue;  // u not in mask
        for (int v = 0; v < n; v++) {
            if ((mask & (1 << v)) != 0) continue;  // v already visited
            int newMask = mask | (1 << v);
            dp[newMask][v] = Math.min(dp[newMask][v], dp[mask][u] + cost[u][v]);
        }
    }
}
```

**When to use:** n ≤ 20, need to track subsets of elements as state.

**Bit tricks:**
- `mask | (1 << i)` — add bit i
- `mask & ~(1 << i)` — remove bit i
- `mask & (1 << i)` — check if bit i set
- `Integer.bitCount(mask)` — count set bits

</details>

---

### ✅ KMP Algorithm
**File:** `com/java/ds/stringalgo/KMPAlgorithm.java`

- [x] Failure function (prefix-suffix table) construction
- [x] Pattern search using failure function
- [x] Repeated Substring Pattern (LC 459)
- [x] Find All Anagrams (LC 438)

<details>
<summary>📖 Guided Coding Helper</summary>

**KMP vs Naive:** Naive = O(n×m). KMP = O(n+m) by never re-scanning matched chars.

**Failure Function:** `lps[i]` = length of longest proper prefix of `pat[0..i]` that is also a suffix.
```java
int[] lps = new int[m]; int len = 0, i = 1;
while (i < m) {
    if (pat.charAt(i) == pat.charAt(len)) { lps[i++] = ++len; }
    else if (len > 0) { len = lps[len - 1]; }  // fallback, don't increment i
    else              { lps[i++] = 0; }
}
```

**Interview Q:** "Why does KMP never go backwards in the text?"
→ The failure function encodes where to resume matching — no text position is re-visited.

</details>

---

### ✅ Rabin-Karp Algorithm
**File:** `com/java/ds/stringalgo/RabinKarp.java`

- [x] Rolling hash computation
- [x] Multiple pattern search
- [x] Longest Duplicate Substring (LC 1044)

<details>
<summary>📖 Guided Coding Helper</summary>

**Rolling Hash:** remove leftmost char, add rightmost char in O(1).
```java
hash = (hash * BASE + newChar) % MOD;             // add right
hash = (hash - leftChar * power % MOD + MOD) % MOD; // remove left
```
**+MOD before %MOD** — prevents negative results from subtraction.
**Use double-hashing** (two different MOD values) to reduce collision probability.

</details>

---

### ✅ Z-Algorithm
**File:** `com/java/ds/stringalgo/ZAlgorithm.java`

- [x] Z-array construction (O(n))
- [x] Pattern search via concatenation (pat + "$" + text)
- [x] Count distinct substrings

<details>
<summary>📖 Guided Coding Helper</summary>

**Z-array:** `z[i]` = length of longest substring starting at `s[i]` that matches a prefix of `s`.
```java
int l = 0, r = 0;
for (int i = 1; i < n; i++) {
    if (i < r) z[i] = Math.min(r - i, z[i - l]);
    while (i + z[i] < n && s.charAt(z[i]) == s.charAt(i + z[i])) z[i]++;
    if (i + z[i] > r) { l = i; r = i + z[i]; }
}
```

**Pattern search:** concat `pat + "$" + text`. Any position where `z[i] == m` is a match.

</details>

---

### ✅ Manacher's Algorithm
**File:** `com/java/ds/stringalgo/ManacherAlgorithm.java`

- [x] Palindrome radius array construction (O(n))
- [x] Longest Palindromic Substring (LC 5)
- [x] Count Palindromic Substrings (LC 647)

<details>
<summary>📖 Guided Coding Helper</summary>

**Transform:** Insert `#` between chars → uniform odd-length palindrome handling.
`"aba"` → `"#a#b#a#"` → center 3 has radius 3 → original palindrome length = radius.

**Core loop:** use mirror `mirror = 2*center - i` to reuse known palindrome radii.
```java
if (i < right) p[i] = Math.min(right - i, p[mirror]);
while (i - p[i] - 1 >= 0 && i + p[i] + 1 < n && t[i-p[i]-1] == t[i+p[i]+1]) p[i]++;
if (i + p[i] > right) { center = i; right = i + p[i]; }
```

</details>

---

## Phase 2 — P0: LLD Problems ✅ (Weeks 3-4)

---

### ✅ LRU / LFU Cache (LLD version)
**File:** `com/java/lld/LRULFUCache.java`

- [x] `Cache<K,V>` interface
- [x] Full LRU implementation — DoublyLinkedList + HashMap, O(1) get/put with sentinel nodes
- [x] Full LFU implementation — three-map design (keyMap + freqMap + minFreq), O(1)
- [x] `CacheFactory` (Factory pattern — Policy enum → LRU or LFU)
- [x] `ThreadSafeCache` decorator (ReadWriteLock)
- [x] `LinkedHashMap`-based LRU shortcut (3-line production version)

<details>
<summary>📖 Guided Coding Helper</summary>

**LRU Structure:**
```
[dummy_head] <-> [MRU node] <-> ... <-> [LRU node] <-> [dummy_tail]
```
On `get`: move node to front. On `put` when full: remove node before tail + map.remove(key).
**#1 bug:** forgetting `map.remove(lruKey)` on eviction → memory leak.

**LFU Three Maps:**
```java
Map<K, Entry<V>> keyMap;              // key → (value, freq)
Map<Integer, LinkedHashSet<K>> freqMap; // freq → ordered set of keys (LRU order)
int minFreq;
```
`minFreq` resets to 1 on every new key. Only increments naturally on existing-key access.

**Thread safety gotcha:** LRU `get()` modifies list structure → needs write lock, not read lock.

</details>

---

### ✅ Rate Limiter
**File:** `com/java/lld/RateLimiter.java`

- [x] Token Bucket (tokens + lastRefillTime + refill() per request)
- [x] Leaky Bucket (ArrayDeque as bucket)
- [x] Fixed Window Counter (ConcurrentHashMap + AtomicInteger)
- [x] Sliding Window Log (TreeMap<timestamp, count>)
- [x] Thread-safety analysis for each algorithm

<details>
<summary>📖 Guided Coding Helper</summary>

**Token Bucket Template:**
```java
synchronized boolean allow() {
    refill();
    if (tokens >= 1) { tokens--; return true; }
    return false;
}
void refill() {
    long now = System.currentTimeMillis();
    double added = (now - lastRefill) / 1000.0 * ratePerSec;
    tokens = Math.min(capacity, tokens + added);
    lastRefill = now;
}
```

**Which algorithm for which use case?**
- Payment API (strict, no burst): Leaky Bucket
- API gateway (allow burst): Token Bucket
- Simple per-minute limit: Fixed Window (cheap, slight edge-boundary issue)
- Strict sliding window: Sliding Window Log (expensive: O(requests) memory)

</details>

---

### ✅ Parking Lot
**File:** `com/java/lld/ParkingLot.java`

- [x] ParkingLot (Singleton), ParkingFloor, ParkingSpot (sealed: TwoWheeler/Car/Truck)
- [x] Vehicle (sealed: Bike/Car/Truck), Ticket, PricingStrategy
- [x] park(Vehicle) → nearest available spot, multi-floor
- [x] Thread-safe with ReentrantLock

<details>
<summary>📖 Guided Coding Helper</summary>

**Class hierarchy (sealed classes for exhaustive matching):**
```
ParkingSpot (abstract) → TwoWheelerSpot | CarSpot | TruckSpot
Vehicle (abstract)     → Bike | Car | Truck
PricingStrategy (interface) → HourlyPricing | FlatPricing
```

**Key design decision:** `park()` uses a TreeMap/PriorityQueue to find the nearest available spot — makes spot assignment deterministic and testable.

**Thread safety:** `ReentrantLock` per floor vs one global lock — finer granularity = better throughput.

</details>

---

### ✅ Design Thread Pool
**File:** `com/java/lld/DesignThreadPool.java`

- [x] BlockingQueue<Runnable> task queue
- [x] Fixed worker threads
- [x] shutdown() + awaitTermination()
- [x] Future-based submit() via FutureTask
- [x] Comparison with ThreadPoolExecutor rejection policies

<details>
<summary>📖 Guided Coding Helper</summary>

**Core loop (worker thread):**
```java
while (!shutdown || !taskQueue.isEmpty()) {
    Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
    if (task != null) task.run();
}
```

**Rejection policies (ThreadPoolExecutor):**
- AbortPolicy (default) — throws RejectedExecutionException
- CallerRunsPolicy — caller thread runs the task (natural backpressure)
- DiscardPolicy — silently drops
- DiscardOldestPolicy — drops oldest queued task

**Interview:** when would you use CallerRunsPolicy? → When you want back-pressure to slow the producer rather than drop work.

</details>

---

### ✅ BookMyShow
**File:** `com/java/lld/BookMyShow.java`

- [x] Theatre, Screen, Show, Seat, Booking classes
- [x] SeatLockService: 15-min expiry via ScheduledExecutorService
- [x] Concurrency: ConcurrentHashMap.computeIfAbsent + optimistic locking
- [x] PaymentService (Strategy: UPI/card/wallet)
- [x] WaitlistService (Queue + Observer pattern)

<details>
<summary>📖 Guided Coding Helper</summary>

**Seat booking concurrency problem:**
Two users select same seat simultaneously. Solution: optimistic locking with version check.
```java
boolean reserve(Seat seat, String userId) {
    synchronized(seat) {
        if (seat.isAvailable()) { seat.hold(userId); return true; }
        return false;
    }
}
```

**Seat lock expiry:** `ScheduledExecutorService.schedule(() -> seat.release(), 15, MINUTES)`
**Observer for waitlist:** `PaymentCompleted` event → notifies next waitlisted user.

</details>

---

### ✅ Ride Sharing System
**File:** `com/java/lld/RideSharingSystem.java`

- [x] Driver, Rider, Trip, Location, RideRequest classes
- [x] MatchingService (Strategy: nearest-driver, min-wait)
- [x] PricingService (surge multiplier)
- [x] TripStateMachine (REQUESTED→ACCEPTED→STARTED→COMPLETED/CANCELLED)
- [x] NotificationService (Observer on state change)

<details>
<summary>📖 Guided Coding Helper</summary>

**State machine (enum-based):**
```java
enum TripState { REQUESTED, ACCEPTED, STARTED, COMPLETED, CANCELLED }
// Allowed transitions enforced in Trip.transition(TripState next)
```

**Matching strategy (Strategy pattern):**
```java
interface MatchingStrategy { Driver match(RideRequest req, List<Driver> available); }
class NearestDriver implements MatchingStrategy { ... }
class MinWaitTime  implements MatchingStrategy { ... }
```

**Surge pricing:** `basePrice × surgeMultiplier` where multiplier = f(activeTrips / availableDrivers).

</details>

---

## Phase 3 — P0: Advanced Java ✅ (Weeks 5-6)

---

### ✅ CompletableFuture Deep Dive
**File:** `com/java/concurrency/advanced/CompletableFutureDeepDive.java`

- [x] supplyAsync, thenApply, thenCompose (flatMap), thenCombine, allOf, anyOf
- [x] exceptionally, handle, whenComplete
- [x] Real async pipeline: fetch user → orders → product details
- [x] orTimeout (Java 9+), custom thread pool
- [x] thenApply vs thenCompose explanation

<details>
<summary>📖 Guided Coding Helper</summary>

**thenApply vs thenCompose:**
- `thenApply(f)` — f returns a plain value T → result: `CompletableFuture<T>`
- `thenCompose(f)` — f returns `CompletableFuture<T>` → unwraps it → avoids `CompletableFuture<CompletableFuture<T>>`
  → thenCompose is the flatMap of async world

**Error handling order of preference:**
1. `exceptionally` — recover from error
2. `handle` — handle both success and error
3. `whenComplete` — side-effects only (can't transform result)

**allOf gotcha:** returns `CompletableFuture<Void>`, not the results. Chain `.thenApply` to collect.

</details>

---

### ✅ StampedLock Demo
**File:** `com/java/concurrency/advanced/StampedLockDemo.java`

- [x] StampedLock vs ReentrantReadWriteLock comparison
- [x] tryOptimisticRead + validate pattern
- [x] Stamp conversion (read → write)
- [x] PhaserDemo (reusable barrier vs CyclicBarrier)

<details>
<summary>📖 Guided Coding Helper</summary>

**Optimistic Read Pattern:**
```java
long stamp = lock.tryOptimisticRead();
int localX = x; int localY = y;            // read without lock
if (!lock.validate(stamp)) {               // check if write happened
    stamp = lock.readLock();               // fall back to read lock
    try { localX = x; localY = y; }
    finally { lock.unlockRead(stamp); }
}
```
**Why faster:** optimistic reads don't block writers; only validate (CAS check) after reading.
**When to use:** read-heavy, writes rare, reads are short (quick validation window).

</details>

---

### ✅ Parallel Streams Deep Dive
**File:** `com/java/concurrency/advanced/ParallelStreamsDeepDive.java`

- [x] ForkJoinPool internals (work-stealing)
- [x] When parallel is faster vs slower
- [x] Custom ForkJoinPool via submit()
- [x] RecursiveTask vs RecursiveAction
- [x] Parallel merge sort

<details>
<summary>📖 Guided Coding Helper</summary>

**When parallel streams HURT performance:**
- Small data (< ~10k elements) — thread overhead dominates
- Ordered operations on ordered sources (`findFirst`, `forEachOrdered`)
- Boxing/unboxing (use `IntStream`, `LongStream` instead)
- Non-associative reduce operations

**Custom ForkJoinPool (to set thread count):**
```java
ForkJoinPool pool = new ForkJoinPool(4);
pool.submit(() -> list.parallelStream().map(...).collect(...)).get();
```

</details>

---

### ✅ Reactive Streams Intro
**File:** `com/java/concurrency/advanced/ReactiveStreamsIntro.java`

- [x] Java 9 Flow API: Publisher, Subscriber, Subscription, Processor
- [x] Manual Publisher implementation
- [x] Backpressure concept
- [x] Compare with blocking queues / CompletableFuture

---

### ✅ Structured Concurrency (Java 21)
**File:** `com/java/java21/StructuredConcurrencyDemo.java`

- [x] StructuredTaskScope: ShutdownOnFailure vs ShutdownOnSuccess
- [x] Tree-of-tasks ownership model
- [x] Cancel-on-failure (fixes CompletableFuture.allOf gap)
- [x] Compare with ExecutorService

<details>
<summary>📖 Guided Coding Helper</summary>

**ShutdownOnFailure template:**
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<User>  user   = scope.fork(() -> fetchUser(id));
    Future<Order> order  = scope.fork(() -> fetchOrder(id));
    scope.join().throwIfFailed();
    return new Response(user.get(), order.get());
}
```
**Key advantage over CompletableFuture.allOf:** if one subtask fails, others are automatically cancelled. allOf leaves other futures running.

</details>

---

### ✅ String Templates (Java 21 Preview)
**File:** `com/java/java21/StringTemplatesDemo.java`

- [x] STR template processor
- [x] FMT template processor (formatted output)
- [x] Custom template processor implementation
- [x] Comparison with String.format / formatted()

---

### ✅ Foreign Memory API (Java 21)
**File:** `com/java/java21/ForeignMemoryAPIDemo.java`

- [x] MemorySegment allocation (on/off heap)
- [x] MemoryLayout (struct definition)
- [x] Arena-based lifecycle management
- [x] Compare with ByteBuffer

---

### ✅ Scoped Values (Java 21)
**File:** `com/java/java21/ScopedValuesDemo.java`

- [x] ScopedValue vs ThreadLocal comparison
- [x] `ScopedValue.where(...).run(...)` pattern
- [x] Inheritance across virtual threads
- [x] Use case: per-request context propagation

---

## Phase 4 — P1: Generics / Functional / Patterns / JVM ✅ (Weeks 7-8)

---

### ✅ Generics Foundations
**File:** `com/java/generics/GenericsFoundations.java`

- [x] Generic classes, methods, bounded type parameters
- [x] Type erasure and its consequences
- [x] Reifiable vs non-reifiable types
- [x] Generic method return type inference

<details>
<summary>📖 Guided Coding Helper</summary>

**Type Erasure:** at runtime, `List<String>` and `List<Integer>` are both `List`. Generic type info is erased.
Consequence: can't do `new T[]`, `instanceof List<String>`, or `T.class`.

**Bounded type parameter:**
```java
<T extends Comparable<T>> T max(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }
```

**Multiple bounds:** `<T extends Comparable<T> & Serializable>` — class bound must come first.

</details>

---

### ✅ Wildcards and Variance
**File:** `com/java/generics/WildcardsAndVariance.java`

- [x] `? extends T` (covariance / upper bounded — read-only)
- [x] `? super T` (contravariance / lower bounded — write-only)
- [x] PECS rule (Producer Extends, Consumer Super)
- [x] Wildcard capture

<details>
<summary>📖 Guided Coding Helper</summary>

**PECS Rule:**
- `<? extends T>` — you READ from it (it's a producer of T). Can't add (type unsafe).
- `<? super T>` — you WRITE to it (it's a consumer of T). Reads return Object.

```java
// Copy from src to dst
void copy(List<? extends T> src, List<? super T> dst) {
    for (T t : src) dst.add(t);
}
```

**Real-world example:** `Collections.sort(List<T> list, Comparator<? super T> c)` — Comparator is a consumer of T.

</details>

---

### ✅ Generic Algorithms
**File:** `com/java/generics/GenericAlgorithms.java`

- [x] Generic sort, search, min/max implementations
- [x] Generic stack, queue, pair
- [x] Recursive generic algorithms (generic merge sort)

---

### ✅ Function Composition
**File:** `com/java/functional/FunctionComposition.java`

- [x] Function.andThen vs Function.compose
- [x] Predicate.and / or / negate
- [x] Method references (4 kinds)
- [x] Currying and partial application in Java

<details>
<summary>📖 Guided Coding Helper</summary>

**andThen vs compose:**
```java
f.andThen(g)  // g(f(x)) — f first, then g
f.compose(g)  // f(g(x)) — g first, then f
```

**4 method reference kinds:**
1. `String::toUpperCase` — instance method, implicit receiver
2. `str::startsWith` — instance method, explicit receiver
3. `Integer::parseInt` — static method
4. `ArrayList::new` — constructor

**Currying:** `Function<A, Function<B, C>>` — convert multi-arg function to chain of single-arg functions.

</details>

---

### ✅ Advanced Streams
**File:** `com/java/functional/AdvancedStreams.java`

- [x] Custom Collectors (Collector.of)
- [x] Collectors.teeing (Java 12)
- [x] Stream.gather (Java 22 preview)
- [x] Infinite streams (iterate, generate)
- [x] flatMap vs mapMulti (Java 16)

---

### ✅ Monad Patterns
**File:** `com/java/functional/MonadPatterns.java`

- [x] Optional as a monad (flatMap chains)
- [x] Either<L,R> monad implementation
- [x] Result<T,E> monad for error handling
- [x] Comparison with checked exceptions

---

### ✅ Structural Patterns
**File:** `com/java/patterns/StructuralPatterns.java`

- [x] Adapter (legacy API integration)
- [x] Decorator (add behavior without subclassing)
- [x] Proxy (lazy init, access control)
- [x] Facade (simplify complex subsystem)
- [x] Composite (tree structures)
- [x] Bridge (decouple abstraction from implementation)
- [x] Flyweight (share common state)

<details>
<summary>📖 Guided Coding Helper</summary>

**Adapter vs Facade:**
- Adapter: makes ONE incompatible interface work with another
- Facade: simplifies MULTIPLE complex subsystems into one clean API

**Decorator vs Proxy:**
- Decorator: adds NEW behavior, controlled by client
- Proxy: controls ACCESS to existing behavior (same interface, different purpose)

**Flyweight:** share intrinsic state (immutable, shared) across many objects. Store extrinsic state (context-specific) outside.
Java String pool is a Flyweight.

</details>

---

### ✅ Behavioral Patterns
**File:** `com/java/patterns/BehavioralPatterns.java`

- [x] Strategy (interchangeable algorithms)
- [x] Command (encapsulate requests, undo/redo)
- [x] Chain of Responsibility (middleware pipelines)
- [x] Template Method (skeleton + hooks)
- [x] Iterator (custom iteration)
- [x] Mediator (reduce object coupling)
- [x] State (state machine without if/else chains)

---

### ✅ Creational Patterns
**File:** `com/java/patterns/CreationalPatterns.java`

- [x] Abstract Factory (families of related objects)
- [x] Prototype (clone-based creation)
- [x] Object Pool (reuse expensive objects)
- [x] Dependency Injection (manual wiring)

---

### ✅ Modern Architecture Patterns
**File:** `com/java/patterns/ModernArchitecturePatterns.java`

- [x] CQRS (Command Query Responsibility Segregation)
- [x] Event Sourcing
- [x] Saga pattern (distributed transactions)
- [x] Circuit Breaker
- [x] Outbox pattern

---

### ✅ Bit Manipulation
**File:** `com/java/ds/bits/BitManipulation.java`

- [x] Core tricks (set/clear/toggle/check bit)
- [x] Single Number I & II (LC 136, 137)
- [x] Number of 1 Bits (LC 191)
- [x] Reverse Bits (LC 190)
- [x] Power of Two / Three (LC 231, 326)
- [x] Sum of Two Integers without + (LC 371)

<details>
<summary>📖 Guided Coding Helper</summary>

**Core bit operations:**
```java
n & (n-1)          // clear lowest set bit (n becomes 0 if power of 2)
n & (-n)           // isolate lowest set bit
n ^ n = 0          // XOR with itself = 0 (used in Single Number)
n ^ 0 = n          // XOR with 0 = no change
a ^ b ^ b = a      // XOR is its own inverse
```

**Sum without +:**
```java
while (b != 0) {
    int carry = (a & b) << 1;  // carry bits
    a = a ^ b;                  // sum without carry
    b = carry;
}
return a;
```

**Single Number II (every number appears 3x, one appears 1x):**
Use `ones` and `twos` to simulate ternary counting per bit.

</details>

---

### ✅ Advanced Bit Problems
**File:** `com/java/ds/bits/AdvancedBitProblems.java`

- [x] Subsets via bitmask enumeration
- [x] Maximum XOR of Two Numbers (LC 421 — Trie approach)
- [x] Bitwise AND of Numbers Range (LC 201)
- [x] Total Hamming Distance (LC 477)

---

### ✅ Garbage Collection Demo
**File:** `com/java/jvm/GarbageCollectionDemo.java`

- [x] GC algorithms: Serial, Parallel, G1, ZGC, Shenandoah
- [x] Generational hypothesis (young/old gen)
- [x] GC roots and reachability
- [x] Memory leak patterns (static collections, listeners, inner classes)
- [x] GC tuning flags (-Xms, -Xmx, -XX:+UseG1GC)

<details>
<summary>📖 Guided Coding Helper</summary>

**GC Algorithm Cheat Sheet:**
| GC | Pause | Throughput | Use Case |
|----|-------|------------|----------|
| Serial | High | Low | Single-core, small heap |
| Parallel | Medium | High | Batch processing |
| G1 | Low | Medium | Default Java 9+; large heap |
| ZGC | Sub-ms | Medium | Latency-critical, huge heap |
| Shenandoah | Sub-ms | Medium | Red Hat alternative to ZGC |

**Common memory leak patterns:**
1. `static` field holding large object (lives forever)
2. Unclosed resources (InputStream, Connection) — use try-with-resources
3. `HashSet` of mutable objects (hashCode changes → objects unreachable but in set)
4. Listener/callback not unregistered

</details>

---

### ✅ JVM Memory Model
**File:** `com/java/jvm/JVMMemoryModel.java`

- [x] Heap (young gen: eden + S0/S1, old gen) vs non-heap (Metaspace, stack, code cache)
- [x] Java Memory Model (JMM): happens-before, visibility, ordering
- [x] volatile semantics (visibility + no reorder, not atomicity)
- [x] Double-checked locking — broken without volatile
- [x] Memory barriers (LoadLoad, StoreStore, LoadStore, StoreLoad)

<details>
<summary>📖 Guided Coding Helper</summary>

**volatile guarantees:**
1. **Visibility:** write to volatile is immediately visible to all threads
2. **Ordering:** no reordering across a volatile read/write
3. **NOT atomic:** `i++` on volatile is still not atomic (still read-modify-write)

**Double-checked locking (correct version):**
```java
private volatile static Singleton instance;
public static Singleton getInstance() {
    if (instance == null) {                    // first check (no lock)
        synchronized(Singleton.class) {
            if (instance == null)              // second check (with lock)
                instance = new Singleton();   // volatile prevents reorder
        }
    }
    return instance;
}
```
**Without volatile:** constructor could be partially published — another thread sees non-null but uninitialized object.

</details>

---

### ✅ Class Loading Demo
**File:** `com/java/jvm/ClassLoadingDemo.java`

- [x] Class loading phases: loading, linking (verify/prepare/resolve), initialization
- [x] Parent delegation model
- [x] Custom ClassLoader implementation
- [x] Class unloading conditions

---

## Phase 5 — P2: Collections / Sorting ✅

---

### ✅ Concurrent Collections Deep Dive
**File:** `com/java/collections/advanced/ConcurrentCollectionsDeepDive.java`

- [x] ConcurrentHashMap internals (segmented/CAS locking, Java 8 tree bins)
- [x] CopyOnWriteArrayList (when to use, cost of write)
- [x] BlockingQueue family (LinkedBlockingQueue, ArrayBlockingQueue, PriorityBlockingQueue)
- [x] ConcurrentSkipListMap (sorted concurrent map)

<details>
<summary>📖 Guided Coding Helper</summary>

**ConcurrentHashMap vs Hashtable vs synchronizedMap:**
- `Hashtable` / `synchronizedMap`: one global lock → all ops serialized
- `ConcurrentHashMap`: fine-grained locking (per bucket), reads mostly lock-free → much higher concurrency

**Java 8 improvement:** when bucket has > 8 entries, list → balanced tree (Red-Black) → O(log n) worst case.

**CopyOnWriteArrayList tradeoff:**
- Reads: zero synchronization (snapshot)
- Writes: copy entire array → O(n) → good ONLY for very-rarely-written, frequently-iterated lists
- Use case: event listener lists, observer registries

</details>

---

### ✅ Advanced Map Techniques
**File:** `com/java/collections/advanced/AdvancedMapTechniques.java`

- [x] merge, compute, computeIfAbsent, computeIfPresent
- [x] TreeMap navigation (floorKey, ceilingKey, subMap, headMap, tailMap)
- [x] LinkedHashMap as LRU cache (removeEldestEntry)
- [x] EnumMap vs HashMap for enum keys

---

### ✅ Advanced Sorting
**File:** `com/java/ds/sorting/AdvancedSorting.java`

- [x] TimSort (Java's Arrays.sort internals)
- [x] Counting Sort / Radix Sort / Bucket Sort (O(n) sorts)
- [x] External sort (merge sort for data that doesn't fit in memory)
- [x] Intro Sort (introsort: quicksort + heapsort fallback)

<details>
<summary>📖 Guided Coding Helper</summary>

**When to choose non-comparison sort:**
- Values in small range [0..k]: Counting Sort O(n+k)
- Integers, fixed-width strings: Radix Sort O(d×n)
- Uniformly distributed floats [0,1): Bucket Sort O(n) avg

**TimSort (Java Arrays.sort for objects):**
- Finds existing runs (sorted subsequences)
- Merges runs with binary insertion sort for small runs
- O(n log n) worst, O(n) best (already sorted)

**Interview:** "Why does Arrays.sort use different algorithms for primitives vs objects?"
→ Primitives: Dual-Pivot QuickSort (faster, no stability needed)
→ Objects: TimSort (stable — preserves equal element order, required for Comparator chaining)

</details>

---

### ✅ Binary Search Variants
**File:** `com/java/ds/sorting/BinarySearchVariants.java`

- [x] Standard binary search (exact match)
- [x] Left boundary (first occurrence / first ≥ target)
- [x] Right boundary (last occurrence / last ≤ target)
- [x] Binary search on answer (minimize maximum, maximize minimum)
- [x] Koko Eating Bananas (LC 875), Split Array Largest Sum (LC 410)

<details>
<summary>📖 Guided Coding Helper</summary>

**Left boundary (first position ≥ target):**
```java
int lo = 0, hi = n;  // hi = n (open right bound)
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (nums[mid] < target) lo = mid + 1;
    else                    hi = mid;       // could be the answer, don't exclude
}
return lo;  // lo == hi == first index where nums[i] >= target
```

**Binary search on answer template:**
```java
int lo = minPossible, hi = maxPossible;
while (lo < hi) {
    int mid = lo + (hi - lo) / 2;
    if (feasible(mid)) hi = mid;   // try smaller
    else               lo = mid + 1;
}
return lo;
```
**Signal:** "minimize the maximum" or "maximize the minimum" → binary search on the answer.

</details>

---

## Additions ✅

---

### ✅ Monotonic Stack
**File:** `com/java/ds/stack/MonotonicStack.java`

- [x] Next Greater Element (LC 496, 503)
- [x] Largest Rectangle in Histogram (LC 84)
- [x] Daily Temperatures (LC 739)
- [x] Asteroid Collision (LC 735)

<details>
<summary>📖 Guided Coding Helper</summary>

**Monotonic Stack Template (Next Greater Element):**
```java
int[] result = new int[n]; Arrays.fill(result, -1);
Deque<Integer> stack = new ArrayDeque<>();  // stack of indices
for (int i = 0; i < n; i++) {
    while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
        result[stack.pop()] = nums[i];  // i is the next greater for popped index
    }
    stack.push(i);
}
```

**Signal:** "next greater/smaller element", "span/area problems", "temperatures".
**Key insight:** when a new element breaks the monotone order, it IS the answer for all popped elements.

**Largest Rectangle in Histogram:**
Maintain increasing stack. When decreasing, pop and compute area = height × (i - stack.peek() - 1).

</details>

---

### ✅ Monotonic Deque
**File:** `com/java/ds/stack/MonotonicDeque.java`

- [x] Sliding Window Maximum (LC 239)
- [x] Shortest Subarray with Sum at Least K (LC 862)

<details>
<summary>📖 Guided Coding Helper</summary>

**Sliding Window Maximum (decreasing deque):**
```java
Deque<Integer> dq = new ArrayDeque<>();  // indices, decreasing values
for (int i = 0; i < n; i++) {
    while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) dq.pollLast(); // remove smaller
    dq.offerLast(i);
    if (dq.peekFirst() <= i - k) dq.pollFirst();   // remove out-of-window
    if (i >= k - 1) result[i - k + 1] = nums[dq.peekFirst()];
}
```

**Deque vs Stack:** deque can efficiently remove from BOTH ends (front = oldest, back = newest).

</details>

---

### ✅ Heap Deep Dive
**File:** `com/java/ds/heap/HeapDeepDive.java`

- [x] K-way merge (LC 23 — Merge K Sorted Lists)
- [x] Median from Data Stream (LC 295 — two-heap trick)
- [x] Task Scheduler (LC 621)
- [x] Smallest Range Covering K Lists (LC 632)

<details>
<summary>📖 Guided Coding Helper</summary>

**Median from Data Stream (two-heap trick):**
```
maxHeap (left half) | minHeap (right half)
   max → median ← min
```
- `maxHeap.size() == minHeap.size()` → median = avg of both tops
- `maxHeap.size() == minHeap.size() + 1` → median = maxHeap.top()
- Balance after each insert: push to maxHeap, then if maxHeap.top() > minHeap.top(), move top to minHeap.

**K-way merge:** min-heap of (value, listIndex, nodeIndex). Pop minimum, push next element from same list.

</details>

---

### ✅ Problem Pattern Cheat Sheet
**File:** `com/java/ds/ProblemPatternCheatSheet.java`

- [x] Signal → technique map (what clues in the problem statement map to which algorithm)
- [x] 45-minute interview time-box guide
- [x] Complexity table (all major algorithms)
- [x] Common gotchas per pattern

<details>
<summary>📖 Key Signal → Technique Map</summary>

| Signal in Problem | Technique |
|---|---|
| Sorted array + pair/triplet sum | Two Pointers (opposite ends) |
| Subarray / substring with constraint | Sliding Window |
| Cycle / middle of linked list | Fast + Slow Pointers |
| Top K / K closest / K largest | Heap (PriorityQueue) |
| Prefix queries on strings | Trie |
| Range sum / range update queries | Segment Tree / Fenwick Tree |
| Shortest path, non-negative weights | Dijkstra |
| Shortest path, negative weights | Bellman-Ford |
| All-pairs shortest path | Floyd-Warshall |
| Minimum spanning tree | Kruskal's / Prim's |
| Connected components / cycle in undirected | Union-Find |
| Topological order / cycle in directed | Topological Sort (Kahn's / DFS) |
| "Optimal substructure" + overlapping subproblems | DP |
| Subset of n items (n≤20) | Bitmask DP |
| Interval merging / scheduling | Sort by start, greedy / DP |
| Next greater / smaller element | Monotonic Stack |
| Sliding window max/min | Monotonic Deque |
| Pattern matching in string | KMP / Z-algorithm |
| Duplicate substring / Rabin-Karp | Rolling Hash |
| Longest palindrome | Manacher's |
| Permutations / subsets / combinations | Backtracking |
| "Minimize maximum" / "maximize minimum" | Binary Search on Answer |

</details>

---

### ✅ Recursion Patterns
**File:** `com/java/ds/recursion/RecursionPatterns.java`

- [x] Subsets (power set)
- [x] Permutations (with/without duplicates)
- [x] Combinations (nCr)
- [x] Tree recursion (Fibonacci tree model)
- [x] Divide & conquer (merge sort, binary search)
- [x] Bridge to memoization (top-down DP)

<details>
<summary>📖 Guided Coding Helper</summary>

**Subsets Template:**
```java
void backtrack(int start, List<Integer> current) {
    result.add(new ArrayList<>(current));
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);
        backtrack(i + 1, current);
        current.remove(current.size() - 1);  // undo
    }
}
```

**Permutations vs Subsets:**
- Subsets: `i` starts from `start` (no reuse, no re-ordering)
- Permutations: use `visited[]` boolean, `i` starts from 0 each time

**Bridge to Memoization:**
When recursion recomputes same sub-problems → add `Map<State, Result>` memo.
State = the parameters that uniquely identify a subproblem.

</details>

---

### ✅ Spring Boot Internals
**File:** `com/java/spring/SpringInternals.java`

- [x] `@Transactional` — proxy-based AOP, self-invocation bypass gotcha
- [x] N+1 query problem and solutions (JOIN FETCH, @BatchSize, entity graph)
- [x] AOP — JoinPoint, Pointcut, Advice types (Before/After/Around)
- [x] `@Async` — requires proxy, void vs Future return
- [x] Bean lifecycle (BeanPostProcessor, @PostConstruct, @PreDestroy)
- [x] `@Cacheable` — key generation, condition, unless, eviction

<details>
<summary>📖 Guided Coding Helper</summary>

**@Transactional self-invocation problem:**
```java
@Service class OrderService {
    public void placeOrder() {
        this.processPayment();  // WRONG: bypasses proxy, @Transactional ignored!
    }
    @Transactional
    public void processPayment() { ... }
}
```
Fix: inject self as dependency, or use `ApplicationContext.getBean()`, or restructure.

**N+1 Problem:**
```java
// Bad: 1 query for orders, then N queries for each order's user
List<Order> orders = orderRepo.findAll();  // 1 query
orders.stream().map(o -> o.getUser().getName());  // N lazy queries
// Fix: @Query("SELECT o FROM Order o JOIN FETCH o.user")
```

**@Async requirement:** must be on a public method, called from outside the class (proxy). Use `Future<T>` or `CompletableFuture<T>` to get results.

</details>

---

## 🎉 Plan Complete!

All 62 files implemented. Full coverage of:
- **DSA**: Two-pointer, Swapping, Pivot, DP (5 flavors), Trie, Segment Tree, Fenwick Tree, LinkedList, Graph (7 files), String algorithms (4), Stack (2), Heap (2), Bits (2), Recursion, Sorting (2), Search
- **LLD**: LRU/LFU Cache, Rate Limiter, Parking Lot, Thread Pool, BookMyShow, Ride Sharing
- **Java 21**: Virtual Threads, Structured Concurrency, Records, Sealed Classes, Pattern Matching, Scoped Values, String Templates, Foreign Memory API
- **Concurrency**: CompletableFuture, StampedLock, Parallel Streams, Reactive Streams
- **Design Patterns**: Creational (6), Structural (7), Behavioral (7), Modern Architecture (5)
- **JVM**: GC, Memory Model, Class Loading
- **Generics & Functional**: Foundations, Wildcards/PECS, Composition, Streams, Monads
- **Spring Boot**: Transactional, AOP, N+1, @Async, Bean lifecycle, @Cacheable
