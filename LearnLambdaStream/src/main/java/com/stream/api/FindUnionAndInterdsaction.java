package com.stream.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FindUnionAndInterdsaction {

	public static List<Integer> list1 = Arrays.asList(1, 2, 3, 4, 5);
	public static List<Integer> list2 = Arrays.asList(3, 4, 5, 6, 7);

	public static void main (String[]args)
	  {
		System.out.println ("Hello World");
		Stream<Integer> s1 = list1.stream();
		Stream<Integer> s2 = list2.stream();



		List < Integer > commonElement = s1.distinct().filter(e->list2.contains(e)).toList();
		
		System.out.println (commonElement.toString ());
		
		List <Integer> union = Stream.concat(list1.stream(), list2.stream()).distinct().toList();
		System.out.println (union.toString ());


	  }

}
