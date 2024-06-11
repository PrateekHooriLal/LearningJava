package com.whiplash;

public class QuickSort {

	public static void quickSort(int[] array) {

		quickSort(array, 0, array.length - 1);
	}

	public static void main(String[] args) {
		int arr[] = { 1, 4, 33, 44, 54, 23, 45, -9, -7, -3, -99, 0, 9, 9, 93 };
		quickSort(arr);
		for (int i = 0; i < arr.length; i++)
			System.out.print(arr[i] + " ");
	}

	private static void quickSort(int[] array, int left, int right) {
		// TODO Auto-generated method stub

		// defining base condition to break recursion
		if (left >= right)
			return;

		// to hold the index of pivot element of the given array
		int pivot = array[(left + right) / 2];

		// now this pivot will act as partitioning point, and this function call
		// will sort both subarrays(left to the pivot and right to the pivot).
		int index = partition(array, left, right, pivot);

		// make recursive call for both left partition and right partition
		quickSort(array, left, index - 1);// recursive call for left subarray.
		quickSort(array, index, right);// recursive call for right subarray.
	}// method ends

	// in this method we will be sorting the subarray from start to end based on
	// comparison from pivot

	public static int partition(int[] in, int start, int end, int pivot) {

		while (start <= end) {

			while (in[start] < pivot) {
				start++;
			}

			while (in[end] > pivot) {
				end--;
			}
			if (start <= end) {
				int temp = in[start];
				in[start] = in[end];
				in[end] = temp;

				// after swapping, increasing the pointers.
				start++;
				end--;
			}
		} // while ends
		return start;

	}// method ends
}