package com.java.sorting;

/**
 * Advantages of Insertion Sort:
 * 
 * 1) It is very simple. 2) It is very efficient for small data sets. 3) It is*
 * stable i.e., it does not change the relative order of elements with equal
 * keys. 4) In-place; i.e., only requires a constant amount O(1) of additional
 * memory space.
 * 
 * Insertion sort iterates through the list by consuming one input element at
 * each repetition, and growing a sorted output list. On a repetition, insertion
 * sort removes one element from the input data, finds the location it belongs
 * within the sorted list, and inserts it there. It repeats until no input
 * elements remain.
 * 
 * - See more at: http://quiz.geeksforgeeks.org/insertion-sort/
 * 
 * @author Prateek
 *
 */
public class InsertionSort extends Sorting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		InsertionSort s = new InsertionSort();
		int[] am = s.insertionSort(Sorting.array);

		System.out.println("Array Sorted in Ascending order:");
		printArray(s.insertionSort(Sorting.array));
	}// main ends

	public int[] insertionSort(int arr[]) {
		// TODO Auto-generated constructor stub
		int len = arr.length;

		for (int i = 0; i < len; i++) {

			int key = arr[i];
			int j = i - 1;

			/*
			 * Move elements of arr[0..i-1], that are greater than key, to one position
			 * ahead of their current position
			 */
			while (j >= 0 && arr[j] > key) {
				arr[j + 1] = arr[j];
				j--;
			}
			arr[j + 1] = key;
		}
		return arr;
	}// method ends.

}// class ends.
