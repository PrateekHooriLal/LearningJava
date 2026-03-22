package com.java.ds.sorting;

/**
 * Selection Sort using Array.
 * 
 * The algorithm works by selecting the smallest unsorted item and then swapping
 * it with the item in the next position to be filled. The selection sort works
 * as follows: you look through the entire array for the smallest element, once
 * you find it you swap it (the smallest element) with the first element of the
 * array. Then you look for the smallest element in the remaining array (an
 * array without the first element) and swap it with the second element. Then
 * you look for the smallest element in the remaining array (an array without
 * first and second elements) and swap it with the third element, and so on.
 * 
 * The worst-case runtime complexity is O(n2).
 * 
 * @author Prateek.
 */
public class SelectionSort extends Sorting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SelectionSort s = new SelectionSort();
		System.out.println("Array Sorted in Ascending order:");
		printArray(s.selectionSort(Sorting.array));

	}

	public int[] selectionSort(int[] ar) {
		int[] arr = ar;
		int min_indx = 0;
		int len = ar.length;
		for (int i = 0; i < len - 1; i++) {
			min_indx = i;// hold's position of the minimum value in the array.

			for (int j = i + 1; j < len; j++) {
				if (arr[j] < arr[min_indx]) {
					min_indx = j;
				}
			} // for jth loop ends

			// Swap the found minimum element with the first element or the
			// element at the ith Position.
			swap(i, min_indx, arr);
		} // for ends
		return arr;
	}
}
