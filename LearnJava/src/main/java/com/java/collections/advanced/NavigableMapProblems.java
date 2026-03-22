package com.java.collections.advanced;

import java.util.*;

/**
 * ============================================================
 * CONCEPT: NavigableMap (TreeMap) — Solving LeetCode Problems with Ordered Keys
 * ============================================================
 *
 * WHY TREEMAP FOR THESE PROBLEMS?
 *   TreeMap implements NavigableMap which provides O(log n) "neighbor key" queries:
 *   floorEntry(k)  : entry with largest key ≤ k
 *   ceilingEntry(k): entry with smallest key ≥ k
 *   lowerEntry(k)  : entry with largest key < k (strictly)
 *   higherEntry(k) : entry with smallest key > k (strictly)
 *
 *   These operations enable efficient overlap detection, range queries, and
 *   time-series lookups that would otherwise require binary search on sorted arrays
 *   (which is harder to maintain dynamically as elements are added/removed).
 *
 * WHEN TREEMAP > HashMap:
 *   "Given a new event [s, e], does it overlap with any existing event?"
 *   → HashMap: must check all existing events. O(n).
 *   → TreeMap: floorEntry(s) gives the last event starting ≤ s. Check just that one. O(log n).
 *
 * WHEN TREEMAP > Sorted Array:
 *   Array: binary search gives O(log n) lookup but O(n) insert (shifting).
 *   TreeMap: O(log n) both lookup AND insert (red-black tree rebalancing).
 *
 * TIME / SPACE:
 *   All TreeMap operations: O(log n) time, O(n) space
 *   LC 729 My Calendar I:    O(log n) per booking
 *   LC 732 My Calendar III:  O(log n) per booking
 *   LC 1146 Snapshot Array:  O(log s) per query where s = number of snapshots
 *
 * INTERVIEW ANGLE:
 *   "Why not use a sorted array with binary search?" → array inserts are O(n)
 *   "What's the time complexity of each booking in My Calendar I?" → O(log n)
 *   "How does the difference array approach work for counting concurrent bookings?"
 *
 * REAL-WORLD:
 *   - Calendar/scheduling systems: room booking overlap detection
 *   - Database MVCC (Multi-Version Concurrency Control): TreeMap<txId, rowVersion>
 *   - Time-series data: efficient range queries on timestamps
 *   - Stock order books: sorted by price, O(log n) best bid/ask queries
 * ============================================================
 */
public class NavigableMapProblems {

    // =========================================================
    // PROBLEM 1: LC 729 — My Calendar I
    // =========================================================

    /**
     * KEY INSIGHT: Store each booking as TreeMap<startTime, endTime>.
     * For a new booking [start, end):
     *   1. Find floorEntry(start) = the last booking that starts at or before 'start'.
     *      If it exists and its end > start → OVERLAP (it extends into our booking).
     *   2. Find ceilingEntry(start) = the first booking that starts at or after 'start'.
     *      If it exists and its start < end → OVERLAP (our booking extends into it).
     *   If neither overlaps → book it (put(start, end)).
     *
     * OVERLAP CONDITION for intervals [a,b) and [c,d):
     *   They overlap if and only if a < d AND c < b.
     *   Equivalently: they DON'T overlap if b ≤ c OR d ≤ a.
     *
     * WHY ONLY CHECK floorEntry AND ceilingEntry?
     *   Any booking that starts strictly between (floorEntry.start, ceilingEntry.start)
     *   would be between these two by definition — but all those slots are unbooked
     *   (otherwise floorEntry/ceilingEntry would be closer). So only neighbors can overlap.
     *
     * COMPLEXITY: O(log n) per booking, O(n) space for n bookings.
     */
    static class MyCalendarI {
        private final TreeMap<Integer, Integer> bookings = new TreeMap<>();

        boolean book(int start, int end) {
            // Check overlap with the booking that starts just before or at 'start'
            Map.Entry<Integer, Integer> prevEntry = bookings.floorEntry(start);
            if (prevEntry != null && prevEntry.getValue() > start) {
                return false; // previous booking's end > our start → overlap
            }

            // Check overlap with the booking that starts just after 'start'
            Map.Entry<Integer, Integer> nextEntry = bookings.ceilingEntry(start);
            if (nextEntry != null && nextEntry.getKey() < end) {
                return false; // next booking's start < our end → overlap
            }

            bookings.put(start, end);
            return true;
        }
    }

    // =========================================================
    // PROBLEM 2: LC 732 — My Calendar III (Max Concurrent Bookings)
    // =========================================================

    /**
     * KEY INSIGHT: DIFFERENCE ARRAY via TreeMap.
     * Classic technique: instead of tracking full intervals, track EVENTS (changes in booking count).
     * At start time: count increases by 1. At end time: count decreases by 1.
     *
     * TreeMap<time, delta>:
     *   book(s, e) → map.merge(s, +1, Integer::sum); map.merge(e, -1, Integer::sum)
     *
     * To find max concurrent bookings: sweep through the map in sorted order.
     *   Keep a running sum. The maximum running sum = maximum concurrent bookings.
     *
     * EXAMPLE:
     *   book(10, 20): map = {10:+1, 20:-1}. Max = 1.
     *   book(15, 25): map = {10:+1, 15:+1, 20:-1, 25:-1}. Sweep: 0→1→2→1→0. Max = 2.
     *   book(20, 30): map = {10:+1, 15:+1, 20:0, 25:-1, 30:-1}. Sweep: 0→1→2→2→1→0. Max = 2.
     *   Note: 20:(-1+1)=0 means the -1 and +1 cancel at time 20.
     *
     * WHY TREEMAP?
     *   We need to iterate in sorted key order (time order) to compute prefix sums correctly.
     *   HashMap would require sorting before sweep — O(n log n) per query.
     *   TreeMap gives O(n) sweep (keys already sorted) after O(log n) update.
     *
     * COMPLEXITY: O(n log n) per booking (O(log n) update + O(n) sweep = O(n) for n bookings → O(n^2) total)
     * This is acceptable for "My Calendar III" where n ≤ 400.
     * For larger n, use a Segment Tree with lazy propagation for O(log n) per query.
     */
    static class MyCalendarIII {
        private final TreeMap<Integer, Integer> delta = new TreeMap<>();

        int book(int start, int end) {
            // Difference array: +1 at start, -1 at end
            delta.merge(start, 1,  Integer::sum);
            delta.merge(end,   -1, Integer::sum);

            // Sweep to find maximum prefix sum (= max concurrent bookings)
            int maxConcurrent = 0, running = 0;
            for (int d : delta.values()) {
                running += d;
                maxConcurrent = Math.max(maxConcurrent, running);
            }
            return maxConcurrent;
        }
    }

    // =========================================================
    // PROBLEM 3: LC 846 / 1296 — Hand of Straights / Divide Array in Sets of K
    // =========================================================

    /**
     * KEY INSIGHT: Greedy + TreeMap frequency count.
     * To form groups of k consecutive cards, always start from the SMALLEST available card.
     * If the smallest card can form a complete group (it + next k-1 consecutive cards all exist),
     * do so and continue. If not, return false immediately (greedy is optimal here because
     * any unused smaller card can only start groups, never complete groups started by larger cards).
     *
     * ALGORITHM:
     *   1. Count frequencies: TreeMap<cardValue, count> (or HashMap, but TreeMap gives firstKey() directly)
     *   2. While map is not empty:
     *      a. Take the smallest card: firstKey = map.firstKey()
     *      b. Try to use firstKey, firstKey+1, ..., firstKey+k-1 to form one group
     *      c. For each consecutive value: if not present → return false (can't complete group)
     *         If present: decrement count. If count reaches 0, remove from map.
     *
     * WHY TREEMAP?
     *   TreeMap.firstKey() gives the smallest remaining card in O(log n) — perfect for greedy.
     *   HashMap would require a manual min() scan each iteration: O(n) per iteration = O(n^2) total.
     *   TreeMap approach: O(n log n) total (each card processed once, each TreeMap op is O(log n)).
     *
     * IMPORTANT GOTCHA: Hand size must be divisible by k for any valid grouping to exist.
     *   Check hand.length % k == 0 first. If not, return false immediately.
     *
     * COMPLEXITY: O(n log n) time, O(n) space.
     */
    static boolean isNStraightHand(int[] hand, int groupSize) {
        if (hand.length % groupSize != 0) return false; // quick check

        TreeMap<Integer, Integer> freq = new TreeMap<>();
        for (int card : hand) freq.merge(card, 1, Integer::sum);

        while (!freq.isEmpty()) {
            int smallest = freq.firstKey(); // O(log n)

            // Try to form a group starting from 'smallest'
            for (int i = 0; i < groupSize; i++) {
                int card = smallest + i;
                if (!freq.containsKey(card)) return false; // can't complete group

                int remaining = freq.get(card) - 1;
                if (remaining == 0) {
                    freq.remove(card); // all cards of this value used up
                } else {
                    freq.put(card, remaining);
                }
            }
        }
        return true;
    }

    // =========================================================
    // PROBLEM 4: LC 480 — Sliding Window Median
    // =========================================================

    /**
     * KEY INSIGHT: Sliding window median = Two TreeMaps simulating a max-heap and min-heap,
     * but supporting O(log n) REMOVAL (which regular PriorityQueue cannot do efficiently).
     *
     * PROBLEM WITH REGULAR HEAPS:
     *   PriorityQueue supports O(log n) add but O(n) remove by value.
     *   For sliding window, we must efficiently remove the element that falls off the left side.
     *   O(n) removal → O(n^2) total → too slow for n = 100,000.
     *
     * TREEMAP AS ORDERED MULTISET:
     *   TreeMap<value, count> simulates a sorted multiset with duplicates.
     *   Operations: add(x) → map.merge(x, 1, Integer::sum) → O(log n)
     *               remove(x) → map.merge(x, -1, Integer::sum); if count=0, remove entry → O(log n)
     *   max() → map.lastKey() → O(log n)
     *   min() → map.firstKey() → O(log n)
     *
     * TWO TREEMAP APPROACH (mirror of two-heap median trick):
     *   lo = lower half (TreeMap as max-heap simulation: max = lastKey)
     *   hi = upper half (TreeMap as min-heap simulation: min = firstKey)
     *   Invariants: lo.size() == hi.size() OR lo.size() == hi.size() + 1
     *   All values in lo ≤ all values in hi
     *
     * BALANCE PROCEDURE: same as two-heap trick, but using TreeMap operations.
     *
     * COMPLEXITY: O(n log k) time, O(k) space.
     *   Each add/remove/rebalance = O(log k) (k = window size).
     *
     * NOTE: This is a hard problem. The clean version uses the "lazy deletion" trick with a
     * single delayed map, but the two-TreeMap version is more conceptually aligned with two-heaps.
     */
    static double[] slidingWindowMedian(int[] nums, int k) {
        // lo: lower half — TreeMap<value, count>, max = lo.lastKey()
        // hi: upper half — TreeMap<value, count>, min = hi.firstKey()
        TreeMap<Integer, Integer> lo = new TreeMap<>();
        TreeMap<Integer, Integer> hi = new TreeMap<>();

        // Track sizes separately (TreeMap size = distinct values, not total count with duplicates)
        int[] sizes = {0, 0}; // sizes[0]=lo count, sizes[1]=hi count

        double[] result = new double[nums.length - k + 1];

        for (int i = 0; i < nums.length; i++) {
            // Add new element
            addToMap(nums[i], lo, hi, sizes);

            // Remove element that falls out of window (when window is full and sliding)
            if (i >= k) {
                removeFromMap(nums[i - k], lo, hi, sizes);
            }

            // Record median once window is fully formed
            if (i >= k - 1) {
                result[i - k + 1] = getMedian(lo, hi, sizes);
            }
        }
        return result;
    }

    private static void addToMap(int num, TreeMap<Integer, Integer> lo,
                                  TreeMap<Integer, Integer> hi, int[] sizes) {
        // Add to lo first (lower half)
        lo.merge(num, 1, Integer::sum);
        sizes[0]++;

        // Ensure lo.max <= hi.min (cross-border check)
        if (!hi.isEmpty() && lo.lastKey() > hi.firstKey()) {
            // lo's max is too large → move it to hi
            int toMove = lo.lastKey();
            removeFromMapEntry(toMove, lo, sizes, 0);
            addToMapEntry(toMove, hi, sizes, 1);
        }

        // Balance sizes: lo should be >= hi in size
        if (sizes[1] > sizes[0]) {
            int toMove = hi.firstKey();
            removeFromMapEntry(toMove, hi, sizes, 1);
            addToMapEntry(toMove, lo, sizes, 0);
        } else if (sizes[0] > sizes[1] + 1) {
            int toMove = lo.lastKey();
            removeFromMapEntry(toMove, lo, sizes, 0);
            addToMapEntry(toMove, hi, sizes, 1);
        }
    }

    private static void removeFromMap(int num, TreeMap<Integer, Integer> lo,
                                       TreeMap<Integer, Integer> hi, int[] sizes) {
        if (!lo.isEmpty() && lo.containsKey(num)) {
            removeFromMapEntry(num, lo, sizes, 0);
        } else {
            removeFromMapEntry(num, hi, sizes, 1);
        }

        // Re-balance after removal
        if (sizes[1] > sizes[0]) {
            int toMove = hi.firstKey();
            removeFromMapEntry(toMove, hi, sizes, 1);
            addToMapEntry(toMove, lo, sizes, 0);
        } else if (sizes[0] > sizes[1] + 1) {
            int toMove = lo.lastKey();
            removeFromMapEntry(toMove, lo, sizes, 0);
            addToMapEntry(toMove, hi, sizes, 1);
        }
    }

    private static void addToMapEntry(int num, TreeMap<Integer, Integer> map, int[] sizes, int idx) {
        map.merge(num, 1, Integer::sum);
        sizes[idx]++;
    }

    private static void removeFromMapEntry(int num, TreeMap<Integer, Integer> map, int[] sizes, int idx) {
        map.merge(num, -1, Integer::sum);
        if (map.get(num) == 0) map.remove(num);
        sizes[idx]--;
    }

    private static double getMedian(TreeMap<Integer, Integer> lo, TreeMap<Integer, Integer> hi, int[] sizes) {
        if (sizes[0] == sizes[1]) {
            return ((long) lo.lastKey() + (long) hi.firstKey()) / 2.0;
        }
        return lo.lastKey(); // lo has one more element
    }

    // =========================================================
    // PROBLEM 5: LC 1146 — Snapshot Array
    // =========================================================

    /**
     * KEY INSIGHT: Don't store the full array state for each snapshot (too much memory).
     * Instead, for each index, store only the CHANGES (diffs) as a TreeMap<snapId, value>.
     * To query index i at snapshot snap_id: find the latest snapshot ≤ snap_id where
     * index i was modified → TreeMap.floorEntry(snap_id).
     *
     * This is TEMPORAL INDEXING — the same pattern used in:
     *   - MVCC databases (store version history, query "what was the value at time T?")
     *   - Version control systems (sparse change tracking)
     *   - Event sourcing (reconstruct state at any past point in time)
     *
     * MEMORY: O(total writes) — only stores changes, not full snapshots.
     *   If n=10^5 indices and 10^5 snapshots but only 10^5 total set() calls,
     *   this uses O(10^5) space, not O(n * snapCount) = O(10^10).
     *
     * TIME: O(log S) per get() where S = number of snapshots (not number of indices).
     *
     * WHY TREEMAP (floorEntry)?
     *   We need "the latest snapshot ≤ snap_id" — this is exactly what floorEntry() provides.
     *   HashMap cannot do this efficiently (would need to scan all entries).
     */
    static class SnapshotArray {
        // Each index has a TreeMap<snapId, value> storing only the changed values
        private final TreeMap<Integer, Integer>[] history;
        private int snapId = 0;

        @SuppressWarnings("unchecked")
        SnapshotArray(int length) {
            history = new TreeMap[length];
            for (int i = 0; i < length; i++) {
                history[i] = new TreeMap<>();
                history[i].put(0, 0); // initial value is 0 at snapshot 0
            }
        }

        void set(int index, int val) {
            history[index].put(snapId, val); // store change at current snapshot
        }

        int snap() {
            return snapId++; // return current snapId, then increment
        }

        int get(int index, int snap_id) {
            // Find the latest snapshot at or before snap_id where index was set
            Map.Entry<Integer, Integer> entry = history[index].floorEntry(snap_id);
            return entry.getValue(); // floorEntry is never null (we seeded 0 at snap 0)
        }
    }

    // =========================================================
    // MAIN: Tests & Demos
    // =========================================================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   NavigableMap / TreeMap Problems            ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        // My Calendar I
        System.out.println("\n=== LC 729: My Calendar I ===");
        MyCalendarI cal1 = new MyCalendarI();
        System.out.println("book(10,20): " + cal1.book(10, 20)); // true
        System.out.println("book(15,25): " + cal1.book(15, 25)); // false (overlaps 10-20)
        System.out.println("book(20,30): " + cal1.book(20, 30)); // true (touches at 20, no overlap)
        System.out.println("book(5,15): "  + cal1.book(5, 15));  // false (overlaps 10-20)
        System.out.println("book(25,35): " + cal1.book(25, 35)); // false (overlaps 20-30)

        // My Calendar III
        System.out.println("\n=== LC 732: My Calendar III (Max Concurrent) ===");
        MyCalendarIII cal3 = new MyCalendarIII();
        System.out.println("book(10,20) max: " + cal3.book(10, 20)); // 1
        System.out.println("book(50,60) max: " + cal3.book(50, 60)); // 1
        System.out.println("book(10,40) max: " + cal3.book(10, 40)); // 2
        System.out.println("book(5,15) max: "  + cal3.book(5,  15)); // 3
        System.out.println("book(5,10) max: "  + cal3.book(5,  10)); // 3
        System.out.println("book(25,55) max: " + cal3.book(25, 55)); // 3

        // Hand of Straights
        System.out.println("\n=== LC 846: Hand of Straights ===");
        System.out.println("hand=[1,2,3,6,2,3,4,7,8], k=3: " +
            isNStraightHand(new int[]{1,2,3,6,2,3,4,7,8}, 3)); // true
        System.out.println("hand=[1,2,3,4,5], k=4: " +
            isNStraightHand(new int[]{1,2,3,4,5}, 4));          // false (5%4 != 0)

        // Sliding Window Median
        System.out.println("\n=== LC 480: Sliding Window Median ===");
        double[] medians = slidingWindowMedian(new int[]{1,3,-1,-3,5,3,6,7}, 3);
        System.out.println("Medians (k=3): " + Arrays.toString(medians));
        // Expected: [1.0,-1.0,-1.0,3.0,5.0,6.0]

        // Snapshot Array
        System.out.println("\n=== LC 1146: Snapshot Array ===");
        SnapshotArray sa = new SnapshotArray(3); // indices 0,1,2
        sa.set(0, 5);
        System.out.println("snap() = " + sa.snap()); // 0
        sa.set(0, 6);
        System.out.println("get(0, 0) = " + sa.get(0, 0)); // 5 (snapshot 0 had value 5)
        System.out.println("get(0, 1) = " + sa.get(0, 1)); // 6 (current, no snap taken, so floor of snap 1 = snap 0's last write = 6)
        sa.snap();                                           // snap 1
        System.out.println("snap() = 1");
        System.out.println("get(0, 1) = " + sa.get(0, 1)); // 6
        System.out.println("get(1, 0) = " + sa.get(1, 0)); // 0 (never set, default = 0)

        System.out.println("\n=== All NavigableMap tests complete ===");
    }
}
