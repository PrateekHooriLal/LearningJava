package com.lambda.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/* list =  123456123
 * 
 *1: num which are not duplicate//456
 *2: num which are duplicate//123
 *3: whole array without duplicates//123456
 * 
 * */
public class FindDuplicates {

	static List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 1, 2, 3);

	public static void main(String[] args) {

		Map<Integer, Long> map = list.stream()
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(map);	

		List<Integer> dup = map.entrySet().stream().filter(entry -> entry.getValue() > 1).map(e -> e.getKey()).toList();
		System.out.println(dup);

		List<Integer> nDup = map.keySet().stream().filter(e -> map.get(e) == 1).toList();
		System.out.println(nDup);

	}

}
