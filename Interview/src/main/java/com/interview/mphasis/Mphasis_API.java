package com.interview.mphasis;

// get 4th non-repeated character form given string "java is good technology"
//output = s

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mphasis_API {

	public static void main(String[] args) {
		String str = "java is good technology";

		// clean string of spaces
		str = str.replace(" ", "");

		// create a linked hash map to maintain insertion order
		Map<Character, Long> freqMap = str.chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

		// Finding the 4th Non-Repeated Character from the character frequency map
		Character s = freqMap.entrySet().stream().filter(e -> e.getValue() == 1).toList().get(3).getKey();
		System.out.println(s);

	}

}
