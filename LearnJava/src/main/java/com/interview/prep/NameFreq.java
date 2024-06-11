package com.interview.prep;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*List<String> names = Arrays.asList("John", "Jake", "Jake", "John", "John", "Amit");
John -> 3
Jake -> 2
Amit -> 1*/
public class NameFreq {

	static List<String> names = Arrays.asList("John", "Jake", "Jake", "John", "John", "Amit");

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Map<String, Integer> count = new HashMap();

		// count.put(names.get(0), 0);
		for (String name : names) {

			if (!count.containsKey(name))
				count.put(name, 1);
			else
				count.put(name, count.get(name) + 1);

		}

		System.out.println(count.toString());
	}

}
