package com.stream.api;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class charFreq {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "adbvvvvvbbb";

		Stream<String> s = Stream.of(str);
		Map<Character, Long> listFrequency = str.chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(listFrequency);
		listFrequency.forEach((character, frequency) -> System.out.println(character + ":" + frequency));
	}

}
