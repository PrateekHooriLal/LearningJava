package com.stream.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

public class CharFreqInListOfString {

	static List<String> names = Arrays.asList("Ra(m", "Krish%na", "R@aghu", "Ra-j", "Kal#yan", "Che+rry");

	static List<List<Integer>> listOfLists = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5),
			Arrays.asList(6, 7, 8, 9));

	public static void main(String[] args) {

		// Flat list using Streams
		List<Integer> flatList_S = listOfLists.stream().flatMap(Collection::stream).toList();
		System.out.println("Flat List using Stream\n" + flatList_S.toString() + "\n");

		// Flat list using PArallel Streams
		List<Integer> flatList = listOfLists.parallelStream().flatMap(Collection::stream).toList();
		System.out.println("Flat List using Parallel Stream\n" + flatList + "\n");

		System.out.println("********Given List of Strings to operate on Ignoring any special characters*******\n"
				+ names.toString() + "\n");
		Map<Character, Long> FrequencyMap = names.stream().flatMapToInt(String::codePoints)

				.filter(Character::isLetter)// this filters on out only the alphabets leaving special
											// characters,isLetter can be used as well
				.mapToObj(c -> (char) c)// converting IntStream → Stream<Character>
				.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

		System.out.println(
				"Unsorted FrequencyMap original Order of character Intact=>\n " + FrequencyMap.toString() + "\n");

		// Sort by value
		List<Entry<Character, Long>> Dsorted = FrequencyMap.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).toList();
		List<Entry<Character, Long>> Asorted = FrequencyMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.toList();
		System.out.println("=========Sort by value========");
		System.out.println("Ascending sorted FrequencyMap  =>" + Asorted.toString());
		System.out.println("Descending sorted FrequencyMap =>" + Dsorted.toString() + "\n");

		// Sort by Key
		System.out.println("=========Sort by Key========");
		FrequencyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.forEach(e -> System.out.print(e.toString() + " "));
		System.out.println();
		FrequencyMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByKey()))
				.forEach(e -> System.out.print(e.toString() + " "));

	}
}