package com.java.sorting;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parent Class for all the sorting Algorithms.
 * 
 * @author Prateek
 *
 */
public abstract class Sorting {

	public static int[] array = { 2, 4, -6, 44, 67, -34, -87, 456 };

	/*
	 * 435, 88, 2, 4, 56, 33, 0, 54, 55, 32, 43, 33, 41, 56, 700, 900,-9,-22,-3 };
	 */
	public void swap(int i, int j, int[] arr) {
		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
	}// swap method ends.

	/* A utility function to print array of size n */
	public static void printArray(int arr[]) {
		List ls = Stream.of(array).toList();
		Collections.sort(ls);
		int n = arr.length;
		for (int i = 0; i < n; ++i)
			System.out.print(arr[i] + " ");

		System.out.println();
	}
}// class ends
