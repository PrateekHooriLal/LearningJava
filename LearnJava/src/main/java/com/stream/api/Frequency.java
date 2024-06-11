package com.stream.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Frequency {
	public static Map<Character, Integer> freqMap = new HashMap<Character, Integer>();

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String s = "Prateek is a Java Developer".replaceAll(" ", "");
		char[] array = s.toCharArray();

		Stream ins = Stream.of(s);
		Stream inse = Stream.of(array);
		IntStream sd = s.chars();
		String[] df = s.split(s);

		System.out.println(getCharFrequency(s));
		System.out.println(getCharFrequencyStream(s));
	}

	// Find frequency of characters in String using hashmap .
	public static Map<Character, Integer> getCharFrequency(String s) {

		for (char i : s.toCharArray()) {

			if (!freqMap.containsKey(i)) {
				freqMap.put(i, 1);
			} else {
				freqMap.put(i, freqMap.get(i) + 1);
			}
		}
		return !freqMap.isEmpty() ? freqMap : null;
	}

	// Find frequency of characters in String using Stream Java 8 .
	public static Map<Character, Integer> getCharFrequencyStream(String str) {

		Map<Character, Integer> frequency = str.chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(c -> 1)));
		return frequency;
	}

}
