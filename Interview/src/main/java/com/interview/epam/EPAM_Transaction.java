package com.interview.epam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*1 :Detect Suspicious Transactions

Problem:
Identify customers who made more than 2 high-value transactions (amount > 1000) on the same day. */

/*
2) Third Most Frequent Element

Problem:
Given an array of integers arr[], find the element that has the third highest frequency.
If multiple elements have the same frequency, choose the largest numerical value among them.
If fewer than 3 distinct frequency levels exist, return -1.

Constraints:

Ignore negative numbers.

Input:  int arr[] = {4, 4, 1, 2, 2, 3, 3, 3, 5, 5, 5, 5};
*/

public class EPAM_Transaction {

//Test Data:
	public static void main(String [] args) {

		int arr[] = { 4, 4, 1, 2, 2, 3, 3, 3, 5, 5, 5, 5 };
		Arrays.sort(arr);
		Map <Integer, Integer>freq = new HashMap<Integer, Integer>();
		Integer ef = 0;
		Integer e = 0;

		for(Integer n : arr) {
			if (freq.containsKey(n)) {
				freq.put(n, (Integer)freq.get(n)+ 1);
				ef = Math.max(ef, freq.get(n));
			} else
				freq.put(n, 1);
		}
		System.out.println(freq.toString());

	}

}
