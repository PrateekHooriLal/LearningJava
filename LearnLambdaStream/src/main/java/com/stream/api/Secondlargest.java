package com.stream.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
/*
students.add(new Student(2, "Marie", 68));
students.add(new Student(2, "Marie", 68));
 
How you will remove duplicate student object from students list?*/
import java.util.stream.Stream;

public class Secondlargest {

	// find second largest value from the

	static List<Integer> list = Arrays.asList(12, 234, 45, 35, 456, 46, 3, 9, 55);

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// streams to find second largest
		Integer secondlargestNumber = list.stream().sorted(Comparator.reverseOrder()).skip(1).toList().get(0);
				
		System.out.println(secondlargestNumber);
		
		//list.stream().
	}

}
/*
 * Collections.sort(list); System.out.println(list); int indx = list.size() - 2;
 * System.out.println(list.get(indx));
 */