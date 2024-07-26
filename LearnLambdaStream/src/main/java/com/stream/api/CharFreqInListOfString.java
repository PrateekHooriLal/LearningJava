package com.stream.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CharFreqInListOfString {

	static List<String> list = Arrays.asList("Ram", "Krishna", "Raghu", "Raj", "Kalyan", "Cherry");
	static List<List<Integer>> listOfLists = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5),
			Arrays.asList(6, 7, 8, 9));

	public static void main(String[] args) {

		List flatList = listOfLists.stream().flatMap(Collection::stream).collect(Collectors.toList());
		System.out.println(flatList);

		Map<Character, Long> map = list.toString().replaceAll(" ", "").replaceAll(",", "").replaceAll("\\[", "")
				.replaceAll("\\]", "").chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(map);
	}
}