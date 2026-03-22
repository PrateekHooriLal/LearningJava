package com.java.ds.sorting;

/**
 * @author Prateek
 *
 */
public class MergeSort extends Sorting {

	/**
	 * @param args
	 */
	int a1[] = new int[array.length];

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Array Sorted in Ascending order:");
		MergeSort m = new MergeSort();
		int a[] = m.mergeSort(array, new int[array.length], 0, array.length - 1);

		printArray(a);
	}// main ends

	/**
	 * @param Integer array
	 * @return
	 * @return Sorted array
	 */
	public int[] mergeSort(int arr[], int[] temp, int leftStart, int rightEnd) {

		/*
		 * int ret []= new int[1]; ret[0] = arr[0];
		 */
		if (leftStart >= rightEnd)
			return arr;

		int middle = (leftStart + rightEnd) / 2;
		mergeSort(arr, temp, leftStart, middle);
		mergeSort(arr, temp, middle + 1, rightEnd);

		// now merging the two halves

		return mergeHalves(arr, temp, leftStart, rightEnd);

	}// mergeSort method ends.

	private int[] mergeHalves(int[] arr, int[] temp, int leftStart, int rightEnd) {
		// TODO Auto-generated method stub
		int leftListEnd = (leftStart + rightEnd) / 2;
		int rightListStart = leftListEnd + 1;
		int totalItem = rightEnd - leftStart + 1;

		int leftListCursor = leftStart;
		int rightListCursor = rightListStart;
		int tempCursor = leftStart;// as leftStart is already passed as Zero
									// through out.

		// walk through the two halves copying over the elements.
		while (leftListCursor <= leftListEnd && rightListCursor <= rightEnd) {

			if (arr[leftListCursor] <= arr[rightListCursor]) {
				temp[tempCursor] = arr[leftListCursor++];
			} else { // copy over the element from right sublist
				temp[tempCursor] = arr[rightListCursor++];
			}
			tempCursor++;

		} // while loop ends
		if (rightListCursor <= rightEnd)
			System.arraycopy(arr, rightListCursor, temp, tempCursor, rightEnd - rightListCursor + 1);
		else if (leftListCursor <= leftListEnd)
			System.arraycopy(array, leftListCursor, temp, tempCursor, leftListEnd - leftListCursor + 1);

		System.arraycopy(temp, leftStart, arr, leftStart, totalItem);
		return temp;
	}// mergeHalves method ends.
}// class ends
