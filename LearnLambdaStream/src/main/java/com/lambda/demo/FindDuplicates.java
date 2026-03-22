package com.lambda.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONCEPT: Finding Duplicates and Non-Duplicates using Streams
 * ============================================================
 * INPUT:  [1, 2, 3, 4, 5, 6, 1, 2, 3]
 *
 * THREE QUESTIONS often asked in interviews:
 *   Q1: Which numbers appear MORE than once?  → [1, 2, 3]  (duplicates)
 *   Q2: Which numbers appear EXACTLY once?    → [4, 5, 6]  (non-duplicates)
 *   Q3: Unique numbers (remove all dupes)?    → [1,2,3,4,5,6] → use stream().distinct()
 *
 * STRATEGY: Build a frequency map ONCE, then filter it for each answer.
 *   One map → three answers. This is more efficient than three separate passes.
 *
 * TIME COMPLEXITY:  O(n) — one pass to build the map, O(k) for each filter [k = unique]
 * SPACE COMPLEXITY: O(k) — map stores only unique keys
 *
 * INTERVIEW FOLLOW-UPS:
 *   Q: Can you do this without a HashMap (O(1) space)?
 *   A: Only if the array is sorted — use two-pointer. Otherwise HashMap is needed.
 *
 *   Q: What if the list contains Strings instead of Integers?
 *   A: Same approach — groupingBy works on any type that has proper equals/hashCode.
 *
 *   Q: Why map.get(e) == 1 instead of map.get(e).equals(1)?
 *   A: For Long (from counting()), use .equals(). For Integer with values -128 to 127,
 *      == works due to Integer cache, but it's unsafe — always use .equals() for objects.
 *      (This code uses Long from counting() — see note below.)
 * ============================================================
 */
public class FindDuplicates {

	static List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 1, 2, 3);

	public static void main(String[] args) {

		// --------------------------------------------------------
		// STEP 1: Build a frequency map — key=number, value=count
		// --------------------------------------------------------
		// groupingBy(Function.identity()) → groups each number by itself
		//   Function.identity() = x -> x (element IS the key)
		// counting() → counts how many times each number appears → returns Long
		//
		// Result: {1=2, 2=2, 3=2, 4=1, 5=1, 6=1}
		// Time: O(n), Space: O(k)
		Map<Integer, Long> map = list.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println("Frequency map: " + map);

		// --------------------------------------------------------
		// STEP 2: Find DUPLICATES — elements with count > 1
		// --------------------------------------------------------
		// entrySet() returns Set<Map.Entry<K,V>> — each entry has a key and value
		// filter(entry -> entry.getValue() > 1) → keep only entries where count > 1
		// map(e -> e.getKey()) → extract just the number (throw away the count)
		// toList() → collect into an unmodifiable List (Java 16+)
		//
		// ALTERNATIVE: list.stream().filter(n -> Collections.frequency(list, n) > 1).distinct()
		//   But this is O(n²) — avoid it. The frequency map approach is O(n).
		List<Integer> dup = map.entrySet().stream()
				.filter(entry -> entry.getValue() > 1)  // count > 1 means duplicate
				.map(e -> e.getKey())                   // extract the number
				.toList();                              // Java 16+ immutable list
		System.out.println("Duplicates (appear > 1 time): " + dup);

		// --------------------------------------------------------
		// STEP 3: Find NON-DUPLICATES — elements with count == 1
		// --------------------------------------------------------
		// keySet() returns just the keys (numbers) — no need for entrySet here
		// because we look up the count separately via map.get(e)
		//
		// GOTCHA: map.get(e) == 1 compares Long with int literal
		//   Java auto-promotes 1 to Long for comparison but this is implicit.
		//   Safer and clearer: map.get(e).equals(1L)  (note: 1L not 1)
		List<Integer> nDup = map.keySet().stream()
				.filter(e -> map.get(e) == 1)  // count exactly 1 = unique element
				.toList();
		System.out.println("Non-duplicates (appear exactly once): " + nDup);

		// --------------------------------------------------------
		// BONUS: distinct() — remove all duplicates (keep first occurrence)
		// --------------------------------------------------------
		// stream().distinct() uses equals()/hashCode() internally (backed by LinkedHashSet)
		// Preserves encounter order for ordered streams (like List)
		// Time: O(n), Space: O(k)
		List<Integer> uniqueAll = list.stream().distinct().toList();
		System.out.println("All unique (distinct): " + uniqueAll);
	}
}
