package com.interview.epam;

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
	public static void main() {

		int arr[] = { 4, 4, 1, 2, 2, 3, 3, 3, 5, 5, 5, 5 };

		Map freq = new HashMap<Integer, Integer>();

		for (int i = 0; i < arr.length; i++) {

			if (freq.get(arr[i]) != null) {
				freq.put(arr[i], (int) freq.get(arr[i]) + 1);
			} else
				freq.put(arr[i], 1);
		}
		System.out.println(freq.toString());

	}

}
