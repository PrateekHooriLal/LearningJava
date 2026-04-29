package com.interview.prep;

public class FirstLeastOccurringNumber {

	public static void main(String[] args) {
		int[] arr = { 4, 3, 1, 6, 4, 6, 7, 1, 9, 3, 2,7 };
		System.out.println("First least occurring number: " + findFirstLeastOccurringNumber(arr));
	}

	public static int findFirstLeastOccurringNumber(int[] arr) {
		int minOccurrence = Integer.MAX_VALUE;
		int minNum = Integer.MAX_VALUE;

		// Iterate through the array to find the count of each number
		for (int i = 0; i < arr.length; i++) {
			int count = 0;

			// Count occurrences of arr[i]
			for (int j = 0; j < arr.length; j++) {
				if (arr[j] == arr[i]) {
					count++;
				}
			}

			// Update the minimum occurrence and the corresponding number
			if (count < minOccurrence || (count == minOccurrence && indexOf(arr, arr[i]) < indexOf(arr, minNum))) {
				minOccurrence = count;
				minNum = arr[i];
	
			}
		}

		return minNum;
	}

	// Helper method to find the index of the first occurrence of a number in the
	// array
	private static int indexOf(int[] arr, int num) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == num) {
				return i;
			}
		}
		return -1; // This should never happen in this context
	}
}
