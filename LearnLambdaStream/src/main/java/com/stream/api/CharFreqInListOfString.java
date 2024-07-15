package com.stream.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CharFreqInListOfString {

	static List<String> list = Arrays.asList("Ram", "Krishna", "Raghu", "Raj", "Kalyan", "Cherry");

	public static void main(String[] args) {
	
		// TODO Auto-generated method stub
		Map<Character, Long> map = list.toString().replaceAll(" ", "").replaceAll(",", "").replaceAll("\\[", "")
				.replaceAll("\\]", "").chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(map);
	}
}