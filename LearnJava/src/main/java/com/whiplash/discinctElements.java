package com.whiplash;

import java.util.HashMap;
import java.util.Map;

public class discinctElements {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int[] arr1 = { 1, 2, 3, 3, 4, 1, 55, 100, 200, 5, 77, -1, -2 };
		// key=[1, 2, 3, 4,5]:value[0, 0, 1,0, 0]
		int[] arr2 = { 3, 4, 5, 1, 2, 55, 99, 43, 100, 90, 87, 87, 88, 9999, 5567 };
		int len1 = arr1.length;
		int len2 = arr2.length;
		int count = 0;
		int[] temp = new int[len1 + len2];
		Map<Integer, Integer> frequency = new HashMap<Integer, Integer>();

		for (int i = 0; i < len1 + len2; i++)// from this we will have one
												// combined array
		{
			if (i < len1)
				temp[i] = arr1[i];

			else if (i >= len1)
				temp[i] = arr2[(len1 + len2 - 1) - i];

			System.out.println(temp[i]);
		}

		for (int i = 0; i < temp.length; i++)// getting frequency of each
												// element.
		{
			int f = 0;
			for (int j = 0; j < temp.length; j++) {
				if (temp[i] == temp[j]) {
					f++;
				}
				frequency.put(temp[i], f);
			} // j loop ends
		} // i loop ends
		System.out.println("key=" + frequency.keySet() + ":value" + frequency.values());// printing whole set with key
																						// as element
																						// and value as it's frequency.
		System.out.print("Distinct Element Are");
		for (Integer key : frequency.keySet()) {
			if (frequency.get(key) == 1) {
				System.out.print(": " + key);
				count++;
			}
		}
		System.out.println("\n There Count=" + count);
	}// main ends
}
