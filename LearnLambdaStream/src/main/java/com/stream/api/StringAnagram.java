package com.stream.api;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringAnagram {

	public static void main(String[] args) {
		System.out.println(StringAnagram.isAnagram("army", "zyza"));
	}// main ends

	private static boolean isAnagram(String s1, String s2) {
		if (s1.length() != s2.length() || s1.isEmpty() || s2.isEmpty())
			return false;

		List<String> len = Stream.of(s1).filter(c -> isPresent(c, s2)).toList();
		//String len = s1.chars().map(c -> (char) c).filter(c -> !isPresent(c, s2)).collect((Supplier<R>) Collectors.toList());
		System.out.println(len);

		if (len.size() == 0)
			return true;
		else
			return false;

	}

	private static boolean isPresent(String c, String s2) {
		if (s2.indexOf(c) != -1)
			return true;
		else
			return false;

	}

}