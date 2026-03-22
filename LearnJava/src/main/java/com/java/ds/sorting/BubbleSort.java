package com.java.ds.sorting;

/**
 * Bubble Sort
 * 
 * The algorithm works by comparing each item in the list with the item next to
 * it, and swapping them if required. In other words, the largest element has
 * bubbled to the top of the array. The algorithm repeats this process until it
 * makes a pass all the way through the list without swapping any items.
 * 
 * The worst-case runtime complexity is O(n2).
 * 
 * @param list
 * @return
 * @author Prateek
 */
public class BubbleSort extends Sorting {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		BubbleSort b = new BubbleSort();
		int[] arr1 = b.bubbleSort(Sorting.array);

		System.out.println("Sorted in aascending order:");
		printArray(arr1);
	}// main ends

	public int[] bubbleSort(int[] arr) {
		// TODO Auto-generated method stub

		int length = arr.length;

		for (int pass = length - 1; pass >= 0; pass--) {
			// here "adjCompare" is generating values to compare adjacent element in the
			// array.
			for (int adjCompare = 1; adjCompare <= pass; adjCompare++) {// here we don't have to care about indexoutOf
																		// Bound exception.

				if (arr[adjCompare - 1] > arr[adjCompare]) {// for descending order use "<"
					swap(adjCompare, adjCompare - 1, arr);// here we are swapping immediately as the is condition
															// fulfilled.
				}
			} // Comparison loop ends
		} // pass loops ends here

		return arr;
	}// method

}// class ends
