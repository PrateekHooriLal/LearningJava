package com.stream.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONCEPT: Character Frequency Count — HashMap vs Stream API
 * ============================================================
 * PROBLEM: Given a String, count how many times each character appears.
 *
 * EXAMPLE:
 *   Input : "PrateekisaJavaDeveloper"
 *   Output: {P=1, r=1, a=3, t=1, e=4, k=1, i=1, s=1, J=1, v=2, D=1, l=1, o=1, p=1}
 *
 * TWO APPROACHES SHOWN:
 *   1. HashMap (manual iteration)  — O(n) time, O(k) space  [k = unique chars]
 *   2. Stream + groupingBy         — O(n) time, O(k) space  [same, cleaner code]
 *
 * WHEN TO USE IN INTERVIEW:
 *   - Anagram detection, palindrome check, first non-repeating char
 *   - Any "how many times does X appear" question
 *
 * INTERVIEW FOLLOW-UPS:
 *   Q: What is the time complexity?
 *   A: O(n) — single pass through the string, HashMap ops are O(1) amortised
 *
 *   Q: What's the space complexity?
 *   A: O(k) where k = number of unique characters. At most O(26) for lowercase
 *      English letters, so effectively O(1) for fixed alphabet.
 *
 *   Q: How would you find the FIRST non-repeating character?
 *   A: Build this frequency map, then iterate the original string in order
 *      and return the first character with count == 1. O(n) time.
 *
 * REAL-WORLD USE: Used in compression algorithms (Huffman coding uses char freq),
 *                 plagiarism detection, text analytics.
 * ============================================================
 */
public class Frequency {

	// Static string with spaces removed — demonstrates replaceAll() before processing
	// replaceAll(" ", "") removes ALL spaces using regex; replace(" ", "") also works (literal)
	static String s = "Prateek is a Java Developer".replaceAll(" ", "");

	public static void main(String[] args) {
		System.out.println("Input string (spaces removed): " + s);
		System.out.println("Using HashMap = " + getCharFrequency(s));
		System.out.println("Using Streams = " + getCharFrequencyStream(s));
	}

	// ============================================================
	// APPROACH 1: HashMap — manual counting
	// ============================================================
	// HOW IT WORKS:
	//   Iterate each character. If not in map → put(char, 1).
	//   If already in map → increment existing count by 1.
	//
	// getOrDefault() is a cleaner alternative to containsKey check:
	//   localMap.put(i, localMap.getOrDefault(i, 0) + 1);
	//
	// Time:  O(n) — one pass through the string
	// Space: O(k) — k unique characters stored in map
	public static Map<Character, Integer> getCharFrequency(String s) {
		// Use LOCAL map, NOT a static/instance field
		// A static field accumulates across calls — classic bug in interview code
		Map<Character, Integer> localMap = new HashMap<>();

		for (char c : s.toCharArray()) {
			// Pattern 1: containsKey check (verbose but readable)
			if (!localMap.containsKey(c)) {
				localMap.put(c, 1);                      // first occurrence
			} else {
				localMap.put(c, localMap.get(c) + 1);   // increment count
			}

			// Pattern 2: getOrDefault (more concise — same result)
			// localMap.put(c, localMap.getOrDefault(c, 0) + 1);

			// Pattern 3: merge (most idiomatic Java 8+)
			// localMap.merge(c, 1, Integer::sum);
		}
		return localMap;
	}

	// ============================================================
	// APPROACH 2: Stream API — groupingBy + summingInt
	// ============================================================
	// PIPELINE BREAKDOWN (read left to right):
	//
	//   str.chars()
	//   └─ Returns an IntStream of char code points (int values, not char)
	//      e.g., "ab" → IntStream[97, 98]
	//
	//   .mapToObj(c -> (char) c)
	//   └─ Converts each int code point back to a Character object
	//      WHY: Stream<Character> is needed for groupingBy; IntStream can't be grouped directly
	//      Cast (char) c converts int → char; autoboxing then wraps char → Character
	//
	//   .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(c -> 1)))
	//   └─ groupingBy(Function.identity()) → groups by the character itself (key = char)
	//   └─ summingInt(c -> 1) → for each element in the group, adds 1 → total = count
	//      WHY summingInt NOT counting(): counting() returns Long; summingInt returns Integer
	//      to match the Map<Character, Integer> return type
	//
	// INTERVIEW: "What does Function.identity() do?"
	//   It returns the element itself: x -> x
	//   Used when the grouping key IS the element (not a derived field)
	//
	// Time:  O(n) — single stream pass
	// Space: O(k) — same as HashMap approach
	public static Map<Character, Integer> getCharFrequencyStream(String str) {
		return str.chars()
				.mapToObj(c -> (char) c)       // IntStream → Stream<Character>
				.collect(Collectors.groupingBy(
						Function.identity(),           // key   = the character itself
						Collectors.summingInt(c -> 1)  // value = count (sum of 1s)
				));
	}
}
