package com.stream.api;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Secondlargest {

	// find second largest value from the

	static List<Integer> list = Arrays.asList(12, 234, 45, 35, 456, 46, 3, 9, 55);

	public static void main(String[] args) {

		// HttpRequest
		// streams to find second largest
		int secondlargestNumber = list.stream().sorted(Comparator.reverseOrder()).toList().get(1);
		System.out.println(secondlargestNumber);

		// list.stream().
	}

}
/*
 * Collections.sort(list); System.out.println(list); int indx = list.size() - 2;
 * System.out.println(list.get(indx));
 */