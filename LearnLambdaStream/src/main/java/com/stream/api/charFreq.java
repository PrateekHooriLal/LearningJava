package com.stream.api;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CONCEPT: String → Stream<Character> → Frequency Map
 * ============================================================
 * PROBLEM: Count frequency of each character in a string using Streams.
 *
 * KEY LEARNING: How to convert a String into a stream of characters.
 *
 * THREE WAYS to stream characters from a String:
 *   1. str.chars()           → IntStream (code points as int)
 *   2. str.chars().mapToObj(c -> (char) c) → Stream<Character>  ← most common
 *   3. Stream.of(str.split("")) → Stream<String> (each element is 1-char string)
 *
 * INTERVIEW ANGLE:
 *   Interviewers often ask: "How do you process each character of a string with Streams?"
 *   Answer: str.chars().mapToObj(c -> (char) c) — explain the IntStream → Character conversion.
 *
 * THREE WAYS to print a Map (all shown below):
 *   1. forEach with lambda       → full control over format
 *   2. entrySet().forEach()      → method reference style
 *   3. forEach(System.out::println) → most concise
 * ============================================================
 */
public class charFreq {

	public static void main(String[] args) {
		String str = "adbvvvvvbbb";
		// Expected output: {a=1, d=1, b=4, v=5}

		// --------------------------------------------------------
		// PIPELINE: str → IntStream → Stream<Character> → Map
		// --------------------------------------------------------
		// str.chars()
		//   Returns IntStream — each int is the Unicode code point of the character
		//   e.g., 'a' → 97, 'b' → 98
		//   WHY IntStream and not Stream<Character>? Java avoids autoboxing overhead
		//   by using primitive streams (IntStream, LongStream, DoubleStream)
		//
		// .mapToObj(c -> (char) c)
		//   Converts int code point back to Character
		//   (char) c  = cast int to primitive char
		//   autoboxing = wraps char into Character object for Stream<Character>
		//
		// .collect(groupingBy(Function.identity(), counting()))
		//   groupingBy(Function.identity()) → groups by character value (key = char)
		//   counting()                      → counts elements in each group → Long value
		//
		// Result type: Map<Character, Long>
		//   Note: counting() always produces Long (not Integer)
		//   Use summingInt(c -> 1) if you need Map<Character, Integer>
		Map<Character, Long> listFrequency = str.chars()
				.mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// --------------------------------------------------------
		// THREE WAYS TO PRINT A MAP — know all three for interviews
		// --------------------------------------------------------

		// Way 1: size() — number of unique characters (not total chars)
		// Time: O(1) — HashMap stores size as a field
		System.out.println("Unique character count: " + listFrequency.size());

		// Way 2: forEach with lambda — most readable, best for custom formatting
		// forEach takes BiConsumer<K, V> — gives you key and value separately
		System.out.println("--- Way 1: forEach with lambda ---");
		listFrequency.forEach((character, frequency) ->
				System.out.println(character + " : " + frequency));

		// Way 3: entrySet().forEach() — iterate Map.Entry objects
		// Each Map.Entry holds getKey() and getValue()
		// System.out::print is a method reference = e -> System.out.print(e)
		System.out.println("--- Way 2: entrySet with method reference ---");
		listFrequency.entrySet().forEach(System.out::print);
		System.out.println();

		// Way 4 (commented — same as forEach with lambda but shorter):
		// listFrequency.forEach(System.out::println);
		// Note: Map's forEach passes (key, value) as two args to BiConsumer
		// System.out::println only accepts one arg → this WON'T compile for Map
		// You'd need: listFrequency.entrySet().forEach(System.out::println);
	}
}
