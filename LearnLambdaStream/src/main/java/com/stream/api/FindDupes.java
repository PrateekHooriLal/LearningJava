package com.stream.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FindDupes {

	static List<String> names = Arrays.asList("John", "Jake", "Jake", "John", "John", "Amit", "Sumit","John");

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		//names.toString().codePoints().forEach(System.out::println);
		Set<String> tempSet = new HashSet<>();

		List<String> duplicateWords = names.stream().filter(word -> !tempSet.add(word)).distinct().collect(Collectors.toList());
		System.out.println("TempSet= "+tempSet);
		System.out.println("duplicateWords= "+duplicateWords);


		List<String> uniqueWords = names.stream().distinct().collect(Collectors.toList());
		System.out.println("uniqueWords= "+uniqueWords);

	}

}
