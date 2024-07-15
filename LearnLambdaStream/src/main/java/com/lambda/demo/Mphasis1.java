package com.lambda.demo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Mphasis1 {
	//select * from Employee order by (Salary) desc row num 2.
	// Need to print common element.
	// Need to merge to single array with distinct value in sorted

	/*
	 * static int[] arr1 = { 71, 21, 34, 89, 56, 28 }; static int[] arr2 = { 12, 56,
	 * 17, 21, 94, 34 };
	 * 
	 * static List list1 = Arrays.asList(arr1); static List list2 =
	 * Arrays.asList(arr2);
	 */

	public static void main(String[] args) {
		 String str= "Abcddefghhgab";


		//System.out.println(Stream.concat(list1.stream(), list2.stream()).distinct().toList().toString());
		
		Map map = str.chars().mapToObj(c-> (char) c).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(map.toString());
	}
}
