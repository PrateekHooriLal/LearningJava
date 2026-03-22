package com.java.ds.sorting;

public class MergeSort_2 extends Sorting {

	public static void main(String[] args) {

		mergeSort(array, new int[array.length], 0, array.length - 1);

		for (int i = 0; i < array.length; i++)
			System.out.print(" " + array[i]);
	}

	public static void mergeSort(int[] arr, int[] temp, int leftStart, int rightEnd) {
		// TODO Auto-generated method stub
		if (leftStart >= rightEnd)
			return;
		int middle = (leftStart + rightEnd) / 2;

		mergeSort(arr, temp, leftStart, middle);
		mergeSort(arr, temp, middle + 1, rightEnd);

		// merging two halves
		merge(arr, temp, leftStart, rightEnd);

	}// merge sort ends.

	public static void merge(int[] arr, int[] temp, int leftStart, int rightEnd) {
		// TODO Auto-genera2ted method stub
		int leftListEnd = (leftStart + rightEnd) / 2;
		int rightListStart = leftListEnd + 1;

		int l_cursor = leftStart;
		int r_cursor = rightListStart;
		int temp_cursor = leftStart;
		int size = rightEnd - leftStart + 1;
		while (l_cursor <= leftListEnd && r_cursor <= rightEnd) {

			if (arr[l_cursor] <= arr[r_cursor]) {
				temp[temp_cursor] = arr[l_cursor];
				l_cursor++;
			}

			else {
				temp[temp_cursor] = arr[r_cursor];
				r_cursor++;
			}
			temp_cursor++;

		} // while ends

		System.arraycopy(arr, l_cursor, temp, temp_cursor, leftListEnd - l_cursor + 1);
		System.arraycopy(arr, r_cursor, temp, temp_cursor, rightEnd - r_cursor + 1);
		System.arraycopy(temp, leftStart, arr, leftStart, size);
		/*
		 * if (l_cursor <= leftListEnd) { while(l_cursor <=leftListEnd) {
		 * temp[temp_cursor] = arr[l_cursor]; temp_cursor++; l_cursor++;} }
		 * 
		 * if (r_cursor <= rightEnd) { while(r_cursor <= rightEnd) { temp[temp_cursor] =
		 * arr[r_cursor]; r_cursor++; temp_cursor++; } } for (int i = 0; i <
		 * temp.length; i++) arr[leftStart+i]=temp[i];
		 */
	}// method ends

}// class ends
