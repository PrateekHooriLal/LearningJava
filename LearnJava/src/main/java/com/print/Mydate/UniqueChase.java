package com.print.Mydate;

import java.util.HashMap;
import java.util.Map;

public class UniqueChase {

	public static void main(String args[])// public static void main(String
											// Args[])
	{
		UniqueChase obj = new UniqueChase();
		int[] arr1 = { 1, 2, 3, 3, 4, 4 };
		int[] arr2 = { 7, 8, 6 };
		System.out.println("hi");
		int x = obj.uniqueElementCount(arr1, arr2);
		System.out.println("\n Count of unique element:" + x);

	}// main ends

	public int uniqueElementCount(int arr[], int array[]) {

		int count = 0;
		int[] arr1 = arr;
		int[] arr2 = array; // key=[1, 2, 3, 4, 5]:value[0, 0, 1, 0, 0]
		int len1 = arr1.length;
		int len2 = arr2.length;
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
		System.out.print("Distinct Element Are:");
		for (Integer key : frequency.keySet()) {
			if (frequency.get(key) == 1) {
				System.out.print(key + ">>");
				count++;
			}
		}
		return count;

	}// function ends
}
