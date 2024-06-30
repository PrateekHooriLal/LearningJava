package com.interview.prep;

import java.util.LinkedHashMap;
import java.util.Map;

//print the first least occurred number
public class Coforge {

	static int[] arr = { 1, 2, 1, 3, 4, 5, 3, 6, 6, 8, 2, 4, 4, 5, 5, 8, 1, 1, 2, 2, 3, 3, 6, 6 };

	public static void main(String[] args) {

		int leastOccurredNumber = Integer.MIN_VALUE;
		int leastOcNumFrequency = Integer.MIN_VALUE;

		
		int len = arr.length;
		Map<Integer, Integer> freqMap = new LinkedHashMap<Integer, Integer>();

		// Time complexity will be O(n*m), Hashing is much better resulting in O(n+m)
		for (int i = 0; i < len; i++) {
			freqMap.put(arr[i], freqMap.getOrDefault(arr[i], 0) + 1);
			// reset counter for counting next element.
			int count = 0;
			for (int j = 0; j < len; j++) {
				if (arr[i] == arr[j]) {
					count++;
				}
			}
			if (i == 0 || count < leastOcNumFrequency) {
				leastOccurredNumber = arr[i];
				leastOcNumFrequency = count;
			}
		}
		System.out.println(freqMap);
		System.out.println(leastOccurredNumber);
	}
}

/*
 * Map<Integer, Integer> map = new LinkedHashMap<Integer, Integer>(); for (int i
 * = 0; i < arr.length; i++) {
 * 
 * if (!map.containsKey(arr[i])) { map.put(arr[i], 1); } else { map.put(arr[i],
 * map.get(arr[i]) + 1); } } //System.out.println(map); for (int j = 1; j <
 * map.size(); j++) { if ((Integer) map.get(j) == 2) {
 * System.out.println((Integer) map.get(j)); break; }
 * 
 * }
 */