package com.java.ds.array;

import java.util.*;

/**
 * MERGE INTERVALS — Classic Interval Problems
 *
 * CONCEPT:
 *   Interval problems involve ranges [start, end] that may overlap.
 *   The standard approach:
 *   1. SORT intervals by start time.
 *   2. SCAN and MERGE overlapping intervals greedily.
 *
 * TWO INTERVALS OVERLAP when: a.start <= b.end AND b.start <= a.end
 * (i.e., one starts before the other ends)
 *
 * AFTER SORTING by start: intervals that overlap will be ADJACENT.
 * So we just need to check the current interval against the last merged interval.
 *
 * PROBLEMS COVERED:
 *   1. Merge Intervals — LC 56
 *   2. Insert Interval — LC 57
 *   3. Meeting Rooms II (min meeting rooms needed) — LC 253
 *
 * INTERVIEW FREQUENCY: High. Interval problems appear in scheduling and calendar problems.
 *
 * COMMON FOLLOW-UP:
 *   1. "What is the time complexity?" → O(n log n) for sorting; O(n) for the merge scan.
 *   2. "What if intervals are already sorted?" → O(n).
 *   3. LC 253 "Meeting Rooms II — why min heap?" → Heap tracks end times of ongoing meetings.
 *      If new meeting starts after the earliest end time, reuse that room (pop old, push new end).
 */
public class MergeIntervals {

    public static void main(String[] args) {

        System.out.println("=== LC 56: Merge Intervals ===");
        printIntervals(merge(new int[][]{{1,3},{2,6},{8,10},{15,18}})); // [[1,6],[8,10],[15,18]]
        printIntervals(merge(new int[][]{{1,4},{4,5}}));                // [[1,5]]
        printIntervals(merge(new int[][]{{1,4},{2,3}}));                // [[1,4]] (inner interval)
        printIntervals(merge(new int[][]{{1,4}}));                      // [[1,4]] (single)

        System.out.println("\n=== LC 57: Insert Interval ===");
        printIntervals(insert(new int[][]{{1,3},{6,9}}, new int[]{2,5}));     // [[1,5],[6,9]]
        printIntervals(insert(new int[][]{{1,2},{3,5},{6,7},{8,10},{12,16}}, new int[]{4,8})); // [[1,2],[3,10],[12,16]]
        printIntervals(insert(new int[][]{}, new int[]{5,7}));                // [[5,7]]
        printIntervals(insert(new int[][]{{1,5}}, new int[]{2,3}));           // [[1,5]]

        System.out.println("\n=== LC 253: Meeting Rooms II (Min Rooms) ===");
        System.out.println(minMeetingRooms(new int[][]{{0,30},{5,10},{15,20}})); // Expected: 2
        System.out.println(minMeetingRooms(new int[][]{{7,10},{2,4}}));          // Expected: 1
        System.out.println(minMeetingRooms(new int[][]{{1,5},{2,3},{4,6}}));     // Expected: 2
        System.out.println(minMeetingRooms(new int[][]{}));                      // Expected: 0
    }

    // =========================================================================
    // LC 56 — Merge Intervals
    // =========================================================================

    /**
     * Merges all overlapping intervals.
     *
     * ALGORITHM:
     *   1. Sort by start time — brings overlapping intervals together.
     *   2. Initialize result with first interval.
     *   3. For each next interval:
     *      - If it overlaps with the last result interval → merge (extend end).
     *      - Else → add as new interval (no overlap, gap between them).
     *
     * OVERLAP CONDITION (after sorting): next.start <= last.end
     *   (if next starts before last ends, they overlap)
     *
     * MERGE: new end = max(last.end, next.end)
     *   (next might be fully inside last, so we take the max)
     *
     * Time: O(n log n)  Space: O(n) for output
     */
    public static int[][] merge(int[][] intervals) {
        if (intervals.length <= 1) return intervals;

        // Sort by start time — brings potentially overlapping intervals adjacent
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        List<int[]> merged = new ArrayList<>();
        merged.add(intervals[0]); // Start with the first interval

        for (int i = 1; i < intervals.length; i++) {
            int[] current = intervals[i];
            int[] last = merged.get(merged.size() - 1); // Last interval in result

            if (current[0] <= last[1]) {
                // OVERLAP: current starts before last ends → merge by extending end
                last[1] = Math.max(last[1], current[1]); // Use max because current might be inside last
            } else {
                // NO OVERLAP: gap between intervals → add current as a new entry
                merged.add(current);
            }
        }

        return merged.toArray(new int[0][]);
    }

    // =========================================================================
    // LC 57 — Insert Interval
    // =========================================================================

    /**
     * Inserts a new interval into a list of non-overlapping, sorted intervals.
     * Merges any overlapping intervals.
     *
     * ALGORITHM (3 phases):
     *   Phase 1: Add all intervals that END before newInterval starts (no overlap, come before).
     *   Phase 2: Merge all intervals that OVERLAP with newInterval (collapse into one).
     *   Phase 3: Add all remaining intervals (they start after newInterval ends).
     *
     * OVERLAP CHECK: interval.end >= newInterval.start AND interval.start <= newInterval.end
     *
     * Time: O(n)  Space: O(n)
     */
    public static int[][] insert(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        int i = 0;
        int n = intervals.length;

        // Phase 1: Skip intervals that end before newInterval starts (no overlap)
        while (i < n && intervals[i][1] < newInterval[0]) {
            result.add(intervals[i]); // These intervals come entirely before the new one
            i++;
        }

        // Phase 2: Merge all overlapping intervals with newInterval
        while (i < n && intervals[i][0] <= newInterval[1]) {
            // Current interval overlaps with newInterval → expand newInterval to cover both
            newInterval[0] = Math.min(newInterval[0], intervals[i][0]); // Expand left
            newInterval[1] = Math.max(newInterval[1], intervals[i][1]); // Expand right
            i++;
        }
        result.add(newInterval); // Add the merged (possibly expanded) new interval

        // Phase 3: Add all remaining intervals (they start after newInterval ends)
        while (i < n) {
            result.add(intervals[i]); // These come entirely after the merged interval
            i++;
        }

        return result.toArray(new int[0][]);
    }

    // =========================================================================
    // LC 253 — Meeting Rooms II
    // =========================================================================

    /**
     * Returns the minimum number of conference rooms required.
     *
     * KEY INSIGHT: A room can be reused when a meeting ends before the next starts.
     *   → We need to find the maximum number of meetings that overlap at any point.
     *
     * MIN HEAP APPROACH:
     *   Sort meetings by START time.
     *   Min heap stores END times of currently ongoing meetings.
     *   For each meeting:
     *     - If the earliest ending meeting ends before this one starts → reuse that room.
     *       (Remove from heap — that room is now free — add new end time.)
     *     - Else → need a new room. Add end time to heap.
     *   Answer = heap size at the end (number of rooms in use at peak).
     *
     * ALTERNATIVE: Chronological ordering / Two sorted arrays approach — also O(n log n).
     *
     * Time: O(n log n)  Space: O(n) for heap
     */
    public static int minMeetingRooms(int[][] intervals) {
        if (intervals.length == 0) return 0;

        // Sort by start time
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        // Min heap of end times — smallest end time at the top
        PriorityQueue<Integer> endTimes = new PriorityQueue<>();

        for (int[] meeting : intervals) {
            int start = meeting[0];
            int end   = meeting[1];

            if (!endTimes.isEmpty() && endTimes.peek() <= start) {
                // Earliest-ending meeting finishes at or before this one starts
                // → Reuse that room: remove its end time, will add this meeting's end time
                endTimes.poll();
            }

            // Allocate room for this meeting (either reused or new)
            endTimes.offer(end);
        }

        // Number of rooms currently in use = heap size (each entry = one occupied room)
        return endTimes.size();
    }

    // Helper to print intervals nicely
    static void printIntervals(int[][] intervals) {
        List<String> parts = new ArrayList<>();
        for (int[] interval : intervals) {
            parts.add("[" + interval[0] + "," + interval[1] + "]");
        }
        System.out.println("  " + parts);
    }
}
