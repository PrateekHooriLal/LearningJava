package com.stream.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FindDupes {

	static List<String> names = Arrays.asList("John", "Jake", "Jake", "John", "John", "Amit");

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		String s = "A";
		System.out.println(s.codePointAt(0));
		Set<String> tempSet = new HashSet<>();
		
		List<String> duplicateWords = names.stream().filter(w ->!tempSet.add(w)).collect(Collectors.toList());

		List<String> uniqueWords = names.stream().distinct().collect(Collectors.toList());
		System.out.println(duplicateWords);
		System.out.println(uniqueWords);

	}

}
