package com.whiplash;

/**
 * @author plal
 *
 */

/*
 * 
 * =======================================================Merge
 * Sort======================================================== in this algo we
 * are virtually dividing the array.and then merging it.
 * 
 */

public class Sort {

	public static void main(String[] args) {

		int arr[] = { 1, 4, 33, 44, 54, 23, 45, -9, -7, -3, -99, 0, 9, 9, 93 };

		mergeSort(arr, new int[arr.length], 0, arr.length - 1);

		for (int i = 0; i < arr.length; i++)
			System.out.print(arr[i] + " ");
	}

	private static void mergeSort(int[] arr, int[] temp, int leftStart, int rightEnd) {
		// TODO Auto-generated method stub
		if (leftStart >= rightEnd)
			return;
		int middle = (leftStart + rightEnd) / 2;

		mergeSort(arr, temp, leftStart, middle);
		mergeSort(arr, temp, middle + 1, rightEnd);

		// merging two halves
		merge(arr, temp, leftStart, rightEnd);

	}// merge sort ends.

	private static void merge(int[] arr, int[] temp, int leftStart, int rightEnd) {
		// TODO Auto-generated method stub
		int leftListEnd = (leftStart + rightEnd) / 2;// calculating the
		int rightListStart = leftListEnd + 1;

		int l_cursor = leftStart;
		int r_cursor = rightListStart;
		int temp_cursor = leftStart;// this is for the temporary array we are creating by sorting the two halves .
		while (l_cursor <= leftListEnd && r_cursor <= rightEnd) {

			if (arr[l_cursor] < arr[r_cursor])
				temp[temp_cursor] = arr[l_cursor++];

			else
				temp[temp_cursor] = arr[r_cursor++];

			temp_cursor++;// any ways we have to increment the temp cursor.

		} // while ends

		if (l_cursor >= leftListEnd && r_cursor <= rightEnd) {// or
			while (r_cursor <= rightEnd)
				temp[temp_cursor++] = arr[r_cursor++];
		}

		if (r_cursor >= rightEnd && l_cursor <= leftListEnd) {
			while (l_cursor <= leftListEnd)
				temp[temp_cursor++] = arr[l_cursor++];
		}

		System.arraycopy(temp, leftStart, arr, leftStart, rightEnd - leftStart + 1);// as we know leftStart and rightEnd
																					// is two extreme boundaries for the
																					// array .
	}// method ends

}// class ends
