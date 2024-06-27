package com.stream.api;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class charFreq {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "adbvvvvvbbb";

		Stream<String> s = Stream.of(str);
		s.collect(collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))).toL
		System.out.println(
		
		);

	}

}
